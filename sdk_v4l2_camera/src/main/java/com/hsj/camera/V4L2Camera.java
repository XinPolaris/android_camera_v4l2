package com.hsj.camera;

import android.util.Log;
import android.view.Surface;

/**
 * @Author:Hsj
 * @Date:2020-08-31
 * @Class:CameraAPI
 * @Desc:CameraAPI
 */
public final class V4L2Camera {

    private static final String TAG = "V4L2Camera";
    //FrameFormat
    public static final int FRAME_FORMAT_MJPEG = 0;
    public static final int FRAME_FORMAT_YUYV = 1;
    public static final int FRAME_FORMAT_DEPTH = 2;
    //Status
    private static final int STATUS_ERROR_DESTROYED = 50;
    private static final int STATUS_ERROR_OPEN = 40;
    private static final int STATUS_ERROR_SIZE = 30;
    private static final int STATUS_ERROR_START = 20;
    private static final int STATUS_ERROR_STOP = 10;
    private static final int STATUS_SUCCESS = 0;

    static {
        System.loadLibrary("camera");
    }

//======================================Java API====================================================

    private long nativeObj;

    public V4L2Camera() {
        this.nativeObj = nativeInit();
    }

    public synchronized boolean create(int productId, int vendorId) {
        if (this.nativeObj == 0) {
            Log.e(TAG, "Can't be call after call destroy");
            return false;
        } else {
            int status = nativeCreate(this.nativeObj, productId, vendorId);
            Log.i(TAG, "create: " + status);
            return STATUS_SUCCESS == status;
        }
    }

    public boolean setAutoExposure(boolean isAuto) {
        if (this.nativeObj == 0) {
            Log.w(TAG, "Can't be call after call destroy");
            return false;
        } else {
            int status = nativeAutoExposure(this.nativeObj, isAuto);
            Log.i(TAG, "setAutoExposure: " + status);
            return STATUS_SUCCESS == status;
        }
    }

    public boolean setExposureLevel(int level) {
        if (this.nativeObj == 0) {
            Log.w(TAG, "Can't be call after call destroy");
            return false;
        } else {
            int status = nativeSetExposure(this.nativeObj, level);
            Log.i(TAG, "setExposureLevel: " + status);
            return STATUS_SUCCESS == status;
        }
    }

    public int[][] getSupportFrameSize() {
        int[][] sizes = null;
        if (this.nativeObj != 0) {
            sizes = nativeSupportSize(this.nativeObj);
            int length = (sizes == null ? 0 : sizes.length);
            if (length > 0) {
                Log.i(TAG, "getSupportFrameSize: " + length);
            } else {
                Log.e(TAG, "getSupportFrameSize: empty");
            }
        } else {
            Log.e(TAG, "getSupportFrameSize: already destroyed");
        }
        return sizes;
    }

    public boolean setFrameSize(int width, int height, int frameFormat) {
        if (this.nativeObj == 0) {
            Log.w(TAG, "Can't be call after call destroy");
            return false;
        } else {
            int status = nativeFrameSize(this.nativeObj, width, height, frameFormat);
            Log.i(TAG, "setFrameSize: " + status);
            return STATUS_SUCCESS == status;
        }
    }

    public boolean setFrameCallback(IFrameCallback frameCallback) {
        if (this.nativeObj == 0) {
            Log.w(TAG, "Can't be call after call destroy");
            return false;
        } else {
            int status = nativeFrameCallback(this.nativeObj, frameCallback);
            Log.i(TAG, "setFrameCallback: " + status);
            return STATUS_SUCCESS == status;
        }
    }

    public boolean captureImage(String filePath, IImageCaptureCallback callback) {
        if (this.nativeObj == 0) {
            Log.w(TAG, "Can't be call after call destroy");
            return false;
        } else {
            int status = nativeCaptureImage(this.nativeObj, filePath, callback);
            Log.i(TAG, "imageCapture: " + status);
            return STATUS_SUCCESS == status;
        }
    }

    public boolean setPreview(Surface surface) {
        if (this.nativeObj == 0) {
            Log.w(TAG, "Can't be call after call setPreview");
            return false;
        } else {
            int status = nativePreview(this.nativeObj, surface);
            Log.i(TAG, "setPreview: " + status);
            return STATUS_SUCCESS == status;
        }
    }

    public synchronized boolean start() {
        if (this.nativeObj == 0) {
            Log.w(TAG, "Can't be call after call destroy");
            return false;
        } else {
            int status = nativeStart(this.nativeObj);
            Log.i(TAG, "start: " + status);
            return STATUS_SUCCESS == status;
        }
    }

    public synchronized boolean stop() {
        if (this.nativeObj == 0) {
            Log.w(TAG, "Can't be call after call destroy");
            return false;
        } else {
            int status = nativeStop(this.nativeObj);
            Log.i(TAG, "stop: " + status);
            return STATUS_SUCCESS == status;
        }
    }

    public synchronized void destroy() {
        if (this.nativeObj == 0) {
            Log.w(TAG, "destroy: already destroyed");
        } else {
            int status = nativeDestroy(this.nativeObj);
            Log.w(TAG, "destroy: " + status);
            this.nativeObj = 0;
        }
    }

//=======================================Native API=================================================

    private native long nativeInit();

    private native int nativeCreate(long nativeObj, int productId, int vendorId);

    private native int nativeAutoExposure(long nativeObj, boolean isAuto);

    private native int nativeSetExposure(long nativeObj, int level);

    private native int[][] nativeSupportSize(long nativeObj);

    private native int nativeFrameSize(long nativeObj, int width, int height, int pixelFormat);

    private native int nativeFrameCallback(long nativeObj, IFrameCallback frameCallback);

    private native int nativeCaptureImage(long nativeObj, String filePath, IImageCaptureCallback callback);

    private native int nativePreview(long nativeObj, Surface surface);

    private native int nativeStart(long nativeObj);

    private native int nativeStop(long nativeObj);

    private native int nativeDestroy(long nativeObj);

}
