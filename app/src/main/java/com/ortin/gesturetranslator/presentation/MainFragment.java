package com.ortin.gesturetranslator.presentation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.ortin.gesturetranslator.components.OnChangedStatusListener;
import com.ortin.gesturetranslator.databinding.MainFrameBinding;
import com.ortin.gesturetranslator.domain.listeners.DetectionHandListener;
import com.ortin.gesturetranslator.domain.listeners.LoadImagesListener;
import com.ortin.gesturetranslator.domain.listeners.RecognizeImageListener;
import com.ortin.gesturetranslator.domain.models.CoordinateClassification;
import com.ortin.gesturetranslator.domain.models.HandDetected;
import com.ortin.gesturetranslator.domain.models.Image;
import com.ortin.gesturetranslator.domain.models.ImageClassification;
import com.ortin.gesturetranslator.domain.usecases.DetectHandUseCase;
import com.ortin.gesturetranslator.domain.usecases.LoadImageUseCase;
import com.ortin.gesturetranslator.domain.usecases.RecognizeCoordinateUseCase;
import com.ortin.gesturetranslator.domain.usecases.RecognizeImageUseCase;
import com.ortin.gesturetranslator.domain.usecases.WordCompileUseCase;

import java.util.Arrays;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;

@AndroidEntryPoint
public class MainFragment extends Fragment implements LoadImagesListener, RecognizeImageListener, DetectionHandListener {
    private MainFrameBinding binding;

    private static final String TAG = "MainFrame";

    @Inject
    LoadImageUseCase loadImageUseCase;

    @Inject
    WordCompileUseCase wordCompileUseCase;

    @Inject
    RecognizeImageUseCase recognizeImageUseCase;

    @Inject
    DetectHandUseCase detectHandUseCase;

    @Inject
    RecognizeCoordinateUseCase recognizeCoordinateUseCase;

    ActivityResultLauncher<String> mGetContent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerPermissionListener();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = MainFrameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        init();
        initListeners();
        start();
    }

    private void registerPermissionListener() {
        mGetContent = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    start();
                } else {
                    mGetContent.launch(Manifest.permission.CAMERA);
                }
            }
        });
    }

    private void init() {
        wordCompileUseCase.clearState();

        binding.bottomSheetBehaviorLayout.bottomSheetBehavior.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.bottomSheetBehaviorLayout.bottomSheetBehavior.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                BottomSheetBehavior.from(binding.bottomSheetBehaviorLayout.bottomSheetBehavior).setPeekHeight(displayMetrics.heightPixels - binding.imageWithPredict.preview.getBottom());
            }
        });
    }

    private void start() {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mGetContent.launch(Manifest.permission.CAMERA);
        } else {
            loadImageUseCase.execute(this, this.getViewLifecycleOwner());
            //recognizeImageUseCase.setOnRecogniseListener(this);
            detectHandUseCase.setOnDetectionHandListener(this);
        }
    }

    private void initListeners() {
        BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetBehaviorLayout.bottomSheetBehavior);
        binding.controlMenu.realTimeBTN.setOnChangedStatusListener(new OnChangedStatusListener() {
            @Override
            public void onStart() {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                wordCompileUseCase.clearState();
            }

            @Override
            public void onStop() {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        binding.controlMenu.flashLight.setOnChangedStatusListener(new OnChangedStatusListener() {
            @Override
            public void onStart() {
                loadImageUseCase.setStatusFlashlight(true);
            }

            @Override
            public void onStop() {
                loadImageUseCase.setStatusFlashlight(false);
            }
        });

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                Log.e(TAG, "onStateChanged: " + newState);
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        if (binding.controlMenu.realTimeBTN.isPlay())
                            binding.controlMenu.realTimeBTN.onStop();
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        if (!binding.controlMenu.realTimeBTN.isPlay())
                            binding.controlMenu.realTimeBTN.onStart();
                        break;
                    default:
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        binding.controlMenu.galleryLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.controlMenu.gallery.setProgress(0);
                binding.controlMenu.gallery.playAnimation();
            }
        });
    }

    @Override
    public void getImage(Image image) {
        Bitmap bitmap = image.getBitmap();

        binding.imageWithPredict.preview.setImageBitmap(bitmap);


        if (binding.controlMenu.realTimeBTN.isPlay()) detectHandUseCase.execute(image);
        //if (binding.controlMenu.realTimeBTN.isPlay()) recognizeImageUseCase.execute(image);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void recognise(ImageClassification imageClassification) {
        binding.imageWithPredict.wordPredictTV.setText(String.format("%s %.2f", imageClassification.getLabel(), imageClassification.getPercent()) + "%");
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n", "CheckResult"})
    @Override
    public void detect(HandDetected handDetected) {
        if (binding != null) { // Костыль
            if (handDetected != null) {
                binding.imageWithPredict.paintHandView.drawHand(handDetected.getCoordinates());

                CoordinateClassification coordinateClassification = recognizeCoordinateUseCase.execute(handDetected);

                if (coordinateClassification.getPercent() > 30f) {
                    wordCompileUseCase.addLetter(coordinateClassification.getLabel());
                }

                Observable.just(String.format("%s %.2f", coordinateClassification.getLabel(), coordinateClassification.getPercent()) + "%").subscribeOn(AndroidSchedulers.mainThread()).subscribe(t -> {
                    if (binding != null) {
                        binding.imageWithPredict.wordPredictTV.setText(t);
                        binding.bottomSheetBehaviorLayout.recognizeTextResult.setText(wordCompileUseCase.getWord());
                    }
                });

                //binding.imageWithPredict.wordPredictTV.setText(String.format("%s %.2f", coordinateClassification.getLabel(), coordinateClassification.getPercent()) + "%");
                Log.e(TAG, "coordinates: " + Arrays.toString(handDetected.getCoordinates()));
                Log.e(TAG, "label: " + coordinateClassification.getLabel() + " percent: " + coordinateClassification.getPercent());
            } else {
                binding.imageWithPredict.paintHandView.clear();
                Observable.just("").subscribeOn(AndroidSchedulers.mainThread()).subscribe(t -> {
                    if (binding != null) binding.imageWithPredict.wordPredictTV.setText(t);
                });
            }
        }
    }

    @Override
    public void error(Exception exception) {
        Log.e(TAG, "Error!  [!]");
        exception.printStackTrace();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        loadImageUseCase.setStatusFlashlight(false);
        binding = null;
    }
}
