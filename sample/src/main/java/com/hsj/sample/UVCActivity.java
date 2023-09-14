package com.hsj.sample;

import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hsj.camera.UsbCameraManager;
import com.hsj.camera.V4L2Camera;
import com.hsj.camera.CameraView;
import com.hsj.camera.IFrameCallback;
import com.hsj.camera.IImageCaptureCallback;
import com.hsj.camera.IRender;
import com.hsj.camera.ISurfaceCallback;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @Author:Hsj
 * @Date:2021/5/10
 * @Class:MainActivity
 * @Desc:
 */
public final class UVCActivity extends AppCompatActivity implements ISurfaceCallback {

    private static final String TAG = "MainActivity";
    // Dialog checked index
    private int index;
    // V4L2Camera
    private V4L2Camera camera;
    // IRender
    private IRender render;
    private Surface surface;
    private LinearLayout ll;
    DebugTool debugTool;
    private int[][] supportFrameSize;
    static int curFrameSizeIndex = 1;
    static int[] curFrameSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uvc);
        findViewById(R.id.btn_create).setOnClickListener(v -> create());
        findViewById(R.id.btn_start).setOnClickListener(v -> start());
        findViewById(R.id.btn_stop).setOnClickListener(v -> stop());
        findViewById(R.id.btn_destroy).setOnClickListener(v -> destroy());
        findViewById(R.id.btnSize).setOnClickListener(v -> showCameraSizeChoiceDialog());
        ll = findViewById(R.id.ll);
        CameraView cameraView = findViewById(R.id.cameraView);
        this.render = cameraView.getRender(CameraView.COMMON);
        this.render.setSurfaceCallback(this);
        cameraView.surfaceCallback = new CameraView.SurfaceCallback() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initCamera();
                    }
                });
            }
        };

        debugTool = new DebugTool(findViewById(R.id.debugInfo));

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SampleActivity.mode = 2;
                finish();
            }
        });

        if (SampleActivity.mode == 1) {
            findViewById(R.id.btnBack).postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 8_000);
        }

//        //Request permission: /dev/video*
//        boolean ret = requestPermission();
//        showToast("Request permission: " + (ret ? "succeed" : "failed"));
    }

    private void initCamera() {
        create();
        start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (render != null) {
            render.onRender(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (render != null) {
            render.onRender(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroy();
    }

//==========================================Menu====================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_camera) {
            captureImage();
        }
        return super.onOptionsItemSelected(item);
    }

    private void captureImage() {
        if (camera != null) {
            String path = new StringBuilder().append(getExternalFilesDir(null)).append("/").append(System.currentTimeMillis()).append(".jpg").toString();
            camera.captureImage(path, new IImageCaptureCallback() {
                @Override
                public void onImageCapture(String filePath) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(UVCActivity.this, "已拍照：" + filePath, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

//===========================================Camera=================================================

    private void create() {
        if (this.camera == null) {
            List<UsbDevice> deviceList = UsbCameraManager.getUsbCameraDevices(this);
            if (deviceList.size() == 0) {
                showToast("未识别到摄像头");
                return;
            }
            V4L2Camera camera = UsbCameraManager.createUsbCamera(deviceList.get(0));
            if (camera != null) {
                supportFrameSize = camera.getSupportFrameSize();
                if (supportFrameSize == null || supportFrameSize.length == 0) {
                    showToast("Get support preview size failed.");
                } else {
                    curFrameSize = supportFrameSize[curFrameSizeIndex];
                    final int width = curFrameSize[0];
                    final int height = curFrameSize[1];
                    Log.i(TAG, "width=" + width + ", height=" + height);
                    camera.setFrameSize(width, height, V4L2Camera.FRAME_FORMAT_MJPEG);
                    this.camera = camera;
                }
            }
        } else {
            showToast("Camera had benn created");
        }
    }

    private void start() {
        if (this.camera != null) {
            if (surface != null) this.camera.setPreview(surface);
            this.camera.setFrameCallback(frameCallback);
            this.camera.start();
        } else {
            showToast("Camera have not create");
        }
    }

    private final IFrameCallback frameCallback = new IFrameCallback() {

        @Override
        public void onFrame(ByteBuffer data) {
            Log.i(TAG, "onFrame: ");
            debugTool.onDataCallback(data, 0, curFrameSize[0], curFrameSize[1]);
        }
    };

    private void stop() {
        if (this.camera != null) {
            this.camera.stop();
        }
    }

    private void destroy() {
        if (this.camera != null) {
            this.camera.destroy();
            this.camera = null;
        }
    }

//=============================================Other================================================

    private void showCameraSizeChoiceDialog() {
        if (supportFrameSize != null) {
            String[] items = new String[supportFrameSize.length];
            for (int i = 0; i < supportFrameSize.length; ++i) {
                items[i] = "" + supportFrameSize[i][0] + " x " + supportFrameSize[i][1];
            }
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setTitle(R.string.select_usb_device);
            ad.setSingleChoiceItems(items, curFrameSizeIndex, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    curFrameSizeIndex = which;
                }
            });
            ad.setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                finish();
                setResult(100);
            });
            ad.show();
        }
    }

    @Override
    public void onSurface(Surface surface) {
        if (surface == null) stop();
        this.surface = surface;
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}