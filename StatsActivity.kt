package com.example.a451_app

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.a451_app.model.StatsResponse
import com.example.a451_app.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class StatsActivity : AppCompatActivity() {

    private var avgSignalPowerNetworkType: List<Double> = listOf(0.0, 0.0, 0.0, 0.0)
    private var avgSnrNetworkType: List<Double> = listOf(0.0, 0.0, 0.0, 0.0)
    private lateinit var deviceId: String

    private lateinit var tvFrom: TextView
    private lateinit var tvTo: TextView
    private lateinit var tvAvgSignal_network: TextView
    private lateinit var tvAvgSNR_network: TextView
    private lateinit var tvAvgSignalDevice: TextView

    private lateinit var segAlfa: View
    private lateinit var segTouch: View
    private lateinit var tvAlfaPercent: TextView
    private lateinit var tvTouchPercent: TextView

    private lateinit var seg2G: View
    private lateinit var seg3G: View
    private lateinit var seg4G: View
    private lateinit var seg5G: View
    private lateinit var tv2GPercent: TextView
    private lateinit var tv3GPercent: TextView
    private lateinit var tv4GPercent: TextView
    private lateinit var tv5GPercent: TextView

    private lateinit var btn2G: TextView
    private lateinit var btn3G: TextView
    private lateinit var btn4G: TextView
    private lateinit var btn5G: TextView

    private var fromDate: String = ""
    private var toDate: String = ""
    private lateinit var statsScrollView: ScrollView
    private lateinit var btnToggleThemeStats: Button
    private var index = 2

    private val statsHandler = Handler(Looper.getMainLooper())
    private val statsInterval = 10000L
    private val statsRunnable = object : Runnable {
        override fun run() {
            askForStats()
            statsHandler.postDelayed(this, statsInterval)
        }
    }

    override fun onStart() {
        super.onStart()
        statsHandler.post(statsRunnable)
    }

    override fun onStop() {
        super.onStop()
        statsHandler.removeCallbacks(statsRunnable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyStoredTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        deviceId = IDmanager(this).checkId()

        tvFrom = findViewById(R.id.tvFrom)
        tvTo = findViewById(R.id.tvTo)
        tvAvgSignal_network = findViewById(R.id.tvAvgSignal)
        tvAvgSNR_network = findViewById(R.id.tvAvgSNR)
        tvAvgSignalDevice = findViewById(R.id.tvAvgSignalDevice)
        statsScrollView = findViewById(R.id.statsScrollView)
        btnToggleThemeStats = findViewById(R.id.btnToggleThemeStats)

        segAlfa = findViewById(R.id.segAlfa)
        segTouch = findViewById(R.id.segTouch)
        tvAlfaPercent = findViewById(R.id.tvAlfaPercent)
        tvTouchPercent = findViewById(R.id.tvTouchPercent)

        seg2G = findViewById(R.id.seg2G)
        seg3G = findViewById(R.id.seg3G)
        seg4G = findViewById(R.id.seg4G)
        seg5G = findViewById(R.id.seg5G)
        tv2GPercent = findViewById(R.id.tv2GPercent)
        tv3GPercent = findViewById(R.id.tv3GPercent)
        tv4GPercent = findViewById(R.id.tv4GPercent)
        tv5GPercent = findViewById(R.id.tv5GPercent)

        btn2G = findViewById(R.id.btn2G)
        btn3G = findViewById(R.id.btn3G)
        btn4G = findViewById(R.id.btn4G)
        btn5G = findViewById(R.id.btn5G)

        // Default date range: yesterday → today
        val calendar = Calendar.getInstance()
        val today = formatDate(calendar)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = formatDate(calendar)
        fromDate = "${yesterday}T00:00:00Z"
        toDate = "${today}T23:59:59Z"
        tvFrom.text = yesterday
        tvTo.text = today

        btn2G.setOnClickListener { index = 0; updateButtons(btn2G); updateUI() }
        btn3G.setOnClickListener { index = 1; updateButtons(btn3G); updateUI() }
        btn4G.setOnClickListener { index = 2; updateButtons(btn4G); updateUI() }
        btn5G.setOnClickListener { index = 3; updateButtons(btn5G); updateUI() }

        updateThemeButtonText()
        btnToggleThemeStats.setOnClickListener { ThemeManager.toggle(this) }

        tvFrom.setOnClickListener { showDatePicker { date -> fromDate = "${date}T00:00:00Z"; tvFrom.text = date } }
        tvTo.setOnClickListener   { showDatePicker { date -> toDate   = "${date}T23:59:59Z"; tvTo.text   = date } }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun updateThemeButtonText() {
        btnToggleThemeStats.text = if (ThemeManager.isDark(this)) "☀ Light" else "🌙 Dark"
    }

    private fun selectedColor() = ContextCompat.getColor(this, R.color.accent_blue)
    private fun unselectedColor() = ContextCompat.getColor(this, R.color.bg_button_unselected)

    private fun formatDate(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    private fun updateButtons(selected: TextView) {
        listOf(btn2G, btn3G, btn4G, btn5G).forEach { it.setBackgroundColor(unselectedColor()) }
        selected.setBackgroundColor(selectedColor())
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            onDateSelected("$year-${(month + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}")
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setSegWeight(view: View, weight: Float) {
        val params = view.layoutParams as LinearLayout.LayoutParams
        params.weight = weight
        view.layoutParams = params
    }

    private fun updateOperatorBar(alfa: Float, touch: Float) {
        val total = alfa + touch
        val wa = if (total > 0f) alfa  else 1f
        val wt = if (total > 0f) touch else 1f
        setSegWeight(segAlfa,  wa)
        setSegWeight(segTouch, wt)
        tvAlfaPercent.text  = if (total > 0f) "${(alfa  / total * 100).toInt()}%" else "--"
        tvTouchPercent.text = if (total > 0f) "${(touch / total * 100).toInt()}%" else "--"
    }

    private fun updateNetworkBar(v2g: Float, v3g: Float, v4g: Float, v5g: Float) {
        val total = v2g + v3g + v4g + v5g
        val w2 = if (total > 0f) v2g else 1f
        val w3 = if (total > 0f) v3g else 1f
        val w4 = if (total > 0f) v4g else 1f
        val w5 = if (total > 0f) v5g else 1f
        setSegWeight(seg2G, w2)
        setSegWeight(seg3G, w3)
        setSegWeight(seg4G, w4)
        setSegWeight(seg5G, w5)
        tv2GPercent.text = if (total > 0f) "${(v2g / total * 100).toInt()}%" else "--"
        tv3GPercent.text = if (total > 0f) "${(v3g / total * 100).toInt()}%" else "--"
        tv4GPercent.text = if (total > 0f) "${(v4g / total * 100).toInt()}%" else "--"
        tv5GPercent.text = if (total > 0f) "${(v5g / total * 100).toInt()}%" else "--"
    }

    private fun askForStats() {
        if (fromDate.isBlank() || toDate.isBlank()) return
        RetrofitClient.apiService.getStats(deviceId, fromDate, toDate)
            .enqueue(object : Callback<StatsResponse> {
                override fun onResponse(call: Call<StatsResponse>, response: Response<StatsResponse>) {
                    if (!response.isSuccessful || response.body() == null) return
                    val stats = response.body()!!

                    val touch = (stats.avg_connectivity_operator[0] ?: 0.0).toFloat()
                    val alfa  = (stats.avg_connectivity_operator[1] ?: 0.0).toFloat()
                    updateOperatorBar(alfa, touch)

                    val v2g = (stats.avg_connectivity_network[0] ?: 0.0).toFloat()
                    val v3g = (stats.avg_connectivity_network[1] ?: 0.0).toFloat()
                    val v4g = (stats.avg_connectivity_network[2] ?: 0.0).toFloat()
                    val v5g = (stats.avg_connectivity_network[3] ?: 0.0).toFloat()
                    updateNetworkBar(v2g, v3g, v4g, v5g)

                    avgSignalPowerNetworkType = stats.avg_signal_power_networkType ?: listOf(0.0, 0.0, 0.0, 0.0)
                    avgSnrNetworkType         = stats.avg_SNR_SNIR               ?: listOf(0.0, 0.0, 0.0, 0.0)
                    tvAvgSignalDevice.text = (stats.avg_signal_power_device ?: 0.0).toString()
                    updateUI()
                }
                override fun onFailure(call: Call<StatsResponse>, t: Throwable) {}
            })
    }

    private fun updateUI() {
        tvAvgSignal_network.text = String.format("%.2f", avgSignalPowerNetworkType.getOrNull(index) ?: 0.0)
        tvAvgSNR_network.text    = String.format("%.2f", avgSnrNetworkType.getOrNull(index)         ?: 0.0)
    }
}
