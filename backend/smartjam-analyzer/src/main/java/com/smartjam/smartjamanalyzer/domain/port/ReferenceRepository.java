package com.smartjam.smartjamanalyzer.domain.port;

import java.util.UUID;

import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;

public interface ReferenceRepository {
    void save(UUID targetId, FeatureSequence features);

    FeatureSequence findById(UUID targetId);
}
