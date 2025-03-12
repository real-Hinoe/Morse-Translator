package com.example.morsetranslator;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private Button langButton;
    private EditText textInput;
    private TextView textOutput;
    public Button translateButton;
    public ImageButton flashlightButton;

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;

    private String currentLanguage = "EN";
    public String[] LANGUAGES = {"EN", "RU"};

    public String getCurrentLanguage() {
        return currentLanguage;
    }
    public void setCurrentLanguage(String currentLanguage) {
        this.currentLanguage = currentLanguage;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        langButton = findViewById(R.id.lang_button);
        translateButton = findViewById(R.id.translate_button);
        flashlightButton = findViewById(R.id.flashlight_button);
        textInput = findViewById(R.id.text_input);
        textOutput = findViewById(R.id.text_output);

        // Смена языка
        langButton.setOnClickListener(view -> {
            String lang = langButton.getText().toString();
            int index = Arrays.asList(LANGUAGES).indexOf(lang);

            if (index + 1 >= LANGUAGES.length) {
                setCurrentLanguage(LANGUAGES[0]);
            } else {
                setCurrentLanguage(LANGUAGES[index + 1]);
            }
            langButton.setText(getCurrentLanguage());
        });

        // Трансляция в азбуку Морзе в виде текста
        translateButton.setOnClickListener(view -> {
            String originalText = textInput.getText().toString();
            textOutput.setText(originalText);
        });

        // Трансляция в азбуку Морзе в виде вспышек Фонарика
        flashlightButton.setOnClickListener(view -> {
            // Получаем экземпляр CameraManager
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            // Находим ID камеры с фонариком
            try {
                for (String id : cameraManager.getCameraIdList()) {
                    if (Boolean.TRUE.equals(cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                        cameraId = id;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (cameraId == null) {
                showToast("Фонарик не поддерживается");
                return;
            }

            // Проверяем разрешение
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CAMERA},
                        100
                );
                return;
            }
            toggleFlashlight();
        });
    }

    // Включение/выключение фонарика
    private void toggleFlashlight() {
        try {
            // Переключаем состояние фонарика
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Не удалось изменить состояние фонарика");
        }
    }

    // Короткое всплывающее уведомление
    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    // Получение ответа на запрос о разрешение камеры
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleFlashlight();
            } else {
                showToast("Разрешение камеры отклонено");
            }
        }
    }
}