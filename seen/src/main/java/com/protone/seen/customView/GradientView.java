package com.protone.seen.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import com.protone.seen.R;


public class GradientView extends View {

    private Handler handler;
    private Runnable colorRunnable;

    private LinearGradient linearGradient;
    private Paint paint;

    private float scroll;
    private int[] colors = new int[]{getResources().getColor(R.color.blue_2, null),
            getResources().getColor(R.color.blue_1, null)};

    private Float length;
    private float y1;
    private float steep;

    public GradientView(Context context) {
        super(context);
        Init();
    }

    public GradientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.getTheme()
                .obtainStyledAttributes(attrs,
                        R.styleable.GradientRelative, 0, 0);
        int color1 = array.getColor(R.styleable.GradientRelative_color1,
                colors[0]);
        int color2 = array.getColor(R.styleable.GradientRelative_color2,
                colors[1]);
        length = array.getFloat(R.styleable.GradientRelative_length, 4);
        y1 = array.getFloat(R.styleable.GradientRelative_y1, 800);
        steep = array.getFloat(R.styleable.GradientRelative_GradientSteep, 40);
        colors[0] = color1;
        colors[1] = color2;
        array.recycle();
        Init();
    }

    public GradientView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Init();
    }


    private void Init() {
        paint = new Paint();
        handler = new Handler(Looper.myLooper());
        colorRunnable = new colorRunnable();
        handler.post(colorRunnable);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        linearGradient =
                new LinearGradient(40, 0,
                        getMeasuredWidth() * 2 + 40, 800,
                        colors, null, Shader.TileMode.MIRROR);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setShader(linearGradient);
        canvas.drawRoundRect(0,0,getRight(),getBottom(),50,50,paint);
        canvas.drawPaint(paint);
    }

    private class colorRunnable implements Runnable {

        @Override
        public void run() {
            scroll += steep;

            if (scroll >= (getMeasuredWidth() * length +scroll)) {
                scroll = 0;
            }
            linearGradient =
                    new LinearGradient(scroll, 0,
                            getMeasuredWidth() * length + scroll,y1,
                            colors, null, Shader.TileMode.MIRROR);
            invalidate();

            if (handler != null) {
                handler.postDelayed(colorRunnable, 100);
            }
        }
    }

    @SuppressWarnings("unused")
    public int[] getColors(){
        return colors;
    }

    @SuppressWarnings("unused")
    public void setColors(int[] colors){
        this.colors = colors;
    }

    @SuppressWarnings("unused")
    public void stopAnimator(){
        handler.removeCallbacks(colorRunnable);
    }

    @SuppressWarnings("unused")
    public void startAnimator(){
        handler.post(colorRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (handler!=null && colorRunnable != null){
            handler.removeCallbacksAndMessages(colorRunnable);
            handler = null;
        }
        super.onDetachedFromWindow();
    }
}
