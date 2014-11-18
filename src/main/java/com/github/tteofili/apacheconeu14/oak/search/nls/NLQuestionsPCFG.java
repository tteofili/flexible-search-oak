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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.opennlp.utils.cfg.ProbabilisticContextFreeGrammar;
import org.apache.opennlp.utils.cfg.Rule;

/**
 * A PCFG for questions in English
 */
public class NLQuestionsPCFG {

    private final ProbabilisticContextFreeGrammar grammar;

    public NLQuestionsPCFG() {
        List<String> nonTerminals = new LinkedList<String>();
        nonTerminals.add("S");
        nonTerminals.add("NP");
        nonTerminals.add("VP");
        nonTerminals.add("DT");
        nonTerminals.add("Vi");
        nonTerminals.add("Vt");
        nonTerminals.add("NN");
        nonTerminals.add("IN");
        nonTerminals.add("NNP");
        nonTerminals.add("Adv");
        nonTerminals.add("E");
        nonTerminals.add("Q");
        nonTerminals.add("R");
        nonTerminals.add("VVP");
        nonTerminals.add("PVP");
        nonTerminals.add("AVP");
        nonTerminals.add("D");
        nonTerminals.add("PN");

        String startSymbol = "S";

        List<String> terminals = new LinkedList<String>();
        terminals.add("work");
        terminals.add("see");
        terminals.add("are");
        terminals.add("is");
        terminals.add("I");
        terminals.add("you");
        terminals.add("he");
        terminals.add("she");
        terminals.add("we");
        terminals.add("they");
        terminals.add("it");
        terminals.add("how");
        terminals.add("how much");
        terminals.add("which");
        terminals.add("what");
        terminals.add("who");
        terminals.add("Lucene");
        terminals.add("Oak");
        terminals.add("admin");
        terminals.add("there");
        terminals.add("repository");
        terminals.add("");

        Map<Rule, Double> rules = new HashMap<Rule, Double>();
        rules.put(new Rule("S", "Q", "R"), 1d);
        rules.put(new Rule("E", ""), 1d);
        rules.put(new Rule("Q", "how"), 0.2d);
        rules.put(new Rule("Q", "how much"), 0.1d);
        rules.put(new Rule("Q", "which"), 0.3d);
        rules.put(new Rule("Q", "what"), 0.2d);
        rules.put(new Rule("Q", "who"), 0.2d);
        rules.put(new Rule("R", "VVP", "NP"), 1d);
        rules.put(new Rule("R", "VVP", "Adv"), 1d);
        rules.put(new Rule("VVP", "PVP", "AVP"), 0.4d);
        rules.put(new Rule("VVP", "PVP", "VP"), 0.3d);
        rules.put(new Rule("VVP", "VP", "E"), 0.3d);
        rules.put(new Rule("PVP", "D", "PN"), 0.4d);
        rules.put(new Rule("D", "do"), 0.7d);
        rules.put(new Rule("D", "does"), 0.3d);
        rules.put(new Rule("PN", "I"), 0.1d);
        rules.put(new Rule("PN", "you"), 0.4d);
        rules.put(new Rule("PN", "he"), 0.1d);
        rules.put(new Rule("PN", "she"), 0.1d);
        rules.put(new Rule("PN", "it"), 0.1d);
        rules.put(new Rule("PN", "we"), 0.1d);
        rules.put(new Rule("PN", "they"), 0.1d);
        rules.put(new Rule("AVP", "Adv", "Vi"), 0.3);
        rules.put(new Rule("AVP", "Adv", "Vt"), 0.2);
        rules.put(new Rule("VP", "Vi", "E"), 0.2);
        rules.put(new Rule("VP", "Vt", "E"), 0.3);
        rules.put(new Rule("Vi", "work"), 1d);
        rules.put(new Rule("Vi", "is"), 1d);
        rules.put(new Rule("Vi", "are"), 1d);
        rules.put(new Rule("Vt", "see"), 1d);
        rules.put(new Rule("NP", "DT", "NN"), 0.5d);
        rules.put(new Rule("NP", "NNP", "E"), 0.5d);
        rules.put(new Rule("NNP", "Lucene"), 0.3);
        rules.put(new Rule("NNP", "Oak"), 0.3);
        rules.put(new Rule("NN", "repository"), 0.3);
        rules.put(new Rule("DT", "the"), 1d);
        rules.put(new Rule("IN", "with"), 0.2);
        rules.put(new Rule("IN", "in"), 0.1);
        rules.put(new Rule("IN", "for"), 0.4);
        rules.put(new Rule("IN", "of"), 0.4);
        rules.put(new Rule("NN", "admin"), 0.1);
        rules.put(new Rule("Adv", "badly"), 0.3);
        rules.put(new Rule("Adv", "nicely"), 0.6);
        rules.put(new Rule("Adv", "there"), 0.1);

        grammar = new ProbabilisticContextFreeGrammar(nonTerminals, terminals, rules, startSymbol);
    }

    public String filterQuestion(String sentence) {
        List<String> whitespacedSentence = Arrays.asList(sentence.split(" "));
        ProbabilisticContextFreeGrammar.BackPointer backPointer = grammar.cky(whitespacedSentence);

        if (backPointer != null) {
            return getRightmostSubtree(backPointer);
        } else {
            return null;
        }
    }

    private String getRightmostSubtree(ProbabilisticContextFreeGrammar.BackPointer backPointer) {
        if (backPointer.getRightTree() == null) {
            return backPointer.toString();
        } else {
            return getRightmostSubtree(backPointer.getRightTree());
        }
    }

}
