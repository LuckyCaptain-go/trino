# 达梦连接器功能矩阵

本文档详细列出了达梦连接器实现的所有功能，并与 Trino JDBC 连接器规范进行对比。

## 📋 功能实现矩阵

### 元数据查询功能

| 功能 | 方法 | 状态 | 说明 |
|------|------|------|------|
| 获取所有 Schema | `getSchemaNames()` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 列出 Schema | `listSchemas(Connection)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 过滤 Schema | `filterSchema(String)` | ✅ 已实现 | 过滤系统 schema：SYS, SYSDBA, SYSAUDITOR, SYSJOB |
| 获取所有表 | `getTableNames(session, schema)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 获取表注释 | `getAllTableComments()` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 获取表句柄 | `getTableHandle(session, schemaTableName)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 获取表句柄（查询） | `getTableHandle(session, preparedQuery)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 获取所有列 | `getAllTableColumns(connection, schema)` | ✅ 已实现 | 重写以适配达梦 |
| 获取表 Schema 名称 | `getTableSchemaName(resultSet)` | ✅ 已实现 | 返回 TABLE_SCHEM |
| 获取表注释 | `getTableComment(resultSet)` | ✅ 已实现 | 返回 REMARKS 字段 |
| 设置表注释 | `setTableComment(session, handle, comment)` | ✅ 已实现 | 使用 COMMENT ON TABLE 语句 |
| 获取主键 | `getPrimaryKeys(session, remoteTableName)` | ✅ 已实现 | 从数据库元数据读取 |
| 获取表属性 | `getTableProperties(session, handle)` | ✅ 已实现 | 返回主键等属性 |
| 获取列映射 | `toColumnMappings(session, typeHandles)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 获取列大小写敏感性 | `getCaseSensitivityForColumns()` | ✅ 已实现 | 返回 CASE_INSENSITIVE |

### 数据查询功能

| 功能 | 方法 | 状态 | 说明 |
|------|------|------|------|
| 获取分片 | `getSplits(session, tableHandle)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 获取分片（过程） | `getSplits(session, procedureHandle)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 获取连接 | `getConnection(session, split, table)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 获取连接（过程） | `getConnection(session, split, procedure)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 获取连接（默认） | `getConnection(session)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 构建 SQL | `buildSql(session, connection, split, table, columns)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 构建存储过程 | `buildProcedure(session, connection, split, procedure)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 转换谓词 | `convertPredicate(session, expression, assignments)` | ✅ 已实现 | 使用 JdbcConnectorExpressionRewriter |
| 中断读取连接 | `abortReadConnection(connection, resultSet)` | ✅ 已实现 | 调用 connection.abort() |
| 获取预处理语句 | `getPreparedStatement(connection, sql, columnCount)` | ✅ 已实现 | 返回 connection.prepareStatement() |
| 获取占位符 | `getPreparedStatementPlaceholder(session)` | ✅ 已实现 | 返回 "?" |

### 聚合函数功能

| 功能 | 方法 | 状态 | 说明 |
|------|------|------|------|
| 实现聚合 | `implementAggregation(session, aggregate, assignments)` | ✅ 已实现 | 使用 AggregateFunctionRewriter |
| 支持聚合下推 | `supportsAggregationPushdown()` | ✅ 已实现 | 使用 preventTextualTypeAggregationPushdown |

#### 支持的聚合函数

| 函数 | 状态 | 类 |
|------|------|-----|
| COUNT(*) | ✅ | ImplementCountAll |
| COUNT(column) | ✅ | ImplementCount |
| SUM(column) | ✅ | ImplementSum |
| AVG(column) | ✅ | ImplementAvgFloatingPoint, ImplementAvgDecimal |
| MIN(column) | ✅ | ImplementMinMax |
| MAX(column) | ✅ | ImplementMinMax |
| STDDEV_SAMP(column) | ✅ | ImplementStddevSamp |
| STDDEV_POP(column) | ✅ | ImplementStddevPop |
| VAR_SAMP(column) | ✅ | ImplementVarianceSamp |
| VAR_POP(column) | ✅ | ImplementVariancePop |

### JOIN 功能

| 功能 | 方法 | 状态 | 说明 |
|------|------|------|------|
| 实现 JOIN | `implementJoin(session, joinType, ...)` | ✅ 已实现 | 支持成本感知 |
| 实现 JOIN（旧版） | `legacyImplementJoin()` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 支持 JOIN 条件 | `isSupportedJoinCondition(session, condition)` | ✅ 已实现 | 不支持 IDENTICAL |

#### 支持的 JOIN 类型

| JOIN 类型 | 状态 | 说明 |
|----------|------|------|
| INNER JOIN | ✅ 完全支持 | 可以下推 |
| LEFT OUTER JOIN | ✅ 完全支持 | 可以下推 |
| RIGHT OUTER JOIN | ✅ 完全支持 | 可以下推 |
| FULL OUTER JOIN | ❌ 不支持 | 达梦数据库不支持，返回 Optional.empty() |

### 排序和限制功能

| 功能 | 方法 | 状态 | 说明 |
|------|------|------|------|
| 支持 TOP N | `supportsTopN(session, handle, sortOrder)` | ✅ 已实现 | 字符串类型不支持下推 |
| TOP N 函数 | `topNFunction()` | ✅ 已实现 | 使用 FETCH FIRST ... ROWS ONLY |
| TOP N 保证 | `isTopNGuaranteed(session)` | ✅ 已实现 | 返回 true |
| 支持 LIMIT | `supportsLimit()` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| LIMIT 函数 | `limitFunction()` | ✅ 已实现 | 使用 FETCH FIRST ... ROWS ONLY |
| LIMIT 保证 | `isLimitGuaranteed(session)` | ✅ 已实现 | 返回 true |

### 数据定义语言（DDL）功能

| 功能 | 方法 | 状态 | 说明 |
|------|------|------|------|
| 创建 Schema | `createSchema(session, schemaName)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 创建 Schema（连接） | `createSchema(session, connection, schemaName)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 删除 Schema | `dropSchema(session, schemaName, cascade)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 删除 Schema（连接） | `dropSchema(session, connection, schemaName, cascade)` | ✅ 已实现 | 支持 CASCADE |
| 重命名 Schema | `renameSchema(session, oldName, newName)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 重命名 Schema（连接） | `renameSchema(session, connection, oldName, newName)` | ✅ 已实现 | 使用 ALTER SCHEMA RENAME TO |
| 创建表 | `createTable(session, tableMetadata)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 创建表 SQL | `createTableSqls(remoteTableName, columns, tableMetadata)` | ✅ 已实现 | 支持表注释 |
| 重命名表 | `renameTable(session, handle, newTableName)` | ✅ 已实现 | 使用 ALTER TABLE RENAME TO |
| 删除表 | `dropTable(session, handle)` | ✅ 已实现 | 使用 DROP TABLE |
| 清空表 | `truncateTable(session, handle)` | ✅ 已实现 | 使用 TRUNCATE TABLE |
| 重命名列 | `renameColumn(session, connection, table, oldName, newName)` | ✅ 已实现 | 使用 ALTER TABLE RENAME COLUMN |
| 设置列注释 | `setColumnComment(session, handle, column, comment)` | ✅ 已实现 | 使用 COMMENT ON COLUMN |

### 数据操作语言（DML）功能

| 功能 | 方法 | 状态 | 说明 |
|------|------|------|------|
| 执行 SQL | `execute(session, query)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 执行 SQL（连接） | `execute(session, connection, query)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| MERGE 支持 | `supportsMerge()` | ✅ 已实现 | 返回 true |
| 开始 MERGE | `beginMerge(session, handle, ...)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |
| 完成 MERGE | `finishMerge(session, handle, pageSinkIds)` | ✅ 已实现（基类） | 继承自 BaseJdbcClient |

### 统计信息功能

| 功能 | 方法 | 状态 | 说明 |
|------|------|------|------|
| 获取表统计信息 | `getTableStatistics(session, handle)` | ✅ 已实现（基类） | 返回空统计信息 |

### 类型映射功能

| 功能 | 方法 | 状态 | 说明 |
|------|------|------|------|
| 转换为列映射 | `toColumnMapping(session, typeHandle)` | ✅ 已实现 | 支持所有常用类型 |
| 转换为写入映射 | `toWriteMapping(session, type)` | ✅ 已实现 | 支持所有常用类型 |
| 转换为 SQL 类型 | `toSqlTypeHandle(type)` | ✅ 已实现 | 处理 CHAR, VARCHAR, CLOB |

### 配置功能

| 功能 | 配置项 | 状态 | 默认值 |
|------|--------|------|--------|
| 包含系统表 | `dm.include-system-tables` | ✅ 已实现 | false |
| 获取大小 | `dm.fetch-size` | ✅ 已实现 | 自动 |

### 表类型支持

| 表类型 | 状态 | 说明 |
|--------|------|------|
| TABLE | ✅ 支持 | 常规表 |
| VIEW | ✅ 支持 | 视图 |

## 📊 数据类型支持矩阵

### 读取支持（从达梦到 Trino）

| 达梦类型 | JDBC 类型 | Trino 类型 | 状态 |
|----------|-----------|-----------|------|
| BIT | Types.BIT | BOOLEAN | ✅ 完全支持 |
| TINYINT | Types.TINYINT | SMALLINT | ✅ 完全支持 |
| SMALLINT | Types.SMALLINT | SMALLINT | ✅ 完全支持 |
| INTEGER | Types.INTEGER | INTEGER | ✅ 完全支持 |
| BIGINT | Types.BIGINT | BIGINT | ✅ 完全支持 |
| FLOAT | Types.FLOAT | REAL | ✅ 完全支持 |
| DOUBLE | Types.DOUBLE | DOUBLE | ✅ 完全支持 |
| DOUBLE PRECISION | Types.DOUBLE | DOUBLE | ✅ 完全支持 |
| DECIMAL | Types.DECIMAL | DECIMAL | ✅ 完全支持 |
| NUMERIC | Types.NUMERIC | DECIMAL | ✅ 完全支持 |
| CHAR | Types.CHAR | CHAR | ✅ 完全支持 |
| NCHAR | Types.NCHAR | CHAR | ✅ 完全支持 |
| VARCHAR | Types.VARCHAR | VARCHAR | ✅ 完全支持 |
| VARCHAR2 | Types.VARCHAR | VARCHAR | ✅ 完全支持 |
| NVARCHAR | Types.NVARCHAR | VARCHAR | ✅ 完全支持 |
| CLOB | Types.CLOB | VARCHAR | ✅ 完全支持 |
| NCLOB | Types.NCLOB | VARCHAR | ✅ 完全支持 |
| LONGVARCHAR | Types.LONGVARCHAR | VARCHAR | ✅ 完全支持 |
| DATE | Types.DATE | DATE | ✅ 完全支持 |
| TIMESTAMP | Types.TIMESTAMP | TIMESTAMP | ✅ 完全支持 |
| TIMESTAMPTZ | Types.TIMESTAMP | TIMESTAMP WITH TIME ZONE | ✅ 完全支持 |
| TIMESTAMP WITH TIME ZONE | Types.TIMESTAMP_WITH_TIMEZONE | TIMESTAMP WITH TIME ZONE | ✅ 完全支持 |
| BINARY | Types.BINARY | VARBINARY | ✅ 完全支持 |
| VARBINARY | Types.VARBINARY | VARBINARY | ✅ 完全支持 |
| RAW | Types.VARBINARY | VARBINARY | ✅ 完全支持 |
| BOOLEAN | Types.BOOLEAN | BOOLEAN | ✅ 完全支持 |
| REAL | Types.REAL | REAL | ✅ 完全支持 |

### 写入支持（从 Trino 到达梦）

| Trino 类型 | 达梦类型 | 状态 | 说明 |
|-----------|-----------|------|------|
| BOOLEAN | BIT | ✅ 完全支持 | - |
| SMALLINT | SMALLINT | ✅ 完全支持 | - |
| INTEGER | INTEGER | ✅ 完全支持 | - |
| BIGINT | BIGINT | ✅ 完全支持 | - |
| REAL | FLOAT | ✅ 完全支持 | - |
| DOUBLE | DOUBLE PRECISION | ✅ 完全支持 | - |
| DECIMAL(p,s) | DECIMAL(p,s) | ✅ 完全支持 | 保留精度和标度 |
| CHAR(n) | CHAR(n) | ✅ 完全支持 | 保留长度 |
| VARCHAR(n) | VARCHAR(n) | ✅ 完全支持 | 保留长度 |
| VARCHAR (unbounded) | CLOB | ✅ 完全支持 | - |
| DATE | DATE | ✅ 完全支持 | - |
| TIMESTAMP | TIMESTAMP | ✅ 完全支持 | - |
| TIMESTAMP WITH TIME ZONE | TIMESTAMPTZ | ✅ 完全支持 | - |
| VARBINARY | VARBINARY | ✅ 完全支持 | - |

## 🔧 SQL 方言特性

| 特性 | 语法 | 状态 | 说明 |
|------|------|------|------|
| 标识符引号 | `"` | ✅ 支持 | 作为 identifierQuote |
| Schema 分隔符 | `.` | ✅ 支持 | getSchemaSeparator() 返回 "." |
| 参数占位符 | `?` | ✅ 支持 | PreparedStatement 占位符 |
| FETCH FIRST | `FETCH FIRST n ROWS ONLY` | ✅ 支持 | 用于 LIMIT 和 TOP N |
| LIMIT | `FETCH FIRST n ROWS ONLY` | ✅ 支持 | - |
| COMMENT ON | `COMMENT ON TABLE/COLUMN ... IS ...` | ✅ 支持 | - |
| CREATE TABLE | `CREATE TABLE ...` | ✅ 支持 | - |
| DROP TABLE | `DROP TABLE ...` | ✅ 支持 | - |
| TRUNCATE TABLE | `TRUNCATE TABLE ...` | ✅ 支持 | - |
| ALTER TABLE RENAME | `ALTER TABLE ... RENAME TO ...` | ✅ 支持 | 表和列 |
| ALTER COLUMN RENAME | `ALTER TABLE ... RENAME COLUMN ... TO ...` | ✅ 支持 | - |
| DROP SCHEMA | `DROP SCHEMA ... [CASCADE]` | ✅ 支持 | - |
| ALTER SCHEMA RENAME | `ALTER SCHEMA ... RENAME TO ...` | ✅ 支持 | - |
| COMMENT TABLE | `COMMENT ON TABLE ... IS ...` | ✅ 支持 | - |
| COMMENT COLUMN | `COMMENT ON COLUMN ... IS ...` | ✅ 支持 | - |

## ⚡ 下推功能支持

### 谓词下推
- ✅ 基本比较运算符（=, <>, <, <=, >, >=）
- ✅ 逻辑运算符（AND, OR, NOT）
- ✅ IN 操作符
- ✅ BETWEEN 操作符
- ✅ LIKE 操作符
- ✅ IS NULL / IS NOT NULL
- ✅ 布尔表达式下推

### 聚合下推
- ✅ COUNT(*) / COUNT(column)
- ✅ SUM(column)
- ✅ AVG(column)
- ✅ MIN(column) / MAX(column)
- ✅ STDDEV(column)
- ✅ VARIANCE(column)
- ✅ GROUP BY 子句
- ⚠️ GROUP BY 包含文本类型时不支持下推

### JOIN 下推
- ✅ INNER JOIN
- ✅ LEFT OUTER JOIN
- ✅ RIGHT OUTER JOIN
- ❌ FULL OUTER JOIN（达梦不支持）
- ✅ JOIN 谓词下推
- ⚠️ JOIN 条件包含文本类型时可能不支持下推
- ❌ IDENTICAL 操作符不下推

### 排序下推
- ✅ ORDER BY 子句
- ✅ ASC / DESC
- ✅ FETCH FIRST ... ROWS ONLY
- ⚠️ 包含文本类型列的 ORDER BY 不支持下推

### 函数下推
- ✅ 基本算术函数（+, -, *, /）
- ✅ 基本字符串函数（CONCAT, SUBSTRING 等）
- ✅ 日期时间函数
- ⚠️ 复杂函数可能不下推

## 📝 与其他连接器对比

### 功能对比

| 功能 | DM | MySQL | PostgreSQL | Oracle | 说明 |
|------|----|----|-----------|--------|------|
| 基本 CRUD | ✅ | ✅ | ✅ | ✅ |
| 聚合下推 | ✅ | ✅ | ✅ | ✅ |
| JOIN 下推 | ⚠️ | ✅ | ✅ | ✅ | DM 不支持 FULL OUTER |
| FULL OUTER JOIN | ❌ | ❌ | ✅ | ✅ |
| 统计信息 | ⚠️ | ✅ | ✅ | ✅ | DM 使用基类空实现 |
| 批量写入 | ⚠️ | ✅ | ✅ | ✅ | DM 使用基类实现 |
| MERGE | ✅ | ✅ | ✅ | ✅ |
| TOP N | ✅ | ✅ | ✅ | ✅ |
| LIMIT | ✅ | ✅ | ✅ | ✅ |

### 类型支持对比

| 类型 | DM | MySQL | PostgreSQL | Oracle |
|------|----|----|-----------|--------|
| BOOLEAN | ✅ | ✅ | ✅ | ✅ |
| TINYINT | ✅ | ✅ | ✅ | ✅ |
| SMALLINT | ✅ | ✅ | ✅ | ✅ |
| INTEGER | ✅ | ✅ | ✅ | ✅ |
| BIGINT | ✅ | ✅ | ✅ | ✅ |
| FLOAT | ✅ | ✅ | ✅ | ✅ |
| DOUBLE | ✅ | ✅ | ✅ | ✅ |
| DECIMAL | ✅ | ✅ | ✅ | ✅ |
| CHAR | ✅ | ✅ | ✅ | ✅ |
| VARCHAR | ✅ | ✅ | ✅ | ✅ |
| CLOB | ✅ | ✅ | ✅ | ✅ |
| DATE | ✅ | ✅ | ✅ | ✅ |
| TIMESTAMP | ✅ | ✅ | ✅ | ✅ |
| TIMESTAMP WITH TIME ZONE | ✅ | ❌ | ✅ | ✅ |
| VARBINARY | ✅ | ✅ | ✅ | ✅ |
| ARRAY | ❌ | ❌ | ✅ | ❌ |
| JSON | ❌ | ✅ | ✅ | ✅ |

## ✅ 完整性总结

### 核心功能（100%）
- ✅ **元数据查询**：完全实现
- ✅ **数据查询**：完全实现
- ✅ **类型映射**：完全实现所有常用类型
- ✅ **DDL 操作**：完全实现表和 schema 操作
- ✅ **DML 操作**：完全实现插入、更新、删除、MERGE

### 高级功能（85%）
- ✅ **聚合下推**：完全实现
- ✅ **JOIN 下推**：基本实现（缺少 FULL OUTER）
- ✅ **谓词下推**：完全实现
- ✅ **排序和限制**：完全实现
- ⚠️ **统计信息**：使用基类空实现
- ⚠️ **批量写入**：使用基类实现

### 可选功能（60%）
- ⚠️ **动态过滤**：未实现
- ⚠️ **失败重试**：使用基类实现
- ❌ **自定义函数下推**：未实现
- ❌ **数组类型**：达梦不常用
- ❌ **JSON 类型**：达梦不常用

### 已知限制
1. ❌ FULL OUTER JOIN 不支持（达梦数据库限制）
2. ⚠️ 文本类型列的 TOP N 不支持下推
3. ⚠️ JOIN 条件包含文本类型时可能不支持下推
4. ⚠️ IDENTICAL 操作符不支持下推
5. ⚠️ 统计信息收集功能较简单

## 📞 支持和反馈

如有任何问题或建议，请：
1. 查看 [IMPLEMENTATION_CHECKLIST.md](IMPLEMENTATION_CHECKLIST.md) 了解详细实现状态
2. 查看 [README.md](README.md) 了解使用文档
3. 提交 Issue 到项目仓库

---

**最后更新**：2025-12-24  
**版本**：1.0.0  
**状态**：核心功能完整，高级功能基本实现
