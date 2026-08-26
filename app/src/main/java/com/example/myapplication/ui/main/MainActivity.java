package com.example.myapplication.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.ui.profile.ProfileFragment;
import com.example.myapplication.ui.collection.CollectionFragment;
import com.example.myapplication.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 默认加载 HomeFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();

        // 进 App 就开始后台加载模型
        com.example.myapplication.ml.ModelManager.preload(this);

        // 🌟 图鉴动态播种：以 assets/labels.txt（当前模型词表）为准建库/补库。
        // 首次安装 = 全量建库；以后换更大的模型只需替换模型文件和 labels.txt，
        // 下次启动自动把新单词补进图鉴，已解锁条目不受影响。
        com.example.myapplication.data.AppDatabase.syncShowcaseFromLabels(this);

        // 监听正常点击切换 Tab 的事件
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_dex) {
                selectedFragment = new CollectionFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // 🚨 核心修复：在这里加上重选拦截器！
        bottomNav.setOnItemReselectedListener(item -> {
            // 留空！什么代码都不写。
            // 这样系统就会吃掉这个点击事件，绝对不会去执行上面的 replace 逻辑。
        });
    }
}