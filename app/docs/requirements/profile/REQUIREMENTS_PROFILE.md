# VisionVoice 用户资料与设置系统需求规格说明书

**项目名称：** VisionVoice - AR 英语学习助手  
**功能编号：** F-009  
**版本：** 1.0  
**编制日期：** 2026-03-19

---

## 1. 功能概述

### 1.1 功能描述

用户资料与设置系统允许用户管理个人资料信息和应用设置，提供个性化学习体验。

### 1.2 业务背景

- 用户需要管理个人资料（昵称、头像等）
- 用户需要自定义学习设置（每日目标、提醒等）
- 用户需要管理应用行为（主题、通知等）

---

## 2. 功能需求

### 2.1 个人资料

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-009-01 | 显示用户头像（默认显示首字母或默认头像） | P0 |
| F-009-02 | 显示用户昵称（未设置显示"用户"+手机号） | P0 |
| F-009-03 | 显示用户手机号（部分隐藏，如 138****8000） | P0 |
| F-009-04 | 显示账户注册时间 | P1 |
| F-009-05 | 支持修改昵称（2-20字符） | P1 |
| F-009-06 | 支持更换头像（拍照/相册） | P1 |
| F-009-07 | 头像支持裁剪（圆形/方形） | P2 |
| F-009-08 | 支持绑定/解绑手机号 | P2 |

### 2.2 学习设置

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-009-09 | 支持设置每日学习目标（单词数，5-50） | P1 |
| F-009-10 | 支持开启/关闭学习提醒 | P1 |
| F-009-11 | 支持设置提醒时间（小时：分钟） | P1 |
| F-009-12 | 支持设置提醒重复（每天/工作日/自定义） | P2 |
| F-009-13 | 支持设置发音评估灵敏度（高/中/低） | P1 |
| F-009-14 | 灵敏度说明：高=严格，中=适中，低=宽松 | P1 |

### 2.3 应用设置

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-009-15 | 支持设置主题（浅色/深色/跟随系统） | P1 |
| F-009-16 | 支持开启/关闭音效 | P2 |
| F-009-17 | 支持开启/关闭振动反馈 | P2 |
| F-009-18 | 支持设置语言（当前仅支持中文） | P2 |
| F-009-19 | 支持清除本地缓存（图片缓存、模型缓存） | P1 |
| F-009-20 | 清除缓存前显示占用大小 | P1 |

### 2.4 数据管理

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-009-21 | 支持查看账户状态（已登录/游客） | P0 |
| F-009-22 | 支持手动同步数据 | P1 |
| F-009-23 | 支持导出个人学习数据（JSON格式） | P2 |
| F-009-24 | 支持退出登录 | P0 |
| F-009-25 | 退出登录前二次确认 | P0 |
| F-009-26 | 支持注销账户（清除所有数据） | P2 |
| F-009-27 | 注销前需输入密码验证 | P2 |

### 2.5 关于与帮助

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-009-28 | 显示当前版本号（如 v1.0.0） | P0 |
| F-009-29 | 显示版本更新日志入口 | P2 |
| F-009-30 | 跳转至用户协议页面 | P0 |
| F-009-31 | 跳转至隐私政策页面 | P0 |
| F-009-32 | 显示用户协议和隐私政策版本日期 | P1 |
| F-009-33 | 提供意见反馈入口（跳转邮箱或表单） | P2 |
| F-009-34 | 提供在线客服入口 | P2 |

---

## 3. 界面设计

### 3.1 资料页面

**布局：**
```
┌─────────────────────────────────────┐
│  ← 个人资料                         │
├─────────────────────────────────────┤
│                                     │
│         ┌─────────────┐             │
│         │             │             │
│         │   👤头像   │             │
│         │   点击更换 │             │
│         │             │             │
│         └─────────────┘             │
│                                     │
│         昵称                        │
│       [用户昵称]          编辑      │
│                                     │
│       138****8000                  │
│                                     │
│       注册时间: 2026年1月15日       │
│                                     │
├─────────────────────────────────────┤
│  学习目标                          │
│  每日 15 个单词          →         │
├─────────────────────────────────────┤
│  学习提醒            [开启]         │
│  每天 20:00 提醒                   │
├─────────────────────────────────────┤
│  账号与安全                         │
│  退出登录                          │
│  注销账户                          │
├─────────────────────────────────────┤
│  关于与帮助                         │
│  用户协议              →            │
│  隐私政策              →            │
│  版本 v1.0.0                       │
└─────────────────────────────────────┘
```

### 3.2 设置页面

**布局：**
```
┌─────────────────────────────────────┐
│  ← 设置                           │
├─────────────────────────────────────┤
│  外观                             │
│  主题                    [深色]    │
│  音效                    [开启]    │
│  振动反馈                [关闭]    │
├─────────────────────────────────────┤
│  学习                             │
│  发音评估灵敏度         [中]       │
│  清除本地缓存            12.5MB   │
├─────────────────────────────────────┤
│  数据                             │
│  手动同步数据                     │
│  导出学习数据                     │
├─────────────────────────────────────┤
│  应用信息                          │
│  用户协议              →           │
│  隐私政策              →           │
│  意见反馈              →           │
│  版本 1.0.0 (Build 1)            │
└─────────────────────────────────────┘
```

### 3.3 编辑资料弹窗

**编辑昵称：**
```
┌─────────────────────────────────────┐
│  修改昵称                    ✕      │
├─────────────────────────────────────┤
│                                     │
│  昵称                             │
│  ┌───────────────────────────────┐ │
│  │ 我的昵称                      │ │
│  └───────────────────────────────┘ │
│                                     │
│  2-20个字符，支持中英文、数字     │
│                                     │
│        [取消]      [保存]          │
│                                     │
└─────────────────────────────────────┘
```

---

## 4. 接口设计

### 4.1 获取用户资料

```
GET /api/user/profile
Authorization: Bearer <token>

Response:
{
    "success": true,
    "data": {
        "id": 1001,
        "phone": "13800138000",
        "nickname": "学习达人",
        "avatar_url": "https://cdn.example.com/avatar/1001.jpg",
        "created_at": "2026-01-15T10:00:00Z",
        "login_at": "2026-03-19T14:00:00Z"
    }
}
```

### 4.2 更新用户资料

```
PUT /api/user/profile
Authorization: Bearer <token>
Content-Type: application/json

{
    "nickname": "新的昵称"
    // 或
    "avatar": "base64编码的图片"
}

Response:
{
    "success": true,
    "data": {
        "nickname": "新的昵称",
        "avatar_url": "https://cdn.example.com/avatar/1001_v2.jpg"
    }
}
```

### 4.3 获取用户设置

```
GET /api/user/settings
Authorization: Bearer <token>

Response:
{
    "success": true,
    "data": {
        "daily_goal": 15,
        "reminder_enabled": true,
        "reminder_time": "20:00",
        "reminder_days": [1,2,3,4,5,6,7],  // 1=周一
        "sensitivity": "medium",  // high, medium, low
        "theme": "dark",  // light, dark, system
        "sound_enabled": true,
        "vibration_enabled": false,
        "language": "zh-CN"
    }
}
```

### 4.4 更新用户设置

```
PUT /api/user/settings
Authorization: Bearer <token>
Content-Type: application/json

{
    "daily_goal": 20,
    "reminder_enabled": true,
    "reminder_time": "08:00",
    "sensitivity": "high",
    "theme": "system"
}

Response:
{
    "success": true
}
```

### 4.5 退出登录

```
POST /api/auth/logout
Authorization: Bearer <token>

Response:
{
    "success": true,
    "message": "已退出登录"
}
```

### 4.6 导出数据

```
GET /api/user/export
Authorization: Bearer <token>

Response: 文件下载 (application/json)
```

---

## 5. 数据模型

### 5.1 用户扩展信息表

```sql
CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY,
    nickname VARCHAR(50),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 5.2 用户设置表

```sql
CREATE TABLE user_settings (
    user_id BIGINT PRIMARY KEY,
    daily_goal INT DEFAULT 10,
    reminder_enabled BOOLEAN DEFAULT FALSE,
    reminder_time VARCHAR(10) DEFAULT "20:00",
    reminder_days JSON DEFAULT '[1,2,3,4,5,6,7]',
    sensitivity VARCHAR(10) DEFAULT 'medium',
    theme VARCHAR(10) DEFAULT 'system',
    sound_enabled BOOLEAN DEFAULT TRUE,
    vibration_enabled BOOLEAN DEFAULT TRUE,
    language VARCHAR(10) DEFAULT 'zh-CN',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 5.3 本地设置（SharedPreferences）

```java
// 本地缓存的设置（未登录时也可使用）
- theme: String  // "light", "dark", "system"
- sound_enabled: Boolean
- vibration_enabled: Boolean
- first_launch: Boolean  // 是否首次启动
- onboarding_completed: Boolean  // 引导页是否完成
- cached_phonetics: Map<String, String>  // 音标缓存
```

---

## 6. 验收标准

- [ ] 资料页面正确显示用户信息
- [ ] 可成功修改昵称
- [ ] 可成功更换头像
- [ ] 每日学习目标设置生效
- [ ] 学习提醒设置生效
- [ ] 主题切换生效
- [ ] 缓存清除功能正常
- [ ] 退出登录成功并清除Token
- [ ] 退出登录前有二次确认
- [ ] 用户协议和隐私政策链接可用
