# ADR-004: ObjectRecognitionHelper — UI 与 ML 解耦

## 状态
✅ **已接受**（v1.1 重构阶段）

## 背景

在 v1.0 开发阶段，`RealtimeActivity` 和 `PhotoRecognitionActivity` 分别独立实现了 YOLO 推理逻辑。代码审查发现两处存在**大量重复代码**：

- 重复的 TFLite 模型加载逻辑
- 重复的图像预处理流程
- 重复的后处理算法
- 重复的资源释放代码

**违反的软件设计原则**：
- ❌ **DRY（Don't Repeat Yourself）**：相同逻辑出现多次，维护成本倍增
- ❌ **SRP（Single Responsibility Principle）**：Activity 承担了 UI 逻辑和 ML 推理双重职责

## 问题陈述

如何在不增加架构复杂度的前提下，消除 ML 推理代码在多个 Activity 间的重复，实现 UI 层与 ML 层的有效解耦？

## 决策方案（已采纳）

遵循 **DRY + SRP 原则**，将所有 ML 逻辑封装到一个独立的 `ObjectRecognitionHelper` 类中：

```java
public class ObjectRecognitionHelper {

    // 私有化 ML 逻辑，对外仅暴露简洁接口
    private final Interpreter tflite;
    private final List<String> labels;
    private final ExecutorService executor;

    // 核心方法：接收图像，返回识别结果（异步回调）
    public void detectObjects(Bitmap bitmap, RecognitionCallback callback) {
        executor.execute(() -> {
            // 图像预处理
            TensorImage tensorImage = preprocess(bitmap);
            // 模型推理
            float[][][] output = inference(tensorImage);
            // 后处理
            Result result = postProcess(output, bitmap.getWidth(), bitmap.getHeight());
            // 异步回调
            callback.onResult(result.label, result.confidence, result.boundingBox);
        });
    }

    // 生命周期管理：严格释放资源
    public void close() {
        tflite.close();
        executor.shutdownNow();
    }
}
```

**关键设计决策**：
1. **面向接口的回调**：`RecognitionCallback` 接口使 ML 结果与具体 UI 组件解耦
2. **单线程执行器**：避免多线程竞争，确保推理线程安全
3. **生命周期绑定**：`close()` 方法与 Activity `onDestroy()` 对齐，确保资源不泄漏
4. **纯 Java 实现**：`ObjectRecognitionHelper` 不依赖 Android Framework（仅需 `Context` 访问 assets），理论上可抽取为独立库

## 评估的备选方案

| 方案 | 优点 | 缺点 | 决策 |
|------|------|------|------|
| **独立 Helper 类（本采纳）** | DRY、SRP、可测试、可复用 | 需要定义接口和回调模式 | ✅ |
| 继承基类 Activity（抽取公共逻辑） | 代码复用简单 | 违反 LSP，耦合 Android 生命周期 | ❌ |
| 使用 Android Architecture Components (ViewModel + Repository) | 官方推荐，生命周期感知 | 引入 Jetpack 依赖，对于本项目过于重量 | ❌ |
| 使用 Service 后台进程 | 解耦最彻底 | 过度设计，增加 IPC 开销 | ❌ |

## 后果

### ✅ 正面后果
- `RealtimeActivity` 和 `PhotoRecognitionActivity` 共享同一 Helper，代码量减少约 **40%**
- ML 模块可通过 **Mock Callback** 独立单元测试，无需 Android 环境
- `ObjectRecognitionHelper` 可被**任何 Android 项目复用**，甚至是其他 Java 项目
- Activity 代码更干净，职责更清晰（UI 归 UI，推理归推理）

### ⚠️ 可迁移后果
- 如果未来引入更多 ML 模型（图像分割、OCR），需要扩展 Helper 或建立"工厂模式"
- 回调的线程安全性需要严格保证（当前使用单线程 ExecutorService，可行）

## 参考资料
- VisionVoice `ObjectRecognitionHelper.java`
- VisionVoice `RealtimeActivity.java`
- VisionVoice `PhotoRecognitionActivity.java`
- PROJECT_REPORT_CA1.md §3.3 决策三
