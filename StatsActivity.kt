package com.example.a451_app
import kotlin.math.*
import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.a451_app.model.MeasurementRequest
import com.example.a451_app.model.MeasurementResponse
import com.example.a451_app.model.StatsResponse
import com.example.a451_app.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.a451_app.IDmanager

import java.util.Calendar

class StatsActivity : AppCompatActivity() {
    val selectedColor = android.graphics.Color.parseColor("#4A90D9")
    val unselectedColor = android.graphics.Color.parseColor("#3A3A3A")
    private var avgSignalPowerNetworkType: List<Double> = listOf(0.0, 0.0, 0.0, 0.0)
    private var avgSnrNetworkType: List<Double> = listOf(0.0, 0.0, 0.0, 0.0)
    private lateinit var deviceId: String
    private lateinit var tvFrom: TextView
    private lateinit var tvTo: TextView
    private lateinit var tvAvgSignal_network: TextView
    private lateinit var tvAvgSNR_network: TextView
    private lateinit var tvAvgSignalDevice: TextView
    private lateinit var pbAlfa: ProgressBar
    private lateinit var pbTouch: ProgressBar
    private lateinit var pb5G: ProgressBar
    private lateinit var pb4G: ProgressBar
    private lateinit var pb3G: ProgressBar
    private lateinit var pb2G: ProgressBar

    private lateinit var btn2G: TextView
    private lateinit var btn3G: TextView
    private lateinit var btn4G: TextView
    private lateinit var btn5G: TextView
    private var fromDate: String = ""
    private var toDate: String = ""
    private var index = 2

    private val statshandler = Handler(Looper.getMainLooper())
    private val statsinterval = 10000L

    private val statsRunnable = object : Runnable {
        override fun run() {
            AskForStats()
            statshandler.postDelayed(this, statsinterval)
        }
    }

    override fun onStart() {
        super.onStart()
        statshandler.post(statsRunnable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        deviceId = IDmanager(this).checkId()
        tvFrom = findViewById(R.id.tvFrom)
        tvTo = findViewById(R.id.tvTo)
        tvAvgSignal_network = findViewById(R.id.tvAvgSignal)
        tvAvgSNR_network = findViewById(R.id.tvAvgSNR)
        tvAvgSignalDevice = findViewById(R.id.tvAvgSignalDevice)
        pbAlfa = findViewById(R.id.pbAlfa)
        pbTouch = findViewById(R.id.pbTouch)
        pb5G = findViewById(R.id.pb5G)
        pb4G = findViewById(R.id.pb4G)
        pb3G = findViewById(R.id.pb3G)
        pb2G = findViewById(R.id.pb2G)

        btn2G = findViewById(R.id.btn2G)
        btn3G = findViewById(R.id.btn3G)
        btn4G = findViewById(R.id.btn4G)
        btn5G = findViewById(R.id.btn5G)



        btn2G.setOnClickListener{
            index = 0
            btn2G.setBackgroundColor(selectedColor)
            btn3G.setBackgroundColor(unselectedColor)
            btn4G.setBackgroundColor(unselectedColor)
            btn5G.setBackgroundColor(unselectedColor)
            updateUI()

        }
        btn3G.setOnClickListener{
            index = 1
            btn2G.setBackgroundColor(unselectedColor)
            btn3G.setBackgroundColor(selectedColor)
            btn4G.setBackgroundColor(unselectedColor)
            btn5G.setBackgroundColor(unselectedColor)
            updateUI()
        }
        btn4G.setOnClickListener{
            index = 2
            btn2G.setBackgroundColor(unselectedColor)
            btn3G.setBackgroundColor(unselectedColor)
            btn4G.setBackgroundColor(selectedColor)
            btn5G.setBackgroundColor(unselectedColor)
            updateUI()
        }

        btn5G.setOnClickListener{
            index = 3
            btn2G.setBackgroundColor(unselectedColor)
            btn3G.setBackgroundColor(unselectedColor)
            btn4G.setBackgroundColor(unselectedColor)
            btn5G.setBackgroundColor(selectedColor)
            updateUI()

        }


        tvFrom.setOnClickListener {
            showDatePicker { date ->
            fromDate = "${date}T00:00:00Z"
            tvFrom.text = date
        }}


        tvTo.setOnClickListener { showDatePicker { date ->
            toDate = "${date}T23:59:59Z"
            tvTo.text = date
        }}


        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }



    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val date = "$year-${(month + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                onDateSelected(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }




    private fun AskForStats() {
        if (fromDate.isBlank() || toDate.isBlank()) return
        RetrofitClient.apiService.getStats( deviceId,fromDate,toDate)
            .enqueue(object : Callback<StatsResponse> {
                override fun onResponse(call: Call<StatsResponse>, response: Response<StatsResponse>) {
                    if (response.isSuccessful && response.body() != null){
                        val stats = response.body()!!
                        pbTouch.progress =  ((stats.avg_connectivity_operator[0]?:0.0) * 100).toInt()
                        pbAlfa.progress =  ((stats.avg_connectivity_operator[1]?:0.0) * 100).toInt()
                        pb2G.progress = ((stats.avg_connectivity_network[0]?:0.0) * 100).toInt()
                        pb3G.progress = ((stats.avg_connectivity_network[1]?:0.0) * 100).toInt()
                        pb4G.progress = ((stats.avg_connectivity_network[2]?:0.0) * 100).toInt()
                        pb5G.progress = ((stats.avg_connectivity_network[3]?:0.0) * 100).toInt()
                        avgSignalPowerNetworkType = stats.avg_signal_power_networkType?:listOf(0.0,0.0,0.0,0.0)
                        avgSnrNetworkType =stats.avg_SNR_SNIR?:listOf(0.0,0.0,0.0,0.0)
                        tvAvgSignalDevice.text = (stats.avg_signal_power_device ?: 0.0).toString()
                        updateUI()
                    }
                }
                override fun onFailure(call: Call<StatsResponse>, t: Throwable) {
                }
            })
    }

    private fun updateUI() {
        tvAvgSignal_network.text =
            String.format("%.2f", avgSignalPowerNetworkType.getOrNull(index) ?: 0.0)

        tvAvgSNR_network.text =
            String.format("%.2f", avgSnrNetworkType.getOrNull(index) ?: 0.0)
    }


    override fun onStop() {
        super.onStop()
        statshandler.removeCallbacks(statsRunnable)
    }
}
