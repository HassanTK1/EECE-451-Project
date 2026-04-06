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
       
  
@app.post("/measurement")
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
    device = db.query(Device).filter(Device.device_id == body.device_id).first()
    if device is not None:
        device.last_seen = datetime.now(UTC)
    db.commit()
    num=db.query(Measurement).filter(Measurement.device_id == device_id).count()
    db.close()
    
    return Measurements_response(status="ok", 
                                 measurement_num = num,
                                                                   
                                  server_time=datetime.now(UTC))




@app.get("/stats")
def get_stats(device_id:str,from_date: datetime, to_date: datetime):

    db = SessionLocal()
    records = db.query(Measurement).filter(
        Measurement.device_id == device_id,
        Measurement.time_stamp >= from_date,
        Measurement.time_stamp <= to_date
    ).all()

    operator_label = {"touch":0, "alfa":1}
    avg_connectivity_operator = [0,0]  # [TOUCH, ALFA]
    network_label = {"2G":0,"3G":1,"4G":2,"5G":3}  
    avg_connectivity_network = [0,0,0,0]  # [2G,3G,4G,5G]
    avg_signal_power_networkType = [0,0,0,0]  # [2G,3G,4G,5G]
    avg_signal_power_device = 0
    avg_SNR_SNIR = [0,0,0,0]
    snr_network_count = [0,0,0,0]
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
            avg_connectivity_network[index] +=1
            avg_signal_power_networkType[index]+= record.signal_power
            if record.SNR != None:
                total_count_snr += 1
                snr_network_count[index] +=1
                avg_SNR_SNIR[index] += record.SNR
                
    
    if total_count > 0:
        avg_connectivity_operator[0] = avg_connectivity_operator[0]/total_count 
        avg_connectivity_operator[1] = avg_connectivity_operator[1]/total_count 
        avg_signal_power_device = avg_signal_power_device/total_count 
        for i in range(0,4):
            if avg_connectivity_network[i] != 0:
                avg_signal_power_networkType[i] = avg_signal_power_networkType[i]/avg_connectivity_network[i]
                
            else:
                avg_signal_power_networkType[i] = 0
            avg_connectivity_network[i] = avg_connectivity_network[i]/total_count

    else:
        avg_connectivity_operator = [0, 0]
        avg_connectivity_network = [0, 0, 0, 0]
        avg_signal_power_networkType = [0, 0, 0, 0]
        avg_signal_power_device = 0
 
    for i in range(0,4):
        if snr_network_count[i] != 0:
            avg_SNR_SNIR[i] = avg_SNR_SNIR[i]/snr_network_count[i]        
    
 
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
