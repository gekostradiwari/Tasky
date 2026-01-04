package com.android.tasky.dto

import com.squareup.moshi.Json

//classe dati per quanto riguarda le entità dipendente

data class Dipendente(
    @field:Json(name = "Dipartimento_id_dipartimento") val Dipartimento_id_dipartimento: Int,
    @field:Json(name = "cognome") val cognome: String,
    @field:Json(name = "data_nascita") val data_nascita: String,
    @field:Json(name = "email") val email: String,
    @field:Json(name = "nome") val nome: String,
    @field:Json(name = "numero_telefono") val numero_telefono: String,
    @field:Json(name = "sesso") val sesso: String
    )