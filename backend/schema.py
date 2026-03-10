from pydantic import BaseModel, Field
from typing import Optional, List, Literal
from datetime import datetime


class Health_response(BaseModel):
    status: Literal["ok"] = "ok"
    time : datetime


class Identification_request(BaseModel):

    device_id: str 
    mac_address: Optional[str] = None


class Identification_response(BaseModel):
    device_id: str
    first_meet: bool
    first_seen: datetime
    last_seen: datetime


class Measurements_request(BaseModel):
    operator: str
    signal_power: int # in decibels
    SNR : Optional[float] = None
    network_type: Literal["2G","3G","4G"]   ### FOCUS - type might be better categorized
    frequency_band: Optional[int] = None # review data format
    cell_id: str # maybe str 
    time_stamp: datetime    


class Measurements_response(BaseModel):
     status: Literal["ok"] = "ok"
     measurement_num: int
     server_time: datetime



class Stats_response(BaseModel):
    from_date: datetime
    to_date: datetime
    avg_connectivity_operator: List
    avg_connectivity_network: List
    avg_signal_power_networkType: List
    avg_signal_power_device: List
    avg_SNR_SNIR: List
    





