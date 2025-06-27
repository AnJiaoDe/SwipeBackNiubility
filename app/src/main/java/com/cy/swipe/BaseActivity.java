package com.cy.swipe;

import android.content.Context;
import android.content.Intent;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cy.swipeback.SwipeBackLayout;
import com.cy.swipeback.VibratorUtils;

/**
 * Created by lenovo on 2017/4/25.
 */

public abstract class BaseActivity extends AppCompatActivity {
    private SwipeBackLayout swipeBackLayout;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        swipeBackLayout = new SwipeBackLayout(this, new SwipeBackLayout.Callback() {
            //由于状态栏和导航栏，故而垂直方向有效触摸范围要设大点
            @Override
            public float getEdgeSizeVRatio() {
                return 0.1f;
            }

            @Override
            public float getEdgeSizeHRatio() {
                return 0.08f;
            }

            @Override
            public float getThresholdHRatio() {
                return super.getThresholdHRatio();
            }

            @Override
            public float getThresholdVRatio() {
                return super.getThresholdVRatio();
            }

            @Override
            public float getRadiusDrag(Context context) {
                return super.getRadiusDrag(context);
            }

            @Override
            public float getRadiusShadow(Context context) {
                return super.getRadiusShadow(context);
            }

            @Override
            public int getShadowColor() {
                return Color.BLACK;
            }

            @Override
            public BlurMaskFilter.Blur getShadowBlur() {
                return BlurMaskFilter.Blur.NORMAL;
            }

            @Override
            public void onEdgeTouched(int edgeTracking) {
                super.onEdgeTouched(edgeTracking);
                VibratorUtils.startVibrate(BaseActivity.this);
            }
        });
    }
    @Override
    public void finish() {
        super.finish();
        swipeBackLayout.overrideFinishPendingTransition();
    }
}
