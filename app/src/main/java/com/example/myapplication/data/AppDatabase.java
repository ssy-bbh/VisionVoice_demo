package com.example.myapplication.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
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
                            // 监听数据库的创建，用于首次注入 80 个官方图鉴
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // 定义回调逻辑：首次建库时注入 YOLOv8 (COCO 80) 全套官方图鉴
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                AppDao dao = INSTANCE.appDao();
                List<ShowcaseItem> initialItems = new ArrayList<>();

                // 🚗 交通与街道 (Traffic & Street)
                String catTraffic = "Traffic & Street";
                initialItems.add(new ShowcaseItem("bicycle", catTraffic, false));
                initialItems.add(new ShowcaseItem("car", catTraffic, false));
                initialItems.add(new ShowcaseItem("motorcycle", catTraffic, false));
                initialItems.add(new ShowcaseItem("airplane", catTraffic, false));
                initialItems.add(new ShowcaseItem("bus", catTraffic, false));
                initialItems.add(new ShowcaseItem("train", catTraffic, false));
                initialItems.add(new ShowcaseItem("truck", catTraffic, false));
                initialItems.add(new ShowcaseItem("boat", catTraffic, false));
                initialItems.add(new ShowcaseItem("traffic light", catTraffic, false));
                initialItems.add(new ShowcaseItem("fire hydrant", catTraffic, false));
                initialItems.add(new ShowcaseItem("stop sign", catTraffic, false));
                initialItems.add(new ShowcaseItem("parking meter", catTraffic, false));
                initialItems.add(new ShowcaseItem("bench", catTraffic, false));

                // 🦁 动物世界 (Animals)
                String catAnimals = "Animals";
                initialItems.add(new ShowcaseItem("bird", catAnimals, false));
                initialItems.add(new ShowcaseItem("cat", catAnimals, false));
                initialItems.add(new ShowcaseItem("dog", catAnimals, false));
                initialItems.add(new ShowcaseItem("horse", catAnimals, false));
                initialItems.add(new ShowcaseItem("sheep", catAnimals, false));
                initialItems.add(new ShowcaseItem("cow", catAnimals, false));
                initialItems.add(new ShowcaseItem("elephant", catAnimals, false));
                initialItems.add(new ShowcaseItem("bear", catAnimals, false));
                initialItems.add(new ShowcaseItem("zebra", catAnimals, false));
                initialItems.add(new ShowcaseItem("giraffe", catAnimals, false));

                // 🎒 穿搭与箱包 (Accessories)
                String catAccessories = "Accessories";
                initialItems.add(new ShowcaseItem("backpack", catAccessories, false));
                initialItems.add(new ShowcaseItem("umbrella", catAccessories, false));
                initialItems.add(new ShowcaseItem("handbag", catAccessories, false));
                initialItems.add(new ShowcaseItem("tie", catAccessories, false));
                initialItems.add(new ShowcaseItem("suitcase", catAccessories, false));

                // ⚽ 运动与户外 (Sports & Outdoors)
                String catSports = "Sports & Outdoors";
                initialItems.add(new ShowcaseItem("frisbee", catSports, false));
                initialItems.add(new ShowcaseItem("skis", catSports, false));
                initialItems.add(new ShowcaseItem("snowboard", catSports, false));
                initialItems.add(new ShowcaseItem("sports ball", catSports, false));
                initialItems.add(new ShowcaseItem("kite", catSports, false));
                initialItems.add(new ShowcaseItem("baseball bat", catSports, false));
                initialItems.add(new ShowcaseItem("baseball glove", catSports, false));
                initialItems.add(new ShowcaseItem("skateboard", catSports, false));
                initialItems.add(new ShowcaseItem("surfboard", catSports, false));
                initialItems.add(new ShowcaseItem("tennis racket", catSports, false));

                // 🍽️ 厨房与餐具 (Kitchen & Dining)
                String catKitchen = "Kitchen & Dining";
                initialItems.add(new ShowcaseItem("bottle", catKitchen, false));
                initialItems.add(new ShowcaseItem("wine glass", catKitchen, false));
                initialItems.add(new ShowcaseItem("cup", catKitchen, false));
                initialItems.add(new ShowcaseItem("fork", catKitchen, false));
                initialItems.add(new ShowcaseItem("knife", catKitchen, false));
                initialItems.add(new ShowcaseItem("spoon", catKitchen, false));
                initialItems.add(new ShowcaseItem("bowl", catKitchen, false));
                initialItems.add(new ShowcaseItem("microwave", catKitchen, false));
                initialItems.add(new ShowcaseItem("oven", catKitchen, false));
                initialItems.add(new ShowcaseItem("toaster", catKitchen, false));
                initialItems.add(new ShowcaseItem("refrigerator", catKitchen, false));
                initialItems.add(new ShowcaseItem("sink", catKitchen, false));

                // 🍔 食物 (Food)
                String catFood = "Food";
                initialItems.add(new ShowcaseItem("banana", catFood, false));
                initialItems.add(new ShowcaseItem("apple", catFood, false));
                initialItems.add(new ShowcaseItem("sandwich", catFood, false));
                initialItems.add(new ShowcaseItem("orange", catFood, false));
                initialItems.add(new ShowcaseItem("broccoli", catFood, false));
                initialItems.add(new ShowcaseItem("carrot", catFood, false));
                initialItems.add(new ShowcaseItem("hot dog", catFood, false));
                initialItems.add(new ShowcaseItem("pizza", catFood, false));
                initialItems.add(new ShowcaseItem("donut", catFood, false));
                initialItems.add(new ShowcaseItem("cake", catFood, false));

                // 🏠 家具与电子设备 (Home & Electronics)
                String catHome = "Home & Electronics";
                initialItems.add(new ShowcaseItem("chair", catHome, false));
                initialItems.add(new ShowcaseItem("couch", catHome, false));
                initialItems.add(new ShowcaseItem("potted plant", catHome, false));
                initialItems.add(new ShowcaseItem("bed", catHome, false));
                initialItems.add(new ShowcaseItem("dining table", catHome, false));
                initialItems.add(new ShowcaseItem("toilet", catHome, false));
                initialItems.add(new ShowcaseItem("tv", catHome, false));
                initialItems.add(new ShowcaseItem("laptop", catHome, false));
                initialItems.add(new ShowcaseItem("mouse", catHome, false));
                initialItems.add(new ShowcaseItem("remote", catHome, false));
                initialItems.add(new ShowcaseItem("keyboard", catHome, false));
                initialItems.add(new ShowcaseItem("cell phone", catHome, false));

                // 📖 日常杂物 (Everyday Items)
                String catEveryday = "Everyday Items";
                initialItems.add(new ShowcaseItem("book", catEveryday, false));
                initialItems.add(new ShowcaseItem("clock", catEveryday, false));
                initialItems.add(new ShowcaseItem("vase", catEveryday, false));
                initialItems.add(new ShowcaseItem("scissors", catEveryday, false));
                initialItems.add(new ShowcaseItem("teddy bear", catEveryday, false));
                initialItems.add(new ShowcaseItem("hair drier", catEveryday, false));
                initialItems.add(new ShowcaseItem("toothbrush", catEveryday, false));
                initialItems.add(new ShowcaseItem("person", catEveryday, false));

                // 批量插入数据库
                dao.insertShowcaseItems(initialItems);
                android.util.Log.d("VISION_DEBUG", "🌟 数据库初始化完毕，80个 COCO 类别已注入展柜！");
            });
        }
    };
}