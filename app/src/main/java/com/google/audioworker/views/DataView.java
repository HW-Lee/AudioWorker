package com.google.audioworker.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.google.audioworker.utils.Constants;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by HWLee on 28/10/2017.
 */

public class DataView extends View {
    static final private String TAG = Constants.packageTag("DataView");

    private int mBgColor;
    private Paint mGridPaint;
    private int mGridSlotsX;
    private int mGridSlotsY;

    private final ArrayList<Paint> mDataPaints = new ArrayList<>();
    private final ArrayList<ArrayList<Double>> mDataBuffer = new ArrayList<>(0);;

    public DataView(Context context) {
        super(context);
        init();
    }

    public DataView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DataView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public DataView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mBgColor = Color.BLACK;
        mGridPaint = new Paint();
        Paint paint = new Paint();

        mGridPaint.setStrokeWidth(5.0f);
        mGridPaint.setColor(Color.GRAY);

        paint.setStrokeWidth(5.0f);
        paint.setColor(Color.GREEN);
        mDataPaints.add(paint);

        mGridSlotsX = 0;
        mGridSlotsY = 0;
    }

    public Paint getGridPaint() {
        return mGridPaint;
    }

    public void setDataPaint(int idx, int color) {
        synchronized (mDataPaints) {
            if (idx < mDataPaints.size() && idx >= 0 && mDataPaints.get(idx) != null) {
                mDataPaints.get(idx).setColor(color);
                return;
            }
            if (idx != mDataPaints.size())
                return;

            Paint paint = new Paint();
            paint.setStrokeWidth(5.0f);
            paint.setColor(color);
            mDataPaints.add(paint);
        }
    }

    public void setGridSlotsX(int gridSlotsX) {
        mGridSlotsX = gridSlotsX;
    }

    public void setGridSlotsY(int gridSlotsY) {
        mGridSlotsY = gridSlotsY;
    }

    public void reset() {
        synchronized (mDataBuffer) {
            mDataBuffer.clear();
        }

        this.postInvalidate();
    }

    public void plot(Collection<? extends Double>[] data) {
        synchronized (mDataBuffer) {
            mDataBuffer.clear();
            for (Collection<? extends Double> each : data) {
                ArrayList<Double> array = new ArrayList<>(each);
                mDataBuffer.add(array);
            }
        }

        this.postInvalidate();
    }

    private float convertToViewPosition(double dataY, int height) {
        return height/2.0f * (1 - (float) dataY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewHeight = this.getMeasuredHeight();
        int viewWidth = this.getMeasuredWidth();

        canvas.drawColor(mBgColor);
        mGridPaint.setStrokeWidth(mGridPaint.getStrokeWidth()*2);
        canvas.drawLine(0, viewHeight/2.0f, viewWidth, viewHeight/2.0f, mGridPaint);
        mGridPaint.setStrokeWidth(mGridPaint.getStrokeWidth()/2);

        if (mGridSlotsX > 0) {
            for (int i = 1; i < mGridSlotsX; i++) {
                float x = (float) viewWidth/mGridSlotsX * i;
                canvas.drawLine(x, 0, x, viewHeight, mGridPaint);
            }
        }

        if (mGridSlotsY > 0) {
            for (int i = 1; i < mGridSlotsY; i++) {
                float y = (float) viewHeight/mGridSlotsY * i;
                canvas.drawLine(0, y, viewWidth, y, mGridPaint);
            }
        }

        synchronized (mDataBuffer) {
            for (int j = 0; j < mDataBuffer.size(); j++) {
                ArrayList<Double> data = mDataBuffer.get(j);
                Paint paint = mDataPaints.get(j);
                if (data == null || paint == null)
                    continue;

                for (int i = 0; i < data.size() - 1; i++) {
                    float startX = (float) viewWidth / data.size() * i;
                    float endX = (float) viewWidth / data.size() * (i + 1);
                    float startY = this.convertToViewPosition(data.get(i), viewHeight);
                    float endY = this.convertToViewPosition(data.get(i + 1), viewHeight);

                    canvas.drawLine(startX, startY, endX, endY, paint);
                }
            }
        }
    }
}
