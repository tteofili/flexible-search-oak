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

import org.apache.jackrabbit.oak.api.PropertyValue;
import org.apache.jackrabbit.oak.spi.query.Cursor;
import org.apache.jackrabbit.oak.spi.query.Filter;
import org.apache.jackrabbit.oak.spi.query.IndexRow;
import org.apache.jackrabbit.oak.spi.query.PropertyValues;
import org.apache.jackrabbit.oak.spi.query.QueryIndex;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

/**
 * Sample query index for Elasticsearch
 */
public class ESQueryIndex implements QueryIndex {

    private static final String NATIVE_ES_QUERY = "es";

    private final Client client;

    public ESQueryIndex(Client client) {
        this.client = client;
    }

    @Override
    public double getCost(Filter filter, NodeState nodeState) {
        // only allow native query language
        if (filter.getPropertyRestriction(NATIVE_ES_QUERY) != null) {
            return 1;
        } else {
            return Double.POSITIVE_INFINITY;
        }
    }

    @Override
    public Cursor query(Filter filter, NodeState nodeState) {
        Filter.PropertyRestriction nativeQueryRestriction = filter.getPropertyRestriction(NATIVE_ES_QUERY);
        String nativeQueryString = String.valueOf(nativeQueryRestriction.first.getValue(nativeQueryRestriction.first.getType()));
        SearchResponse response = client.prepareSearch("oak")
                .setTypes("node")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.queryString(nativeQueryString))
                .setFrom(0).setSize(60).setExplain(true)
                .execute()
                .actionGet();

        final SearchHits searchHits = response.getHits();
        return new Cursor() {

            private int index = 0;

            @Override
            public IndexRow next() {
                final SearchHit searchHit = searchHits.getAt(index);
                index++;

                return new IndexRow() {
                    @Override
                    public String getPath() {
                        return String.valueOf(searchHit.getSource().get("path"));
                    }

                    @Override
                    public PropertyValue getValue(String s) {
                        return PropertyValues.newString(String.valueOf(searchHit.getSource().get(s)));
                    }
                };
            }

            @Override
            public boolean hasNext() {
                return index < searchHits.hits().length;
            }

            @Override
            public void remove() {
                // do nothing
            }
        };
    }

    @Override
    public String getPlan(Filter filter, NodeState nodeState) {
        return null;
    }

    @Override
    public String getIndexName() {
        return NATIVE_ES_QUERY;
    }
}
