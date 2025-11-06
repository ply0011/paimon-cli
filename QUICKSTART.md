# Paimon CLI 快速开始

## 5 分钟快速上手

### 步骤 1: 构建项目

```bash
cd paimon-cli
mvn clean package
```

等待构建完成（首次构建需要下载依赖，可能需要几分钟）。

### 步骤 2: 准备测试数据（可选）

如果你还没有 Paimon 数据，可以使用 Flink SQL 快速创建一些测试数据：

```sql
-- 启动 Flink SQL Client
bin/sql-client.sh

-- 创建 Paimon Catalog
CREATE CATALOG paimon_catalog WITH (
  'type' = 'paimon',
  'warehouse' = 'file:///tmp/paimon-warehouse'
);

USE CATALOG paimon_catalog;

-- 创建测试表
CREATE TABLE users (
  id BIGINT PRIMARY KEY NOT ENFORCED,
  name STRING,
  email STRING,
  age INT,
  created_at TIMESTAMP(3)
);

-- 插入测试数据
INSERT INTO users VALUES 
  (1, 'Alice', 'alice@example.com', 25, TIMESTAMP '2024-01-01 10:00:00'),
  (2, 'Bob', 'bob@example.com', 30, TIMESTAMP '2024-01-02 11:00:00'),
  (3, 'Charlie', 'charlie@example.com', 35, TIMESTAMP '2024-01-03 12:00:00'),
  (4, 'David', 'david@example.com', 28, TIMESTAMP '2024-01-04 13:00:00'),
  (5, 'Eve', 'eve@example.com', 32, TIMESTAMP '2024-01-05 14:00:00');
```

### 步骤 3: 运行 CLI

```bash
java -jar target/paimon-cli-1.0-SNAPSHOT.jar
```

### 步骤 4: 配置存储

```
请选择存储类型:
  1. Local (本地文件系统)
  2. S3
请输入选项 (1 或 2): 1
请输入本地存储路径 (例如: /tmp/paimon): /tmp/paimon-warehouse
```

### 步骤 5: 开始查询

```
paimon> show databases
paimon> show tables default
paimon> desc default.users
paimon> count default.users
paimon> select default.users 5
paimon> exit
```

## 完整示例

```bash
$ java -jar target/paimon-cli-1.0-SNAPSHOT.jar
========================================
    欢迎使用 Paimon CLI 工具
========================================

请选择存储类型:
  1. Local (本地文件系统)
  2. S3
请输入选项 (1 或 2): 1
请输入本地存储路径 (例如: /tmp/paimon): /tmp/paimon-warehouse

正在连接到存储...
存储连接成功!
配置信息: StorageConfig{type=LOCAL, warehouse='/tmp/paimon-warehouse', options=0 entries}

可用命令:
  show databases                    - 显示所有数据库
  show tables <database>            - 显示指定数据库的所有表
  desc <database>.<table>           - 显示表结构
  count <database>.<table>          - 查询表的总行数
  select <database>.<table> [limit] - 查询表数据，可选 limit 参数
  help                              - 显示帮助信息
  exit/quit                         - 退出程序

paimon> show databases

数据库列表:
====================
  - default
总计: 1 个数据库

paimon> show tables default

数据库 'default' 的表列表:
====================
  - users
总计: 1 个表

paimon> desc default.users

表: default.users
====================
字段信息:
字段名                           类型                            可空        
--------------------------------------------------------------------------------
id                           BIGINT                         NO         
name                         STRING                         YES        
email                        STRING                         YES        
age                          INT                            YES        
created_at                   TIMESTAMP(3)                   YES        

主键: id

paimon> count default.users

表 default.users 的总行数: 5

paimon> select default.users

表: default.users
====================
id                   | name                 | email                | age                  | created_at           | 
---------------------+-+---------------------+-+---------------------+-+---------------------+-+---------------------+-+
1                    | Alice                | alice@example.com    | 25                   | 2024-01-01 10:00:00  | 
2                    | Bob                  | bob@example.com      | 30                   | 2024-01-02 11:00:00  | 
3                    | Charlie              | charlie@example.com  | 35                   | 2024-01-03 12:00:00  | 
4                    | David                | david@example.com    | 28                   | 2024-01-04 13:00:00  | 
5                    | Eve                  | eve@example.com      | 32                   | 2024-01-05 14:00:00  | 

显示 5 行数据

paimon> exit
再见!
```

## 使用 S3 存储

如果你的 Paimon 数据存储在 S3 上：

```bash
$ java -jar target/paimon-cli-1.0-SNAPSHOT.jar

请选择存储类型:
  1. Local (本地文件系统)
  2. S3
请输入选项 (1 或 2): 2
请输入 S3 路径 (例如: s3://bucket-name/path): s3://my-bucket/paimon-data
请输入 Access Key (可选，直接回车跳过): AKIAIOSFODNN7EXAMPLE
请输入 Secret Key (可选，直接回车跳过): wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
请输入 Endpoint (可选，直接回车跳过): 
请输入 Region (可选，直接回车跳过): us-east-1

正在连接到存储...
存储连接成功!

paimon> show databases
...
```

## 使用 MinIO

如果使用 MinIO 作为对象存储：

```bash
请选择存储类型:
  1. Local (本地文件系统)
  2. S3
请输入选项 (1 或 2): 2
请输入 S3 路径 (例如: s3://bucket-name/path): s3://paimon/warehouse
请输入 Access Key (可选，直接回车跳过): minioadmin
请输入 Secret Key (可选，直接回车跳过): minioadmin
请输入 Endpoint (可选，直接回车跳过): http://localhost:9000
请输入 Region (可选，直接回车跳过): us-east-1
```

## 常用命令速查

| 命令 | 说明 | 示例 |
|------|------|------|
| `show databases` | 显示所有数据库 | `paimon> show databases` |
| `show tables <db>` | 显示数据库中的表 | `paimon> show tables default` |
| `desc <db>.<table>` | 显示表结构 | `paimon> desc default.users` |
| `count <db>.<table>` | 查询表行数 | `paimon> count default.users` |
| `select <db>.<table>` | 查询表数据（默认10行） | `paimon> select default.users` |
| `select <db>.<table> <limit>` | 查询表数据（指定行数） | `paimon> select default.users 20` |
| `help` | 显示帮助信息 | `paimon> help` |
| `exit` 或 `quit` | 退出程序 | `paimon> exit` |

## 故障排除

### 问题 1: 找不到数据库或表

**症状**: 
```
数据库不存在: mydb
```

**解决方案**:
1. 检查 warehouse 路径是否正确
2. 使用 `show databases` 查看可用的数据库
3. 确认数据确实存在于指定路径

### 问题 2: 内存不足

**症状**:
```
java.lang.OutOfMemoryError: Java heap space
```

**解决方案**:
增加 JVM 堆内存：
```bash
java -Xmx4g -jar target/paimon-cli-1.0-SNAPSHOT.jar
```

### 问题 3: S3 连接失败

**症状**:
```
存储初始化失败: Unable to load credentials
```

**解决方案**:
1. 检查 Access Key 和 Secret Key 是否正确
2. 检查网络连接
3. 检查 S3 endpoint 和 region 配置
4. 尝试使用 AWS CLI 测试连接：
   ```bash
   aws s3 ls s3://your-bucket/
   ```

### 问题 4: 表数据显示异常

**症状**:
数据显示为 "N/A" 或格式不正确

**解决方案**:
这可能是由于复杂数据类型（如 ARRAY、MAP、ROW）的显示限制。当前版本对这些类型的支持有限。

## 下一步

- 查看 [README.md](README.md) 了解项目详情
- 查看 [USAGE.md](USAGE.md) 了解详细使用说明
- 查看 [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) 了解技术实现

## 反馈和贡献

如果你发现问题或有改进建议，欢迎提交 Issue 或 Pull Request！

