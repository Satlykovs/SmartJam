package com.smartjam.app.domain.repository

import android.util.Log
import com.smartjam.app.api.AssignmentsApi
import com.smartjam.app.api.SubmissionsApi
import com.smartjam.app.data.local.AudioFileStore
import com.smartjam.app.data.local.dao.AssignmentDao
import com.smartjam.app.data.local.dao.SubmissionResultDao
import com.smartjam.app.data.local.entity.AssignmentEntity
import com.smartjam.app.data.local.entity.SubmissionResultEntity
import com.smartjam.app.model.AssignmentResponseDetailed
import com.smartjam.app.model.AssignmentUploadResponse
import com.smartjam.app.model.CreateAssignmentRequest
import com.smartjam.app.model.SubmissionResultResponse
import com.smartjam.app.model.SubmissionUploadResponse
import java.io.File
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

class RoomRepository(
    private val assignmentsApi: AssignmentsApi,
    private val submissionsApi: SubmissionsApi,
    private val assignmentDao: AssignmentDao,
    private val submissionResultDao: SubmissionResultDao,
    private val audioFileStore: AudioFileStore,
) {

    data class AssignmentPageInfo(
        val pageNumber: Int,
        val totalPages: Int,
        val pageSize: Int,
        val totalElements: Long,
    )

    private val httpClient =
        OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()

    fun getAssignmentsFlow(connectionId: UUID): Flow<List<AssignmentEntity>> {
        return assignmentDao.getAssignmentsForConnection(connectionId)
    }

    fun getSubmissionsFlow(assignmentId: UUID): Flow<List<SubmissionResultEntity>> {
        return submissionResultDao.getResultsForAssignment(assignmentId)
    }

    suspend fun syncAssignmentsPage(
        connectionId: UUID,
        page: Int,
        size: Int,
    ): Result<AssignmentPageInfo> {
        return try {
            val response =
                assignmentsApi.getAssignmentsByConnection(connectionId, page = page, size = size)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val existing =
                    assignmentDao.getAssignmentsByIds(body.content.map { it.id }).associateBy {
                        it.id
                    }

                val entities =
                    body.content.map { dto ->
                        val cached = existing[dto.id]
                        AssignmentEntity(
                            id = dto.id,
                            connectionId = connectionId,
                            title = dto.title,
                            description = cached?.description,
                            referenceAudioUrl = cached?.referenceAudioUrl,
                            referenceAudioLocalPath = cached?.referenceAudioLocalPath,
                            status = dto.status.name,
                            createdAt = dto.createdAt,
                        )
                    }
                assignmentDao.insertAll(entities)

                Result.success(
                    AssignmentPageInfo(
                        pageNumber = body.page.number,
                        totalPages = body.page.totalPages,
                        pageSize = body.page.propertySize,
                        totalElements = body.page.totalElements,
                    )
                )
            } else {
                Result.failure(Exception("Failed to fetch assignments: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncAssignments(connectionId: UUID): Result<Unit> {
        return syncAssignmentsPage(connectionId, page = 0, size = 20).map {}
    }

    suspend fun ensureAssignmentDetailsCached(assignmentId: UUID): Result<AssignmentEntity> {
        return try {
            val existing =
                assignmentDao.getAssignmentById(assignmentId)
                    ?: return Result.failure(Exception("Assignment not found in cache"))

            val response = assignmentsApi.getAssignment(assignmentId)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                Log.d(
                    "RoomRepository",
                    "Fetched assignment details: id=${dto.id} description=${dto.description?.take(100)} referenceAudioUrl=${dto.referenceAudioUrl}",
                )
                val localPath = cacheReferenceAudioIfNeeded(existing, dto)
                Log.d(
                    "RoomRepository",
                    "Reference audio localPath for assignment ${existing.id}: $localPath",
                )
                val updated =
                    existing.copy(
                        title = dto.title,
                        description = dto.description,
                        referenceAudioUrl = dto.referenceAudioUrl,
                        referenceAudioLocalPath = localPath,
                        status = dto.status.name,
                    )
                assignmentDao.insertAll(listOf(updated))
                Result.success(updated)
            } else {
                Result.failure(Exception("Failed to fetch detailed assignment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAssignment(
        request: CreateAssignmentRequest
    ): Result<AssignmentUploadResponse> {
        return try {
            val response = assignmentsApi.createAssignment(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.w(
                    "RoomRepository",
                    "createAssignment failed: code=${response.code()} body=$errorBody",
                )
                Result.failure(Exception("Failed to create assignment: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w("RoomRepository", "createAssignment exception", e)
            Result.failure(e)
        }
    }

    suspend fun syncSubmissions(assignmentId: UUID): Result<Unit> {
        return try {
            val response = submissionsApi.getSubmissionsByAssignment(assignmentId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val existing =
                    submissionResultDao.getResultsForAssignmentOnce(assignmentId).associateBy {
                        it.id
                    }

                val entities =
                    body.content.map { dto ->
                        val cached = existing[dto.id]
                        SubmissionResultEntity(
                            id = dto.id,
                            assignmentId = assignmentId,
                            status = dto.status.name,
                            totalScore = dto.totalScore?.toFloat(),
                            pitchScore = cached?.pitchScore,
                            rhythmScore = cached?.rhythmScore,
                            errorMessage = cached?.errorMessage,
                            fileUrl = cached?.fileUrl,
                            submissionAudioLocalPath = cached?.submissionAudioLocalPath,
                            createdAt = dto.createdAt,
                        )
                    }
                submissionResultDao.insertAll(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch submissions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSubmission(assignmentId: UUID): Result<SubmissionUploadResponse> {
        return try {
            val response = submissionsApi.createSubmission(assignmentId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.w(
                    "RoomRepository",
                    "createSubmission failed: code=${response.code()} body=$errorBody",
                )
                Result.failure(Exception("Failed to create submission"))
            }
        } catch (e: Exception) {
            Log.w("RoomRepository", "createSubmission exception", e)
            Result.failure(e)
        }
    }

    suspend fun getSubmissionResult(
        submissionId: UUID,
        assignmentId: UUID,
    ): Result<SubmissionResultResponse> {
        return try {
            val response = submissionsApi.getSubmissionResult(submissionId)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val created = java.time.Instant.now()
                val existing =
                    submissionResultDao
                        .getResultsForAssignmentOnce(assignmentId)
                        .associateBy { it.id }[dto.id]
                submissionResultDao.insertAll(
                    listOf(
                        SubmissionResultEntity(
                            id = dto.id,
                            assignmentId = assignmentId,
                            status = dto.status.name,
                            totalScore = dto.totalScore?.toFloat(),
                            pitchScore = dto.pitchScore?.toFloat(),
                            rhythmScore = dto.rhythmScore?.toFloat(),
                            errorMessage = dto.errorMessage,
                            fileUrl = dto.submissionAudioUrl?.toString(),
                            submissionAudioLocalPath = existing?.submissionAudioLocalPath,
                            createdAt = created,
                        )
                    )
                )
                Result.success(dto)
            } else {
                Result.failure(Exception("Failed to fetch submission result"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cacheSubmissionAudioIfNeeded(
        submissionId: UUID,
        assignmentId: UUID,
        urlString: String?,
    ): Result<String?> =
        withContext(Dispatchers.IO) {
            try {
                if (urlString.isNullOrBlank()) {
                    return@withContext Result.success(null)
                }

                val existing =
                    submissionResultDao
                        .getResultsForAssignmentOnce(assignmentId)
                        .associateBy { it.id }[submissionId]
                val existingPath = existing?.submissionAudioLocalPath
                if (!existingPath.isNullOrBlank()) {
                    val f = java.io.File(existingPath)
                    if (f.exists()) return@withContext Result.success(existingPath)
                }
                val uri = java.net.URI(urlString)
                val originalHost = if (uri.port == -1) uri.host else "${uri.host}:${uri.port}"

                val fixedUrl =
                    urlString.replace("localhost", "10.0.2.2").replace("127.0.0.1", "10.0.2.2")

                val target = audioFileStore.getSubmissionAudioFile(submissionId)
                val request = Request.Builder().url(fixedUrl).header("Host", originalHost).build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.success(null)
                    }

                    response.body?.byteStream()?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }

                val updated =
                    SubmissionResultEntity(
                        id = submissionId,
                        assignmentId = assignmentId,
                        status = existing?.status ?: "",
                        totalScore = existing?.totalScore,
                        pitchScore = existing?.pitchScore,
                        rhythmScore = existing?.rhythmScore,
                        errorMessage = existing?.errorMessage,
                        fileUrl = existing?.fileUrl,
                        submissionAudioLocalPath = target.absolutePath,
                        createdAt = existing?.createdAt ?: java.time.Instant.now(),
                    )
                submissionResultDao.insertAll(listOf(updated))
                Result.success(target.absolutePath)
            } catch (e: Exception) {
                Log.w("RoomRepository", "cacheSubmissionAudioIfNeeded exception", e)
                Result.failure(e)
            }
        }

    suspend fun uploadFileToS3(uploadUrl: String, file: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val uri = java.net.URI(uploadUrl)
                val originalHost = if (uri.port == -1) uri.host else "${uri.host}:${uri.port}"

                val fixedUrl =
                    uploadUrl
                        .replace("localhost", "10.0.2.2")
                        .replace("127.0.0.1", "10.0.2.2")
                        .replace("references.localhost", "10.0.2.2")

                val requestBody = file.asRequestBody(null)
                val request =
                    Request.Builder()
                        .url(fixedUrl)
                        .header("Host", originalHost)
                        .put(requestBody)
                        .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        val errorBody = response.body?.string()
                        Log.w(
                            "RoomRepository",
                            "uploadFileToS3 failed: code=${response.code} body=$errorBody",
                        )
                        Result.failure(Exception("S3 upload failed: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Log.w("RoomRepository", "uploadFileToS3 exception", e)
                Result.failure(e)
            }
        }

    private suspend fun cacheReferenceAudioIfNeeded(
        existing: AssignmentEntity,
        dto: AssignmentResponseDetailed,
    ): String? =
        withContext(Dispatchers.IO) {
            val url =
                dto.referenceAudioUrl.toString().takeIf { it.isNotBlank() }
                    ?: return@withContext existing.referenceAudioLocalPath

            val existingPath = existing.referenceAudioLocalPath
            if (!existingPath.isNullOrBlank()) {
                val file = File(existingPath)
                if (file.exists()) {
                    return@withContext existingPath
                }
            }

            val target = audioFileStore.getAssignmentAudioFile(existing.id)

            val uri = java.net.URI(url)
            val originalHost = if (uri.port == -1) uri.host else "${uri.host}:${uri.port}"
            val fixedUrl = url.replace("localhost", "10.0.2.2").replace("127.0.0.1", "10.0.2.2")

            val request = Request.Builder().url(fixedUrl).header("Host", originalHost).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext existing.referenceAudioLocalPath
                }

                response.body?.byteStream()?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }

            target.absolutePath
        }
}
