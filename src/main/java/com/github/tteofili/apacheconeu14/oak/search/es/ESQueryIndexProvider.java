/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tteofili.apacheconeu14.oak.search.es;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.query.QueryIndex;
import org.apache.jackrabbit.oak.spi.query.QueryIndexProvider;
import org.apache.jackrabbit.oak.spi.state.ChildNodeEntry;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.elasticsearch.client.Client;

import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.TYPE_PROPERTY_NAME;

/**
 * provider for {@link com.github.tteofili.apacheconeu14.oak.search.es.ESQueryIndex}
 */
@Component(immediate = true)
@Service(value = QueryIndexProvider.class)
public class ESQueryIndexProvider implements QueryIndexProvider {
    @Nonnull
    @Override
    public List<? extends QueryIndex> getQueryIndexes(NodeState nodeState) {
        List<QueryIndex> tempIndexes = new ArrayList<QueryIndex>();
        NodeState definitions = nodeState.getChildNode(INDEX_DEFINITIONS_NAME);
        for (ChildNodeEntry entry : definitions.getChildNodeEntries()) {
            NodeState definition = entry.getNodeState();
            PropertyState type = definition.getProperty(TYPE_PROPERTY_NAME);
            if (type != null
                    && "es".equals(type.getValue(Type.STRING))) {
                try {
                    Thread thread = Thread.currentThread();
                    ClassLoader loader = thread.getContextClassLoader();
                    thread.setContextClassLoader(Client.class.getClassLoader());
                    try {
                        tempIndexes.add(new ESQueryIndex(ESUtils.getClient()));
                    } finally {
                        thread.setContextClassLoader(loader);
                    }
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
        return tempIndexes;
    }
}
