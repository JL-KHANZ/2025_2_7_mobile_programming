package com.example.mobile_programming_2025_2.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BubbleTriangleView extends View {

    private final List<Bubble> bubbles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private ValueAnimator animator;

    public BubbleTriangleView(Context context) {
        super(context);
    }

    public BubbleTriangleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void explodeTriangle(float startX, float startY, int color) {
        if (bubbles.size() > 150) {
            bubbles.clear();
        }

        for (int i = 0; i < 30; i++) {
            bubbles.add(new Bubble(startX, startY, color));
        }

        if (animator == null || !animator.isRunning()) {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(animation -> {
                updateBubbles();
                invalidate();
            });
            animator.start();
        }
    }

    private void updateBubbles() {
        List<Bubble> deadBubbles = new ArrayList<>();
        for (Bubble b : bubbles) {
            b.x += b.vx;
            b.y += b.vy;

            b.alpha -= 4;
            b.radius -= 0.05f;

            if (b.alpha <= 0 || b.radius <= 0) {
                deadBubbles.add(b);
            }
        }
        bubbles.removeAll(deadBubbles);

        if (bubbles.isEmpty() && animator != null) {
            animator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Bubble b : bubbles) {
            paint.setColor(b.color);
            paint.setAlpha(b.alpha);
            canvas.drawCircle(b.x, b.y, b.radius, paint);
        }
    }

    private class Bubble {
        float x, y, vx, vy, radius;
        int color, alpha = 255;

        Bubble(float startX, float startY, int baseColor) {
            this.x = startX;
            this.y = startY;
            this.color = baseColor;

            // ⭐️ [수정됨] 버블 크기 키우기 (20 ~ 60)
            // 기존: random.nextFloat() * 20 + 10;
            this.radius = random.nextFloat() * 40 + 20;

            // 역삼각형 움직임
            this.vx = (random.nextFloat() - 0.5f) * 15;
            this.vy = - (random.nextFloat() * 15 + 10);
        }
    }
}