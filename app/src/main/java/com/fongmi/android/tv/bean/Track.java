package com.fongmi.android.tv.bean;

public class Track {

    private int index;
    private String name;
    private boolean selected;

    public Track(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
