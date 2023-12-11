package com.fongmi.android.tv.player.danmu;

import android.graphics.Color;
import android.text.TextUtils;

import com.fongmi.android.tv.bean.Danmu;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;

import org.json.JSONArray;
import org.json.JSONException;

import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.Duration;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplay;
import master.flame.danmaku.danmaku.model.SpecialDanmaku;
import master.flame.danmaku.danmaku.model.android.DanmakuFactory;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

public class Parser extends BaseDanmakuParser {

    private final Danmu danmu;
    private BaseDanmaku item;
    private float scaleX;
    private float scaleY;
    private int index;

    public Parser(String path) {
        this.danmu = Danmu.fromXml(getContent(path));
    }

    private String getContent(String path) {
        if (path.startsWith("file")) return Path.read(path);
        if (path.startsWith("http")) return OkHttp.string(path);
        return path;
    }

    @Override
    protected Danmakus parse() {
        Danmakus result = new Danmakus(IDanmakus.ST_BY_TIME);
        for (Danmu.Data data : danmu.getData()) {
            String[] values = data.getParam().split(",");
            if (values.length < 4) continue;
            setParam(values);
            setText(data.getText());
            synchronized (result.obtainSynchronizer()) {
                result.addItem(item);
            }
        }
        return result;
    }

    @Override
    public BaseDanmakuParser setDisplay(IDisplay display) {
        super.setDisplay(display);
        scaleX = mDisplayWidth / DanmakuFactory.BILI_PLAYER_WIDTH;
        scaleY = mDisplayHeight / DanmakuFactory.BILI_PLAYER_HEIGHT;
        return this;
    }

    private void setParam(String[] values) {
        int type = Integer.parseInt(values[1]);
        long time = (long) (Float.parseFloat(values[0]) * 1000);
        float size = Float.parseFloat(values[2]) * (mDisplayDensity - 0.6f);
        int color = (int) ((0x00000000ff000000L | Long.parseLong(values[3])) & 0x00000000ffffffffL);
        item = mContext.mDanmakuFactory.createDanmaku(type, mContext);
        item.setTime(time);
        item.setTimer(mTimer);
        item.setTextSize(size);
        item.setTextColor(color);
        item.setTextShadowColor(color <= Color.BLACK ? Color.WHITE : Color.BLACK);
        item.setFlags(mContext.mGlobalFlagValues);
    }

    private void setText(String text) {
        item.index = index++;
        DanmakuUtils.fillText(item, decodeXmlString(text));
        if (item.getType() == BaseDanmaku.TYPE_SPECIAL && text.startsWith("[") && text.endsWith("]")) setSpecial();
    }

    private void setSpecial() {
        String[] textArr = null;
        try {
            JSONArray jsonArray = new JSONArray(item.getText());
            textArr = new String[jsonArray.length()];
            for (int i = 0; i < textArr.length; i++) {
                textArr[i] = jsonArray.getString(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (textArr == null || textArr.length < 5 || TextUtils.isEmpty(textArr[4])) {
            item = null;
            return;
        }
        DanmakuUtils.fillText(item, textArr[4]);
        float beginX = Float.parseFloat(textArr[0]);
        float beginY = Float.parseFloat(textArr[1]);
        float endX = beginX;
        float endY = beginY;
        String[] alphaArr = textArr[2].split("-");
        int beginAlpha = (int) (AlphaValue.MAX * Float.parseFloat(alphaArr[0]));
        int endAlpha = beginAlpha;
        if (alphaArr.length > 1) {
            endAlpha = (int) (AlphaValue.MAX * Float.parseFloat(alphaArr[1]));
        }
        long alphaDuraion = (long) (Float.parseFloat(textArr[3]) * 1000);
        long translationDuration = alphaDuraion;
        long translationStartDelay = 0;
        float rotateY = 0, rotateZ = 0;
        if (textArr.length >= 7) {
            rotateZ = Float.parseFloat(textArr[5]);
            rotateY = Float.parseFloat(textArr[6]);
        }
        if (textArr.length >= 11) {
            endX = Float.parseFloat(textArr[7]);
            endY = Float.parseFloat(textArr[8]);
            if (!"".equals(textArr[9])) {
                translationDuration = Integer.parseInt(textArr[9]);
            }
            if (!"".equals(textArr[10])) {
                translationStartDelay = (long) (Float.parseFloat(textArr[10]));
            }
        }
        if (isPercentageNumber(textArr[0])) {
            beginX *= DanmakuFactory.BILI_PLAYER_WIDTH;
        }
        if (isPercentageNumber(textArr[1])) {
            beginY *= DanmakuFactory.BILI_PLAYER_HEIGHT;
        }
        if (textArr.length >= 8 && isPercentageNumber(textArr[7])) {
            endX *= DanmakuFactory.BILI_PLAYER_WIDTH;
        }
        if (textArr.length >= 9 && isPercentageNumber(textArr[8])) {
            endY *= DanmakuFactory.BILI_PLAYER_HEIGHT;
        }
        item.duration = new Duration(alphaDuraion);
        item.rotationZ = rotateZ;
        item.rotationY = rotateY;
        mContext.mDanmakuFactory.fillTranslationData(item, beginX, beginY, endX, endY, translationDuration, translationStartDelay, scaleX, scaleY);
        mContext.mDanmakuFactory.fillAlphaData(item, beginAlpha, endAlpha, alphaDuraion);
        if (textArr.length >= 12) {
            if (!TextUtils.isEmpty(textArr[11]) && "true".equalsIgnoreCase(textArr[11])) {
                item.textShadowColor = Color.TRANSPARENT;
            }
        }
        if (textArr.length >= 14) {
            ((SpecialDanmaku) item).isQuadraticEaseOut = ("0".equals(textArr[13]));
        }
        if (textArr.length >= 15) {
            if (!"".equals(textArr[14])) {
                String motionPathString = textArr[14].substring(1);
                if (!TextUtils.isEmpty(motionPathString)) {
                    String[] pointStrArray = motionPathString.split("L");
                    if (pointStrArray.length > 0) {
                        float[][] points = new float[pointStrArray.length][2];
                        for (int i = 0; i < pointStrArray.length; i++) {
                            String[] pointArray = pointStrArray[i].split(",");
                            if (pointArray.length >= 2) {
                                points[i][0] = Float.parseFloat(pointArray[0]);
                                points[i][1] = Float.parseFloat(pointArray[1]);
                            }
                        }
                        DanmakuFactory.fillLinePathData(item, points, scaleX, scaleY);
                    }
                }
            }
        }
    }

    private boolean isPercentageNumber(String number) {
        return number != null && number.contains(".");
    }

    private String decodeXmlString(String title) {
        if (title.contains("&amp;")) title = title.replace("&amp;", "&");
        if (title.contains("&quot;")) title = title.replace("&quot;", "\"");
        if (title.contains("&gt;")) title = title.replace("&gt;", ">");
        if (title.contains("&lt;")) title = title.replace("&lt;", "<");
        return title;
    }
}
