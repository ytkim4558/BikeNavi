/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.nagnek.bikenavi.app.AppController;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;

import java.util.HashMap;

/**
 * Created by user on 2016-09-28.
 */
public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = WelcomeActivity.class.getSimpleName();
    private TextView textEmail;

    private SQLiteHandler db;
    private SessionManager sessionManager;

    private SplashRunnable mSplashRunnable;
    private Handler mSplashLodingHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        AppController.setCurrentActivity(this);

        textEmail = (TextView) findViewById(R.id.email);

        // SqLite database handler
        db = SQLiteHandler.getInstance(getApplicationContext());

        // session manager
        sessionManager = new SessionManager(getApplicationContext());
//
//        if (!sessionManager.isLoggedIn()) {
//            logoutUser();
//        }

        // Fetching user details from sqlite
        HashMap<String, String> user = null;
        String email = null;
        if (sessionManager.isLoggedIn()) {
            user = db.getUserNickname(SQLiteHandler.UserType.BIKENAVI);
            email = user.get(SQLiteHandler.KEY_EMAIL);
        } else if (sessionManager.isGoogleLoggedIn()) {
            user = db.getUserNickname(SQLiteHandler.UserType.GOOGLE);
            email = user.get(SQLiteHandler.KEY_GOOGLE_EMAIL);
        } else if (sessionManager.isFacebookIn()) {
            user = db.getUserNickname(SQLiteHandler.UserType.FACEBOOK);
            email = user.get(SQLiteHandler.KEY_FACEBOOK_NAME);
        } else if (sessionManager.isKakaoLoggedIn()) {
            Log.d(TAG, "카카오로긴");
            user = db.getUserNickname(SQLiteHandler.UserType.KAKAO);
            email = user.get(SQLiteHandler.KEY_KAKAO_NICK_NAME);
        }

        // Displaying th euser details on the screen
        textEmail.setText(email + "님 환영합니다.");

        mSplashLodingHandler = new Handler();
        mSplashRunnable = new SplashRunnable();
        mSplashLodingHandler.postDelayed(mSplashRunnable, 3000);
    }

    // 로딩화면
    private class SplashRunnable implements Runnable {
        @Override
        public void run() {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
