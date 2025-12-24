# Trino DM Database Connector

达梦数据库（Dameng Database）连接器，允许通过 Trino 查询达梦数据库。

## 简介

达梦数据库是中国自主研发的关系型数据库管理系统。此连接器使 Trino 能够连接和查询达梦数据库。

## 功能特性

- 支持 SELECT、INSERT、UPDATE、DELETE 操作
- 支持聚合函数下推
- 支持 JOIN 下推（除 FULL OUTER JOIN）
- 支持 TOP N 和 LIMIT 下推
- 支持类型映射和转换
- 支持 MERGE 操作

## 数据类型映射

| 达梦类型 | Trino 类型 | 说明 |
|---------|-----------|------|
| BIT | BOOLEAN | 布尔类型 |
| TINYINT, SMALLINT | SMALLINT | 小整数 |
| INTEGER | INTEGER | 整数 |
| BIGINT | BIGINT | 长整数 |
| FLOAT | REAL | 单精度浮点数 |
| DOUBLE, DOUBLE PRECISION | DOUBLE | 双精度浮点数 |
| DECIMAL, NUMERIC | DECIMAL | 小数 |
| CHAR, NCHAR | CHAR | 定长字符串 |
| VARCHAR, VARCHAR2, NVARCHAR | VARCHAR | 变长字符串 |
| CLOB, NCLOB | VARCHAR | 大文本 |
| DATE | DATE | 日期 |
| TIMESTAMP | TIMESTAMP | 时间戳 |
| TIMESTAMPTZ, TIMESTAMP WITH TIME ZONE | TIMESTAMP WITH TIME ZONE | 带时区时间戳 |
| BINARY, VARBINARY, RAW | VARBINARY | 二进制数据 |

## 配置

### 1. 获取达梦 JDBC 驱动

由于达梦 JDBC 驱动不在 Maven 中央仓库，您需要手动安装驱动：

```bash
# 下载达梦 JDBC 驱动 DmJdbcDriver18.jar
# 然后安装到本地 Maven 仓库
mvn install:install-file \
  -Dfile=/path/to/DmJdbcDriver18.jar \
  -DgroupId=com.dameng \
  -DartifactId=DmJdbcDriver18 \
  -Dversion=8.1.3.62 \
  -Dpackaging=jar
```

### 2. 创建连接器配置

在 Trino 的 `etc/catalog` 目录下创建 `dm.properties` 文件：

```properties
connector.name=dm

# 数据库连接 URL
connection-url=jdbc:dm://localhost:5236

# 数据库用户名
connection-user=SYSDBA

# 数据库密码
connection-password=SYSDBA

# 可选：指定默认数据库
dm.database=YOUR_DATABASE

# 可选：是否包含系统表
dm.include-system-tables=false
```

### 3. 配置参数

| 参数 | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| connection-url | String | 必填 | JDBC 连接 URL |
| connection-user | String | 必填 | 数据库用户名 |
| connection-password | String | 必填 | 数据库密码 |
| dm.include-system-tables | Boolean | false | 是否包含系统表 |
| dm.fetch-size | Integer | 自动 | 查询获取大小 |

## 使用示例

### 查询数据

```sql
-- 查询所有表
SHOW TABLES FROM dm;

-- 查询特定表
SELECT * FROM dm.schema_name.table_name LIMIT 10;

-- 聚合查询
SELECT status, COUNT(*) as count 
FROM dm.schema_name.table_name 
GROUP BY status;
```

### 插入数据

```sql
-- 插入单条记录
INSERT INTO dm.schema_name.table_name (id, name, age) 
VALUES (1, 'Alice', 25);

-- 从其他表插入数据
INSERT INTO dm.schema_name.target_table
SELECT * FROM mysql.source_schema.source_table;
```

### 创建表

```sql
-- 在达梦数据库中创建表
CREATE TABLE dm.schema_name.new_table (
    id INTEGER,
    name VARCHAR(100),
    created_at TIMESTAMP
);
```

## 构建

从源代码构建连接器：

```bash
# 从项目根目录执行
./mvnw clean install -DskipTests -pl plugin/trino-dm
```

## 系统要求

- Trino 479 或更高版本
- 达梦数据库 8.x 或更高版本
- Java 25 或更高版本

## 已知限制

1. 不支持 FULL OUTER JOIN 下推
2. 不支持某些复杂的表达式下推
3. TEXT 类型的列可能不支持某些谓词下推
4. 字符串类型的列在排序时可能不支持 TOP N 下推

## 故障排除

### 连接失败

确保达梦数据库服务正在运行，且连接 URL、用户名、密码配置正确。

### 类型映射错误

检查达梦数据库中的数据类型是否在支持的类型列表中。

### 性能问题

可以通过调整 `dm.fetch-size` 参数来优化查询性能。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

Apache License 2.0

## 参考资料

- [达梦数据库官网](https://www.dameng.com/)
- [达梦数据库文档](https://eco.dameng.com/docs/zh-cn/)
- [Trino 文档](https://trino.io/docs/current/)
