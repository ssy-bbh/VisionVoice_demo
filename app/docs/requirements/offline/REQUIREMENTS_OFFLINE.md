# VisionVoice 离线模式系统需求规格说明书

**项目名称：** VisionVoice - AR 英语学习助手  
**功能编号：** F-010  
**版本：** 1.0  
**编制日期：** 2026-03-19

---

## 1. 功能概述

### 1.1 功能描述

离线模式系统确保 VisionVoice 核心功能在无网络连接情况下仍可正常使用，提升应用可用性和用户体验。

### 1.2 业务背景

- 用户可能在无网络的场景下使用应用（如地铁、电梯、偏远地区）
- 提升应用可靠性，减少网络依赖
- 为游客模式提供基础功能保障

---

## 2. 功能需求

### 2.1 功能可用性

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-010-01 | 物体识别功能在离线状态下可用 | P0 |
| F-010-02 | 照片识别功能在离线状态下可用 | P0 |
| F-010-03 | 发音练习功能在离线状态下可用 | P0 |
| F-010-04 | TTS 语音合成在离线状态下可用 | P0 |
| F-010-05 | 收藏集功能在离线状态下可用 | P0 |
| F-010-06 | 本地数据存储在离线状态下可用 | P0 |

### 2.2 网络状态检测

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-010-07 | 实时检测网络连接状态 | P0 |
| F-010-08 | 网络状态变化时显示提示Banner | P1 |
| F-010-09 | 离线状态下显示"离线模式"标识 | P1 |

### 2.3 数据同步

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-010-10 | 离线时收藏的数据标记为"待同步" | P0 |
| F-010-11 | 网络恢复后自动同步待同步数据 | P0 |
| F-010-12 | 同步冲突时以云端数据为准 | P1 |
| F-010-13 | 同步失败时显示重试提示 | P1 |
| F-010-14 | 手动触发数据同步入口 | P2 |

### 2.4 离线资源管理

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-010-15 | 预置基础离线资源（识别模型、音标数据） | P0 |
| F-010-16 | 首次联网时自动下载完整离线资源 | P1 |
| F-010-17 | 显示离线资源下载进度 | P1 |
| F-010-18 | 支持手动检查离线资源更新 | P2 |
| F-010-19 | 显示离线资源占用空间 | P1 |

### 2.5 受限功能

| 需求ID | 需求描述 | 优先级 |
|--------|----------|--------|
| F-010-20 | 登录/注册功能需要网络 | P0 |
| F-010-21 | 云端数据同步需要网络 | P0 |
| F-010-22 | 某些在线API功能不可用时给出提示 | P1 |

---

## 3. 离线功能详情

### 3.1 物体识别（离线）

| 功能 | 离线可用 | 说明 |
|------|----------|------|
| 实时识别 | ✅ | 使用本地 TFLite 模型 |
| 边界框显示 | ✅ | 本地渲染 |
| 词汇显示 | ✅ | 本地词汇表 |
| 词汇发音 | ✅ | 使用本地 TTS |

### 3.2 发音练习（离线）

| 功能 | 离线可用 | 说明 |
|------|----------|------|
| 录音 | ✅ | 本地 MediaRecorder |
| 评分 | ✅ | 使用本地 ONNX 模型 |
| 音标显示 | ✅ | 本地缓存数据 |
| TTS 朗读 | ✅ | 本地 TTS 引擎 |

### 3.3 收藏集（离线）

| 功能 | 离线可用 | 说明 |
|------|----------|------|
| 查看收藏 | ✅ | 本地 Room 数据库 |
| 添加收藏 | ✅ | 本地存储，标记待同步 |
| 删除收藏 | ✅ | 本地存储，标记待同步 |
| 分类管理 | ✅ | 本地数据库 |
| 搜索收藏 | ✅ | 本地数据库查询 |

---

## 4. 界面设计

### 4.1 离线提示Banner

**位置：** 顶部Banner

**样式：**
```
┌─────────────────────────────────────┐
│ 📡 当前处于离线模式，功能可能受限  │
└─────────────────────────────────────┘
```

### 4.2 同步状态

**位置：** 设置页或个人中心

**样式：**
```
┌─────────────────────────────────────┐
│ 数据同步                                    │
│ 同步状态: 已同步 ✓                         │
│ 待同步: 3 条                              │
│ 上次同步: 10分钟前                        │
│                        [立即同步]          │
├─────────────────────────────────────┤
│ 离线资源                                    │
│ 模型版本: v1.0.2                          │
│ 资源大小: 385MB                          │
│                        [检查更新]          │
└─────────────────────────────────────┘
```

---

## 5. 技术实现

### 5.1 网络状态监听

```java
public class NetworkMonitor {
    
    private ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;
    
    public void startMonitoring(Context context) {
        connectivityManager = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        networkCallback = new NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // 网络可用，触发同步
                SyncManager.getInstance().syncPendingData();
            }
            
            @Override
            public void onLost(Network network) {
                // 网络断开
                EventBus.post(new NetworkStatusEvent(false));
            }
        };
        
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }
}
```

### 5.2 数据同步策略

```java
public class SyncManager {
    
    // 同步优先级
    // 1. 学习记录（高优先级）
    // 2. 收藏数据（中优先级）
    // 3. 用户设置（低优先级）
    
    public void syncPendingData() {
        // 1. 同步收藏数据
        syncCollections();
        
        // 2. 同步学习记录
        syncLearningRecords();
        
        // 3. 同步成就进度
        syncAchievements();
    }
}
```

### 5.3 离线资源管理

```java
public class OfflineResourceManager {
    
    // 资源清单
    private static final String[] OFFLINE_RESOURCES = {
        "yolov8n.tflite",      // 物体识别模型
        "labels.txt",           // 识别标签
        "phonetics.db",         // 音标数据库
        "wav2vec2.onnx"        // 语音模型(较大，可能不预置)
    };
    
    public void downloadResources(ProgressListener listener) {
        // 从服务器下载最新离线资源
    }
    
    public long getResourceSize() {
        // 计算离线资源总大小
    }
}
```

---

## 6. 数据同步冲突处理

### 6.1 冲突场景

| 场景 | 处理策略 |
|------|----------|
| 离线新增收藏，云端已删除 | 保留本地（重新同步） |
| 离线修改，云端也修改 | 以云端为准 |
| 离线删除，云端未变 | 执行删除同步 |

### 6.2 冲突检测

```java
public class SyncConflictResolver {
    
    public void resolve(Collection local, Collection remote) {
        if (local.getUpdatedAt().after(remote.getUpdatedAt())) {
            // 本地更新，使用本地
            return local;
        } else {
            // 云端更新，使用云端
            return remote;
        }
    }
}
```

---

## 7. 数据模型

### 7.1 同步状态表

```sql
CREATE TABLE sync_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    table_name VARCHAR(50) NOT NULL,  -- collections, learning_records, etc.
    record_id BIGINT NOT NULL,
    operation VARCHAR(20) NOT NULL,  -- INSERT, UPDATE, DELETE
    data JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    synced_at TIMESTAMP NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_sync_queue_synced ON sync_queue(synced_at);
```

### 7.2 资源版本表

```sql
CREATE TABLE offline_resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_name VARCHAR(100) NOT NULL,
    version VARCHAR(20) NOT NULL,
    file_size BIGINT,
    downloaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    checksum VARCHAR(64)
);
```

---

## 8. 验收标准

- [ ] 离线状态下物体识别功能正常
- [ ] 离线状态下发音练习功能正常
- [ ] 离线状态下收藏集功能正常
- [ ] 网络恢复后自动同步待同步数据
- [ ] 同步失败时提示用户
- [ ] 离线模式Banner正确显示
- [ ] 显示离线资源占用大小
- [ ] 登录/注册功能在离线时不可用
- [ ] 数据同步冲突以云端为准
