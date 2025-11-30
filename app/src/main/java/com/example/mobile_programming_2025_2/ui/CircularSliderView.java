package com.example.mobile_programming_2025_2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.mobile_programming_2025_2.R;

public class CircularSliderView extends View {

    private Paint arcPaint;
    private Paint thumbPaint;
    private Paint backgroundPaint;

    private RectF arcRect;

    private float centerX;
    private float centerY;
    private float radius;
    private float strokeWidth = 60f; // 휠 두께

    private float currentAngle = 0f;  // 현재 각도 (0-360)
    private float currentValue = 0f;  // 현재 값 (0-100)

    private boolean isDragging = false;

    // 감정 색상 배열
    private int[] emotionColors;

    public interface OnValueChangeListener {
        void onValueChange(float value);
    }

    private OnValueChangeListener listener;

    public CircularSliderView(Context context) {
        super(context);
        init(context);
    }

    public CircularSliderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CircularSliderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // XML 색상 가져오기
        emotionColors = new int[] {
                ContextCompat.getColor(context, R.color.joy_color),
                ContextCompat.getColor(context, R.color.love_color),
                ContextCompat.getColor(context, R.color.trust_color),
                ContextCompat.getColor(context, R.color.submission_color),
                ContextCompat.getColor(context, R.color.fear_color),
                ContextCompat.getColor(context, R.color.awe_color),
                ContextCompat.getColor(context, R.color.surprise_color),
                ContextCompat.getColor(context, R.color.disapproval_color),
                ContextCompat.getColor(context, R.color.sadness_color),
                ContextCompat.getColor(context, R.color.remorse_color),
                ContextCompat.getColor(context, R.color.disgust_color),
                ContextCompat.getColor(context, R.color.contempt_color),
                ContextCompat.getColor(context, R.color.anger_color),
                ContextCompat.getColor(context, R.color.aggressiveness_color),
                ContextCompat.getColor(context, R.color.anticipation_color),
                ContextCompat.getColor(context, R.color.optimism_color)
        };

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setColor(Color.parseColor("#E0E0E0"));
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidth);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(Color.WHITE);
        thumbPaint.setShadowLayer(8f, 0, 4f, Color.parseColor("#40000000"));

        setLayerType(LAYER_TYPE_SOFTWARE, thumbPaint);
        arcRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) / 2f - strokeWidth;
        float padding = strokeWidth / 2f;
        arcRect.set(padding, padding, w - padding, h - padding);
        createGradient();
    }

    private void createGradient() {
        int[] gradientColors = new int[emotionColors.length + 1];
        System.arraycopy(emotionColors, 0, gradientColors, 0, emotionColors.length);
        gradientColors[emotionColors.length] = emotionColors[0];
        SweepGradient gradient = new SweepGradient(centerX, centerY, gradientColors, null);
        arcPaint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint);

        canvas.save();
        canvas.rotate(-90, centerX, centerY);
        canvas.drawCircle(centerX, centerY, radius, arcPaint);
        canvas.restore();

        float thumbAngle = currentAngle - 90;
        float thumbX = centerX + (float) (radius * Math.cos(Math.toRadians(thumbAngle)));
        float thumbY = centerY + (float) (radius * Math.sin(Math.toRadians(thumbAngle)));

        Paint thumbBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbBgPaint.setStyle(Paint.Style.FILL);
        thumbBgPaint.setColor(getCurrentColor());
        canvas.drawCircle(thumbX, thumbY, 32f, thumbBgPaint);
        canvas.drawCircle(thumbX, thumbY, 28f, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isNearThumb(x, y)) {
                    isDragging = true;
                    updateAngle(x, y);
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    updateAngle(x, y);
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.onTouchEvent(event);
    }

    private boolean isNearThumb(float x, float y) {
        float thumbAngle = currentAngle - 90;
        float thumbX = centerX + (float) (radius * Math.cos(Math.toRadians(thumbAngle)));
        float thumbY = centerY + (float) (radius * Math.sin(Math.toRadians(thumbAngle)));
        float distance = (float) Math.sqrt(Math.pow(x - thumbX, 2) + Math.pow(y - thumbY, 2));
        return distance <= 100f;
    }

    private void updateAngle(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90;
        if (angle < 0) angle += 360;
        currentAngle = angle;
        currentValue = (angle / 360f) * 100f;
        invalidate();
        if (listener != null) listener.onValueChange(currentValue);
    }

    public int getCurrentColor() {
        if (emotionColors == null || emotionColors.length == 0) return Color.WHITE;
        float ratio = currentAngle / 360f;
        int numColors = emotionColors.length;
        float colorIndex = ratio * numColors;
        int index1 = (int) colorIndex % numColors;
        int index2 = (index1 + 1) % numColors;
        float blend = colorIndex - (int) colorIndex;
        return blendColors(emotionColors[index1], emotionColors[index2], blend);
    }

    private int blendColors(int color1, int color2, float ratio) {
        float inverseRatio = 1f - ratio;
        float r = (Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio);
        float g = (Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio);
        float b = (Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.listener = listener;
    }

    public int getCurrentEmotionColor() {
        return getCurrentColor();
    }
}