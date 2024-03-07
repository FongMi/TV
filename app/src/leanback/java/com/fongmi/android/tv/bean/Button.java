package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Button {

    public int id;
    public int resId;

    private static List<Button> buttons;


    public static List<Button> all() {
        if (buttons != null) return buttons;
        buttons = new ArrayList<>();
        buttons.add(new Button(0, R.string.home_vod));
        buttons.add(new Button(1, R.string.home_live));
        buttons.add(new Button(2, R.string.home_search));
        buttons.add(new Button(3, R.string.home_keep));
        buttons.add(new Button(4, R.string.home_push));
        buttons.add(new Button(5, R.string.home_history_short));
        buttons.add(new Button(6, R.string.home_setting));
        return buttons;
    }

    public static List<Button> sortedAll() {
        String buttons = Setting.getHomeButtonsSorted("0,1,2,3,4,5,6");
        if (TextUtils.isEmpty(buttons)) return all();
        String[] buttonsArr = buttons.split(",");
        List<Button> buttonList = new ArrayList<>();
        Map<Integer, Button> allButtons = getMap(all());
        for(int i=0; i<buttonsArr.length; i++) {
            int id = Integer.parseInt(buttonsArr[i]);
            if (allButtons.containsKey(id)) buttonList.add(allButtons.get(id));
        }
        return buttonList;
    }

    public static Map<Integer, Button> getMap(List<Button> buttons) {
        Map<Integer, Button> map = new LinkedHashMap<>();
        if (buttons.size() == 0) return map;
        for(int i=0; i<buttons.size(); i++) {
            Button one = buttons.get(i);
            map.put(one.getId(), one);
        }
        return map;
    }

    public static String getDefaultButtons() {
        return Setting.getHomeUI() == 0 ? "0,1,2,3,4,5,6" : "1,2,3,4,5,6";
    }

    public static List<Button> getButtons() {
        String buttons = Setting.getHomeButtons(getDefaultButtons());
        if (TextUtils.isEmpty(buttons)) return new ArrayList<>();
        String[] buttonsArr = buttons.split(",");
        List<Button> buttonList = new ArrayList<>();
        Map<Integer, Button> allButtons = getMap(all());
        for(int i=0; i<buttonsArr.length; i++) {
            int id = Integer.parseInt(buttonsArr[i]);
            if (allButtons.containsKey(id)) buttonList.add(allButtons.get(id));
        }
        return buttonList;
    }

    public static Map<Integer, Button> getButtonsMap() {
        return getMap(getButtons());
    }

    public static void save(Map<Integer, Button> map) {
        List<Integer> ids = new ArrayList<>();
        if (map.size() > 0) {
            for(Integer id : map.keySet()) {
                ids.add(id);
            }
        }
        String buttonsStr = TextUtils.join(",", ids);
        Setting.putHomeButtons(buttonsStr);
    }

    public static void saveSorted(Map<Integer, Button> map) {
        List<Integer> ids = new ArrayList<>();
        if (map.size() > 0) {
            for(Integer id : map.keySet()) {
                ids.add(id);
            }
        }
        String buttonsStr = TextUtils.join(",", ids);
        Setting.putHomeButtonsSorted(buttonsStr);
    }

    public Button(int id, int resId) {
        this.id = id;
        this.resId = resId;
    }

    public String getName() {
        return ResUtil.getString(resId);
    }

    public int getResId() {
        return resId;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Button)) return false;
        Button it = (Button) obj;
        return getId() == it.getId();
    }

}
