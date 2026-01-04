package com.android.tasky.utility

import com.android.tasky.dto.Progetto
import com.android.tasky.dto.Task
import com.squareup.moshi.Json

data class ProjectData(
    @field:Json(name = "count") val count: Int,
    @field:Json(name = "items") val items: List<Progetto>
)

data class ListProjectByMGRResponse (
    @field:Json(name = "data") val data: ProjectData,
    @field:Json(name = "message") val message: String
)