package com.opentok.android.samples.custom_video_driver.renderer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.View;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.samples.custom_video_driver.renderer.customtextureview.OTTextureRenderer;
import com.opentok.android.samples.custom_video_driver.renderer.customtextureview.OTTextureView;
import com.opentok.android.samples.custom_video_driver.renderer.logs.Logger;
import com.opentok.android.samples.custom_video_driver.renderer.logs.LoggerFactory;


/**
 * Class OTVideoRenderer created by
 * -Vikas Goyal(https://github.com/avirepo) on 20/12/16 - 8:02 PM.
 */
@SuppressWarnings("unused")
public class OTVideoRenderer extends BaseVideoRenderer {
    private final static Logger LOGGER = LoggerFactory.createLogger(OTVideoRenderer.class);

    private OTTextureView mGLTextureView;
    private OTTextureRenderer mRenderer;
    private boolean mSaveScreenshot;

    public OTVideoRenderer(Context context) {
        mGLTextureView = new OTTextureView(context);
        mGLTextureView.setEGLContextClientVersion(2);
        mGLTextureView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mRenderer = new OTTextureRenderer(this);
        mGLTextureView.setRenderer(mRenderer);
        mGLTextureView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onFrame(Frame frame) {
        mRenderer.displayFrame(frame);
        mGLTextureView.requestRender();
    }

    @Override
    public void setStyle(String key, String value) {
        if (BaseVideoRenderer.STYLE_VIDEO_SCALE.equals(key)) {
            if (BaseVideoRenderer.STYLE_VIDEO_FIT.equals(value)) {
                mRenderer.enableVideoFit(true);
            } else if (BaseVideoRenderer.STYLE_VIDEO_FILL.equals(value)) {
                mRenderer.enableVideoFit(false);
            }
        }
    }

    @Override
    public void onVideoPropertiesChanged(boolean videoEnabled) {
        mRenderer.disableVideo(!videoEnabled);
    }

    @Override
    public View getView() {
        return mGLTextureView;
    }

    @Override
    public void onPause() {
        mGLTextureView.onPause();
    }

    @Override
    public void onResume() {
        mGLTextureView.onResume();
    }

    public void saveScreenshot(Boolean enableScreenshot) {
        mSaveScreenshot = enableScreenshot;
    }

    public boolean isSaveScreenshot() {
        return mSaveScreenshot;
    }
}

