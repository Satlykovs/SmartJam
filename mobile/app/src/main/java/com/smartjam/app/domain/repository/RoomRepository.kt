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
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.File
import java.time.Instant
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

@Singleton
class RoomRepository
@Inject
constructor(
    private val assignmentsApi: AssignmentsApi,
    private val submissionsApi: SubmissionsApi,
    private val assignmentDao: AssignmentDao,
    private val submissionResultDao: SubmissionResultDao,
    private val audioFileStore: AudioFileStore,
    private val httpClient: okhttp3.OkHttpClient,
) {

    data class AssignmentPageInfo(
        val pageNumber: Int,
        val totalPages: Int,
        val pageSize: Int,
        val totalElements: Long,
    )

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

                val localPath = cacheReferenceAudioIfNeeded(existing, dto)

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
                Result.failure(Exception("Failed to create assignment: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncSubmissions(assignmentId: UUID): Result<Unit> {
        return try {
            val response = submissionsApi.getSubmissionsByAssignment(assignmentId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val existingMap =
                    submissionResultDao.getResultsForAssignmentOnce(assignmentId).associateBy {
                        it.id
                    }
                val entities =
                    body.content.map { dto ->
                        val cached = existingMap[dto.id]
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
                            teacherWaveform = cached?.teacherWaveform,
                            studentWaveform = cached?.studentWaveform,
                            analysisFeedback = cached?.analysisFeedback,
                            createdAt = dto.createdAt,
                        )
                    }
                submissionResultDao.insertAll(entities)
                Result.success(Unit)
            } else Result.failure(Exception("Sync Error"))
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
                Result.failure(Exception("Failed to create submission"))
            }
        } catch (e: Exception) {
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
                val existing =
                    submissionResultDao.getResultsForAssignmentOnce(assignmentId).find {
                        it.id == dto.id
                    }
                val entity =
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
                        teacherWaveform = dto.teacherWaveform,
                        studentWaveform = dto.studentWaveform,
                        analysisFeedback = dto.feedback,
                        createdAt = existing?.createdAt ?: Instant.now(),
                    )
                submissionResultDao.insertAll(listOf(entity))
                Result.success(dto)
            } else Result.failure(Exception("Fetch Error"))
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
                if (urlString.isNullOrBlank()) return@withContext Result.success(null)
                val existing =
                    submissionResultDao.getResultsForAssignmentOnce(assignmentId).find {
                        it.id == submissionId
                    }
                val existingPath = existing?.submissionAudioLocalPath
                if (!existingPath.isNullOrBlank() && File(existingPath).exists())
                    return@withContext Result.success(existingPath)

                val target = audioFileStore.getSubmissionAudioFile(submissionId)
                val request = Request.Builder().url(urlString).build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext Result.failure<String?>(Exception("Download failed"))
                    response.body?.byteStream()?.use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    }

                    val updated =
                        existing?.copy(submissionAudioLocalPath = target.absolutePath)
                            ?: SubmissionResultEntity(
                                id = submissionId,
                                assignmentId = assignmentId,
                                status = "DOWNLOADED",
                                totalScore = null,
                                pitchScore = null,
                                rhythmScore = null,
                                errorMessage = null,
                                fileUrl = urlString,
                                submissionAudioLocalPath = target.absolutePath,
                                teacherWaveform = null,
                                studentWaveform = null,
                                analysisFeedback = null,
                                createdAt = Instant.now(),
                            )

                    submissionResultDao.insertAll(listOf(updated))
                    Result.success(target.absolutePath)
                }
            } catch (e: Exception) {
                Result.failure<String?>(e)
            }
        }

    suspend fun uploadFileToS3(uploadUrl: String, file: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(uploadUrl).put(file.asRequestBody(null)).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) Result.success(Unit)
                    else Result.failure(Exception("S3 upload failed: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun cacheReferenceAudioIfNeeded(
        existing: AssignmentEntity,
        dto: AssignmentResponseDetailed,
    ): String? =
        withContext(Dispatchers.IO) {
            val url =
                dto.referenceAudioUrl?.toString()?.takeIf { it.isNotBlank() }
                    ?: return@withContext existing.referenceAudioLocalPath

            val existingPath = existing.referenceAudioLocalPath
            if (!existingPath.isNullOrBlank() && File(existingPath).exists())
                return@withContext existingPath

            val target = audioFileStore.getAssignmentAudioFile(existing.id)

            val request = Request.Builder().url(url).build()
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use { input ->
                            target.outputStream().use { output -> input.copyTo(output) }
                        }
                        return@withContext target.absolutePath
                    }
                }
            } catch (e: Exception) {
                Log.e("RoomRepository", "Error caching reference", e)
            }
            existing.referenceAudioLocalPath
        }

    suspend fun getAssignment(assignmentId: UUID): AssignmentEntity? =
        withContext(Dispatchers.IO) { assignmentDao.getAssignmentById(assignmentId) }

    suspend fun getSubmissionEntity(
        assignmentId: UUID,
        submissionId: UUID,
    ): SubmissionResultEntity? =
        withContext(Dispatchers.IO) {
            submissionResultDao.getResultsForAssignmentOnce(assignmentId).find {
                it.id == submissionId
            }
        }
}
