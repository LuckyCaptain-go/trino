# 达梦连接器完整性检查清单

本文档详细列出了达梦数据库连接器的实现状态，确保没有遗漏任何重要功能。

## ✅ 已实现的核心功能

### 1. 基础配置和初始化
- [x] `DmPlugin` - 插件入口类，继承 `JdbcPlugin`
- [x] `DmConfig` - 配置类，支持连接参数配置
- [x] `DmClientModule` - 客户端模块，配置依赖注入
- [x] `DmConnectionFactoryModule` - 连接工厂模块
- [x] `DmSessionProperties` - 会话属性类

### 2. DmClient 核心功能

#### 2.1 构造函数和初始化
- [x] 构造函数注入必需依赖（BaseJdbcConfig, ConnectionFactory, TypeManager, QueryBuilder, PredicatePushdownController）
- [x] 初始化 ConnectorExpressionRewriter
- [x] 初始化 AggregateFunctionRewriter
- [x] 注册聚合函数规则（Count, Sum, Avg, Min, Max, Stddev, Variance）

#### 2.2 元数据操作
- [x] `supportedTableTypes()` - 返回支持的表类型（TABLE, VIEW）
- [x] `getSchemaSeparator()` - 返回 schema 分隔符（"."）
- [x] `getTables(Connection, schema, table)` - 获取表列表
- [x] `getAllTableColumns(Connection, schema)` - 获取所有表列
- [x] `getTableSchemaName(ResultSet)` - 从结果集获取表 schema 名称
- [x] `getTableComment(ResultSet)` - 获取表注释
- [x] `setTableComment(session, handle, comment)` - 设置表注释
- [x] `getPrimaryKeys(session, remoteTableName)` - 获取主键
- [x] `getTableProperties(session, handle)` - 获取表属性

#### 2.3 模式（Schema）操作
- [x] `filterSchema(schemaName)` - 过滤系统 schema
  - 过滤：SYS, SYSDBA, SYSAUDITOR, SYSJOB
- [x] `dropSchema(session, connection, schemaName, cascade)` - 删除 schema
- [x] `renameSchema(session, connection, oldName, newName)` - 重命名 schema

#### 2.4 表操作
- [x] `createTableSqls(remoteTableName, columns, tableMetadata)` - 创建表的 SQL
- [x] `renameTable(session, handle, newTableName)` - 重命名表
- [x] `dropTable(session, handle)` - 删除表
- [x] `truncateTable(session, handle)` - 清空表

#### 2.5 列操作
- [x] `renameColumn(session, connection, table, oldName, newName)` - 重命名列
- [x] `setColumnComment(session, handle, column, comment)` - 设置列注释

#### 2.6 查询和下推功能
- [x] `convertPredicate(session, expression, assignments)` - 谓词转换
- [x] `implementAggregation(session, aggregate, assignments)` - 聚合函数实现
- [x] `supportsAggregationPushdown(session, table, aggregates, assignments, groupingSets)` - 支持聚合下推
- [x] `implementJoin(session, joinType, ...)` - JOIN 实现
- [x] `isSupportedJoinCondition(session, joinCondition)` - 支持 JOIN 条件
  - 不支持：IDENTICAL 操作符
- [x] `supportsTopN(session, handle, sortOrder)` - 支持 TOP N
- [x] `topNFunction()` - TOP N 函数实现
- [x] `isTopNGuaranteed(session)` - TOP N 保证
- [x] `limitFunction()` - LIMIT 函数实现
- [x] `isLimitGuaranteed(session)` - LIMIT 保证
- [x] `getCaseSensitivityForColumns(session, connection, table, remoteTable)` - 列大小写敏感性

#### 2.7 连接管理
- [x] `abortReadConnection(connection, resultSet)` - 中断读取连接
- [x] `getPreparedStatement(connection, sql, columnCount)` - 获取预编译语句
- [x] `getPreparedStatementPlaceholder(session)` - 获取占位符（"?"）

### 3. 类型映射

#### 3.1 读取类型映射（toColumnMapping）
- [x] `BIT` → `BOOLEAN`
- [x] `TINYINT`, `SMALLINT` → `SMALLINT`
- [x] `INTEGER` → `INTEGER`
- [x] `BIGINT` → `BIGINT`
- [x] `FLOAT` → `REAL`
- [x] `DOUBLE`, `DOUBLE PRECISION` → `DOUBLE`
- [x] `DECIMAL`, `NUMERIC` → `DECIMAL`（支持精度和标度）
- [x] `CHAR`, `NCHAR` → `CHAR`
- [x] `VARCHAR`, `VARCHAR2`, `NVARCHAR` → `VARCHAR`
- [x] `CLOB`, `NCLOB`, `LONGVARCHAR` → `VARCHAR`
- [x] `DATE` → `DATE`
- [x] `TIMESTAMP` → `TIMESTAMP`
- [x] `TIMESTAMPTZ`, `TIMESTAMP WITH TIME ZONE` → `TIMESTAMP WITH TIME ZONE`
- [x] `BINARY`, `VARBINARY`, `RAW` → `VARBINARY`
- [x] `BOOLEAN` (JDBC Types) → `BOOLEAN`
- [x] `REAL`, `FLOAT` (JDBC Types) → `REAL`
- [x] `TIMESTAMP_WITH_TIMEZONE` (JDBC Types) → `TIMESTAMP WITH TIME ZONE`

#### 3.2 写入类型映射（toWriteMapping）
- [x] `BOOLEAN` → `BIT`
- [x] `SMALLINT` → `SMALLINT`
- [x] `INTEGER` → `INTEGER`
- [x] `BIGINT` → `BIGINT`
- [x] `REAL` → `FLOAT`
- [x] `DOUBLE` → `DOUBLE PRECISION`
- [x] `DECIMAL` → `DECIMAL(precision, scale)`
- [x] `CHAR` → `CHAR(length)`
- [x] `VARCHAR` (bounded) → `VARCHAR(length)`
- [x] `VARCHAR` (unbounded) → `CLOB`
- [x] `DATE` → `DATE`
- [x] `TIMESTAMP` → `TIMESTAMP`
- [x] `TIMESTAMP WITH TIME ZONE` → `TIMESTAMPTZ`
- [x] `VARBINARY` → `VARBINARY`

#### 3.3 SQL 类型转换
- [x] `toSqlTypeHandle(Type)` - Trino 类型转换为 SQL 类型
  - `CHAR` → `CHAR(length)`
  - `VARCHAR` (bounded) → `VARCHAR(length)`
  - `VARCHAR` (unbounded) → `CLOB`

### 4. 聚合函数支持
- [x] `COUNT(*)` - Count All
- [x] `COUNT(column)` - Count
- [x] `SUM(column)` - Sum
- [x] `AVG(column)` - Average（浮点数和小数）
- [x] `MIN(column)` - Minimum
- [x] `MAX(column)` - Maximum
- [x] `STDDEV_SAMP(column)` - Sample Standard Deviation
- [x] `STDDEV_POP(column)` - Population Standard Deviation
- [x] `VAR_SAMP(column)` - Sample Variance
- [x] `VAR_POP(column)` - Population Variance

### 5. JOIN 支持
- [x] INNER JOIN
- [x] LEFT OUTER JOIN
- [x] RIGHT OUTER JOIN
- [x] JOIN 成本感知
- [ ] FULL OUTER JOIN（达梦不支持 FULL OUTER JOIN）

### 6. MERGE 支持
- [x] `supportsMerge()` - 支持 MERGE 操作

### 7. 配置参数
- [x] `dm.include-system-tables` - 是否包含系统表（默认 false）
- [x] `dm.fetch-size` - 查询获取大小（默认自动）

### 8. 统计信息
- [x] 继承基类的 `getTableStatistics()` - 返回空统计信息
  - 基类实现返回 TableStatistics.empty()
  - 如需支持统计信息，需要重写此方法

### 9. 错误处理
- [x] `TableNotFoundException` - 表不存在异常
- [x] `JDBC_ERROR` - JDBC 错误
- [x] `NOT_SUPPORTED` - 不支持的操作异常
- [x] 达梦错误码处理（-2106 表不存在）

## ⚠️ 部分实现或可选功能

### 1. 统计信息收集
- [ ] 实现自定义的统计信息收集
  - 当前使用基类空实现
  - 可选：实现表行数、列统计信息收集

### 2. 动态过滤（Dynamic Filtering）
- [ ] 支持 JDBC 动态过滤
  - 需要实现 `JdbcDynamicFilteringSplitManager`
  - 需要相应的配置类

### 3. 写入操作优化
- [ ] 批量写入支持（Write Batch Size）
- [ ] 并行写入支持（Write Parallelism）
- [ ] MERGE 操作优化

### 4. 列操作扩展
- [ ] `addColumn(session, handle, column, position)` - 添加列
- [ ] `setColumnType(session, handle, column, type)` - 设置列类型
- [ ] `dropNotNullConstraint(session, handle, column)` - 删除非空约束
- [ ] `addColumnComment(session, connection, table, columnName, comment)` - 添加列注释（已通过 setColumnComment 实现）

### 5. 高级 JOIN 特性
- [ ] JOIN 谓词下推的更多优化
- [ ] JOIN 条件类型支持扩展

### 6. 事务支持
- [ ] 事务管理（Transaction Manager）
- [ ] 非事务性插入/合并
- [ ] 失败重试支持（Retry Support）

## 📋 与其他连接器对比

### 与 MySQL 连接器对比
| 功能 | MySQL | DM | 说明 |
|------|--------|-----|------|
| 基本 CRUD | ✅ | ✅ | 完全支持 |
| 聚合下推 | ✅ | ✅ | 完全支持 |
| JOIN 下推 | ✅ | ✅ | 支持 INNER/LEFT/RIGHT，无 FULL |
| FULL OUTER JOIN | ❌ | ❌ | 都不支持 |
| 统计信息 | ✅ | ⚠️ | MySQL 有完整实现，DM 使用基类空实现 |
| 批量写入 | ✅ | ⚠️ | DM 使用基类实现 |
| MERGE | ✅ | ✅ | 都支持 |

### 与 PostgreSQL 连接器对比
| 功能 | PostgreSQL | DM | 说明 |
|------|-----------|-----|------|
| 基本 CRUD | ✅ | ✅ | 完全支持 |
| 聚合下推 | ✅ | ✅ | 完全支持 |
| FULL OUTER JOIN | ✅ | ❌ | PostgreSQL 支持，DM 不支持 |
| 数组类型 | ✅ | ❌ | PostgreSQL 特有 |
| JSON 类型 | ✅ | ❌ | PostgreSQL 特有 |
| 地理空间类型 | ✅ | ❌ | PostgreSQL 特有 |

### 与 Oracle 连接器对比
| 功能 | Oracle | DM | 说明 |
|------|--------|-----|------|
| 基本 CRUD | ✅ | ✅ | 完全支持 |
| 聚合下推 | ✅ | ✅ | 完全支持 |
| FULL OUTER JOIN | ✅ | ❌ | Oracle 支持，DM 不支持 |
| 批量写入 | ✅ | ⚠️ | Oracle 有优化，DM 使用基类 |
| MERGE | ✅ | ✅ | 都支持 |

## 🔄 达梦数据库特性支持

### 1. 达梦特有的数据类型
- [x] `VARCHAR2` - Oracle 兼容的 VARCHAR
- [x] `TIMESTAMPTZ` - 带时区的时间戳
- [ ] `BFILE` - 外部文件（不常见）
- [ ] `ROWID` - 行标识符（内部使用）

### 2. 达梦特有的函数
- [ ] 支持达梦特有函数的下推
- [ ] 自定义函数支持

### 3. 达梦存储过程
- [x] 基类支持存储过程调用
- [ ] 达梦存储过程特性支持

## 📊 测试覆盖

### 单元测试
- [x] `TestDmConnector` - 基础测试
  - [x] 插件创建测试
  - [x] 配置测试

### 集成测试（建议添加）
- [ ] `TestDmConnectorIntegration` - 集成测试
  - [ ] 连接测试
  - [ ] 查询测试
  - [ ] 类型映射测试
  - [ ] 聚合函数测试
  - [ ] JOIN 测试
  - [ ] 写入操作测试

### 性能测试（建议添加）
- [ ] 查询性能测试
- [ ] 批量写入性能测试
- [ ] 大数据量测试

## 🐛 已知问题和限制

### 1. FULL OUTER JOIN
- **问题**：达梦数据库不支持 FULL OUTER JOIN
- **影响**：此类查询无法下推到达梦
- **解决方案**：在 Trino 层面执行 JOIN

### 2. 字符串类型排序
- **问题**：TOP N 对字符串类型列不支持下推
- **影响**：字符串类型列的排序在 Trino 层面执行
- **原因**：避免因大小写敏感性导致结果不一致

### 3. 统计信息
- **问题**：未实现自定义统计信息收集
- **影响**：查询优化器可能无法获得准确的数据分布信息
- **状态**：使用基类空实现，返回空统计信息

### 4. IDENTICAL 操作符
- **问题**：JOIN 条件中的 IDENTICAL 操作符不支持下推
- **影响**：此类 JOIN 在 Trino 层面执行
- **原因**：达梦不直接支持 IS NOT DISTINCT FROM

## 🎯 待优化项

### 高优先级
1. 实现统计信息收集功能
2. 添加更多集成测试用例
3. 性能优化和基准测试

### 中优先级
1. 添加列操作支持（addColumn, setColumnType, dropNotNullConstraint）
2. 优化批量写入性能
3. 实现动态过滤支持

### 低优先级
1. 支持更多达梦特有数据类型
2. 支持达梦特有函数下推
3. 增强错误处理和日志记录

## ✅ 总结

达梦数据库连接器已经实现了完整的 JDBC 基础功能，包括：

1. ✅ **完整的类型映射**：支持达梦所有常用数据类型
2. ✅ **查询下推**：支持谓词、聚合、JOIN、TOP N、LIMIT 下推
3. ✅ **基本 CRUD**：支持完整的查询、插入、更新、删除操作
4. ✅ **DDL 操作**：支持创建、修改、删除表和 schema
5. ✅ **元数据操作**：支持表、列、主键、注释等元数据查询
6. ✅ **MERGE 操作**：支持数据合并操作
7. ✅ **配置灵活**：支持多种配置参数

### 缺失的高级功能
- 自定义统计信息收集（可选）
- 动态过滤支持（可选）
- 批量写入优化（已有基类实现）
- FULL OUTER JOIN（达梦不支持）

## 📞 支持信息

如有问题或建议，请：
1. 查看本文档的限制部分
2. 检查 Trino 和达梦数据库日志
3. 提交 Issue 到项目仓库

## 📄 相关文档

- [README.md](README.md) - 完整的使用文档
- [QUICKSTART.md](QUICKSTART.md) - 快速入门指南
- 达梦数据库官方文档：https://eco.dameng.com/docs/zh-cn/
- Trino JDBC 连接器文档：https://trino.io/docs/current/develop/jdbc-overview.html

---

**最后更新时间**：2025-12-24  
**版本**：1.0.0  
**状态**：核心功能完整，部分高级功能待优化
