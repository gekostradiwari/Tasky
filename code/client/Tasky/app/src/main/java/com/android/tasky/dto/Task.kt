package com.android.tasky.dto

import java.sql.Date

//Classe dati per quanto riguarda le entità tasks

data class Task(val stato: String, val descrizione: String, val data_inizio: Date, val data_fine: Date)