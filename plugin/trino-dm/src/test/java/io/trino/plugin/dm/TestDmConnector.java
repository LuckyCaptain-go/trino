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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for DM Database connector
 * 
 * 达梦数据库连接器测试
 */
public class TestDmConnector
{
    /**
     * Basic connector test
     * 基本连接器测试
     */
    @Test
    public void testCreateConnector()
    {
        DmPlugin plugin = new DmPlugin();
        assertThat(plugin).isNotNull();
        assertThat(plugin.getConnectorFactories())
                .singleElement()
                .satisfies(factory -> assertThat(factory.getName()).isEqualTo("dm"));
    }

    /**
     * Test configuration
     * 配置测试
     */
    @Test
    public void testDmConfig()
    {
        DmConfig config = new DmConfig();
        assertThat(config.isIncludeSystemTables()).isFalse();

        config.setIncludeSystemTables(true);
        assertThat(config.isIncludeSystemTables()).isTrue();

        config.setFetchSize(1000);
        assertThat(config.getFetchSize()).hasValue(1000);
    }
}
