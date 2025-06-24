Android自定义控件之仿微信滑动返回
上下左右，双指缩放

![请添加图片描述](https://i-blog.csdnimg.cn/direct/0d1d7bdd8c7147baad2a4a9cef7e45d6.gif)



使用

```c
       maven { url "https://jitpack.io" }
       maven { url 'https://www.jitpack.io' }
```

```c
implementation 'com.github.AnJiaoDe:SwipeBackNiubility:V0.0.0'
```
在你APP的BaseActivity 里添加如下代码
```c
public abstract class BaseActivity extends AppCompatActivity {
    private SwipeBackLayout swipeBackLayout;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        swipeBackLayout = new SwipeBackLayout(this, new SwipeBackLayout.Callback() 
        {
            //由于状态栏和导航栏，故而垂直方向有效触摸范围要设大点
            @Override
            public float getEdgeVSizeRatio() {
                return 0.15f;
            }

            @Override
            public void onEdgeTouched(int edgeTracking) {
                super.onEdgeTouched(edgeTracking);
                //可以添加震动效果
                VibratorUtils.startVibrate(BaseActivity.this);
            }
        });
        swipeBackLayout.attachActivity(this);
    }
    //    * 一旦被触摸，当前activity退出的动画就会被改变（因为设置了透明），
    //可能会变成上下动画，//贼丑，
 //    * 故而必须在BaseActivity中调用此函数，设置为左右动画
    @Override
    public void finish() {
        super.finish();
        swipeBackLayout.overrideFinishPendingTransition(this);
    }
}
```
可以在回调接口中设置参数
```c
  public static abstract class Callback {
        public float getEdgeVSizeRatio() {
            return 0.1f;
        }

        public float getEdgeHSizeRatio() {
            return 0.1f;
        }

        public float getRadiusDrag(Context context) {
            return com.cy.router.utils.ScreenUtils.dpAdapt(context, 30);
        }

        public float getRadiusShadow(Context context) {
            return com.cy.router.utils.ScreenUtils.dpAdapt(context, 10);
        }

        public void onEdgeTouched(int edgeTracking) {
        }
    }
```
