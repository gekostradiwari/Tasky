package com.android.tasky.utility

import com.android.tasky.dto.Progetto
import com.squareup.moshi.Json

data class ProjectDataDepartment(
    @field:Json(name = "count") val count: Int,
    @field:Json(name = "items") val items: List<Progetto>,
    @field:Json(name = "scope") val scope: String
)

data class ListProjectByDepartmentResponse(
    @field:Json(name = "data") val data: ProjectDataDepartment,
    @field:Json(name = "message") val message: String
)
