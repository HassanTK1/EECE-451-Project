package com.example.a451_app.model

data class MeasurementRequest (
    val device_id: String,
    val operator : String,
    val signal_power : Int,
    val SNR : Float?,
    val network_type: String,
    val frequency_band: Int?,
    val cell_id: String,
    val time_stamp: String

)
