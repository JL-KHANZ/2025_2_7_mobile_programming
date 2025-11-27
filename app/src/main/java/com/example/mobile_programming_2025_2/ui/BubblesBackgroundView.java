package com.example.mobile_programming_2025_2.ui;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class BubblesBackgroundView extends View {

    private final Paint bubblePaint;
    private final List<Bubble> bubbles = new ArrayList<>();
    private final Random random = new Random();
    private static final int NUM_BUBBLES = 30;

    public BubblesBackgroundView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubblesBackgroundView(Context context) {
        this(context, null, 0);
    }

    public BubblesBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float blurRadius = 15f;
        bubblePaint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));

        setBubbleColor(Color.parseColor("#4C81D4FA"));
        for (int i = 0; i < NUM_BUBBLES; i++) {
            bubbles.add(new Bubble());
        }
    }

    public void setBubbleColor(int color) {
        bubblePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        for (Bubble bubble : bubbles) {
            bubble.reset(w, h, random);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        for (int i = 0; i < bubbles.size(); i++) {
            Bubble bubble = bubbles.get(i);
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius, bubblePaint);
            bubble.y -= bubble.speed;
            if (bubble.y < -bubble.radius) {
                bubble.reset(width, height, random);
            }
        }
        invalidate();
    }

    private static class Bubble {
        float x, y;
        float radius;
        float speed;
        public void reset(int width, int height, Random random) {
            this.x = random.nextInt(width);
            this.y = height + random.nextInt(height);
            this.radius = 20 + random.nextFloat() * 70;
            this.speed = 0.5f + random.nextFloat() * 1.5f;
        }

    }

}

