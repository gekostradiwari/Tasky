package com.android.tasky.utility

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitInstance {
    val baseUrl = "http://192.168.1.27:5001/api/" //Impostare l'url del server dove sono hostate le API
    val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    fun getRetrofitInstance(): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(MoshiConverterFactory.create()).build()
    }
}