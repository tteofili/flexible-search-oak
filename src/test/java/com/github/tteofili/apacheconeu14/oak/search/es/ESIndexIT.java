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
import javax.jcr.NoSuchWorkspaceException;
import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.index.IndexConstants;
import org.apache.jackrabbit.oak.plugins.index.IndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.IndexUpdateCallback;
import org.apache.jackrabbit.oak.plugins.index.IndexUpdateProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.query.QueryEngineSettings;
import org.apache.jackrabbit.oak.query.ast.Operator;
import org.apache.jackrabbit.oak.query.ast.SelectorImpl;
import org.apache.jackrabbit.oak.query.index.FilterImpl;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.commit.Editor;
import org.apache.jackrabbit.oak.spi.commit.EditorHook;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.query.Cursor;
import org.apache.jackrabbit.oak.spi.query.Filter;
import org.apache.jackrabbit.oak.spi.query.IndexRow;
import org.apache.jackrabbit.oak.spi.query.PropertyValues;
import org.apache.jackrabbit.oak.spi.query.QueryIndex;
import org.apache.jackrabbit.oak.spi.query.QueryIndexProvider;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.apache.jackrabbit.oak.spi.state.ChildNodeEntry;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.TYPE_PROPERTY_NAME;
import static org.apache.jackrabbit.oak.plugins.memory.EmptyNodeState.EMPTY_NODE;
import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link com.github.tteofili.apacheconeu14.oak.search.es.ESIndexEditor}
 */
public class ESIndexIT {

    protected NodeStore store;
    protected EditorHook hook;
    private ContentRepository repository;

    @Before
    public void setUp() throws Exception {
        store = new SegmentNodeStore();
        hook = new EditorHook(new IndexUpdateProvider(
                new IndexEditorProvider() {
                    @Override
                    public Editor getIndexEditor(@Nonnull String type, @Nonnull NodeBuilder nodeBuilder, @Nonnull NodeState nodeState,
                                                 @Nonnull IndexUpdateCallback indexUpdateCallback) throws CommitFailedException {
                        return "es".equals(type) ? new ESIndexEditor(ESUtils.getClient()) : null;
                    }
                }));
        Oak oak = new Oak().with(new InitialContent())
                .with(new OpenSecurityProvider())
                .with(new RepositoryInitializer() {
                    @Override
                    public void initialize(@Nonnull NodeBuilder builder) {
                        if (builder.hasChildNode(IndexConstants.INDEX_DEFINITIONS_NAME)
                                && !builder.getChildNode(IndexConstants.INDEX_DEFINITIONS_NAME).hasChildNode("es")) {
                            NodeBuilder indexDefinitionsNode = builder.getChildNode(IndexConstants.INDEX_DEFINITIONS_NAME);
                            if (!indexDefinitionsNode.hasChildNode("es")) {
                                NodeBuilder esIndexDefinitionNode = indexDefinitionsNode.child("es");
                                esIndexDefinitionNode.setProperty(JcrConstants.JCR_PRIMARYTYPE, IndexConstants.INDEX_DEFINITIONS_NODE_TYPE, Type.NAME)
                                        .setProperty(IndexConstants.TYPE_PROPERTY_NAME, "es")
                                        .setProperty(IndexConstants.REINDEX_PROPERTY_NAME, true);
                                esIndexDefinitionNode.setProperty(IndexConstants.ASYNC_PROPERTY_NAME, "async");
                            }

                        }
                    }
                })
                .with(new QueryIndexProvider() {
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
                                    tempIndexes.add(new ESQueryIndex(ESUtils.getClient()));
                                } catch (Exception e) {
                                    // do nothing
                                }

                            }
                        }
                        return tempIndexes;
                    }
                })
                .with(hook);
        repository = oak
                .createContentRepository();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSingleNodeCreation() throws Exception {
        NodeState root = EMPTY_NODE;

        NodeBuilder builder = root.builder();
        builder.child("oak:index").child("es")
                .setProperty(JCR_PRIMARYTYPE, "oak:QueryIndexDefinition")
                .setProperty("type", "es");

        NodeState before = builder.getNodeState();
        builder.child("newnode").setProperty("prop", "val");
        NodeState after = builder.getNodeState();

        NodeState indexed = hook.processCommit(before, after, CommitInfo.EMPTY);

        QueryIndex queryIndex = new ESQueryIndex(ESUtils.getClient());
        FilterImpl filter = new FilterImpl(mock(SelectorImpl.class), "", new QueryEngineSettings());
        filter.restrictPath("/newnode", Filter.PathRestriction.EXACT);
        filter.restrictProperty("es", Operator.EQUAL,
                PropertyValues.newString("+val"));
        Cursor cursor = queryIndex.query(filter, indexed);
        assertNotNull(cursor);
        assertTrue("no results found", cursor.hasNext());
        IndexRow next = cursor.next();
        assertNotNull("first returned item should not be null", next);
        assertEquals("/newnode", next.getPath());
        assertFalse(cursor.hasNext());
    }
}
