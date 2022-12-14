package com.hiker.drpy.bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Info {

    public int index;
    public String rule;
    public List<String> excludes;

    public Info(String rule) {
        this.rule = rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public void setInfo(String pos) {
        if (rule.contains("--")) {
            String[] rules = rule.split("--");
            setExcludes(rules);
            setRule(rules[0]);
            setIndex(pos);
        } else if (pos.contains("--")) {
            String[] rules = pos.split("--");
            setExcludes(rules);
            setIndex(rules[0]);
        }
    }

    public void setExcludes(String[] rules) {
        excludes = new ArrayList<>(Arrays.asList(rules));
        excludes.remove(0);
    }

    public void setIndex(String pos) {
        try {
            index = Integer.parseInt(pos.split("\\(")[1].split("\\)")[0]);
        } catch (Exception ignored) {
        }
    }
}
