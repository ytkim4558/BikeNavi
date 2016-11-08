/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by user on 2016-10-21.
 */

public class Track implements Serializable {
    public POI startPOI;
    public POI destPOI;
    public ArrayList<POI> stop_poi_list;    // 경유지 리스트;
    public String created_at;
    public String updated_at;
    public String last_used_at;
    boolean bookmarked; // 북마크되었는지 여부 - 서버용 변수
}
