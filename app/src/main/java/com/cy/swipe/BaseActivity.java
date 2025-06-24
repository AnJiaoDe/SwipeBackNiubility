package com.cy.swipe;

import android.content.Intent;
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
            public float getEdgeVSizeRatio() {
                return 0.15f;
            }

            @Override
            public void onEdgeTouched(int edgeTracking) {
                super.onEdgeTouched(edgeTracking);
                VibratorUtils.startVibrate(BaseActivity.this);
            }
        });
        swipeBackLayout.attachActivity(this);
    }
    @Override
    public void finish() {
        super.finish();
        swipeBackLayout.overrideFinishPendingTransition(this);
    }
}
