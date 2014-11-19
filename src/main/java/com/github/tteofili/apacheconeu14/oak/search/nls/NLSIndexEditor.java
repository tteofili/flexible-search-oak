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
package com.github.tteofili.apacheconeu14.oak.search.nls;

import java.io.IOException;
import java.io.StringReader;

import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.plugins.index.IndexEditor;
import org.apache.jackrabbit.oak.spi.commit.Editor;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.oak.commons.PathUtils.concat;

/**
 * Demo code for indexing data for NLS
 */
public class NLSIndexEditor implements IndexEditor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;
    private final IndexWriter writer;

    private boolean changed;

    private String path;

    private NLSIndexEditor parent;

    public NLSIndexEditor(NLSIndexEditor parent, String name) {
        this.parent = parent;
        this.writer = parent.writer;
        this.name = name;
        this.path = null;
    }

    public NLSIndexEditor(IndexWriter writer) {
        this.writer = writer;
        name = null;
        path = "/";
    }

    private String getPath() {
        if (parent != null && name != null) {
            path = concat(parent.getPath(), name);
        }
        return path;
    }

    @Override
    public void enter(NodeState nodeState, NodeState nodeState2) throws CommitFailedException {

    }

    @Override
    public void leave(NodeState nodeState, NodeState nodeState2) throws CommitFailedException {
        if (changed) {
            try {
                String path = getPath();
                Document d = makeDocument(path, nodeState);
                if (d != null) {
                    try {
                        writer.updateDocument(newPathTerm(path), d);
                    } catch (IOException e) {
                        log.error("could not index doc at path {}", path, e);
                    }
                }
            } finally {
                // do nothing
            }
        }

    }

    private Term newPathTerm(String path) {
        if (!"/".equals(path) && !path.startsWith("/")) {
            path = "/" + path;
        }
        return new Term("path", path);
    }

    private Document makeDocument(String path, NodeState nodeState) {
        Document d = new Document();
        d.add(new TextField("path", new StringReader(path)));
        for (PropertyState property : nodeState.getProperties()) {
            d.add(new TextField(property.getName(), new StringReader(String.valueOf(property.getValue(property.getType())))));
        }
        return d;
    }

    @Override
    public void propertyAdded(PropertyState propertyState) throws CommitFailedException {
        changed = true;
    }

    @Override
    public void propertyChanged(PropertyState propertyState, PropertyState propertyState2) throws CommitFailedException {
        changed = true;
    }

    @Override
    public void propertyDeleted(PropertyState propertyState) throws CommitFailedException {
        changed = true;
    }

    @Override
    public Editor childNodeAdded(String name, NodeState nodeState) throws CommitFailedException {
        return new NLSIndexEditor(this, name);
    }

    @Override
    public Editor childNodeChanged(String name, NodeState nodeState, NodeState nodeState2) throws CommitFailedException {
        return new NLSIndexEditor(this, name);
    }

    @Override
    public Editor childNodeDeleted(String s, NodeState nodeState) throws CommitFailedException {
        // TODO : implement this
        return null;
    }
}
