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

import org.apache.jackrabbit.oak.api.PropertyValue;
import org.apache.jackrabbit.oak.spi.query.Cursor;
import org.apache.jackrabbit.oak.spi.query.Filter;
import org.apache.jackrabbit.oak.spi.query.IndexRow;
import org.apache.jackrabbit.oak.spi.query.PropertyValues;
import org.apache.jackrabbit.oak.spi.query.QueryIndex;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.classification.Classifier;
import org.apache.lucene.classification.KNearestNeighborClassifier;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.BoostedQuery;
import org.apache.lucene.queries.function.valuesource.ConstValueSource;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.client.Client;

/**
 * Sample query index for NLS
 */
public class NLSQueryIndex implements QueryIndex {

    private static final String NATIVE_NLS_QUERY = "nls";

    private final NLQuestionsPCFG pcfg;
    private final Classifier<BytesRef> classifier;

    public NLSQueryIndex() {
        pcfg = new NLQuestionsPCFG();
        classifier = new KNearestNeighborClassifier(1);
    }

    @Override
    public double getCost(Filter filter, NodeState nodeState) {
        // only allow native query language
        if (filter.getPropertyRestriction(NATIVE_NLS_QUERY) != null) {
            return 1;
        } else {
            return Double.POSITIVE_INFINITY;
        }
    }

    @Override
    public Cursor query(Filter filter, NodeState nodeState) {

        Thread thread = Thread.currentThread();
        ClassLoader loader = thread.getContextClassLoader();
        thread.setContextClassLoader(Client.class.getClassLoader());
        try {
            final IndexSearcher searcher = IndexUtils.getSearcher();

            if (searcher != null) {
                try {
                    classifier.train(SlowCompositeReaderWrapper.wrap(searcher.getIndexReader()), "jcr:title", "jcr:primaryType", new StandardAnalyzer());
                } catch (IOException e) {
                    // error in training
                }

                try {
                    classifier.train(SlowCompositeReaderWrapper.wrap(searcher.getIndexReader()), "jcr:title", "jcr:primaryType", new StandardAnalyzer());
                } catch (IOException e) {
                    // error in training
                }

                Filter.PropertyRestriction nativeQueryRestriction = filter.getPropertyRestriction(NATIVE_NLS_QUERY);
                String nativeQueryString = String.valueOf(nativeQueryRestriction.first.getValue(nativeQueryRestriction.first.getType()));

                // build the parse tree of the query and filter the uninteresting part (e.g. "who is the admin" -> "admin")
                String purgedQuery = pcfg.filterQuestion(nativeQueryString);

                BooleanQuery booleanClauses = new BooleanQuery();

                // add clauses for the purged natural language query (if existing)
                if (purgedQuery != null) {
                    booleanClauses.add(new BooleanClause(new TermQuery(new Term("jcr:title", purgedQuery)), BooleanClause.Occur.SHOULD));
                    booleanClauses.add(new BooleanClause(new TermQuery(new Term("jcr:description", purgedQuery)), BooleanClause.Occur.SHOULD));
                    booleanClauses.add(new BooleanClause(new TermQuery(new Term("text", purgedQuery)), BooleanClause.Occur.SHOULD));
                }
                // infer "class" of the query and boost based on that
                try {
                    ClassificationResult<BytesRef> result = null;
                    try {
                        result = classifier.assignClass(nativeQueryString);
                    } catch (Exception e) {
                        // do nothing
                    }
                    if (result != null) {
                        booleanClauses.add(new BooleanClause(new BoostedQuery(new TermQuery(new Term("jcr:primaryType", result.getAssignedClass())),
                                new ConstValueSource(2.0f)), BooleanClause.Occur.SHOULD));
                    }

                    final TopDocs topDocs = searcher.search(booleanClauses, 100);
                    final ScoreDoc[] scoreDocs = topDocs.scoreDocs;

                    return new Cursor() {
                        private int index = 0;

                        @Override
                        public IndexRow next() {

                            final ScoreDoc scoreDoc = scoreDocs[index];

                            index++;
                            return new IndexRow() {
                                @Override
                                public String getPath() {
                                    try {
                                        return searcher.doc(scoreDoc.doc).get("path");
                                    } catch (IOException e) {
                                        return null;
                                    }
                                }

                                @Override
                                public PropertyValue getValue(String s) {
                                    try {
                                        if ("jcr:score".equals(s)) {
                                            PropertyValues.newString(String.valueOf(scoreDoc.score));
                                        }
                                        return PropertyValues.newString(searcher.doc(scoreDoc.doc).get(s));
                                    } catch (IOException e) {
                                        return null;
                                    }
                                }
                            };
                        }

                        @Override
                        public boolean hasNext() {
                            return index < scoreDocs.length;
                        }

                        @Override
                        public void remove() {

                        }
                    };
                } catch (IOException e) {
                    // do nothing
                }
            }
        } finally {
            thread.setContextClassLoader(loader);
        }
        return null;
    }

    @Override
    public String getPlan(Filter filter, NodeState nodeState) {
        return null;
    }

    @Override
    public String getIndexName() {
        return NATIVE_NLS_QUERY;
    }
}
