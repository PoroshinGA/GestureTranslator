package com.ortin.gesturetranslator.core.repository;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import com.ortin.gesturetranslator.core.managers.mediapipe.MediaPipeManager;
import com.ortin.gesturetranslator.core.managers.mediapipe.listeners.MPDetectionListener;
import com.ortin.gesturetranslator.core.managers.mediapipe.models.MPDetection;
import com.ortin.gesturetranslator.core.managers.mediapipe.models.MPImageInput;
import com.ortin.gesturetranslator.domain.listeners.DetectionHandListener;
import com.ortin.gesturetranslator.domain.models.HandDetected;
import com.ortin.gesturetranslator.domain.models.Image;
import com.ortin.gesturetranslator.domain.repository.HandDetectionRepository;

import java.util.Iterator;

public class HandDetectionRepositoryImpl implements HandDetectionRepository {
    private MediaPipeManager mediaPipeManager;

    public HandDetectionRepositoryImpl(MediaPipeManager mediaPipeManager) {
        this.mediaPipeManager = mediaPipeManager;
    }

    @Override
    public void detectImage(Image image) {
        mediaPipeManager.detectImage(mapToMPImageInput(image));
    }

    @Override
    public void setDetectionHandListener(DetectionHandListener detectionHandListener) {
        mediaPipeManager.setMPDetectionListener(mapToCoreListener(detectionHandListener));
    }


    // Правила перевода для связи domain и core модулей

    private MPImageInput mapToMPImageInput(Image image) {
        Bitmap bitmap = image.getBitmap();
        MPImage mpImage = new BitmapImageBuilder(bitmap).build();

        return new MPImageInput(mpImage);
    }

    private HandDetected mapToCoreHandDetection(MPDetection mpDetection) {
        if (mpDetection == null) return null;

        float[] coordinates = new float[42];
        HandLandmarkerResult result = mpDetection.getResult();

        if (result.landmarks().size() != 0) {

            Iterator<NormalizedLandmark> iterator = result.landmarks().get(0).iterator();

            for (int i = 0; i < coordinates.length; i += 2) {
                if (iterator.hasNext()) {
                    NormalizedLandmark landmark = iterator.next();
                    coordinates[i] = landmark.x();
                    coordinates[i + 1] = landmark.y();
                } else {
                    break;
                }
            }
        }

        return new HandDetected(coordinates);
    }

    private MPDetectionListener mapToCoreListener(DetectionHandListener detectionHandListener) {
        return new MPDetectionListener() {
            @Override
            public void detect(MPDetection mpDetection) {
                detectionHandListener.detect(mapToCoreHandDetection(mpDetection));
            }

            @Override
            public void error(Exception exception) {
                detectionHandListener.error(exception);
            }
        };
    }
}
