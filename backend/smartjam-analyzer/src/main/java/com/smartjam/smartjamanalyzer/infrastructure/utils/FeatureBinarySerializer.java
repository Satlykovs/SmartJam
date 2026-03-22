package com.smartjam.smartjamanalyzer.infrastructure.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;

/**
 * High-performance binary serializer for spectral feature matrices. Storage format (Little-Endian): [0-3 bytes] - int:
 * frameCount (N) [4-7 bytes] - int: binCount (M) [8-11 bytes] - float: frameRate [12+ bytes] - N * M raw floats
 */
public class FeatureBinarySerializer {

    private static final int HEADER_SIZE = 12;

    private FeatureBinarySerializer() {
        // utility class, no need to be constructed
    }

    /**
     * Serializes a {@link FeatureSequence} into a byte array.
     *
     * @param sequence the sequence to serialize. Must not be null.
     * @return little-endian byte array representation.
     * @throws NullPointerException if sequence is null.
     * @throws IllegalArgumentException if the resulting buffer size exceeds Integer.MAX_VALUE.
     */
    public static byte[] serialize(FeatureSequence sequence) {

        Objects.requireNonNull(sequence, "Feature sequence must not be null");

        List<float[]> frames = sequence.frames();
        int frameCount = frames.size(); // Формально, не может быть = 0
        int binCount = sequence.binCount();
        float frameRate = sequence.frameRate();

        for (var frame : frames) {
            if (frame.length != binCount) {
                throw new IllegalArgumentException("All frames must have the same number of bins. Expected " + binCount
                        + ", but got " + frame.length);
            }
        }

        long payloadBytes;
        try {
            long totalElements = (long) frameCount * binCount;
            payloadBytes = Math.multiplyExact(totalElements, (long) Float.BYTES);
            if (payloadBytes > Integer.MAX_VALUE - HEADER_SIZE) {
                throw new ArithmeticException();
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Feature matrix is too large for binary serialization");
        }

        ByteBuffer buffer =
                ByteBuffer.allocate(HEADER_SIZE + (int) payloadBytes).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(frameCount);
        buffer.putInt(binCount);
        buffer.putFloat(frameRate);

        for (float[] frame : frames) {
            for (float val : frame) {
                buffer.putFloat(val);
            }
        }

        return buffer.array();
    }

    /**
     * Deserializes a byte array back to a {@link FeatureSequence}.
     *
     * @param data byte array produced by {@link #serialize(FeatureSequence)}.
     * @return a FeatureSequence instance, or null if data is null or shorter than header.
     * @throws IllegalArgumentException if the data is malformed (e.g., non-positive counts or truncated data).
     */
    public static FeatureSequence deserialize(byte[] data) {
        if (data == null || data.length < HEADER_SIZE) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int frameCount = buffer.getInt();
        int binCount = buffer.getInt();
        float frameRate = buffer.getFloat();

        if (frameCount <= 0 || binCount <= 0) { // Хотя, даже без проверки здесь, объект не
            // сконструируется
            throw new IllegalArgumentException("Invalid binary header: sequence cannot be empty");
        }
        if (frameRate <= 0) {
            throw new IllegalArgumentException("Invalid binary header: frameRate must be positive");
        }

        long expectedPayload;
        try {
            expectedPayload = Math.multiplyExact((long) frameCount * binCount, (long) Float.BYTES);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Invalid header: dimensions cause integer overflow");
        }

        if (data.length < HEADER_SIZE + expectedPayload) {
            throw new IllegalArgumentException(
                    "Binary data truncated. Expected " + (HEADER_SIZE + expectedPayload) + " bytes");
        }

        List<float[]> frames = new ArrayList<>(frameCount);
        for (int i = 0; i < frameCount; ++i) {
            float[] frame = new float[binCount];
            for (int j = 0; j < binCount; ++j) {
                frame[j] = buffer.getFloat();
            }
            frames.add(frame);
        }
        return new FeatureSequence(frames, frameRate);
    }
}
