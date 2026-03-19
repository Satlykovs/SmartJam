package com.smartjam.smartjamanalyzer.domain.model;

import java.util.List;

/**
 * Represents a musical "fingerprint" of an audio track. It's a sequence of frames where each
 * frame contains energy
 * magnitudes across different frequency bins (semitones).
 *
 * @param frames    A list of feature vectors (e.g., CQT or Chromagram).
 * @param frameRate How many frames are processed per second (Hz). Necessary for time-alignment
 *                  during comparison.
 */
public record FeatureSequence(List<float[]> frames, float frameRate)
{
    public FeatureSequence
    {
        if (frames == null || frames.isEmpty())
        {
            throw new IllegalArgumentException("Frames list cannot be null or empty");
        }

        if (frames.getFirst() == null)
        {
            throw new IllegalArgumentException("First frame cannot be null");
        }
        if (frameRate <= 0)
        {
            throw new IllegalArgumentException("Frame rate must be strictly positive");
        }
    }

    /**
     * @return Total number of semitone bins in one frame.
     */
    public int binCount()
    {
        return frames.isEmpty() ? 0 : frames.getFirst().length;
    }

    /**
     * @return Total duration of the sequence in seconds.
     */
    public double durationSeconds()
    {
        return frames.size() / (double) frameRate;
    }
}
