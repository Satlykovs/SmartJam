package com.smartjam.smartjamanalyzer.domain.port;

import java.nio.file.Path;

import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;

/** Port for extracting musical features from an audio file. */
public interface FeatureExtractor {

    /**
     * Analyzes the audio file and produces a {@link FeatureSequence}
     *
     * @param audioFile Path to the local standardized audio file
     * @return Extracted musical features
     */
    FeatureSequence extract(Path audioFile);
}
