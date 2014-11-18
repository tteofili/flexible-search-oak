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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.plugins.index.IndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.IndexUpdateCallback;
import org.apache.jackrabbit.oak.spi.commit.Editor;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.elasticsearch.client.Client;

/**
 * provider for {@link com.github.tteofili.apacheconeu14.oak.search.es.ESIndexEditor}
 */
@Component(immediate = true)
@Service(value = IndexEditorProvider.class)
public class ESIndexEditorProvider implements IndexEditorProvider {
    @Override
    public Editor getIndexEditor(@Nonnull String type, @Nonnull NodeBuilder nodeBuilder, @Nonnull NodeState nodeState, @Nonnull IndexUpdateCallback indexUpdateCallback) throws CommitFailedException {
        Thread thread = Thread.currentThread();
        ClassLoader loader = thread.getContextClassLoader();
        thread.setContextClassLoader(Client.class.getClassLoader());
        try {
            return "es".equals(type) ? new ESIndexEditor(ESUtils.getClient()) : null;
        } finally {
            thread.setContextClassLoader(loader);
        }

    }
}