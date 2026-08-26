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

    // 🚨 新增：记录最后一次点击的时间戳，用于全局防抖拦截
    private long lastClickTime = 0;

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
        // 【防闪退】空串 substring(0,1) 会抛 StringIndexOutOfBoundsException
        String displayWord = (item.targetWord == null || item.targetWord.isEmpty())
                ? ""
                : item.targetWord.substring(0, 1).toUpperCase() + item.targetWord.substring(1);
        holder.tvObjectWord.setText(displayWord);

        // 2. 根据解锁状态控制 UI
        if (item.isUnlocked) {
            // ================== 已解锁状态 ==================
            holder.layoutLocked.setVisibility(View.GONE);
            holder.layoutTopBar.setVisibility(View.VISIBLE);
            holder.viewScrim.setVisibility(View.VISIBLE);

            holder.tvObjectScore.setText(item.highestScore + "%");

            // 🚨 核心修复：在这里加入防抖判定
            holder.itemView.setOnClickListener(v -> {
                long currentTime = System.currentTimeMillis();
                // 如果距离上一次点击不到 500 毫秒（半秒），直接无视这次点击！
                if (currentTime - lastClickTime < 500) {
                    return;
                }
                lastClickTime = currentTime;

                ShowcaseDetailDialog dialog = ShowcaseDetailDialog.Companion.newInstance(
                        item.targetWord,
                        item.bestImagePath,
                        item.highestScore
                );

                try {
                    androidx.appcompat.app.AppCompatActivity activity =
                            (androidx.appcompat.app.AppCompatActivity) v.getContext();
                    dialog.show(activity.getSupportFragmentManager(), "ShowcaseDetail");
                } catch (ClassCastException e) {
                    android.util.Log.e("VISION_DEBUG", "无法获取 Activity 上下文来显示弹窗");
                }
            });

            // 🚨 工业级修复：引入 Glide 全异步加载、自动降维缓存、绝对保留透明通道！
            if (item.bestImagePath != null && !item.bestImagePath.isEmpty()) {
                com.bumptech.glide.Glide.with(holder.itemView.getContext())
                        .load(item.bestImagePath) // Glide 智能处理所有的本地路径、Uri 和网络链接
                        .override(400, 400)       // 强制压缩为小尺寸，彻底告别 OOM 闪退
                        .centerCrop()
                        .into(holder.ivObjectPhoto);
            } else {
                com.bumptech.glide.Glide.with(holder.itemView.getContext()).clear(holder.ivObjectPhoto);
                holder.ivObjectPhoto.setImageDrawable(null);
            }

            // 动态调整卡片高度
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.height = (int) (260 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.itemView.setLayoutParams(params);

            // 🌟 核心状态逻辑：蒙尘复习 vs 完美掌握
            long threeDaysInMillis = 3L * 24 * 60 * 60 * 1000;

            // 防 0 判定
            long validReviewTime = item.lastReviewedTime <= 0 ? System.currentTimeMillis() : item.lastReviewedTime;
            boolean needsReview = (System.currentTimeMillis() - validReviewTime) > threeDaysInMillis;

            // 先清除滤镜
            holder.ivObjectPhoto.clearColorFilter();

            if (needsReview) {
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("⚠️ Needs Review");
                holder.tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"));

                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                holder.ivObjectPhoto.setColorFilter(new ColorMatrixColorFilter(matrix));

            } else if (item.highestScore >= 95) {
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("✨ Perfect Master");
                holder.tvStatusBadge.setTextColor(Color.parseColor("#06B6D4"));
            } else {
                holder.tvStatusBadge.setVisibility(View.GONE);
            }

        } else {
            // ================== 未解锁状态 ==================
            holder.layoutLocked.setVisibility(View.VISIBLE);
            holder.layoutTopBar.setVisibility(View.INVISIBLE);
            holder.viewScrim.setVisibility(View.GONE);

            // 🚨 必须在此清理 Glide 缓存，防止快速滑动时“未解锁”卡片显示了别的图片
            com.bumptech.glide.Glide.with(holder.itemView.getContext()).clear(holder.ivObjectPhoto);
            holder.ivObjectPhoto.setImageURI(null);
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
    // 🌟 专为 RecyclerView 列表滑动优化的图像降维解码器
   /* private android.graphics.Bitmap decodeSafeBitmap(String path, int reqWidth, int reqHeight) {
        final android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(path, options);
        options.inSampleSize = 1;
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            final int halfH = options.outHeight / 2;
            final int halfW = options.outWidth / 2;
            while ((halfH / options.inSampleSize) >= reqHeight && (halfW / options.inSampleSize) >= reqWidth) {
                options.inSampleSize *= 2;
            }
        }
        options.inJustDecodeBounds = false;
        return android.graphics.BitmapFactory.decodeFile(path, options);
    }*/
}