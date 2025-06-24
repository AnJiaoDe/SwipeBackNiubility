package com.cy.swipeback;

import android.app.Service;
import android.content.Context;
import android.os.Vibrator;

public class VibratorUtils {

    public static void startVibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        if(vibrator!=null)vibrator.vibrate(50);
    }

    public static void stopVibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        if(vibrator!=null)vibrator.cancel();
    }
}

