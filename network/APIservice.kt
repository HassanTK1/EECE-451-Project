package com.example.a451_app.network
import com.example.a451_app.model.HealthResponse
import com.example.a451_app.model.IdentificationRequest
import com.example.a451_app.model.IdentificationResponse
import com.example.a451_app.model.MeasurementRequest
import com.example.a451_app.model.MeasurementResponse

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


interface APIservice{

    @GET("/health")
    fun  isHealthy(): Call<HealthResponse>

    @POST("/identification")
    fun identifyDevice(
        @Body body : IdentificationRequest
    ) : Call<IdentificationResponse>

    @POST("/measurements")
    fun sendMetrics(
        @Query("device_id") deviceId: String,
        @Body body: MeasurementRequest
    ): Call<MeasurementResponse>


}
