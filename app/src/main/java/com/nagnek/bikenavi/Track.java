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
}
