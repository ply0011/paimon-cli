# Paimon CLI 使用指南

## 快速开始

### 1. 构建项目

```bash
mvn clean package
```

构建成功后，会在 `target` 目录下生成 `paimon-cli-1.0-SNAPSHOT.jar` 文件（约 364MB）。

### 2. 运行 CLI

```bash
java -jar target/paimon-cli-1.0-SNAPSHOT.jar
```

## 配置存储

启动后，首先需要配置存储类型。

### Local 存储配置

```
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
```

### S3 存储配置

```
请选择存储类型:
  1. Local (本地文件系统)
  2. S3
请输入选项 (1 或 2): 2
请输入 S3 路径 (例如: s3://bucket-name/path): s3://my-bucket/paimon-data
请输入 Access Key (可选，直接回车跳过): AKIAIOSFODNN7EXAMPLE
请输入 Secret Key (可选，直接回车跳过): wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
请输入 Endpoint (可选，直接回车跳过): https://s3.amazonaws.com
请输入 Region (可选，直接回车跳过): us-east-1

正在连接到存储...
存储连接成功!
```

**注意**：如果使用环境变量或 IAM 角色配置 AWS 凭证，可以直接跳过 Access Key 和 Secret Key 的输入。

## 命令参考

### 1. 查看帮助

```
paimon> help
```

输出：
```
可用命令:
  show databases                    - 显示所有数据库
  show tables <database>            - 显示指定数据库的所有表
  desc <database>.<table>           - 显示表结构
  count <database>.<table>          - 查询表的总行数
  select <database>.<table> [limit] - 查询表数据，可选 limit 参数
  help                              - 显示帮助信息
  exit/quit                         - 退出程序
```

### 2. 查看所有数据库

```
paimon> show databases
```

示例输出：
```
数据库列表:
====================
  - default
  - test_db
  - production
总计: 3 个数据库
```

### 3. 查看数据库中的表

```
paimon> show tables default
```

示例输出：
```
数据库 'default' 的表列表:
====================
  - users
  - orders
  - products
总计: 3 个表
```

### 4. 查看表结构

```
paimon> desc default.users
```

示例输出：
```
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
分区键: created_at
```

### 5. 查询表总行数

```
paimon> count default.users
```

示例输出：
```
表 default.users 的总行数: 10000
```

### 6. 查询表数据

#### 使用默认 limit（10 行）

```
paimon> select default.users
```

#### 指定 limit

```
paimon> select default.users 20
```

示例输出：
```
表: default.users
====================
id                   | name                 | email                | age                  | 
---------------------+-+---------------------+-+---------------------+-+---------------------+-+
1                    | Alice                | alice@example.com    | 25                   | 
2                    | Bob                  | bob@example.com      | 30                   | 
3                    | Charlie              | charlie@example.com  | 35                   | 
4                    | David                | david@example.com    | 28                   | 
5                    | Eve                  | eve@example.com      | 32                   | 
6                    | Frank                | frank@example.com    | 29                   | 
7                    | Grace                | grace@example.com    | 27                   | 
8                    | Henry                | henry@example.com    | 31                   | 
9                    | Ivy                  | ivy@example.com      | 26                   | 
10                   | Jack                 | jack@example.com     | 33                   | 

显示 10 行数据
```

### 7. 退出程序

```
paimon> exit
```

或

```
paimon> quit
```

## 完整示例会话

```
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
age                          INT                            YES        

主键: id

paimon> count default.users

表 default.users 的总行数: 100

paimon> select default.users 5

表: default.users
====================
id                   | name                 | age                  | 
---------------------+-+---------------------+-+---------------------+-+
1                    | Alice                | 25                   | 
2                    | Bob                  | 30                   | 
3                    | Charlie              | 35                   | 
4                    | David                | 28                   | 
5                    | Eve                  | 32                   | 

显示 5 行数据

paimon> exit
再见!
```

## 常见问题

### 1. 如何创建测试数据？

Paimon CLI 目前只支持读取操作。要创建测试数据，可以使用 Flink 或 Spark 与 Paimon 集成。

### 2. 支持哪些存储类型？

目前支持：
- **Local**: 本地文件系统
- **S3**: Amazon S3 及兼容的对象存储（如 MinIO）

### 3. 如何配置 S3 兼容存储（如 MinIO）？

在配置 S3 时，输入 MinIO 的 endpoint：

```
请输入 Endpoint (可选，直接回车跳过): http://localhost:9000
```

### 4. 内存不足怎么办？

如果处理大量数据时遇到内存问题，可以增加 JVM 堆内存：

```bash
java -Xmx4g -jar target/paimon-cli-1.0-SNAPSHOT.jar
```

### 5. 如何查看详细日志？

默认使用 slf4j-simple，日志级别为 INFO。如需调整，可以设置系统属性：

```bash
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar target/paimon-cli-1.0-SNAPSHOT.jar
```

## 技术细节

### 架构

```
PaimonCLI (主程序)
    ├── StorageConfig (存储配置)
    ├── CatalogManager (Catalog 管理)
    ├── MetadataService (元数据服务)
    │   ├── showDatabases()
    │   ├── showTables()
    │   └── describeTable()
    └── DataQueryService (数据查询服务)
        ├── countTable()
        └── selectTable()
```

### 依赖

- **Apache Paimon 0.9.0**: 核心 API
- **Hadoop 3.3.6**: 文件系统支持
- **AWS SDK**: S3 支持
- **Commons CLI**: 命令行解析
- **SLF4J**: 日志框架

### 限制

1. **只读操作**: 当前版本只支持读取操作，不支持写入、更新或删除
2. **性能**: 对于大表的 count 操作可能较慢，因为需要扫描所有数据
3. **数据类型**: 某些复杂数据类型（如 ARRAY、MAP、ROW）的显示可能不够友好

## 未来改进

- [ ] 支持 SQL 查询
- [ ] 支持数据导出（CSV、JSON）
- [ ] 支持更多存储类型（HDFS、OSS 等）
- [ ] 性能优化（使用统计信息获取行数）
- [ ] 更好的数据格式化显示
- [ ] 支持分页查询
- [ ] 命令历史记录
- [ ] 自动补全功能

