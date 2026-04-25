package com.example.a451_app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.a451_app.model.HealthResponse
import com.example.a451_app.model.IdentificationRequest
import com.example.a451_app.model.IdentificationResponse
import com.example.a451_app.model.MeasurementRequest
import com.example.a451_app.model.MeasurementResponse
import com.example.a451_app.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var deviceId: String
    private lateinit var tvServerStatus: TextView
    private lateinit var tvOperator: TextView
    private lateinit var tvNetwork: TextView
    private lateinit var tvSignal: TextView
    private lateinit var tvSNR: TextView
    private lateinit var tvCellid: TextView
    private lateinit var tvBand: TextView
    private lateinit var tvSignalGrade: TextView
    private lateinit var tvSignalQualityLabel: TextView
    private lateinit var statusDot: View
    private lateinit var mainScrollView: ScrollView
    private lateinit var btnToggleTheme: Button

    private lateinit var signalBarEmpty: View
    private lateinit var signalBarFill: View
    private lateinit var snrBarEmpty: View
    private lateinit var snrBarFill: View

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
        ThemeManager.applyStoredTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deviceId = IDmanager(this).checkId()
        tvServerStatus = findViewById(R.id.tvServerStatus)
        tvOperator = findViewById(R.id.tvOperator)
        tvNetwork = findViewById(R.id.tvNetwork)
        tvSignal = findViewById(R.id.tvSignal)
        tvSNR = findViewById(R.id.tvSNR)
        tvCellid = findViewById(R.id.tvCellid)
        tvBand = findViewById(R.id.tvBand)
        tvSignalGrade = findViewById(R.id.tvSignalGrade)
        tvSignalQualityLabel = findViewById(R.id.tvSignalQualityLabel)
        statusDot = findViewById(R.id.statusDot)
        mainScrollView = findViewById(R.id.mainScrollView)
        btnToggleTheme = findViewById(R.id.btnToggleTheme)
        signalBarEmpty = findViewById(R.id.signalBarEmpty)
        signalBarFill = findViewById(R.id.signalBarFill)
        snrBarEmpty = findViewById(R.id.snrBarEmpty)
        snrBarFill = findViewById(R.id.snrBarFill)

        updateThemeButtonText()

        btnToggleTheme.setOnClickListener {
            ThemeManager.toggle(this)
            // Activity will recreate automatically with new theme
        }

        val btnViewStats = findViewById<Button>(R.id.btnViewStats)
        btnViewStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        handler.post(healthRunnable)
        registerDevice()
    }

    private fun updateThemeButtonText() {
        btnToggleTheme.text = if (ThemeManager.isDark(this)) "☀ Light" else "🌙 Dark"
    }

    // Signal: -120 dBm = 0%, -50 dBm = 100%
    private fun signalToPercent(dbm: Int): Int =
        ((dbm + 120).toFloat() / 70f * 100f).toInt().coerceIn(0, 100)

    // SNR: 0 dB = 0%, 40 dB = 100%
    private fun snrToPercent(snr: Float): Int =
        (snr / 40f * 100f).toInt().coerceIn(0, 100)

    private fun setBarPercent(emptyView: View, fillView: View, percent: Int) {
        val emptyParams = emptyView.layoutParams as LinearLayout.LayoutParams
        emptyParams.weight = (100 - percent).toFloat()
        emptyView.layoutParams = emptyParams

        val fillParams = fillView.layoutParams as LinearLayout.LayoutParams
        fillParams.weight = percent.toFloat()
        fillView.layoutParams = fillParams
    }

    private fun updateSignalGrade(signalPower: Int) {
        val (grade, label, colorRes) = when {
            signalPower > -70  -> Triple("A", "Excellent", R.color.accent_green)
            signalPower > -85  -> Triple("B", "Good",      R.color.accent_light_green)
            signalPower > -100 -> Triple("C", "Fair",      R.color.accent_orange)
            else               -> Triple("D", "Poor",      R.color.accent_red)
        }
        val color = ContextCompat.getColor(this, colorRes)
        tvSignalGrade.text = grade
        tvSignalGrade.setTextColor(color)
        tvSignalQualityLabel.text = label
        tvSignalQualityLabel.setTextColor(color)
    }

    private fun registerDevice() {
        val request = IdentificationRequest(device_id = deviceId, mac_address = null)
        RetrofitClient.apiService.identifyDevice(request)
            .enqueue(object : Callback<IdentificationResponse> {
                override fun onResponse(call: Call<IdentificationResponse>, response: Response<IdentificationResponse>) {
                    if (hasRequiredPermissions()) measurementHandler.post(measurementRunnable)
                    else requestRequiredPermissions()
                }
                override fun onFailure(call: Call<IdentificationResponse>, t: Throwable) {
                    if (hasRequiredPermissions()) measurementHandler.post(measurementRunnable)
                    else requestRequiredPermissions()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(healthRunnable)
        measurementHandler.removeCallbacks(measurementRunnable)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            measurementHandler.post(measurementRunnable)
        }
    }

    private fun requestRequiredPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE),
            100
        )
    }

    private fun sendMeasurementToServer(measurement: MeasurementRequest) {
        RetrofitClient.apiService.sendMetrics(deviceId, measurement)
            .enqueue(object : Callback<MeasurementResponse> {
                override fun onResponse(call: Call<MeasurementResponse>, response: Response<MeasurementResponse>) {}
                override fun onFailure(call: Call<MeasurementResponse>, t: Throwable) {}
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
            tvSignalGrade.text = "--"
            tvSignalQualityLabel.text = "--"
            setBarPercent(signalBarEmpty, signalBarFill, 0)
            setBarPercent(snrBarEmpty, snrBarFill, 0)
        }
    }

    private fun updateUI(measurement: MeasurementRequest) {
        tvOperator.text = measurement.operator
        tvNetwork.text = measurement.network_type
        tvSignal.text = "${measurement.signal_power} dBm"
        tvSNR.text = measurement.SNR?.toString() ?: "--"
        tvCellid.text = measurement.cell_id
        tvBand.text = measurement.frequency_band?.toString() ?: "--"
        updateSignalGrade(measurement.signal_power)
        setBarPercent(signalBarEmpty, signalBarFill, signalToPercent(measurement.signal_power))
        setBarPercent(snrBarEmpty, snrBarFill, measurement.SNR?.let { snrToPercent(it) } ?: 0)
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkHealth() {
        try {
            RetrofitClient.apiService.isHealthy().enqueue(object : Callback<HealthResponse> {
                override fun onResponse(call: Call<HealthResponse>, response: Response<HealthResponse>) {
                    if (response.isSuccessful && response.body()?.status == "ok") {
                        tvServerStatus.text = "ONLINE"
                        tvServerStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_green))
                        statusDot.setBackgroundResource(R.drawable.dot_background)
                    } else {
                        tvServerStatus.text = "OFFLINE"
                        tvServerStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_red))
                    }
                }
                override fun onFailure(call: Call<HealthResponse>, t: Throwable) {
                    tvServerStatus.text = "OFFLINE"
                    tvServerStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_red))
                }
            })
        } catch (e: Exception) {
            tvServerStatus.text = "ERROR"
        }
    }
}
