package com.example.a451_app.model

data class IdentificationResponse (
    val device_id: String,
    val first_meet: Boolean,
    val first_seen: String,
    val last_seen: String
)