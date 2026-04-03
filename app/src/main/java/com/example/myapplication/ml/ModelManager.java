package com.example.myapplication.ml;

import android.content.Context;
import android.util.Log;

public class ModelManager {
    private static Wav2Vec2Scorer globalScorer = null;
    private static boolean isReady = false;

    // 在主页调用它，后台静默加载
    public static void preload(Context context) {
        if (globalScorer == null) {
            new Thread(() -> {
                try {
                    Log.i("ModelManager", "🚀 全局预加载 Wav2Vec2 模型开始...");
                    // 🚨 必须用 ApplicationContext，防止内存泄漏！
                    globalScorer = new Wav2Vec2Scorer(context.getApplicationContext());
                    isReady = true;
                    Log.i("ModelManager", "✅ 模型全局就绪，后续可秒开！");
                } catch (Exception e) {
                    Log.e("ModelManager", "模型加载失败", e);
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