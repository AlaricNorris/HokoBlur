package com.hoko.blur.processor;

import com.hoko.blur.HokoBlur;
import com.hoko.blur.anno.Scheme;

import java.lang.reflect.Constructor;

class BlurProcessorFactory {

    static BlurProcessor get(@Scheme int scheme, BlurProcessor.Builder builder) {

        BlurProcessor generator = null;

        switch (scheme) {
            case HokoBlur.SCHEME_RENDER_SCRIPT:
                Class<?> rsProcessorClazz = null;
                try {
                    rsProcessorClazz = Class.forName("com.hoko.blur.processor.RenderScriptBlurProcessor");
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unsupported RenderScript scheme, please add com.hoko:hoko-blur-rs dependency");
                }

                try {
                    Constructor<?> constructor = rsProcessorClazz.getDeclaredConstructor(BlurProcessor.Builder.class);
                    constructor.setAccessible(true);
                    generator = (BlurProcessor) constructor.newInstance(builder);
                } catch (Exception e) {
                    e.printStackTrace();
                    generator = null;
                }
                break;
            case HokoBlur.SCHEME_OPENGL:
                generator = new OpenGLBlurProcessor(builder);
                break;
            case HokoBlur.SCHEME_NATIVE:
                generator = new NativeBlurProcessor(builder);
                break;
            case HokoBlur.SCHEME_JAVA:
                generator = new OriginBlurProcessor(builder);
                break;
            default:
                throw new IllegalArgumentException("Unsupported blur scheme!");
        }

        return generator;
    }
}
