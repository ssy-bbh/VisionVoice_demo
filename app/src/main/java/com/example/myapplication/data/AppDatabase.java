package com.example.myapplication.data;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {PracticeRecord.class, ShowcaseItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppDao appDao();

    private static volatile AppDatabase INSTANCE;

    // 供数据库异步操作使用的全局线程池
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "visionvoice_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // ==================== 图鉴动态播种 ====================
    // 图鉴不再硬编码 80 条："模型能识别什么词表，图鉴就有什么条目"。
    // 以后换更大的模型（比如 LVIS 1203 类），只需替换 assets 里的
    // 模型文件和 labels.txt，下次启动 App 就会自动把新词补进图鉴，
    // 已解锁条目和成绩完全不受影响。

    // 保证每次进程生命周期内只同步一次（MainActivity 每次 onCreate 都会调用）
    private static volatile boolean showcaseSyncScheduled = false;

    /**
     * 幂等同步：把 labels.txt 里有、但图鉴里还没有的单词插入数据库。
     * 首次安装 = 全量建库；换模型升级 = 只补差集。
     */
    public static void syncShowcaseFromLabels(Context context) {
        if (showcaseSyncScheduled) return;
        showcaseSyncScheduled = true;

        // 【防闪退】后台线程只用 ApplicationContext
        final Context appContext = context.getApplicationContext();
        databaseWriteExecutor.execute(() -> {
            try {
                AppDao dao = getInstance(appContext).appDao();

                // 1. 读当前模型的词表
                List<String> labels = readAssetLines(appContext, "labels.txt");
                if (labels.isEmpty()) return;

                // 2. 读分类映射表（可选文件，缺了就全归 "Other"）
                Map<String, String> categoryMap = readCategoryMap(appContext);

                // 3. 差集比对：只插入数据库里还没有的单词
                Set<String> known = new HashSet<>();
                List<String> existing = dao.getAllShowcaseWords();
                if (existing != null) {
                    for (String w : existing) {
                        if (w != null) known.add(w.toLowerCase(Locale.ROOT));
                    }
                }

                List<ShowcaseItem> toInsert = new ArrayList<>();
                for (String raw : labels) {
                    String word = raw.trim();
                    if (word.isEmpty()) continue;
                    String key = word.toLowerCase(Locale.ROOT);
                    if (known.contains(key)) continue;
                    known.add(key); // 防止 labels.txt 自身有重复行

                    String category = categoryMap.get(key);
                    toInsert.add(new ShowcaseItem(word, category != null ? category : "Other", false));
                }

                if (!toInsert.isEmpty()) {
                    dao.insertShowcaseItems(toInsert);
                    Log.d("VISION_DEBUG", "🌟 图鉴动态补库完成：新增 " + toInsert.size() + " 个当前模型的单词");
                }
            } catch (Throwable t) {
                // 播种失败不影响主流程，最多图鉴暂时为空
                Log.e("VISION_DEBUG", "图鉴播种失败", t);
            }
        });
    }

    /** 按行读取 assets 文本文件，读不到返回空列表 */
    private static List<String> readAssetLines(Context context, String assetPath) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(assetPath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            Log.e("VISION_DEBUG", "读取 assets/" + assetPath + " 失败", e);
        }
        return lines;
    }

    /** 解析 showcase_categories.txt：每行 "单词|分类"，# 开头为注释 */
    private static Map<String, String> readCategoryMap(Context context) {
        Map<String, String> map = new HashMap<>();
        for (String line : readAssetLines(context, "showcase_categories.txt")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int sep = trimmed.indexOf('|');
            if (sep <= 0 || sep >= trimmed.length() - 1) continue;
            String word = trimmed.substring(0, sep).trim().toLowerCase(Locale.ROOT);
            String category = trimmed.substring(sep + 1).trim();
            if (!word.isEmpty() && !category.isEmpty()) {
                map.put(word, category);
            }
        }
        return map;
    }
}
