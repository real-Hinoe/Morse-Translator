package com.example.morsetranslator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.GravityCompat;
import androidx.core.graphics.Insets;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.slider.Slider;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


public class MyCameraActivity extends CameraActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {
    private DrawerLayout drawerLayout;
    private CameraBridgeViewBase cameraView;     // Вид с камеры
    private Mat currentFrame;
    // Классификаторы
    private CascadeClassifier faceCascade;
    private CascadeClassifier eyeCascade;
    private Runnable pauseChecker;               // Для проверки пауз между вспышками
    public ImageButton drawerBtn, startStopBtn, backBtn, eyeBtn, switchCameraBtn, flashBtn;
    public Spinner languageSpinner;
    public Slider dotSlider, pauseSlider, imgBrightnessSlider;
    public Slider thresholdSlider, pixelBrightnessSlider, gammaSlider;
    public CheckBox analyticsCheckbox, translateMorseCheckbox;
    public TextView morseView, analyticsView;
    public TextView languageView, dotView, pauseView, imgBrightnessView;
    public TextView thresholdView, pixelBrightnessView, gammaView;
    public TextView thresholdLabel, pixelBrightnessLabel, gammaLabel;
    public LinearLayout thresholdSection, pixelBrightnessSection, gammaSection;
    private Rect[] facesToDraw, eyesToDraw = new Rect[0];
    private final ImageButton[] btnsToBlock = new ImageButton[4];
    private final List<String> morseSignals = new ArrayList<>(); // Список сигналов Морзе
    private final Handler handler = new Handler();
    private int cameraFacing = LENS_FACING_BACK; // Задняя камера по умолчанию
    private int cameraMode = READ_FLASHES;       // Распознавание вспышек по умолчанию
    private String currentLanguage = "RU";       // Текущий язык
    private float dotLength = 300;               // Мин. длина точки
    private float pauseLength = 1000;            // Длина паузы между сигналами
    private float imgBrightness = 0;             // Изменение яркости изображения
    private float threshold = 5;                 // Мин. количесто пикселей для регистрации вспышки
    private float minPixelBrightness = 254;      // Мин. яркость пикселя, чтобы тот считался ярким
    private float gammaValue = 3;                // Значение гаммы для гаммы-коррекции
    private long signalStartTime = 0;             // Время начала вспышки
    private int frameCounter = 0;                // Счетчик кадров
    private boolean detectionEnded, showAnalysis, startedDecoding, decodeSignals = false;
    private static final String TAG = "MyCameraActivity";
    private static final int LENS_FACING_BACK = 0;
    private static final int LENS_FACING_FRONT = 1;
    private static final int READ_FLASHES = 0;
    private static final int READ_BLINKS = 1;
    private static final int FRAME_SKIP = 5;       // Анализируем каждый 5-й кадр
    private static final Map<String, Character> RU_CHAR_MAP = new HashMap<>();
    static {
        RU_CHAR_MAP.put("·—", 'а');
        RU_CHAR_MAP.put("—···", 'б');
        RU_CHAR_MAP.put("·——", 'в');
        RU_CHAR_MAP.put("——·", 'г');
        RU_CHAR_MAP.put("—··", 'д');
        RU_CHAR_MAP.put("·", 'е');
        RU_CHAR_MAP.put("···—", 'ж');
        RU_CHAR_MAP.put("——··", 'з');
        RU_CHAR_MAP.put("··", 'и');
        RU_CHAR_MAP.put("·———", 'й');
        RU_CHAR_MAP.put("—·—", 'к');
        RU_CHAR_MAP.put("·—··", 'л');
        RU_CHAR_MAP.put("——", 'м');
        RU_CHAR_MAP.put("—·", 'н');
        RU_CHAR_MAP.put("———", 'о');
        RU_CHAR_MAP.put("·——·", 'п');
        RU_CHAR_MAP.put("·—·", 'р');
        RU_CHAR_MAP.put("···", 'с');
        RU_CHAR_MAP.put("—", 'т');
        RU_CHAR_MAP.put("··—", 'у');
        RU_CHAR_MAP.put("··—·", 'ф');
        RU_CHAR_MAP.put("····", 'х');
        RU_CHAR_MAP.put("—·—·", 'ц');
        RU_CHAR_MAP.put("———·", 'ч');
        RU_CHAR_MAP.put("————", 'ш');
        RU_CHAR_MAP.put("——·—", 'щ');
        RU_CHAR_MAP.put("——·——", 'ъ');
        RU_CHAR_MAP.put("—·——", 'ы');
        RU_CHAR_MAP.put("—··—", 'ь');
        RU_CHAR_MAP.put("··—··", 'э');
        RU_CHAR_MAP.put("··——", 'ю');
        RU_CHAR_MAP.put("·—·—", 'я');

        RU_CHAR_MAP.put("·————", '1');
        RU_CHAR_MAP.put("··———", '2');
        RU_CHAR_MAP.put("···——", '3');
        RU_CHAR_MAP.put("····—", '4');
        RU_CHAR_MAP.put("·····", '5');
        RU_CHAR_MAP.put("—····", '6');
        RU_CHAR_MAP.put("——···", '7');
        RU_CHAR_MAP.put("———··", '8');
        RU_CHAR_MAP.put("————·", '9');
        RU_CHAR_MAP.put("—————", '0');
    }
    private static final Map<String, Character> EN_CHAR_MAP = new HashMap<>();
    static {
        EN_CHAR_MAP.put("·—", 'a');
        EN_CHAR_MAP.put("—···", 'b');
        EN_CHAR_MAP.put("—·—·", 'c');
        EN_CHAR_MAP.put("—··", 'd');
        EN_CHAR_MAP.put("·", 'e');
        EN_CHAR_MAP.put("··—·", 'f');
        EN_CHAR_MAP.put("——·", 'g');
        EN_CHAR_MAP.put("····", 'h');
        EN_CHAR_MAP.put("··", 'i');
        EN_CHAR_MAP.put("·———", 'j');
        EN_CHAR_MAP.put("—·—", 'k');
        EN_CHAR_MAP.put("·—··", 'l');
        EN_CHAR_MAP.put("——", 'm');
        EN_CHAR_MAP.put("—·", 'n');
        EN_CHAR_MAP.put("———", 'o');
        EN_CHAR_MAP.put("·——·", 'p');
        EN_CHAR_MAP.put("——·—", 'q');
        EN_CHAR_MAP.put("·—·", 'r');
        EN_CHAR_MAP.put("···", 's');
        EN_CHAR_MAP.put("—", 't');
        EN_CHAR_MAP.put("··—", 'u');
        EN_CHAR_MAP.put("···—", 'v');
        EN_CHAR_MAP.put("·——", 'w');
        EN_CHAR_MAP.put("—··—", 'x');
        EN_CHAR_MAP.put("—·——", 'y');
        EN_CHAR_MAP.put("——··", 'z');

        EN_CHAR_MAP.put("·————", '1');
        EN_CHAR_MAP.put("··———", '2');
        EN_CHAR_MAP.put("···——", '3');
        EN_CHAR_MAP.put("····—", '4');
        EN_CHAR_MAP.put("·····", '5');
        EN_CHAR_MAP.put("—····", '6');
        EN_CHAR_MAP.put("——···", '7');
        EN_CHAR_MAP.put("———··", '8');
        EN_CHAR_MAP.put("————·", '9');
        EN_CHAR_MAP.put("—————", '0');
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.camera), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        drawerLayout = findViewById(R.id.drawer_layout);             // Настройка Navigation Drawer
        NavigationView navigationView = findViewById(R.id.nav_view); // Находим NavigationView
        View headerView = navigationView.getHeaderView(0);           // Получаем headerView

        // Инициализация OpenCV
        if (OpenCVLoader.initLocal()) Log.d(TAG, "OpenCV загружен успешно");
        else {
            Log.e(TAG, "Не удалось загрузить OpenCV локально");
            Toast.makeText(this, "Ошибка загрузки OpenCV", Toast.LENGTH_SHORT).show();
            finish();
        }
        cameraView = findViewById(R.id.cameraView);
        drawerBtn = findViewById(R.id.drawerButton);
        startStopBtn = findViewById(R.id.startStop);
        backBtn = findViewById(R.id.backButton);
        eyeBtn = findViewById(R.id.eyeButton);
        switchCameraBtn = findViewById(R.id.switchCameraButton);
        flashBtn = findViewById(R.id.flashButton);
        morseView = findViewById(R.id.morseView);
        analyticsView = findViewById(R.id.analyticsView);

        // Открытие Navigation Drawer при нажатии на кнопку меню
        drawerBtn.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        startStopBtn.setOnClickListener(v -> startStopToggle());
        backBtn.setOnClickListener(v -> {
            if (startedDecoding) startStopToggle();
            finish();
        });
        eyeBtn.setOnClickListener(v -> changeModeTo(READ_BLINKS));
        switchCameraBtn.setOnClickListener(v -> switchCamera());
        flashBtn.setOnClickListener(v -> changeModeTo(READ_FLASHES));

        btnsToBlock[0] = drawerBtn;
        btnsToBlock[1] = eyeBtn;
        btnsToBlock[2] = switchCameraBtn;
        btnsToBlock[3] = flashBtn;

        languageView = headerView.findViewById(R.id.languageValue);
        dotView = headerView.findViewById(R.id.dotLengthValue);
        pauseView = headerView.findViewById(R.id.pauseLengthValue);
        imgBrightnessView = headerView.findViewById(R.id.imgBrightnessValue);
        thresholdView = headerView.findViewById(R.id.thresholdValue);
        pixelBrightnessView = headerView.findViewById(R.id.pixelBrightnessValue);
        gammaView = headerView.findViewById(R.id.gammaValue);

        languageSpinner = headerView.findViewById(R.id.languageSpinner);
        dotSlider = headerView.findViewById(R.id.dotLengthSlider);
        pauseSlider = headerView.findViewById(R.id.pauseLengthSlider);
        imgBrightnessSlider = headerView.findViewById(R.id.imgBrightnessSlider);
        thresholdSlider = headerView.findViewById(R.id.thresholdSlider);
        pixelBrightnessSlider = headerView.findViewById(R.id.pixelBrightnessSlider);
        gammaSlider = headerView.findViewById(R.id.gammaSlider);
        analyticsCheckbox = headerView.findViewById(R.id.analyticsCheckbox);
        translateMorseCheckbox = headerView.findViewById(R.id.translateMorseCheckbox);

        thresholdLabel = headerView.findViewById(R.id.thresholdLabel);
        pixelBrightnessLabel = headerView.findViewById(R.id.pixelBrightnessLabel);
        gammaLabel = headerView.findViewById(R.id.gammaLabel);
        thresholdSection = headerView.findViewById(R.id.thresholdSection);
        pixelBrightnessSection = headerView.findViewById(R.id.pixelBrightnessSection);
        gammaSection = headerView.findViewById(R.id.gammaSection);

        // Создаем адаптер для Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.languages,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
        languageSpinner.setSelection(0); // Значение по умолчанию
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // Обработка выбора элемента
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                languageView.setText(selectedItem);
                languageSpinner.setSelection(position);
                currentLanguage = selectedItem;
            }
            // Ничего не выбрано
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        dotSlider.addOnChangeListener((slider, value, fromUser) -> {
            dotView.setText(String.valueOf(Math.round(value)));
            dotLength = value;
        });
        pauseSlider.addOnChangeListener((slider, value, fromUser) -> {
            pauseView.setText(String.valueOf(Math.round(value * 10.0) / 10.0));
            pauseLength = value * 1000;
        });
        imgBrightnessSlider.addOnChangeListener((slider, value, fromUser) -> {
            imgBrightnessView.setText(String.valueOf(Math.round(value)));
            imgBrightness = value;
        });
        thresholdSlider.addOnChangeListener((slider, value, fromUser) -> {
            thresholdView.setText(String.valueOf(Math.round(value)));
            threshold = value * 1000;
        });
        pixelBrightnessSlider.addOnChangeListener((slider, value, fromUser) -> {
            pixelBrightnessView.setText(String.valueOf(Math.round(value)));
            minPixelBrightness = value;
        });
        gammaSlider.addOnChangeListener((slider, value, fromUser) -> {
            gammaView.setText(String.valueOf(Math.round(value * 10.0) / 10.0));
            gammaValue = value;
        });

        analyticsCheckbox.setChecked(false);
        analyticsCheckbox.setOnCheckedChangeListener((compoundButton, b) -> {
            analyticsCheckbox.setSelected(b);
            showAnalysis = !showAnalysis;
            runOnUiThread(() -> {
                if (showAnalysis && cameraMode == READ_FLASHES) {
                    analyticsView.setVisibility(View.VISIBLE);
                } else analyticsView.setVisibility(View.INVISIBLE);
            });
        });
        translateMorseCheckbox.setOnCheckedChangeListener((compoundButton, b) -> {
            translateMorseCheckbox.setSelected(b);
            decodeSignals = !decodeSignals;
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraView.setMaxFrameSize(640, 480);     // Разрешение 640x480
        cameraView.setCvCameraViewListener(this); // Устанавливаем слушатель для обработки кадров

        loadCascadeClassifiers();                 // Загрузка классификаторов
        pauseChecker = this::checkPause;          // Проверка пауз
    }

    // При открытии свернутого приложения
    @Override
    protected void onResume() {
        super.onResume();
        if (cameraView != null) cameraView.enableView();
    }

    // При сворачивании приложения
    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) cameraView.disableView();
    }

    // При выходе из приложения
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) cameraView.disableView(); // Освобождаем ресурсы камеры
    }

    // Если Navigation Drawer открыт, закрыть его
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Сменить камеру с задней на переднюю и наоборот
    private void switchCamera() {
        if (cameraFacing == LENS_FACING_BACK) cameraFacing = LENS_FACING_FRONT;
        else cameraFacing = LENS_FACING_BACK;
        cameraView.disableView();                // Отключаем текущую камеру
        cameraView.setCameraIndex(cameraFacing); // Устанавливаем новую камеру
        cameraView.enableView();                 // Включаем новую камеру
    }

    // Изменение режима распознавания
    private void changeModeTo(int mode) {
        if (mode == cameraMode) return;
        if (mode == READ_FLASHES) {
            cameraMode = READ_FLASHES;
            runOnUiThread(() -> {
                if (showAnalysis) analyticsView.setVisibility(View.VISIBLE);
                thresholdLabel.setVisibility(View.VISIBLE);
                pixelBrightnessLabel.setVisibility(View.VISIBLE);
                gammaLabel.setVisibility(View.VISIBLE);
                thresholdSection.setVisibility(View.VISIBLE);
                pixelBrightnessSection.setVisibility(View.VISIBLE);
                gammaSection.setVisibility(View.VISIBLE);
            });
        } else if (mode == READ_BLINKS) {
            cameraMode = READ_BLINKS;
            runOnUiThread(() -> {
                analyticsView.setVisibility(View.INVISIBLE);
                thresholdLabel.setVisibility(View.GONE);
                pixelBrightnessLabel.setVisibility(View.GONE);
                gammaLabel.setVisibility(View.GONE);
                thresholdSection.setVisibility(View.GONE);
                pixelBrightnessSection.setVisibility(View.GONE);
                gammaSection.setVisibility(View.GONE);
            });
        }
    }

    // Начало/остановка распознавания сигналов
    private void startStopToggle() {
        startedDecoding = !startedDecoding;
        if (startedDecoding) {
            startStopBtn.setImageResource(R.drawable.stop);
            runOnUiThread(() -> morseView.setVisibility(View.VISIBLE));
            detectionEnded = false;
            disableButtons();
        } else {
            startStopBtn.setImageResource(R.drawable.start);
            runOnUiThread(() -> morseView.setVisibility(View.INVISIBLE));
            morseView.setText("");
            morseSignals.clear();
            enableButtons();
        }
    }

    // Изменение яркости изображения
    private void adjustBrightness(Mat frame, int value) {
        // Создаем матрицу с константным значением яркости
        Scalar brightnessScalar = new Scalar(value, value, value, 0);
        Core.add(frame, brightnessScalar, frame); // Изменяем яркость
    }

    // Если пауза длится долго, завершаем текущий символ или слово
    private void checkPause() {
        if (!detectionEnded) decodeMorse();
    }

    // Добавить считанный с камеры сигнал
    private void addMorseSignal(long duration) {
        if (duration <= dotLength) morseSignals.add("·"); // Короткий сигнал - точка
        else morseSignals.add("—");                       // Длинный сигнал - тире
    }

    // Расшифровать получившееся сообщение
    private void decodeMorse() {
        if (!morseSignals.isEmpty()) {
            StringBuilder morseCode = new StringBuilder();
            for (String signal : morseSignals) morseCode.append(signal);

            if (decodeSignals) {
                // Декодируем символ
                char decodedChar;
                if (currentLanguage.equals("RU")) {
                    decodedChar = RU_CHAR_MAP.getOrDefault(morseCode.toString(), '?');
                } else if (currentLanguage.equals("EN")) {
                    decodedChar = EN_CHAR_MAP.getOrDefault(morseCode.toString(), '?');
                } else decodedChar = '?';
                // Выводим символ на экран
                if (morseCode.length() > 0) morseView.append(String.valueOf(decodedChar));
            } else morseView.append(morseCode);
            morseSignals.clear(); // Очищаем список сигналов
        }
    }

    // Распознавание ярких вспышек
    private Mat detectBrightFlashes(Mat originalFrame) {
        // Инциализация таблицы поиска для гамма-коррекции
        Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
        byte[] lut = new byte[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = (byte) Math.min(255, Math.pow(i / 255.0, gammaValue) * 255.0);
        }
        lookUpTable.put(0, 0, lut);

        Mat frame = new Mat();
        originalFrame.copyTo(frame);         // Копируем оригинальный кадр для вывода

        Core.LUT(frame, lookUpTable, frame); // Применение гамма-коррекции к кадру

        Mat gray = new Mat();
        Mat thresholded = new Mat();
        // Преобразуем изображение в градации серого
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY);
        // Применяем пороговое значение для выявления ярких областей
        Imgproc.threshold(gray, thresholded, minPixelBrightness, 255, Imgproc.THRESH_BINARY);

        // Подсчитываем яркие пиксели
        int brightPixelCount = Core.countNonZero(thresholded);
        // Если количество ярких пикселей превышает порог, считаем это вспышкой
        boolean currentFrameDetected = brightPixelCount > threshold;

        if (showAnalysis && frameCounter % FRAME_SKIP == 0) {
            String str = getResources().getString(R.string.analytics_hint);
            String displayedText = str.substring(0, str.length() - 1) + brightPixelCount;
            if (currentFrameDetected) displayedText += " DETECTED!";
            String finalDisplayedText = displayedText;
            runOnUiThread(() -> analyticsView.setText(finalDisplayedText));
        }

        if (startedDecoding) {
            if (currentFrameDetected && !detectionEnded) {
                signalStartTime = System.currentTimeMillis(); // Начало нового сигнала
                detectionEnded = true;
            } else if (!currentFrameDetected && detectionEnded) {
                long flashDuration = System.currentTimeMillis() - signalStartTime; // Конец сигнала
                detectionEnded = false;
                addMorseSignal(flashDuration);
                handler.removeCallbacksAndMessages(null);     // Убираем метку прошлой паузы
                // Запускаем проверку паузы
                handler.postDelayed(pauseChecker, Math.round(pauseLength));
                // runOnUiThread(() -> morseView.append(flashDuration + " "));
            }
        }
        // Освобождаем ресурсы
        gray.release();
        return thresholded;
    }

    // Распознавание лиц и глаз
    private void detectFacesAndEyes(Mat frame) {
        Mat grayFrame = new Mat();
        // Преобразуем в градации серого
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_RGBA2GRAY);

        // Обнаружение лиц
        MatOfRect faces = new MatOfRect();
        MatOfRect eyes = new MatOfRect();
        faceCascade.detectMultiScale(
                grayFrame,
                faces,
                1.3, // scaleFactor: меньшее значение увеличивает точность, но замедляет обработку
                5    // minNeighbors: большее значение уменьшает количество ложных срабатываний
        );

        // Для обнаруженных лиц ищем глаза
        for (Rect face : faces.toArray()) {
            Mat faceROI = grayFrame
                    .colRange(face.x, face.x + face.width)
                    .rowRange(face.y, (int) (face.y + face.height * 0.5));
            eyeCascade.detectMultiScale(faceROI, eyes);
        }

        facesToDraw = faces.toArray();
        eyesToDraw = eyes.toArray();

        if (startedDecoding) {
            if (eyesToDraw.length == 0 && !detectionEnded) {
                signalStartTime = System.currentTimeMillis(); // Начало нового сигнала
                detectionEnded = true;
            } else if (eyesToDraw.length > 0 && detectionEnded) {
                long flashDuration = System.currentTimeMillis() - signalStartTime; // Конец сигнала
                detectionEnded = false;
                addMorseSignal(flashDuration);
                handler.removeCallbacksAndMessages(null);     // Убираем метку прошлой паузы
                // Запускаем проверку паузы
                handler.postDelayed(pauseChecker, Math.round(pauseLength));
                // runOnUiThread(() -> morseView.append(flashDuration + " "));
            }
        }

        grayFrame.release();
    }

    // Рисование лиц и глаз
    private void drawFacesAndEyes(Mat frame) {
        for (Rect face : facesToDraw){
            Imgproc.rectangle(frame, face.tl(), face.br(), new Scalar(0, 0, 255), 2);
            for (Rect eye : eyesToDraw) {
                Imgproc.rectangle(
                        frame,
                        new Point(face.x + eye.x, face.y + eye.y),
                        new Point(face.x + eye.x + eye.width, face.y + eye.y + eye.height),
                        new Scalar(0, 255, 0), 2);
            }
        }
    }

    // Загрузка классификаторов для лица и глаз
    private void loadCascadeClassifiers() {
        try {
            // Загрузка классификатора для лиц
            InputStream faceInputStream = getResources().openRawResource(
                    R.raw.haarcascade_frontalface_default
            );
            File faceCascadeFile = new File(getCacheDir(), "haarcascade_frontalface_default.xml");
            FileOutputStream faceOutputStream = new FileOutputStream(faceCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = faceInputStream.read(buffer)) != -1) {
                faceOutputStream.write(buffer, 0, bytesRead);
            }
            faceInputStream.close();
            faceOutputStream.close();
            faceCascade = new CascadeClassifier(faceCascadeFile.getAbsolutePath());
            if (faceCascade.empty()) {
                Log.e(TAG, "Не удалось загрузить классификатор лиц");
            }

            // Загрузка классификатора для глаз
            InputStream eyeInputStream = getResources().openRawResource(
                    R.raw.haarcascade_eye
            );
            File eyeCascadeFile = new File(getCacheDir(), "haarcascade_eye.xml");
            FileOutputStream eyeOutputStream = new FileOutputStream(eyeCascadeFile);
            while ((bytesRead = eyeInputStream.read(buffer)) != -1) {
                eyeOutputStream.write(buffer, 0, bytesRead);
            }
            eyeInputStream.close();
            eyeOutputStream.close();
            eyeCascade = new CascadeClassifier(eyeCascadeFile.getAbsolutePath());
            if (eyeCascade.empty()) {
                Log.e(TAG, "Не удалось загрузить классификатор глаз");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки классификаторов", e);
        }
    }

    // Блокировка кнопок во время распознавания
    private void disableButtons() {
        for (ImageButton button : btnsToBlock) {
            button.setImageAlpha(255);
            button.setEnabled(true);
        }
    }

    // Разблокировка кнопок после распознавания
    private void enableButtons() {
        for (ImageButton button : btnsToBlock) {
            button.setImageAlpha(255);
            button.setEnabled(true);
        }
    }

    // При начале трансляции вида с камеры
    @Override
    public void onCameraViewStarted(int width, int height) {
        // Создаем матрицу для хранения кадров
        currentFrame = new Mat(height, width, CvType.CV_8UC4);
    }

    // По завершении трансляции вида с камеры
    @Override
    public void onCameraViewStopped() {
        if (currentFrame != null) currentFrame.release(); // Освобождаем ресурсы
    }

    // При каждом новом кадре
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        currentFrame = inputFrame.rgba(); // Получаем текущий кадр в формате RGBA
        frameCounter++;

        adjustBrightness(currentFrame, Math.round(imgBrightness)); // Изменяем яркость кадра

        if (cameraMode == READ_FLASHES) {
            // Возвращаем обработанный кадр
            if (showAnalysis) return detectBrightFlashes(currentFrame);
            // Кадр остается нетронутым
            else detectBrightFlashes(currentFrame);
        } else if (cameraMode == READ_BLINKS) {
            detectFacesAndEyes(currentFrame);
        }
        if (showAnalysis) drawFacesAndEyes(currentFrame);

        frameCounter = 0;
        return currentFrame;
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraView);
    }
}
