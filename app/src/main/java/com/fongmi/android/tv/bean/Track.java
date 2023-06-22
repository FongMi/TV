package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.player.Players;

import java.util.List;

@Entity(indices = @Index(value = {"key", "player", "type"}, unique = true))
public class Track {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private int type;
    private int group;
    private int track;
    private int player;
    private String key;
    private String name;
    private boolean selected;
    private boolean adaptive;

    public Track(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public int getTrack() {
        return track;
    }

    public void setTrack(int track) {
        this.track = track;
    }

    public int getPlayer() {
        return player;
    }

    public void setPlayer(int player) {
        this.player = player;
    }

    public String getKey() {
        return key;
    }

    public void setKey(@NonNull String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isAdaptive() {
        return adaptive;
    }

    public void setAdaptive(boolean adaptive) {
        this.adaptive = adaptive;
    }

    public boolean isExo(int player) {
        return getPlayer() == player && player == Players.EXO;
    }

    public boolean isIjk(int player) {
        return getPlayer() == player && player != Players.EXO;
    }

    public Track toggle() {
        setSelected(!isSelected());
        return this;
    }

    public void save() {
        AppDatabase.get().getTrackDao().insert(this);
    }

    public static List<Track> find(String key) {
        return AppDatabase.get().getTrackDao().find(key);
    }
}
