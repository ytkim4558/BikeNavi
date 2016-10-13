/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.customview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;

import com.nagnek.bikenavi.R;

/**
 * Created by user on 2016-10-04.
 * 지울 수 있는 글자
 */
public class ClearableAppCompatEditText extends AppCompatEditText implements TextWatcher, View.OnTouchListener, View.OnFocusChangeListener {

    private Drawable clearDrawable;
    private OnFocusChangeListener onFocusChangeListener;
    private OnTouchListener onTouchListener;

    public ClearableAppCompatEditText(final Context context) {
        super(context);
        init();
    }

    public ClearableAppCompatEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClearableAppCompatEditText(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 입력창이 흔들리는 애니메이션 (counts 횟수만큼)
     *
     * @param counts 애니메이션 횟수
     * @return
     */
    public static Animation shakeAnimation(int counts) {
        // 이동 애니메이션
        Animation translateAnimation = new TranslateAnimation(0, 30, 0, 0);
        translateAnimation.setInterpolator(new CycleInterpolator(counts));
        translateAnimation.setDuration(100);
        return translateAnimation;
    }

    @Override
    public void setOnFocusChangeListener(OnFocusChangeListener onFocusChangeListener) {
        this.onFocusChangeListener = onFocusChangeListener;
    }

    @Override
    public void setOnTouchListener(OnTouchListener onTouchListener) {
        this.onTouchListener = onTouchListener;
    }

    private void init() {

        Drawable tempDrawable = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_clear_material);
        clearDrawable = DrawableCompat.wrap(tempDrawable);
        DrawableCompat.setTintList(clearDrawable, getHintTextColors());
        clearDrawable.setBounds(0, 0, clearDrawable.getIntrinsicWidth(), clearDrawable.getIntrinsicHeight());

        setClearIconVisible(false);


        super.setOnTouchListener(this);
        super.setOnFocusChangeListener(this);
        addTextChangedListener(this);
    }

    @Override
    public void onFocusChange(final View view, final boolean hasFocus) {
        if (hasFocus) {
            setClearIconVisible(getText().length() > 0);
        } else {
            setClearIconVisible(false);
        }

        if (onFocusChangeListener != null) {
            onFocusChangeListener.onFocusChange(view, hasFocus);
        }
    }

    @Override
    public boolean onTouch(final View view, final MotionEvent motionEvent) {
        final int x = (int) motionEvent.getX();
        if (clearDrawable.isVisible() && x > getWidth() - getPaddingRight() - clearDrawable.getIntrinsicWidth()) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                setError(null);
                setText(null);
            }
            return true;
        }

        if (onTouchListener != null) {
            return onTouchListener.onTouch(view, motionEvent);
        } else {
            return false;
        }

    }

    @Override
    public final void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        if (isFocused()) {
            setClearIconVisible(s.length() > 0);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private void setClearIconVisible(boolean visible) {
        clearDrawable.setVisible(visible, false);
        setCompoundDrawables(null, null, visible ? clearDrawable : null, null);
    }

    /**
     * 애니메이션 설정
     */
    public void setShakeAnimation() {
        this.startAnimation(shakeAnimation(5));
    }
}
