package com.example.smartsnap

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application 入口
 * 初始化 Hilt DI 和 Timber 日志框架
 */
@HiltAndroidApp
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    /**
     * Release 日志树：仅记录 ERROR 和 WARN 级别
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                if (t != null) {
                    android.util.Log.println(priority, tag, "$message\n${android.util.Log.getStackTraceString(t)}")
                } else {
                    android.util.Log.println(priority, tag, message)
                }
            }
        }
    }
}