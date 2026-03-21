package com.example.sample

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface CloudSyncApi {
    // Targets the @app.route('/sync_to_cloud') endpoint we will build in Python
    @POST("sync_to_cloud")
    fun pushDataToCloud(@Body payload: CloudSyncPayload): Call<SyncResponse>

    // Add inside your existing Retrofit Interface
    @POST("login_and_sync")
    fun loginUser(@Body credentials: LoginCredentials): Call<LoginResponse>
}
