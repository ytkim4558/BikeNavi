/*
 * Copyright (c) 2017. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.customview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

import com.nagnek.bikenavi.R;

/**
 * Created by ytkim on 2017-03-01.
 * 글자 외곽선 라인을 가진 BorderTextView
 */

public class BorderTextView extends TextView {
    private boolean stroke = false;
    private float strokeWidth = 0.0f;
    private int strokeColor;

    public BorderTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initView(context, attrs);
    }

    public BorderTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initView(context, attrs);
    }

    public BorderTextView(Context context) {
        super(context);
    }

    private void initView(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BorderTextView);
        stroke = a.getBoolean(R.styleable.BorderTextView_textStroke, false);
        strokeWidth = a.getFloat(R.styleable.BorderTextView_textStrokeWidth, 0.0f);
        strokeColor = a.getColor(R.styleable.BorderTextView_textStrokeColor, 0xffffffff);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (stroke) {
            ColorStateList states = getTextColors();
            getPaint().setStyle(Paint.Style.STROKE);
            getPaint().setStrokeWidth(strokeWidth);
            setTextColor(strokeColor);
            super.onDraw(canvas);

            getPaint().setStyle(Paint.Style.FILL);
            setTextColor(states);
        }

        super.onDraw(canvas);
    }
}
