package com.hoko.blur.processor;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RSRuntimeException;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;

import com.hoko.blur.HokoBlur;
import com.hoko.blur.renderscript.ScriptC_BoxblurHorizontal;
import com.hoko.blur.renderscript.ScriptC_BoxblurVertical;
import com.hoko.blur.renderscript.ScriptC_Stackblur;
import com.hoko.blur.util.BlurUtil;
import com.hoko.blur.util.Preconditions;

/**
 * Created by yuxfzju on 16/9/7.
 */
class RenderScriptBlurProcessor extends BlurProcessor {

    private RenderScript mRenderScript;
    private ScriptIntrinsicBlur mGaussianBlurScirpt;
    private ScriptC_BoxblurHorizontal mBoxBlurScriptH;
    private ScriptC_BoxblurVertical mBoxBlurScriptV;
    private ScriptC_Stackblur mStackBlurScript;

    private Allocation mAllocationIn;
    private Allocation mAllocationOut;

    RenderScriptBlurProcessor(Builder builder) {
        super(builder);
        init(builder.mCtx);
    }

    private void init(Context context) {
        try {
            mRenderScript = RenderScript.create(context.getApplicationContext());
            mGaussianBlurScirpt = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
            mBoxBlurScriptH = new ScriptC_BoxblurHorizontal(mRenderScript);
            mBoxBlurScriptV = new ScriptC_BoxblurVertical(mRenderScript);
            mStackBlurScript = new ScriptC_Stackblur(mRenderScript);
        } catch (RSRuntimeException e) {
            e.printStackTrace();
        }

    }


    /**
     * RenderScript自带并行实现
     * @param scaledInBitmap
     * @param concurrent
     * @return
     */
    @Override
    protected Bitmap doInnerBlur(Bitmap scaledInBitmap,  boolean concurrent) {
        Preconditions.checkNotNull(scaledInBitmap, "scaledInBitmap == null");

        Bitmap scaledOutBitmap = Bitmap.createBitmap(scaledInBitmap.getWidth(), scaledInBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        mAllocationIn = Allocation.createFromBitmap(mRenderScript, scaledInBitmap);
        mAllocationOut = Allocation.createFromBitmap(mRenderScript, scaledOutBitmap);

        try {
            switch (mMode) {
                case HokoBlur.MODE_BOX:
                    doBoxBlur(scaledInBitmap);
                    break;
                case HokoBlur.MODE_STACK:
                    doStackBlur(scaledInBitmap);
                    break;
                case HokoBlur.MODE_GAUSSIAN:
                    doGaussianBlur(scaledInBitmap);
                    break;
            }

            mAllocationOut.copyTo(scaledInBitmap);
        } catch (Exception e) {
            e.printStackTrace();
//            scaledOutBitmap = scaledInBitmap;
        }

        return scaledInBitmap;
    }


    private void doBoxBlur(Bitmap input) {
        if (mBoxBlurScriptH == null || mBoxBlurScriptV == null) {
            mAllocationOut = mAllocationIn;
            return;
        }
        mBoxBlurScriptH.set_input(mAllocationIn);
        mBoxBlurScriptH.set_output(mAllocationOut);
        mBoxBlurScriptH.set_width(input.getWidth());
        mBoxBlurScriptH.set_height(input.getHeight());
        mBoxBlurScriptH.set_radius(mRadius);
        mBoxBlurScriptH.forEach_boxblur_h(mAllocationIn);

        mBoxBlurScriptV.set_input(mAllocationOut);
        mBoxBlurScriptV.set_output(mAllocationIn);
        mBoxBlurScriptV.set_width(input.getWidth());
        mBoxBlurScriptV.set_height(input.getHeight());
        mBoxBlurScriptV.set_radius(mRadius);
        mBoxBlurScriptV.forEach_boxblur_v(mAllocationOut);
        mAllocationOut = mAllocationIn;
    }

    private void doGaussianBlur(Bitmap input) {
        if (mGaussianBlurScirpt == null) {
            mAllocationOut = mAllocationIn;
            return;
        }
        // 模糊核半径太大，RenderScript失效，这里做发限制
        mRadius = BlurUtil.checkRadius(mRadius);
        mGaussianBlurScirpt.setRadius(mRadius);
//        mAllocationIn.copyFrom(input);
        mGaussianBlurScirpt.setInput(mAllocationIn);
        mGaussianBlurScirpt.forEach(mAllocationOut);
    }

    private void doStackBlur(Bitmap input) {
        if (mStackBlurScript == null) {
            mAllocationOut = mAllocationIn;
            return;
        }

        mStackBlurScript.set_input(mAllocationIn);
        mStackBlurScript.set_output(mAllocationOut);
        mStackBlurScript.set_width(input.getWidth());
        mStackBlurScript.set_height(input.getHeight());
        mStackBlurScript.set_radius(mRadius);
        mStackBlurScript.forEach_stackblur_v(mAllocationIn);

        mStackBlurScript.set_input(mAllocationOut);
        mStackBlurScript.set_output(mAllocationIn);
        mStackBlurScript.forEach_stackblur_h(mAllocationOut);
        mAllocationOut = mAllocationIn;
    }

    @Override
    public Builder newBuilder() {
        Builder builder = super.newBuilder();
        builder.context(mRenderScript.getApplicationContext());
        return builder;
    }
}
