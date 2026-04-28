package com.example.a451_app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.example.a451_app.model.MeasurementRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
fun getCurrentLocationBlocking(context: Context): Location? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    var result: Location? = null
    val latch = CountDownLatch(1)

    // Create a background looper for the callback to run on
    val handlerThread = android.os.HandlerThread("LocationThread")
    handlerThread.start()
    val looper = handlerThread.looper
    val executor = java.util.concurrent.Executors.newSingleThreadExecutor()

    // Try lastLocation first
    client.lastLocation
        .addOnSuccessListener(executor, { loc ->
            if (loc != null) result = loc
            latch.countDown()
        })
        .addOnFailureListener(executor, {
            latch.countDown()
        })

    latch.await(3000, TimeUnit.MILLISECONDS)

    if (result != null) {
        handlerThread.quitSafely()
        return result
    }

    // Fresh fix if no cache
    val freshLatch = CountDownLatch(1)
    val cts = CancellationTokenSource()

    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
        .addOnSuccessListener(executor, { loc ->
            result = loc
            freshLatch.countDown()
        })
        .addOnFailureListener(executor, {
            freshLatch.countDown()
        })

    freshLatch.await(5000, TimeUnit.MILLISECONDS)
    handlerThread.quitSafely()
    return result
}

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

@RequiresApi(Build.VERSION_CODES.S)
private fun get5GBand(identity: CellIdentityNr): Int? {
    return identity.bands.firstOrNull()
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun build5GMeasurement(
    cell: CellInfoNr,
    deviceId: String,
    operator: String,
    lat: Double?,
    lon: Double?
): MeasurementRequest {
    val signal = cell.cellSignalStrength as CellSignalStrengthNr
    val identity = cell.cellIdentity as CellIdentityNr

    var snr: Float? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val sinr = signal.csiSinr
        snr = if (sinr != CellInfo.UNAVAILABLE) sinr.toFloat() else null
    }

    var band: Int? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        band = get5GBand(identity)
    }

    return MeasurementRequest(
        device_id = deviceId,
        operator = operator,
        signal_power = signal.dbm,
        SNR = snr,
        network_type = "5G",
        frequency_band = band,
        cell_id = identity.nci.toString(),
        time_stamp = getCurrentTimestamp(),
        latitude = lat,
        longitude = lon
    )
}

@SuppressLint("MissingPermission")
fun readMeasurementFromPhone(context: Context, deviceId: String): MeasurementRequest? {

    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    var operator = tm.networkOperatorName
    if (operator.isEmpty()) operator = "Unknown"

    val cell = getFreshRegisteredCellBlocking(tm, context) ?: return null

    val location = getCurrentLocationBlocking(context)
    val lat = location?.latitude
    val lon = location?.longitude
    android.util.Log.d("LOC_DEBUG", "Got location: lat=$lat, lon=$lon")

    // 5G — API 29+ only
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell is CellInfoNr) {
        return build5GMeasurement(cell, deviceId, operator, lat, lon)
    }

    // 4G
    if (cell is CellInfoLte) {
        val signal = cell.cellSignalStrength
        var snr: Float? = null
        var band: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val rssnr = signal.rssnr
            snr = if (rssnr != CellInfo.UNAVAILABLE) rssnr.toFloat() else null
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
            time_stamp = getCurrentTimestamp(),
            latitude = lat,
            longitude = lon
        )
    }

    // 3G
    if (cell is CellInfoWcdma) {
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
            time_stamp = getCurrentTimestamp(),
            latitude = lat,
            longitude = lon
        )
    }

    // 2G
    if (cell is CellInfoGsm) {
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
            time_stamp = getCurrentTimestamp(),
            latitude = lat,
            longitude = lon
        )
    }

    return null
}
