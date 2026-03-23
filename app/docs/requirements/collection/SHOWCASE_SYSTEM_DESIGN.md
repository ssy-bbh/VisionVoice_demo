# VisionVoice 词汇学习展示柜与打卡系统业务逻辑说明书

## 1. 模块设计概述
本模块是 VisionVoice 系统的核心用户留存与激励模块。系统摒弃了传统的“单词本”列表设计，引入**游戏化（Gamification）**与**艾宾浩斯遗忘曲线（Spaced Repetition）**理念，将抽象的学习记录转化为具象化的“虚拟展示柜”。

系统底层基于 Room 数据库构建，数据流转依托于两个核心维度：
1. **流水维度（Practice Record）：** 记录用户的单次发音练习行为，是统计与打卡功能的基石。
2. **成就维度（Showcase Item）：** 将目标词汇封装为可收集、可升级、会随时间“蒙尘”的虚拟展品。

## 2. 核心数据模型 (Room Database)

### 2.1 练习记录表 (`practice_records`)
作为底层流水表，不可修改，仅做插入和条件查询。
* `id` (Int, 主键自增)
* `word` (String): 目标单词。
* `score` (Int): 机器评测的发音得分（0-100）。
* `timestamp` (Long): 练习完成时间戳。
* `imagePath` (String): 练习时的真实场景截图路径。

### 2.2 展示柜成就表 (`showcase_items`)
承载游戏化收集与遗忘曲线复习机制的核心状态表。
* `id` (Int, 主键自增)
* `targetWord` (String): 预设的目标单词（如 "keyboard"）。
* `category` (String): **【场景分类】** 展品所属的场景套装（如 "Office", "Kitchen"）。
* `isUnlocked` (Boolean): 该物品是否已被用户在真实世界中捕获并解锁。
* `unlockTime` (Long): 首次解锁的时间戳。
* `bestImagePath` (String): 历次练习该单词中，得分最高的那次对应的实景图片路径（用于展柜高亮渲染）。
* `highestScore` (Int): 该物品的历史最高发音得分。
* `lastReviewedTime` (Long): **【复习机制】** 最后一次成功发音练习该单词的时间戳。

## 3. 核心业务流程 (Workflows)

### 3.1 练习结算与展柜更新流程
发生在发音评测得出结果后（异步后台执行）：
1. **流水落库：** 无论得分多少，生成 `PracticeRecord` 存入流水表，实时激活“今日打卡”状态。
2. **展品状态判定：**
    * 查询 `showcase_items` 表中是否存在本次练习的单词。
    * **若未解锁 (`isUnlocked == false`)**：触发【新物品发现】成就。更新 `isUnlocked = true`，写入 `unlockTime` 和当前 `score` 作为最高分，保存 `imagePath`，并将 `lastReviewedTime` 更新为当前时间。
    * **若已解锁 (`isUnlocked == true`)**：
        * **刷新高分：** 若当前 `score > highestScore`，覆盖最高分和最佳截图。
        * **刷新记忆期：** 无论分数是否破纪录，只要完成练习，均将 `lastReviewedTime` 刷新为当前时间，清除“蒙尘”状态。

### 3.2 每日打卡与连击判定流程
不单独建立打卡状态表，基于流水表动态溯源：
1. **今日打卡状态：** 获取当天 00:00 至 23:59 的时间戳范围，查询 `practice_records` 是否存在记录。有记录即视为打卡成功。
2. **连续打卡计算（Streak）：** 以后台查询方式，从“昨日”起逐日向前校验区间内是否存在记录。遇到首个“0 记录”日即中止，累计的天数即为连续打卡天数。

### 3.3 展柜渲染与“蒙尘”复习机制
发生在用户打开 Collection 页面时：
1. **场景化渲染：** 按 `category` 字段对 `showcase_items` 进行分组（如“极客桌面”、“厨房用具”），通过带 Tabs 的 RecyclerView 分类展示。
2. **状态 UI 映射：**
    * **未解锁：** 渲染占位剪影与锁定图标。
    * **已解锁且记忆新鲜：** 加载 `bestImagePath`，展示带有最高分角标的明亮实景卡片。
    * **触发遗忘曲线（蒙尘）：** 若 `当前时间戳 - lastReviewedTime > 预设阈值（如 3 天）`，UI 层对该展品卡片叠加灰度遮罩或灰尘特效，提示用户需要重新在真实世界中寻找并扫描该物体以“擦亮”展品。

## 4. 架构分层设计
1. **Data Layer (`com.example.myapplication.data`)**:
    * `AppDatabase` 管理单例。
    * `PracticeRecord`, `ShowcaseItem` 定义 Entity。
    * `AppDao` 提供复合查询与事务接口。
2. **UI Layer (`ui.collection` / `ui.practice`)**:
    * `PracticeActivity` 负责成绩的计算并唤起 DAO 进行写入操作。
    * `CollectionFragment` 负责读取 DAO 数据，并包含日历视图（CalendarView）与网格展柜视图（RecyclerView）的组合。