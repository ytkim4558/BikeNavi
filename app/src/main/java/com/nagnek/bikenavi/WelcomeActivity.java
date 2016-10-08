/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nagnek.bikenavi.activity.LoginActivity;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;

import java.util.HashMap;

/**
 * Created by user on 2016-09-28.
 */
public class WelcomeActivity extends AppCompatActivity{
    private TextView textEmail;
    private Button btnLogout;

    private SQLiteHandler db;
    private SessionManager sessionManager;

    private SplashRunnable mSplashRunnable;
    private Handler mSplashLodingHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        textEmail = (TextView) findViewById(R.id.email);
        btnLogout = (Button) findViewById(R.id.btnLogout);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        sessionManager = new SessionManager(getApplicationContext());
//
//        if (!sessionManager.isLoggedIn()) {
//            logoutUser();
//        }

        // Fetching user details from sqlite
        HashMap<String, String> user = db.getUserDetails();

        String email = user.get("email");

        // Displaying th euser details on the screen
        textEmail.setText(email + "님 환영합니다.");

        // Logout button click event
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        mSplashLodingHandler = new Handler();
        mSplashRunnable = new SplashRunnable();
        mSplashLodingHandler.postDelayed(mSplashRunnable, 3000);
    }

    // 로딩화면
    private class SplashRunnable implements Runnable {
        @Override
        public void run() {
            WelcomeActivity.this.finish();
        }
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     */
    private void logoutUser() {
        mSplashLodingHandler.removeCallbacks(mSplashRunnable);
        if(sessionManager.isLoggedIn()) {
            sessionManager.setLogin(false);
        } else {
            sessionManager.setGoogleLogin(false);
        }
        db.deleteUsers();
        // Launching the login activity
        Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
