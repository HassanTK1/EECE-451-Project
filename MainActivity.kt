package com.example.a451_app
import android.os.Bundle
import android.os.Looper
import android.Manifest
import android. content.pm.PackageManager
import androidx.core. content. ContextCompat
import android.os.Handler
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.a451_app.R
import com.example.a451_app.model.HealthResponse
import com.example.a451_app.model.MeasurementRequest
import com.example.a451_app.model.MeasurementResponse
import com.example.a451_app.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.a451_app.readMeasurementFromPhone

class MainActivity : AppCompatActivity() {

    private lateinit var tvServerStatus: TextView

    private lateinit var tvOperator: TextView
    private lateinit var tvNetwork: TextView
    private lateinit var tvSignal: TextView
    private lateinit var tvSNR: TextView
    private lateinit var tvCellid: TextView
    private lateinit var tvBand: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val interval = 3000L

    private val measurementHandler = Handler(Looper.getMainLooper())
    private val measurementInterval = 10000L // 10 seconds



    private val healthRunnable = object : Runnable {
        override fun run() {
            checkHealth()
            handler.postDelayed(this, interval)
        }
    }

    private val measurementRunnable = object : Runnable {
        override fun run() {
            readShowAndSendMeasurement()

            measurementHandler.postDelayed( this , measurementInterval)
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



        handler.post(healthRunnable)

        if(hasRequiredPermissions()) {
            measurementHandler.post(measurementRunnable)
        }
        else{
            requestRequiredPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(healthRunnable)
        measurementHandler.removeCallbacks(measurementRunnable)
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
        RetrofitClient.apiService.sendMetrics(measurement)
    }
    private fun readShowAndSendMeasurement() {
        val deviceId = "device_123"   // temporary for now

        val measurement = readMeasurementFromPhone(this, deviceId)

        if (measurement != null) {
            updateUI(measurement)
            sendMeasurementToServer(measurement)
        } else {
            tvOperator.text = "--"
            tvNetwork.text = "333"
            tvSignal.text = "333"
            tvSNR.text = "--"
            tvCellid.text = "--"
            tvBand.text = "--"

        }
    }
    private fun updateUI(measurement : MeasurementRequest){
        tvOperator.text = measurement.operator
        tvNetwork.text = measurement.network_type
        tvSignal.text = "${measurement.signal_power} dBm"
        tvSNR.text = measurement.SNR?.toString() ?: "--"
        tvCellid.text = measurement.cell_id
        tvBand.text = measurement.frequency_band?.toString() ?: "--"
    }
    private fun hasRequiredPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val phoneStateGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted && phoneStateGranted
    }
    private fun checkHealth() {


        try {
            RetrofitClient.apiService.isHealthy().enqueue(object : Callback<HealthResponse> {
                override fun onResponse(
                    call: Call<HealthResponse>,
                    response: Response<HealthResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "ok") {
                        tvServerStatus.text = "ONLINE"
                    } else {
                        tvServerStatus.text = "OFFLINE"
                    }
                }

                override fun onFailure(call: Call<HealthResponse>, t: Throwable) {
                    tvServerStatus.text = "OFFLINE: ${t.message}"
                }
            })
        } catch (e: Exception) {
            tvServerStatus.text = "CRASH: ${e.message}"
        }
    }
}