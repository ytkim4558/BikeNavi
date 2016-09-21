/*
 * Copyright (c) 2016. UGIF. All Rights Reserved
 */

package com.nagnek.bikenavi.guide;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class GuideContent {

    /**
     * An array of sample (guide) items.
     */
    public static final List<GuideItem> ITEMS = new ArrayList<GuideItem>();
//
//    /**
//     * A map of sample (guide) items, by ID.
//     */
//    public static final Map<String, GuideItem> ITEM_MAP = new HashMap<String, GuideItem>();

//    private static final int COUNT = 25;
//
//    static {
//        // Add some sample items.
//        for (int i = 1; i <= COUNT; i++) {
//            addItem(createGuideItem(i));
//        }
//    }
//
//    private static void addItem(GuideItem item) {
//        ITEMS.add(item);
//        ITEM_MAP.put(item.id, item);
//    }
//
//    private static GuideItem createGuideItem(int position) {
//        return new GuideItem(String.valueOf(position), "Item " + position, makeDetails(position));
//    }

    private static String makeDetails(int position) {
        StringBuilder builder = new StringBuilder();
        builder.append("Details about Item: ").append(position);
        for (int i = 0; i < position; i++) {
            builder.append("\nMore details information here.");
        }
        return builder.toString();
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class GuideItem {
        public final String id;
        public final String content;

        public GuideItem(String id, String content) {
            this.id = id;
            this.content = content;
        }

        @Override
        public String toString() {
            return content;
        }
    }
}
