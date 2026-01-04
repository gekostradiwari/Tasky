package com.android.tasky.utility

import com.android.tasky.dto.Dipendente
import com.squareup.moshi.Json

data class ListDipendentiResponse(
    @field:Json(name = "count") val count: Int,
    @field:Json(name = "items") val items: List<Dipendente>
)