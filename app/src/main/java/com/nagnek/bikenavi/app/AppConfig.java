/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.app;

/**
 * Created by user on 2016-09-27.
 */
public class AppConfig {
    // Server user login url
    public static String URL_LOGIN = "http://43.224.34.5/android_login_api/login.php";

    // Server user register url
    public static String URL_REGISTER = "http://43.224.34.5/android_login_api/register.php";

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
}