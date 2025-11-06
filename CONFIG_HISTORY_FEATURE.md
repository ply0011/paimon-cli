# 配置历史功能说明

## 功能概述

新增了配置历史管理功能，可以将用户输入的连接配置保存在本地文件中，方便下次启动时快速选择使用。

## 主要特性

1. **自动保存配置**：每次成功连接后，自动将配置保存到本地文件
2. **历史配置选择**：启动时可以选择使用已保存的配置，或输入新配置
3. **最多保留3个配置**：自动保留最新的3个配置，删除更旧的配置
4. **配置文件路径显示**：每次启动时打印配置文件的完整路径
5. **支持Local和S3配置**：两种存储类型的配置都可以保存和恢复

## 配置文件位置

配置文件保存在用户主目录下：
```
~/.paimon-cli/config-history.txt
```

在macOS/Linux系统中，完整路径通常是：
```
/Users/<username>/.paimon-cli/config-history.txt
```

## 使用示例

### 第一次启动（无历史配置）

```
========================================
    Welcome to Paimon CLI
========================================

Configuration file: /Users/samuel/.paimon-cli/config-history.txt


Please select storage type:
  1. Local (Local File System)
  2. S3
Enter option (1 or 2): 1
Enter local storage path (e.g., /tmp/paimon): /tmp/paimon-warehouse

Connecting to storage...
Storage connected successfully!
Configuration: StorageConfig{type=LOCAL, warehouse='/tmp/paimon-warehouse', options=0 entries}
```

### 第二次启动（有历史配置）

```
========================================
    Welcome to Paimon CLI
========================================

Configuration file: /Users/samuel/.paimon-cli/config-history.txt

Recent configurations:
  1. Local: /tmp/paimon-warehouse
  2. Enter new configuration
Select option (1-2): 1
Using configuration: StorageConfig{type=LOCAL, warehouse='/tmp/paimon-warehouse', options=0 entries}

Connecting to storage...
Storage connected successfully!
```

### 多个历史配置

```
Configuration file: /Users/samuel/.paimon-cli/config-history.txt

Recent configurations:
  1. Local: /tmp/paimon-test3
  2. Local: /tmp/paimon-test2
  3. Local: /tmp/paimon-test1
  4. Enter new configuration
Select option (1-4): 
```

## 配置文件格式

配置文件采用简单的文本格式，示例：

```
# Paimon CLI Configuration History
# This file stores the last 3 configurations
# Auto-generated, do not edit manually

type=LOCAL
warehouse=/tmp/paimon-test3
---
type=LOCAL
warehouse=/tmp/paimon-test2
---
type=S3
warehouse=s3a://my-bucket/paimon-data
option.fs.s3a.access.key=AKIAIOSFODNN7EXAMPLE
option.fs.s3a.secret.key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
option.fs.s3a.endpoint=https://s3.amazonaws.com
option.fs.s3a.endpoint.region=us-east-1
option.fs.s3a.path.style.access=true
option.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
```

## 实现细节

### 新增类

1. **ConfigHistoryManager** (`src/main/java/io/tapdata/paimon/cli/config/ConfigHistoryManager.java`)
   - 负责配置的保存、加载和管理
   - 实现配置文件的读写
   - 处理配置去重和数量限制

### 修改的类

1. **PaimonCLI** (`src/main/java/io/tapdata/paimon/cli/PaimonCLI.java`)
   - 添加了 `ConfigHistoryManager` 字段
   - 修改了 `initializeStorage()` 方法，增加历史配置选择逻辑
   - 新增了 `promptNewConfiguration()` 方法，用于输入新配置

## 技术要点

1. **配置去重**：相同的配置不会重复保存，会移到列表最前面
2. **只保存成功的配置**：只有在成功连接到存储后才保存配置
3. **自动创建目录**：如果配置目录不存在，会自动创建
4. **错误处理**：配置文件读写失败不会影响程序正常运行

## 测试验证

已通过以下测试：

1. ✅ 首次启动无历史配置，输入新配置后保存
2. ✅ 第二次启动显示历史配置选项
3. ✅ 可以选择使用历史配置
4. ✅ 可以选择输入新配置
5. ✅ 最多保留3个配置（第4个配置会删除最旧的）
6. ✅ 配置文件路径在启动时正确显示
7. ✅ Local配置正确保存和加载
8. ✅ S3配置格式正确（包含所有选项）

## 注意事项

1. 配置文件包含敏感信息（如S3的Access Key和Secret Key），请妥善保管
2. 配置文件是自动生成的，不建议手动编辑
3. 如需清空历史配置，可以直接删除配置文件：`rm ~/.paimon-cli/config-history.txt`

