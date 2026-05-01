package com.smartjam.app.data.api

import com.smartjam.app.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SmartJamApi {
    @POST("/api/connections/invite-code")
    suspend fun generateInviteCode(): Response<InviteCodeResponse>

    @POST("/api/connections/join")
    suspend fun joinByCode(@Body request: JoinRequest): Response<Unit>

    @GET("/api/connections/pending")
    suspend fun getPendingConnections(): Response<List<ConnectionDto>>

    @GET("/api/connections/active")
    suspend fun getActiveConnections(): Response<List<ConnectionDto>>

    @POST("/api/connections/{connectionId}/respond")
    suspend fun respondToConnection(
        @Path("connectionId") connectionId: String,
        @Body request: RespondConnectionRequest
    ): Response<Unit>

    @POST("/api/assignments")
    suspend fun createAssignment(
        @Body request: CreateAssignmentRequest
    ): Response<PresignedUrlResponse>

    @POST("/api/submissions")
    suspend fun createSubmission(
        @Body request: CreateSubmissionRequest
    ): Response<PresignedUrlResponse>

    @GET("/api/submissions/{submissionId}/status")
    suspend fun getSubmissionStatus(
        @Path("submissionId") submissionId: String
    ): Response<SubmissionStatusResponse>

}