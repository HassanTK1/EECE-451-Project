from sqlalchemy import create_engine,Column,String,DateTime,Boolean,Integer,Float,ForeignKey
from sqlalchemy.orm import sessionmaker,declarative_base

DATABASE_URL = "postgresql://postgres:Garod2005@localhost:5432/network_analyzer" #postgresql://username:password@host:port/database_name (this is the format , replace with your own values )

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(bind=engine)
Base = declarative_base()


class Device(Base):
    __tablename__ = "devices"
    
    device_id=Column(String,primary_key=True)
    first_seen=Column(DateTime)
    last_seen=Column(DateTime)
    mac_address=Column(String,nullable=True)
    first_meet=Column(Boolean)


class Measurement(Base):
    __tablename__ = "measure"

    device_id = Column(String, ForeignKey("devices.device_id")) ##ForeignKey checks if the id in the 'devices' tabel , if not it rejects it 
    operator=Column(String)
    signal_power=Column(Integer)
    SNR=Column(Float,nullable=True)
    network_type=Column(String)
    frequency_band=Column(Integer,nullable=True)
    cell_id=Column(String)
    time_stamp=Column(DateTime)
    id=Column(Integer,primary_key=True,autoincrement=True)   
    
    
    
Base.metadata.create_all(bind=engine)
    