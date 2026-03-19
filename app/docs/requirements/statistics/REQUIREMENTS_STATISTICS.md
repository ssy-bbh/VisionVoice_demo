# VisionVoice 学习统计系统需求规格说明书

**项目名称：** VisionVoice - AR 英语学习助手  
**功能编号：** F-008  
**版本：** 1.0  
**编制日期：** 2026-03-19

---

## 1. 功能概述

### 1.1 功能描述

学习统计系统为用户提供全面的学习数据可视化展示，帮助用户了解自己的学习进度、效果和习惯，从而更好地规划学习目标。

### 1.2 业务背景

- 用户需要了解自己的学习成果和进度
- 数据可视化增强学习动力
- 为个性化推荐提供依据

---

## 2. 功能需求

### 2.1 统计数据概览

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-008-01 | 显示总学习词汇数 | P0 |
| F-008-02 | 显示累计学习时长（小时/分钟） | P0 |
| F-008-03 | 显示当前连续学习天数 | P0 |
| F-008-04 | 显示平均发音得分 | P0 |
| F-008-05 | 显示今日学习词汇数 | P0 |

### 2.2 学习趋势图表

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-008-06 | 显示本周学习天数柱状图（周一到周日） | P0 |
| F-008-07 | 显示本月学习天数柱状图 | P1 |
| F-008-08 | 显示词汇量增长曲线（近30天） | P0 |
| F-008-09 | 显示发音准确率趋势（近30天） | P1 |
| F-008-10 | 支持切换时间范围（本周/本月/全部） | P1 |

### 2.3 分类统计

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-008-11 | 显示各类别学习占比饼图 | P1 |
| F-008-12 | 识别类别使用排行（前10） | P2 |
| F-008-13 | 收藏最多的单词排行（前10） | P2 |

### 2.4 学习日历

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-008-14 | 显示本月学习日历（每日学习状态） | P1 |
| F-008-15 | 有学习的日期显示学习时长颜色深浅 | P1 |
| F-008-16 | 点击日期显示当天学习详情 | P2 |

### 2.5 详细记录

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-008-17 | 显示最近学习记录列表（最近50条） | P0 |
| F-008-18 | 每条记录显示：单词、得分、学习时间 | P0 |
| F-008-19 | 支持按日期筛选学习记录 | P2 |

---

## 3. 界面设计

### 3.1 统计首页

**布局：**
```
┌─────────────────────────────────────┐
│  ← 学习统计                    ↗️  │  ← 标题栏
├─────────────────────────────────────┤
│ 今日  3个单词   🔥 连续 7天        │  ← 核心数据
├─────────────────────────────────────┤
│ 本周学习天数                       │
│ ▓ ▓ ▓ ▓ ▓ ░ ░                    │  ← 柱状图
│ 一  二  三  四  五  六  日        │
├─────────────────────────────────────┤
│ 词汇量增长 (近30天)                │
│     ╭──╮                          │
│   ╭─╯  ╰─╮      ╭─               │  ← 折线图
│ ──╯        ╰────╯                │
│  2月    3月    3/19               │
├─────────────────────────────────────┤
│ 累计学习  12.5 小时               │
│ 平均得分   82 分                   │
│ 学习词汇  58 个                    │
└─────────────────────────────────────┘
```

### 3.2 学习日历视图

**布局：**
```
┌─────────────────────────────────────┐
│  ← 学习日历          < 3月 >       │
├─────────────────────────────────────┤
│ 一  二  三  四  五  六  日         │
│       1   2   3   4   5           │
│  浅  深   深  深   ●              │
│  6   7   8   9  10  11  12       │
│  ●   ●   ●   ●   ●   ●   ●       │
│ ...                                 │
├─────────────────────────────────────┤
│ 图例:                              │
│ ● 学习>30分钟  ● 学习>10分钟      │
│ ○ 学习<10分钟  - 未学习            │
└─────────────────────────────────────┘
```

### 3.3 学习记录列表

**布局：**
```
┌─────────────────────────────────────┐
│  ← 学习记录                    🔍  │
├─────────────────────────────────────┤
│ 今天                               │
│ ┌─────────────────────────────────┐ │
│ │ apple    95分  ✓  2分钟前     │ │
│ ├─────────────────────────────────┤ │
│ │ book     88分  ✓  5分钟前     │ │
│ ├─────────────────────────────────┤ │
│ │ chair    72分  ○ 10分钟前     │ │
│ └─────────────────────────────────┘ │
│ 昨天                               │
│ ┌─────────────────────────────────┐ │
│ │ desk     90分  ✓  昨天 14:30  │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

---

## 4. 接口设计

### 4.1 获取统计数据概览

```
GET /api/stats/overview
Authorization: Bearer <token>

Response:
{
    "success": true,
    "data": {
        "today_words": 3,
        "total_words": 58,
        "total_time_minutes": 750,  // 12.5小时
        "current_streak": 7,
        "avg_score": 82,
        "last_learned_at": "2026-03-19T14:30:00Z"
    }
}
```

### 4.2 获取学习趋势

```
GET /api/stats/trend
Authorization: Bearer <token>
Query Parameters:
    period: string  // "week" | "month" | "all"

Response:
{
    "success": true,
    "data": {
        "period": "month",
        "daily_data": [
            {
                "date": "2026-03-19",
                "words_learned": 5,
                "time_minutes": 25,
                "avg_score": 85
            },
            {
                "date": "2026-03-18",
                "words_learned": 8,
                "time_minutes": 40,
                "avg_score": 78
            }
            // ... 30 days
        ],
        "summary": {
            "total_words": 150,
            "total_time_minutes": 750,
            "avg_daily_words": 5,
            "avg_daily_time": 25
        }
    }
}
```

### 4.3 获取分类统计

```
GET /api/stats/categories
Authorization: Bearer <token>

Response:
{
    "success": true,
    "data": {
        "category_distribution": [
            {"category": "食物", "count": 25, "percentage": 43},
            {"category": "动物", "count": 15, "percentage": 26},
            {"category": "日常用品", "count": 12, "percentage": 21},
            {"category": "其他", "count": 6, "percentage": 10}
        ],
        "top_recognized": [
            {"category": "cup", "count": 15},
            {"category": "book", "count": 12},
            {"category": "apple", "count": 10}
        ]
    }
}
```

### 4.4 获取日历数据

```
GET /api/stats/calendar
Authorization: Bearer <token>
Query Parameters:
    year: number
    month: number

Response:
{
    "success": true,
    "data": {
        "calendar": [
            {"date": "2026-03-01", "words": 0, "time": 0},
            {"date": "2026-03-02", "words": 3, "time": 15, "level": 1},
            {"date": "2026-03-03", "words": 8, "time": 45, "level": 3},
            // ... full month
        ]
    }
}
```

### 4.5 获取学习记录

```
GET /api/stats/records
Authorization: Bearer <token>
Query Parameters:
    page?: number
    page_size?: number
    date?: string  // 筛选特定日期

Response:
{
    "success": true,
    "data": {
        "total": 200,
        "records": [
            {
                "id": 1001,
                "word": "apple",
                "score": 95,
                "status": "perfect",  // perfect | good | need_practice
                "learned_at": "2026-03-19T14:30:00Z",
                "duration_seconds": 30
            }
        ]
    }
}
```

---

## 5. 数据模型

### 5.1 每日统计表

```sql
CREATE TABLE daily_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    words_learned INT DEFAULT 0,
    time_minutes INT DEFAULT 0,
    practice_count INT DEFAULT 0,
    avg_score DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_date (user_id, date)
);

CREATE INDEX idx_daily_stats_user ON daily_stats(user_id);
CREATE INDEX idx_daily_stats_date ON daily_stats(date);
```

### 5.2 学习记录表

```sql
CREATE TABLE learning_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    word VARCHAR(100) NOT NULL,
    phonemes JSON,  // 用户发音音素序列
    target_phonemes JSON,  // 目标音素序列
    score INT,
    status VARCHAR(20),  -- perfect, good, need_practice
    duration_seconds INT,
    learned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_records_user ON learning_records(user_id);
CREATE INDEX idx_records_learned_at ON learning_records(learned_at);
```

---

## 6. 计算逻辑

### 6.1 连续学习天数计算

```
连续学习天数 = 从今天往前推，连续有学习记录的天数
- 今天有学习 → count++
- 昨天有学习 → count++
- 前天有学习 → count++
- ...
- 断开则停止
```

### 6.2 平均得分计算

```
平均得分 = sum(所有学习记录的得分) / 学习记录总数
```

---

## 7. 验收标准

- [ ] 统计首页正确显示所有核心数据
- [ ] 本周学习天数柱状图正确
- [ ] 词汇量增长曲线显示正确
- [ ] 日历视图正确显示每日学习状态
- [ ] 学习记录列表分页正确
- [ ] 离线模式下本地统计数据正常显示
- [ ] 数据同步后统计正确更新
