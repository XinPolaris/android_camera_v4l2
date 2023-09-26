package com.hsj.sample;

import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.hsj.camera.CameraView;
import com.hsj.camera.IFrameCallback;
import com.hsj.camera.IImageCaptureCallback;
import com.hsj.camera.IRender;
import com.hsj.camera.ISurfaceCallback;
import com.hsj.camera.UsbCameraManager;
import com.hsj.camera.V4L2Camera;
import com.hsj.sample.databinding.ActivityUvcBinding;

import java.io.File;
import java.nio.ByteBuffer;
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

    private static final String TAG = "UVCActivity";
    private ActivityUvcBinding binding;
    // V4L2Camera
    private V4L2Camera camera;
    // IRender
    private IRender render;
    private Surface surface;
    DebugTool debugTool;
    private int[][] supportFrameSize;
    private static int saveFrameSize = 1280 * 720;
    private int[] curFrameSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: " + SampleActivity.mode);
        binding = ActivityUvcBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        binding.btnSize.setOnClickListener(v -> showCameraSizeChoiceDialog());
        this.render = binding.cameraView.getRender(CameraView.COMMON);
        this.render.setSurfaceCallback(this);
        binding.cameraView.surfaceCallback = new CameraView.SurfaceCallback() {
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

        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SampleActivity.mode = 2;
                finish();
            }
        });

        if (SampleActivity.mode == 1) {
            binding.btnBack.setText("退出自动开关测试");
            binding.btnBack.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 8_000);
        } else {
            binding.btnBack.setText("退出长时间推流测试");
        }

//        //Request permission: /dev/video*
//        boolean ret = requestPermission();
//        showToast("Request permission: " + (ret ? "succeed" : "failed"));

        binding.ivCloseCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.ivCapture.setVisibility(View.GONE);
                binding.ivCloseCapture.setVisibility(View.GONE);
            }
        });
    }

    private void initCamera() {
        create();
        start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: " + SampleActivity.mode);
        if (render != null) {
            render.onRender(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
        if (render != null) {
            render.onRender(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: ");
        stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
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
                            binding.ivCapture.setImageURI(Uri.fromFile(new File(filePath)));
                            binding.ivCapture.setVisibility(View.VISIBLE);
                            binding.ivCloseCapture.setVisibility(View.VISIBLE);
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
                    curFrameSize = supportFrameSize[findSizeIndex(saveFrameSize)];
                    final int width = curFrameSize[0];
                    final int height = curFrameSize[1];
                    Log.i(TAG, "width=" + width + ", height=" + height);
                    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) binding.cameraView.getLayoutParams();
                    layoutParams.dimensionRatio = width + ":" + height;
                    binding.cameraView.setLayoutParams(layoutParams);
                    camera.setFrameSize(width, height, V4L2Camera.FRAME_FORMAT_MJPEG);
                    this.camera = camera;
                }
            } else {
                Log.e(TAG, "create camera: fail");
                showToast("摄像头创建失败");
            }
        }
    }

    private void start() {
        if (this.camera != null) {
            if (surface != null) this.camera.setPreview(surface);
            this.camera.setFrameCallback(frameCallback);
            this.camera.start();
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
            ad.setSingleChoiceItems(items, findSizeIndex(saveFrameSize), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    saveFrameSize = supportFrameSize[which][0] * supportFrameSize[which][1];
                }
            });
            ad.setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                finish();
                setResult(100);
            });
            ad.show();
        }
    }

    private int findSizeIndex(int sizeSum) {
        int sizeIndex = 0;
        for (int i = 0; i < supportFrameSize.length; i++) {
            if (supportFrameSize[i][0] * supportFrameSize[i][1] <= sizeSum) {
                sizeIndex = i;
            }
        }
        return sizeIndex;
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