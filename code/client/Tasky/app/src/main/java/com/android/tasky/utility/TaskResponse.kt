package com.android.tasky.utility

import com.android.tasky.dto.Task
import com.squareup.moshi.Json


data class TaskResponse(
    @field:Json(name = "data") val data: Task,
    @field:Json(name = "message") val message: String
)