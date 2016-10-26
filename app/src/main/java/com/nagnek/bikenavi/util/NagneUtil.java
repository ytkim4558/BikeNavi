/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.util;

import android.content.Context;

/**
 * Created by user on 2016-10-26.
 */

public class NagneUtil {
    public static String getStringFromResources(Context context, final int id) {
        return context.getApplicationContext().getResources().getString(id);
    }
}
