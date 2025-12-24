# 达梦数据库连接器快速入门指南

本文档将帮助您快速配置和使用 Trino 达梦数据库连接器。

## 前提条件

1. 已安装 Trino 服务器
2. 已安装达梦数据库（DM 8.x 或更高版本）
3. 已获取达梦 JDBC 驱动（DmJdbcDriver18.jar）
4. Java 25 或更高版本

## 步骤 1: 安装达梦 JDBC 驱动

达梦 JDBC 驱动需要手动安装到 Maven 本地仓库：

```bash
# 1. 从达梦官网或安装目录获取 DmJdbcDriver18.jar
# 2. 安装到 Maven 本地仓库
mvn install:install-file \
  -Dfile=/path/to/DmJdbcDriver18.jar \
  -DgroupId=com.dameng \
  -DartifactId=DmJdbcDriver18 \
  -Dversion=8.1.3.62 \
  -Dpackaging=jar
```

## 步骤 2: 构建达梦连接器

如果从源代码安装：

```bash
# 进入 Trino 源码目录
cd /path/to/trino

# 构建达梦连接器
./mvnw clean install -DskipTests -pl plugin/trino-dm

# 构建完成后，jar 包位于
# plugin/trino-dm/target/trino-dm-479-SNAPSHOT.jar
```

或者从预编译版本安装：
- 下载 `trino-dm-479-SNAPSHOT.jar`
- 复制到 Trino 的 `plugin/dm` 目录

```bash
# 创建插件目录
mkdir -p /path/to/trino/plugin/dm

# 复制连接器 jar 包
cp trino-dm-479-SNAPSHOT.jar /path/to/trino/plugin/dm/
```

## 步骤 3: 配置连接器

创建配置文件：

```bash
# 创建配置目录
mkdir -p /path/to/trino/etc/catalog

# 创建达梦连接器配置文件
vi /path/to/trino/etc/catalog/dm.properties
```

配置内容：

```properties
# 连接器名称
connector.name=dm

# 数据库连接 URL
# 格式: jdbc:dm://host:port
connection-url=jdbc:dm://192.168.1.100:5236

# 数据库用户名
connection-user=SYSDBA

# 数据库密码
connection-password=SYSDBA

# 可选：指定默认数据库（catalog）
# dm.database=YOUR_DATABASE

# 可选：是否包含系统表（默认为 false）
dm.include-system-tables=false

# 可选：查询获取大小（默认自动）
# dm.fetch-size=10000
```

### 连接 URL 格式说明

达梦 JDBC 连接 URL 格式：

```
jdbc:dm://[host]:[port]/[database]?[parameters]
```

示例：

```properties
# 本地默认端口
connection-url=jdbc:dm://localhost:5236

# 指定数据库
connection-url=jdbc:dm://192.168.1.100:5236/TESTDB

# 带参数
connection-url=jdbc:dm://192.168.1.100:5236?characterEncoding=UTF-8
```

### 常用参数

| 参数 | 说明 |
|-----|------|
| characterEncoding | 字符编码，如 UTF-8、GBK |
| connectTimeout | 连接超时时间（毫秒） |
| socketTimeout | Socket 超时时间（毫秒） |
| autoReconnect | 是否自动重连 |
| failoverReadOnly | 故障转移时只读 |

## 步骤 4: 启动 Trino 服务器

```bash
# 启动 Trino
/path/to/trino/bin/launcher start

# 或者在开发环境中启动
/path/to/trino/bin/launcher run
```

## 步骤 5: 验证连接

使用 Trino CLI 连接并验证：

```bash
# 启动 Trino CLI
./trino-cli --server localhost:8080

# 或指定 catalog
./trino-cli --server localhost:8080 --catalog dm
```

在 CLI 中执行查询：

```sql
-- 查看所有 schemas
SHOW SCHEMAS FROM dm;

-- 查看特定 schema 的表
SHOW TABLES FROM dm.YOUR_SCHEMA;

-- 查询表数据
SELECT * FROM dm.YOUR_SCHEMA.YOUR_TABLE LIMIT 10;

-- 测试聚合函数
SELECT COUNT(*) as total FROM dm.YOUR_SCHEMA.YOUR_TABLE;
```

## 常用查询示例

### 查询数据

```sql
-- 基本查询
SELECT id, name, created_at 
FROM dm.YOUR_SCHEMA.users 
WHERE status = 'active' 
LIMIT 100;

-- 聚合查询
SELECT 
    department,
    COUNT(*) as emp_count,
    AVG(salary) as avg_salary
FROM dm.YOUR_SCHEMA.employees
GROUP BY department
ORDER BY emp_count DESC;

-- 多表连接
SELECT 
    u.name as user_name,
    o.order_id,
    o.amount
FROM dm.YOUR_SCHEMA.users u
JOIN dm.YOUR_SCHEMA.orders o ON u.id = o.user_id
WHERE o.status = 'completed';
```

### 插入数据

```sql
-- 插入单条记录
INSERT INTO dm.YOUR_SCHEMA.users (id, name, email, status)
VALUES (1001, '张三', 'zhangsan@example.com', 'active');

-- 从其他数据源插入数据
INSERT INTO dm.YOUR_SCHEMA.users_archive
SELECT * FROM mysql.source.users WHERE created_at < '2020-01-01';
```

### 更新数据

```sql
-- 更新单条记录
UPDATE dm.YOUR_SCHEMA.users 
SET status = 'inactive' 
WHERE id = 1001;

-- 批量更新
UPDATE dm.YOUR_SCHEMA.orders 
SET status = 'shipped' 
WHERE status = 'pending' AND created_at < CURRENT_DATE - INTERVAL '7' DAY;
```

### 删除数据

```sql
-- 删除单条记录
DELETE FROM dm.YOUR_SCHEMA.users WHERE id = 1001;

-- 批量删除
DELETE FROM dm.YOUR_SCHEMA.logs 
WHERE created_at < CURRENT_DATE - INTERVAL '30' DAY;
```

### 创建表

```sql
-- 创建简单表
CREATE TABLE dm.YOUR_SCHEMA.new_table (
    id INTEGER,
    name VARCHAR(100),
    created_at TIMESTAMP
);

-- 创建带约束的表
CREATE TABLE dm.YOUR_SCHEMA.employees (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(200),
    department VARCHAR(50),
    salary DECIMAL(10, 2)
);

-- 创建分区表（如果达梦支持）
CREATE TABLE dm.YOUR_SCHEMA.sales (
    id INTEGER,
    sale_date DATE,
    amount DECIMAL(10, 2),
    region VARCHAR(50)
);
```

## 性能优化建议

### 1. 调整获取大小

```properties
# 在 dm.properties 中配置
dm.fetch-size=10000
```

根据数据量调整：
- 小数据量：1000-5000
- 中等数据量：5000-10000
- 大数据量：10000-50000

### 2. 使用谓词下推

```sql
-- 推荐：在 WHERE 子句中尽早过滤
SELECT * FROM dm.YOUR_SCHEMA.big_table 
WHERE created_at > '2024-01-01' 
  AND status = 'active';

-- 避免全表扫描后再过滤
SELECT * FROM (
    SELECT * FROM dm.YOUR_SCHEMA.big_table
) WHERE created_at > '2024-01-01';
```

### 3. 利用聚合下推

```sql
-- 推荐：让达梦数据库执行聚合
SELECT status, COUNT(*) 
FROM dm.YOUR_SCHEMA.users 
GROUP BY status;

-- 避免将所有数据拉取到 Trino 再聚合
```

### 4. 使用 LIMIT 子句

```sql
-- 总是使用 LIMIT 避免返回过多数据
SELECT * FROM dm.YOUR_SCHEMA.large_table 
ORDER BY created_at DESC 
LIMIT 100;
```

## 故障排查

### 问题 1: 连接失败

**错误信息**：
```
Failed to connect to DM database
```

**解决方案**：
1. 检查达梦数据库是否运行
2. 验证连接 URL、端口是否正确
3. 确认用户名和密码正确
4. 检查防火墙设置

### 问题 2: 找不到驱动

**错误信息**：
```
Driver not found: dm.jdbc.driver.DmDriver
```

**解决方案**：
1. 确认已安装达梦 JDBC 驱动到 Maven 仓库
2. 重新构建连接器
3. 检查连接器 jar 包是否正确部署

### 问题 3: 类型映射错误

**错误信息**：
```
Unsupported column type
```

**解决方案**：
1. 检查达梦表的数据类型是否在支持列表中
2. 考虑在达梦端进行类型转换
3. 查看类型映射文档

### 问题 4: 查询性能慢

**解决方案**：
1. 在达梦数据库上创建合适的索引
2. 调整 `dm.fetch-size` 参数
3. 优化 SQL 查询，使用 WHERE 过滤
4. 检查网络延迟

## 下一步

- 阅读完整的 [README.md](README.md) 了解更多详细信息
- 查看 [达梦数据库官方文档](https://eco.dameng.com/docs/zh-cn/)
- 了解 [Trino SQL 语法](https://trino.io/docs/current/sql.html)

## 获取帮助

如遇问题，请：
1. 查看本文档的故障排查部分
2. 检查 Trino 和达梦数据库日志
3. 提交 Issue 到项目仓库

## 许可证

Apache License 2.0
