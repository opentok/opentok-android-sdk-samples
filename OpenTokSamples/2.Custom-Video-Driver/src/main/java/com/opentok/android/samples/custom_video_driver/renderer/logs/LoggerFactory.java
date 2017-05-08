package com.opentok.android.samples.custom_video_driver.renderer.logs;


public class LoggerFactory {

    public static Logger createLogger(Class<?> clazz) {
        return new Logger(clazz.getSimpleName());
    }
}
