/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.nagnek.bikenavi.MainActivity;
import com.nagnek.bikenavi.R;
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.app.AppController;
import com.nagnek.bikenavi.customview.ClearableAppCompatEditText;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by user on 2016-09-27.
 */
public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = RegisterActivity.class.getSimpleName();
    private static final int VALIDATE_MINIMUM_PASSWORD_COUNT = 4;
    private Button btnRegister;
    private Button btnLinkToLogin;
    private TextInputLayout ti_input_email;
    private ClearableAppCompatEditText inputEmail;
    private TextInputLayout ti_input_password;
    private ClearableAppCompatEditText inputPassword;
    private TextInputLayout ti_input_confirm_password;
    private ClearableAppCompatEditText inputPasswordConfirm;
    private ProgressDialog progressDialog;
    private SessionManager sessionManager;
    private SQLiteHandler db;

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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ti_input_email = (TextInputLayout) findViewById(R.id.ti_email);
        inputEmail = (ClearableAppCompatEditText) findViewById(R.id.email);
        ti_input_password = (TextInputLayout) findViewById(R.id.ti_password);
        inputPassword = (ClearableAppCompatEditText) findViewById(R.id.password);
        ti_input_confirm_password = (TextInputLayout) findViewById(R.id.ti_confirm_password);
        inputPasswordConfirm = (ClearableAppCompatEditText) findViewById(R.id.confirm_password);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnLinkToLogin = (Button) findViewById(R.id.btnLinkToLoginScreen);

        /**
         * textinputlayout 에러메시지 사용
         */
        ti_input_email.setErrorEnabled(true);
        ti_input_password.setErrorEnabled(true);
        ti_input_confirm_password.setErrorEnabled(true);

        ti_input_password.setCounterEnabled(true);
        ti_input_confirm_password.setCounterEnabled(true);

        // ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);    // 백키로 캔슬 가능안하게끔 설정

        // Session manager
        sessionManager = new SessionManager(getApplicationContext());

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // Check if user is already logged in or not
        if (sessionManager.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            Intent intent = new Intent(RegisterActivity.this,
                    MainActivity.class);
            startActivity(intent);
            finish();
        }

        // inputEmail watcher
        inputEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                inputEmail.setError(null);
                if (s.length() > 0) {
                    if (!isValidEmail(s)) {
                        ti_input_email.setError("유효한 이메일을 입력해주세요.");
                    } else {
                        ti_input_email.setError(null);
                    }
                } else {
                    ti_input_email.setError(null);
                }
            }
        });

        inputPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                inputPassword.setError(null);
                String password = inputPassword.getText().toString().trim();
                String passwordConfirm = inputPasswordConfirm.getText().toString().trim();
                if (!password.isEmpty() && !passwordConfirm.isEmpty()) {
                    if (!password.equals(passwordConfirm)) {
                        ti_input_password.setError("입력한 비밀번호가 서로 일치하지 않습니다.");
                    } else if (!isValidPassword(s)) {
                        ti_input_password.setError("최소 4자리 이상 설정해주세요.");
                    } else {
                        ti_input_password.setError(null);
                        ti_input_confirm_password.setError(null);
                    }
                } else if (!isValidPassword(s)) {
                    ti_input_password.setError("최소 4자리 이상 설정해주세요.");
                } else {
                    ti_input_password.setError(null);
                }
            }
        });

        inputPasswordConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                inputPassword.setError(null);
                String password = inputPassword.getText().toString().trim();
                String passwordConfirm = inputPasswordConfirm.getText().toString().trim();
                if (!password.isEmpty() && !passwordConfirm.isEmpty()) {
                    if (!password.equals(passwordConfirm)) {
                        ti_input_confirm_password.setError("입력한 비밀번호가 서로 일치하지 않습니다.");
                    } else if (!isValidPassword(s)) {
                        ti_input_confirm_password.setError("최소 4자리 이상 설정해주세요.");
                    } else {
                        ti_input_password.setError(null);
                        ti_input_confirm_password.setError(null);
                    }
                } else if (!isValidPassword(s)) {
                    ti_input_confirm_password.setError("최소 4자리 이상 설정해주세요.");
                } else {
                    ti_input_confirm_password.setError(null);
                }
            }
        });

        // Register Button Click event
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();
                String passwordConfirm = inputPasswordConfirm.getText().toString().trim();

                // 이메일 입력창이 비어있고 패스워드와 패스워드 확인 입력창이 비어있지 않을 때
                if (!email.isEmpty() && !password.isEmpty() && !passwordConfirm.isEmpty()) {
                    if (password.equals(passwordConfirm)) {
                        if (!isValidEmail(email)) {
                            inputEmail.setError("유효한 이메일을 입력해주세요!");
                            new AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("입력 유효성 에러")
                                    .setMessage("유효한 이메일을 입력해주세요!")
                                    .setNeutralButton("닫기", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .show();
                        } else if (!isValidPassword(password)) {
                            inputPassword.setError("유효한 패스워드를 입력해주세요!");
                            new AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("입력 유효성 에러")
                                    .setMessage("유효한 패스워드를 입력해주세요!")
                                    .setNeutralButton("닫기", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .show();
                        } else if (!isValidPassword(passwordConfirm)) {
                            inputPasswordConfirm.setError("유효한 패스워드 확인를 입력해주세요!");
                            new AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("입력 유효성 에러")
                                    .setMessage("유효한 패스워드 확인을 입력해주세요!")
                                    .setNeutralButton("닫기", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .show();
                        } else {
                            registerUser(email, password);
                        }

                    } else {
                        new AlertDialog.Builder(RegisterActivity.this)
                                .setTitle("입력 유효성 에러")
                                .setMessage("입력한 패스워드들이 일치하지 않습니다!")
                                .setNeutralButton("닫기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();
                    }
                } else {
                    final StringBuffer sb = new StringBuffer();
                    if (email.isEmpty()) {
                        inputEmail.setError("이메일을 입력해주세요.");
                        sb.append("이메일");
                        setShakeAnimation(ti_input_email);
                    }
                    if (password.isEmpty()) {
                        inputPassword.setError("비밀번호를 4자 이상 입력해주세요.");
                        if (!sb.toString().isEmpty()) {
                            sb.append(", ");
                        }
                        sb.append("비밀번호");

                        setShakeAnimation(ti_input_password);
                    } else {
                        if (!isValidPassword(password)) {
                            if (!sb.toString().isEmpty()) {
                                sb.append(", ");
                            }

                            sb.append("비밀번호");
                            setShakeAnimation(ti_input_password);
                        }
                    }

                    if (passwordConfirm.isEmpty()) {
                        inputPasswordConfirm.setError("비밀번호를 4자 이상 입력해주세요.");
                        if (!sb.toString().isEmpty()) {
                            sb.append(", ");
                        }

                        sb.append("비밀번호 확인");
                        setShakeAnimation(ti_input_confirm_password);
                    } else {
                        if (!isValidPassword(passwordConfirm)) {
                            if (!sb.toString().isEmpty()) {
                                sb.append(", ");
                            }

                            sb.append("비밀번호 확인");
                            setShakeAnimation(ti_input_confirm_password);
                        }
                    }

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Vibrator tVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                            tVibrator.vibrate(1000);

                            new AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("입력 유효성 에러")
                                    .setMessage(sb.toString() + "를 제대로 입력하세요.")
                                    .setNeutralButton("닫기", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .show();
                        }
                    }, 500);

                }
            }
        });

        // Link to Login Screen
        btnLinkToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),
                        LoginActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    /**
     * 애니메이션 설정
     */
    public void setShakeAnimation(View view) {
        view.startAnimation(shakeAnimation(5));
    }

    /**
     * 이메일 인증 시스템
     */
    public final boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    /**
     * 비밀번호 유효 확인
     */
    public final boolean isValidPassword(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return target.length() >= VALIDATE_MINIMUM_PASSWORD_COUNT;
        }
    }


    /**
     * Function to store user in MySQL database will post params(tag, email, password) to register url
     */
    private void registerUser(final String email, final String password) {
        // Tag used to cancel the request
        String tag_string_req = "req_register";

        progressDialog.setMessage("등록중 ...");
        showDialog();

        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                AppConfig.URL_REGISTER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("error");
                    if (!error) {
                        // User successfully stored in MySQL
                        // Now store the user in sqlite

                        JSONObject user = jsonObject.getJSONObject("user");
                        String email = user.getString("email");
                        String created_at = user.getString("created_at");

                        // Inserting row in users table
                        db.addUser(SQLiteHandler.UserType.BIKENAVI, email, created_at);

                        Toast.makeText(getApplicationContext(), "성공적으로 회원가입되었습니다. 지금 로그인하세요!", Toast.LENGTH_LONG).show();

                        // Launch login activity
                        Intent intent = new Intent(
                                RegisterActivity.this,
                                LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error occured in registration. Get the error message
                        String errorMsg = jsonObject.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);
                params.put("password", password);

                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(stringRequest, tag_string_req);
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
}