package com.android.tasky.utility

import com.squareup.moshi.Json

data class ProjectAddData(
    @field:Json(name = "id_progetto") val id_progetto: Int
)

data class ProjectAddResponse(
    @field:Json(name = "data") val data: ProjectAddData,
    @field:Json(name = "message") val message: String
)
