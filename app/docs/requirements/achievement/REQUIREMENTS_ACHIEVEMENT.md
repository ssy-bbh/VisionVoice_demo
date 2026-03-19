# VisionVoice 成就系统需求规格说明书

**项目名称：** VisionVoice - AR 英语学习助手  
**功能编号：** F-007  
**版本：** 1.0  
**编制日期：** 2026-03-19

---

## 1. 功能概述

### 1.1 功能描述

成就系统是 VisionVoice 的激励机制核心，通过设置各类学习成就徽章，激励用户持续学习、探索应用功能，形成正向学习循环。

### 1.2 业务背景

- 提高用户留存率和活跃度
- 通过游戏化设计提升学习趣味性
- 展示用户学习成果，增强成就感

---

## 2. 成就类型定义

### 2.1 成就分类

| 分类ID | 分类名称 | 描述 |
|--------|----------|------|
| CAT_LEARNING | 学习成就 | 与学习行为相关的成就 |
| CAT_EXPLORATION | 探索成就 | 与功能使用相关的成就 |
| CAT_STREAK | 坚持成就 | 与学习连续性相关的成就 |
| CAT_SPECIAL | 特殊成就 | 限时/特殊条件成就 |

### 2.2 成就列表

#### 学习成就 (CAT_LEARNING)

| 成就ID | 成就名称 | 描述 | 条件 | 奖励 |
|--------|----------|------|------|------|
| ACH001 | 初学者 | 开启学习之旅 | 完成首次发音练习 | 解锁"学习达人"称号 |
| ACH010 | 词汇新手 | 积累第一批词汇 | 学习 10 个不同单词 | 成就徽章 |
| ACH011 | 词汇达人 | 词汇量初具规模 | 学习 50 个不同单词 | 成就徽章 + 50积分 |
| ACH012 | 词汇专家 | 词汇量达到中级 | 学习 100 个不同单词 | 成就徽章 + 100积分 |
| ACH013 | 词汇大师 | 词汇量达到高级 | 学习 300 个不同单词 | 成就徽章 + 200积分 |
| ACH014 | 完美主义者 | 追求极致发音 | 获得 10 次 100 分 | 成就徽章 |
| ACH015 | 发音大师 | 发音准确度超高 | 获得 50 次 90+ 分 | 成就徽章 + 100积分 |

#### 探索成就 (CAT_EXPLORATION)

| 成就ID | 成就名称 | 描述 | 条件 | 奖励 |
|--------|----------|------|------|------|
| ACH020 | 探索者 | 开启探索之旅 | 使用实时识别功能 | 解锁"探索者"称号 |
| ACH021 | 好奇宝宝 | 探索各个领域 | 使用过 10 个不同识别类别 | 成就徽章 |
| ACH022 | 全能识别 | 覆盖所有领域 | 使用过全部 40 个识别类别 | 成就徽章 + 100积分 |
| ACH023 | 照片达人 | 喜欢拍照识别 | 使用照片识别功能 20 次 | 成就徽章 |
| ACH024 | 声音捕捉者 | 多次练习发音 | 完成 50 次发音练习 | 成就徽章 |

#### 坚持成就 (CAT_STREAK)

| 成就ID | 成就名称 | 描述 | 条件 | 奖励 |
|--------|----------|------|------|------|
| ACH030 | 第一天 | 开始学习之旅 | 连续学习 1 天 | 解锁"坚持"称号 |
| ACH031 | 坚持不懈 | 保持学习习惯 | 连续学习 7 天 | 成就徽章 |
| ACH032 | 周末学习者 | 周末也在学习 | 连续学习 14 天 | 成就徽章 |
| ACH033 | 月度学习者 | 一个月过去了 | 连续学习 30 天 | 成就徽章 + 100积分 |
| ACH034 | 季度学习者 | 三个月如一日 | 连续学习 90 天 | 成就徽章 + 200积分 |
| ACH035 | 学年学习者 | 半个学年坚持 | 连续学习 180 天 | 成就徽章 + 300积分 |
| ACH036 | 全勤奖 | 一年365天 | 连续学习 365 天 | 终极成就徽章 + 500积分 |

#### 特殊成就 (CAT_SPECIAL)

| 成就ID | 成就名称 | 描述 | 条件 | 奖励 |
|--------|----------|------|------|------|
| ACH040 | 收藏达人 | 积累收藏 | 收藏 50 个单词 | 成就徽章 |
| ACH041 | 收藏大师 | 收藏量惊人 | 收藏 200 个单词 | 成就徽章 + 100积分 |
| ACH050 | 时光旅行者 | 夜间学习 | 凌晨 0-6 点使用应用 | 隐藏成就徽章 |
| ACH051 | 早起鸟儿 | 晨间学习 | 早上 6-8 点使用应用 | 隐藏成就徽章 |
| ACH052 | 错题攻关 | 攻克难点 | 同一个单词练习 10 次 | 成就徽章 |

---

## 3. 功能需求

### 3.1 成就展示

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-007-01 | 在用户主页/个人中心展示成就入口 | P0 |
| F-007-02 | 显示已获得成就数量/总数（如：5/20） | P0 |
| F-007-03 | 点击进入成就详情页 | P0 |
| F-007-04 | 成就详情页展示所有成就列表 | P0 |
| F-007-05 | 已解锁成就显示徽章图标和名称 | P0 |
| F-007-06 | 未解锁成就显示灰色占位符和解锁条件 | P0 |
| F-007-07 | 点击未解锁成就显示详细信息（如何解锁） | P1 |

### 3.2 成就解锁

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-007-08 | 用户满足成就条件时自动检测并解锁 | P0 |
| F-007-09 | 解锁时显示成就解锁弹窗动画 | P0 |
| F-007-10 | 弹窗显示成就名称、描述、奖励 | P0 |
| F-007-11 | 弹窗可选择"分享"或"关闭" | P1 |
| F-007-12 | 成就解锁后发送系统通知（可选） | P2 |
| F-007-13 | 首页顶部显示最新解锁成就提示 | P1 |

### 3.3 成就进度

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-007-14 | 部分成就支持显示当前进度/完成百分比 | P1 |
| F-007-15 | 进度实时更新（如：已学习 8/10 个单词） | P1 |
| F-007-16 | 连续学习天数在首页显著位置显示 | P0 |

### 3.4 成就分享

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-007-17 | 支持将成就分享到微信朋友圈 | P1 |
| F-007-18 | 分享图片包含成就名称、图标、获得时间 | P1 |
| F-007-19 | 支持生成成就海报（多个成就组合） | P2 |

### 3.5 积分系统（扩展）

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-007-20 | 成就解锁奖励积分（积分用途待定） | P2 |
| F-007-21 | 积分可查看来源明细 | P2 |

---

## 4. 界面设计

### 4.1 成就入口（首页）

**位置：** 首页顶部Banner或个人中心入口

**显示：**
```
┌─────────────────────────────────────┐
│  🎖️ 已获得 5/36 成就     查看详情 → │
└─────────────────────────────────────┘
```

### 4.2 成就解锁弹窗

**触发：** 满足成就条件时自动弹出

**布局：**
```
┌─────────────────────────────────────┐
│                                     │
│            🏆                       │
│                                     │
│         成就解锁！                   │
│                                     │
│        [成就名称]                   │
│        [成就描述]                   │
│                                     │
│        奖励: +50 积分               │
│                                     │
│    [分享朋友圈]      [知道了]       │
│                                     │
└─────────────────────────────────────┘
```

### 4.3 成就详情页

**布局：**
```
┌─────────────────────────────────────┐
│  ← 成就徽章              🔍 筛选   │
├─────────────────────────────────────┤
│ 当前连续: 🔥 7 天                    │
│ 总成就数: 5/36                       │
├─────────────────────────────────────┤
│ [全部] [学习] [探索] [坚持] [特殊]  │
├─────────────────────────────────────┤
│ 🏆 初学者                    ✓     │
│    完成首次发音练习                  │
│ ─────────────────────────────────  │
│ 🏆 词汇新手              8/10    │
│    已学习 8 个单词，还差 2 个       │
│ ─────────────────────────────────  │
│ 🏆 坚持不懈              3/7      │
│    已连续学习 3 天，还差 4 天       │
│ ─────────────────────────────────  │
│ 🏆 全能识别                    ✗     │
│    使用全部 40 个识别类别           │
└─────────────────────────────────────┘
```

---

## 5. 接口设计

### 5.1 获取成就列表

```
GET /api/achievements
Authorization: Bearer <token>

Response:
{
    "success": true,
    "data": {
        "total": 36,
        "unlocked_count": 5,
        "current_streak": 7,
        "achievements": [
            {
                "id": "ACH001",
                "name": "初学者",
                "description": "完成首次发音练习",
                "category": "LEARNING",
                "unlocked": true,
                "unlocked_at": "2026-03-19T10:00:00Z",
                "reward": 0,
                "progress": null
            },
            {
                "id": "ACH010",
                "name": "词汇新手",
                "description": "学习 10 个不同单词",
                "category": "LEARNING",
                "unlocked": false,
                "progress": {
                    "current": 8,
                    "target": 10
                }
            }
        ]
    }
}
```

### 5.2 成就解锁通知（WebSocket或轮询）

```
Event: achievement_unlocked
{
    "achievement": {
        "id": "ACH010",
        "name": "词汇新手",
        "description": "学习 10 个不同单词",
        "reward": 50
    }
}
```

### 5.3 分享成就

```
POST /api/achievements/share
Authorization: Bearer <token>
Content-Type: application/json

{
    "achievement_id": "ACH010"
}

Response:
{
    "success": true,
    "data": {
        "share_image_url": "https://cdn.example.com/share/ach010.png"
    }
}
```

---

## 6. 数据模型

### 6.1 成就定义表（系统预置）

```sql
CREATE TABLE achievement_definitions (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    category VARCHAR(20) NOT NULL,  -- LEARNING, EXPLORATION, STREAK, SPECIAL
    condition_type VARCHAR(50) NOT NULL,
    condition_value INT NOT NULL,
    reward_points INT DEFAULT 0,
    is_hidden BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0
);
```

### 6.2 用户成就表

```sql
CREATE TABLE user_achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    achievement_id VARCHAR(20) NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (achievement_id) REFERENCES achievement_definitions(id),
    
    UNIQUE KEY uk_user_achievement (user_id, achievement_id)
);

CREATE INDEX idx_user_achievements_user ON user_achievements(user_id);
```

### 6.3 用户统计表

```sql
CREATE TABLE user_stats (
    user_id BIGINT PRIMARY KEY,
    total_words_learned INT DEFAULT 0,
    total_learning_time INT DEFAULT 0,  -- 分钟
    total_practice_count INT DEFAULT 0,
    current_streak INT DEFAULT 0,
    longest_streak INT DEFAULT 0,
    last_learned_date DATE,
    total_collections INT DEFAULT 0,
    total_achievements INT DEFAULT 0,
    total_points INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 7. 成就检测逻辑

### 7.1 检测触发时机

| 成就类型 | 触发时机 |
|----------|----------|
| 学习类 | 每次发音练习完成 |
| 探索类 | 每次使用识别功能 |
| 坚持类 | 每日首次使用应用 |
| 特殊类 | 满足特定条件时 |

### 7.2 检测算法

```java
public class AchievementChecker {
    
    public void checkAchievements(User user, LearningEvent event) {
        // 1. 获取用户当前统计
        UserStats stats = getUserStats(user.getId());
        
        // 2. 获取所有未解锁成就
        List<Achievement> lockedAchievements = getLockedAchievements(user.getId());
        
        // 3. 逐个检查条件
        for (Achievement achievement : lockedAchievements) {
            if (evaluateCondition(achievement, stats, event)) {
                // 4. 解锁成就
                unlockAchievement(user, achievement);
                
                // 5. 发送通知
                sendNotification(user, achievement);
            }
        }
    }
}
```

---

## 8. 验收标准

- [ ] 成就入口在首页正确显示
- [ ] 成就详情页展示所有成就
- [ ] 已解锁/未解锁状态区分明显
- [ ] 满足条件时自动解锁成就
- [ ] 解锁弹窗正确显示
- [ ] 连续学习天数正确计算
- [ ] 成就解锁后正确记录到数据库
- [ ] 成就数据支持云端同步
