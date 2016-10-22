/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import java.io.Serializable;

/**
 * Created by user on 2016-10-21.
 */

public class POI implements Serializable{
    public String name; // 장소이름
    public String address; // 장소 주소
    public String latLng; // 장소 좌표
    public String last_used_at; // 마지막으로 사용한 시각
}
