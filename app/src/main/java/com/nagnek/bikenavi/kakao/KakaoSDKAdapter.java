/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.kakao;

import android.app.Activity;
import android.content.Context;

import com.kakao.auth.ApprovalType;
import com.kakao.auth.AuthType;
import com.kakao.auth.IApplicationConfig;
import com.kakao.auth.ISessionConfig;
import com.kakao.auth.KakaoAdapter;
import com.nagnek.bikenavi.app.AppController;

/**
 * Created by ytkim on 2016-10-09.
 */
public class KakaoSDKAdapter extends KakaoAdapter {
    /**
     * Session COnfig 대해서는 default 값이 존재한다.
     * 필요한 상황에서만 override해서 사용하면 됨.
     *
     * @return Session 설정값.
     */
    @Override
    public ISessionConfig getSessionConfig() {
        return new ISessionConfig() {
            @Override
            public AuthType[] getAuthTypes() {
                return new AuthType[]{AuthType.KAKAO_LOGIN_ALL};
            }

            @Override
            public boolean isUsingWebviewTimer() {
                return false;
            }

            @Override
            public boolean isSecureMode() {
                return false;
            }

            @Override
            public ApprovalType getApprovalType() {
                return ApprovalType.INDIVIDUAL;
            }

            @Override
            public boolean isSaveFormData() {
                return false;
            }
        };
    }

    @Override
    public IApplicationConfig getApplicationConfig() {
        return new IApplicationConfig() {
            @Override
            public Activity getTopActivity() {
                return AppController.getCurrentActivity();
            }

            @Override
            public Context getApplicationContext() {
                return AppController.getGlobalApplicationContext();
            }
        };
    }
}
