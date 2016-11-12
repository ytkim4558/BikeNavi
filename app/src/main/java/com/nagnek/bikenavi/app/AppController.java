/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.multidex.MultiDex;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kakao.auth.KakaoSDK;
import com.nagnek.bikenavi.helper.SQLiteHandler;
import com.nagnek.bikenavi.helper.SessionManager;
import com.nagnek.bikenavi.kakao.KakaoSDKAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

/**
 * Created by yongtak on 2016-09-27.
 * 앱 시작할때 실행됨.
 * 카카오톡 로그인을 위한 소스추가 : 2016-10-09 참고 : http://uareuni.tistory.com/13
 */

public class AppController extends Application {

    /**
     * 자체로그인 위함.
     */
    public static final String TAG = AppController.class.getSimpleName();
    public static Display mDisplay;
    /**
     * 카카오톡 로그인 위한 소스추가
     */
    private static volatile AppController instance = null;
    private static volatile Activity currentActivity = null;
    private static AppController mInstance;
    /**
     * 크래쉬 방지용 코드 추가
     * 참조 : http://www.kmshack.kr/2013/03/uncaughtexceptionhandler%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%9C-%EC%95%B1-%EB%B9%84%EC%A0%95%EC%83%81-%EC%A2%85%EB%A3%8C%EC%8B%9C-log%EC%A0%84%EC%86%A1-%EB%B0%8F-%EC%9E%AC%EC%8B%A4%ED%96%89-%ED%95%98/
     */
    private Thread.UncaughtExceptionHandler mUncaughtExceptionhandler;
    private ImageLoader imageLoader;
    private RequestQueue mRequestQueue;
    private SessionManager session; // 로그인했는지 확인용 변수

    /**
     * 카카오톡
     *
     * @param
     */
    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    public static void setCurrentActivity(Activity activity) {
        AppController.currentActivity = activity;
    }

    /**
     * singleton 애플리케이션 객체를 얻는다.
     *
     * @return singleton 애플리케이션 객체
     */
    public static AppController getGlobalApplicationContext() {
        if (instance == null)
            throw new IllegalStateException("this application does not inherit com.kakao.GlobalApplication");
        return instance;
    }

    public static int getDisplayWidth() {
        return mDisplay.getWidth();
    }

    public static int getDisplayHeight() {
        return mDisplay.getHeight();
    }

    public static synchronized AppController getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {

        /**
         * 크래쉬 방지용 코드 추가
         * 참조 : http://www.kmshack.kr/2013/03/uncaughtexceptionhandler%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%9C-%EC%95%B1-%EB%B9%84%EC%A0%95%EC%83%81-%EC%A2%85%EB%A3%8C%EC%8B%9C-log%EC%A0%84%EC%86%A1-%EB%B0%8F-%EC%9E%AC%EC%8B%A4%ED%96%89-%ED%95%98/
         */
        Log.d(TAG, "onCreate시작");
        mUncaughtExceptionhandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new uncaughtExceptionHandlerApplication());

        super.onCreate();
        mInstance = this;

        /**
         * 카카오톡
         * 이미지 로더, 이미지 캐시, 요청 큐를 초기화한다.
         */
        instance = this;

        // Session manager
        session = new SessionManager(getApplicationContext());

        Log.d(TAG, "카카오톡 시작전");
        KakaoSDK.init(new KakaoSDKAdapter());

        final RequestQueue requestQueue = Volley.newRequestQueue(this);

        ImageLoader.ImageCache imageCache = new ImageLoader.ImageCache() {
            final LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(3);

            @Override
            public Bitmap getBitmap(String key) {
                return imageCache.get(key);
            }

            @Override
            public void putBitmap(String key, Bitmap value) {
                imageCache.put(key, value);
            }
        };

        imageLoader = new ImageLoader(requestQueue, imageCache);
        Log.d(TAG, "AppController시작");
    }

    /**
     * 크래쉬 방지용 코드 추가
     * 참조 : http://www.kmshack.kr/2013/03/uncaughtexceptionhandler%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%9C-%EC%95%B1-%EB%B9%84%EC%A0%95%EC%83%81-%EC%A2%85%EB%A3%8C%EC%8B%9C-log%EC%A0%84%EC%86%A1-%EB%B0%8F-%EC%9E%AC%EC%8B%A4%ED%96%89-%ED%95%98/
     * 메시지로 변환
     */
    private String getStackTrace(Throwable th) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);

        Throwable cause = th;
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        final String stacktraceAsString = result.toString();
        printWriter.close();

        return stacktraceAsString;
    }

    // 에러 전송
    private void sendErrorReportToServer(final String errorMessage) {

        // Tag used to cancel the request
        String tag_string_req = "send_error_report.";

        //JsonArrayRequest of volley
        final StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_USER_ERROR_REPORT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Calling method parsePOIList to parse the json response
                        try {
                            Log.d(TAG, "response : " + response);
                            JSONObject jsonObject = new JSONObject(response);
                            boolean error = jsonObject.getBoolean("error");

                            if (!error) {
                                Log.d(TAG, "에러전송완료");
                            } else {
                                // Error in login. Get the error message
                                String errorMsg = jsonObject.getString("error_msg");
                                Log.d(TAG, errorMsg);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d(TAG, e.toString());
                        }
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //If an error occurs that means end of the list has reached
                        Log.d(TAG, error.toString());
                    }
                }) {
            @Override
            protected HashMap<String, String> getParams() {
                HashMap<String, String> mRequestParams = new HashMap<String, String>();
                if (session.isSessionLoggedIn()) {
                    mRequestParams = inputUserInfoToInputParams(mRequestParams);
                }
                mRequestParams.put("MESSAGE", errorMessage);

                return mRequestParams;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private HashMap<String, String> inputUserInfoToInputParams(HashMap<String, String> params) {
        SessionManager session; // 로그인했는지 확인용 변수
        SQLiteHandler db;   // sqlite
        // SqLite database handler 초기화
        db = SQLiteHandler.getInstance(this);
        // Session manager
        session = new SessionManager(this);
        SQLiteHandler.UserType loginUserType = session.getUserType();
        HashMap<String, String> user = db.getLoginedUserDetails(loginUserType);

        switch (loginUserType) {
            case BIKENAVI:
                String email = user.get(SQLiteHandler.KEY_EMAIL);
                params.put("email", email);
                Log.d(TAG, "bikenavi타입 유저네" + email);
                break;
            case GOOGLE:
                String googleemail = user.get(SQLiteHandler.KEY_GOOGLE_EMAIL);
                params.put("googleemail", googleemail);
                Log.d(TAG, "구글 유저네" + googleemail);
                break;
            case KAKAO:
                String kakaoId = user.get(SQLiteHandler.KEY_KAKAO_ID);
                params.put("kakaoid", kakaoId);
                Log.d(TAG, "카카오 유저네" + kakaoId);
                break;
            case FACEBOOK:
                String facebookId = user.get(SQLiteHandler.KEY_FACEBOOK_ID);
                params.put("facebookid", facebookId);
                Log.d(TAG, "페북 유저네" + facebookId);
                break;
        }

        return params;

    }

    /**
     * 이미지 로더를 반환한다.
     *
     */
    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    /**
     * 애플리케이션 종료시 singleton 어플리케이션 객체 초기화한다.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        instance = null;
    }

    public int resize_Height(int width, int height, int resize_width) {
        return (getDisplayHeight() * resize_width) / getDisplayWidth();
    }

    // 참고 : https://developer.android.com/reference/android/support/multidex/MultiDexApplication.html
    // multidex 에러 해결용
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // Instantiate the cache
            Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1mb cap

            // Set up the network to use HttpURLConnection as the HTTP client.
            Network network = new BasicNetwork(new HurlStack());

            // Instantiate the RequestQueue with the cache and network.
            mRequestQueue = new RequestQueue(cache, network);

            // Start the queue
            mRequestQueue.start();
        }

        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

    /**
     * 크래쉬 방지용 코드 추가
     * 참조 : http://www.kmshack.kr/2013/03/uncaughtexceptionhandler%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%9C-%EC%95%B1-%EB%B9%84%EC%A0%95%EC%83%81-%EC%A2%85%EB%A3%8C%EC%8B%9C-log%EC%A0%84%EC%86%A1-%EB%B0%8F-%EC%9E%AC%EC%8B%A4%ED%96%89-%ED%95%98/
     */
    class uncaughtExceptionHandlerApplication implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            // 예외상황이 발행 되는 경우 작업
            Log.e("Error", getStackTrace(ex));
            sendErrorReportToServer(getStackTrace(ex));

            // 예외처리를 하지 않고 DefaultUncaughtException으로 넘긴다.
            mUncaughtExceptionhandler.uncaughtException(thread, ex);
        }
    }
}

