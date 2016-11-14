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
    private static volatile AppController mInstance;

    private RequestQueue mRequestQueue;


    public static void setCurrentActivity(Activity activity) {
//        AppController.currentActivity = activity;
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

