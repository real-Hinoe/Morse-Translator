package com.example.morsetranslator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.annotation.NonNull;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


public class MyCameraActivity extends CameraActivity
        implements SensorEventListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase cameraView;     // Вид с камеры
    private Mat currentFrame, lookUpTable;
    // Классификаторы
    private CascadeClassifier faceCascade;
    private CascadeClassifier eyeCascade;
    private int cameraFacing = LENS_FACING_BACK; // Задняя камера по умолчанию
    private int cameraMode = READ_FLASHES;       // Распознавание вспышек по умолчанию
    private boolean isDetected, showAnalysis = false;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    public ImageButton backButton, eyeButton, switchCameraButton, flashButton, statusButton;
    private final View[] allViews = new View[5];  // Все объекты
    private final float[] gravity = new float[3]; // Координаты поворота смартфона в пространстве
    private static final String TAG = "MyCameraActivity";
    private static final int LENS_FACING_BACK = 0;
    private static final int LENS_FACING_FRONT = 1;
    private static final int READ_FLASHES = 0;
    private static final int READ_BLINKS = 1;
    private static final int THRESHOLD = 15_360;   // Сколько ярких пикселей нужно для вспышки
    // Переменные для хранения координат
    private Rect[] lastDetectedFaces = new Rect[0];
    private Rect[] lastDetectedEyes = new Rect[0];
    private int frameCounter = 0;
    private static final int FRAME_SKIP = 15;      // Анализируем каждый 15-й кадр
    private static final int FPS = 15;             // Количество кадров в секунду

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.camera), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Инициализация OpenCV
        if (OpenCVLoader.initLocal()) Log.d(TAG, "OpenCV загружен успешно");
        else {
            Log.e(TAG, "Не удалось загрузить OpenCV локально");
            showToast("Ошибка загрузки OpenCV");
            finish();
        }

        // Инициализация датчика
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Инциализация таблицы поиска для гамма-коррекции
        lookUpTable = new Mat(1, 256, CvType.CV_8U);
        byte[] lut = new byte[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = (byte) Math.min(255, Math.pow(i / 255.0, 10) * 255.0); // γ = 10
        }
        lookUpTable.put(0, 0, lut);

        cameraView = findViewById(R.id.cameraView);
        backButton = findViewById(R.id.backButton);
        eyeButton = findViewById(R.id.eyeButton);
        switchCameraButton = findViewById(R.id.switchCameraButton);
        flashButton = findViewById(R.id.flashButton);
        statusButton = findViewById(R.id.statusButton);

        allViews[0] = backButton;
        allViews[1] = eyeButton;
        allViews[2] = switchCameraButton;
        allViews[3] = flashButton;
        allViews[4] = statusButton;

        backButton.setOnClickListener(v -> finish());
        switchCameraButton.setOnClickListener(v -> switchCamera());
        flashButton.setOnClickListener(v -> changeMode(READ_FLASHES));
        eyeButton.setOnClickListener(v -> changeMode(READ_BLINKS));
        statusButton.setOnClickListener(v -> showAnalysis = !showAnalysis);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraView.setMaxFrameSize(640, 480);     // Разрешение 640x480
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            cameraView.setRequestedFrameRate(FPS);
        }
        cameraView.setCvCameraViewListener(this); // Устанавливаем слушатель для обработки кадров

        loadCascadeClassifiers();                 // Загрузка классификаторов
    }

    // При открытии свернутого приложения
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        if (cameraView != null) cameraView.enableView();
    }

    // При сворачивании приложения
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (cameraView != null) cameraView.disableView();
    }

    // При выходе из приложения
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) cameraView.disableView(); // Освобождаем ресурсы камеры
        if (lookUpTable != null) lookUpTable.release();   // Освобождаем ресурсы Mat
    }

    // Сменить камеру с задней на переднюю и наоборот
    private void switchCamera() {
        if (cameraFacing == LENS_FACING_BACK) cameraFacing = LENS_FACING_FRONT;
        else cameraFacing = LENS_FACING_BACK;
        cameraView.disableView();                // Отключаем текущую камеру
        cameraView.setCameraIndex(cameraFacing); // Устанавливаем новую камеру
        cameraView.enableView();                 // Включаем новую камеру
    }

    // Распознавание ярких вспышек
    private Mat detectBrightFlashes(Mat originalFrame) {
        Mat frame = new Mat();
        originalFrame.copyTo(frame);         // Копируем оригинальный кадр для вывода

        Core.LUT(frame, lookUpTable, frame); // Применение гамма-коррекции к кадру

        Mat gray = new Mat();
        Mat thresholded = new Mat();
        // Преобразуем изображение в градации серого
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY);
        // Применяем пороговое значение для выявления ярких областей
        Imgproc.threshold(gray, thresholded, 254, 255, Imgproc.THRESH_BINARY);

        // Подсчитываем яркие пиксели
        int brightPixelCount = Core.countNonZero(thresholded);
        // Если количество ярких пикселей превышает порог, считаем это вспышкой
        isDetected = brightPixelCount > THRESHOLD;
        runOnUiThread(this::changeStatus);

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
        faceCascade.detectMultiScale(
                grayFrame,
                faces,
                1.1, // scaleFactor: меньшее значение увеличивает точность, но замедляет обработку
                10   // minNeighbors: большее значение уменьшает количество ложных срабатываний
        );

        // Сохраняем координаты обнаруженных лиц
        Rect[] faceArray = faces.toArray();
        if (faceArray.length > 0) {
            lastDetectedFaces = faceArray;   // Берем обнаруженные лица
            isDetected = true;
        } else {
            lastDetectedFaces = new Rect[0]; // Если лица не найдено
            lastDetectedEyes = new Rect[0];  // Глаза тоже не найдены
            isDetected = false;
        }
        runOnUiThread(this::changeStatus);

        grayFrame.release();
    }

    // Рисование прямоугольников вокруг лиц и глаз
    private void drawFacesAndEyesRectangles(Mat frame) {
        // Рисуем прямоугольники вокруг лиц
        for (Rect face : lastDetectedFaces) {
            Imgproc.rectangle(frame, face.tl(), face.br(), new Scalar(0, 255, 0), 2);
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

    // Изменение изображения в случае распознавания
    private void changeStatus() {
        if (cameraMode == READ_FLASHES) {
            if (isDetected) statusButton.setImageResource(R.drawable.flash_on);
            else statusButton.setImageResource(R.drawable.flash_off);
        } else if (cameraMode == READ_BLINKS) {
            if (isDetected) statusButton.setImageResource(R.drawable.eye_opened);
            else statusButton.setImageResource(R.drawable.eye_closed);
        }
    }

    // Изменение режима распознавания
    private void changeMode(int mode) {
        if (mode == cameraMode) return;
        if (mode == READ_FLASHES) {
            cameraMode = READ_FLASHES;
            changeStatus();
        } else if (mode == READ_BLINKS) {
            cameraMode = READ_BLINKS;
            changeStatus();
        }
    }

    // Короткое всплывающее уведомление
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Вращение всех объектов
    private void rotateAllViews(float degrees) {
        for (View view : allViews) {
            view.animate()
                    .rotation(degrees)
                    .setDuration(10) // Длительность анимации (в миллисекундах)
                    .start();
        }
    }

    // Если смартфон повернулся в пространстве
    @Override
    public void onSensorChanged(@NonNull SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity[0] = event.values[0];
            gravity[1] = event.values[1];
            gravity[2] = event.values[2];

            // Определяем ориентацию устройства
            float x = gravity[0];
            float y = gravity[1];

            if (Math.abs(x) > Math.abs(y)) {
                if (x > 0) rotateAllViews(90); // Поворот вправо
                else rotateAllViews(-90);      // Поворот влево
            } else {
                if (y > 0) rotateAllViews(0);  // Исходная ориентация
                else rotateAllViews(180);      // Поворот вверх ногами
            }
        }
    }

    // Если точность акселерометра изменилась
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // При начале трансляции вида с камеры
    @Override
    public void onCameraViewStarted(int width, int height) {
        // Создаем матрицу для хранения кадров
        Log.d(TAG, "onCameraFrame вызван");
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
        currentFrame = inputFrame.rgba();            // Получаем текущий кадр в формате RGBA
        frameCounter++;

        if (cameraMode == READ_FLASHES) {
            // Возвращаем обработанный кадр
            if (showAnalysis) return detectBrightFlashes(currentFrame);
            // Кадр остается нетронутым
            else detectBrightFlashes(currentFrame);
        } else if (cameraMode == READ_BLINKS) {
            if (frameCounter % FRAME_SKIP == 0) detectFacesAndEyes(currentFrame);
            // Рисуем прямоугольники вокруг лица и глаз
            if (showAnalysis) drawFacesAndEyesRectangles(currentFrame);
        }
        return currentFrame;
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraView);
    }
}