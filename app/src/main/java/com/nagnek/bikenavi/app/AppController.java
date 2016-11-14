/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.app;

import android.app.Activity;
import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.kakao.auth.KakaoSDK;
import com.nagnek.bikenavi.helper.SessionManager;
import com.nagnek.bikenavi.kakao.KakaoSDKAdapter;

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
    /**
     * 카카오톡 로그인 위한 소스추가
     */
    private static volatile AppController instance = null;
    private static volatile Activity currentActivity = null;
    private static volatile AppController mInstance;
    /**
     * 크래쉬 방지용 코드 추가
     * 참조 : http://www.kmshack.kr/2013/03/uncaughtexceptionhandler%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%9C-%EC%95%B1-%EB%B9%84%EC%A0%95%EC%83%81-%EC%A2%85%EB%A3%8C%EC%8B%9C-log%EC%A0%84%EC%86%A1-%EB%B0%8F-%EC%9E%AC%EC%8B%A4%ED%96%89-%ED%95%98/
     */
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
        Log.d(TAG, "AppController시작");
    }

    /**
     * 애플리케이션 종료시 singleton 어플리케이션 객체 초기화한다.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        mInstance = null;
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
}

