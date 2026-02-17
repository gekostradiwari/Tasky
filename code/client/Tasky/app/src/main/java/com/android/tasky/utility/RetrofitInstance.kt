package com.android.tasky.utility

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object RetrofitInstance {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val baseUrl = "https://tasky.bytethecookies.org/api/" //Impostare l'url del server dove sono hostate le API
    val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    val api: RetrofitInterface by lazy{
        Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(RetrofitInterface::class.java)
    }
}