package com.smartjam.smartjamanalyzer.infrastructure.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;

/**
 * Binary serializer for spectral feature matrices. Storage format: [int:N frames][int:M bins][float: framerate][N * M
 * raw floats]
 */
public class FeatureBinarySerializer {

    private static final int HEADER_SIZE = 12;

    private FeatureBinarySerializer() {
        // utility class, no need to be constructed
    }

    /**
     * Serializes a list of equally sized float frames into a byte array.
     *
     * @param sequence list of frames (each frame is an array of floats)
     * @return byte array with little-endian representation
     * @throws NullPointerException if sequence is null.
     * @throws IllegalArgumentException if frames are null, empty, or frames have different lengths
     */
    public static byte[] serialize(FeatureSequence sequence) {

        Objects.requireNonNull(sequence, "Feature sequence must not be null");

        if (sequence.frames().isEmpty()) {
            return new byte[0];
        }

        List<float[]> frames = sequence.frames();
        int frameCount = frames.size();
        int binCount = frames.getFirst().length;
        float frameRate = sequence.frameRate();

        for (var frame : frames) {
            if (frame.length != binCount) {
                throw new IllegalArgumentException("All frames must have the same number of bins. Expected " + binCount
                        + ", but got " + frame.length);
            }
        }

        long totalPayloadBytes = (long) frameCount * binCount * Float.BYTES;
        if (totalPayloadBytes > Integer.MAX_VALUE - HEADER_SIZE) {
            throw new IllegalArgumentException("Matrix is too large for binary serialization");
        }

        int totalBytes = HEADER_SIZE + (int) totalPayloadBytes;

        ByteBuffer buffer = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN);

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
     * Deserializes a byte array back to a list of frames.
     *
     * @param data byte array produced by {@link #serialize(FeatureSequence)}
     * @return FeatureSequence, or null if data is null or too short
     * @throws IllegalArgumentException if the data is malformed (e.g., negative sizes, insufficient length)
     */
    public static FeatureSequence deserialize(byte[] data) {
        if (data == null || data.length < HEADER_SIZE) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int frameCount = buffer.getInt();
        int binCount = buffer.getInt();
        float frameRate = buffer.getFloat();

        if (frameCount < 0 || binCount < 0) {
            throw new IllegalArgumentException("Invalid header: frameCount or binCount is negative");
        }

        long totalElements = (long) frameCount * binCount;
        if (totalElements > Integer.MAX_VALUE / Float.BYTES) {
            throw new IllegalArgumentException("Too many elements to deserialize: " + totalElements);
        }

        int expectedBytes = HEADER_SIZE + frameCount * binCount * Float.BYTES;

        if (data.length < expectedBytes) {
            throw new IllegalArgumentException(
                    "Data too short. Expected " + expectedBytes + " bytes, got " + data.length);
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
