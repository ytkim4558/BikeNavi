/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.customview;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by user on 2016-10-06.
 */
public class ClearableSqliteAutoCompleteTextView extends ClearableAutoCompleteTextView {

    public ClearableSqliteAutoCompleteTextView(Context context) {
        super(context);
    }

    public ClearableSqliteAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClearableSqliteAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // this is how to disable AutoCompleteTextView filter
    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        String filterText = "";
        super.performFiltering(filterText, keyCode);
    }

    /**
     * after selection we have to capture the new value and append to the existing text
     */
    @Override
    protected void replaceText(CharSequence text) {
        super.replaceText(text);
    }
}
