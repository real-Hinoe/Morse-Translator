package com.example.morsetranslator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import android.Manifest;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.content.Intent;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.speech.RecognizerIntent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.annotation.NonNull;


public class MainActivity extends AppCompatActivity {
    private EditText textInput;
    private TextView textOutput;
    public Button toMorseCodeButton;
    public ImageButton aboutButton, microphoneButton, cameraButton, speakerButton, flashlightButton;
    private CameraManager cameraManager;
    private String cameraId, savedInput, savedOutput;
    private boolean isFlashOn, soundIsPlaying, lightIsFlashing, noCamera = false;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private SoundPool soundPool;
    private int shortBeep, longBeep;
    private final View[] allButtons = new View[6];
    private final Handler onStartHandler = new Handler(); // Для задержки воспроизведения
    // Для управления событиями в случае окончания воспроизведения звука или вспышке фонарика
    private final Handler onStopHandler = new Handler();

    private static final int SHORT_BEEP_LENGTH_MS = 100;
    private static final Map<Character, String> MORSE_CODE_MAP = new HashMap<>();
    static {
        MORSE_CODE_MAP.put('а', "·—");
        MORSE_CODE_MAP.put('б', "—···");
        MORSE_CODE_MAP.put('в', "·——");
        MORSE_CODE_MAP.put('г', "——·");
        MORSE_CODE_MAP.put('д', "—··");
        MORSE_CODE_MAP.put('е', "·");
        MORSE_CODE_MAP.put('ё', "·");
        MORSE_CODE_MAP.put('ж', "···—");
        MORSE_CODE_MAP.put('з', "——··");
        MORSE_CODE_MAP.put('и', "··");
        MORSE_CODE_MAP.put('й', "·———");
        MORSE_CODE_MAP.put('к', "—·—");
        MORSE_CODE_MAP.put('л', "·—··");
        MORSE_CODE_MAP.put('м', "——");
        MORSE_CODE_MAP.put('н', "—·");
        MORSE_CODE_MAP.put('о', "———");
        MORSE_CODE_MAP.put('п', "·——·");
        MORSE_CODE_MAP.put('р', "·—·");
        MORSE_CODE_MAP.put('с', "···");
        MORSE_CODE_MAP.put('т', "—");
        MORSE_CODE_MAP.put('у', "··—");
        MORSE_CODE_MAP.put('ф', "··—·");
        MORSE_CODE_MAP.put('х', "····");
        MORSE_CODE_MAP.put('ц', "—·—·");
        MORSE_CODE_MAP.put('ч', "———·");
        MORSE_CODE_MAP.put('ш', "————");
        MORSE_CODE_MAP.put('щ', "——·—");
        MORSE_CODE_MAP.put('ъ', "——·——");
        MORSE_CODE_MAP.put('ы', "—·——");
        MORSE_CODE_MAP.put('ь', "—··—");
        MORSE_CODE_MAP.put('э', "··—··");
        MORSE_CODE_MAP.put('ю', "··——");
        MORSE_CODE_MAP.put('я', "·—·—");

        MORSE_CODE_MAP.put('a', "·—");
        MORSE_CODE_MAP.put('b', "—···");
        MORSE_CODE_MAP.put('c', "—·—·");
        MORSE_CODE_MAP.put('d', "—··");
        MORSE_CODE_MAP.put('e', "·");
        MORSE_CODE_MAP.put('f', "··—·");
        MORSE_CODE_MAP.put('g', "——·");
        MORSE_CODE_MAP.put('h', "····");
        MORSE_CODE_MAP.put('i', "··");
        MORSE_CODE_MAP.put('j', "·———");
        MORSE_CODE_MAP.put('k', "—·—");
        MORSE_CODE_MAP.put('l', "·—··");
        MORSE_CODE_MAP.put('m', "——");
        MORSE_CODE_MAP.put('n', "—·");
        MORSE_CODE_MAP.put('o', "———");
        MORSE_CODE_MAP.put('p', "·——·");
        MORSE_CODE_MAP.put('q', "——·—");
        MORSE_CODE_MAP.put('r', "·—·");
        MORSE_CODE_MAP.put('s', "···");
        MORSE_CODE_MAP.put('t', "—");
        MORSE_CODE_MAP.put('u', "··—");
        MORSE_CODE_MAP.put('v', "···—");
        MORSE_CODE_MAP.put('w', "·——");
        MORSE_CODE_MAP.put('x', "—··—");
        MORSE_CODE_MAP.put('y', "—·——");
        MORSE_CODE_MAP.put('z', "——··");

        MORSE_CODE_MAP.put('1', "·————");
        MORSE_CODE_MAP.put('2', "··———");
        MORSE_CODE_MAP.put('3', "···——");
        MORSE_CODE_MAP.put('4', "····—");
        MORSE_CODE_MAP.put('5', "·····");
        MORSE_CODE_MAP.put('6', "—····");
        MORSE_CODE_MAP.put('7', "——···");
        MORSE_CODE_MAP.put('8', "———··");
        MORSE_CODE_MAP.put('9', "————·");
        MORSE_CODE_MAP.put('0', "—————");

        MORSE_CODE_MAP.put('.', "······");
        MORSE_CODE_MAP.put(',', "·—·—·—");
        MORSE_CODE_MAP.put(':', "———···");
        MORSE_CODE_MAP.put(';', "—·—·—·");
        MORSE_CODE_MAP.put('(', "—·——·—");
        MORSE_CODE_MAP.put(')', "—·——·—");
        MORSE_CODE_MAP.put('\'', "·————·");
        MORSE_CODE_MAP.put('\"', "·—··—·");
        MORSE_CODE_MAP.put('«', "·—··—·");
        MORSE_CODE_MAP.put('»', "·—··—·");
        MORSE_CODE_MAP.put('-', "—····—");
        MORSE_CODE_MAP.put('/', "—··—·");
        MORSE_CODE_MAP.put('_', "··——·—");
        MORSE_CODE_MAP.put('?', "··——··");
        MORSE_CODE_MAP.put('!', "——··——");
        MORSE_CODE_MAP.put('+', "·—·—·");
        MORSE_CODE_MAP.put('§', "—···—");
        MORSE_CODE_MAP.put('@', "·——·—·");

        MORSE_CODE_MAP.put(' ', "/");
        MORSE_CODE_MAP.put('\n', "\n");
    }

    // При создании активности
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
            savedInput = savedInstanceState.getString("textInput", defaultValue);
            savedOutput = savedInstanceState.getString("textOutput", defaultValue);
        }

        textInput = findViewById(R.id.textInput);
        textOutput = findViewById(R.id.textOutput);
        aboutButton = findViewById(R.id.drawerButton);
        microphoneButton = findViewById(R.id.microphoneButton);
        cameraButton = findViewById(R.id.cameraButton);
        speakerButton = findViewById(R.id.speakerButton);
        toMorseCodeButton = findViewById(R.id.translateButton);
        flashlightButton = findViewById(R.id.flashlightButton);

        if (savedInstanceState != null) {
            textInput.setText(savedInput);
            textOutput.setText(savedOutput);
        }
        allButtons[0] = aboutButton;
        allButtons[1] = microphoneButton;
        allButtons[2] = cameraButton;
        allButtons[3] = speakerButton;
        allButtons[4] = toMorseCodeButton;
        allButtons[5] = flashlightButton;

        // Если текст слишком большой, его можно будет скролить
        textOutput.setMovementMethod(new android.text.method.ScrollingMovementMethod());

        // Регистрация контракта для получения результата распознавания голоса
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            ArrayList<String> matches =
                                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
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
            noCamera = true; // Если произошла ошибка, значит у устройства нету камеры
        }

        textOutput.setOnLongClickListener(v -> {
            copyToClipboard();
            return true;
        });

        microphoneButton.setOnClickListener(v -> {
            disableAllButtons();
            startVoiceInput();
        });
        aboutButton.setOnClickListener(v -> {
            String msg =
                    "Транслятор/Распознаватель азбуки Морзе\n" +
                    "для мобильных устройств на базе Android\n\n" +
                    "Дмитрий Савинцев, СФУ ИКИТ, КИ23-16/1б";
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog alertDialog = builder
                    .setTitle("О приложении")
                    .setMessage(msg)
                    .setIcon(R.drawable.app_logo)
                    .create();
            alertDialog.show();
            alertDialog.getWindow().setLayout(1075, 575);
        });
        cameraButton.setOnClickListener(v -> {
            // Переход к активности с камерой
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CAMERA},
                        101 // Код запроса для перехода к MyCameraActivity
                );
            } else {
                Intent intent = new Intent(this, MyCameraActivity.class);
                startActivity(intent);
            }
        });
        speakerButton.setOnClickListener(v -> {
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
        toMorseCodeButton.setOnClickListener(v -> toMorseCode());
        flashlightButton.setOnClickListener(v -> {
            if (lightIsFlashing) {
                onStartHandler.removeCallbacksAndMessages(null); // Очистить все задачи
                onStopHandler.removeCallbacksAndMessages(null);
                flashlightButton.setImageResource(R.drawable.flashlight_on);
                enableAllButtons();
                lightIsFlashing = false;
                if (isFlashOn) toggleFlashlight();
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
    }

    // При открытии свернутого приложения
    @Override
    protected void onResume() {
        super.onResume();
    }

    // При сворачивании приложения
    @Override
    protected void onPause() {
        super.onPause();
        onStartHandler.removeCallbacksAndMessages(null);
        onStopHandler.removeCallbacksAndMessages(null);
        if (soundIsPlaying) {
            speakerButton.setImageResource(R.drawable.speaker);
            enableAllButtons();
            soundIsPlaying = false;
        }
        if (isFlashOn) toggleFlashlight();
    }

    // При выходе из приложения
    @Override
    protected void onDestroy() {
        super.onDestroy();
        onStartHandler.removeCallbacksAndMessages(null);
        onStopHandler.removeCallbacksAndMessages(null);
        if (soundPool != null) {
            soundPool.release(); // Освободить ресурсы
            soundPool = null;
        }
        if (isFlashOn) toggleFlashlight();
    }

    // Копирование кода Морзе в буфер обмена
    private void copyToClipboard() {
        if (textOutput.length() == 0) {
            if (textInput.length() == 0) showToast("Сначала введите сообщение.");
            else showToast("Сначала переведите в азбуку Морзе.");
            return;
        }
        String textToCopy = textOutput.getText().toString();
        textToCopy = textToCopy.replace('·', '.').replace('—', '-');

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", textToCopy);
        clipboard.setPrimaryClip(clip);
        showToast("Текст скопирован в буфер обмена.");
    }

    // Трансляция в азбуку Морзе в виде текста
    private void toMorseCode() {
        String originalText = textInput.getText().toString().toLowerCase().trim();
        StringBuilder translatedText = new StringBuilder();
        for (char c : originalText.toCharArray()) {
            if (c == '\n') {
                translatedText.append(MORSE_CODE_MAP.get(c));
            }
            else if (MORSE_CODE_MAP.get(c) != null) {
                translatedText.append(MORSE_CODE_MAP.get(c)).append(' ');
            }
            else translatedText.append("? ");
        }
        textOutput.setText(translatedText.toString().trim());
    }

    // Голосовой ввод текста
    private void startVoiceInput() {
        // Создаем Intent для голосового ввода
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getResources().getString(R.string.microphone_extra_prompt));

        // Указываем несколько языков через EXTRA_LANGUAGE_PREFERENCE
        String[] languages = {"en-US", "ru-RU"}; // Английский, русский
        // Основной язык
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        // Дополнительные языки
        intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, languages);
        // Запускаем активити через ActivityResultLauncher
        try {
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            showToast("Микрофон не поддерживается устройством.");
        }
    }

    // Вывод кода Морзе в виде звука
    private int translateInSound() {
        if (textOutput.length() == 0) {
            if (textInput.length() == 0) showToast("Сначала введите сообщение.");
            else showToast("Сначала переведите в азбуку Морзе.");
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
                    onStartHandler.postDelayed(() ->
                            soundPool.play(shortBeep, 1.0f, 1.0f, 1, 0, 1.0f), delay);
                    delay += SHORT_BEEP_LENGTH_MS * 2; // Длина точки + маленькая пауза
                    break;
                case '—':
                    onStartHandler.postDelayed(() ->
                            soundPool.play(longBeep, 1.0f, 1.0f, 1, 0, 1.0f), delay);
                    delay += SHORT_BEEP_LENGTH_MS * 4; // Длина тире + маленькая пауза
                    break;
                case ' ':
                    // Пауза между знаками в слове - маленькая пауза
                    delay += SHORT_BEEP_LENGTH_MS * 2;
                    break;
                case '/':
                case '\n':
                    // Пауза между словами - маленькая пауза
                    delay += SHORT_BEEP_LENGTH_MS * 6;
                    break;
            }
        }
        return delay;
    }

    // Трансляция в азбуку Морзе в виде вспышек Фонарика
    private int translateInFlashes() {
        if (noCamera) {
            showToast("Камера не поддерживается устройством.");
            return 0;
        }
        if (cameraId == null) {
            showToast("Фонарик не поддерживается устройством.");
            return 0;
        }
        if (textOutput.length() == 0) {
            if (textInput.length() == 0) showToast("Сначала введите сообщение.");
            else showToast("Сначала переведите в азбуку Морзе.");
            return 0;
        }
        // Проверка разрешения на доступ к камере
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    100 // Код запроса функции translateInFlashes
            );
            return 0;
        }

        // Перед вспышками выключаем фонарик
        try {
            cameraManager.setTorchMode(cameraId, false);
            isFlashOn = false;
        } catch (Exception e) {
            showToast("Error");
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
                    // Пауза между знаками в слове - маленькая пауза
                    delay += SHORT_BEEP_LENGTH_MS * 2;
                    break;
                case '/':
                case '\n':
                    // Пауза между словами - маленькая пауза
                    delay += SHORT_BEEP_LENGTH_MS * 6;
                    break;
            }
        }
        return delay;
    }

    // Включение/выключение фонарика
    private void toggleFlashlight() {
        try {
            isFlashOn = !isFlashOn; // Переключаем состояние фонарика
            cameraManager.setTorchMode(cameraId, isFlashOn);
        } catch (Exception e) {
            showToast("Error");
        }
    }

    // Короткое всплывающее уведомление
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Разблокировка всех кнопок
    private void enableAllButtons() {
        for (View button : allButtons) {
            if (!(button instanceof Button)) {
                ((ImageButton) button).setImageAlpha(255);
                button.setEnabled(true);
            }
        }
    }

    // Блокировка всех кнопок
    private void disableAllButtons() {
        for (View button : allButtons) {
            if (!(button instanceof Button)) {
                ((ImageButton) button).setImageAlpha(40);
                button.setEnabled(false);
            }
        }
    }
    // Блокировка всех кнопок, кроме указанной
    private void disableAllButtonsExcept(View exceptionButton) {
        for (View button : allButtons) {
            if (!(button instanceof Button)) {
                ((ImageButton) button).setImageAlpha(40);
                button.setEnabled(button == exceptionButton);
            }
        }
    }

    // Сохранение данных перед пересозданием активности
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        savedInput = textInput.getText().toString();
        savedOutput = textOutput.getText().toString();
        outState.putString("textInput", savedInput);
        outState.putString("textOutput", savedOutput);
    }

    // Получение ответа на запрос о разрешениях
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // От функции translateInFlashes
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
            } else showToast("Разрешение камеры отклонено");
        }
        // Переход к MyCameraActivity
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this, MyCameraActivity.class);
                startActivity(intent);
            } else showToast("Разрешение камеры отклонено");
        }
    }
}
