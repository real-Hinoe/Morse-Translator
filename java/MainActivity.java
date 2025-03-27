package com.example.morsetranslator;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private EditText textInput;
    private TextView textOutput;
    public Button toMorseCodeButton;
    public ImageButton flashlightButton;
    public ImageButton microphoneButton;

    private CameraManager cameraManager;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private String cameraId;
    private boolean isFlashOn = false;
    private static final Map<Character, String> MORSE_MAP = new HashMap<>();


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

        toMorseCodeButton = findViewById(R.id.translate_button);
        flashlightButton = findViewById(R.id.flashlight_button);
        microphoneButton = findViewById(R.id.microphone_button);
        textInput = findViewById(R.id.text_input);
        textOutput = findViewById(R.id.text_output);

        // Если текст слишком большой, его можно будет скролить
        textOutput.setMovementMethod(new android.text.method.ScrollingMovementMethod());

        // Регистрация контракта для получения результата распознавания голоса
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            if (matches != null && !matches.isEmpty()) {
                                // Отображаем первый результат
                                textInput.setText(matches.get(0));
                            }
                        }
                    }
                }
        );

        {
            MORSE_MAP.put('а', "·—");
            MORSE_MAP.put('б', "—···");
            MORSE_MAP.put('в', "·——");
            MORSE_MAP.put('г', "——·");
            MORSE_MAP.put('д', "—··");
            MORSE_MAP.put('е', "·");
            MORSE_MAP.put('ё', "·");
            MORSE_MAP.put('ж', "···—");
            MORSE_MAP.put('з', "——··");
            MORSE_MAP.put('и', "··");
            MORSE_MAP.put('й', "·———");
            MORSE_MAP.put('к', "—·—");
            MORSE_MAP.put('л', "·—··");
            MORSE_MAP.put('м', "——");
            MORSE_MAP.put('н', "—·");
            MORSE_MAP.put('о', "———");
            MORSE_MAP.put('п', "·——·");
            MORSE_MAP.put('р', "·—·");
            MORSE_MAP.put('с', "···");
            MORSE_MAP.put('т', "—");
            MORSE_MAP.put('у', "··—");
            MORSE_MAP.put('ф', "··—·");
            MORSE_MAP.put('х', "····");
            MORSE_MAP.put('ц', "—·—·");
            MORSE_MAP.put('ч', "———·");
            MORSE_MAP.put('ш', "————");
            MORSE_MAP.put('щ', "——·—");
            MORSE_MAP.put('ъ', "——·——");
            MORSE_MAP.put('ы', "—·——");
            MORSE_MAP.put('ь', "—··—");
            MORSE_MAP.put('э', "··—··");
            MORSE_MAP.put('ю', "··——");
            MORSE_MAP.put('я', "·—·—");

            MORSE_MAP.put('a', "·—");
            MORSE_MAP.put('b', "—···");
            MORSE_MAP.put('c', "—·—·");
            MORSE_MAP.put('d', "—··");
            MORSE_MAP.put('e', "·");
            MORSE_MAP.put('f', "··—·");
            MORSE_MAP.put('g', "——·");
            MORSE_MAP.put('h', "····");
            MORSE_MAP.put('i', "··");
            MORSE_MAP.put('j', "·———");
            MORSE_MAP.put('k', "—·—");
            MORSE_MAP.put('l', "·—··");
            MORSE_MAP.put('m', "——");
            MORSE_MAP.put('n', "—·");
            MORSE_MAP.put('o', "———");
            MORSE_MAP.put('p', "·——·");
            MORSE_MAP.put('q', "——·—");
            MORSE_MAP.put('r', "·—·");
            MORSE_MAP.put('s', "···");
            MORSE_MAP.put('t', "—");
            MORSE_MAP.put('u', "··—");
            MORSE_MAP.put('v', "···—");
            MORSE_MAP.put('w', "·——");
            MORSE_MAP.put('x', "—··—");
            MORSE_MAP.put('y', "—·——");
            MORSE_MAP.put('z', "——··");

            MORSE_MAP.put('1', "·————");
            MORSE_MAP.put('2', "··———");
            MORSE_MAP.put('3', "···——");
            MORSE_MAP.put('4', "····—");
            MORSE_MAP.put('5', "·····");
            MORSE_MAP.put('6', "—····");
            MORSE_MAP.put('7', "——···");
            MORSE_MAP.put('8', "———··");
            MORSE_MAP.put('9', "————·");
            MORSE_MAP.put('0', "—————");

            MORSE_MAP.put('.', "······");
            MORSE_MAP.put(',', "·—·—·—");
            MORSE_MAP.put(':', "———···");
            MORSE_MAP.put(';', "—·—·—·");
            MORSE_MAP.put('(', "—·——·—");
            MORSE_MAP.put(')', "—·——·—");
            MORSE_MAP.put('\'', "·————·");
            MORSE_MAP.put('\"', "·—··—·");
            MORSE_MAP.put('«', "·—··—·");
            MORSE_MAP.put('»', "·—··—·");
            MORSE_MAP.put('-', "—····—");
            MORSE_MAP.put('/', "—··—·");
            MORSE_MAP.put('_', "··——·—");
            MORSE_MAP.put('?', "··——··");
            MORSE_MAP.put('!', "——··——");
            MORSE_MAP.put('+', "·—·—·");
            MORSE_MAP.put('§', "—···—");
            MORSE_MAP.put('@', "·——·—·");

            MORSE_MAP.put(' ', " ");
            MORSE_MAP.put('\n', "\n");
        }

        // Трансляция в азбуку Морзе в виде текста
        toMorseCodeButton.setOnClickListener(view -> {
            String originalText = textInput.getText().toString().toLowerCase().trim();
            StringBuilder translatedText = new StringBuilder();
            for (char c : originalText.toCharArray()) {
                if (c == '\n') {
                    translatedText.append(MORSE_MAP.get(c));
                } else if (MORSE_MAP.get(c) != null) {
                    translatedText.append(MORSE_MAP.get(c)).append(' ');
                } else {
                    translatedText.append("? ");
                }
            }
            textOutput.setText(translatedText.toString().trim());
        });

        // Голосовой ввод текста
        microphoneButton.setOnClickListener(view -> startVoiceInput());

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
                showToast("Ошибка: " + e.getMessage());
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

    // Начать запись голоса
    private void startVoiceInput() {
        // Создаем Intent для голосового ввода
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите");

        // Указываем несколько языков через EXTRA_LANGUAGE_PREFERENCE
        String[] languages = {"en-US", "ru-RU"}; // Английский, русский
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag()); // Основной язык
        intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, languages); // Дополнительные языки
        // Запускаем активити через ActivityResultLauncher
        try {
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            showToast("Ошибка: " + e.getMessage());
        }
    }

    // Включение/выключение фонарика
    private void toggleFlashlight() {
        try {
            // Переключаем состояние фонарика
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);

        } catch (Exception e) {
            showToast("Не удалось изменить состояние фонарика: " + e.getMessage());
        }
    }

    // Короткое всплывающее уведомление
    private void showToast(String message) {
        Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
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