package com.smartjam.app.data.local

import android.content.Context
import java.io.File
import java.util.UUID

class AudioFileStore(context: Context) {
    private val baseDir: File = File(context.filesDir, "assignment_audio").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    fun getAssignmentAudioFile(assignmentId: UUID): File {
        return File(baseDir, "$assignmentId.wav")
    }

    fun getSubmissionAudioFile(submissionId: UUID): File {
        return File(baseDir, "submission_$submissionId.wav")
    }
}

