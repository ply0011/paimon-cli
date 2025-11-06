# Paimon CLI 工具

一个基于 Paimon Java API 的命令行交互工具，支持查询 Paimon 表的元数据和数据。

## 功能特性

- ✅ 支持 Local 和 S3 两种存储类型
- ✅ 查看数据库列表
- ✅ 查看表列表
- ✅ 切换当前数据库（use database）
- ✅ 查看表结构（字段、类型、主键、分区键等）
- ✅ 查询表总行数
- ✅ 查询表数据（支持 limit、分页查询、条件过滤）
- ✅ 纯 Java API 实现，无需 Flink 或 Spark

## 构建

使用 Maven 构建可执行 JAR：

```bash
mvn clean package
```

构建完成后，会在 `target` 目录下生成 `paimon-cli-1.0-SNAPSHOT.jar` 文件。

## 运行

```bash
java -jar target/paimon-cli-1.0-SNAPSHOT.jar
```

## 使用说明

### 1. 启动和配置存储

启动程序后，首先需要配置存储：

#### Local 存储示例：
```
请选择存储类型:
  1. Local (本地文件系统)
  2. S3
请输入选项 (1 或 2): 1
请输入本地存储路径 (例如: /tmp/paimon): /tmp/paimon-warehouse
```

#### S3 存储示例：
```
请选择存储类型:
  1. Local (本地文件系统)
  2. S3
请输入选项 (1 或 2): 2
请输入 S3 路径 (例如: s3://bucket-name/path): s3://my-bucket/paimon
请输入 Access Key (可选，直接回车跳过): AKIAIOSFODNN7EXAMPLE
请输入 Secret Key (可选，直接回车跳过): wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
请输入 Endpoint (可选，直接回车跳过): https://s3.amazonaws.com
请输入 Region (可选，直接回车跳过): us-east-1
```

### 2. 可用命令

连接成功后，可以使用以下命令：

#### 查看所有数据库
```
paimon> show databases
```

#### 查看指定数据库的所有表
```
# 指定数据库名
paimon> show tables my_database

# 使用当前数据库（需先执行 use 命令）
paimon [my_database]> show tables
```

#### 切换当前数据库
使用 `use` 命令可以设置当前数据库，之后的命令可以省略数据库名：
```
paimon> use my_database
Database changed to: my_database
paimon [my_database]>
```

切换数据库后，命令提示符会显示当前数据库名称。

#### 查看表结构
```
# 使用完整格式
paimon> desc my_database.my_table

# 使用当前数据库（需先执行 use 命令）
paimon [my_database]> desc my_table
```

#### 查询表总行数
```
# 使用完整格式
paimon> count my_database.my_table

# 使用当前数据库（需先执行 use 命令）
paimon [my_database]> count my_table
```

#### 查询表数据（默认显示 10 行）
```
# 使用完整格式
paimon> select my_database.my_table

# 使用当前数据库（需先执行 use 命令）
paimon [my_database]> select my_table
```

#### 查询表数据（指定 limit）
```
# 使用完整格式
paimon> select my_database.my_table 20

# 使用当前数据库（需先执行 use 命令）
paimon [my_database]> select my_table 20
```

#### 查询表数据（分页查询所有数据）
使用 `all` 关键字启用分页模式，每页显示 5 行数据，输入 `it` 继续查看下一页：
```
# 使用完整格式
paimon> select my_database.my_table all

# 使用当前数据库（需先执行 use 命令）
paimon [my_database]> select my_table all
```

#### 查询表数据（使用 filter 过滤）
```
# 使用完整格式
paimon> select my_database.my_table where age>18
paimon> select my_database.my_table 10 where age>18
paimon> select my_database.my_table where age>=18 AND name=Alice
paimon> select my_database.my_table 20 where id!=5

# 使用当前数据库（需先执行 use 命令）
paimon [my_database]> select my_table where age>18
paimon [my_database]> select my_table 10 where age>18
```

#### 查询表数据（分页查询 + 过滤）
```
# 使用完整格式
paimon> select my_database.my_table all where age>18

# 使用当前数据库（需先执行 use 命令）
paimon [my_database]> select my_table all where age>18
```

支持的过滤操作符：
- `=` - 等于
- `!=` - 不等于
- `>` - 大于
- `<` - 小于
- `>=` - 大于等于
- `<=` - 小于等于
- `AND` - 多个条件组合（不区分大小写）

#### 查看帮助
```
paimon> help
```

#### 退出程序
```
paimon> exit
```
或
```
paimon> quit
```

## 示例会话

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

可用命令:
  show databases                              - 显示所有数据库
  show tables [<database>]                    - 显示指定数据库的所有表
                                                (使用当前数据库如果未指定)
  use <database>                              - 设置当前数据库
  desc <database>.<table>                     - 显示表结构
  desc <table>                                - 显示表结构（使用当前数据库）
  count <database>.<table>                    - 查询表的总行数
  count <table>                               - 查询表的总行数（使用当前数据库）
  select <database>.<table> [limit|all] [where <filter>]
                                              - 查询表数据，可选 limit 和 filter 参数
  select <table> [limit|all] [where <filter>] - 查询表数据（使用当前数据库）
                                                使用 'all' 启用分页模式（每页 5 行）
  help                                        - 显示帮助信息
  exit/quit                                   - 退出程序

查询示例:
  use default                                 - 设置当前数据库为 'default'
  show tables                                 - 显示当前数据库的表
  select users 10                             - 显示前 10 行（使用当前数据库）
  select default.users 10                     - 显示前 10 行

paimon> show databases

数据库列表:
====================
  - default
总计: 1 个数据库

paimon> use default
Database changed to: default

paimon [default]> show tables

数据库 'default' 的表列表:
====================
  - users
  - orders
总计: 2 个表

paimon [default]> desc users

表: default.users
====================
字段信息:
字段名                           类型                            可空
--------------------------------------------------------------------------------
id                           BIGINT                         NO
name                         STRING                         YES
age                          INT                            YES

主键: id

paimon [default]> count users

表 default.users 的总行数: 1000

paimon [default]> select users 5

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

paimon [default]> select users where age>30

Applied filter: age>30

表: default.users
====================
id                   | name                 | age                  |
---------------------+-+---------------------+-+---------------------+-+
3                    | Charlie              | 35                   |
5                    | Eve                  | 32                   |

显示 2 行数据

paimon [default]> select users all

表: default.users
====================
id                   | name                 | age                  |
---------------------+-+---------------------+-+---------------------+-+
1                    | Alice                | 25                   |
2                    | Bob                  | 30                   |
3                    | Charlie              | 35                   |
4                    | David                | 28                   |
5                    | Eve                  | 32                   |

--- Page complete (5 rows) ---
Type 'it' to continue, or press Enter to stop: it

6                    | Frank                | 27                   |
7                    | Grace                | 29                   |
8                    | Henry                | 31                   |
9                    | Ivy                  | 26                   |
10                   | Jack                 | 33                   |

--- Page complete (5 rows) ---
Type 'it' to continue, or press Enter to stop:

Total displayed: 10 row(s)

paimon> exit
再见!
```

## 技术栈

- Java 17
- Apache Paimon 1.2.0
- Hadoop 3.3.6 (用于文件系统支持)
- AWS SDK 1.12.367 (用于 S3 支持)
- JLine 3.25.0 (用于命令行交互)
- Maven

## 注意事项

1. 确保 Java 17 或更高版本已安装
2. 对于 S3 存储，需要正确配置访问凭证
3. 对于本地存储，确保指定的路径存在且有读写权限
4. 该工具直接使用 Paimon Java API，不依赖 Flink 或 Spark 运行时

## 许可证

本项目采用 Apache License 2.0 许可证。

