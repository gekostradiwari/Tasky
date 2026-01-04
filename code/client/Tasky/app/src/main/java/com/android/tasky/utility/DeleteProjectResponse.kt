package com.android.tasky.utility

import com.android.tasky.dto.Progetto
import com.squareup.moshi.Json

data class DeleteProjectResponse(
    @field:Json(name = "data") val data: Progetto,
    @field:Json(name = "message") val message: String

)
