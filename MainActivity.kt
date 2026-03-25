package com.example.networkcellanalyzer

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.networkcellanalyzer.model.HealthResponse
import com.example.networkcellanalyzer.model.IdentificationRequest
import com.example.networkcellanalyzer.model.IdentificationResponse
import com.example.networkcellanalyzer.model.MeasurementRequest
import com.example.networkcellanalyzer.model.MeasurementResponse
import com.example.networkcellanalyzer.network.RetrofitClient
import com.example.networkcellanalyzer.telephony.readMeasurementFromPhone
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var tvServerStatus: TextView
    private lateinit var tvOperator: TextView
    private lateinit var tvNetwork: TextView
    private lateinit var tvSignal: TextView
    private lateinit var tvSNR: TextView
    private lateinit var tvCellid: TextView
    private lateinit var tvBand: TextView

    private val deviceId = "device_12"

    private val handler = Handler(Looper.getMainLooper())
    private val interval = 3000L

    private val measurementHandler = Handler(Looper.getMainLooper())
    private val measurementInterval = 10000L

    private val healthRunnable = object : Runnable {
        override fun run() {
            checkHealth()
            handler.postDelayed(this, interval)
        }
    }

    private val measurementRunnable = object : Runnable {
        override fun run() {
            readShowAndSendMeasurement()
            measurementHandler.postDelayed(this, measurementInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvServerStatus = findViewById(R.id.tvServerStatus)
        tvOperator = findViewById(R.id.tvOperator)
        tvNetwork = findViewById(R.id.tvNetwork)
        tvSignal = findViewById(R.id.tvSignal)
        tvSNR = findViewById(R.id.tvSNR)
        tvCellid = findViewById(R.id.tvCellid)
        tvBand = findViewById(R.id.tvBand)

        val btnViewStats = findViewById<Button>(R.id.btnViewStats)
        btnViewStats.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
        }

        // start health check immediately
        handler.post(healthRunnable)

        // register device first, then start measurements
        registerDevice()
    }

    private fun registerDevice() {
        val request = IdentificationRequest(
            device_id = deviceId,
            mac_address = null
        )
        RetrofitClient.apiService.identifyDevice(request)
            .enqueue(object : Callback<IdentificationResponse> {
                override fun onResponse(
                    call: Call<IdentificationResponse>,
                    response: Response<IdentificationResponse>
                ) {
                    // device registered successfully, now start measurements
                    if (hasRequiredPermissions()) {
                        measurementHandler.post(measurementRunnable)
                    } else {
                        requestRequiredPermissions()
                    }
                }
                override fun onFailure(call: Call<IdentificationResponse>, t: Throwable) {
                    // server unreachable, still try measurements
                    if (hasRequiredPermissions()) {
                        measurementHandler.post(measurementRunnable)
                    } else {
                        requestRequiredPermissions()
                    }
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(healthRunnable)
        measurementHandler.removeCallbacks(measurementRunnable)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            measurementHandler.post(measurementRunnable)
        }
    }

    private fun requestRequiredPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            ),
            100
        )
    }

    private fun sendMeasurementToServer(measurement: MeasurementRequest) {
        RetrofitClient.apiService.sendMetrics(measurement.device_id, measurement)
            .enqueue(object : Callback<MeasurementResponse> {
                override fun onResponse(call: Call<MeasurementResponse>, response: Response<MeasurementResponse>) {
                }
                override fun onFailure(call: Call<MeasurementResponse>, t: Throwable) {
                }
            })
    }

    private fun readShowAndSendMeasurement() {
        val measurement = readMeasurementFromPhone(this, deviceId)

        if (measurement != null) {
            updateUI(measurement)
            sendMeasurementToServer(measurement)
        } else {
            tvOperator.text = "--"
            tvNetwork.text = "--"
            tvSignal.text = "--"
            tvSNR.text = "--"
            tvCellid.text = "--"
            tvBand.text = "--"
        }
    }

    private fun updateUI(measurement: MeasurementRequest) {
        tvOperator.text = measurement.operator
        tvNetwork.text = measurement.network_type
        tvSignal.text = "${measurement.signal_power} dBm"
        tvSNR.text = measurement.SNR?.toString() ?: "--"
        tvCellid.text = measurement.cell_id
        tvBand.text = measurement.frequency_band?.toString() ?: "--"
    }

    private fun hasRequiredPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val phoneStateGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted && phoneStateGranted
    }

    private fun checkHealth() {
        try {
            RetrofitClient.apiService.isHealthy().enqueue(object : Callback<HealthResponse> {
                override fun onResponse(call: Call<HealthResponse>, response: Response<HealthResponse>) {
                    if (response.isSuccessful && response.body()?.status == "ok") {
                        tvServerStatus.text = "ONLINE"
                        tvServerStatus.setTextColor(0xFF4CAF50.toInt())
                    } else {
                        tvServerStatus.text = "OFFLINE"
                        tvServerStatus.setTextColor(0xFFE24B4A.toInt())
                    }
                }
                override fun onFailure(call: Call<HealthResponse>, t: Throwable) {
                    tvServerStatus.text = "OFFLINE"
                    tvServerStatus.setTextColor(0xFFE24B4A.toInt())
                }
            })
        } catch (e: Exception) {
            tvServerStatus.text = "CRASH: ${e.message}"
        }
    }
}
