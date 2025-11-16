package com.android.tasky.utility

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RetrofitInterface {
    @POST("login")
    suspend fun login(@Body body: Map<String, String?>): Response<LoginResponse>


}