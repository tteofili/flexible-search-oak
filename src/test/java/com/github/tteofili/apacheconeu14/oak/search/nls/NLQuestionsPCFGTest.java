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

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Testcase for {@link com.github.tteofili.apacheconeu14.oak.search.nls.NLQuestionsPCFG}
 */
public class NLQuestionsPCFGTest {

    @Test
    public void testQuestion() throws Exception {
        NLQuestionsPCFG nlQuestionsPCFG = new NLQuestionsPCFG();
        String filteredQuestion = nlQuestionsPCFG.filterQuestion("what do you see");
        assertNotNull(filteredQuestion);
        filteredQuestion = nlQuestionsPCFG.filterQuestion("what is the repository");
        assertNotNull(filteredQuestion);
        filteredQuestion = nlQuestionsPCFG.filterQuestion("who is the admin");
        assertNotNull(filteredQuestion);
    }
}
