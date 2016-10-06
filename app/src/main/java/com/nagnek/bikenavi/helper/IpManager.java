/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by user on 2016-10-06.
 */
public class IPManager {
    // LogCat tag
    private static String TAG = IPManager.class.getSimpleName();

    // Shared Preferences
    SharedPreferences pref;

    SharedPreferences.Editor editor;
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "BIKENAVIServerIP";

    private static final String KEY_SERVER_IP = "ServerIP";

    public IPManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void saveServerIP(String serverIP) {
        editor.putString(KEY_SERVER_IP, serverIP);

        // commit changes
        editor.commit();

        Log.d(TAG, "Server IP modified!");
    }

    public String loadServerIP() {
        return pref.getString(KEY_SERVER_IP, null);
    }
}
