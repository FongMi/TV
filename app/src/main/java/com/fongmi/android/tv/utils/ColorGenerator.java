package com.fongmi.android.tv.utils;

import com.github.bassaer.library.MDColor;

import java.util.ArrayList;
import java.util.List;

public class ColorGenerator {

    private static final List<Integer> PALETTE_500;
    private static final List<Integer> PALETTE_700;

    static {
        PALETTE_500 = new ArrayList<>();
        PALETTE_500.add(MDColor.RED_500);
        PALETTE_500.add(MDColor.PINK_500);
        PALETTE_500.add(MDColor.PURPLE_500);
        PALETTE_500.add(MDColor.DEEP_PURPLE_500);
        PALETTE_500.add(MDColor.INDIGO_500);
        PALETTE_500.add(MDColor.BLUE_500);
        PALETTE_500.add(MDColor.LIGHT_BLUE_500);
        PALETTE_500.add(MDColor.CYAN_500);
        PALETTE_500.add(MDColor.TEAL_500);
        PALETTE_500.add(MDColor.GREEN_500);
        PALETTE_500.add(MDColor.LIGHT_GREEN_500);
        PALETTE_500.add(MDColor.LIME_500);
        PALETTE_500.add(MDColor.YELLOW_500);
        PALETTE_500.add(MDColor.AMBER_500);
        PALETTE_500.add(MDColor.ORANGE_500);
        PALETTE_500.add(MDColor.DEEP_ORANGE_500);
        PALETTE_500.add(MDColor.BROWN_500);
        PALETTE_500.add(MDColor.GREY_500);
        PALETTE_500.add(MDColor.BLUE_GREY_500);

        PALETTE_700 = new ArrayList<>();
        PALETTE_700.add(MDColor.RED_700);
        PALETTE_700.add(MDColor.PINK_700);
        PALETTE_700.add(MDColor.PURPLE_700);
        PALETTE_700.add(MDColor.DEEP_PURPLE_700);
        PALETTE_700.add(MDColor.INDIGO_700);
        PALETTE_700.add(MDColor.BLUE_700);
        PALETTE_700.add(MDColor.LIGHT_BLUE_700);
        PALETTE_700.add(MDColor.CYAN_700);
        PALETTE_700.add(MDColor.TEAL_700);
        PALETTE_700.add(MDColor.GREEN_700);
        PALETTE_700.add(MDColor.LIGHT_GREEN_700);
        PALETTE_700.add(MDColor.LIME_700);
        PALETTE_700.add(MDColor.YELLOW_700);
        PALETTE_700.add(MDColor.AMBER_700);
        PALETTE_700.add(MDColor.ORANGE_700);
        PALETTE_700.add(MDColor.DEEP_ORANGE_700);
        PALETTE_700.add(MDColor.BROWN_700);
        PALETTE_700.add(MDColor.GREY_700);
        PALETTE_700.add(MDColor.BLUE_GREY_700);
    }

    public static int get500(String key) {
        return PALETTE_500.get(Math.abs(key.hashCode()) % PALETTE_500.size());
    }

    public static int get700(String key) {
        return PALETTE_700.get(Math.abs(key.hashCode()) % PALETTE_700.size());
    }
}
