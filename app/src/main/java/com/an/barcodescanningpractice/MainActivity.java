package com.an.barcodescanningpractice;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Constants
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_RUNTIME_PERMISSION_CAMERA = 400;

    // UI Components
    private LinearLayout mLlLayoutBase;
    private SurfaceView mSvCameraView;
    private TextView mTvScannedCode;
    private Button mBtnClearScannedCode, mBtnFlashLight;

    // Other Objects
    private CameraSource mCameraSource;
    private Camera mCamera;
    private boolean mFlashMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        requestRunTimePermissionForAccessCamera();
    }

    private void initView() {
        mLlLayoutBase = findViewById(R.id.activity_main_ll_layout_base);

        mSvCameraView = findViewById(R.id.activity_main_sv_camera_view);

        mTvScannedCode = findViewById(R.id.activity_main_tv_scanned_code);

        mBtnClearScannedCode = findViewById(R.id.activity_main_btn_clear_scanned_code);
        mBtnClearScannedCode.setOnClickListener(this);
        mBtnFlashLight = findViewById(R.id.activity_main_btn_flash_light);
        mBtnFlashLight.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_main_btn_clear_scanned_code:
                clearScannedData();
                break;
            case R.id.activity_main_btn_flash_light:
                flashOnButton();
                break;
            default:
                break;
        }
    }

    // request runtime permissions for camera
    private void requestRunTimePermissionForAccessCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Snackbar.make(mLlLayoutBase, "Need this permission for access camera", Snackbar.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_RUNTIME_PERMISSION_CAMERA);
        } else {
            setUpQRScanner();
        }
    }

    // handle runtime permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "----> onRequestPermissionResult is called <----");
        switch (requestCode) {
            case REQUEST_RUNTIME_PERMISSION_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "----> Camera permission granted <----");
                    setUpQRScanner();
                } else {
                    Log.i(TAG, "----> Camera permission denied <----");
                    boolean showRational = ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA);
                    if (showRational) {
                        Log.i(TAG, "----> Checkbox unchecked ---->");
                        requestRunTimePermissionForAccessCamera();
                    } else {
                        Log.i(TAG, "----> Checkbox checked ---->");
                        Toast.makeText(MainActivity.this, "Need Camera permission", Toast.LENGTH_LONG).show();
                        MainActivity.this.finish();
                    }
                }
            }
            break;
            default:
                break;
        }
    }

    // setting up camera to scan barcode or QR
    private void setUpQRScanner() {
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        mCameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
                .build();

        Log.d(TAG, "+---------> setUpQRScanner <---------+");
        mSvCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    Log.d(TAG, "+---------> surfaceCreated <---------+");
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    Log.d(TAG, "+---------> start <---------+");
                    mCameraSource.start(mSvCameraView.getHolder());

                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() != 0) {
                    Log.d(TAG, barcodes.valueAt(0).displayValue);
                    mTvScannedCode.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                clearScannedData();
                                String scannedValue = barcodes.valueAt(0).displayValue;

                                onScannedComplete(scannedValue);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    private void onScannedComplete(String scannedData) {
        mTvScannedCode.setText(scannedData);
        mCameraSource.stop();
        mBtnFlashLight.setText(getResources().getString(R.string.activity_main_flash_on_text));
    }

    // switch on / off flash light
    private void flashOnButton() {
        mCamera = getCamera(mCameraSource);
        if (mCamera != null) {
            try {
                android.hardware.Camera.Parameters param = mCamera.getParameters();
                param.setFlashMode(!mFlashMode ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(param);
                mFlashMode = !mFlashMode;
                if (mFlashMode) {
                    mBtnFlashLight.setText(getResources().getString(R.string.activity_main_flash_off_text));
                } else {
                    mBtnFlashLight.setText(getResources().getString(R.string.activity_main_flash_on_text));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void clearScannedData() {
        mTvScannedCode.setText("");
        mTvScannedCode.setHint(getResources().getString(R.string.activity_main_scanned_code_text));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            mCameraSource.start(mSvCameraView.getHolder());
        } catch (Exception ex) {
            ex.printStackTrace();
            Snackbar.make(mLlLayoutBase, "Camera is not working", Snackbar.LENGTH_LONG).show();
        }
    }

    // get camera from camera source
    private static Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }
                    return null;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return null;
    }
}