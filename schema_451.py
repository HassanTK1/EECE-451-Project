from pydantic import BaseModel, Field
from typing import Optional, List, Literal
from datetime import datetime


class Health_response(BaseModel):
    status: Literal["ok"] = "ok"
    time : datetime


class Identification_request(Basemodel):

    device_id: str 
    mac_address: Optional[str]


class Identification_response(Basemodel):
    device_id: str
    first_meet: bool
    first_seen: datetime
    last_seen: datetime


class Measurements_request(BaseModel):
    operator: str
    signal_power: int # in decibels
    SNR : int
    network_type: str   ### FOCUS - type might be better categorized
    frequency_band: int # review data format
    Cell_id: int # maybe str 
    time_stamp: datetime    


class Measuremnents_response(Basemodel):
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
    





