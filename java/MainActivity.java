package com.example.morsetranslator;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.media.AudioAttributes;
import android.media.SoundPool;

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
    public ImageButton speakerButton;
    private CameraManager cameraManager;
    private String cameraId;
    private String savedOutput; // Для сохранения кода Морзе при смене темы
    private boolean isFlashOn, soundIsPlaying, lightIsFlashing = false;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private SoundPool soundPool;
    private int shortBeep, longBeep;
    private final View[] allButtons = new View[4];
    private final Handler onStartHandler = new Handler(); // Для задержки воспроизведения
    // Для управления событиями в случае окончания воспроизведения звука или вспышке фонарика
    private final Handler onStopHandler = new Handler();

    private static final Map<Character, String> TO_MORSE_CODE_MAP = new HashMap<>();
    private static final int SHORT_BEEP_LENGTH_MS = 100;

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

        if (savedInstanceState != null) {
            // Восстанавливаем данные из сохранённого состояния
            String defaultValue = getResources().getString(R.string.output_hint);
            savedOutput = savedInstanceState.getString("textOutput", defaultValue);
        }

        toMorseCodeButton = findViewById(R.id.translateButton);
        flashlightButton = findViewById(R.id.flashlightButton);
        microphoneButton = findViewById(R.id.microphoneButton);
        speakerButton = findViewById(R.id.speakerButton);
        textInput = findViewById(R.id.textInput);
        textOutput = findViewById(R.id.textOutput);

        textOutput.setText(savedOutput);
        allButtons[0] = toMorseCodeButton;
        allButtons[1] = flashlightButton;
        allButtons[2] = microphoneButton;
        allButtons[3] = speakerButton;

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
                    enableAllButtons();
                }
        );
        // Настройка SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(2) // Максимальное количество одновременно воспроизводимых звуков
                .setAudioAttributes(audioAttributes)
                .build();
        shortBeep = soundPool.load(this, R.raw.dot, 1); // короткий гудок
        longBeep = soundPool.load(this, R.raw.dash, 1); // долгий гудок

        {
            TO_MORSE_CODE_MAP.put('а', "·—");
            TO_MORSE_CODE_MAP.put('б', "—···");
            TO_MORSE_CODE_MAP.put('в', "·——");
            TO_MORSE_CODE_MAP.put('г', "——·");
            TO_MORSE_CODE_MAP.put('д', "—··");
            TO_MORSE_CODE_MAP.put('е', "·");
            TO_MORSE_CODE_MAP.put('ё', "·");
            TO_MORSE_CODE_MAP.put('ж', "···—");
            TO_MORSE_CODE_MAP.put('з', "——··");
            TO_MORSE_CODE_MAP.put('и', "··");
            TO_MORSE_CODE_MAP.put('й', "·———");
            TO_MORSE_CODE_MAP.put('к', "—·—");
            TO_MORSE_CODE_MAP.put('л', "·—··");
            TO_MORSE_CODE_MAP.put('м', "——");
            TO_MORSE_CODE_MAP.put('н', "—·");
            TO_MORSE_CODE_MAP.put('о', "———");
            TO_MORSE_CODE_MAP.put('п', "·——·");
            TO_MORSE_CODE_MAP.put('р', "·—·");
            TO_MORSE_CODE_MAP.put('с', "···");
            TO_MORSE_CODE_MAP.put('т', "—");
            TO_MORSE_CODE_MAP.put('у', "··—");
            TO_MORSE_CODE_MAP.put('ф', "··—·");
            TO_MORSE_CODE_MAP.put('х', "····");
            TO_MORSE_CODE_MAP.put('ц', "—·—·");
            TO_MORSE_CODE_MAP.put('ч', "———·");
            TO_MORSE_CODE_MAP.put('ш', "————");
            TO_MORSE_CODE_MAP.put('щ', "——·—");
            TO_MORSE_CODE_MAP.put('ъ', "——·——");
            TO_MORSE_CODE_MAP.put('ы', "—·——");
            TO_MORSE_CODE_MAP.put('ь', "—··—");
            TO_MORSE_CODE_MAP.put('э', "··—··");
            TO_MORSE_CODE_MAP.put('ю', "··——");
            TO_MORSE_CODE_MAP.put('я', "·—·—");

            TO_MORSE_CODE_MAP.put('a', "·—");
            TO_MORSE_CODE_MAP.put('b', "—···");
            TO_MORSE_CODE_MAP.put('c', "—·—·");
            TO_MORSE_CODE_MAP.put('d', "—··");
            TO_MORSE_CODE_MAP.put('e', "·");
            TO_MORSE_CODE_MAP.put('f', "··—·");
            TO_MORSE_CODE_MAP.put('g', "——·");
            TO_MORSE_CODE_MAP.put('h', "····");
            TO_MORSE_CODE_MAP.put('i', "··");
            TO_MORSE_CODE_MAP.put('j', "·———");
            TO_MORSE_CODE_MAP.put('k', "—·—");
            TO_MORSE_CODE_MAP.put('l', "·—··");
            TO_MORSE_CODE_MAP.put('m', "——");
            TO_MORSE_CODE_MAP.put('n', "—·");
            TO_MORSE_CODE_MAP.put('o', "———");
            TO_MORSE_CODE_MAP.put('p', "·——·");
            TO_MORSE_CODE_MAP.put('q', "——·—");
            TO_MORSE_CODE_MAP.put('r', "·—·");
            TO_MORSE_CODE_MAP.put('s', "···");
            TO_MORSE_CODE_MAP.put('t', "—");
            TO_MORSE_CODE_MAP.put('u', "··—");
            TO_MORSE_CODE_MAP.put('v', "···—");
            TO_MORSE_CODE_MAP.put('w', "·——");
            TO_MORSE_CODE_MAP.put('x', "—··—");
            TO_MORSE_CODE_MAP.put('y', "—·——");
            TO_MORSE_CODE_MAP.put('z', "——··");

            TO_MORSE_CODE_MAP.put('1', "·————");
            TO_MORSE_CODE_MAP.put('2', "··———");
            TO_MORSE_CODE_MAP.put('3', "···——");
            TO_MORSE_CODE_MAP.put('4', "····—");
            TO_MORSE_CODE_MAP.put('5', "·····");
            TO_MORSE_CODE_MAP.put('6', "—····");
            TO_MORSE_CODE_MAP.put('7', "——···");
            TO_MORSE_CODE_MAP.put('8', "———··");
            TO_MORSE_CODE_MAP.put('9', "————·");
            TO_MORSE_CODE_MAP.put('0', "—————");

            TO_MORSE_CODE_MAP.put('.', "······");
            TO_MORSE_CODE_MAP.put(',', "·—·—·—");
            TO_MORSE_CODE_MAP.put(':', "———···");
            TO_MORSE_CODE_MAP.put(';', "—·—·—·");
            TO_MORSE_CODE_MAP.put('(', "—·——·—");
            TO_MORSE_CODE_MAP.put(')', "—·——·—");
            TO_MORSE_CODE_MAP.put('\'', "·————·");
            TO_MORSE_CODE_MAP.put('\"', "·—··—·");
            TO_MORSE_CODE_MAP.put('«', "·—··—·");
            TO_MORSE_CODE_MAP.put('»', "·—··—·");
            TO_MORSE_CODE_MAP.put('-', "—····—");
            TO_MORSE_CODE_MAP.put('/', "—··—·");
            TO_MORSE_CODE_MAP.put('_', "··——·—");
            TO_MORSE_CODE_MAP.put('?', "··——··");
            TO_MORSE_CODE_MAP.put('!', "——··——");
            TO_MORSE_CODE_MAP.put('+', "·—·—·");
            TO_MORSE_CODE_MAP.put('§', "—···—");
            TO_MORSE_CODE_MAP.put('@', "·——·—·");

            TO_MORSE_CODE_MAP.put(' ', "/");
            TO_MORSE_CODE_MAP.put('\n', "\n");
        }

        toMorseCodeButton.setOnClickListener(view -> toMorseCode());
        microphoneButton.setOnClickListener(view -> {
            disableAllButtons();
            startVoiceInput();
        });
        flashlightButton.setOnClickListener(view -> {
            if (lightIsFlashing) {
                onStartHandler.removeCallbacksAndMessages(null); // Очистить все задачи
                onStopHandler.removeCallbacksAndMessages(null);
                flashlightButton.setImageResource(R.drawable.flashlight_on);
                enableAllButtons();
                lightIsFlashing = false;
            } else {
                int delay = translateInFlashes();
                if (delay > 0) {
                    flashlightButton.setImageResource(R.drawable.flashlight_off);
                    disableAllButtonsExcept(flashlightButton);
                    lightIsFlashing = true;
                    onStopHandler.postDelayed(() -> {
                        flashlightButton.setImageResource(R.drawable.flashlight_on);
                        enableAllButtons();
                        lightIsFlashing = false;
                    }, delay);
                }
            }
        });
        speakerButton.setOnClickListener(view -> {
            if (soundIsPlaying) {
                onStartHandler.removeCallbacksAndMessages(null); // Очистить все задачи
                onStopHandler.removeCallbacksAndMessages(null);
                speakerButton.setImageResource(R.drawable.speaker);
                enableAllButtons();
                soundIsPlaying = false;
            } else {
                int delay = translateInSound();
                if (delay > 0) {
                    speakerButton.setImageResource(R.drawable.speaker_mute);
                    disableAllButtonsExcept(speakerButton);
                    soundIsPlaying = true;
                    onStopHandler.postDelayed(() -> {
                        speakerButton.setImageResource(R.drawable.speaker);
                        enableAllButtons();
                        soundIsPlaying = false;
                    }, delay);
                }
            }
        });
        textOutput.setOnClickListener(view -> copyToClipboard());
    }

    // Трансляция в азбуку Морзе в виде текста
    private void toMorseCode() {
        String originalText = textInput.getText().toString().toLowerCase().trim();
        StringBuilder translatedText = new StringBuilder();
        for (char c : originalText.toCharArray()) {
            if (c == '\n') {
                translatedText.append(TO_MORSE_CODE_MAP.get(c));
            } else if (TO_MORSE_CODE_MAP.get(c) != null) {
                translatedText.append(TO_MORSE_CODE_MAP.get(c)).append(' ');
            } else {
                translatedText.append("? ");
            }
        }
        textOutput.setText(translatedText.toString().trim());
    }

    // Голосовой ввод текста
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
            showToast("Error");
        }
    }

    // Трансляция в азбуку Морзе в виде вспышек Фонарика
    private int translateInFlashes() {
        // Проверка разрешения на доступ к камере
        {
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
                showToast("Error");
            }
            if (cameraId == null) {
                showToast("Фонарик не поддерживается.");
                return 0;
            }
            // Проверяем разрешение
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CAMERA},
                        100
                );
                return 0;
            }
        }

        if (textOutput.length() == 0) {
            if (textInput.length() == 0) {
                showToast("Сначала введите сообщение.");
            } else {
                showToast("Сначала переведите в азбуку Морзе.");
            }
            return 0;
        }
        String morseCode = textOutput.getText().toString();
        morseCode = morseCode.replace(" / ", "/");
        morseCode = morseCode.replace(" ? ", "");

        /*
        Длина точки - длина точки x1
        Длина тире - длина точки x3
        Пауза между символами одного знака - длина точки x1
        Пауза между знаками одного слова - длина точки x3
        Пауза между словами - длина точки x7
        */
        if (isFlashOn) {
            isFlashOn = false;
        }
        int delay = SHORT_BEEP_LENGTH_MS * 7; // Фора перед вспышками (1 большая пауза)
        for (char c : morseCode.toCharArray()) {
            switch (c) {
                case '·':
                    onStartHandler.postDelayed(this::toggleFlashlight, delay);
                    delay += SHORT_BEEP_LENGTH_MS; // Длина точки
                    onStartHandler.postDelayed(this::toggleFlashlight, delay);
                    delay += SHORT_BEEP_LENGTH_MS; // Маленькая пауза
                    break;
                case '—':
                    onStartHandler.postDelayed(this::toggleFlashlight, delay);
                    delay += SHORT_BEEP_LENGTH_MS * 3; // Длина тире
                    onStartHandler.postDelayed(this::toggleFlashlight, delay);
                    delay += SHORT_BEEP_LENGTH_MS; // Маленькая пауза
                    break;
                case ' ':
                    delay += SHORT_BEEP_LENGTH_MS * 2; // Пауза между знаками в слове - маленькая пауза
                    break;
                case '/':
                case '\n':
                    delay += SHORT_BEEP_LENGTH_MS * 6; // Пауза между словами - маленькая пауза
                    break;
            }
        }
        return delay;
    }

    // Вывод кода Морзе в виде звука
    private int translateInSound() {
        if (textOutput.length() == 0) {
            if (textInput.length() == 0) {
                showToast("Сначала введите сообщение.");
            } else {
                showToast("Сначала переведите в азбуку Морзе.");
            }
            return 0;
        }
        String morseCode = textOutput.getText().toString();
        morseCode = morseCode.replace(" / ", "/");
        morseCode = morseCode.replace(" ? ", "");

        /*
        Длина точки - длина точки x1
        Длина тире - длина точки x3
        Пауза между символами одного знака - длина точки x1
        Пауза между знаками одного слова - длина точки x3
        Пауза между словами - длина точки x7
        */
        // Чтобы "разбудить" динамики смартфона, проигрывается очень тихая точка в начале сообщения
        soundPool.play(shortBeep, 0.01f, 0.01f, 2, 0, 1.0f);
        int delay = SHORT_BEEP_LENGTH_MS * 7; // Фора перед проигрыванием звуков (1 большая пауза)
        for (char c : morseCode.toCharArray()) {
            switch (c) {
                case '·':
                    onStartHandler.postDelayed(() -> soundPool.play(shortBeep, 1.0f, 1.0f, 1, 0, 1.0f), delay);
                    delay += SHORT_BEEP_LENGTH_MS * 2; // Длина точки + маленькая пауза
                    break;
                case '—':
                    onStartHandler.postDelayed(() -> soundPool.play(longBeep, 1.0f, 1.0f, 1, 0, 1.0f), delay);
                    delay += SHORT_BEEP_LENGTH_MS * 4; // Длина тире + маленькая пауза
                    break;
                case ' ':
                    delay += SHORT_BEEP_LENGTH_MS * 2; // Пауза между знаками в слове - маленькая пауза
                    break;
                case '/':
                case '\n':
                    delay += SHORT_BEEP_LENGTH_MS * 6; // Пауза между словами - маленькая пауза
                    break;
            }
        }
        return delay;
    }

    // Копирование кода Морзе в буфер обмена
    private void copyToClipboard() {
        if (textOutput.length() == 0) {
            return;
        }
        String textToCopy = textOutput.getText().toString();
        textToCopy = textToCopy.replace('·', '.').replace('—', '-');

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", textToCopy);
        clipboard.setPrimaryClip(clip);
        showToast("Текст скопирован в буфер обмена.");
    }

    // Включение/выключение фонарика
    private void toggleFlashlight() {
        try {
            // Переключаем состояние фонарика
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);
        } catch (Exception e) {
            showToast("Error");
        }
    }

    // Разблокировка всех кнопок
    private void enableAllButtons() {
        for (View button : allButtons) {
            button.setEnabled(true);
        }
    }

    // Блокировка всех кнопок
    private void disableAllButtons() {
        for (View button : allButtons) {
            button.setEnabled(false);
        }
    }

    // Блокировка всех кнопок, кроме указанной
    private void disableAllButtonsExcept(View exceptionButton) {
        for (View button : allButtons) {
            button.setEnabled(button == exceptionButton);
        }
    }

    // Короткое всплывающее уведомление
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Сохранение данных перед пересозданием
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        savedOutput = textOutput.getText().toString();
        outState.putString("textOutput", savedOutput);
    }

    // При изменении темы приложения
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onStartHandler.removeCallbacksAndMessages(null);
        onStopHandler.removeCallbacksAndMessages(null);
        recreate();
    }

    // При выходе из приложения
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            if (soundIsPlaying) {
                onStartHandler.removeCallbacksAndMessages(null);
                onStopHandler.removeCallbacksAndMessages(null);
                soundIsPlaying = false;
            }
            soundPool.release(); // Освободить ресурсы
            soundPool = null;
        }
    }

    // При сворачивании приложения
    @Override
    protected void onPause() {
        super.onPause();
        onStartHandler.removeCallbacksAndMessages(null);
        if (soundIsPlaying) {
            onStopHandler.removeCallbacksAndMessages(null);
            speakerButton.setImageResource(R.drawable.speaker);
            enableAllButtons();
            soundIsPlaying = false;
        }
    }

    // При открытии свернутого приложения
    @Override
    protected void onResume() {
        super.onResume();
    }

    // Получение ответа на запрос о разрешение камеры
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                translateInFlashes();
            } else {
                showToast("Разрешение камеры отклонено");
            }
        }
    }
}