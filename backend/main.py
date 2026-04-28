from fastapi import FastAPI, Body, Reques
from datetime import datetime, UTC
from schema import *
from database import *
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse
from datetime import timedelta
from argon2 import PasswordHasher
from fastapi import Body
from typing import List
from sqlalchemy import desc
from fastapi import Depends
from sqlalchemy.orm import Session
from auth import create_token, require_auth, get_current_user
from fastapi.responses import HTMLResponse, RedirectResponse, JSONResponse



Source = "$argon2id$v=19$m=65536"
app = FastAPI()

app.mount("/static", StaticFiles(directory="static"), name="static")
# authenticated access to dashboard
@app.get("/dashboard", response_class=HTMLResponse)
def dashboard(request: Request):
    user = get_current_user(request)
    if not user:
        return RedirectResponse(url="/index", status_code=302)
    with open("static/dashboard.html") as f:
        return f.read()
@app.get("/test")
def test():
    return {"ok": True}    
@app.get("/index", response_class=HTMLResponse)
def index():
    with open("static/index.html") as f:
        return f.read()

@app.get("/", response_class=HTMLResponse)
def root():
    with open("static/index.html") as f:
        return f.read()

@app.get("/health")
def health():
    return Health_response(status="ok", time=datetime.now())

@app.post("/identification")
def identify(body: Identification_request):
    db = SessionLocal()
    device = db.query(Device).filter(Device.device_id == body.device_id).first()
    if device != None:
        first_seen = device.first_seen
        last_seen = datetime.now()
        device.last_seen = last_seen
        first_meet = False
        mac_address = body.mac_address
        device.mac_address = mac_address
        device.first_meet = False
        db.commit()
    else:
        first_meet = True
        first_seen = last_seen = datetime.now()
        new_device = Device(
            device_id=body.device_id,
            mac_address=body.mac_address,
            first_seen=first_seen,
            last_seen=last_seen,
            first_meet=True
        )
        db.add(new_device)
        db.commit()
    db.close()

    return Identification_response(
        device_id=body.device_id,
        first_seen=first_seen,
        last_seen=last_seen,
        first_meet=first_meet)

@app.post("/measurement")
def resp_measurements(device_id: str, body: Measurements_request):
    db = SessionLocal()

    # update last_seen on every measurement
    device = db.query(Device).filter(Device.device_id == device_id).first()
    if device:
        device.last_seen = datetime.now()
        db.commit()

    new_Measurment = Measurement(
        device_id=device_id,
        operator=body.operator,
        signal_power=body.signal_power,
        SNR=body.SNR,
        network_type=body.network_type,
        frequency_band=body.frequency_band,
        cell_id=body.cell_id,
        time_stamp=body.time_stamp,
        latitude = body.latitude,
        longitude = body.longitude,

    )

    db.add(new_Measurment)
    db.commit()
    num = db.query(Measurement).filter(Measurement.device_id == device_id).count()
    db.close()

    return Measurements_response(
        status="ok",
        measurement_num=num,
        server_time=datetime.now()
    )

@app.get("/global_stats")
def get_global_stats(request: Request):
    require_auth(request)
    db = SessionLocal()
    records = db.query(Measurement).all()

    operator_label = {"touch": 0, "alfa": 1}
    avg_connectivity_operator = [0, 0]
    operator_signal_sum = [0, 0]
    operator_signal_count = [0, 0]

    network_label = {"2G": 0, "3G": 1, "4G": 2, "5G": 3}
    avg_connectivity_network = [0, 0, 0, 0]
    avg_signal_power_networkType = [0, 0, 0, 0]
    network_count = [0, 0, 0, 0]

    avg_signal_power_overall = 0
    total_count = 0

    for record in records:
        total_count += 1
        avg_signal_power_overall += record.signal_power

        if record.operator in operator_label:
            i = operator_label[record.operator]
            avg_connectivity_operator[i] += 1
            operator_signal_sum[i] += record.signal_power
            operator_signal_count[i] += 1

        if record.network_type in network_label:
            i = network_label[record.network_type]
            avg_connectivity_network[i] += 1
            avg_signal_power_networkType[i] += record.signal_power
            network_count[i] += 1

    if total_count > 0:
        avg_connectivity_operator = [c / total_count for c in avg_connectivity_operator]
        avg_connectivity_network = [c / total_count for c in avg_connectivity_network]
        avg_signal_power_overall = avg_signal_power_overall / total_count
    else:
        avg_signal_power_overall = 0

    operator_avg_signal = []
    for i in range(2):
        if operator_signal_count[i] > 0:
            operator_avg_signal.append(operator_signal_sum[i] / operator_signal_count[i])
        else:
            operator_avg_signal.append(0)

    for i in range(4):
        if network_count[i] > 0:
            avg_signal_power_networkType[i] = avg_signal_power_networkType[i] / network_count[i]
        else:
            avg_signal_power_networkType[i] = 0

    db.close()
    return {
        "total_measurements": total_count,
        "avg_connectivity_operator": avg_connectivity_operator,
        "avg_connectivity_network": avg_connectivity_network,
        "avg_signal_power_networkType": avg_signal_power_networkType,
        "avg_signal_power_operator": operator_avg_signal,
        "avg_signal_power_overall": avg_signal_power_overall
    }

@app.get("/connected_coordinates")
def get_connected_coordinates(request: Request):
    require_auth(request)
    db = SessionLocal()
    now = datetime.now()
    devices = db.query(Device).all()

    results = []
    for device in devices:
        # Only include currently connected devices (matching /devices logic: <30s)
        if (now - device.last_seen).total_seconds() >= 30:
            continue

        latest = (
            db.query(Measurement)
            .filter(Measurement.device_id == device.device_id)
            .filter(Measurement.latitude.isnot(None))
            .filter(Measurement.longitude.isnot(None))
            .order_by(desc(Measurement.time_stamp))
            .first()
        )
        if latest:
            results.append({
                "device_id": device.device_id,
                "latitude": latest.latitude,
                "longitude": latest.longitude,
                "signal_power": latest.signal_power,
                "network_type": latest.network_type,
                "time_stamp": latest.time_stamp.isoformat()
            })
    db.close()
    return results
    
@app.get("/stats")
def get_stats(device_id: str, from_date: datetime, to_date: datetime):
    db = SessionLocal()
    records = db.query(Measurement).filter(
        Measurement.device_id == device_id,
        Measurement.time_stamp >= from_date,
        Measurement.time_stamp <= to_date
    ).all()

    operator_label = {"touch": 0, "alfa": 1}
    avg_connectivity_operator = [0, 0]
    network_label = {"2G": 0, "3G": 1, "4G": 2, "5G": 3}
    avg_connectivity_network = [0, 0, 0, 0]
    avg_signal_power_networkType = [0, 0, 0, 0]
    avg_signal_power_device = 0
    avg_SNR_SNIR = [0, 0, 0, 0]
    snr_network_count = [0, 0, 0, 0]
    total_count = 0
    total_count_snr = 0

    for record in records:
        total_count = total_count + 1
        avg_signal_power_device += record.signal_power
        if record.operator in operator_label:
            tracker = operator_label[record.operator]
            avg_connectivity_operator[tracker] += 1

        if record.network_type in network_label:
            index = network_label[record.network_type]
            avg_connectivity_network[index] += 1
            avg_signal_power_networkType[index] += record.signal_power
            if record.SNR != None:
                total_count_snr += 1
                snr_network_count[index] += 1
                avg_SNR_SNIR[index] += record.SNR

    if total_count > 0:
        avg_connectivity_operator[0] = avg_connectivity_operator[0] / total_count
        avg_connectivity_operator[1] = avg_connectivity_operator[1] / total_count
        avg_signal_power_device = avg_signal_power_device / total_count
        for i in range(0, 4):
            if avg_connectivity_network[i] != 0:
                avg_signal_power_networkType[i] = avg_signal_power_networkType[i] / avg_connectivity_network[i]
            else:
                avg_signal_power_networkType[i] = 0
            avg_connectivity_network[i] = avg_connectivity_network[i] / total_count
    else:
        avg_connectivity_operator = [0, 0]
        avg_connectivity_network = [0, 0, 0, 0]
        avg_signal_power_networkType = [0, 0, 0, 0]
        avg_signal_power_device = 0

    for i in range(0, 4):
        if snr_network_count[i] != 0:
            avg_SNR_SNIR[i] = avg_SNR_SNIR[i] / snr_network_count[i]

    db.close()
    return {
        "from_date": from_date,
        "to_date": to_date,
        "avg_connectivity_operator": avg_connectivity_operator,
        "avg_connectivity_network": avg_connectivity_network,
        "avg_signal_power_networkType": avg_signal_power_networkType,
        "avg_signal_power_device": avg_signal_power_device,
        "avg_SNR_SNIR": avg_SNR_SNIR
    }


@app.post("/logout")
def logout():
    response = JSONResponse(content={"response": "ok"})
    response.delete_cookie("session_token")
    return response

@app.get("/devices")
def get_devices(request: Request):
    require_auth(request)
    db = SessionLocal()
    devices = db.query(Device).all()
    db.close()

    now = datetime.now()
    result = []
    for d in devices:
        last_seen = d.last_seen
        is_connected = (now - last_seen).total_seconds() < 30

        result.append({
            "device_id": d.device_id,
            "mac_address": d.mac_address or "N/A",
            "first_seen": str(d.first_seen),
            "last_seen": str(d.last_seen),
            "status": "Connected" if is_connected else "Previously connected"
        })
    return result

@app.post("/login")
def checkCreds(body: Login_request = Body(...)):
    username = body.username
    password = body.password

    if username != "Admin451":
        return {"response": "user"}

    if password != "1234":
        return {"response": "pass"}

    token = create_token(username)
    response = JSONResponse(content={"response": "ok"})
    response.set_cookie(
        key="session_token",
        value=token,
        httponly=True,       # JS cannot read the cookie
        secure=True,        # set True if using HTTPS
        samesite="lax",
        max_age=43200,
        path = "/"
        # 12 hours
    )
    return response
