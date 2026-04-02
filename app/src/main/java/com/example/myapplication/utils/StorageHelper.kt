package com.example.myapplication.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.myapplication.data.AppDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object StorageHelper {

    /**
     * 终极保存与覆盖方案
     * @param context 上下文
     * @param targetWord 目标单词 (比如 "Apple")
     * @param bitmap 从 AR 截帧传来的图像 (相册模式传 null)
     * @param uri 从相册选择的图像 Uri (AR 模式传 null)
     * @param appDao 你的数据库 Dao
     */
    suspend fun saveAndOverwriteImage(
        context: Context,
        targetWord: String,
        bitmap: Bitmap? = null,
        uri: Uri? = null,
        appDao: AppDao
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 1. 去数据库里查出这个单词的“前任”数据
            val oldItem = appDao.getShowcaseItemByWord(targetWord)
            if (oldItem == null) {
                Log.e("StorageHelper", "数据库里没找到单词: $targetWord")
                return@withContext null
            }

            // 2. 🚨 无情斩草除根：删除旧的物理文件
            val oldImagePath = oldItem.bestImagePath // 注意替换为你 ShowcaseItem 里实际的变量名
            if (!oldImagePath.isNullOrEmpty()) {
                val oldFile = File(oldImagePath)
                if (oldFile.exists() && oldFile.delete()) {
                    Log.d("StorageHelper", "成功删除旧文件: $oldImagePath")
                }
            }

            // 3. 生成永不冲突的新名字，并指向安全的 filesDir
            val fileName = "vision_voice_${System.currentTimeMillis()}.jpg"
            val newFile = File(context.filesDir, fileName)
            val out = FileOutputStream(newFile)

            // 4. 殊途同归：把新图写入沙盒
            if (bitmap != null) {
                // 路线 A: AR 截帧
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            } else if (uri != null) {
                // 路线 B: 相册选图 (直接通过流拷贝，绝对安全)
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.copyTo(out)
                inputStream?.close()
            }
            out.flush()
            out.close()

            // 5. 更新数据库通讯录
            oldItem.bestImagePath = newFile.absolutePath
            appDao.updateShowcaseItem(oldItem)

            Log.d("StorageHelper", "新图片保存并更新成功: ${newFile.absolutePath}")
            return@withContext newFile.absolutePath

        } catch (e: Exception) {
            Log.e("StorageHelper", "保存图片失败", e)
            return@withContext null
        }
    }
}