/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.dm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.airlift.log.Logger;
import io.trino.plugin.base.aggregation.AggregateFunctionRewriter;
import io.trino.plugin.base.aggregation.AggregateFunctionRule;
import io.trino.plugin.base.expression.ConnectorExpressionRewriter;
import io.trino.plugin.jdbc.BaseJdbcClient;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.ColumnMapping;
import io.trino.plugin.jdbc.ConnectionFactory;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcExpression;
import io.trino.plugin.jdbc.JdbcJoinCondition;
import io.trino.plugin.jdbc.JdbcTableHandle;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.LongReadFunction;
import io.trino.plugin.jdbc.LongWriteFunction;
import io.trino.plugin.jdbc.PredicatePushdownController;
import io.trino.plugin.jdbc.PreparedQuery;
import io.trino.plugin.jdbc.QueryBuilder;
import io.trino.plugin.jdbc.RemoteTableName;
import io.trino.plugin.jdbc.WriteMapping;
import io.trino.plugin.jdbc.aggregation.ImplementAvgDecimal;
import io.trino.plugin.jdbc.aggregation.ImplementAvgFloatingPoint;
import io.trino.plugin.jdbc.aggregation.ImplementCount;
import io.trino.plugin.jdbc.aggregation.ImplementCountAll;
import io.trino.plugin.jdbc.aggregation.ImplementMinMax;
import io.trino.plugin.jdbc.aggregation.ImplementStddevPop;
import io.trino.plugin.jdbc.aggregation.ImplementStddevSamp;
import io.trino.plugin.jdbc.aggregation.ImplementSum;
import io.trino.plugin.jdbc.aggregation.ImplementVariancePop;
import io.trino.plugin.jdbc.aggregation.ImplementVarianceSamp;
import io.trino.plugin.jdbc.expression.JdbcConnectorExpressionRewriterBuilder;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.AggregateFunction;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.JoinCondition;
import io.trino.spi.connector.JoinStatistics;
import io.trino.spi.connector.JoinType;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.TableNotFoundException;
import io.trino.spi.type.CharType;
import io.trino.spi.type.DecimalType;
import io.trino.spi.type.Decimals;
import io.trino.spi.type.StandardTypes;
import io.trino.spi.type.Type;
import io.trino.spi.type.TypeManager;
import io.trino.spi.type.TypeSignature;
import io.trino.spi.type.VarcharType;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static io.trino.plugin.jdbc.DecimalConfig.DEFAULT_DECIMAL_MAPPING;
import static io.trino.plugin.jdbc.DecimalSessionProperties.getDecimalRounding;
import static io.trino.plugin.jdbc.DecimalSessionProperties.getDecimalRoundingMode;
import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static io.trino.plugin.jdbc.StandardColumnMappings.bigintColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.bigintWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.booleanColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.charReadFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.charWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.dateColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.decimalColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.doubleColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.doubleWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.integerColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.integerWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.realColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.realWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.smallintColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.smallintWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.timestampWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.varbinaryColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.varcharReadFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.varcharWriteFunction;
import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.BooleanType.BOOLEAN;
import static io.trino.spi.type.DateType.DATE;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.RealType.REAL;
import static io.trino.spi.type.SmallintType.SMALLINT;
import static io.trino.spi.type.TimestampType.TIMESTAMP_MILLIS;
import static io.trino.spi.type.TimestampWithTimeZoneType.TIMESTAMP_TZ_MILLIS;
import static io.trino.spi.type.VarbinaryType.VARBINARY;
import static java.lang.String.format;

/**
 * DM Database Client
 * 
 * 达梦数据库客户端，处理与达梦数据库的交互
 */
public class DmClient
        extends BaseJdbcClient
{
    private static final Logger log = Logger.get(DmClient.class);
    
    private static final JdbcTypeHandle BIGINT_TYPE_HANDLE = new JdbcTypeHandle(Types.BIGINT, Optional.of("BIGINT"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    private static final JdbcTypeHandle INTEGER_TYPE_HANDLE = new JdbcTypeHandle(Types.INTEGER, Optional.of("INTEGER"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    private static final JdbcTypeHandle SMALLINT_TYPE_HANDLE = new JdbcTypeHandle(Types.SMALLINT, Optional.of("SMALLINT"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    
    private final AggregateFunctionRewriter<JdbcExpression, ParameterizedExpression> aggregateFunctionRewriter;
    private final ConnectorExpressionRewriter<JdbcExpression, ParameterizedExpression> connectorExpressionRewriter;

    @Inject
    public DmClient(
            BaseJdbcConfig config,
            ConnectionFactory connectionFactory,
            TypeManager typeManager,
            QueryBuilder queryBuilder,
            PredicatePushdownController predicatePushdownController)
    {
        super(config, "\"", connectionFactory, queryBuilder, predicatePushdownController, typeManager);
        
        this.connectorExpressionRewriter = new JdbcConnectorExpressionRewriterBuilder()
                .addStandardRules(this)
                .build();
        
        this.aggregateFunctionRewriter = new AggregateFunctionRewriter<>(
                this.connectorExpressionRewriter,
                ImmutableList.<AggregateFunctionRule<JdbcExpression, ParameterizedExpression>>builder()
                        .add(new ImplementCountAll(BIGINT_TYPE_HANDLE))
                        .add(new ImplementCount(BIGINT_TYPE_HANDLE))
                        .add(new ImplementMinMax(false))
                        .add(new ImplementSum(DmClient::toTypeHandle))
                        .add(new ImplementAvgFloatingPoint())
                        .add(new ImplementAvgDecimal())
                        .add(new ImplementStddevSamp())
                        .add(new ImplementStddevPop())
                        .add(new ImplementVarianceSamp())
                        .add(new ImplementVariancePop())
                        .build());
    }

    @Override
    protected Iterable<String> supportedTableTypes()
    {
        return ImmutableList.of("TABLE", "VIEW");
    }

    @Override
    protected String getSchemaSeparator()
    {
        return ".";
    }

    @Override
    public Optional<ParameterizedExpression> convertPredicate(ConnectorSession session, io.trino.spi.expression.ConnectorExpression expression, Map<String, io.trino.spi.connector.ColumnHandle> assignments)
    {
        return connectorExpressionRewriter.rewrite(session, expression, assignments);
    }

    @Override
    public Optional<JdbcExpression> implementAggregation(ConnectorSession session, AggregateFunction aggregate, Map<String, io.trino.spi.connector.ColumnHandle> assignments)
    {
        return aggregateFunctionRewriter.rewrite(session, aggregate, assignments);
    }

    @Override
    public boolean supportsAggregationPushdown(ConnectorSession session, JdbcTableHandle table, List<AggregateFunction> aggregates, Map<String, io.trino.spi.connector.ColumnHandle> assignments, List<List<io.trino.spi.connector.ColumnHandle>> groupingSets)
    {
        return preventTextualTypeAggregationPushdown(groupingSets);
    }

    @Override
    public ResultSet getTables(Connection connection, Optional<String> schemaName, Optional<String> tableName)
            throws SQLException
    {
        DatabaseMetaData metadata = connection.getMetaData();
        return metadata.getTables(
                null,
                schemaName.orElse(null),
                escapeObjectNameForMetadataQuery(tableName, metadata.getSearchStringEscape()).orElse(null),
                getTableTypes().map(types -> types.toArray(String[]::new)).orElse(null));
    }

    @Override
    protected ResultSet getAllTableColumns(Connection connection, Optional<String> remoteSchemaName)
            throws SQLException
    {
        DatabaseMetaData metadata = connection.getMetaData();
        return metadata.getColumns(
                null,
                remoteSchemaName.orElse(null),
                null,
                null);
    }

    @Override
    protected String getTableSchemaName(ResultSet resultSet)
            throws SQLException
    {
        return resultSet.getString("TABLE_SCHEM");
    }

    @Override
    public boolean supportsTopN(ConnectorSession session, JdbcTableHandle handle, List<io.trino.plugin.jdbc.JdbcSortItem> sortOrder)
    {
        for (io.trino.plugin.jdbc.JdbcSortItem sortItem : sortOrder) {
            Type sortItemType = sortItem.column().getColumnType();
            if (sortItemType instanceof CharType || sortItemType instanceof VarcharType) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Optional<TopNFunction> topNFunction()
    {
        return Optional.of((query, sortItems, limit) -> {
            String orderBy = sortItems.stream()
                    .map(sortItem -> {
                        String ordering = sortItem.sortOrder().isAscending() ? "ASC" : "DESC";
                        return format("%s %s", quoted(sortItem.column().getColumnName()), ordering);
                    })
                    .collect(java.util.stream.Collectors.joining(", "));
            return format("%s ORDER BY %s FETCH FIRST %d ROWS ONLY", query, orderBy, limit);
        });
    }

    @Override
    public boolean isTopNGuaranteed(ConnectorSession session)
    {
        return true;
    }

    @Override
    protected Optional<TopNFunction> limitFunction()
    {
        return Optional.of((sql, limit) -> sql + " FETCH FIRST " + limit + " ROWS ONLY");
    }

    @Override
    public boolean isLimitGuaranteed(ConnectorSession session)
    {
        return true;
    }

    @Override
    protected Optional<BiFunction<String, Long, String>> limitFunction()
    {
        return Optional.of((sql, limit) -> sql + " FETCH FIRST " + limit + " ROWS ONLY");
    }

    @Override
    public boolean supportsMerge()
    {
        return true;
    }

    @Override
    protected Optional<PreparedQuery> implementJoin(
            ConnectorSession session,
            JoinType joinType,
            PreparedQuery leftSource,
            Map<JdbcColumnHandle, String> leftProjections,
            PreparedQuery rightSource,
            Map<JdbcColumnHandle, String> rightProjections,
            List<ParameterizedExpression> joinConditions,
            JoinStatistics statistics)
    {
        if (joinType == JoinType.FULL_OUTER) {
            return Optional.empty();
        }
        return implementJoinCostAware(
                session,
                joinType,
                leftSource,
                rightSource,
                statistics,
                () -> super.implementJoin(session, joinType, leftSource, leftProjections, rightSource, rightProjections, joinConditions, statistics));
    }

    @Override
    protected boolean isSupportedJoinCondition(ConnectorSession session, JdbcJoinCondition joinCondition)
    {
        if (joinCondition.getOperator() == JoinCondition.Operator.IDENTICAL) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean filterSchema(String schemaName)
    {
        if (schemaName.equalsIgnoreCase("SYS")
                || schemaName.equalsIgnoreCase("SYSDBA")
                || schemaName.equalsIgnoreCase("SYSAUDITOR")
                || schemaName.equalsIgnoreCase("SYSJOB")) {
            return false;
        }
        return super.filterSchema(schemaName);
    }

    @Override
    protected Map<String, io.trino.plugin.jdbc.CaseSensitivity> getCaseSensitivityForColumns(
            ConnectorSession session,
            Connection connection,
            SchemaTableName schemaTableName,
            RemoteTableName remoteTableName)
    {
        // 达梦数据库默认是大小写不敏感的，但在某些配置下可能大小写敏感
        // 这里返回 CASE_INSENSITIVE，即列名比较时不区分大小写
        PreparedQuery preparedQuery = new PreparedQuery(
                format("SELECT * FROM %s WHERE 1=0", quoted(remoteTableName)),
                ImmutableList.of());

        try (PreparedStatement preparedStatement = queryBuilder.prepareStatement(
                        this, session, connection, preparedQuery, Optional.empty())) {
            ResultSetMetaData metadata = preparedStatement.getMetaData();
            ImmutableMap.Builder<String, io.trino.plugin.jdbc.CaseSensitivity> columns = ImmutableMap.builder();
            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                String name = metadata.getColumnName(column);
                // 达梦数据库通常不区分大小写
                columns.put(name, io.trino.plugin.jdbc.CaseSensitivity.CASE_INSENSITIVE);
            }
            return columns.buildOrThrow();
        }
        catch (SQLException e) {
            if (e.getErrorCode() == -2106) { // 表不存在的错误码
                throw new TableNotFoundException(schemaTableName);
            }
            throw new TrinoException(JDBC_ERROR, "Failed to get case sensitivity for columns. " + firstNonNull(e.getMessage(), e), e);
        }
    }

    @Override
    public Optional<String> getTableComment(ResultSet resultSet)
            throws SQLException
    {
        return Optional.ofNullable(emptyToNull(resultSet.getString("REMARKS")));
    }

    @Override
    protected String toSqlTypeHandle(Type type)
    {
        if (type instanceof CharType charType) {
            return "CHAR(" + charType.getLength() + ")";
        }
        if (type instanceof VarcharType varcharType) {
            if (varcharType.isUnbounded()) {
                return "CLOB";
            }
            return "VARCHAR(" + varcharType.getBoundedLength() + ")";
        }
        return super.toSqlTypeHandle(type);
    }

    @Override
    public ColumnMapping toColumnMapping(ConnectorSession session, JdbcTypeHandle typeHandle)
    {
        String jdbcTypeName = typeHandle.getJdbcTypeName()
                .orElseThrow(() -> new TrinoException(JDBC_ERROR, "Type name is missing: " + typeHandle));

        switch (jdbcTypeName) {
            case "BIT":
                return booleanColumnMapping();
                
            case "TINYINT":
            case "SMALLINT":
                return smallintColumnMapping();
                
            case "INTEGER":
                return integerColumnMapping();
                
            case "BIGINT":
                return bigintColumnMapping();
                
            case "FLOAT":
                return realColumnMapping();
                
            case "DOUBLE":
            case "DOUBLE PRECISION":
                return doubleColumnMapping();
                
            case "DECIMAL":
            case "NUMERIC":
                return decimalColumnMapping(
                        typeHandle.getRequiredColumnSize(),
                        typeHandle.getRequiredDecimalDigits(),
                        typeHandle.getRequiredDisplayName(),
                        getDecimalRounding(session),
                        getDecimalRoundingMode(session),
                        DEFAULT_DECIMAL_MAPPING);
                
            case "CHAR":
            case "NCHAR":
                return ColumnMapping.sliceMapping(
                        CharType.createCharType(typeHandle.getRequiredColumnSize()),
                        charReadFunction(CharType.createCharType(typeHandle.getRequiredColumnSize())),
                        charWriteFunction(),
                        DISABLE_PUSHDOWN);
                
            case "VARCHAR":
            case "VARCHAR2":
            case "NVARCHAR":
                return ColumnMapping.sliceMapping(
                        VarcharType.createVarcharType(typeHandle.getRequiredColumnSize()),
                        varcharReadFunction(VarcharType.createVarcharType(typeHandle.getRequiredColumnSize())),
                        varcharWriteFunction(),
                        DISABLE_PUSHDOWN);
                
            case "CLOB":
            case "NCLOB":
                return ColumnMapping.sliceMapping(
                        VarcharType.VARCHAR,
                        varcharReadFunction(VarcharType.VARCHAR),
                        varcharWriteFunction(),
                        DISABLE_PUSHDOWN);
                
            case "DATE":
                return dateColumnMapping();
                
            case "TIMESTAMP":
                return ColumnMapping.longMapping(
                        TIMESTAMP_MILLIS,
                        (resultSet, columnIndex) -> {
                            java.sql.Timestamp timestamp = resultSet.getTimestamp(columnIndex);
                            if (timestamp == null) {
                                return null;
                            }
                            return timestamp.getTime();
                        },
                        timestampWriteFunction(TIMESTAMP_MILLIS));
                
            case "TIMESTAMPTZ":
            case "TIMESTAMP WITH TIME ZONE":
                return ColumnMapping.longMapping(
                        TIMESTAMP_TZ_MILLIS,
                        (resultSet, columnIndex) -> {
                            java.sql.Timestamp timestamp = resultSet.getTimestamp(columnIndex);
                            if (timestamp == null) {
                                return null;
                            }
                            return timestamp.getTime();
                        },
                        timestampWriteFunction(TIMESTAMP_TZ_MILLIS));
                
            case "BINARY":
            case "VARBINARY":
            case "RAW":
                return varbinaryColumnMapping();
                
            default:
                break;
        }

        // Handle by JDBC type
        int columnSize = typeHandle.getColumnSize().orElse(0);
        switch (typeHandle.getJdbcType()) {
            case Types.BOOLEAN:
                return booleanColumnMapping();
                
            case Types.TINYINT:
            case Types.SMALLINT:
                return smallintColumnMapping();
                
            case Types.INTEGER:
                return integerColumnMapping();
                
            case Types.BIGINT:
                return bigintColumnMapping();
                
            case Types.REAL:
            case Types.FLOAT:
                return realColumnMapping();
                
            case Types.DOUBLE:
                return doubleColumnMapping();
                
            case Types.NUMERIC:
            case Types.DECIMAL:
                return decimalColumnMapping(
                        typeHandle.getRequiredColumnSize(),
                        typeHandle.getRequiredDecimalDigits(),
                        typeHandle.getRequiredDisplayName(),
                        getDecimalRounding(session),
                        getDecimalRoundingMode(session),
                        DEFAULT_DECIMAL_MAPPING);
                
            case Types.CHAR:
                return ColumnMapping.sliceMapping(
                        CharType.createCharType(columnSize),
                        charReadFunction(CharType.createCharType(columnSize)),
                        charWriteFunction(),
                        DISABLE_PUSHDOWN);
                
            case Types.VARCHAR:
                return ColumnMapping.sliceMapping(
                        VarcharType.createVarcharType(columnSize),
                        varcharReadFunction(VarcharType.createVarcharType(columnSize)),
                        varcharWriteFunction(),
                        DISABLE_PUSHDOWN);
                
            case Types.LONGVARCHAR:
                return ColumnMapping.sliceMapping(
                        VarcharType.VARCHAR,
                        varcharReadFunction(VarcharType.VARCHAR),
                        varcharWriteFunction(),
                        DISABLE_PUSHDOWN);
                
            case Types.DATE:
                return dateColumnMapping();
                
            case Types.TIMESTAMP:
                return ColumnMapping.longMapping(
                        TIMESTAMP_MILLIS,
                        (resultSet, columnIndex) -> {
                            java.sql.Timestamp timestamp = resultSet.getTimestamp(columnIndex);
                            if (timestamp == null) {
                                return null;
                            }
                            return timestamp.getTime();
                        },
                        timestampWriteFunction(TIMESTAMP_MILLIS));
                
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return ColumnMapping.longMapping(
                        TIMESTAMP_TZ_MILLIS,
                        (resultSet, columnIndex) -> {
                            java.sql.Timestamp timestamp = resultSet.getTimestamp(columnIndex);
                            if (timestamp == null) {
                                return null;
                            }
                            return timestamp.getTime();
                        },
                        timestampWriteFunction(TIMESTAMP_TZ_MILLIS));
                
            case Types.BINARY:
            case Types.VARBINARY:
                return varbinaryColumnMapping();
                
            default:
                throw new TrinoException(NOT_SUPPORTED, "Unsupported column type: " + jdbcTypeName + " (jdbcType=" + typeHandle.getJdbcType() + ")");
        }
    }

    @Override
    public WriteMapping toWriteMapping(ConnectorSession session, Type type)
    {
        if (type.equals(BOOLEAN)) {
            return WriteMapping.booleanMapping("BIT", booleanWriteFunction());
        }
        
        if (type.equals(SMALLINT)) {
            return WriteMapping.longMapping("SMALLINT", smallintWriteFunction());
        }
        
        if (type.equals(INTEGER)) {
            return WriteMapping.longMapping("INTEGER", integerWriteFunction());
        }
        
        if (type.equals(BIGINT)) {
            return WriteMapping.longMapping("BIGINT", bigintWriteFunction());
        }
        
        if (type.equals(REAL)) {
            return WriteMapping.longMapping("FLOAT", realWriteFunction());
        }
        
        if (type.equals(DOUBLE)) {
            return WriteMapping.longMapping("DOUBLE PRECISION", doubleWriteFunction());
        }
        
        if (type instanceof DecimalType decimalType) {
            String dataType = format("DECIMAL(%d, %d)", decimalType.getPrecision(), decimalType.getScale());
            return WriteMapping.objectMapping(dataType, decimalWriteFunction(decimalType));
        }
        
        if (type instanceof CharType charType) {
            return WriteMapping.sliceMapping("CHAR(" + charType.getLength() + ")", charWriteFunction());
        }
        
        if (type instanceof VarcharType varcharType) {
            String dataType;
            if (varcharType.isUnbounded()) {
                dataType = "CLOB";
            }
            else {
                dataType = "VARCHAR(" + varcharType.getBoundedLength() + ")";
            }
            return WriteMapping.sliceMapping(dataType, varcharWriteFunction());
        }
        
        if (type.equals(DATE)) {
            return WriteMapping.longMapping("DATE", dateWriteFunction());
        }
        
        if (type.equals(TIMESTAMP_MILLIS)) {
            return WriteMapping.longMapping("TIMESTAMP", timestampWriteFunction(TIMESTAMP_MILLIS));
        }
        
        if (type.equals(TIMESTAMP_TZ_MILLIS)) {
            return WriteMapping.longMapping("TIMESTAMPTZ", timestampWriteFunction(TIMESTAMP_TZ_MILLIS));
        }
        
        if (type.equals(VARBINARY)) {
            return WriteMapping.sliceMapping("VARBINARY", varbinaryWriteFunction());
        }
        
        throw new TrinoException(NOT_SUPPORTED, "Unsupported column type: " + type.getDisplayName());
    }

    private static Optional<JdbcTypeHandle> toTypeHandle(DecimalType decimalType)
    {
        return Optional.of(new JdbcTypeHandle(Types.NUMERIC, Optional.of("DECIMAL"), Optional.of(decimalType.getPrecision()), Optional.of(decimalType.getScale()), Optional.empty(), Optional.empty()));
    }

    @Override
    protected String getPreparedStatementPlaceholder(ConnectorSession session)
    {
        return "?";
    }

    @Override
    public void setTableComment(ConnectorSession session, JdbcTableHandle handle, Optional<String> comment)
    {
        // 达梦数据库使用 COMMENT ON 语句
        RemoteTableName remoteTableName = handle.asPlainTable().getRemoteTableName();
        String commentSql;
        if (comment.isEmpty()) {
            commentSql = format("COMMENT ON TABLE %s IS ''", quoted(remoteTableName));
        }
        else {
            commentSql = format("COMMENT ON TABLE %s IS %s", quoted(remoteTableName), quoted(comment.get()));
        }
        execute(session, commentSql);
    }

    @Override
    public void setColumnComment(ConnectorSession session, JdbcTableHandle handle, JdbcColumnHandle column, Optional<String> comment)
    {
        // 达梦数据库使用 COMMENT ON COLUMN 语句
        String sql = format(
                "COMMENT ON COLUMN %s.%s IS %s",
                quoted(handle.asPlainTable().getRemoteTableName()),
                quoted(column.getColumnName()),
                comment.map(this::varcharLiteral).orElse("NULL"));
        execute(session, sql);
    }

    @Override
    protected List<String> createTableSqls(RemoteTableName remoteTableName, List<String> columns, ConnectorTableMetadata tableMetadata)
    {
        ImmutableList.Builder<String> createTableSqlsBuilder = ImmutableList.builder();
        createTableSqlsBuilder.add(format("CREATE TABLE %s (%s)", quoted(remoteTableName), join(", ", columns)));
        
        Optional<String> tableComment = tableMetadata.getComment();
        if (tableComment.isPresent() && !tableComment.get().isEmpty()) {
            createTableSqlsBuilder.add(format("COMMENT ON TABLE %s IS %s", quoted(remoteTableName), quoted(tableComment.get())));
        }
        
        return createTableSqlsBuilder.build();
    }

    @Override
    public List<JdbcColumnHandle> getPrimaryKeys(ConnectorSession session, RemoteTableName remoteTableName)
    {
        SchemaTableName tableName = new SchemaTableName(remoteTableName.getSchemaName().orElse(null), remoteTableName.getTableName());
        Map<String, JdbcColumnHandle> columns = getColumns(session, tableName, remoteTableName).stream()
                .collect(java.util.stream.Collectors.toMap(JdbcColumnHandle::getColumnName, java.util.function.Function.identity()));
        
        try (Connection connection = connectionFactory.openConnection(session)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            ResultSet primaryKeys = metaData.getPrimaryKeys(
                    remoteTableName.getCatalogName().orElse(null),
                    remoteTableName.getSchemaName().orElse(null),
                    remoteTableName.getTableName());
            
            Map<Short, String> primaryKeysMap = new java.util.TreeMap<>();
            while (primaryKeys.next()) {
                primaryKeysMap.put(primaryKeys.getShort("KEY_SEQ"), primaryKeys.getString("COLUMN_NAME"));
            }
            
            if (primaryKeysMap.isEmpty()) {
                return ImmutableList.of();
            }
            
            return primaryKeysMap.values().stream()
                    .map(columns::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(ImmutableList.toImmutableList());
        }
        catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, "Failed to get primary keys for table: " + remoteTableName, e);
        }
    }

    @Override
    public Map<String, Object> getTableProperties(ConnectorSession session, JdbcTableHandle tableHandle)
    {
        List<String> primaryKeys = getPrimaryKeys(session, tableHandle.getRequiredNamedRelation().getRemoteTableName()).stream()
                .map(JdbcColumnHandle::getColumnName)
                .collect(java.util.stream.Collectors.toList());
        
        ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();
        if (!primaryKeys.isEmpty()) {
            properties.put("primary_key", primaryKeys);
        }
        return properties.buildOrThrow();
    }

    @Override
    public void renameTable(ConnectorSession session, JdbcTableHandle handle, SchemaTableName newTableName)
    {
        RemoteTableName remoteTableName = handle.asPlainTable().getRemoteTableName();
        String sql = format(
                "ALTER TABLE %s RENAME TO %s",
                quoted(remoteTableName),
                quoted(newTableName.getSchemaName().orElse(null), newTableName.getTableName()));
        execute(session, sql);
    }

    @Override
    protected void renameColumn(ConnectorSession session, Connection connection, RemoteTableName remoteTableName, String remoteColumnName, String newRemoteColumnName)
            throws SQLException
    {
        String sql = format(
                "ALTER TABLE %s RENAME COLUMN %s TO %s",
                quoted(remoteTableName),
                quoted(remoteColumnName),
                quoted(newRemoteColumnName));
        execute(session, connection, sql);
    }

    @Override
    public void dropTable(ConnectorSession session, JdbcTableHandle tableHandle)
    {
        execute(session, format("DROP TABLE %s", quoted(tableHandle.asPlainTable().getRemoteTableName())));
    }

    @Override
    public void truncateTable(ConnectorSession session, JdbcTableHandle tableHandle)
    {
        execute(session, format("TRUNCATE TABLE %s", quoted(tableHandle.asPlainTable().getRemoteTableName())));
    }

    @Override
    protected void dropSchema(ConnectorSession session, Connection connection, String remoteSchemaName, boolean cascade)
            throws SQLException
    {
        String dropSchemaSql = "DROP SCHEMA " + quoted(remoteSchemaName);
        if (cascade) {
            dropSchemaSql += " CASCADE";
        }
        execute(session, connection, dropSchemaSql);
    }

    @Override
    protected void renameSchema(ConnectorSession session, Connection connection, String remoteSchemaName, String newRemoteSchemaName)
            throws SQLException
    {
        execute(session, connection, "ALTER SCHEMA " + quoted(remoteSchemaName) + " RENAME TO " + quoted(newRemoteSchemaName));
    }
}
