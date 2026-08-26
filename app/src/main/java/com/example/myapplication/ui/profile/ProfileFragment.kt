package com.example.myapplication.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.AppDatabase
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

class ProfileFragment : Fragment() {

    private lateinit var tvStatUnlocked: TextView
    private lateinit var tvStatPerfect: TextView
    private lateinit var tvStatRecords: TextView
    private lateinit var radarChart: RadarChart // 🚨 新增雷达图变量

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        tvStatUnlocked = view.findViewById(R.id.tvStatUnlocked)
        tvStatPerfect = view.findViewById(R.id.tvStatPerfect)
        tvStatRecords = view.findViewById(R.id.tvStatRecords)
        radarChart = view.findViewById(R.id.radarChart) // 绑定控件

        setupRadarChartStyle() // 预先化个妆
        return view
    }

    override fun onResume() {
        super.onResume()
        loadProfileStats()
    }

    // 🎨 魔法发生的地方：赛博朋克主题深度定制
    private fun setupRadarChartStyle() {
        radarChart.apply {
            description.isEnabled = false // 去掉无用的描述文字
            legend.isEnabled = false // 隐藏图例，保持极简
            webLineWidth = 1.5f // 雷达网格主线宽度
            webColor = Color.parseColor("#44FFFFFF") // 半透明白网格
            webLineWidthInner = 1.0f // 内网格线宽度
            webColorInner = Color.parseColor("#22FFFFFF") // 更淡的内网格
            webAlpha = 100 // 网格整体透明度
            setTouchEnabled(false) // 禁用触摸旋转，保持固定姿态更高级
        }

        // 配置 X 轴（也就是雷达图的五个角）
        val xAxis = radarChart.xAxis
        xAxis.textSize = 12f
        xAxis.yOffset = 0f
        xAxis.xOffset = 0f
        xAxis.textColor = Color.parseColor("#06B6D4") // 极客蓝文字
        // 五维能力名字
        xAxis.valueFormatter = IndexAxisValueFormatter(arrayOf("发音精准度", "图鉴广度", "练习活跃度", "突破潜力", "长期记忆"))

        // 配置 Y 轴（数值标尺，我们把它隐藏掉，只看图形比例）
        val yAxis = radarChart.yAxis
        yAxis.setDrawLabels(false)
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 100f // 满分 100
    }

    private fun loadProfileStats() {
        // 🚨 防闪退：先把 ApplicationContext 抓在手里再进 IO 线程。
        // 用户快速切 Tab 时 Fragment 会先被销毁，此时在 IO 线程上调用
        // requireContext() 会抛 IllegalStateException 直接闪退。
        val appContext = context?.applicationContext ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(appContext).appDao()
            val unlockedCount = dao.getUnlockedCount()
            val perfectCount = dao.getPerfectPronunciationCount()
            val totalRecords = dao.getTotalPracticeCount()

            withContext(Dispatchers.Main) {
                // 页面已销毁就不再更新 UI
                if (!isAdded || view == null) return@withContext

                // 1. 更新顶部文字大盘
                tvStatUnlocked.text = unlockedCount.toString()
                tvStatPerfect.text = perfectCount.toString()
                tvStatRecords.text = totalRecords.toString()

                // 2. 动态计算战力面板数据
                updateRadarData(unlockedCount, perfectCount, totalRecords)
            }
        }
    }

    private fun updateRadarData(unlocked: Int, perfect: Int, totalRecords: Int) {
        // 动态将你的真实数据映射到 0-100 的评分体系里
        val accuracyScore = if (totalRecords > 0) min(100f, (perfect.toFloat() / totalRecords) * 100f + 40f) else 10f
        val widthScore = min(100f, unlocked * 10f + 10f)
        val activityScore = min(100f, totalRecords * 5f + 10f)
        val potentialScore = if (unlocked > 0) min(100f, 60f + perfect * 5f) else 10f
        val memoryScore = if (totalRecords > 5) 85f else 20f

        val entries = ArrayList<RadarEntry>()
        entries.add(RadarEntry(accuracyScore))
        entries.add(RadarEntry(widthScore))
        entries.add(RadarEntry(activityScore))
        entries.add(RadarEntry(potentialScore))
        entries.add(RadarEntry(memoryScore))

        val dataSet = RadarDataSet(entries, "战力雷达")
        // 🚨 核心视觉：霓虹发光与半透明填充
        dataSet.color = Color.parseColor("#06B6D4") // 边框颜色 (极客青)
        dataSet.fillColor = Color.parseColor("#06B6D4") // 填充颜色
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 60 // 灵魂半透明 (0-255)
        dataSet.lineWidth = 2.5f
        dataSet.isDrawHighlightCircleEnabled = true
        dataSet.setDrawHighlightIndicators(false)
        dataSet.setDrawValues(false) // 隐藏每个点上的具体数字，保持高级感

        val radarData = RadarData(dataSet)
        radarChart.data = radarData
        radarChart.invalidate() // 刷新图表

        // 🚀 终极特效：入场动画！平滑展开 1.4 秒
        radarChart.animateXY(1400, 1400, Easing.EaseInOutQuad)
    }
}