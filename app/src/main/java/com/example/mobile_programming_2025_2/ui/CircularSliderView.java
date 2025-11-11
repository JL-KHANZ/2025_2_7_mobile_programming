package com.example.mobile_programming_2025_2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.min;
import static java.lang.Math.sin;

public class CircularSliderView extends View {

    public interface OnValueChangeListener {
        void onValueChanged(int value); // 0..100
    }

    private OnValueChangeListener listener;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float cx, cy;           // center
    private float radius;           // track radius
    private float strokeWidth;      // track thickness
    private float thumbRadius;      // knob size

    private final RectF arcBounds = new RectF();

    private int value = 0;          // 0..100
    private float angleDeg = -90f;  // -90° at top (12 o’clock)

    public CircularSliderView(Context c) { this(c, null); }
    public CircularSliderView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }

    private void init() {
        strokeWidth = dp(25);
        thumbRadius = dp(20);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(0xFFE0E0E0); // light gray
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setColor(0xFF6200EE); // accent
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(0xFF6200EE);
    }

    public void setOnValueChangeListener(OnValueChangeListener l) { this.listener = l; }

    public void setValue(int v) {
        v = Math.max(0, Math.min(100, v));
        if (this.value != v) {
            this.value = v;
            this.angleDeg = valueToAngle(v);
            invalidate();
            if (listener != null) listener.onValueChanged(this.value);
        }
    }

    public int getValue() { return value; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(w, h); // keep it square
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cx = w / 2f;
        cy = h / 2f;
        radius = (min(w, h) / 2f) - strokeWidth - thumbRadius;
        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw track
        canvas.drawOval(arcBounds, trackPaint);

        // draw progress arc from -90° (top) to current angle
        float sweep = normalizeSweep(angleDeg + 90f);
        if (sweep > 0f) {
            // canvas.drawArc(arcBounds, -90f, sweep, false, progressPaint);
        }

        // draw thumb at current angle
        float rad = (float) Math.toRadians(angleDeg);
        float tx = cx + (float) cos(rad) * radius;
        float ty = cy + (float) sin(rad) * radius;
        canvas.drawCircle(tx, ty, thumbRadius, thumbPaint);
    }

    // In CircularSliderView.java
    private boolean isDragging = false;
    private float touchSlop;
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    private boolean isNearThumb(float x, float y) {
        // hit test: near the thumb point
        float rad = (float) Math.toRadians(angleDeg);
        float tx = cx + (float) Math.cos(rad) * radius;
        float ty = cy + (float) Math.sin(rad) * radius;
        float dx = x - tx, dy = y - ty;
        float r = thumbRadius + touchSlop * 1.5f; // forgiving hit target
        return (dx*dx + dy*dy) <= r*r;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Only start if the user touched near the thumb (or the track, if you prefer)
                if (isNearThumb(x, y)) {
                    isDragging = true;
                    getParent().requestDisallowInterceptTouchEvent(true); // block ScrollView/ViewPager
                    updateAngleAndValue(x, y); // your angle/value code
                    return true;               // consume from now on
                }
                return false; // let other views handle this tap

            case MotionEvent.ACTION_MOVE:
                if (!isDragging) return false;
                updateAngleAndValue(x, y);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                return false;
        }
        return false;
    }

    private void updateAngleAndValue(float x, float y) {
        // your existing angle/value computation + invalidate + listener callback
    }

    private float valueToAngle(int v) {
        // v:0..100 -> angle:-90..270 (0 at top, clockwise)
        return -90f + (v / 100f) * 360f;
    }

    private float angleToValue(float deg) {
        // deg in [-180..180] from atan2; map to 0..360 starting from -90 at 0%
        float normalized = deg - (-90f); // shift so -90 -> 0
        while (normalized < 0f) normalized += 360f;
        while (normalized >= 360f) normalized -= 360f;
        return (normalized / 360f) * 100f;
    }

    private float normalizeSweep(float sweep) {
        // sweep must be 0..360
        float s = sweep;
        while (s < 0f) s += 360f;
        while (s > 360f) s -= 360f;
        return s;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
