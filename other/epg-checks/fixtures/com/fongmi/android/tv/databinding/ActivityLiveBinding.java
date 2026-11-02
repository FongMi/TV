package com.fongmi.android.tv.databinding;
import android.view.View;import android.widget.*;
public class ActivityLiveBinding {
 public FrameLayout root;public LinearLayout recycler;public Control control=new Control();public Widget widget=new Widget();
 public FrameLayout getRoot(){return root;}
 public static class Control{public FrameLayout root;public LinearLayout bottom;public View center,top;public Right right=new Right();public TextView subtitle;public FrameLayout getRoot(){return root;}}
 public static class Right{public View root;public View getRoot(){return root;}}
 public static class Widget{public LinearLayout info;public View infoPip;public TextView play;public ProgressBar programmeProgress;}
}
