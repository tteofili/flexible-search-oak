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

import org.apache.jackrabbit.oak.spi.query.Cursor;
import org.apache.jackrabbit.oak.spi.query.Filter;
import org.apache.jackrabbit.oak.spi.query.IndexRow;
import org.apache.jackrabbit.oak.spi.query.QueryIndex;
import org.apache.jackrabbit.oak.spi.state.NodeState;

/**
 * Sample query index for NLS
 */
public class NLSQueryIndex implements QueryIndex {

    private static final String NATIVE_NLS_QUERY = "nls";

    private final NLQuestionsPCFG pcfg;

    public NLSQueryIndex() {
        pcfg = new NLQuestionsPCFG();
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
        Filter.PropertyRestriction nativeQueryRestriction = filter.getPropertyRestriction(NATIVE_NLS_QUERY);
        String nativeQueryString = String.valueOf(nativeQueryRestriction.first.getValue(nativeQueryRestriction.first.getType()));
        String purgedQuery = pcfg.filterQuestion(nativeQueryString);


        return new Cursor() {
            @Override
            public IndexRow next() {
                return null;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public void remove() {

            }
        };
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
