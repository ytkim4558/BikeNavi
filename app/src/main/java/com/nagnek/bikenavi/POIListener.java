/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

/**
 * Created by user on 2016-10-21.
 */

public interface POIListener {
    void latLngToDelete(POI poi);

    void poiClickToSet(POI poi);
}
