package com.android.tasky.dto

import com.squareup.moshi.Json
import java.sql.Date

data class Progetto(
    @field:Json(name = "Dipartimento_id_dipartimento") val Dipartimento_id_dipartimento: Int,
    @field:Json(name = "budgetIstanziato") val budgetIstanziato: String,
    @field:Json(name = "dataFine") val dataFine: String,
    @field:Json(name = "dataInizio") val dataInizio: String,
    @field:Json(name = "descrizione") val descrizione: String,
    @field:Json(name = "id_progetto") val id_progetto: Int,
    @field:Json(name = "nome") val nome: String,
)