# Paimon CLI 项目总结

## 项目概述

Paimon CLI 是一个基于 Apache Paimon Java API 的命令行交互工具，提供了查询 Paimon 表元数据和数据的能力。该工具完全使用 Paimon 原生 Java API 实现，不依赖 Flink 或 Spark 运行时。

## 实现的功能

### ✅ 已完成功能

1. **存储配置**
   - 支持 Local 本地文件系统
   - 支持 S3 及兼容对象存储（MinIO 等）
   - 交互式配置向导

2. **元数据查询**
   - 查看所有数据库列表
   - 查看指定数据库的表列表
   - 查看表结构（字段、类型、主键、分区键）

3. **数据查询**
   - 查询表总行数
   - 查询表数据（支持 limit）
   - 格式化数据显示

4. **命令行交互**
   - 友好的交互式命令行界面
   - 帮助命令
   - 错误处理和提示

5. **打包部署**
   - 可执行 JAR 包（Fat JAR）
   - 包含所有依赖
   - 无需额外配置即可运行

## 项目结构

```
paimon-cli/
├── pom.xml                                    # Maven 配置文件
├── README.md                                  # 项目说明文档
├── USAGE.md                                   # 使用指南
├── PROJECT_SUMMARY.md                         # 项目总结
└── src/main/java/io/tapdata/paimon/cli/
    ├── PaimonCLI.java                        # 主程序入口
    ├── config/
    │   └── StorageConfig.java                # 存储配置类
    ├── catalog/
    │   └── CatalogManager.java               # Catalog 管理器
    └── service/
        ├── MetadataService.java              # 元数据查询服务
        └── DataQueryService.java             # 数据查询服务
```

## 核心类说明

### 1. PaimonCLI.java
- **职责**: 主程序入口，命令行交互循环
- **功能**:
  - 初始化存储配置
  - 处理用户命令输入
  - 调用相应的服务方法
  - 资源清理

### 2. StorageConfig.java
- **职责**: 存储配置管理
- **功能**:
  - 支持 Local 和 S3 两种存储类型
  - 提供工厂方法创建配置
  - 管理存储相关的选项参数

### 3. CatalogManager.java
- **职责**: Paimon Catalog 管理
- **功能**:
  - 创建和管理 Paimon Catalog
  - 提供数据库和表的基本操作
  - 封装 Catalog API

### 4. MetadataService.java
- **职责**: 元数据查询服务
- **功能**:
  - 显示数据库列表
  - 显示表列表
  - 显示表结构信息

### 5. DataQueryService.java
- **职责**: 数据查询服务
- **功能**:
  - 查询表总行数
  - 查询表数据
  - 格式化数据输出

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Apache Paimon | 0.9.0 | 核心 API |
| Hadoop | 3.3.6 | 文件系统支持 |
| AWS SDK | 1.12.367 | S3 支持 |
| Commons CLI | 1.5.0 | 命令行解析 |
| SLF4J | 2.0.9 | 日志框架 |
| Maven | 3.x | 构建工具 |

## 关键技术点

### 1. Paimon API 使用

```java
// 创建 Catalog
Options options = new Options();
options.set("warehouse", warehousePath);
CatalogContext context = CatalogContext.create(options);
Catalog catalog = CatalogFactory.createCatalog(context);

// 获取表
Identifier identifier = Identifier.create(database, tableName);
Table table = catalog.getTable(identifier);

// 读取数据
ReadBuilder readBuilder = table.newReadBuilder();
List<Split> splits = readBuilder.newScan().plan().splits();
TableRead tableRead = readBuilder.newRead();
```

### 2. 数据读取

使用 RecordReader 和 RecordIterator 读取数据：

```java
try (RecordReader<InternalRow> reader = tableRead.createReader(split)) {
    RecordReader.RecordIterator<InternalRow> iterator;
    while ((iterator = reader.readBatch()) != null) {
        InternalRow row;
        while ((row = iterator.next()) != null) {
            // 处理数据行
        }
        iterator.releaseBatch();
    }
}
```

### 3. Maven Shade Plugin

使用 Maven Shade Plugin 打包成可执行的 Fat JAR：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.0</version>
    <configuration>
        <transformers>
            <transformer implementation="...ManifestResourceTransformer">
                <mainClass>io.tapdata.paimon.cli.PaimonCLI</mainClass>
            </transformer>
        </transformers>
    </configuration>
</plugin>
```

## 构建和运行

### 构建

```bash
mvn clean package
```

生成的 JAR 文件：
- 位置: `target/paimon-cli-1.0-SNAPSHOT.jar`
- 大小: 约 364MB
- 类型: 可执行 Fat JAR

### 运行

```bash
java -jar target/paimon-cli-1.0-SNAPSHOT.jar
```

## 使用示例

### 1. Local 存储

```bash
$ java -jar target/paimon-cli-1.0-SNAPSHOT.jar
请选择存储类型:
  1. Local (本地文件系统)
  2. S3
请输入选项 (1 或 2): 1
请输入本地存储路径: /tmp/paimon-warehouse

paimon> show databases
paimon> show tables default
paimon> desc default.users
paimon> count default.users
paimon> select default.users 10
paimon> exit
```

### 2. S3 存储

```bash
$ java -jar target/paimon-cli-1.0-SNAPSHOT.jar
请选择存储类型:
  1. Local (本地文件系统)
  2. S3
请输入选项 (1 或 2): 2
请输入 S3 路径: s3://my-bucket/paimon
请输入 Access Key: AKIAIOSFODNN7EXAMPLE
请输入 Secret Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
请输入 Endpoint: https://s3.amazonaws.com
请输入 Region: us-east-1

paimon> show databases
...
```

## 特点和优势

### ✅ 优点

1. **纯 Java API**: 不依赖 Flink 或 Spark，轻量级
2. **交互式**: 友好的命令行交互界面
3. **易部署**: 单个 JAR 文件，包含所有依赖
4. **多存储支持**: 支持 Local 和 S3
5. **易扩展**: 清晰的架构，易于添加新功能

### ⚠️ 限制

1. **只读**: 当前版本只支持读取操作
2. **性能**: count 操作需要全表扫描
3. **数据类型**: 复杂类型显示有限
4. **功能**: 不支持 SQL 查询

## 未来改进方向

### 短期改进

1. **性能优化**
   - 使用表统计信息获取行数
   - 支持并行读取
   - 添加缓存机制

2. **功能增强**
   - 支持数据导出（CSV、JSON）
   - 支持过滤条件
   - 支持排序

3. **用户体验**
   - 命令历史记录
   - 自动补全
   - 更好的错误提示

### 长期改进

1. **SQL 支持**
   - 集成 SQL 解析器
   - 支持复杂查询

2. **更多存储**
   - HDFS 支持
   - OSS 支持
   - Azure Blob 支持

3. **写入操作**
   - 数据导入
   - 表创建
   - 数据更新/删除

4. **监控和管理**
   - 表统计信息
   - 快照管理
   - 压缩管理

## 测试建议

### 1. 创建测试数据

使用 Flink SQL 创建测试表：

```sql
CREATE CATALOG paimon_catalog WITH (
  'type' = 'paimon',
  'warehouse' = 'file:///tmp/paimon-warehouse'
);

USE CATALOG paimon_catalog;

CREATE TABLE users (
  id BIGINT PRIMARY KEY NOT ENFORCED,
  name STRING,
  age INT
);

INSERT INTO users VALUES 
  (1, 'Alice', 25),
  (2, 'Bob', 30),
  (3, 'Charlie', 35);
```

### 2. 测试 CLI

```bash
java -jar target/paimon-cli-1.0-SNAPSHOT.jar
# 选择 Local 存储
# 输入路径: /tmp/paimon-warehouse
# 执行各种查询命令
```

## 总结

Paimon CLI 是一个功能完整的命令行工具，成功实现了以下目标：

1. ✅ 使用 Paimon Java API（不依赖 Flink/Spark）
2. ✅ 支持 Local 和 S3 存储
3. ✅ 提供元数据查询功能
4. ✅ 提供数据查询功能（支持 limit）
5. ✅ 交互式命令行界面
6. ✅ 打包成可执行 JAR

该工具可以作为 Paimon 数据探索和调试的有用工具，也可以作为学习 Paimon API 的参考实现。

