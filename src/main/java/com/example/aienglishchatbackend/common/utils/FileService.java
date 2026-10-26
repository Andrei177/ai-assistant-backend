package com.example.aienglishchatbackend.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileService {
    @Value("${file.store.path}")
    private String STORE_DIRECTORY;

    public String saveFile(MultipartFile multipartFile) throws IOException {
        Path uploadPath = Paths.get(STORE_DIRECTORY);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = UUID.randomUUID().toString() + "_" + multipartFile.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(multipartFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    public File getFile(String filename) {
        return Paths.get(STORE_DIRECTORY, filename).toFile();
    }

    public File convertToWav(File inputAudioFile) throws IOException {
        // 1. Создаем временный файл для выходного WAV
        File tempWavFile = File.createTempFile("voice_", ".wav");
        tempWavFile.deleteOnExit(); // Резервная очистка

        try {
            // 2. Конфигурируем аудио-параметры строго под Whisper
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("pcm_s16le");     // 16-bit PCM
            audio.setSamplingRate(16000);    // 16 kHz
            audio.setChannels(1);            // Моно

            // 3. Настройки контейнера (формата)
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("wav");
            attrs.setAudioAttributes(audio);

            // 4. Запуск конвертации
            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(inputAudioFile), tempWavFile, attrs);

        } catch (EncoderException e) {
            // Если конвертация упала, удаляем созданный пустой wav-файл
            Files.deleteIfExists(tempWavFile.toPath());
            throw new IOException("JAVE2 conversion failed: " + e.getMessage(), e);
        }

        return tempWavFile;
    }
}
