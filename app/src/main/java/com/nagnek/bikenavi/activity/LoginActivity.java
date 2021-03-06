/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;
import com.nagnek.bikenavi.R;
import com.nagnek.bikenavi.User;
import com.nagnek.bikenavi.WelcomeActivity;
import com.nagnek.bikenavi.app.AppConfig;
import com.nagnek.bikenavi.app.AppController;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;
import com.nagnek.bikenavi.kakao.KakaoSignupActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by user on 2016-09-27.
 * 자체 회원가입 및 구글 로그인용 가입 및 로그인
 */
public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    private static final boolean GET_HASH_KEY = false; // 어플의 해쉬키 카카오톡 로그인 때문에 필요
    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 9001; // 구글 로그인 요청 키
    /**
     * 페이스북
     */
    CallbackManager facebookCallbackManager;    // 페북 콜백매니저
    private Button btnLogin; // 자체 로그인
    private Button btnGoogleLogin; //구글 로그인
    private Button btnLinkToRegister; // 회원가입으로 가게하는 버튼
    private AppCompatEditText inputEmail;   // 이메일 입력창
    private AppCompatEditText inputPassword;    // 패스워드 입력창
    private GoogleApiClient mGoogleApiClient; // 구글 로그인등을 위한 구글 api 클라이언트
    private GoogleSignInOptions mGso; // 구글 로그인 후 유저 아이디나 기본 프로필 정보를 요청하기 위한 객체
    private ProgressDialog pDialog;
    private SessionManager session;
    private SQLiteHandler db;
    private TextInputLayout ti_input_email;
    /**
     * 카카오톡
     */
    private SessionCallback callback; // 콜백 선언

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppController.setCurrentActivity(this);
        // 페이스북
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());

        // facebook sdk 초기화부터 하고나서 setContentView를 해야한다.
        setContentView(R.layout.activity_login);

        // 카카오톡 등록하기 위한 1회용 코드
//        if(GET_HASH_KEY) {
//            getAppKeyHash();
//        }

        // SQLite database handler
        db = SQLiteHandler.getInstance(getApplicationContext());

        // Session manager
        session = new SessionManager(getApplicationContext());

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // 페이스북
        facebookCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(facebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                pDialog.setMessage("페북 로그인 시도중 ...");
                showDialog();
                Log.d(TAG, "페북 로긴 성공");
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.v(TAG, response.toString());

                                try {
                                    handleFacebookSignResult(object.getString("id"), object.getString("name"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id, name");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "onError");
            }
        });

        inputEmail = (AppCompatEditText) findViewById(R.id.email);
        inputPassword = (AppCompatEditText) findViewById(R.id.password);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnGoogleLogin = (Button) findViewById(R.id.sign_in_button);

        ti_input_email = (TextInputLayout) findViewById(R.id.ti_email);

        /**
         * textinputlayout 에러메시지 사용
         */
        ti_input_email.setErrorEnabled(true);

        btnLinkToRegister = (Button) findViewById(R.id.btnLinkToRegisterScreen);

        // Configure sign-in to request offline access to the user's ID, basic
        // profile, and Google Drive. The first time you request a code you will
        // be able to exchange it for an access token and refresh token, which
        // you should store. In subsequent calls, the code will only result in
        // an access token. By asking for profile access (through
        // DEFAULT_SIGN_IN) you will also get an ID Token as a result of the
        // code exchange.
        String serverClientId = getString(R.string.server_client_id);
//        mGso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
//                .requestServerAuthCode(serverClientId, false)
//                .build();
        mGso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestServerAuthCode(getString(R.string.server_client_id))
                .requestEmail()
                .build();

        // 구글 로그인 api에 접근하기 위한 googleapi 클라이언트 객체 생성.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGso).addApi(AppIndex.API)
                .build();

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activty
            Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
            startActivity(intent);
        }

        if (session.isGoogleLoggedIn()) {
            signIn();
        }

        // Login button Click Event
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                // Check for empty data in the form
                if (!email.isEmpty() && !password.isEmpty()) {
                    // login user
                    checkLogin(email, password);
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "이메일과 비밀번호를 입력하세요!", Toast.LENGTH_LONG).show();
                }
            }
        });

        // 구글 로그인 버튼 눌렀을 때
        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        // Link to Register Screen
        btnLinkToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),
                        RegisterActivity.class);
                startActivity(i);
                finish();
            }
        });

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

        /**
         * 카카오톡
         */
        Button kakaoLoginButton = (Button) findViewById(R.id.com_kakao_login);
        kakaoLoginButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                pDialog.setMessage("카카오톡 로그인 시도중...");
                // 카카오 세션을 오픈한다.
                callback = new SessionCallback();
                Session.getCurrentSession().addCallback(callback);
                Session.getCurrentSession().checkAndImplicitOpen();
                Session.getCurrentSession().open(AuthType.KAKAO_TALK_EXCLUDE_NATIVE_LOGIN, LoginActivity.this);
                showDialog();
            }
        });

        /**
         * 페이스북
         */
        Button facebookLoginButton = (Button) findViewById(R.id.facebook_login_button);
        facebookLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("public_profile"));
            }
        });
    }

    protected void redirectSignupActivity() {
        hideDialog();
        final Intent intent = new Intent(this, KakaoSignupActivity.class);
        startActivity(intent);
        finish();
    }

    private void getAppKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                Log.d("Hash key", something);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e("name not found", e.toString());
        }
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

    // 구글 로그인 버튼 눌렀을 때
    private void signIn() {
        pDialog.setMessage("구글 로그인 시도중 ...");
        showDialog();
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }

        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {  // 카카오톡 콜백.. 뭐하는거지? 처리?
            return;
        }

        // 페북 콜백
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        Session.getCurrentSession().removeCallback(callback); // 카카오톡 콜백 제거
    }

    private void handleFacebookSignResult(final String id, final String name) {
        // Tag used to cancel the request
        String tag_string_req = "req_facebook_login";

        // 성공적으로 로그인 하면 id 넘버와 이름을 가져온다.
        Log.d(TAG, "fb usre id: " + id);
        Log.d(TAG, "fb user name : " + name);

        // id 와 이름을 내 서버(회원가입쪽으로)로 HTTP POST를 이용해 보낸다
        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_REGISTER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "FB Register Response: " + response);
                hideDialog();

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        //user successfully logged in
                        // Create login session
                        session.setFacebookLogin(true);

                        // Now store the user in SQLite
                        JSONObject user = jsonObject.getJSONObject("user");
                        String name = user.getString("facebookName");
                        String id = user.getString("facebookID");
                        String created_at = user
                                .getString("created_at");
                        String updated_at = user.getString("updated_at");
                        String last_used_at = user.getString("last_used_at");
                        User facebookUser = new User();
                        facebookUser.facebook_user_name = name;
                        facebookUser.facebook_id = id;

                        // Inserting row in users table
                        db.addUser(SQLiteHandler.UserType.FACEBOOK, facebookUser, created_at, updated_at, last_used_at);
                        Log.d(TAG, "name : " + name);
                        Log.d(TAG, "created_at : " + created_at);

                        // Launch main activity
                        Intent intent = new Intent(LoginActivity.this,
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
                if (error instanceof TimeoutError) {
                    Log.e(TAG, "Login Error: 서버가 응답하지 않습니다." + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            "Login Error: 서버가 응답하지 않습니다.", Toast.LENGTH_LONG).show();
                } else if (error instanceof ServerError) {
                    Log.e(TAG, "서버 에러래" + error.getMessage());
                    VolleyLog.e(TAG, error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            "Login Error: 서버 Error.", Toast.LENGTH_LONG).show();
                } else {
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
                params.put("facebookID", id);
                params.put("facebookName", name);

                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    // 구글 로그인 처리
    private void handleSignInResult(GoogleSignInResult result) {
        // Tag used to cancel the request
        String tag_string_req = "req_google_login";


        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // 성공적으로 로그인 하면 auth code를 가져온다.
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            final String authCode = acct.getServerAuthCode();
            final String idToken = acct.getIdToken();
            Log.d(TAG, "authCode : " + authCode + "idToken : " + idToken);

            // authcode 및 idToken을 내 서버(회원가입쪽으로)로 HTTP POST를 이용해 보낸다
            StringRequest strReq = new StringRequest(Request.Method.POST,
                    AppConfig.URL_REGISTER, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Register Response: " + response);
                    hideDialog();

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean error = jsonObject.getBoolean("error");

                        // Check for error node in json
                        if (!error) {
                            //user successfully logged in
                            // Create login session
                            session.setGoogleLogin(true);

                            // Now store the user in SQLite
                            JSONObject user = jsonObject.getJSONObject("user");
                            String email = user.getString("googleemail");
                            String created_at = user
                                    .getString("created_at");
                            String updated_at = user.getString("updated_at");
                            String last_used_at = user.getString("last_used_at");
                            User googleUser = new User();
                            googleUser.google_email = email;

                            // Inserting row in users table
                            db.addUser(SQLiteHandler.UserType.GOOGLE, googleUser, created_at, updated_at, last_used_at);
                            Log.d(TAG, "email : " + email);
                            Log.d(TAG, "created_at : " + created_at);

                            // Launch main activity
                            Intent intent = new Intent(LoginActivity.this,
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
                    if (error instanceof TimeoutError) {
                        Log.e(TAG, "Login Error: 서버가 응답하지 않습니다." + error.getMessage());
                        VolleyLog.e(TAG, error.getMessage());
                        Toast.makeText(getApplicationContext(),
                                "Login Error: 서버가 응답하지 않습니다.", Toast.LENGTH_LONG).show();
                    } else if (error instanceof ServerError) {
                        Log.e(TAG, "서버 에러래" + error.getMessage());
                        VolleyLog.e(TAG, error.getMessage());
                        Toast.makeText(getApplicationContext(),
                                "Login Error: 서버 Error.", Toast.LENGTH_LONG).show();
                    } else {
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
                    params.put("google_authcode", authCode);
                    params.put("idToken", idToken);

                    return params;
                }
            };

            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        } else {
            switch (result.getStatus().getStatusCode()) {
                case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                    Log.d(TAG, "sign in canceled");
                    break;
                case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                    Log.d(TAG, "SIGN_IN_FAILED");
                    break;
                case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                    Log.d(TAG, "SIGN_IN_REQUIRED");
                    break;
                default:
                    Log.d(TAG, GoogleSignInStatusCodes.getStatusCodeString(result.getStatus().getStatusCode()));
            }
            Log.d(TAG, "에러 코드 : " + result.getStatus().getStatusCode() + "메시지 : " + GoogleSignInStatusCodes.getStatusCodeString(result.getStatus().getStatusCode()));

            // Signed out, show unauthenticated UI.
            Log.d(TAG, "fail~");
            String message = result.getStatus().getStatusMessage();
            if (message != null) {
                Log.d(TAG, result.getStatus().getStatusMessage());
            } else {
                Log.d(TAG, "에러");
            }
            Toast.makeText(this, "로그인 에러! 다시 시도해주세요! " + GoogleSignInStatusCodes.getStatusCodeString(result.getStatus().getStatusCode()), Toast.LENGTH_LONG);
            hideDialog();
        }

    }

    /**
     * function to verify login details in mysql db
     */
    private void checkLogin(final String email, final String password) {
        // Tag used to cancel the request
        String tag_string_req = "req_login";

        pDialog.setMessage("로그인 시도중 ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_LOGIN, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Login Response: " + response);
                hideDialog();

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        //user successfully logged in
                        // Create login session
                        session.setLogin(true);

                        // Now store the user in SQLite
                        JSONObject user = jsonObject.getJSONObject("user");
                        String email = user.getString("email");
                        String created_at = user
                                .getString("created_at");
                        String updated_at = user.getString("updated_at");
                        String last_used_at = user.getString("last_used_at");

                        User bikenaviUser = new User();
                        bikenaviUser.bike_navi_email = email;
                        // Inserting row in users table
                        db.addUser(SQLiteHandler.UserType.BIKENAVI, bikenaviUser, created_at, updated_at, last_used_at);
                        Log.d(TAG, "email : " + email);
                        Log.d(TAG, "created_at : " + created_at);

                        // Launch main activity
                        Intent intent = new Intent(LoginActivity.this,
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
                Log.e(TAG, "Login Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);
                params.put("password", password);

                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void showDialog() {
        if (!pDialog.isShowing()) {
            pDialog.show();
        }
    }

    private void hideDialog() {
        if (pDialog.isShowing()) {
            pDialog.dismiss();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private class SessionCallback implements ISessionCallback { // 카카오톡 콜백

        // 	access token을 성공적으로 발급 받아 valid access token을 가지고 있는 상태. 일반적으로 로그인 후의 다음 activity로 이동한다.
        @Override
        public void onSessionOpened() {
            redirectSignupActivity();
        }

        // 카카오톡 설명엔 : memory와 cache에 session 정보가 전혀 없는 상태. 일반적으로 로그인 버튼이 보이고 사용자가 클릭시 동의를 받아 access token 요청을 시도한다.
        // 함수 설명엔 : 로그인을 실패한 상태. 세션이 만료된 경우와는 다르게 네트웤등 일반적인 에러로 오픈에 실패한경우 불린다.
        // 아래 설명이 더 맞는것 같다.
        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if (exception != null) {
                Logger.e(exception);
            }
        }
    }
}
