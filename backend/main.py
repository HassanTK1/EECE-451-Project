from fastapi import FastAPI
from datetime import datetime,UTC
from schema import *

app = FastAPI()

@app.get("/")
def root():
    return {"message":"server is running"}

@app.get("/health")
def health():
    return { "status":"ok"}


devices = {}
@app.post("/identification")
def identify(body: Identification_request):
    if body.device_id in devices:
        first_seen = devices[body.device_id]["first_seen"]
        last_seen=datetime.now(UTC)
        devices[body.device_id]["last_seen"] = last_seen
        first_meet=False
        
        
    else:
        first_meet=True
        first_seen=last_seen=datetime.now(UTC)
        devices[body.device_id] = {
    "first_seen": first_seen,
    "last_seen": last_seen,
    "mac_address": body.mac_address,
    "first_meet": True}
        
    return Identification_response(
        device_id=body.device_id,
        first_seen=first_seen,
        last_seen=last_seen,
        first_meet=first_meet)
       
measure={}    
@app.post("/measurements")
def resp_measurements(device_id: str,body:Measurements_request):
    if device_id not in measure:
        measure[device_id] = []

    measure[device_id].append({
        "operator": body.operator,
        "signal_power": body.signal_power,
        "SNR ": body.SNR,
        "network_type": body.network_type,
        "frequency_band": body.frequency_band, 
        "Cell_id": body.cell_id ,
        "time_stamp": datetime.now(UTC)})
    
    return Measurements_response(status="ok", 
                                 measurement_num =len(measure[device_id]),
                                                                   
                                  server_time=datetime.now(UTC))
