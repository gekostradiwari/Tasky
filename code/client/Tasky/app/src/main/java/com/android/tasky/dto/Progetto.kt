package com.android.tasky.dto

import java.sql.Date

data class Progetto(val descrizione: String, val budget: Double, val nome: String, val dataInizio: Date, val dataFine: Date, val id_dipartimento: Int)