/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.kakao;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.kakao.auth.ErrorCode;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.helper.log.Logger;
import com.nagnek.bikenavi.WelcomeActivity;
import com.nagnek.bikenavi.activity.LoginActivity;
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.app.AppController;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class KakaoSignupActivity extends AppCompatActivity {

    private static final String TAG = KakaoSignupActivity.class.getSimpleName();
    private ProgressDialog progressDialog;
    private SQLiteHandler db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);    // 백키로 캔슬 가능안하게끔 설정
        progressDialog.setMessage("카카오 로그인 시도중..");
        showDialog();

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // Session manager
        session = new SessionManager(getApplicationContext());

        Log.d(TAG, "kakao signup");
        requestMe();
    }

    /**
     * 사용자의 상태를 알아보기 위해 me API 호출을 한다.
     */
    protected void requestMe() { // 유저의 정보를 받아오는 함수
        UserManagement.requestMe(new MeResponseCallback() {
            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                hideDialog();
                Log.d(TAG, errorResult.toString());
                redirectLoginActivity();
            }

            @Override
            public void onNotSignedUp() {
                hideDialog();
                // 카카오톡 회원이 아닐 시 showSignup(); 호출해야함.
            }

            @Override
            public void onSuccess(UserProfile result) { // 성공 시 userProfile 형태로 반환
                hideDialog();
                String message = "UserProfile : " + result;
                Logger.d("UserProfile : " + result);
                Toast.makeText(KakaoSignupActivity.this, message, Toast.LENGTH_LONG);

                Log.d("test", "로그인 성공");

                registerKakaoUser(result.getNickname());
            }

            @Override
            public void onFailure(ErrorResult errorResult) {
                hideDialog();
                String message = "failured to get user info. msg=" + errorResult;
                Toast.makeText(KakaoSignupActivity.this, message, Toast.LENGTH_LONG);
                Logger.d(message);

                ErrorCode result = ErrorCode.valueOf(errorResult.getErrorCode());
                if (result == ErrorCode.CLIENT_ERROR_CODE) {
                    finish();
                } else {
                    redirectLoginActivity();
                }
            }
        });
    }

    private void registerKakaoUser(final String nickname) {
        // authcode 및 idToken을 내 서버(회원가입쪽으로)로 HTTP POST를 이용해 보낸다

        String tag_string_req = "req_kakao_login";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_REGISTER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "KaKao Register Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        //user successfully logged in
                        // Create login session
                        session.setKakaoLogin(true);

                        // Now store the user in SQLite
                        JSONObject user = jsonObject.getJSONObject("user");
                        String email = user.getString("kakaoemail");
                        String created_at = user
                                .getString("created_at");

                        // Inserting row in users table
                        db.addUser(SQLiteHandler.UserType.KAKAO, email, created_at);
                        Log.d(TAG, "email : " + email);
                        Log.d(TAG, "created_at : " + created_at);

                        // Launch main activity
                        Intent intent = new Intent(KakaoSignupActivity.this,
                                WelcomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jsonObject.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error instanceof TimeoutError) {
                    Log.e(TAG, "Login Error: 서버가 응답하지 않습니다." + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            "Login Error: 서버가 응답하지 않습니다.", Toast.LENGTH_LONG).show();
                } else if(error instanceof ServerError){
                    Log.e(TAG, "서버 에러래" + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            "Login Error: 서버 Error.", Toast.LENGTH_LONG).show();
                }else {
                    Log.e(TAG, error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            error.getMessage(), Toast.LENGTH_LONG).show();
                }

                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("kakaoNickName", nickname);

                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void showDialog() {
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideDialog() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void redirectMainActivity() {
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }

    protected void redirectLoginActivity() {
        final Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }
}
