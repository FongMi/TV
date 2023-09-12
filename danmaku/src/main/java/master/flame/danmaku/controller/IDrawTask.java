/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package master.flame.danmaku.controller;

import master.flame.danmaku.danmaku.model.AbsDisplay;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.renderer.IRenderer.RenderingState;

public interface IDrawTask {

    int PLAY_STATE_PLAYING = 1;
    int PLAY_STATE_PAUSE = 2;

    void addDanmaku(BaseDanmaku item);

    void removeAllDanmakus(boolean isClearDanmakusOnScreen);

    void removeAllLiveDanmakus();

    void clearDanmakusOnScreen(long currMillis);

    IDanmakus getVisibleDanmakusOnTime(long time);

    RenderingState draw(AbsDisplay display);

    void reset();

    void seek(long mills);

    void start();

    void quit();

    void prepare();

    void onPlayStateChanged(int state);

    void requestClear();

    void requestClearRetainer();

    void requestSync(long fromTimeMills, long toTimeMills, long offsetMills);

    void setParser(BaseDanmakuParser parser);

    void invalidateDanmaku(BaseDanmaku item, boolean remeasure);

    void requestHide();

    void requestRender();

    interface TaskListener {

        void ready();

        void onDanmakuAdd(BaseDanmaku danmaku);

        void onDanmakuShown(BaseDanmaku danmaku);

        void onDanmakuConfigChanged();

        void onDanmakusDrawingFinished();
    }
}
