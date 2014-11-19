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

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.elasticsearch.common.lucene.Lucene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for fetching the underlying Lucene index
 */
public class IndexUtils {

    private static Logger log = LoggerFactory.getLogger(IndexUtils.class);

    private static Directory directory = openDir();
    private static IndexWriter cachedIndexWriter;

    private static FSDirectory openDir() {
        try {
            File path = new File("/tmp/nls-lucene");
            if (!path.exists()) {
                assert path.mkdirs();
            }
            return FSDirectory.open(path);
        } catch (IOException e) {
            log.error("could not open /tmp/nls-lucene", e);
        }
        return null;
    }

    public static IndexWriter getWriter() {
        if (directory == null) {
            directory = openDir();
        }
        try {
            cachedIndexWriter = new IndexWriter(directory, new IndexWriterConfig(Lucene.VERSION, new StandardAnalyzer()));
        } catch (Exception e) {
            log.error("could not create index writer", e);
        }
        return cachedIndexWriter;
    }

    public static IndexSearcher getSearcher() {
        if (directory == null) {
            directory = openDir();
        }
        try {
            return new IndexSearcher(DirectoryReader.open(directory));
        } catch (Exception e) {
            log.error("could not create index searcher", e);
        }
        return null;
    }
}
