package com.ttwishing.stretchablelistview.library;

import android.app.Application;
import android.content.Context;

/**
 * Created by kurt on 8/13/15.
 */
public abstract class App extends Application {

    private static App sApp;

    public App() {
        sApp = this;
    }

    public static Context getContext() {
        return sApp;
    }

    public static <T extends App> T getInstance() {
        return (T) sApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
