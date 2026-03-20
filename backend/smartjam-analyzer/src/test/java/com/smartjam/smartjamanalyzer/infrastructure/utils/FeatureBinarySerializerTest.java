package com.smartjam.smartjamanalyzer.infrastructure.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureBinarySerializerTest {

    @Test
    @DisplayName("Сериализация и десериализация сохраняет данные")
    void shouldMaintainDataIntegrity() {

        float[] frame1 = {0.1f, 0.5f, 0.9f};
        float[] frame2 = {0.2f, 0.4f, 0.6f};
        List<float[]> original = List.of(frame1, frame2);

        byte[] encoded = FeatureBinarySerializer.serialize(original);
        assertNotNull(encoded);
        assertTrue(encoded.length > 8);

        List<float[]> decoded = FeatureBinarySerializer.deserialize(encoded);

        assertEquals(original.size(), decoded.size());
        assertEquals(original.getFirst().length, decoded.getFirst().length);

        for (int i = 0; i < original.size(); i++) {
            assertArrayEquals(original.get(i), decoded.get(i), 1e-6f);
        }
    }

    @Test
    @DisplayName("Пустой список возвращает пустой массив и пустой результат")
    void emptyList() {

        List<float[]> empty = List.of();
        byte[] encoded = FeatureBinarySerializer.serialize(empty);

        assertEquals(0, encoded.length);

        List<float[]> decoded = FeatureBinarySerializer.deserialize(encoded);

        assertTrue(decoded.isEmpty());
    }

    @Test
    @DisplayName("Сериализация с null вызывает NullPointerException")
    void serializeNullThrows() {

        assertThrows(NullPointerException.class, () -> FeatureBinarySerializer.serialize(null));
    }

    @Test
    @DisplayName("Десериализация null возвращает пустой список")
    void deserializeNullReturnsEmpty() {

        List<float[]> result = FeatureBinarySerializer.deserialize(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Десериализация слишком короткого массива возвращает пустой список")
    void deserializeTooShortReturnsEmpty() {

        byte[] shortData = new byte[4];
        List<float[]> result = FeatureBinarySerializer.deserialize(shortData);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Сериализация с фреймами разной длины вызывает исключение")
    void serializeFramesWithDifferentLengthsThrows() {

        List<float[]> frames = List.of(new float[] {1.0f, 2.0f}, new float[] {3.0f, 4.0f, 5.0f});
        assertThrows(IllegalArgumentException.class, () -> FeatureBinarySerializer.serialize(frames));
    }

    @Test
    @DisplayName("Десериализация с отрицательным frameCount вызывает исключение")
    void deserializeNegativeFrameCountThrows() {

        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(-1);
        buffer.putInt(10);
        byte[] data = buffer.array();

        assertThrows(IllegalArgumentException.class, () -> FeatureBinarySerializer.deserialize(data));
    }

    @Test
    @DisplayName("Десериализация с отрицательным binCount вызывает исключение")
    void deserializeNegativeBinCountThrows() {

        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
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
        byte[] encoded = FeatureBinarySerializer.serialize(frames);

        ByteBuffer buffer = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(1, buffer.getInt());
        assertEquals(1, buffer.getInt());
        assertEquals(1.0f, buffer.getFloat(), 1e-6f);
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

        byte[] encoded = FeatureBinarySerializer.serialize(frames);
        List<float[]> decoded = FeatureBinarySerializer.deserialize(encoded);

        assertEquals(framesCount, decoded.size());

        for (int i = 0; i < framesCount; i++) {
            assertArrayEquals(frames.get(i), decoded.get(i), 1e-6f);
        }
    }

    @Test
    @DisplayName("Один фрейм с одним значением")
    void singleFrameSingleValue() {

        List<float[]> original = List.of(new float[] {42.0f});

        byte[] encoded = FeatureBinarySerializer.serialize(original);
        List<float[]> decoded = FeatureBinarySerializer.deserialize(encoded);

        assertEquals(1, decoded.size());
        assertEquals(1, decoded.getFirst().length);
        assertEquals(42.0f, decoded.getFirst()[0], 1e-6f);
    }

    @Test
    @DisplayName("Сериализация и десериализация с максимальными значениями float")
    void extremeFloatValues() {

        List<float[]> frames = List.of(
                new float[] {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MIN_VALUE, Float.NaN, Float.POSITIVE_INFINITY});

        byte[] encoded = FeatureBinarySerializer.serialize(frames);
        List<float[]> decoded = FeatureBinarySerializer.deserialize(encoded);

        float[] originalFrame = frames.getFirst();
        float[] decodedFrame = decoded.getFirst();
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

        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

        int large = Integer.MAX_VALUE / 1000;
        buffer.putInt(large);
        buffer.putInt(large);
        byte[] data = buffer.array();

        assertThrows(IllegalArgumentException.class, () -> FeatureBinarySerializer.deserialize(data));
    }
}
