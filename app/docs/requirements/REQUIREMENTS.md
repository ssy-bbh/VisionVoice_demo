# VisionVoice 需求规格说明书 (SRS)

**项目名称：** VisionVoice - AR 英语学习助手  
**版本：** v1.0  
**编制日期：** 2026-03-19  
**编制人：** OpenClaw (AI Assistant)

---

## 1. 引言

### 1.1 目的

本文档旨在完整记录 VisionVoice 系统的功能需求和非功能需求，为开发团队提供清晰的设计和实现指导。本文档涵盖已实现功能的需求规格，以及待实现功能的初步需求定义。

### 1.2 范围

VisionVoice 是一款基于 AR 和 AI 的英语学习应用，通过计算机视觉识别日常物品，为用户提供"看到什么就学什么"的即时学习体验，并结合语音识别技术进行发音练习。

---

## 2. 用户角色与特征

### 2.1 主要用户角色

| 角色 | 描述 | 典型场景 |
|------|------|----------|
| 学习者 | 希望通过图像和发音学习英语词汇的用户 | 学生、日常语言学习者 |
| 教育者 | 教学中使用 AR 辅助教学的教师 | 学校教师、培训机构 |
| 游客 | 对园林、博物馆等场景中物品感兴趣的访客 | 博物馆参观者、旅游者 |

### 2.2 用户特征

- **技术接受度：** 中等偏上，愿意尝试新技术的年轻用户
- **使用频率：** 每日多次使用，每次 5-15 分钟
- **设备要求：** Android 7.0+ (API 24+)，推荐 4GB+ RAM

---

## 3. 功能需求

### 3.1 已实现功能

#### 3.1.1 实时 AR 物体识别 (F-001)

**功能描述：**  
用户通过摄像头扫描周围环境，系统实时识别画面中的物体，并在物体周围显示边界框和对应的英文词汇。

**业务规则：**
- 识别对象为日常生活中常见的 40 类物品（详见附件 A）
- 单次最多显示 3 个识别结果，按置信度排序
- 识别到物体后，用户点击边界框可进入发音练习

**用户流程：**
1. 用户点击首页"实时扫描"按钮
2. 打开摄像头实时预览
3. 系统每 500ms 进行一次物体识别
4. 识别结果显示边界框和英文词汇
5. 用户点击某个物体，进入发音练习页面

**优先级：** P0 (核心功能)

---

#### 3.1.2 照片物体识别 (F-002)

**功能描述：**  
用户从相册选择图片或拍摄照片，系统识别图片中的物体并返回词汇列表。

**业务规则：**
- 支持 JPEG、PNG 格式
- 单张图片最多识别 10 个物体
- 识别结果可批量加入学习列表

**优先级：** P0 (核心功能)

---

#### 3.1.3 发音练习 (F-003)

**功能描述：**  
用户选择目标词汇后，系统展示该词汇的音标和中文释义，用户跟读发音，系统评估发音准确度并给出反馈。

**业务规则：**
- 音标获取：调用后端 API (FastAPI) 或本地缓存
- 录音：用户按住录音按钮进行跟读，松开后停止
- 评分：使用 Wav2Vec2 模型进行语音识别，与目标音素对比
- 反馈：显示每个音素的对错（Match/Flaw/Substitution）

**评分算法：**
- Match（完全正确）：100分
- Flaw（轻微错误）：60分  
- Substitution（完全错误）：0分
- 总分 = (Match数×100 + Flaw数×60) / 总音素数

**优先级：** P0 (核心功能)

---

#### 3.1.4 语音合成 (TTS) (F-004)

**功能描述：**  
系统朗读目标词汇，帮助用户正确理解发音。

**业务规则：**
- 使用 Android TextToSpeech 引擎
- 支持英语发音
- 语速可调节（0.5x - 2.0x）

**优先级：** P1 (重要功能)

---

### 3.2 待实现功能

#### 3.2.1 用户登录与认证 (F-005)

**功能描述：**  
用户注册账号并登录系统，以同步学习数据和成就进度。

**业务规则：**
- 支持手机号 + 验证码登录
- 支持游客模式（不登录也可使用基础功能）
- 登录后自动同步本地数据到云端
- 支持退出登录

**数据需求：**
```
用户表 (users)
- id: 主键
- phone: 手机号
- nickname: 昵称
- avatar_url: 头像URL
- created_at: 创建时间
- last_login: 最后登录时间
```

**优先级：** P1 (重要功能)

**接口设计：**
```
POST /api/auth/send_code
  参数: phone (String)
  返回: {success: Boolean, message: String}

POST /api/auth/login
  参数: phone, code
  返回: {token: String, user: User}

POST /api/auth/logout
  头部: Authorization: Bearer <token>
  返回: {success: Boolean}
```

---

#### 3.2.2 学习收藏集 (F-006)

**功能描述：**  
用户将学习过的词汇收藏到个人收藏集，形成自己的单词本。

**业务规则：**
- 用户可在发音练习页面收藏当前词汇
- 收藏集支持分类（按主题、按时间）
- 支持搜索收藏的词汇
- 支持导出收藏集（CSV 格式）

**数据需求：**
```
收藏表 (collections)
- id: 主键
- user_id: 用户ID
- word: 英文单词
- phonetics: 音标
- meaning: 中文释义
- category: 分类
- learned_count: 学习次数
- last_learned: 最后学习时间
- created_at: 收藏时间
```

**优先级：** P1 (重要功能)

**接口设计：**
```
GET /api/collections
  头部: Authorization: Bearer <token>
  返回: {collections: Array<Collection>}

POST /api/collections
  头部: Authorization: Bearer <token>
  参数: word, phonetics, meaning, category
  返回: {id: Number, success: Boolean}

DELETE /api/collections/:id
  头部: Authorization: Bearer <token>
  返回: {success: Boolean}

GET /api/collections/export
  头部: Authorization: Bearer <token>
  返回: CSV 文件下载
```

---

#### 3.2.3 学习成就系统 (F-007)

**功能描述：**  
用户完成特定学习任务后获得成就徽章，激励持续学习。

**成就类型：**

| 成就ID | 名称 | 条件 | 奖励 |
|--------|------|------|------|
| ACH001 | 初学者 | 完成首次发音练习 | 解锁新主题 |
| ACH002 | 词汇达人 | 学习 50 个不同单词 | 成就徽章 |
| ACH003 | 每日坚持 | 连续 7 天学习 | 连续打卡徽章 |
| ACH004 | 完美发音 | 获得 10 次 100 分 | 完美徽章 |
| ACH005 | 探索者 | 使用过所有识别类别 | 探索者徽章 |
| ACH006 | 收藏家 | 收藏 100 个单词 | 收藏家徽章 |
| ACH007 | 学期王者 | 累计学习 100 小时 | 最高成就 |

**数据需求：**
```
成就表 (achievements)
- id: 主键
- user_id: 用户ID
- achievement_id: 成就ID
- unlocked_at: 解锁时间

用户统计 (user_stats)
- user_id: 主键
- total_words: 学习单词总数
- total_time: 累计学习时间(分钟)
- current_streak: 当前连续学习天数
- longest_streak: 最长连续学习天数
- created_at: 账户创建时间
```

**优先级：** P2 (增强功能)

---

#### 3.2.4 学习统计 (F-008)

**功能描述：**  
展示用户的学习数据统计，包括学习时长、词汇量、发音准确度趋势等。

**展示内容：**
- 本周/本月学习天数柱状图
- 词汇量增长曲线
- 发音准确度趋势图
- 各分类学习占比饼图
- 学习时长统计

**数据需求：**
```
学习记录表 (learning_records)
- id: 主键
- user_id: 用户ID
- word: 单词
- score: 得分
- duration: 学习时长(秒)
- learned_at: 学习时间

每日统计表 (daily_stats)
- id: 主键
- user_id: 用户ID
- date: 日期
- words_learned: 学习单词数
- time_spent: 学习时长(分钟)
- avg_score: 平均分
```

**优先级：** P2 (增强功能)

---

#### 3.2.5 用户资料与设置 (F-009)

**功能描述：**  
用户管理个人资料和系统设置。

**功能点：**
- 修改昵称和头像
- 绑定/解绑手机号
- 设置学习目标（每日单词数）
- 设置提醒时间
- 切换发音评估灵敏度（高/中/低）
- 清除本地数据
- 关于/版本信息
- 隐私政策链接
- 用户协议链接

**数据需求：**
```
用户设置表 (user_settings)
- user_id: 主键
- daily_goal: 每日目标单词数
- reminder_enabled: 是否开启提醒
- reminder_time: 提醒时间(HH:mm)
- sensitivity: 评估灵敏度(high/medium/low)
- theme: 主题(light/dark/system)
- updated_at: 更新时间
```

**优先级：** P2 (增强功能)

---

#### 3.2.6 离线模式支持 (F-010)

**功能描述：**  
核心功能在无网络环境下仍可使用。

**离线可用功能：**
- 物体识别（模型已内置）
- 发音练习（本地 ONNX 评估）
- TTS 语音合成
- 本地数据存储

**需要联网的功能：**
- 用户登录/注册
- 数据同步
- 音标在线查询（首次查询后缓存）

**优先级：** P1 (重要功能)

---

### 3.3 未来可能的功能

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|----------|------|--------|
| F-011 | 场景模式 | 识别特定场景（教室、厨房、办公室）自动推荐相关词汇 | P3 |
| F-012 | 多人对战 | 与好友比拼发音准确度 | P3 |
| F-013 | AI 口语对话 | 基于识别物体进行情景对话练习 | P3 |
| F-014 | AR 收藏 | 在 AR 场景中收藏虚拟物品 | P3 |
| F-015 | 生词本复习 | 基于艾宾浩斯遗忘曲线提醒复习 | P2 |

---

## 4. 非功能需求

### 4.1 性能需求

| 指标 | 要求 | 说明 |
|------|------|------|
| 物体识别延迟 | ≤500ms | 从摄像到显示结果 |
| 语音识别延迟 | ≤800ms | 从录音结束到显示评分 |
| 应用启动时间 | ≤3s | 冷启动 |
| 内存占用 | ≤500MB | 正常运行时的 Java 堆 |
| 电池消耗 | ≤10%/小时 | 正常使用场景 |

### 4.2 兼容性需求

- **操作系统：** Android 7.0 (API 24) 至 Android 14 (API 34)
- **屏幕尺寸：** 5寸 - 10寸
- **架构：** ARM64, ARMv7

### 4.3 安全需求

- 用户密码本地存储使用加密
- 网络请求使用 HTTPS
- 敏感数据不记录日志

### 4.4 可用性需求

- 关键功能有离线降级方案
- 错误提示友好，不显示技术术语
- 支持深色主题

---

## 5. 数据流设计

### 5.1 核心数据流

```
用户拍照/摄像
    ↓
CameraX 采集帧
    ↓
YOLOv8n 物体识别 (TFLite)
    ↓
返回识别结果 (物体类别 + 边界框)
    ↓
显示 AR 叠加层
    ↓
用户点击选择
    ↓
获取音标 (本地缓存/后端API)
    ↓
TTS 朗读
    ↓
用户跟读录音
    ↓
Wav2Vec2 语音评估 (ONNX Runtime)
    ↓
评分反馈
    ↓
用户收藏 → 本地数据库
```

### 5.2 本地数据存储

使用 Room 数据库：
- `WordDao`: 单词数据
- `CollectionDao`: 收藏数据
- `LearningRecordDao`: 学习记录
- `AchievementDao`: 成就数据

---

## 6. 模块设计

### 6.1 已实现模块

| 模块名 | 路径 | 功能 |
|--------|------|------|
| ObjectRecognitionHelper | ml/ObjectRecognitionHelper.java | YOLOv8n 推理封装 |
| Wav2Vec2Scorer | ml/Wav2Vec2Scorer.java | 语音评分封装 |
| AudioProcessor | ml/AudioProcessor.java | 音频预处理 |
| RealtimeActivity | ui/ar/RealtimeActivity.java | 实时识别页面 |
| PhotoRecognitionActivity | ui/photo/PhotoRecognitionActivity.java | 照片识别页面 |
| PracticeActivity | ui/practice/PracticeActivity.java | 发音练习页面 |
| HomeFragment | ui/home/HomeFragment.java | 首页 |

### 6.2 待实现模块

| 模块名 | 功能 | 依赖 |
|--------|------|------|
| LoginActivity | 登录/注册页面 | F-005 |
| ProfileFragment | 用户资料页面 | F-009 |
| CollectionFragment | 收藏集页面 | F-006 |
| StatisticsFragment | 学习统计页面 | F-008 |
| AchievementManager | 成就管理系统 | F-007 |
| SyncManager | 数据同步服务 | F-005 |
| LocalDatabase | Room 本地数据库 | 所有离线功能 |

---

## 7. 附件

### 附件 A：40 类识别物体列表

```
日常用品: backpack, book, bowl, cup, keyboard, laptop, mouse, remote, phone, suitcase
食物: apple, banana, orange, pizza, donut, cake, fruit, carrot
家居: bed, chair, couch, table, TV, clock, vase, scissors, teddy bear, hair drier, toothbrush
动物: bird, cat, dog, horse, sheep, cow, elephant, bear, zebra, giraffe
户外: bicycle, car, motorcycle, airplane, bus, train, truck, boat, traffic light, fire hydrant, stop sign
```

### 附件 B：API 接口清单

（见各功能需求的接口设计部分）

---

**文档结束**

*本需求文档将随项目迭代持续更新。*
