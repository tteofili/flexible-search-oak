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

import javax.annotation.Nonnull;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.index.IndexConstants;
import org.apache.jackrabbit.oak.plugins.index.IndexUpdateProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.query.QueryEngineSettings;
import org.apache.jackrabbit.oak.query.ast.Operator;
import org.apache.jackrabbit.oak.query.ast.SelectorImpl;
import org.apache.jackrabbit.oak.query.index.FilterImpl;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.commit.EditorHook;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.query.Cursor;
import org.apache.jackrabbit.oak.spi.query.Filter;
import org.apache.jackrabbit.oak.spi.query.IndexRow;
import org.apache.jackrabbit.oak.spi.query.PropertyValues;
import org.apache.jackrabbit.oak.spi.query.QueryIndex;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
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
import static org.apache.jackrabbit.oak.plugins.memory.EmptyNodeState.EMPTY_NODE;
import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link com.github.tteofili.apacheconeu14.oak.search.es.ESIndexEditor}
 */
public class NLSIndexIntegrationTest {

    protected NodeStore store;
    protected EditorHook hook;
    private ContentRepository repository;

    @Before
    public void setUp() throws Exception {
        store = new SegmentNodeStore();
        hook = new EditorHook(new IndexUpdateProvider(new NLSIndexEditorProvider()));
        Oak oak = new Oak().with(new InitialContent())
                .with(new OpenSecurityProvider())
                .with(new RepositoryInitializer() {
                    @Override
                    public void initialize(@Nonnull NodeBuilder builder) {
                        String type = "nls";
                        if (builder.hasChildNode(IndexConstants.INDEX_DEFINITIONS_NAME)
                                && !builder.getChildNode(IndexConstants.INDEX_DEFINITIONS_NAME).hasChildNode(type)) {
                            NodeBuilder indexDefinitionsNode = builder.getChildNode(IndexConstants.INDEX_DEFINITIONS_NAME);
                            if (!indexDefinitionsNode.hasChildNode(type)) {
                                NodeBuilder esIndexDefinitionNode = indexDefinitionsNode.child(type);
                                esIndexDefinitionNode.setProperty(JcrConstants.JCR_PRIMARYTYPE, IndexConstants.INDEX_DEFINITIONS_NODE_TYPE, Type.NAME)
                                        .setProperty(IndexConstants.TYPE_PROPERTY_NAME, type)
                                        .setProperty(IndexConstants.REINDEX_PROPERTY_NAME, true);
                                esIndexDefinitionNode.setProperty(IndexConstants.ASYNC_PROPERTY_NAME, "async");
                            }

                        }
                    }
                })
                .with(new NLSQueryIndexProvider())
                .with(hook);
        repository = oak
                .createContentRepository();
    }

    @After
    public void tearDown() throws Exception {
        IndexUtils.getWriter().deleteAll();
        IndexUtils.getWriter().commit();
    }

    @Test
    public void testNodeIndexingAndQuery() throws Exception {
        NodeState root = EMPTY_NODE;

        NodeBuilder builder = root.builder();
        builder.child("oak:index").child("es")
                .setProperty(JCR_PRIMARYTYPE, "oak:QueryIndexDefinition")
                .setProperty("type", "nls");

        NodeState before = builder.getNodeState();
        builder.child("newnode").setProperty("jcr:description", "a repository is quite a thing").setProperty("jcr:primaryType", "nt:unstructured");
        NodeState after = builder.getNodeState();

        NodeState indexed = hook.processCommit(before, after, CommitInfo.EMPTY);

        QueryIndex queryIndex = new NLSQueryIndex();
        FilterImpl filter = new FilterImpl(mock(SelectorImpl.class), "", new QueryEngineSettings());
        filter.restrictPath("/newnode", Filter.PathRestriction.EXACT);
        filter.restrictProperty("nls", Operator.EQUAL,
                PropertyValues.newString("what is the repository"));
        Cursor cursor = queryIndex.query(filter, indexed);
        assertNotNull(cursor);
        assertTrue("no results found", cursor.hasNext());
        IndexRow next = cursor.next();
        assertNotNull("first returned item should not be null", next);
        assertEquals("/newnode", next.getPath());
        assertFalse(cursor.hasNext());
    }
}
