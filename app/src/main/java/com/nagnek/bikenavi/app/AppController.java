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
import android.view.Display;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.kakao.auth.KakaoSDK;
import com.nagnek.bikenavi.kakao.KakaoSDKAdapter;

/**
 * Created by yongtak on 2016-09-27.
 * 앱 시작할때 실행됨.
 * 카카오톡 로그인을 위한 소스추가 : 2016-10-09 참고 : http://uareuni.tistory.com/13
 */
public class AppController extends Application {

    /**
     * 카카오톡 로그인 위한 소스추가
     */
    private static volatile AppController instance = null;
    private static volatile Activity currentActivity = null;
    private ImageLoader imageLoader;

    /**
     * 자체로그인 위함.
     */
    public static final String TAG = AppController.class.getSimpleName();

    private RequestQueue mRequestQueue;

    private static AppController mInstance;

    /**
     * 카카오톡
     * @param
     */
    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    public static void setCurrentActivity(Activity activity) {
        AppController.currentActivity = currentActivity;
    }

    /**
     * singleton 애플리케이션 객체를 얻는다.
     * @return singleton 애플리케이션 객체
     */
    public static AppController getGlobalApplicationContext() {
        if (instance == null)
            throw new IllegalStateException("this application does not inherit com.kakao.GlobalApplication");
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        /**
         * 카카오톡
         * 이미지 로더, 이미지 캐시, 요청 큐를 초기화한다.
         */
        instance = this;

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
    }

    /**
     * 이미지 로더를 반환한다.
     * @param 이미지 로더
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

    public static Display mDisplay;

    public static int getDisplayWidth() {
        return mDisplay.getWidth();
    }

    public static int getDisplayHeight() {
        return mDisplay.getHeight();
    }

    public int resize_Height(int width, int height, int resize_width) {
        return (getDisplayHeight()*resize_width)/getDisplayWidth();
    }










    // 참고 : https://developer.android.com/reference/android/support/multidex/MultiDexApplication.html
    // multidex 에러 해결용
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static synchronized AppController getInstance() {
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
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
}

