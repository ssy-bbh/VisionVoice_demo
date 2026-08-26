package com.example.myapplication.ml;

import android.content.Context;
import android.util.Log;

public class ModelManager {
    private static volatile Wav2Vec2Scorer globalScorer = null;
    private static volatile boolean isReady = false;
    // 【防闪退】防止 preload 被多个入口并发调用时，两条线程各拷一份 300MB 模型
    private static volatile boolean isLoading = false;

    // 在主页调用它，后台静默加载
    public static synchronized void preload(Context context) {
        if (globalScorer == null && !isLoading) {
            isLoading = true;
            final Context appContext = context.getApplicationContext();
            new Thread(() -> {
                try {
                    Log.i("ModelManager", "🚀 全局预加载 Wav2Vec2 模型开始...");
                    // 🚨 必须用 ApplicationContext，防止内存泄漏！
                    globalScorer = new Wav2Vec2Scorer(appContext);
                    isReady = true;
                    Log.i("ModelManager", "✅ 模型全局就绪，后续可秒开！");
                } catch (Throwable e) {
                    // 300MB 模型拷贝/加载可能因磁盘、内存失败，这里必须兜住，
                    // 否则后台线程未捕获异常会直接闪退整个 App
                    isReady = false;
                    Log.e("ModelManager", "模型加载失败", e);
                } finally {
                    isLoading = false;
                }
            }).start();
        }
    }

    public static Wav2Vec2Scorer getScorer() {
        return globalScorer;
    }

    public static boolean isReady() {
        return isReady;
    }
}
