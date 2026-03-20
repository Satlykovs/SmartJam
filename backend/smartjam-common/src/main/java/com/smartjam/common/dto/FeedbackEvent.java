package com.smartjam.common.dto;

import com.smartjam.common.model.FeedbackType;

/**
 * A shared contract for a single feedback occurrence. Used by Analyzer to produce results and by API/Mobile to display
 * them.
 */
public record FeedbackEvent(
        double teacherStartTime,
        double teacherEndTime,
        double studentStartTime,
        double studentEndTime,
        FeedbackType type,
        double severity) {}
