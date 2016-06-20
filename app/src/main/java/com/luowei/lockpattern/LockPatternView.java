package com.luowei.lockpattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * 图案解锁
 * Created by 骆巍 on 2016/6/16.
 */
public class LockPatternView extends View {
    private LockPoint[] points;
    private Paint paint = new Paint();
    private Paint selectedPaint = new Paint();
    private Paint textPaint = new Paint();
    private List<LockPoint> selectedPoint = new ArrayList<>();
    private PointF selectedPf;
    private Vibrator vibrator;

    private OnLockListener lockListener;

    public LockPatternView(Context context) {
        this(context, null);
    }

    public LockPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp2px(1));
        paint.setColor(0x99000000);

        selectedPaint.setAntiAlias(true);
        selectedPaint.setDither(true);
        selectedPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        selectedPaint.setColor(0x55000000);
        selectedPaint.setStrokeWidth(dp2px(10));
        selectedPaint.setStrokeCap(Paint.Cap.ROUND);
        selectedPaint.setStrokeJoin(Paint.Join.ROUND);

        textPaint.setAntiAlias(true);
        textPaint.setColor(0x99000000);
        textPaint.setTextSize(sp2px(30));
        textPaint.setTypeface(Typeface.SERIF);

        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (points != null) return;
        points=new LockPoint[10];
        int width = getWidth();
        int height = getHeight();
        float offset = Math.abs((height - width) / 2);
        int lockWidth = Math.min(width, height);
        float gap = lockWidth / 4;
        for (int i = 0; i < points.length; i++) {
            LockPoint p = new LockPoint();
            //判断横竖屏
            if (width < height) p.y = offset;
            else p.x = offset;
            points[i] = p;
            p.index = i + 1 + "";
            p.x += gap * (i % 3 + 1);
            p.y += gap * (i / 3) + gap / 2;
            p.raduis = lockWidth / 12;
            if (i == points.length - 1) {
                p.x += gap;
                p.index = "0";
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (LockPoint lp : points) {
            canvas.drawText(lp.index, lp.x - getTextWidth(lp.index) / 2f, lp.y + getTextHeight(lp.index) / 2f, textPaint);
            if (selectedPoint.contains(lp)) {
                canvas.drawCircle(lp.x, lp.y, lp.raduis, selectedPaint);
            } else {
                canvas.drawCircle(lp.x, lp.y, lp.raduis, paint);
            }
        }
        if (selectedPoint.size() > 0) {
            float[] ps = new float[selectedPoint.size() * 4];
            for (int i = 0; i < selectedPoint.size(); i++) {
                LockPoint lp = selectedPoint.get(i);
                if (i == 0) {
                    ps[0] = selectedPoint.get(0).x;
                    ps[1] = selectedPoint.get(0).y;
                } else {
                    ps[i * 4 - 2] = lp.x;
                    ps[i * 4 - 1] = lp.y;
                    if (i < selectedPoint.size() - 1) {
                        ps[i * 4] = lp.x;
                        ps[i * 4 + 1] = lp.y;
                    }
                }
                if (i == selectedPoint.size() - 1 && selectedPf != null) {
                    ps[i * 4] = lp.x;
                    ps[i * 4 + 1] = lp.y;
                    ps[i * 4 + 2] = selectedPf.x;
                    ps[i * 4 + 3] = selectedPf.y;
                }
            }
            if (selectedPoint.size() > 1 || selectedPf != null)
                canvas.drawLines(ps, selectedPaint);
        }
    }

    public float dp2px(float dipValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, getResources().getDisplayMetrics());
    }

    public float sp2px(float dipValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dipValue, getResources().getDisplayMetrics());
    }

    public float getTextWidth(String s) {
        return textPaint.measureText(s);
    }

    public float getTextHeight(String s) {
//        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
//        return fontMetrics.descent - fontMetrics.ascent;
        Rect r = new Rect();
        textPaint.getTextBounds(s, 0, 1, r);
        return r.height();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (selectedPf == null) selectedPf = new PointF();
        LockPoint lp = null;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (selectedPoint.size() > 0) {
                    selectedPoint.clear();
                    selectedPaint.setColor(0x55000000);
                }
                lp = checkPoint(event.getX(), event.getY());
                if (lp != null) {
                    selectedPoint.add(lp);
                    selectedPf.x = event.getX();
                    selectedPf.y = event.getY();
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                lp = checkPoint(event.getX(), event.getY());
                if (lp != null) {
                    selectedPoint.add(lp);
                } else {
                    selectedPf.x = event.getX();
                    selectedPf.y = event.getY();
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                selectedPf = null;
                String str = "";
                for (LockPoint p : selectedPoint) {
                    str += p.index;
                }
                if (lockListener != null) {
                    if (!lockListener.onLock(str)) {
                        vibrate(100);
                        setSelectedColor(0x55ff0000);
                    }
                }
                invalidate();
                break;
        }
        return super.onTouchEvent(event);
    }


    /**
     * 判断选中的点
     *
     * @param x
     * @param y
     * @return
     */
    public LockPoint checkPoint(float x, float y) {
        for (LockPoint lp : points) {
            if (!selectedPoint.contains(lp) &&
                    Math.sqrt(Math.pow(x - lp.x, 2) + Math.pow(y - lp.y, 2)) < lp.raduis) {
                return lp;
            }
        }
        return null;
    }

    interface OnLockListener {
        /**
         * @param pwd 用户输入的密码
         * @return false 执行自带效果(解锁失败) true 拦截自带效果(解锁成功)
         */
        boolean onLock(String pwd);
    }

    public void setOnLockListener(OnLockListener l) {
        this.lockListener = l;
    }

    public void vibrate(long time) {
        vibrator.vibrate(time);
    }

    public void setSelectedColor(int color) {
        selectedPaint.setColor(color);
        invalidate();
    }
}
