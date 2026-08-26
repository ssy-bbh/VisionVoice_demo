# ADR-003: 500ms 节流阀 + 单次遍历后处理

## 状态
✅ **已接受**（v1.1 性能优化阶段）

## 背景

在实时物体识别功能上线后，我们发现了一个严重的**用户体验问题**：识别结果在屏幕上"闪烁"——边界框位置和物体标签在连续帧之间频繁跳动，用户无法稳定阅读识别出的单词。

**根因分析**：
1. 摄像头以 ~30 FPS 的速度输出图像帧，每帧都触发 YOLO 推理
2. YOLO 模型在连续帧之间的预测存在天然波动（物体边缘、光照变化）
3. 每次推理结果都立即更新 UI，导致"闪烁"效应

## 问题陈述

如何在不牺牲识别实时性的前提下，消除实时识别中的结果闪烁，提供稳定、流畅的 AR 显示效果？

## 决策方案（已采纳）

### 方案一：时间节流阀（Throttling）

在 `RealtimeActivity.ImageAnalysis.Analyzer` 中引入 `lastAnalysisTime` 时间戳判断：

```java
private static final long ANALYSIS_INTERVAL_MS = 500; // 500ms = 约 2 FPS

public void analyze(@NonNull ImageProxy imageProxy) {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastAnalysisTime < ANALYSIS_INTERVAL_MS) {
        imageProxy.close(); // 丢弃中间帧
        return;
    }
    lastAnalysisTime = currentTime;
    // ... 执行推理
}
```

### 方案二：单次遍历后处理（已同步到 ADR-001）

重构 `ObjectRecognitionHelper.postProcess()`：

**优化前（O(2N) 复杂度）**：
```java
// 1. 完整转置 [84][8400] → [8400][84]
float[][] transposed = new float[8400][84];
for (int j = 0; j < 84; j++)
    for (int i = 0; i < 8400; i++)
        transposed[i][j] = output[0][j][i];

// 2. 外层遍历找每个预测的最高类别分
for (int i = 0; i < 8400; i++) {
    // 内层再遍历 40 个类别
    for (int j = 4; j < 84; j++) { ... }
}
```

**优化后（O(N) 复杂度）**：
```java
// 单次遍历，同时完成：① 找各类别最高分 ② 找全局最优
for (int i = 0; i < 8400; i++) {
    for (int j = 0; j < numClasses; j++) {
        float score = output[0][j + 4][i]; // 直接从原张量读取
        if (score > maxScore) {
            maxScore = score;
            maxScoreIndex = j;
        }
    }
    // 在同一循环内更新全局最优
    if (maxScore > topConfidence && maxScore > 0.5f) {
        topConfidence = maxScore;
        // ... 更新结果
    }
}
```

## 后果

### ✅ 正面后果
- **消除闪烁**：识别结果刷新频率从 ~30 FPS 降至 ~2 FPS，UI 稳定可读
- **降低 CPU 负载**：平均每秒推理次数从 30 次降至 2 次，CPU 占用率显著降低
- **电池友好**：减少不必要的推理运算，延长设备续航
- **算法优化**：单次遍历后处理将后处理延迟降低约 50%

### ⚠️ 可迁移后果
- 最优帧可能被节流阀意外丢弃 → 通过"时间戳最近优先"策略缓解
- 节流间隔（500ms）是经验值，未来可考虑动态调整（基于设备性能自适应）

## 参考资料
- VisionVoice `RealtimeActivity.java` — `analyze()` 方法
- VisionVoice `ObjectRecognitionHelper.java` — `postProcess()` 方法
- PROJECT_REPORT_CA1.md §4.3 核心挑战三
