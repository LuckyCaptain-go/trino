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

import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigDescription;

import java.util.Optional;

/**
 * Configuration for DM Database connector
 * 
 * 达梦数据库连接器配置类
 */
public class DmConfig
{
    private Boolean includeSystemTables;
    private Integer fetchSize;

    /**
     * Whether to include system tables in metadata queries
     * 
     * 是否在元数据查询中包含系统表
     */
    public boolean isIncludeSystemTables()
    {
        return includeSystemTables != null && includeSystemTables;
    }

    @Config("dm.include-system-tables")
    @ConfigDescription("Include DM system tables in metadata queries")
    public DmConfig setIncludeSystemTables(Boolean includeSystemTables)
    {
        this.includeSystemTables = includeSystemTables;
        return this;
    }

    /**
     * Fetch size for queries
     * 
     * 查询的获取大小
     */
    public Optional<Integer> getFetchSize()
    {
        return Optional.ofNullable(fetchSize);
    }

    @Config("dm.fetch-size")
    @ConfigDescription("DM fetch size, Trino specific heuristic is applied if empty")
    public DmConfig setFetchSize(Integer fetchSize)
    {
        this.fetchSize = fetchSize;
        return this;
    }
}
