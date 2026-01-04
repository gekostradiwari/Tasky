package com.android.tasky.utility

import com.android.tasky.dto.Task
import com.squareup.moshi.Json

data class TaskData(
    @field:Json(name = "count") val count: Int,
    @field:Json(name = "items") val items: List<Task>
)

data class ListTaskResponse(
    @field:Json(name = "data") val data: TaskData,
    @field:Json(name = "message") val message: String
)