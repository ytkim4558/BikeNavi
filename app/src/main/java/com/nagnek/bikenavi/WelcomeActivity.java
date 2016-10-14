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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.nagnek.bikenavi.activity.LoginActivity;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;

import java.util.HashMap;

/**
 * Created by user on 2016-09-28.
 */
public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = WelcomeActivity.class.getSimpleName();
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
        HashMap<String, String> user = null;
        String email = null;
        if (sessionManager.isLoggedIn()) {
            user = db.getUserDetails(SQLiteHandler.UserType.BIKENAVI);
            email = user.get("email");
        } else if (sessionManager.isGoogleLoggedIn()) {
            user = db.getUserDetails(SQLiteHandler.UserType.GOOGLE);
            email = user.get("googleemail");
        } else if (sessionManager.isFacebookIn()) {
            user = db.getUserDetails(SQLiteHandler.UserType.FACEBOOK);
            email = user.get("facebookemail");
        } else if (sessionManager.isKakaoLoggedIn()) {
            Log.d(TAG, "카카오로긴");
            user = db.getUserDetails(SQLiteHandler.UserType.KAKAO);
            email = user.get("kakaoemail");
        }

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

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     */
    private void logoutUser() {
        db.deleteUsers();
        mSplashLodingHandler.removeCallbacks(mSplashRunnable);
        if (sessionManager.isLoggedIn()) {
            sessionManager.setLogin(false);
            redirectLoginActivity();
        } else if (sessionManager.isGoogleLoggedIn()) {
            sessionManager.setGoogleLogin(false);
            redirectLoginActivity();
        } else if (sessionManager.isKakaoLoggedIn()) {
            Log.d(TAG, "카카오로갓");
            sessionManager.setKakaoLogin(false);
            UserManagement.requestLogout(new LogoutResponseCallback() {
                @Override
                public void onCompleteLogout() {
                    Log.d(TAG, "로갓 성공");
                    redirectLoginActivity();
                }

                @Override
                public void onFailure(ErrorResult errorResult) {

                    super.onFailure(errorResult);
                    redirectLoginActivity();
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) {

                    super.onSessionClosed(errorResult);
                    redirectLoginActivity();
                }

                @Override
                public void onSuccess(Long result) {
                    super.onSuccess(result);
                    redirectLoginActivity();
                }

                @Override
                public void onNotSignedUp() {
                    super.onNotSignedUp();
                    redirectLoginActivity();
                }

                @Override
                public void onDidEnd() {
                    super.onDidEnd();
                    redirectLoginActivity();
                }

                @Override
                public void onFailureForUiThread(ErrorResult errorResult) {
                    super.onFailureForUiThread(errorResult);
                    redirectLoginActivity();
                }

            });
        } else if (sessionManager.isFacebookIn()) {
            sessionManager.setFacebookLogin(false);
            redirectLoginActivity();
        } else {
            redirectLoginActivity();
        }
    }

    private void redirectLoginActivity() { // Launching the login activity
        Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // 로딩화면
    private class SplashRunnable implements Runnable {
        @Override
        public void run() {
            WelcomeActivity.this.finish();
        }
    }
}
