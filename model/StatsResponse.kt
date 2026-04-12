package com.example.networkcellanalyzer.model

data class StatsResponse(
    val from_date: String,
    val to_date: String,
    val avg_connectivity_operator: List<Double>,
    val avg_connectivity_network: List<Double>,
    val avg_signal_power_networkType: List<Double>,
    val avg_signal_power_device: Double,
    val avg_SNR_SNIR: List<Double>
)
