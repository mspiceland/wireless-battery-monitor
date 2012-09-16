/*
* Copyright (c) 2011 Michael Spiceland
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.fuzzymonkey.wirelessbatterymonitor.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Example of how to write a custom subclass of View. LabelView
 * is used to draw simple text views. Note that it does not handle
 * styled text or right-to-left writing systems.
 *
 */
public class SpeedometerView extends View {
    private Paint mTextPaint;
    private Paint mBigTextPaint;
    private Paint mShadePaint;
    private Paint mArcPaint;
    private Paint mShadeArcPaint;
    final String TAG = "FUZZYMONKEY";
    protected final int ARCSTROKEWIDTH = 20;
    String mLabel = "mph";
    int mMinValue = 0;
    float mMaxValue = (float)25.0;
    float mCurValue = (float)0;

    /**
     * Constructor.  This version is only needed if you will be instantiating
     * the object manually (not from a layout XML file).
     * @param context
     */
    public SpeedometerView(Context context) {
        super(context);
        Log.v(TAG,"creating new Speedometer(Context).");
        initGraphView();
    }

    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public SpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.v(TAG,"creating new SpeedometerView(Context, AttributeSet).");
        initGraphView();

        setText("Speedometer");
        setTextColor(Color.WHITE);
        Log.v(TAG,"done creating new SpeedometerView(Context, AttributeSet).");
    }

    private final void initGraphView() {
        Log.v(TAG,"Starting initGraphView");

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(24);
        mTextPaint.setColor(0xFF000000);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        setPadding(3, 3, 3, 3);

        mBigTextPaint = new Paint();
        mBigTextPaint.setAntiAlias(true);
        mBigTextPaint.setTextSize(64);
        mBigTextPaint.setColor(0xFF000000);
        mBigTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

        mShadePaint = new Paint();
        mShadePaint.setAntiAlias(true);
        mShadePaint.setTextSize(16);
        mShadePaint.setColor(0x3FFFFFFF);

        mArcPaint = new Paint();
        mArcPaint.setARGB(200, 255, 130, 20);
        mArcPaint.setAntiAlias(true);
        mArcPaint.setStyle(Style.STROKE);
        mArcPaint.setStrokeWidth(ARCSTROKEWIDTH / 3);

        mShadeArcPaint = new Paint();
        mShadeArcPaint.setARGB(128, 128, 128, 128);
        mShadeArcPaint.setAntiAlias(true);
        mShadeArcPaint.setStyle(Style.STROKE);
        mShadeArcPaint.setStrokeWidth(ARCSTROKEWIDTH);

        Log.v(TAG,"Finished initGraphView");
    }

    /**
     * Sets the text to display in this label
     * @param text The text to display. This will be drawn as one line.
     */
    public void setText(String text) {
        Log.v(TAG,"Starting setText with list size ");
        requestLayout();
        invalidate();
        Log.v(TAG,"Finished setText with list size ");
    }

    /**
     * Sets the text size for this label
     * @param size Font size
     */
    public void setTextSize(int size) {
        Log.v(TAG,"Starting setTextSize with list size ");
        mTextPaint.setTextSize(size);
        requestLayout();
        invalidate();
        Log.v(TAG,"Finished setTextSize with list size ");
    }

    /**
     * Sets the text color for this label.
     * @param color ARGB value for the text
     */
    public void setTextColor(int color) {
        Log.v(TAG,"Starting setTextColor with list size ");
        mTextPaint.setColor(color);
        mBigTextPaint.setColor(color);
        invalidate();
        Log.v(TAG,"Finished setTextColor with list size ");
    }

    /**
     * @see android.view.View#measure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.v(TAG,"Starting onMeasure with list size ");
        setMeasuredDimension(measureWidth(widthMeasureSpec),
            measureHeight(heightMeasureSpec));
        Log.v(TAG,"Finished onMeasure with list size ");
    }

    /**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        Log.v(TAG,"Starting measureWidth with list size ");
        return MeasureSpec.getSize(measureSpec);
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        Log.v(TAG,"Starting measureHeight with list size ");
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        } else {
            return 300;
        }
    }

    /**
     * Render the text
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //int axisMargin = 10;
        //int leftAxisExtraMargin = 20;
        //int leftAxis = axisMargin + leftAxisExtraMargin+ getPaddingLeft();
        //int bottomAxis = getHeight() - axisMargin;
        PointF center;
        PointF tickStart;
        PointF tickEnd;
        PointF tickLabel;
        final int TICKMARGIN = 120;
        final int TICKLENGTH = ARCSTROKEWIDTH;

        center = new PointF(getWidth()/2, getHeight()*2/3);

        mTextPaint.setStrokeWidth(2);

        /* TODO: these settings only work for landscape */
        canvas.drawText(mLabel, center.x, center.y - 64, mTextPaint);

        mBigTextPaint.setStrokeWidth(2);
        mBigTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.format("%02.1f", mCurValue), center.x, center.y + 20, mBigTextPaint);

        mTextPaint.setTextAlign(Paint.Align.CENTER);
        for (int angle = 0; angle <= 210; angle += 15) {
            tickStart = getPointAtAngle(center.x, center.y, TICKMARGIN, angle);
            tickEnd = getPointAtAngle(center.x, center.y, TICKMARGIN + TICKLENGTH, angle);
            tickLabel = getPointAtAngle(center.x, center.y, TICKMARGIN + TICKLENGTH + 20, angle);
            canvas.drawLine(tickStart.x, tickStart.y, tickEnd.x, tickEnd.y, mTextPaint);
            if ((angle % 30) == 0) {
                canvas.drawText(String.format("%d", Math.round(scaleValue(angle))), tickLabel.x, tickLabel.y, mTextPaint);
            }
        }

        RectF shadeArcRect = new RectF(center.x - TICKMARGIN- TICKLENGTH/2,
                center.y - TICKMARGIN - TICKLENGTH/2,
                center.x + TICKMARGIN + TICKLENGTH/2,
                center.y + TICKMARGIN + TICKLENGTH/2);

        RectF arcRect = new RectF(center.x - TICKMARGIN,
                center.y - TICKMARGIN,
                center.x + TICKMARGIN,
                center.y + TICKMARGIN);

        /* grey shade */
        canvas.drawArc(shadeArcRect, -195, mCurValue / mMaxValue * 210, false, mShadeArcPaint);

        /* bright highlight color */
        canvas.drawArc(arcRect, -195, mCurValue / mMaxValue * 210, false, mArcPaint);
    }

    PointF getPointAtAngle(float centerX, float centerY, float radius, float angle) {
        /*
         * make a point from a radial distance from another point
         * currently assumes (centerX, centerY) == (0, 0)
         * Need to do something else to offset for centerX and center Y
         */

        angle = 375 - angle; // re-orient axes

        final int RIGHT = 90;
        float otherAngle = 180-RIGHT-angle;
        /* implicit divide by sin(90) which is 1 */
        double x = (radius * Math.sin((double) otherAngle*Math.PI/180));
        double y = (radius * Math.sin((double) angle*Math.PI/180));

        return new PointF(centerX - (float)x, centerY + (float)y);
    }

    /* get a value and scale it based on our configured min and max */
    float scaleValue (float value) {
        return value / 210 * mMaxValue;
    }

    public void setValue(float value) {
        mCurValue = value;
        invalidate();
    }

    public void setMaxValue(float value) {
        mMaxValue = value;
        invalidate();
    }

    public void setLabel(String newLabel) {
        Log.v(TAG, "setLabel to " + newLabel);
        mLabel = newLabel;
        invalidate();
    }
}
