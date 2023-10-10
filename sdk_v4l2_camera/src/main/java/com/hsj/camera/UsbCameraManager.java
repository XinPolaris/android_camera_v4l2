package com.hsj.camera;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by HuangXin on 2023/9/14.
 */
public final class UsbCameraManager {

    private static final String TAG = "UsbCameraManager";

    public static List<UsbDevice> getUsbCameraDevices(Context context) {
        List<UsbDevice> cameras = new ArrayList<>();
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        Collection<UsbDevice> values = usbManager.getDeviceList().values();
        for (UsbDevice device : values) {
            if (isUsbCamera(device)) {
                cameras.add(device);
            }
        }
        if (cameras.size() == 0) {
            Log.e(TAG, "create camera: not find camera device");
        }
        return cameras;
    }

    public static V4L2Camera createUsbCamera(UsbDevice device) {
        if (!isUsbCamera(device)) {
            Log.e(TAG, "createUsbCamera: device not USB Camera");
            return null;
        }
        return createV4L2Camera(device.getProductId(), device.getVendorId());
    }

    public static V4L2Camera createV4L2Camera(int productId, int vendorId) {
        V4L2Camera camera = new V4L2Camera();
        boolean ret = camera.create(productId, vendorId);
        if (ret) {
            return camera;
        }
        return null;
    }

    /**
     * check is usb camera
     *
     * @param device usb device
     * @return result
     */
    public static boolean isUsbCamera(UsbDevice device) {
        boolean result = false;
        switch (device.getDeviceClass()) {
            case UsbConstants.USB_CLASS_VIDEO:
                result = true;
                break;
            case UsbConstants.USB_CLASS_MISC:
                for (int i = 0; i < device.getInterfaceCount(); i++) {
                    if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_VIDEO) {
                        result = true;
                        break;
                    }
                }
                break;
            default:
                break;
        }
        return result;
    }

    /**
     * request permission /dev/video*
     *
     * @return result
     */
    public static boolean requestDevVideoPermission() {
        boolean result;
        Process process = null;
        DataOutputStream dos = null;
        try {
            process = Runtime.getRuntime().exec("su");
            dos = new DataOutputStream(process.getOutputStream());
            dos.writeBytes("chmod 666 /dev/video*\n");
            dos.writeBytes("exit\n");
            dos.flush();
            result = (process.waitFor() == 0);
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            try {
                if (dos != null) {
                    dos.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "request video rw permission: " + result);
        return result;
    }

}
