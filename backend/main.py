from fastapi import FastAPI
from datetime import datetime,UTC
from schema import *
from database import *

app = FastAPI()

@app.get("/")
def root():
    return {"message":"server is running"}

@app.get("/health")
def health():
    return { "status":"ok"}



@app.post("/identification")
def identify(body: Identification_request):
    db = SessionLocal()
    device = db.query(Device).filter(Device.device_id == body.device_id).first()
    if device!=None :
        first_seen = device.first_seen
        last_seen=datetime.now(UTC)
        device.last_seen = last_seen
        first_meet = False 
        mac_address=body.mac_address
        device.mac_address=mac_address
        device.first_meet=False
        db.commit()
        
        
    else:
        first_meet=True
    
        first_seen=last_seen=datetime.now(UTC)
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
       
  
@app.post("/measurements")
def resp_measurements(device_id:str,body:Measurements_request):
    db=SessionLocal()

    new_Measurment=Measurement(
            device_id = device_id, 
            operator=body.operator,
            signal_power=body.signal_power,
            SNR=body.SNR,
            network_type=body.network_type,
            frequency_band=body.frequency_band,
            cell_id=body.cell_id,
            time_stamp=body.time_stamp,
            
        )
        
    db.add(new_Measurment)
    db.commit()
    num=db.query(Measurement).filter(Measurement.device_id == device_id).count()
    db.close()
    
    return Measurements_response(status="ok", 
                                 measurement_num = num,
                                                                   
                                  server_time=datetime.now(UTC))




@app.get("/stats")
def get_stats(from_date: datetime, to_date: datetime):
    filtered = []

    for device_id, records in measure.items():
        for record in records:
            if from_date <= record["time_stamp"] <= to_date:
                filtered.append({
                    "device_id": device_id,
                    "operator": record["operator"],
                    "network_type": record["network_type"],
                    "signal_power": record["signal_power"],
                    "SNR": record["SNR"],
                    "time_stamp": record["time_stamp"]
                })

    return {
        "from_date": from_date,
        "to_date": to_date,
        "count": len(filtered),
        "measurements": filtered
    }
