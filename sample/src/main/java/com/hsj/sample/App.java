package com.hsj.sample;

import android.app.Application;

import com.fotile.fiks.flog.Flog;

/**
 * Created by HuangXin on 2023/6/29.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Flog.init(this, true);
    }
}
