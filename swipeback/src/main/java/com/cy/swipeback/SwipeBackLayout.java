package com.cy.swipeback;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

public class SwipeBackLayout extends FrameLayout {
    private final Paint paintShadow;
    private final Path pathClip;
    private View contentView;
    private float edgeVSize = -1;
    private float edgeHSize = -1;
    private RectF rectFEdgeLeft;
    private RectF rectFEdgeTop;
    private RectF rectFEdgeRight;
    private RectF rectFEdgeBottom;
    private float zoom = 1; // 缩放
    private float zoom_max = 10; // 缩放
    private float zoom_min = 0.1f; // 缩放
    private float translate_x, translate_y;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    public static final int STATE_IDLE = 0;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;
    private int dragState = STATE_IDLE;
    private boolean finishActivity = false;
    private float start_x;
    private float start_y;
    private float end_x;
    private float end_y;
    private boolean onScaling = false;
    private Activity activity;
    private VelocityTracker velocityTracker;
    private float maxVelocity;
    private float minVelocity;
    private float velocityThreshold;
    public static final int EDGE_LEFT = 1 << 0;
    public static final int EDGE_RIGHT = 1 << 1;
    public static final int EDGE_TOP = 1 << 2;
    public static final int EDGE_BOTTOM = 1 << 3;
    public static final int EDGE_ALL = EDGE_LEFT | EDGE_TOP | EDGE_RIGHT | EDGE_BOTTOM;
    private int edgeTracking = -1;
    private boolean convertActivityToTranslucented = false;
    private Callback callback;
    private float radiusDrag;
    private float radiusShadow;
    private float touchSlop;
    private boolean checkTouchSloped = false;

    public SwipeBackLayout(@NonNull final Activity activity, @NonNull final Callback callback) {
        super(activity);
        this.activity = activity;
        this.callback = callback;
        radiusDrag = callback.getRadiusDrag(getContext());
        radiusShadow = callback.getRadiusShadow(getContext());

        rectFEdgeLeft = new RectF();
        rectFEdgeTop = new RectF();
        rectFEdgeRight = new RectF();
        rectFEdgeBottom = new RectF();
        paintShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintShadow.setColor(callback.getShadowColor());
        paintShadow.setMaskFilter(new BlurMaskFilter(radiusShadow, callback.getShadowBlur()));
        pathClip = new Path();

        final ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        maxVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        minVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        velocityThreshold = minVelocity + (maxVelocity - minVelocity) * 0.3f;
        touchSlop = viewConfiguration.getScaledTouchSlop();
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            /**
             * 计算两个 MotionEvent 之间的距离（基于 pointer index 0）
             * @param e1 第一个事件
             * @param e2 第二个事件
             * @return 两点之间的欧几里得距离
             */
            private float getDistance(MotionEvent e1, MotionEvent e2) {
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                return (float) Math.hypot(dx, dy);
            }

            private int getEdgesTouched(float x, float y) {
                int result = 0;
                if (x < getLeft() + edgeHSize) result |= EDGE_LEFT;
                if (y < getTop() + edgeVSize) result |= EDGE_TOP;
                if (x > getRight() - edgeHSize) result |= EDGE_RIGHT;
                if (y > getBottom() - edgeVSize) result |= EDGE_BOTTOM;
                return result;
            }

            private boolean checkTouchSlop(float dx, float dy) {
                return dx * dx + dy * dy > touchSlop * touchSlop;
            }

            //注意：多指触摸缩放的时候，这里也会回调,e1是down ,e2是move
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (Math.abs(distanceX) > Math.abs(distanceY)
                        && !rectFEdgeLeft.contains(e1.getX(), e1.getY())
                        && !rectFEdgeRight.contains(e1.getX(), e1.getY())
                        || Math.abs(distanceX) <= Math.abs(distanceY)
                        && !rectFEdgeTop.contains(e1.getX(), e1.getY())
                        && !rectFEdgeBottom.contains(e1.getX(), e1.getY()))
                    return false;

                checkTouchSloped = checkTouchSlop(distanceX, distanceY);
//                if (distanceX < 0 && !canScrollHorizontal(SwipeBackLayout.this, false, 1, e1.getX(), e1.getY())
//                        || distanceX > 0 && !canScrollHorizontal(SwipeBackLayout.this, false, -1, e1.getX(), e1.getY())) {
                translate_x = Math.max(-getWidth(), Math.min(getWidth(), translate_x - distanceX));
//                } else {
//                    return false;
//                }
//                if (distanceY < 0 && !canScrollVertical(SwipeBackLayout.this, false, 1, e1.getX(), e1.getY())
//                        || distanceY > 0 && !canScrollVertical(SwipeBackLayout.this, false, -1, e1.getX(), e1.getY())) {
                translate_y = Math.max(-getHeight(), Math.min(getHeight(), translate_y - distanceY));
//                } else {
//                    return false;
//                }
                if (dragState == STATE_IDLE && convertActivityToTranslucented && (Math.abs(translate_x) > callback.getThresholdHRatio() * getWidth()
                        || Math.abs(translate_y) > callback.getThresholdVRatio() * getHeight())) {
                    dragState = STATE_DRAGGING;
                    edgeTracking = getEdgesTouched(e1.getX(), e1.getY());
                    callback.onEdgeTouched(edgeTracking);
                }
//                else {
//                    return false;
//                }
                if (dragState == STATE_DRAGGING) {
                    if (!onScaling) {
                        // 方法一：使用 Math.hypot（推荐）
//                        double distance = Math.hypot(Math.abs(translate_x) - getWidth() * 0.5, Math.abs(translate_y) - getHeight() * 0.5);
//                        double distance = Math.hypot(Math.abs(translate_x) - getWidth() * 0.5, Math.abs(translate_y) - getHeight() * 0.5);
//                        double distance = Math.sqrt(Math.pow(Math.abs(translate_x) - getWidth() * 0.5, 2)
//                                + Math.pow(Math.abs(translate_y) - getHeight() * 0.5, 2));
                        zoom = Math.min(1, Math.max(0, 1 - getDistance(e1, e2) / Math.max(getWidth(), getHeight())));
                    }
                    invalidate();
                }
                return true;
            }
        });
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                onScaling = true;
                zoom *= detector.getScaleFactor();
                zoom = Math.max(zoom_min, Math.min(zoom, zoom_max));
                invalidate();
                return true;
            }
        });

        TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.windowBackground
        });
        int background = a.getResourceId(0, 0);
        a.recycle();
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        View decorChild = decor.getChildAt(0);
        decorChild.setBackgroundResource(background);
        decor.removeView(decorChild);
        addView(decorChild, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        contentView = decorChild;
        decor.addView(this, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //必须限制在0.5以内，否则GG，因为left比right还大了，你说寄不寄
        if (edgeHSize < 0)
            edgeHSize = w * Math.max(0, Math.min(0.4f, callback.getEdgeSizeHRatio()));
        if (edgeVSize < 0)
            edgeVSize = h * Math.max(0, Math.min(0.4f, callback.getEdgeSizeVRatio()));

        rectFEdgeLeft.left = 0;
        rectFEdgeLeft.top = 0;
        rectFEdgeLeft.right = edgeHSize;
        rectFEdgeLeft.bottom = getHeight();

        rectFEdgeTop.left = 0;
        rectFEdgeTop.top = 0;
        rectFEdgeTop.right = getWidth();
        rectFEdgeTop.bottom = edgeVSize;

        rectFEdgeRight.left = getWidth() - edgeHSize;
        rectFEdgeRight.top = 0;
        rectFEdgeRight.right = getWidth();
        rectFEdgeRight.bottom = getHeight();

        rectFEdgeBottom.left = 0;
        rectFEdgeBottom.top = getHeight() - edgeVSize;
        rectFEdgeBottom.right = getWidth();
        rectFEdgeBottom.bottom = getHeight();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (contentView == null) return;
        contentView.layout(0, 0, right, bottom);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (convertActivityToTranslucented && (translate_x != 0 || translate_y != 0 || zoom != 1)) {
            canvas.scale(zoom, zoom, getWidth() * 1f / 2 + translate_x, getHeight() * 1f / 2 + translate_y);
            //必须先scale后translate，否则会往相反方向translate
            canvas.translate(translate_x, translate_y);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), radiusDrag, radiusDrag, paintShadow);

            pathClip.reset();
            pathClip.addRoundRect(0, 0, getWidth(), getHeight(), radiusDrag, radiusDrag, Path.Direction.CW);
            canvas.clipPath(pathClip); // 裁剪子View绘制区域
        }
        super.dispatchDraw(canvas);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                checkTouchSloped=false;
                translate_x = 0;
                translate_y = 0;
                dragState = STATE_IDLE;
                onScaling = false;
                //这个必须写在这里，否则GG，出现黑闪
                convertActivityToTranslucented = TransparentUtils.convertActivityToTranslucent(activity);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                checkTouchSloped=false;
                velocityTracker.computeCurrentVelocity(1000, maxVelocity);
                final float xvel = clampMag(velocityTracker.getXVelocity(), minVelocity, maxVelocity);
                final float yvel = clampMag(velocityTracker.getYVelocity(), minVelocity, maxVelocity);
                velocityTracker.recycle();
                velocityTracker = null;

                if (!convertActivityToTranslucented || (translate_x == 0 && translate_y == 0 && zoom == 1))
                    break;
                //不能少
                if (dragState != STATE_DRAGGING) {
                    translate_x = 0;
                    translate_y = 0;
                }
                start_x = translate_x;
                start_y = translate_y;
                end_x = 0;
                end_y = 0;
                finishActivity = false;
                if (!onScaling && dragState == STATE_DRAGGING) {
                    if ((edgeTracking & EDGE_LEFT) != 0 && xvel > velocityThreshold
                            || (edgeTracking & EDGE_RIGHT) != 0 && xvel < -velocityThreshold
                            || (edgeTracking & EDGE_TOP) != 0 && yvel > velocityThreshold
                            || (edgeTracking & EDGE_BOTTOM) != 0 && yvel < -velocityThreshold) {
                        finishActivity = true;
                        if (Math.abs(xvel) > Math.abs(yvel)) {
                            end_x = xvel > minVelocity ? getWidth() : -getWidth();
                            end_y = translate_y;
                        } else {
                            end_x = translate_x;
                            end_y = yvel > minVelocity ? getHeight() : -getHeight();
                        }
                    } else if (Math.abs(translate_x) > getWidth() * 0.5f || Math.abs(translate_y) > getWidth() * 0.5f) {
                        finishActivity = true;
                        if (Math.abs(translate_x) / getWidth() > Math.abs(translate_y) / getHeight()) {
                            end_x = translate_x > 0 ? getWidth() : -getWidth();
                            end_y = translate_y;
                        } else {
                            end_x = translate_x;
                            end_y = translate_y > 0 ? getHeight() : -getHeight();
                        }
                    }
                }

                float start;
                float end;
                if (Math.abs(end_x - start_x) > Math.abs(end_y - start_y)) {
                    start = start_x;
                    end = end_x;
                } else {
                    start = start_y;
                    end = end_y;
                }
                final float tx = translate_x;
                final float ty = translate_y;
                final float z = zoom;
                ValueAnimator valueAnimator = ValueAnimator.ofFloat(start, end);
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        dragState = STATE_SETTLING;
                        translate_x = tx + animation.getAnimatedFraction() * (end_x - start_x);
                        translate_y = ty + animation.getAnimatedFraction() * (end_y - start_y);
                        //退出做缩放会导致动画太快，贼丑
                        if (!finishActivity) zoom = z + animation.getAnimatedFraction() * (1 - z);
                        invalidate();
                    }
                });

                valueAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        dragState = STATE_IDLE;
                        if (finishActivity && !activity.isFinishing()) {
                            activity.finish();
                            activity.overridePendingTransition(0, 0);
                        }
                    }
                });
                valueAnimator.setDuration(300);
                valueAnimator.setInterpolator(new DecelerateInterpolator());
                valueAnimator.setEvaluator(new FloatEvaluator());
                valueAnimator.start();
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!convertActivityToTranslucented) return super.onInterceptTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                //必须放MOVE中，因为onInterceptTouchEvent和onTouchEvent都写了scaleGestureDetector,否则容易单指移动出现scale情况
                scaleGestureDetector.onTouchEvent(event);
                //必须做拦截，否则GG
                if (checkTouchSloped) return true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!convertActivityToTranslucented) return super.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                //必须放MOVE中，因为onInterceptTouchEvent和onTouchEvent都写了scaleGestureDetector,否则容易单指移动出现scale情况
                scaleGestureDetector.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        //必须是true ,否则GG
        return true;
    }


    /**
     * 一旦被触摸，当前activity退出的动画就会被改变（因为设置了透明），可能会变成上下动画，贼丑，
     * 故而必须在BaseActivity中调用此函数，设置为左右动画
     */
    public void overrideFinishPendingTransition() {
        activity.overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }

    private boolean canScrollHorizontal(@NonNull View v, boolean checkV, float dx, float x, float y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                        && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                        && canScrollHorizontal(child, true, dx, x + scrollX - child.getLeft(),
                        y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && v.canScrollHorizontally((int) -dx);
    }

    private boolean canScrollVertical(@NonNull View v, boolean checkV, float dy, float x, float y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                        && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                        && canScrollVertical(child, true, dy, x + scrollX - child.getLeft(),
                        y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && v.canScrollVertically((int) -dy);
    }

    private float clampMag(float value, float absMin, float absMax) {
        final float absValue = Math.abs(value);
        if (absValue < absMin) return 0;
        if (absValue > absMax) return value > 0 ? absMax : -absMax;
        return value;
    }

    public static abstract class Callback {
        //由于状态栏和导航栏，故而垂直方向有效触摸范围要设大点

        /**
         * @return [0, 0.4]
         */
        public float getEdgeSizeVRatio() {
            return 0.1f;
        }

        /**
         * @return [0, 0.4]
         */
        public float getEdgeSizeHRatio() {
            return 0.08f;
        }

        /**
         * @return [0, 0.4]
         */
        public float getThresholdVRatio() {
            return 0.2f;
        }

        /**
         * @return [0, 0.4]
         */
        public float getThresholdHRatio() {
            return 0.2f;
        }

        public float getRadiusDrag(Context context) {
            return ScreenUtils.dpAdapt(context, 30);
        }

        public float getRadiusShadow(Context context) {
            return ScreenUtils.dpAdapt(context, 10);
        }

        public int getShadowColor() {
            return Color.BLACK;
        }

        public BlurMaskFilter.Blur getShadowBlur() {
            return BlurMaskFilter.Blur.NORMAL;
        }

        public void onEdgeTouched(int edgeTracking) {
        }
    }
}
