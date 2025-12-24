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
import com.google.inject.Inject;
import io.trino.plugin.base.session.SessionPropertiesProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.session.PropertyMetadata;

import java.util.List;

/**
 * Session properties for DM Database connector
 * 
 * 达梦数据库连接器会话属性
 */
public class DmSessionProperties
        implements SessionPropertiesProvider
{
    private final List<PropertyMetadata<?>> sessionProperties;

    @Inject
    public DmSessionProperties()
    {
        sessionProperties = ImmutableList.of();
    }

    @Override
    public List<PropertyMetadata<?>> getSessionProperties()
    {
        return sessionProperties;
    }
}
