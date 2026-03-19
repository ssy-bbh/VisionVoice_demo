# VisionVoice 用户登录与认证系统需求规格说明书

**项目名称：** VisionVoice - AR 英语学习助手  
**功能编号：** F-005  
**版本：** 1.0  
**编制日期：** 2026-03-19

---

## 1. 功能概述

### 1.1 功能描述

用户登录与认证系统是 VisionVoice 应用的基础支撑模块，负责用户身份的注册、登录、验证和会话管理。该系统支持手机号验证码登录、游客模式两种认证方式，并实现用户数据的云端同步。

### 1.2 业务背景

- 现有应用完全运行在本地，无用户系统
- 学习数据存储在本地，无法跨设备同步
- 需要支持离线模式下的基础功能体验

---

## 2. 用户角色与使用场景

### 2.1 角色定义

| 角色 | 描述 | 权限 |
|------|------|------|
| 游客 | 未注册/未登录用户 | 使用基础功能，数据仅本地存储 |
| 注册用户 | 完成手机号验证的用户 | 所有功能，数据云端同步 |

### 2.2 使用场景

**场景1：首次使用**
```
1. 用户打开应用
2. 系统检测无登录状态，显示登录引导页
3. 用户选择"游客模式"直接进入 → 跳过登录
4. 用户选择"手机号登录" → 进入登录流程
```

**场景2：手机号登录**
```
1. 用户输入手机号
2. 点击"获取验证码"
3. 系统发送验证码（60秒倒计时防刷）
4. 用户输入4位验证码
5. 系统验证通过 → 创建/更新用户记录 → 登录成功
```

**场景3：自动登录**
```
1. 用户打开应用
2. 检测本地存有有效token
3. 自动使用token登录 → 登录成功
```

**场景4： token失效**
```
1. 使用本地token请求API
2. 服务端返回401未授权
3. 跳转至登录页面
4. 用户重新登录
```

---

## 3. 功能需求

### 3.1 游客模式

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-005-01 | 用户可选择不登录直接使用应用 | P0 |
| F-005-02 | 游客模式下所有功能可用（除数据同步） | P0 |
| F-005-03 | 游客模式下产生的数据存储在本地数据库 | P0 |
| F-005-04 | 游客可随时通过引导注册/登录将本地数据绑定至云端 | P1 |

### 3.2 手机号登录

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-005-05 | 支持中国大陆手机号（11位）格式校验 | P0 |
| F-005-06 | 点击发送验证码后，60秒内不可重复发送 | P0 |
| F-005-07 | 验证码有效期5分钟，超时需重新获取 | P0 |
| F-005-08 | 验证码错误3次后，当次验证码失效，需重新获取 | P1 |
| F-005-09 | 验证码输入错误时，提示"验证码错误" | P0 |
| F-005-10 | 新手机号首次登录自动创建用户账号 | P0 |
| F-005-11 | 登录成功后返回JWT token，保存至本地 | P0 |

### 3.3 Token管理

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-005-12 | Token有效期30天，过期需重新登录 | P0 |
| F-005-13 | 支持Token刷新机制（Refresh Token） | P1 |
| F-005-14 | 退出登录时清除本地Token | P0 |

### 3.4 数据同步

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-005-15 | 登录后自动同步本地数据至云端 | P1 |
| F-005-16 | 支持手动触发数据同步 | P2 |
| F-005-17 | 同步冲突时以云端数据为准（可配置） | P2 |

### 3.5 账户安全

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-005-18 | 同一账号同时只能在一个设备登录（新设备登录踢掉旧设备） | P1 |
| F-005-19 | 敏感操作（如删除数据）需二次确认 | P2 |

---

## 4. 界面设计

### 4.1 登录引导页

**布局：**
- 应用Logo居中显示
- "VisionVoice" 文字标题
- "手机号登录" 按钮（主要）
- "游客模式" 按钮（次要）
- 底部"用户协议"和"隐私政策"链接

**交互：**
- 点击"游客模式" → 直接跳转首页
- 点击"手机号登录" → 跳转登录页

### 4.2 登录页

**布局：**
- 手机号输入框 + 国家代码（默认+86）
- "获取验证码" 按钮
- 验证码输入框（4位数字，每位一个输入框）
- "登录" 按钮
- "返回" 导航

**状态：**
- 输入手机号后，"获取验证码"按钮可点击
- 发送验证码后，按钮显示60秒倒计时
- 4位验证码输入完成后，"登录"按钮高亮

### 4.3 首页登录提示

**位置：** 首页顶部Banner

**文案：** "登录后同步学习数据，更换设备不丢失"

**交互：** 点击跳转登录页

---

## 5. 接口设计

### 5.1 发送验证码

```
POST /api/auth/send_code
Content-Type: application/json

请求参数：
{
    "phone": "13800138000"
}

响应：
{
    "success": true,
    "message": "验证码已发送",
    "expires_in": 300  // 有效期秒数
}

错误响应：
{
    "success": false,
    "error_code": "RATE_LIMIT",
    "message": "发送过于频繁，请稍后重试"
}
```

### 5.2 验证码登录

```
POST /api/auth/login
Content-Type: application/json

请求参数：
{
    "phone": "13800138000",
    "code": "1234"
}

响应：
{
    "success": true,
    "data": {
        "user": {
            "id": 1001,
            "phone": "13800138000",
            "nickname": null,
            "avatar_url": null,
            "created_at": "2026-03-19T10:00:00Z"
        },
        "token": "eyJhbGciOiJIUzI1NiIs...",
        "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
        "expires_in": 2592000
    }
}
```

### 5.3 刷新Token

```
POST /api/auth/refresh
Content-Type: application/json

请求参数：
{
    "refresh_token": "eyJhbGciOiJIUzI1NiIs..."
}

响应：
{
    "success": true,
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIs...",
        "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
        "expires_in": 2592000
    }
}
```

### 5.4 退出登录

```
POST /api/auth/logout
Authorization: Bearer <token>

响应：
{
    "success": true,
    "message": "已退出登录"
}
```

---

## 6. 数据模型

### 6.1 用户表 (users)

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) UNIQUE NOT NULL,
    nickname VARCHAR(50),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    status TINYINT DEFAULT 1 COMMENT '1:正常 0:禁用'
);

CREATE INDEX idx_users_phone ON users(phone);
```

### 6.2 Token表 (user_tokens)

```sql
CREATE TABLE user_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    refresh_token VARCHAR(500),
    device_info VARCHAR(200),
    ip_address VARCHAR(50),
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_tokens_token ON user_tokens(token);
CREATE INDEX idx_tokens_user ON user_tokens(user_id);
```

---

## 7. 错误码定义

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| INVALID_PHONE | 手机号格式错误 | 400 |
| INVALID_CODE | 验证码错误 | 400 |
| CODE_EXPIRED | 验证码已过期 | 400 |
| CODE_RATE_LIMIT | 发送过于频繁 | 429 |
| TOKEN_EXPIRED | Token已过期 | 401 |
| TOKEN_INVALID | Token无效 | 401 |
| USER_DISABLED | 用户已被禁用 | 403 |

---

## 8. 安全考虑

1. **验证码存储：** 验证码哈希存储，防止泄露
2. **Token安全：** Token存储在Android Keystore或EncryptedSharedPreferences
3. **日志脱敏：** 日志中不记录手机号、验证码等敏感信息
4. **频率限制：** 单IP单手机号每分钟最多1次请求

---

## 9. 性能要求

| 指标 | 要求 |
|------|------|
| 发送验证码接口响应时间 | ≤500ms |
| 登录接口响应时间 | ≤1s |
| 验证码发送成功率 | ≥99.9% |

---

## 10. 验收标准

- [ ] 游客模式可正常进入应用并使用所有基础功能
- [ ] 手机号格式校验正确，无效号码提示友好
- [ ] 验证码60秒倒计时正常
- [ ] 验证码5分钟有效期内可登录
- [ ] 首次登录自动创建账号
- [ ] Token本地存储正确
- [ ] Token过期后自动跳转登录页
- [ ] 退出登录清除本地Token
