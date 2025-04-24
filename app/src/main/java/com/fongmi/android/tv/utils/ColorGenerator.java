package com.fongmi.android.tv.utils;

import com.github.bassaer.library.MDColor;

import java.util.ArrayList;
import java.util.List;

public class ColorGenerator {

    private static final List<Integer> PALETTE_400;
    private static final List<Integer> PALETTE_700;

    static {
        PALETTE_400 = new ArrayList<>();
        PALETTE_400.add(MDColor.RED_400);
        PALETTE_400.add(MDColor.PINK_400);
        PALETTE_400.add(MDColor.PURPLE_400);
        PALETTE_400.add(MDColor.DEEP_PURPLE_400);
        PALETTE_400.add(MDColor.INDIGO_400);
        PALETTE_400.add(MDColor.BLUE_400);
        PALETTE_400.add(MDColor.LIGHT_BLUE_400);
        PALETTE_400.add(MDColor.CYAN_400);
        PALETTE_400.add(MDColor.TEAL_400);
        PALETTE_400.add(MDColor.GREEN_400);
        PALETTE_400.add(MDColor.LIGHT_GREEN_400);
        PALETTE_400.add(MDColor.LIME_400);
        PALETTE_400.add(MDColor.YELLOW_400);
        PALETTE_400.add(MDColor.AMBER_400);
        PALETTE_400.add(MDColor.ORANGE_400);
        PALETTE_400.add(MDColor.DEEP_ORANGE_400);
        PALETTE_400.add(MDColor.BROWN_400);
        PALETTE_400.add(MDColor.GREY_400);
        PALETTE_400.add(MDColor.BLUE_GREY_400);

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

    public static int get400(String key) {
        return PALETTE_400.get((key.hashCode() & Integer.MAX_VALUE) % PALETTE_400.size());
    }

    public static int get700(String key) {
        return PALETTE_700.get((key.hashCode() & Integer.MAX_VALUE) % PALETTE_700.size());
    }
}
