package com.example.networkcellanalyzer.model

data class StatsResponse(
    val from_date: String,
    val to_date: String,
    val avg_connectivity_operator: List<Map<String, Any>>,
    val avg_connectivity_network: List<Map<String, Any>>,
    val avg_signal_power_networkType: List<Map<String, Any>>,
    val avg_signal_power_device: List<Map<String, Any>>,
    val avg_SNR_SNIR: List<Map<String, Any>>
)