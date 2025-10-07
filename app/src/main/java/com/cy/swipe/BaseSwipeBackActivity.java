package com.cy.swipe;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public abstract class BaseSwipeBackActivity extends Activity {

    private ImageView mBackgroundView;
    private View mContentView;
    private float mDownX;
    private boolean mIsSwiping = false;
    private static Bitmap sLastScreenshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wrapContentWithBackground();
    }

    /**
     * 设置内容视图，替代 setContentView()
     */
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        wrapContentWithBackground();
    }

    private void wrapContentWithBackground() {
        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        ViewGroup rootView = decorView.findViewById(android.R.id.content);

        if (rootView.getChildCount() == 0) return;

        mContentView = rootView.getChildAt(0);
        if (mBackgroundView == null) {
            mBackgroundView = new ImageView(this);
            mBackgroundView.setScaleType(ImageView.ScaleType.FIT_XY);
            if (sLastScreenshot != null) {
                mBackgroundView.setImageBitmap(sLastScreenshot);
            }
            // 半透明遮罩层，可选
            mBackgroundView.setColorFilter(Color.argb(100, 0, 0, 0));
            rootView.addView(mBackgroundView, 0,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

//        mContentView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return handleSwipeBack(event);
//            }
//        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getX() - mDownX;
//                if (moveX > 0) {
                mIsSwiping = true;
                mContentView.setTranslationX(moveX);
//                mBackgroundView.setAlpha(1 - moveX / mContentView.getWidth());
//                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mIsSwiping) {
                    float totalMoveX = event.getX() - mDownX;
                    if (totalMoveX > mContentView.getWidth() / 3) {
                        finishWithSwipeAnimation(totalMoveX);
                    } else {
                        cancelSwipeBack();
                    }
                }
                mIsSwiping = false;
               break;
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean handleSwipeBack(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                return true;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getX() - mDownX;
//                if (moveX > 0) {
                    mIsSwiping = true;
                    mContentView.setTranslationX(moveX);
                    mBackgroundView.setAlpha(1 - moveX / mContentView.getWidth());
//                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mIsSwiping) {
                    float totalMoveX = event.getX() - mDownX;
                    if (totalMoveX > mContentView.getWidth() / 3) {
                        finishWithSwipeAnimation(totalMoveX);
                    } else {
                        cancelSwipeBack();
                    }
                }
                mIsSwiping = false;
                return true;
        }
        return false;
    }

    private void cancelSwipeBack() {
        mContentView.animate()
                .translationX(0)
                .setDuration(150)
                .withEndAction(() -> mBackgroundView.setAlpha(1f))
                .start();
    }

    private void finishWithSwipeAnimation(float currentX) {
        mContentView.animate()
                .translationX(mContentView.getWidth())
                .setDuration(200)
                .withEndAction(() -> {
                    finish();
                    overridePendingTransition(0, 0);
                })
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearLastScreenshot();
    }

    /**
     * 调用此方法启动下一个支持滑动返回的页面
     */
    public void startSwipeActivity(Intent intent) {
        sLastScreenshot = captureScreen(this);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private static Bitmap captureScreen(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        decor.setDrawingCacheEnabled(true);
        decor.buildDrawingCache();
        Bitmap bmp = Bitmap.createBitmap(decor.getDrawingCache());
        decor.setDrawingCacheEnabled(false);
        return bmp;
    }

    private static void clearLastScreenshot() {
        if (sLastScreenshot != null && !sLastScreenshot.isRecycled()) {
            sLastScreenshot.recycle();
            sLastScreenshot = null;
        }
    }
}
