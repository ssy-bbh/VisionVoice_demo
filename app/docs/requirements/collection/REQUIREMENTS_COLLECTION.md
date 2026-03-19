# VisionVoice 学习收藏集系统需求规格说明书

**项目名称：** VisionVoice - AR 英语学习助手  
**功能编号：** F-006  
**版本：** 1.0  
**编制日期：** 2026-03-19

---

## 1. 功能概述

### 1.1 功能描述

学习收藏集是用户管理个人学习词汇的核心模块，允许用户将学习过的单词收藏到个人单词本中，支持分类管理、搜索、复习提醒等功能。

### 1.2 业务背景

- 用户在实时识别和发音练习中会接触到大量新词汇
- 需要一个个人化的单词本来记录学习进度
- 支持按主题分类便于复习和查找

---

## 2. 用户角色与使用场景

### 2.1 主要用户场景

**场景1：收藏单词**
```
1. 用户在发音练习页面完成一次练习
2. 页面显示"收藏"按钮
3. 用户点击收藏 → 收藏成功 toast 提示
4. 单词自动添加到默认分类
```

**场景2：查看收藏集**
```
1. 用户点击底部导航栏"收藏集"
2. 显示所有收藏的单词列表
3. 默认按收藏时间倒序排列
4. 用户可点击单词进入复习页面
```

**场景3：分类管理**
```
1. 用户在收藏集页面点击"分类管理"
2. 显示已有的分类列表
3. 可新建、编辑、删除分类
4. 可将单词移动到不同分类
```

**场景4：搜索单词**
```
1. 用户在收藏集页面顶部搜索框
2. 输入单词或中文释义
3. 实时显示匹配的收藏单词
4. 点击结果进入详情页
```

**场景5：导出收藏集**
```
1. 用户点击右上角"导出"按钮
2. 选择导出格式（CSV）
3. 系统生成文件
4. 分享给其他应用（微信、邮箱等）
```

---

## 3. 功能需求

### 3.1 收藏功能

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-006-01 | 用户可在发音练习页面收藏当前学习的单词 | P0 |
| F-006-02 | 收藏时自动记录收藏时间和学习次数 | P0 |
| F-006-03 | 已收藏的单词再次收藏时更新记录（学习次数+1） | P0 |
| F-006-04 | 收藏时显示成功/已收藏 toast 提示 | P1 |

### 3.2 收藏列表

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-006-05 | 显示所有收藏单词，支持滚动加载 | P0 |
| F-006-06 | 按收藏时间倒序排列（最新在前） | P0 |
| F-006-07 | 每个条目显示：单词、音标、中文释义、学习次数 | P0 |
| F-006-08 | 下拉刷新收藏列表 | P1 |
| F-006-09 | 空状态显示引导语："还没有收藏，快去学习吧" | P1 |

### 3.3 分类管理

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-006-10 | 系统预设默认分类："全部"、"未分类" | P0 |
| F-006-11 | 用户可新建自定义分类（最多20个） | P1 |
| F-006-12 | 用户可编辑分类名称 | P1 |
| F-006-13 | 用户可删除分类（删除后单词移至"未分类"） | P1 |
| F-006-14 | 单词详情页可修改所属分类 | P1 |

### 3.4 搜索功能

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-006-15 | 支持按英文单词搜索（模糊匹配） | P0 |
| F-006-16 | 支持按中文释义搜索 | P1 |
| F-006-17 | 搜索结果实时显示（输入后立即响应） | P1 |
| F-006-18 | 无结果时显示"未找到匹配的收藏" | P1 |

### 3.5 复习功能

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-006-19 | 点击收藏单词进入发音练习页面 | P0 |
| F-006-20 | 复习后更新最后学习时间和学习次数 | P0 |
| F-006-21 | 支持艾宾浩斯遗忘曲线复习提醒（可选） | P2 |

### 3.6 导出功能

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-006-22 | 支持导出为 CSV 格式 | P1 |
| F-006-23 | CSV 包含：单词、音标、释义、分类、学习次数、收藏时间 | P1 |
| F-006-24 | 导出后支持系统分享（ShareSheet） | P1 |

### 3.7 删除功能

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-006-25 | 长按单词显示删除选项 | P0 |
| F-006-26 | 删除前二次确认 | P0 |
| F-006-27 | 支持批量删除 | P2 |

---

## 4. 界面设计

### 4.1 收藏集页面

**布局：**
```
┌─────────────────────────────┐
│ ← 收藏集              🔍 ↗ │  ← 标题栏
├─────────────────────────────┤
│ [全部] [未分类] [自定义1] │  ← 分类标签（可滑动）
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │ apple    /ˈæp.əl/      │ │
│ │ 苹果                      │ │
│ │ 学习 5 次 · 刚刚收藏    │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ book      /bʊk/        │ │
│ │ 书                        │ │
│ │ 学习 3 次 · 昨天         │ │
│ └─────────────────────────┘ │
│           ...              │
└─────────────────────────────┘
```

**交互：**
- 点击单词卡片 → 进入发音练习
- 长按 → 显示删除选项
- 点击搜索图标 → 展开搜索框
- 点击导出图标 → 显示导出选项

### 4.2 单词详情弹窗

**布局：**
```
┌─────────────────────────────┐
│        apple                │
│        /ˈæp.əl/             │
│                             │
│        苹果                 │
│                             │
│ 分类: [未分类 ▼]           │
│ 学习次数: 5                 │
│ 收藏时间: 2026-03-19       │
│                             │
│  [发音练习]    [删除]       │
└─────────────────────────────┘
```

---

## 5. 接口设计

### 5.1 获取收藏列表

```
GET /api/collections
Authorization: Bearer <token>
Query Parameters:
    category?: string      // 分类ID，不传则返回全部
    page?: number         // 页码，默认1
    page_size?: number    // 每页数量，默认20
    keyword?: string      // 搜索关键词

Response:
{
    "success": true,
    "data": {
        "total": 100,
        "page": 1,
        "page_size": 20,
        "list": [
            {
                "id": 1,
                "word": "apple",
                "phonetics": "/ˈæp.əl/",
                "meaning": "苹果",
                "category_id": 1,
                "category_name": "未分类",
                "learned_count": 5,
                "last_learned": "2026-03-19T10:30:00Z",
                "created_at": "2026-03-19T08:00:00Z"
            }
        ]
    }
}
```

### 5.2 添加收藏

```
POST /api/collections
Authorization: Bearer <token>
Content-Type: application/json

{
    "word": "apple",
    "phonetics": "/ˈæp.əl/",
    "meaning": "苹果",
    "category_id": 1
}

Response:
{
    "success": true,
    "data": {
        "id": 1,
        "message": "收藏成功"
    }
}
```

### 5.3 更新收藏

```
PUT /api/collections/:id
Authorization: Bearer <token>
Content-Type: application/json

{
    "category_id": 2,
    "learned_count": 6
}

Response:
{
    "success": true
}
```

### 5.4 删除收藏

```
DELETE /api/collections/:id
Authorization: Bearer <token>

Response:
{
    "success": true,
    "message": "删除成功"
}
```

### 5.5 获取分类列表

```
GET /api/collections/categories
Authorization: Bearer <token>

Response:
{
    "success": true,
    "data": [
        {"id": 0, "name": "全部", "count": 100},
        {"id": 1, "name": "未分类", "count": 30},
        {"id": 2, "name": "食物", "count": 25},
        {"id": 3, "name": "动物", "count": 20}
    ]
}
```

### 5.6 导出收藏

```
GET /api/collections/export
Authorization: Bearer <token>
Query: format=csv

Response: 文件下载 (text/csv)
```

CSV 格式：
```csv
单词,音标,释义,分类,学习次数,收藏时间
apple,/ˈæp.əl/,苹果,食物,5,2026-03-19
book,/bʊk/,书,学习用品,3,2026-03-18
```

---

## 6. 数据模型

### 6.1 收藏表 (collections)

```sql
CREATE TABLE collections (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    word VARCHAR(100) NOT NULL,
    phonetics VARCHAR(100),
    meaning VARCHAR(200),
    category_id BIGINT DEFAULT 1,
    learned_count INT DEFAULT 0,
    last_learned_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    
    UNIQUE KEY uk_user_word (user_id, word)
);

CREATE INDEX idx_collections_user ON collections(user_id);
CREATE INDEX idx_collections_word ON collections(word);
CREATE INDEX idx_collections_category ON collections(category_id);
```

### 6.2 分类表 (categories)

```sql
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    
    UNIQUE KEY uk_user_category (user_id, name)
);

-- 默认分类在用户注册时创建
INSERT INTO categories (user_id, name) VALUES 
    (NEW_USER_ID, '未分类');
```

---

## 7. 本地存储设计

### 7.1 Room 数据库

```java
@Entity(tableName = "collections")
public class CollectionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String word;
    public String phonetics;
    public String meaning;
    public long categoryId;
    public int learnedCount;
    public long lastLearnedAt;
    public long createdAt;
    public long updatedAt;
    public boolean synced;  // 是否已同步到云端
}

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public int wordCount;
}
```

### 7.2 数据同步策略

```
1. 本地新增收藏 → 标记 synced=false → 后台同步
2. 本地修改收藏 → 标记 synced=false → 后台同步
3. 本地删除收藏 → 后台同步删除
4. 启动时检查 synced=false 的记录 → 批量同步
5. 冲突处理：以服务器数据为准
```

---

## 8. 验收标准

- [ ] 可在发音练习页面成功收藏单词
- [ ] 收藏列表正确显示所有收藏的单词
- [ ] 搜索功能可按单词和释义模糊匹配
- [ ] 分类管理可新建、编辑、删除分类
- [ ] 单词可移动到不同分类
- [ ] CSV 导出包含所有必要字段
- [ ] 删除收藏前有二次确认
- [ ] 离线模式下本地收藏功能正常
