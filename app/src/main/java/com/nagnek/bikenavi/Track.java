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
    public int start_poi_id;
    public int dest_poi_id;
    public ArrayList<Integer> stop_id_list;    // 경유지 리스트;
}
