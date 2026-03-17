package com.smartjam.smartjamanalyzer.infrastructure.analysis;

import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "analyzer.dsp")
@Validated
record DspProperties(
        @Min(512) int bufferSize,
        @Min(256) int overlap,
        float minFreq,
        @Min(1) int octaves,
        @Min(12) int binsPerOctave) {}
