package com.ortin.gesturetranslator.core.managers.mediapipe;

import android.content.Context;
import android.os.SystemClock;

import com.ortin.gesturetranslator.core.managers.mediapipe.listeners.MPDetectionListener;
import com.ortin.gesturetranslator.core.managers.mediapipe.models.MPDetection;
import com.ortin.gesturetranslator.core.managers.mediapipe.models.MPImageInput;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

public class MediaPipeManagerImpl implements MediaPipeManager {
    private final Context context;

    private MPDetectionListener mpDetectionListener;

    private HandLandmarker handLandmarker = null;

    private final String modelName;

    public MediaPipeManagerImpl(Context context, String modelName) {
        this.context = context;
        this.modelName = modelName;
    }

    @Override
    public void detectImage(MPImageInput mpImageInput) {
        if (handLandmarker == null) {
            setupBuilder();
        }

        handLandmarker.detectAsync(mpImageInput.getMpImage(), SystemClock.uptimeMillis());
    }

    private void setupBuilder() {
        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setModelAssetPath(modelName);

        BaseOptions baseOptions = baseOptionsBuilder.build();

        HandLandmarker.HandLandmarkerOptions.Builder optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.5f) // Минимальная оценка достоверности для обнаружения рук, которая считается успешной в модели обнаружения ладоней
                .setMinTrackingConfidence(0.5f) // Минимальная оценка достоверности для того, чтобы отслеживание рук считалось успешным
                .setMinHandPresenceConfidence(0.5f) // Минимальная оценка достоверности для оценки присутствия руки в модели обнаружения ориентира руки
                .setNumHands(1) // Максимальное количество рук, обнаруженных Детектором ориентиров рук
                .setRunningMode(RunningMode.LIVE_STREAM) // Устанавливает режим работы для задачи ручного ориентира
                .setResultListener(this::resultDetection)
                .setErrorListener(this::errorDetection);

        HandLandmarker.HandLandmarkerOptions options = optionsBuilder.build();

        handLandmarker = HandLandmarker.createFromOptions(context, options);
    }

    @Override
    public void setMPDetectionListener(MPDetectionListener mpDetectionListener) {
        this.mpDetectionListener = mpDetectionListener;
    }


    private void resultDetection(HandLandmarkerResult result, MPImage mpImage) {
        if (mpDetectionListener != null) {
            if (result.landmarks().size() == 0) mpDetectionListener.detect(null);
            else mpDetectionListener.detect(new MPDetection(result, mpImage));
        }
    }

    private void errorDetection(RuntimeException exception) {
        if (mpDetectionListener != null) {
            mpDetectionListener.error(exception);
        }
    }
}
