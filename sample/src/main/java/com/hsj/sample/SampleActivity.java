package com.hsj.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.hsj.camera.UsbCameraManager;

/**
 * Created by HuangXin on 2023/6/27.
 */
public class SampleActivity extends Activity {

    private static final String TAG = "SampleActivity";

    public static int startCount = 0;
    public static boolean isPreview = false;
    public static int previewCount = 0;

    static int mode = 2;
    TextView textView1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        findViewById(R.id.finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });
        textView1 = findViewById(R.id.textView1);
        textView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = 1;
                startV4L2CameraActivity(0);
            }
        });
        findViewById(R.id.textView2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = 2;
                startActivity(new Intent(SampleActivity.this, UVCActivity.class));
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        if (mode == 1) {
            startV4L2CameraActivity(3_000);
        }
    }

    private void startV4L2CameraActivity(long delayMillis) {
        String msg = "循环开关摄像头（每10秒）测试\n打开次数 " + startCount + "，出流次数 " + previewCount;
        Log.i(TAG, msg);
        textView1.setText(msg);
        findViewById(R.id.root).postDelayed(new Runnable() {
            @Override
            public void run() {
                startCount++;
                isPreview = false;
                startActivity(new Intent(SampleActivity.this, UVCActivity.class));
            }
        }, delayMillis);
    }

    public static void onPreview() {
        if (mode == 1 && !isPreview) {
            isPreview = true;
            previewCount++;
        }
    }
}
