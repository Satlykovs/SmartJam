package com.smartjam.smartjamanalyzer.infrastructure.analysis;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.ConstantQ;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;
import com.smartjam.smartjamanalyzer.domain.port.FeatureExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/** TarsosDSP implementation of {@link FeatureExtractor}. */
@Slf4j
@Service
@EnableConfigurationProperties(DspProperties.class)
@RequiredArgsConstructor
public class TarsosFeatureExtractor implements FeatureExtractor {

    private static final int SAMPLE_RATE = 44100;

    private final DspProperties props;

    @Override
    public FeatureSequence extract(Path audioFile) {
        log.info("Извлечение признаков из файла: {}", audioFile.getFileName());

        List<float[]> frames = new ArrayList<>();

        File file = audioFile.toFile();

        try {
            AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(file, props.bufferSize(), props.overlap());

            float maxFreq = props.minFreq() * (float) Math.pow(2, props.octaves());

            ConstantQ cqt = new ConstantQ(SAMPLE_RATE, props.minFreq(), maxFreq, props.binsPerOctave(), 0.005f, 1.0f);

            dispatcher.addAudioProcessor(cqt);

            dispatcher.addAudioProcessor(new AudioProcessor() {
                @Override
                public boolean process(AudioEvent audioEvent) {
                    float[] magnitudes = cqt.getMagnitudes().clone();

                    double sumOfSquares = 0;
                    for (float m : magnitudes) {
                        sumOfSquares += m * m;
                    }
                    float norm = (float) Math.sqrt(sumOfSquares);

                    if (norm > 1e-6) {
                        for (int i = 0; i < magnitudes.length; i++) {
                            magnitudes[i] /= norm;

                            if (magnitudes[i] < 0.01f) {
                                magnitudes[i] = 0f;
                            }
                        }
                    } else {
                        java.util.Arrays.fill(magnitudes, 0f);
                    }

                    frames.add(magnitudes);
                    return true;
                }

                @Override
                public void processingFinished() {
                    log.info("Обработка завершена. Всего кадров: {}", frames.size());
                }
            });

            dispatcher.run();

            float frameRate = (float) SAMPLE_RATE / (props.bufferSize() - props.overlap());

            return new FeatureSequence(frames, frameRate);

        } catch (Exception e) {
            log.error("Ошибка при извлечении признаков: {}", e.getMessage(), e);
            throw new RuntimeException("Extraction failed", e);
        }
    }
}
