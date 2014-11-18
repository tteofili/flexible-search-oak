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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.oak.commons.PathUtils.concat;

/**
 * Demo code for indexing data into Elasticsearch
 */
public class ESIndexEditor implements IndexEditor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Client client;
    private final ESIndexEditor parent;
    private final String name;

    private boolean changed;

    private String path;

    public ESIndexEditor(Client client) {
        this.client = client;
        path = "/";
        parent = null;
        name = null;
    }

    public ESIndexEditor(ESIndexEditor parent, String name) {
        this.client = parent.client;
        this.path = null;
        this.parent = parent;
        this.name = name;
    }

    @Override
    public void enter(NodeState before, NodeState after) throws CommitFailedException {

    }

    private String getPath() {
        if (parent != null && name != null) {
            path = concat(parent.getPath(), name);
        }
        return path;
    }

    @Override
    public void leave(NodeState before, NodeState after) throws CommitFailedException {
        if (changed) {
            String source = jsonFromState(after);
            try {
                IndexResponse response = client.prepareIndex("oak", "node", getPath())
                        .setSource(source)
                        .execute()
                        .actionGet();

                if (response.isCreated()) {
                    log.info("indexed doc {}", source);
                }
            } catch (Exception e) {
                log.error("failed indexing {}", after);
            }
        }
    }

    private String jsonFromState(NodeState after) {
        StringBuilder json = new StringBuilder("{" +
                "\"path\":\"" + getPath() + "\"");

        for (PropertyState ps : after.getProperties()) {
            json.append(",\"").append(ps.getName()).append("\":\"").append(ps.getValue(ps.getType())).append("\"");
        }
        json.append("}");


        return json.toString();
    }

    @Override
    public void propertyAdded(PropertyState propertyState) throws CommitFailedException {
        changed = true;
    }

    @Override
    public void propertyChanged(PropertyState before, PropertyState after) throws CommitFailedException {
        changed = true;
    }

    @Override
    public void propertyDeleted(PropertyState propertyState) throws CommitFailedException {
        changed = true;
    }

    @Override
    public Editor childNodeAdded(String name, NodeState nodeState) throws CommitFailedException {
        return new ESIndexEditor(this, name);
    }

    @Override
    public Editor childNodeChanged(String name, NodeState before, NodeState after) throws CommitFailedException {
        return new ESIndexEditor(this, name);
    }

    @Override
    public Editor childNodeDeleted(String s, NodeState nodeState) throws CommitFailedException {
        // TODO : implement deletion
        return null;
    }
}
