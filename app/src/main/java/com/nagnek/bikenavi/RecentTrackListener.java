/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi;

/**
 * Created by user on 2016-10-27.
 */
public interface RecentTrackListener {
    void trackClickToDelete(Track track);

    void trackClickToSet(Track track, int fromPosition);    // fromPosition : 원래 위치
}
