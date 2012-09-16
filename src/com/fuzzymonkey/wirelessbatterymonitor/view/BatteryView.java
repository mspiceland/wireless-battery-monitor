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
public class BatteryView extends View {
    private Paint mTextPaint;
    private Paint mBigTextPaint;
    private Paint mShadePaint;
    final String TAG = "FUZZYMONKEY";
    protected final int ARCSTROKEWIDTH = 20;
    String mLabel = "v";
    double mMinValue = 34.2;
    double mMaxValue = 40;
    double mCurValue = 0;

    /**
     * Constructor.  This version is only needed if you will be instantiating
     * the object manually (not from a layout XML file).
     * @param context
     */
    public BatteryView(Context context) {
        super(context);
        Log.v(TAG,"creating new BatteryView(Context).");
        initGraphView();
    }

    /**
     * Construct object, initializing with any attributes we understand from a
     * layout file. These attributes are defined in
     * SDK/assets/res/any/classes.xml.
     * 
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.v(TAG,"creating new BatteryView(Context, AttributeSet).");
        initGraphView();

        setText("Battery");
        setTextColor(Color.WHITE);
        Log.v(TAG,"done creating new BatteryView(Context, AttributeSet).");
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
        mBigTextPaint.setTextSize(48);
        mBigTextPaint.setColor(0xFF000000);
        mBigTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

        mShadePaint = new Paint();
        mShadePaint.setAntiAlias(true);
        mShadePaint.setTextSize(16);
        mShadePaint.setColor(0x6600FF00);

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
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            /* We were told how big to be */
            result = specSize;
        } else {
            result = 400;
        }

        Log.v(TAG,"Finished measureWidth with list size ");
        return result;
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        Log.v(TAG,"Starting measureHeight with list size ");
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            /* We were told how big to be */
            result = specSize;
        } else {
            result = 200;
        }
        Log.v(TAG,"Finished measureHeight with list size ");
        return result;
    }

    /**
     * Render the text
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.v(TAG,"Starting onDraw");
        final int MARGIN = 30;
        int left = MARGIN;
        int right = getWidth() - MARGIN;
        int bottom = getHeight() - MARGIN;
        int top = MARGIN;
        int battery_right = right - 40;

        mTextPaint.setStrokeWidth(5);

        Log.v(TAG, "height = " + canvas.getHeight());
        Log.v(TAG, "width = " + canvas.getWidth());
        Log.v(TAG, "height = " + getHeight());
        Log.v(TAG, "width = " + getWidth());

        Log.v(TAG, "left = " + left);
        Log.v(TAG, "right = " + right);
        Log.v(TAG, "bottom = " + bottom);
        Log.v(TAG, "top = " + top);

        int tab_top = top + 20;
        int tab_bottom = bottom - 20;

        /* draw level */
        canvas.drawRect(left, top, (float)(left + scale(mCurValue, battery_right - left)), bottom, mShadePaint);

        canvas.drawLine(left, top, battery_right, top, mTextPaint);
        canvas.drawLine(left, bottom, battery_right, bottom, mTextPaint);
        canvas.drawLine(left, top, left, bottom, mTextPaint);
        canvas.drawLine(battery_right, top, battery_right, bottom, mTextPaint);

        canvas.drawLine(battery_right, tab_top, right, tab_top, mTextPaint);
        canvas.drawLine(battery_right, tab_bottom, right, tab_bottom, mTextPaint);
        canvas.drawLine(battery_right, tab_top, battery_right, tab_bottom, mTextPaint);
        canvas.drawLine(right, tab_top, right, tab_bottom, mTextPaint);

        int center_x = getWidth()/2;
        int center_y = getHeight()/2;
        mBigTextPaint.setStrokeWidth(2);
        mBigTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.format("%02.1f " + mLabel, mCurValue), center_x, center_y + 20, mBigTextPaint);

        Log.v(TAG,"Finished onDraw");
    }

    private double scale(double value, double max) {
        double newvalue = (value - mMinValue) / (mMaxValue - mMinValue) * max;
        if (newvalue > max) {
            newvalue = max;
        }
        return newvalue;
    }

    public void setValue(double value) {
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
