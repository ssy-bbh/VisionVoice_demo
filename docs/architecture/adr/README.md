# Architecture Decision Records (ADR)

## 索引

| 编号 | 标题 | 状态 | 日期 | 关联章节 |
|------|------|------|------|----------|
| [ADR-001](adr-001-tflite-interpreter.md) | 采用 TFLite Interpreter API 而非高级 ObjectDetector API | ✅ 已接受 | 2024 | SAD §3.1 / §5.1 |
| [ADR-002](adr-002-onnx-offline-scoring.md) | 端侧 Wav2Vec2 离线发音评分 | ✅ 已接受 | 2025 | SAD §3.4 / §5.2 |
| [ADR-003](adr-003-throttling-optimization.md) | 500ms 节流阀 + 单次遍历后处理 | ✅ 已接受 | v1.1 | SAD §3.3 / §5.3 |
| [ADR-004](adr-004-ml-helper-decoupling.md) | ObjectRecognitionHelper — UI 与 ML 解耦 | ✅ 已接受 | v1.1 | SAD §3.1 / §5.4 |

---

## 新建 ADR 指南

每当有重大架构决策时，在 `adr/` 目录下新建 `adr-XXX-short-title.md` 文件，并在本索引中添加一行。

### ADR 文件命名规范

```
adr-XXX-short-title.md
  │││ │    │        │
  │││ │    │        └── 小写+连字符
  │││ │    └── 简短标题（不超过5个词）
  │││ └── 连字符分隔符
  ││└── 3位数字编号
  │└── 前缀
```

### ADR 状态说明

| 状态 | 说明 |
|------|------|
| 🔎 **已提出** | 正在讨论中，尚未决定 |
| ✅ **已接受** | 正式采纳，正在实施 |
| ⚠️ **已废弃** | 已被新决策取代 |
| ❌ **已否决** | 讨论后决定不采纳 |
