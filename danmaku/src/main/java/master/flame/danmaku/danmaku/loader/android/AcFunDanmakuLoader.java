package master.flame.danmaku.danmaku.loader.android;

import android.net.Uri;

import java.io.InputStream;

import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.parser.android.JSONSource;

/**
 * Ac danmaku loader
 *
 * @author yrom
 */
public class AcFunDanmakuLoader implements ILoader {
    private static volatile AcFunDanmakuLoader instance;
    private JSONSource dataSource;
    private AcFunDanmakuLoader() {
    }

    public static ILoader instance() {
        if (instance == null) {
            synchronized (AcFunDanmakuLoader.class) {
                if (instance == null)
                    instance = new AcFunDanmakuLoader();
            }
        }
        return instance;
    }

    @Override
    public JSONSource getDataSource() {
        return dataSource;
    }

    @Override
    public void load(String uri) throws IllegalDataException {
        try {
            dataSource = new JSONSource(Uri.parse(uri));
        } catch (Exception e) {
            throw new IllegalDataException(e);
        }
    }

    @Override
    public void load(InputStream in) throws IllegalDataException {
        try {
            dataSource = new JSONSource(in);
        } catch (Exception e) {
            throw new IllegalDataException(e);
        }
    }


}
