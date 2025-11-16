package com.android.tasky.utility

import com.squareup.moshi.Json

data class UserData(
    @Json(name = "token") val token: String,
    @Json(name = "type") val type: String,
    @Json(name = "id_dipartimento") val dipartimento: Int,
    @Json(name = "sesso") val sesso: String
)
data class LoginResponse(
    @Json(name = "data") val data: UserData,
    @Json(name = "message") val message: String
)
