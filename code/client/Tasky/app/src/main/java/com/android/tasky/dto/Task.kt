package com.android.tasky.dto

import com.squareup.moshi.Json
import java.sql.Date

//Classe dati per quanto riguarda le entità tasks

data class Task(
    @field:Json(name = "Dipendente_email") val Dipendente_email: String,
    @field:Json(name = "Manager_email") val Manager_email: String,
    @field:Json(name = "Progetto_id_progetto") val Progetto_id_progetto: Int,
    @field:Json(name = "data_fine") val data_fine: String,
    @field:Json(name = "data_inizio") val data_inizio: String,
    @field:Json(name = "descrizione") val descrizione: String,
    @field:Json(name = "id") val id: Int,
    @field:Json(name = "nome") val nome:String,
    @field:Json(name = "stato") var stato: String,
    @field:Json(name = "nome_progetto") var nome_progetto: String?
)