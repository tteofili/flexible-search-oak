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

import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.plugins.index.IndexEditor;
import org.apache.jackrabbit.oak.spi.commit.Editor;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;


/**
 * Demo code for indexing data into Elasticsearch
 */
public class ESIndexEditor implements IndexEditor {


    private final Client client;

    public ESIndexEditor(Client client) {
        this.client = client;
    }

    @Override
    public void enter(NodeState before, NodeState after) throws CommitFailedException {

    }

    @Override
    public void leave(NodeState before, NodeState after) throws CommitFailedException {
        String json = "{" +
                "\"user\":\"kimchy\"," +
                "\"postDate\":\"2013-01-30\"," +
                "\"message\":\"trying out Elasticsearch\"" +
                "}";

        IndexResponse response = client.prepareIndex("twitter", "tweet")
                .setSource(json)
                .execute()
                .actionGet();

        if (response.isCreated()) {
            // ok
        }
    }

    @Override
    public void propertyAdded(PropertyState propertyState) throws CommitFailedException {

    }

    @Override
    public void propertyChanged(PropertyState before, PropertyState after) throws CommitFailedException {

    }

    @Override
    public void propertyDeleted(PropertyState propertyState) throws CommitFailedException {

    }

    @Override
    public Editor childNodeAdded(String s, NodeState nodeState) throws CommitFailedException {
        return null;
    }

    @Override
    public Editor childNodeChanged(String s, NodeState before, NodeState after) throws CommitFailedException {
        return null;
    }

    @Override
    public Editor childNodeDeleted(String s, NodeState nodeState) throws CommitFailedException {
        return null;
    }
}
