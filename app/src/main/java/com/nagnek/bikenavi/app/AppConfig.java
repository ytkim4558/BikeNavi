/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.app;

import android.content.Context;

import com.skp.Tmap.TMapTapi;

/**
 * Created by user on 2016-09-27.
 */
public class AppConfig {
    public static String HOSTING_IP = "43.224.34.5";
    // Server user login url
    public static String URL_LOGIN = "http://" + HOSTING_IP + "/android_login_api/login.php";   // 회원 로그인 (카카오톡, 구글, 페이스북은 여기서 가입도 한다)
    // Server user register url
    public static String URL_REGISTER = "http://" + HOSTING_IP + "/android_login_api/register.php"; // 회원 가입,  자체 회원가입을 말한다
    public static String URL_POI_REGISTER_OR_UPDATE = "http://" + HOSTING_IP + "/android_login_api/request_add_or_update_poi.php";  // poi 정보를 추가하거나 업데이트
    public static String URL_POI_DELETE = "http://" + HOSTING_IP + "/android_login_api/request_delete_poi.php";  // poi 정보를 삭제
    public static String URL_POILIST_LOAD = "http://" + HOSTING_IP + "/android_login_api/request_range_user_poi.php";  // poi 정보를 추가하거나 업데이트
    public static String URL_GET_LAST_UESD_POI = "http://" + HOSTING_IP + "/android_login_api/request_last_used_user_poi.php";  // 마지막에 사용한 POI 가져오기
    public static String URL_TRACK_LIST_LOAD = "http://" + HOSTING_IP + "/android_login_api/request_range_user_track.php";  // poi 정보를 추가하거나 업데이트
    public static String URL_USER_TRACK_REGISTER_OR_UPDATE = "http://" + HOSTING_IP + "/android_login_api/request_add_or_update_user_track.php";   // 현재 경로를 유저의 최근,북마크 테이블에 추가, 수정하기 위한 주소
    public static String URL_USER_TRACK_DELETE = "http://" + HOSTING_IP + "/android_login_api/request_delete_user_track.php";   // 현재 경로삭제 하기 위한 주소
    public static String URL_USER_ERROR_REPORT = "http://" + HOSTING_IP + "/bikenavi_android_client_error_log.php";   // 에러 리포팅용
    private static TMapTapi tMapTapi = null;

    public static void setServerIp(String serverIp) {
        StringBuffer loginURLBuffer = new StringBuffer();
        loginURLBuffer.append("http://");
        loginURLBuffer.append(serverIp);
        loginURLBuffer.append("/android_login_api/login.php");
        URL_LOGIN = loginURLBuffer.toString().trim();

        StringBuffer registerURLBuffer = new StringBuffer();
        registerURLBuffer.append("http://");
        registerURLBuffer.append(serverIp);
        registerURLBuffer.append("/android_login_api/register.php");
        URL_REGISTER = registerURLBuffer.toString().trim();
    }

    public static void initializeTMapTapi(Context context) {
        if (tMapTapi == null) {
            tMapTapi = new TMapTapi(context.getApplicationContext());
            tMapTapi.setSKPMapAuthentication("d2bc2636-c213-3bad-9058-7d46cf9f8039");
        }
    }

    public enum ListType {BOOKMARK, RECENT}   // 리스트타입
}