package com.android.tasky.utility

import com.android.tasky.dto.Task
import com.squareup.moshi.Json

data class TaskAddData(
    @field:Json(name = "id_task") val id_task: Int
)

data class TaskAddResponse(
    @field:Json(name = "data") val data: TaskAddData,
    @field:Json(name = "message") val message: String
)
