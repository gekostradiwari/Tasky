package com.android.tasky.utility

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RetrofitInterface {
    @POST("login")
    suspend fun login(@Body body: Map<String, String?>): Response<LoginResponse>

    @POST("tasks/in-progress")
    suspend fun TaskLister(@Body body: Map<String, String?>): Response<ListTaskResponse>

    @POST("tasks/completed")
    suspend fun TaskListerCompleted(@Body body: Map<String, String?>): Response<ListTaskResponse>

    @POST("tasks/suspended")
    suspend fun TaskListerSuspended(@Body body: Map<String, String?>): Response<ListTaskResponse>

    @POST("update/Task/Status")
    suspend fun updateTask(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<TaskResponse>

    @POST("dipendenti/data/by-department")
    suspend fun dipendentiByDepartment(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ListDipendentiResponse>

    @POST("update/Task")
    suspend fun updateTaskMGR(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ListTaskResponse>

    @POST("project/by-department")
    suspend fun getProjectByDepartment(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ListProjectByDepartmentResponse>

    @POST("task/by-project")
    suspend fun getTaskByProjectMGR(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ListTaskResponse>

    @POST("delete/Task")
    suspend fun deleteTask(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ListTaskResponse>

    @POST("delete/Project")
    suspend fun deleteProject(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<DeleteProjectResponse>

    @POST("add/Task")
    suspend fun addTask(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<TaskAddResponse>

    @POST("add/Project")
    suspend fun addProject(@Body body: @JvmSuppressWildcards Map<String, Any>): Response<ProjectAddResponse>

    @POST("projects/by-manager")
    suspend fun getProjectsByMGR(@Body body: Map<String, String>): Response<ListProjectByMGRResponse>






}