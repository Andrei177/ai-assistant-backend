package com.example.aienglishchatbackend.transcription.service;

import io.github.givimad.whisperjni.WhisperJNI;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import javax.sound.sampled.AudioInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Service
public class TranscriptionService {
    private WhisperJNI whisper;
    private WhisperContext ctx;

    @Value("${whisper.model.path}")
    private String modelPath;

    @PostConstruct
    public void init() {
        // Загрузка библиотеки и модели
        try {
            WhisperJNI.loadLibrary();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load whisper native library", e);
        }
        
        whisper = new WhisperJNI();
        try {
            ctx = whisper.init(Paths.get(modelPath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to init whisper", e);
        }
    }

    public String transcribe(File wavFile) {
        // Чтение аудио из файла в float[]
        float[] samples = readAudio(wavFile);

        WhisperFullParams params = new WhisperFullParams();
        // Установка параметров транскрибации
        
        int result = whisper.full(ctx, params, samples, samples.length);
        if (result != 0) {
            throw new RuntimeException("Transcription failed");
        }
        
        int numSegments = whisper.fullNSegments(ctx);
        StringBuilder transcription = new StringBuilder();
        for (int i = 0; i < numSegments; i++) {
            transcription.append(whisper.fullGetSegmentText(ctx, i));
        }
        
        return transcription.toString();
    }
    
    private float[] readAudio(File file) {
        // Реализация чтения WAV файла и преобразования в float[]
        try (AudioInputStream audioInputStream = javax.sound.sampled.AudioSystem.getAudioInputStream(file)) {
            byte[] bytes = audioInputStream.readAllBytes();
            float[] samples = new float[bytes.length / 2];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = (float) ((bytes[2 * i] & 0xFF) | (bytes[2 * i + 1] << 8)) / 32768.0f;
            }
            return samples;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read audio", e);
        }
    }
    
    @PreDestroy
    public void close() {
        if (ctx != null) {
            ctx.close();
        }
    }
}
