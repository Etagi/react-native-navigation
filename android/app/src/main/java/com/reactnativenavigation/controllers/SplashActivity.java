package com.reactnativenavigation.controllers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.reactnativenavigation.NavigationApplication;
import com.reactnativenavigation.react.*;
import com.reactnativenavigation.utils.CompatUtils;

public abstract class SplashActivity extends AppCompatActivity {
    public static boolean isResumed = false;

    private static final String MY_SETTINGS = "my_settings";
    SharedPreferences mSettings;

    public static void start(Activity activity) {
        Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (intent == null) return;
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = getSharedPreferences(MY_SETTINGS, Context.MODE_PRIVATE);

        LaunchArgs.instance.set(getIntent());
        setSplashLayout();
        IntentDataHandler.saveIntentData(getIntent());
    }

    public static boolean shouldAskPermission() {
        return Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(NavigationApplication.instance);
    }

    @TargetApi(23)
    public static void askOverlayPermission(Context context, String packageName) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + packageName));
        context.startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResumed = true;

        if (NavigationApplication.instance.getReactGateway().hasStartedCreatingContext()) {
            if (CompatUtils.isSplashOpenedOverNavigationActivity(this, getIntent())) {
                finish();
                return;
            }
            NavigationApplication.instance.getEventEmitter().sendAppLaunchedEvent();
            if (NavigationApplication.instance.clearHostOnActivityDestroy(this)) {
                overridePendingTransition(0, 0);
                finish();
            }
            return;
        }

        if (ReactDevPermission.shouldAskPermission()) {
            ReactDevPermission.askPermission(this);
            return;
        }

        //----------Permissions Overlay-------------------------------------
        SharedPreferences.Editor editor = mSettings.edit();
        boolean checkOverlayPerm = mSettings.getBoolean("hasOverlay", false);

        if (!checkOverlayPerm && shouldAskPermission()) {
            askOverlayPermission(this, getPackageName());

            editor.putBoolean("hasOverlay", true);
            editor.commit();
            return;
        }

        editor.putBoolean("hasOverlay", false);
        editor.commit();

        //----------EndPermissions-------------------------------------

        if (NavigationApplication.instance.isReactContextInitialized()) {
            NavigationApplication.instance.getEventEmitter().sendAppLaunchedEvent();
            return;
        }

        // TODO I'm starting to think this entire flow is incorrect and should be done in Application
        NavigationApplication.instance.startReactContextOnceInBackgroundAndExecuteJS();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResumed = false;
    }

    private void setSplashLayout() {
        final int splashLayout = getSplashLayout();
        if (splashLayout > 0) {
            setContentView(splashLayout);
        } else {
            setContentView(createSplashLayout());
        }
    }

    /**
     * @return xml layout res id
     */
    @LayoutRes
    public int getSplashLayout() {
        return 0;
    }

    /**
     * @return the layout you would like to show while react's js context loads
     */
    public View createSplashLayout() {
        View view = new View(this);
        view.setBackgroundColor(Color.WHITE);
        return view;
    }
}
