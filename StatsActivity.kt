package com.example.networkcellanalyzer

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.networkcellanalyzer.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.networkcellanalyzer.model.StatsResponse
import java.util.Calendar

class StatsActivity : AppCompatActivity() {

    private lateinit var tvFrom: TextView
    private lateinit var tvTo: TextView
    private lateinit var tvAvgSignal: TextView
    private lateinit var tvAvgSNR: TextView
    private lateinit var tvAvgSignalDevice: TextView
    private lateinit var pbAlfa: ProgressBar
    private lateinit var pbTouch: ProgressBar
    private lateinit var pb4G: ProgressBar
    private lateinit var pb3G: ProgressBar
    private lateinit var pb2G: ProgressBar

    private var fromDate: String = ""
    private var toDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        tvFrom = findViewById(R.id.tvFrom)
        tvTo = findViewById(R.id.tvTo)
        tvAvgSignal = findViewById(R.id.tvAvgSignal)
        tvAvgSNR = findViewById(R.id.tvAvgSNR)
        tvAvgSignalDevice = findViewById(R.id.tvAvgSignalDevice)
        pbAlfa = findViewById(R.id.pbAlfa)
        pbTouch = findViewById(R.id.pbTouch)
        pb4G = findViewById(R.id.pb4G)
        pb3G = findViewById(R.id.pb3G)
        pb2G = findViewById(R.id.pb2G)

       
        tvFrom.setOnClickListener { showDatePicker { date ->
            fromDate = date
            tvFrom.text = date
        }}

        
        tvTo.setOnClickListener { showDatePicker { date ->
            toDate = date
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
}
