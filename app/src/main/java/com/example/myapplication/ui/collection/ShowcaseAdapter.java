package com.example.myapplication.ui.collection;

import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.ShowcaseItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShowcaseAdapter extends RecyclerView.Adapter<ShowcaseAdapter.ShowcaseViewHolder> {

    private List<ShowcaseItem> items = new ArrayList<>();

    // 更新数据源的方法
    public void updateData(List<ShowcaseItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ShowcaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collection, parent, false);
        return new ShowcaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowcaseViewHolder holder, int position) {
        ShowcaseItem item = items.get(position);

        // 1. 设置底部单词文本 (首字母大写)
        String displayWord = item.targetWord.substring(0, 1).toUpperCase() + item.targetWord.substring(1);
        holder.tvObjectWord.setText(displayWord);

        // 2. 根据解锁状态控制 UI
        if (item.isUnlocked) {
            // ================== 已解锁状态 ==================
            holder.layoutLocked.setVisibility(View.GONE);
            holder.layoutTopBar.setVisibility(View.VISIBLE);
            holder.viewScrim.setVisibility(View.VISIBLE);

            holder.tvObjectScore.setText(item.highestScore + "%");

            holder.itemView.setOnClickListener(v -> {
                // 实例化我们的极客弹窗
                ShowcaseDetailDialog dialog = ShowcaseDetailDialog.newInstance(
                        item.targetWord,
                        item.bestImagePath,
                        item.highestScore
                );

                // 显示弹窗 (由于在 Adapter 里，需要通过 Context 强转获取 FragmentManager)
                androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) v.getContext();
                dialog.show(activity.getSupportFragmentManager(), "ShowcaseDetail");
            });
            // 加载用户自己拍的真实照片
            // 加载照片：兼容 AR 实景的本地 File 和 相册的 Content URI
            if (item.bestImagePath != null && !item.bestImagePath.isEmpty()) {
                try {
                    if (item.bestImagePath.startsWith("content://") || item.bestImagePath.startsWith("file://")) {
                        // 1. 如果是从相册选的虚拟 URI，直接解析加载
                        holder.ivObjectPhoto.setImageURI(Uri.parse(item.bestImagePath));
                    } else {
                        // 2. 如果是 AR 扫出来存的本地绝对路径
                        File imgFile = new File(item.bestImagePath);
                        if (imgFile.exists()) {
                            holder.ivObjectPhoto.setImageURI(Uri.fromFile(imgFile));
                        }
                    }
                } catch (SecurityException e) {
                    android.util.Log.e("VISION_DEBUG", "相册图片权限失效: " + e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.height = (int) (260 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.itemView.setLayoutParams(params);
            // 🌟 核心状态逻辑：蒙尘复习 vs 完美掌握
            long threeDaysInMillis = 3L * 24 * 60 * 60 * 1000;
            boolean needsReview = (System.currentTimeMillis() - item.lastReviewedTime) > threeDaysInMillis;

            if (needsReview) {
                // 状态 A: 超过3天未复习，显示警告
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("⚠️ Needs Review");
                holder.tvStatusBadge.setTextColor(Color.parseColor("#F59E0B")); // 琥珀色

                // 给图片加个去色（灰度）滤镜，营造“蒙尘”感
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                holder.ivObjectPhoto.setColorFilter(new ColorMatrixColorFilter(matrix));

            } else if (item.highestScore >= 95) {
                // 状态 B: 刚刚测过并且大于等于95分，显示完美徽章
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("✨ Perfect Master");
                holder.tvStatusBadge.setTextColor(Color.parseColor("#06B6D4")); // 赛博青色
                holder.ivObjectPhoto.clearColorFilter(); // 清除滤镜，光亮如新

            } else {
                // 状态 C: 普通已解锁（不到95分）
                holder.tvStatusBadge.setVisibility(View.GONE);
                holder.ivObjectPhoto.clearColorFilter();
            }

        } else {
            // ================== 未解锁状态 ==================
            holder.layoutLocked.setVisibility(View.VISIBLE);
            holder.layoutTopBar.setVisibility(View.INVISIBLE); // 隐藏顶部面板
            holder.viewScrim.setVisibility(View.GONE);         // 隐藏文字遮罩

            holder.ivObjectPhoto.setImageURI(null); // 清空背景图
            holder.ivObjectPhoto.clearColorFilter();
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.height = (int) (180 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.itemView.setLayoutParams(params);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // 绑定最新的 XML 控件 ID
    static class ShowcaseViewHolder extends RecyclerView.ViewHolder {
        ImageView ivObjectPhoto;
        View viewScrim;
        ConstraintLayout layoutLocked;
        LinearLayout layoutTopBar;
        TextView tvStatusBadge;
        TextView tvObjectScore;
        TextView tvObjectWord;

        public ShowcaseViewHolder(@NonNull View itemView) {
            super(itemView);
            ivObjectPhoto = itemView.findViewById(R.id.ivObjectPhoto);
            viewScrim = itemView.findViewById(R.id.viewScrim);
            layoutLocked = itemView.findViewById(R.id.layoutLocked);
            layoutTopBar = itemView.findViewById(R.id.layoutTopBar);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvObjectScore = itemView.findViewById(R.id.tvObjectScore);
            tvObjectWord = itemView.findViewById(R.id.tvObjectWord);
        }
    }
}