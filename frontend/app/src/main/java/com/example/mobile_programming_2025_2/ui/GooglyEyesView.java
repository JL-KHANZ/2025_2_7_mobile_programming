package com.example.mobile_programming_2025_2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.hypot;
import static java.lang.Math.min;
import static java.lang.Math.sin;

public class GooglyEyesView extends View {

    private final Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pupil = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float targetX = -1f;
    private float targetY = -1f;

    private float eyeRadius = 0f;
    private float pupilRadius = 0f;
    private float eyeGap = 0f;
    private float leftCx = 0f;
    private float rightCx = 0f;
    private float eyeCy = 0f;
    private float maxOffset = 0f;

    public GooglyEyesView(Context context) {
        this(context, null);
    }

    public GooglyEyesView(Context context, AttributeSet attrs) {
        super(context, attrs);

        white.setColor(0xFFFFFFFF);
        stroke.setColor(0xFF222222);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(6f);
        pupil.setColor(0xFF222222);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float widthF = (float) w;
        float heightF = (float) h;

        eyeRadius = (float) min(widthF / 6f, heightF / 3.5f);
        pupilRadius = eyeRadius * 0.35f;
        eyeGap = eyeRadius * 0.8f;

        float totalWidth = eyeRadius * 2f * 2f + eyeGap;
        leftCx = widthF / 2f - totalWidth / 2f + eyeRadius;
        rightCx = leftCx + eyeRadius * 2f + eyeGap;
        eyeCy = heightF / 2f;

        maxOffset = eyeRadius - pupilRadius - (eyeRadius * 0.15f);

        if (targetX < 0 || targetY < 0) {
            targetX = widthF / 2f;
            targetY = heightF / 2f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // whites + outline
        canvas.drawCircle(leftCx, eyeCy, eyeRadius, white);
        canvas.drawCircle(rightCx, eyeCy, eyeRadius, white);
        canvas.drawCircle(leftCx, eyeCy, eyeRadius, stroke);
        canvas.drawCircle(rightCx, eyeCy, eyeRadius, stroke);

        // pupils
        drawPupil(canvas, leftCx, eyeCy);
        drawPupil(canvas, rightCx, eyeCy);

        postInvalidateOnAnimation(); // smooth updates
    }

    private void drawPupil(Canvas canvas, float cx, float cy) {
        float dx = targetX - cx;
        float dy = targetY - cy;
        double dist = hypot(dx, dy);
        double angle = atan2(dy, dx);

        float r = (float) min(dist, maxOffset);
        float px = (float) (cx + cos(angle) * r);
        float py = (float) (cy + sin(angle) * r);

        canvas.drawCircle(px, py, pupilRadius, pupil);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                targetX = event.getX();
                targetY = event.getY();
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
    public void lookAtRaw(float rawX, float rawY) {
        int[] loc = new int[2];
        getLocationOnScreen(loc); // view's top-left on screen
        targetX = rawX - loc[0];  // convert to local X
        targetY = rawY - loc[1];  // convert to local Y
        invalidate();
    }

}
