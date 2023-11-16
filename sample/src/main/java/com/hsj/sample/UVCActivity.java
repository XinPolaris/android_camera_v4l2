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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
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
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

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
    private static int saveRenderType = 0;
    private int[] curFrameSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: " + SampleActivity.mode);
        binding = ActivityUvcBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        binding.btnSize.setOnClickListener(v -> showCameraSizeChoiceDialog());
        this.render = binding.cameraView.getRender(saveRenderType);
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
        binding.saveYUV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileName = getYUVFilePath();
                saveYUV = true;
            }
        });
        binding.renderMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] items = new String[]{"COMMON", "BEAUTY", "DEPTH"};
                AlertDialog.Builder ad = new AlertDialog.Builder(UVCActivity.this);
                ad.setTitle("RenderType");
                ad.setSingleChoiceItems(items, saveRenderType, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveRenderType = which;
                    }
                });
                ad.setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    finish();
                });
                ad.show();
            }
        });
        binding.btnCaptureLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureLoop();
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

    private Timer captureLoopTimer;
    private int captureLoop = 0;
    private void captureLoop() {
        if (captureLoopTimer != null) {
            captureLoopTimer.cancel();
            showToast("循环截图已停止");
        } else {
            showToast("循环截图已启动");
            captureLoop = 0;
            captureLoopTimer = new Timer();
            captureLoopTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (camera != null) {
                        captureLoop++;
                        File file = new File(getExternalFilesDir(null) + "/captureLoop.jpg");
                        file.delete();
                        Log.i(TAG, captureLoop + " onImageCapture start: " + file.getAbsolutePath());
                        camera.captureImage(file.getAbsolutePath(), new IImageCaptureCallback() {
                            @Override
                            public void onImageCapture(String filePath) {
                                Log.i(TAG, captureLoop + " onImageCapture end: " + filePath);
                            }
                        });
                    }
                }
            }, 0, 100);
        }
    }

    private void captureImage() {
        if (camera != null) {
            String path = getExternalFilesDir(null) + "/" + System.currentTimeMillis() + ".jpg";
            camera.captureImage(path, new IImageCaptureCallback() {
                @Override
                public void onImageCapture(String filePath) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.ivCapture.setImageURI(Uri.fromFile(new File(filePath)));
//                            binding.ivCapture.setVisibility(View.VISIBLE);
//                            binding.ivCloseCapture.setVisibility(View.VISIBLE);
                            Toast.makeText(UVCActivity.this, "已拍照：" + filePath, Toast.LENGTH_SHORT).show();

                            captureAnimate();
                        }
                    });
                }
            });
        }
    }

    private void captureAnimate() {
        binding.ivCapture.clearAnimation();

        AnimationSet animationSet = new AnimationSet(false);

        AlphaAnimation alphaAnimation1 = new AlphaAnimation(0f, 1f);
        alphaAnimation1.setDuration(300);
        alphaAnimation1.setFillAfter(true);
        float scale = binding.viewDismiss.getWidth() / (float)binding.ivCapture.getWidth();
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f,
                scale,
                1.0f,
                scale,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        );
        TranslateAnimation translateAnimation = new TranslateAnimation(
                0f,
                (binding.viewDismiss.getLeft() + binding.viewDismiss.getWidth()/2f) -(binding.ivCapture.getLeft() + binding.ivCapture.getWidth()/2f),
                0f,
                (binding.viewDismiss.getTop() + binding.viewDismiss.getHeight()/2f) - (binding.ivCapture.getTop() + binding.ivCapture.getHeight()/2f)
        );
        AnimationSet animationSet2 =new AnimationSet(false);
        animationSet2.setInterpolator(new DecelerateInterpolator());
        animationSet2.addAnimation(scaleAnimation);
        animationSet2.addAnimation(translateAnimation);
        animationSet2.setDuration(2000);
        animationSet2.setFillAfter(true);
        animationSet2.setStartOffset(alphaAnimation1.getDuration());

        AlphaAnimation alphaAnimation2 = new  AlphaAnimation(1f, 0f);
        alphaAnimation2.setStartOffset(animationSet2.getDuration() * 2);
        alphaAnimation2.setDuration(200);
        alphaAnimation2.setFillAfter(true);

        animationSet.addAnimation(alphaAnimation1);
        animationSet.addAnimation(animationSet2);
        animationSet.addAnimation(alphaAnimation2);
        binding.ivCapture.startAnimation(animationSet);
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

    boolean saveYUV = false;
    private SaveYUVThread saveYUVThread;
    int count = 0;
    String fileName = getYUVFilePath();
    private final IFrameCallback frameCallback = new IFrameCallback() {

        @Override
        public void onFrame(ByteBuffer data) {
            debugTool.onDataCallback(data, 0, curFrameSize[0], curFrameSize[1]);
            if (saveYUV) {
                count++;
                if (count < 20) {
                    if (saveYUVThread == null) {
                        saveYUVThread = new SaveYUVThread();
                        saveYUVThread.start();
                    }
                    saveYUVThread.queue.offer(data);
                } else {
                    saveYUV = false;
                    if (saveYUVThread != null) {
                        saveYUVThread.exitLoop();
                    }
                    saveYUVThread = null;
                    count = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast("写入完成");
                        }
                    });
                }
            }
        }
    };

    class SaveYUVThread extends Thread {
        private final LinkedBlockingQueue<ByteBuffer> queue = new LinkedBlockingQueue<>();
        private boolean exitLoop = false;

        @Override
        public void run() {
            super.run();
            while (!exitLoop) {
                while (queue.peek() != null) {
                    ByteBuffer buffer = queue.poll();
                    try {
                        File file = new File(fileName);
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        FileOutputStream fe = new FileOutputStream(file, true);
                        byte[] arr = new byte[buffer.remaining()];
                        buffer.get(arr);
                        fe.write(arr);
                        fe.flush();
                        fe.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void exitLoop() {
            exitLoop = true;
        }
    }

    private String getYUVFilePath() {
        return "/sdcard/" + System.currentTimeMillis() + "_1280x720.yuv";
    }

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