package com.smartjam.smartjamanalyzer.infrastructure.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureBinarySerializerTest {

    private static final float TEST_FRAME_RATE = 21.53f;

    @Test
    @DisplayName("Сериализация и десериализация сохраняет данные")
    void shouldMaintainDataIntegrity() {

        float[] frame1 = {0.1f, 0.5f, 0.9f};
        float[] frame2 = {0.2f, 0.4f, 0.6f};
        FeatureSequence original = new FeatureSequence(List.of(frame1, frame2), TEST_FRAME_RATE);

        byte[] encoded = FeatureBinarySerializer.serialize(original);
        assertNotNull(encoded);
        assertTrue(encoded.length > 8);
        assertEquals(36, encoded.length);

        FeatureSequence decoded = FeatureBinarySerializer.deserialize(encoded);

        assertEquals(original.frameRate(), decoded.frameRate(), 1e-6f);
        assertEquals(original.frames().size(), decoded.frames().size());
        assertEquals(original.frames().getFirst().length, decoded.frames().getFirst().length);

        for (int i = 0; i < original.frames().size(); i++) {
            assertArrayEquals(original.frames().get(i), decoded.frames().get(i), 1e-6f);
        }
    }

    @Test
    @DisplayName("Сериализация с null вызывает NullPointerException")
    void serializeNullThrows() {

        assertThrows(NullPointerException.class, () -> FeatureBinarySerializer.serialize(null));
    }

    @Test
    @DisplayName("Десериализация слишком короткого массива возвращает пустой список")
    void deserializeTooShortReturnsEmpty() {

        byte[] shortData = new byte[4];
        assertNull(FeatureBinarySerializer.deserialize(shortData));
    }

    @Test
    @DisplayName("Сериализация с фреймами разной длины вызывает исключение")
    void serializeFramesWithDifferentLengthsThrows() {
        // Технически, исключение вылетает вообще из конструктора FeatureSequence
        assertThrows(
                IllegalArgumentException.class,
                () -> FeatureBinarySerializer.serialize(new FeatureSequence(
                        List.of(new float[] {1.0f, 2.0f}, new float[] {3.0f, 4.0f, 5.0f}), TEST_FRAME_RATE)));
    }

    @Test
    @DisplayName("Десериализация с отрицательным frameCount вызывает исключение")
    void deserializeNegativeFrameCountThrows() {

        ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(-1);
        buffer.putInt(10);
        byte[] data = buffer.array();

        assertThrows(IllegalArgumentException.class, () -> FeatureBinarySerializer.deserialize(data));
    }

    @Test
    @DisplayName("Десериализация с отрицательным binCount вызывает исключение")
    void deserializeNegativeBinCountThrows() {

        ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(5);
        buffer.putInt(-3);
        byte[] data = buffer.array();

        assertThrows(IllegalArgumentException.class, () -> FeatureBinarySerializer.deserialize(data));
    }

    @Test
    @DisplayName("Десериализация с недостаточным количеством данных вызывает исключение")
    void deserializeInsufficientDataThrows() {

        ByteBuffer buffer = ByteBuffer.allocate(8 + Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(1);
        buffer.putInt(2);
        buffer.putFloat(1.0f);
        byte[] data = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, data, 0, data.length);

        assertThrows(IllegalArgumentException.class, () -> FeatureBinarySerializer.deserialize(data));
    }

    @Test
    @DisplayName("Проверка little-endian порядка байтов")
    void littleEndianOrder() {

        List<float[]> frames = List.of(new float[] {1.0f});
        FeatureSequence seq = new FeatureSequence(frames, TEST_FRAME_RATE);
        byte[] encoded = FeatureBinarySerializer.serialize(seq);

        ByteBuffer buffer = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(1, buffer.getInt(), "Frame count mismatch");
        assertEquals(1, buffer.getInt(), "Bin count mismatch");
        assertEquals(TEST_FRAME_RATE, buffer.getFloat(), 1e-6f, "Frame rate mismatch");
        assertEquals(1.0f, buffer.getFloat(), 1e-6f, "First data value mismatch");
    }

    @Test
    @DisplayName("Работа с большим количеством данных (3000 фреймов по 100 бинов)")
    void largeData() {

        int framesCount = 3000;
        int bins = 100;

        List<float[]> frames = new ArrayList<>(framesCount);

        for (int i = 0; i < framesCount; i++) {
            float[] frame = new float[bins];
            for (int j = 0; j < bins; j++) {
                frame[j] = (float) (i + j);
            }
            frames.add(frame);
        }

        FeatureSequence original = new FeatureSequence(frames, TEST_FRAME_RATE);

        byte[] encoded = FeatureBinarySerializer.serialize(original);

        FeatureSequence decoded = FeatureBinarySerializer.deserialize(encoded);

        assertEquals(framesCount, decoded.frames().size());
        assertEquals(TEST_FRAME_RATE, decoded.frameRate());

        for (int i = 0; i < framesCount; i++) {
            assertArrayEquals(frames.get(i), decoded.frames().get(i), 1e-6f);
        }
    }

    @Test
    @DisplayName("Один фрейм с одним значением")
    void singleFrameSingleValue() {

        List<float[]> original = List.of(new float[] {42.0f});
        FeatureSequence seq = new FeatureSequence(original, TEST_FRAME_RATE);

        byte[] encoded = FeatureBinarySerializer.serialize(seq);
        FeatureSequence decoded = FeatureBinarySerializer.deserialize(encoded);

        assertEquals(1, decoded.frames().size());
        assertEquals(1, decoded.frames().getFirst().length);
        assertEquals(42.0f, decoded.frames().getFirst()[0], 1e-6f);
    }

    @Test
    @DisplayName("Сериализация и десериализация с максимальными значениями float")
    void extremeFloatValues() {

        List<float[]> frames = List.of(
                new float[] {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MIN_VALUE, Float.NaN, Float.POSITIVE_INFINITY});

        FeatureSequence seq = new FeatureSequence(frames, TEST_FRAME_RATE);

        byte[] encoded = FeatureBinarySerializer.serialize(seq);

        FeatureSequence decoded = FeatureBinarySerializer.deserialize(encoded);

        float[] originalFrame = frames.getFirst();
        float[] decodedFrame = decoded.frames().getFirst();
        assertEquals(originalFrame.length, decodedFrame.length);
        for (int i = 0; i < originalFrame.length; i++) {
            if (Float.isNaN(originalFrame[i])) {
                assertTrue(Float.isNaN(decodedFrame[i]));
            } else {
                assertEquals(originalFrame[i], decodedFrame[i], 1e-6f);
            }
        }
    }

    @Test
    @DisplayName("Защита от переполнения при десериализации")
    void deserializeOverflowThrows() {

        ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);

        int large = Integer.MAX_VALUE / 1000;
        buffer.putInt(large);
        buffer.putInt(large);
        byte[] data = buffer.array();

        assertThrows(IllegalArgumentException.class, () -> FeatureBinarySerializer.deserialize(data));
    }
}
