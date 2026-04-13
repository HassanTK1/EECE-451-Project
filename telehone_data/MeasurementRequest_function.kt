package com.example.networkcellanalyzer.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.telephony.CellInfoGsm
import android.telephony.CellInfoWcdma
import android.telephony.CellInfoLte
import android.telephony.CellInfo
import com.example.networkcellanalyzer.model.MeasurementRequest
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// this function gives the format so that it matches fastAPI
fun getCurrentTimestamp(): String {
    return Instant.now().toString()
        .replace("T", " ")
        .replace("Z", "")
}

@SuppressLint("MissingPermission")
fun getFreshRegisteredCellBlocking(
    tm: TelephonyManager,
    context: Context
): CellInfo? {
    val cachedList = tm.allCellInfo
    val cachedCell = cachedList?.firstOrNull { it.isRegistered } ?: cachedList?.firstOrNull()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return cachedCell
    }

    var result: CellInfo? = null
    val latch = CountDownLatch(1)
    val executor = Executors.newSingleThreadExecutor()

    try {
        tm.requestCellInfoUpdate(
            executor,
            object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cellList: MutableList<CellInfo>) {
                    result = cellList.firstOrNull { it.isRegistered } ?: cellList.firstOrNull()
                    latch.countDown()
                }
            }
        )

        latch.await(1500, TimeUnit.MILLISECONDS)
    } catch (_: Exception) {
    } finally {
        executor.shutdown()
    }

    return result ?: cachedCell
}

@SuppressLint("MissingPermission")
fun readMeasurementFromPhone(context: Context, deviceId: String): MeasurementRequest? {

    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    var operator = tm.networkOperatorName
    if (operator.isEmpty()) operator = "Unknown"

    val cell = getFreshRegisteredCellBlocking(tm, context) ?: return null

    // 4G
    if (cell is CellInfoLte) {
        val signal = cell.cellSignalStrength
        var snr: Float? = null
        var band: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            snr = signal.rssnr.toFloat()
            band = cell.cellIdentity.bands.firstOrNull()
        }
        return MeasurementRequest(
            device_id = deviceId,
            operator = operator,
            signal_power = signal.dbm,
            SNR = snr,
            network_type = "4G",
            frequency_band = band,
            cell_id = cell.cellIdentity.ci.toString(),
            time_stamp = getCurrentTimestamp()
        )
    }
    // 3G
    else if (cell is CellInfoWcdma) {
        val signal = cell.cellSignalStrength

        return MeasurementRequest(
            device_id = deviceId,
            operator = operator,
            signal_power = signal.dbm,
            SNR = null,
            network_type = "3G",
            frequency_band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val uarfcn = cell.cellIdentity.uarfcn
                if (uarfcn != CellInfo.UNAVAILABLE) uarfcn else null
            } else null,
            cell_id = cell.cellIdentity.cid.toString(),
            time_stamp = getCurrentTimestamp()
        )
    }
    // 2G
    else if (cell is CellInfoGsm) {
        val signal = cell.cellSignalStrength
        return MeasurementRequest(
            device_id = deviceId,
            operator = operator,
            signal_power = signal.dbm,
            SNR = null,
            network_type = "2G",
            frequency_band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val arfcn = cell.cellIdentity.arfcn
                if (arfcn != CellInfo.UNAVAILABLE) arfcn else null
            } else null,
            cell_id = cell.cellIdentity.cid.toString(),
            time_stamp = getCurrentTimestamp()
        )
    }
    else {
        return null
    }
}
