package com.lodz.android.mmsplayer.ijk.setting;


import androidx.annotation.IntDef;
import com.lodz.android.mmsplayer.ijk.media.IRenderView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class IjkPlayerSetting {

    public static IjkPlayerSetting getDefault(){
        IjkPlayerSetting setting = new IjkPlayerSetting();
        setting.playerType = PlayerType.PLAY_IJK;
        setting.isBackgroundPlay = false;
        setting.isUsingMediaCodec = true;
        setting.isUsingMediaCodecAutoRotate = true;
        setting.isMediaCodecHandleResolutionChange = true;
        setting.isUsingOpenSLES = true;
        setting.pixelFormatType = PixelFormatType.PIXEL_AUTO;
        setting.isUsingMediaDataSource = true;
        setting.renderViewType = RenderViewType.TEXTURE_VIEW;
        setting.isEnableDetachedSurfaceTexture = true;
        setting.aspectRatioType = IRenderView.AR_ASPECT_FIT_PARENT;
        return setting;
    }

    //--------------------------- 播放器设置 -----------------------------------
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PlayerType.PALY_ANDROID_MEDIA, PlayerType.PLAY_IJK})
    public @interface PlayerType {
        /** android原生播放器 */
        int PALY_ANDROID_MEDIA = 1;
        /** Ijk播放器 */
        int PLAY_IJK = 2;
    }

    /** 播放器类型 */
    @PlayerType
    public int playerType = PlayerType.PLAY_IJK;

//--------------------------- 后台播放设置 -----------------------------------
    /** 是否后台播放（4.0+） */
    public boolean isBackgroundPlay = false;

//--------------------------- 硬解码设置 -----------------------------------
    /** 是否使用硬解码 */
    public boolean isUsingMediaCodec = true;
    /** 是否使用硬解码下自动旋转 */
    public boolean isUsingMediaCodecAutoRotate = true;
    /** 是否使用硬解码下处理分辨率更改 */
    public boolean isMediaCodecHandleResolutionChange = true;

//--------------------------- 使用OpenGLES -----------------------------------
    /** 是否使用OpenGLES */
    public boolean isUsingOpenSLES = true;

    //--------------------------- 设置像素格式 -----------------------------------
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PixelFormatType.PIXEL_AUTO, PixelFormatType.PIXEL_RGB_565, PixelFormatType.PIXEL_RGB_888, PixelFormatType.PIXEL_RGBX_8888,
            PixelFormatType.PIXEL_YV12, PixelFormatType.PIXEL_OPENGL_ES2})
    public @interface PixelFormatType {
        /** 自动选择 */
        int PIXEL_AUTO = 0;
        /** RGB 565 */
        int PIXEL_RGB_565 = 1;
        /** RGB 888 */
        int PIXEL_RGB_888 = 2;
        /** RGBX 8888 */
        int PIXEL_RGBX_8888 = 3;
        /** YV12 */
        int PIXEL_YV12 = 4;
        /** OpenGL ES2 */
        int PIXEL_OPENGL_ES2 = 5;
    }

    /** 像素格式 */
    @PixelFormatType
    public int pixelFormatType = PixelFormatType.PIXEL_AUTO;

//--------------------------- 使用数据源 -----------------------------------
    /** 是否使用数据源（需要6.0+且播放本地文件） */
    public boolean isUsingMediaDataSource = true;

    //--------------------------- 渲染的view -----------------------------------
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RenderViewType.NO_VIEW, RenderViewType.SURFACE_VIEW, RenderViewType.TEXTURE_VIEW})
    public @interface RenderViewType {
        /** 不使用渲染的view */
        int NO_VIEW = 0;
        /** 用surfaceview渲染 */
        int SURFACE_VIEW = 1;
        /** 用textureview渲染 */
        int TEXTURE_VIEW = 2;
    }

    /** 渲染的view的类型 */
    @RenderViewType
    public int renderViewType = RenderViewType.TEXTURE_VIEW;

    /** 是否使用SurfaceTexture处理视频图像 */
    public boolean isEnableDetachedSurfaceTexture = true;

    //--------------------------- 播放器长宽比类型 -----------------------------------
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({IRenderView.AR_ASPECT_FIT_PARENT, IRenderView.AR_ASPECT_FILL_PARENT, IRenderView.AR_ASPECT_WRAP_CONTENT,
            IRenderView.AR_16_9_FIT_PARENT, IRenderView.AR_4_3_FIT_PARENT})
    public @interface AspectRatioType {}
    @AspectRatioType
    public int aspectRatioType = IRenderView.AR_ASPECT_FIT_PARENT;

    @Override
    public String toString() {
        return "IjkPlayerSetting{" +
                "\n playerType=" + playerType +
                ", \n isBackgroundPlay=" + isBackgroundPlay +
                ", \n isUsingMediaCodec=" + isUsingMediaCodec +
                ", \n isUsingMediaCodecAutoRotate=" + isUsingMediaCodecAutoRotate +
                ", \n isMediaCodecHandleResolutionChange=" + isMediaCodecHandleResolutionChange +
                ", \n isUsingOpenSLES=" + isUsingOpenSLES +
                ", \n pixelFormatType=" + pixelFormatType +
                ", \n isUsingMediaDataSource=" + isUsingMediaDataSource +
                ", \n renderViewType=" + renderViewType +
                ", \n isEnableDetachedSurfaceTexture=" + isEnableDetachedSurfaceTexture +
                ", \n aspectRatioType=" + aspectRatioType +
                '}';
    }
}