/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * Created by user on 2016-09-27.
 */
public class SessionManager {
    // LogCat tag
    private static String TAG = SessionManager.class.getSimpleName();

    // Shared Preferences
    SharedPreferences pref;

    Editor editor;
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "AndroidHiveLogin";

    private static final String KEY_IS_LOGGEDIN = "isLoggedIn";

    // 구글에 로그인 되어있는 상태
    private static final String KEY_IS_GOOGLE_LOGGEDIN = "isGoogleLoggedIn";

    // 카카오에 로그인 되어있는 상태
    private static final String KEY_IS_KAKAO_LOGGEDIN = "isKakaoLoggedIn";

    // 페북에 로그인 되어있는 상태
    private static final String KEY_IS_FACEBOOK_LOGGEDIN = "isFaceBookLoggedIn";

    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setLogin(boolean isLoggedIn) {
        editor.putBoolean(KEY_IS_LOGGEDIN, isLoggedIn);

        // commit changes
        editor.commit();

        Log.d(TAG, "User login session modified!");
    }

    public void setGoogleLogin(boolean isGoogleLoggedIn) {
        editor.putBoolean(KEY_IS_GOOGLE_LOGGEDIN, isGoogleLoggedIn);

        // commit changes
        editor.commit();

        Log.d(TAG, "User Google login session modified!");
    }

    public void setKakaoLogin(boolean isKakaoLoggedIn) {
        editor.putBoolean(KEY_IS_KAKAO_LOGGEDIN, isKakaoLoggedIn);

        // commit changes
        editor.commit();

        Log.d(TAG, "User Kakao login session modified!");
    }

    public void setFacebookLogin(boolean isFacebookLoggedIn) {
        editor.putBoolean(KEY_IS_FACEBOOK_LOGGEDIN, isFacebookLoggedIn);

        // commit changes
        editor.commit();

        Log.d(TAG, "User Facebook login session modified!");
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGEDIN, false);
    }

    public boolean isGoogleLoggedIn() {
        return pref.getBoolean(KEY_IS_GOOGLE_LOGGEDIN, false);
    }

    public boolean isFacebookIn() {
        return pref.getBoolean(KEY_IS_FACEBOOK_LOGGEDIN, false);
    }

    public boolean isKakaoLoggedIn() {
        return pref.getBoolean(KEY_IS_KAKAO_LOGGEDIN, false);
    }
}
