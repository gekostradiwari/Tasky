package com.android.tasky.utility

import com.squareup.moshi.Json

data class UserData(
    @field:Json(name = "email") val email: String,
    @field:Json(name = "token") val token: String,
    @field:Json(name = "type") val type: String,
    @field:Json(name = "id_dipartimento") val id_dipartimento: Int,
    @field:Json(name = "sesso") val sesso: String,
    @field:Json(name = "nome_dipartimento") val nome_dipartimento: String,
    @field:Json(name = "name") val name: String
)
data class LoginResponse(
    @field:Json(name = "data") val data: UserData,
    @field:Json(name = "message") val message: String
)
