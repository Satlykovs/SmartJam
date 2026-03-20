package com.smartjam.common.model;

/**
 * Represents the lifecycle stages of an audio processing task. Used to track the state from the moment a database
 * record is created until the final analysis result is stored.
 */
public enum AudioProcessingStatus {
    AWAITING_UPLOAD,
    UPLOADED,
    ANALYZING,
    COMPLETED,
    FAILED
}
