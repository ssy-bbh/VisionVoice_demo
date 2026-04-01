package com.example.myapplication.ui.collection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.AppDatabase;
import com.example.myapplication.data.ShowcaseItem;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CollectionFragment extends Fragment {

    private RecyclerView rvCollection;
    private TabLayout tabLayoutCategories;
    private TextView tvCollectionStats;
    private ShowcaseAdapter adapter;

    // 用来在内存中保存所有数据库里的展品，方便随时过滤
    private List<ShowcaseItem> allItems = new ArrayList<>();
    // 用来将展品按场景分类存放
    private Map<String, List<ShowcaseItem>> categoryMap = new LinkedHashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_collection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCollection = view.findViewById(R.id.rvCollection);
        tabLayoutCategories = view.findViewById(R.id.tabLayoutCategories);
        tvCollectionStats = view.findViewById(R.id.tvCollectionStats);

        rvCollection.setLayoutManager(new androidx.recyclerview.widget.StaggeredGridLayoutManager(2, androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL));
        adapter = new ShowcaseAdapter();
        rvCollection.setAdapter(adapter);

        // 监听 Tab 点击事件，切换不同分类的展品
        tabLayoutCategories.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String category = tab.getText().toString();
                if ("All".equals(category)) {
                    adapter.updateData(allItems);
                } else {
                    adapter.updateData(categoryMap.get(category));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadDataFromDatabase();
    }

    private void loadDataFromDatabase() {
        // 在后台线程查询数据库
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ShowcaseItem> dbItems = AppDatabase.getInstance(getContext()).appDao().getAllShowcaseItems();

            // 回到主线程更新 UI
            requireActivity().runOnUiThread(() -> {
                allItems = dbItems;
                categoryMap.clear();

                int unlockedCount = 0;

                // 整理数据，按 category 分组
                for (ShowcaseItem item : dbItems) {
                    if (item.isUnlocked) unlockedCount++;

                    if (!categoryMap.containsKey(item.category)) {
                        categoryMap.put(item.category, new ArrayList<>());
                    }
                    categoryMap.get(item.category).add(item);
                }

                // 更新统计文本
                tvCollectionStats.setText("Unlocked: " + unlockedCount + " / " + allItems.size() + " objects");

                // 动态生成 Tab 标签
                tabLayoutCategories.removeAllTabs();
                tabLayoutCategories.addTab(tabLayoutCategories.newTab().setText("All")); // 默认添加"全部"标签
                for (String categoryName : categoryMap.keySet()) {
                    tabLayoutCategories.addTab(tabLayoutCategories.newTab().setText(categoryName));
                }

                // 默认展示全部数据
                adapter.updateData(allItems);
            });
        });
    }
}