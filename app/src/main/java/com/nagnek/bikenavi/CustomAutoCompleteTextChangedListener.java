/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;

/**
 * Created by user on 2016-10-06.
 */
public class CustomAutoCompleteTextChangedListener implements TextWatcher {

    public static final String TAG = "CustomAutoCompleteTextChangedListener.java";
    Context context;

    public CustomAutoCompleteTextChangedListener(Context context){
        this.context = context;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence userInput, int start, int before, int count) {
        MainActivity mainActivity = ((MainActivity) context);

        // query the database based on the user input
        mainActivity.serverIPs = mainActivity.getItemsFromDb(userInput.toString());

        // update the adapater
        mainActivity.arrayAdapter.notifyDataSetChanged();
        mainActivity.arrayAdapter = new ArrayAdapter<String>(mainActivity, android.R.layout.simple_dropdown_item_1line, mainActivity.serverIPs);
        mainActivity.serverIpAutoComplete.setAdapter(mainActivity.arrayAdapter);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
