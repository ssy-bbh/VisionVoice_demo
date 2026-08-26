package com.example.myapplication.ui.collection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

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

    private List<ShowcaseItem> allItems = new ArrayList<>();
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

        rvCollection.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new ShowcaseAdapter();
        rvCollection.setAdapter(adapter);

        tabLayoutCategories.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 【防闪退】getText() 理论上可能为 null，toString 前先判空
                CharSequence text = tab.getText();
                String category = text != null ? text.toString() : "";
                if ("All".equals(category)) {
                    adapter.updateData(allItems);
                } else {
                    // 🚨 防崩点 1：防止 Map 返回 null 传给 Adapter 导致崩溃
                    List<ShowcaseItem> filteredList = categoryMap.get(category);
                    adapter.updateData(filteredList != null ? filteredList : new ArrayList<>());
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
        // 【防闪退】在主线程先抓住 ApplicationContext。
        // 原来在后台线程里用 isAdded()+getContext() 仍有竞态：
        // 检查通过后、使用前用户切走 Tab，getContext() 就变 null 了。
        final android.content.Context appContext = getContext() != null
                ? getContext().getApplicationContext() : null;
        if (appContext == null) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ShowcaseItem> dbItems = AppDatabase.getInstance(appContext).appDao().getAllShowcaseItems();

            // 🚨 防崩点 3：必须使用 getActivity() 并判空，绝不能盲目使用 requireActivity() 强杀
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                allItems = dbItems != null ? dbItems : new ArrayList<>();
                categoryMap.clear();

                // 🚨 防崩点 4：空数据状态优雅拦截，直接阻断后续的空引用逻辑
                if (allItems.isEmpty()) {
                    tvCollectionStats.setText("Unlocked: 0 / 0 objects");
                    tabLayoutCategories.removeAllTabs();
                    tabLayoutCategories.addTab(tabLayoutCategories.newTab().setText("All"));
                    adapter.updateData(new ArrayList<>());
                    return; // 拦截成功，安全退出
                }

                int unlockedCount = 0;
                for (ShowcaseItem item : allItems) {
                    if (item.isUnlocked) unlockedCount++;

                    if (!categoryMap.containsKey(item.category)) {
                        categoryMap.put(item.category, new ArrayList<>());
                    }
                    categoryMap.get(item.category).add(item);
                }

                tvCollectionStats.setText("Unlocked: " + unlockedCount + " / " + allItems.size() + " objects");

                tabLayoutCategories.removeAllTabs();
                tabLayoutCategories.addTab(tabLayoutCategories.newTab().setText("All"));
                for (String categoryName : categoryMap.keySet()) {
                    tabLayoutCategories.addTab(tabLayoutCategories.newTab().setText(categoryName));
                }

                adapter.updateData(allItems);
            });
        });
    }
}