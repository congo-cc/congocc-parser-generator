/* Copyright (c) 2008-2021 Jonathan Revusky, revusky@javacc.com
 * Copyright (c) 2006, Sun Microsystems Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notices,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name Jonathan Revusky, Sun Microsystems, Inc.
 *       nor the names of any contributors may be used to endorse or promote
 *       products derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.congocc.core;

import java.util.*;

import org.congocc.Grammar;
import org.congocc.parser.tree.RegexpChoice;
import org.congocc.parser.tree.RegexpSpec;
import org.congocc.parser.tree.RegexpStringLiteral;
import org.congocc.parser.tree.TokenProduction;

public class LexicalStateData {

    private Grammar grammar;
    private LexerData lexerData;
    private String name;

    private List<TokenProduction> tokenProductions = new ArrayList<>();

    private Map<Set<NfaState>, CompositeStateSet> canonicalSets = new HashMap<>();

    private Map<String, RegularExpression> caseSensitiveTokenTable = new HashMap<>();
    private Map<String, RegularExpression> caseInsensitiveTokenTable = new HashMap<>();

    private HashSet<RegularExpression> regularExpressions = new HashSet<>();

    private NfaState initialState;

    Set<NfaState> allStates = new HashSet<>();
    
    public LexicalStateData(Grammar grammar, String name) {
        this.grammar = grammar;
        this.lexerData = grammar.getLexerData();
        this.name = name;
        initialState = new NfaState(this);
    }

    Grammar getGrammar() {
        return grammar;
    }

    boolean isEmpty() {
        return regularExpressions.isEmpty();
    }
   
    public NfaState getInitialState() {return initialState;}

    public String getName() {return name;}

    public Collection<NfaState> getAllNfaStates() {
        List<NfaState> result = new ArrayList<>(allStates);
        Collections.sort(result, (first,second)->first.index-second.index);
        return result;
    }

    void addTokenProduction(TokenProduction tokenProduction) {
        tokenProductions.add(tokenProduction);
    }

    boolean containsRegularExpression(RegularExpression re) {
        return regularExpressions.contains(re);
    }

    void addStringLiteral(RegexpStringLiteral re) {
        if (re.getIgnoreCase()) {
            caseInsensitiveTokenTable.put(re.getImage().toUpperCase(), re);
        } else {
            caseSensitiveTokenTable.put(re.getImage(), re);
        }
    }

    RegularExpression getStringLiteral(String image) {
        RegularExpression result = caseSensitiveTokenTable.get(image);
        if (result == null) {
            result = caseInsensitiveTokenTable.get(image.toUpperCase());
        }
        return result;
    }

    NfaState getCanonicalComposite(Set<NfaState> stateSet) {
        assert stateSet.size() >1;
        if (stateSet.size() == 1) {
            return stateSet.iterator().next();
        }
        CompositeStateSet result = canonicalSets.get(stateSet);
        if (result == null) {
            result = new CompositeStateSet(stateSet, this);
            canonicalSets.put(stateSet, result);
        }
        return result;
    }

    List<RegexpChoice> process() {
    	List<RegexpChoice> choices = new ArrayList<>();
        boolean isFirst = true;
        for (TokenProduction tp : tokenProductions) {
            choices.addAll(processTokenProduction(tp, isFirst));
            isFirst = false;
        }
        generateData();
        return choices;
    }

    void generateData() {
        for (NfaState state : allStates) {
            state.doEpsilonClosure();
        }
        addCompositeStates();
        indexStates();
    }

    void addCompositeStates() {
        for (NfaState state : new ArrayList<>(allStates))  {
            NfaState canonicalState = state.getCanonicalState();
            if (state != canonicalState) {
                allStates.add(state.getCanonicalState());
                allStates.remove(state);
            }
        }
    }

    void indexStates() {
        // Make sure that the index of the starting state is zero.
        initialState = initialState.getCanonicalState();
        initialState.index = 0;
        int idx = 1;
        Set<NfaState> statesInComposite = new HashSet<>();
        for (NfaState state : allStates) {
            if (state.index!=0 && state.isComposite()) {
                state.index = idx++;
                statesInComposite.addAll(((CompositeStateSet) state).states);
            }
        }
        for (NfaState state : allStates) {
            if (state.index!=0 
                && !state.isComposite()
                &&state.isMoveCodeNeeded() 
                && !statesInComposite.contains(state)) {
                   state.index = idx++;
            }
        }
        for (NfaState state : statesInComposite) {
            state.index = idx++;
        }
        allStates.removeIf(state->state.index<0);
    }

    List<RegexpChoice> processTokenProduction(TokenProduction tp, boolean isFirst) {
        boolean ignore = tp.isIgnoreCase() || grammar.isIgnoreCase();//REVISIT
        List<RegexpChoice> choices = new ArrayList<>();
        for (RegexpSpec respec : tp.getRegexpSpecs()) {
            RegularExpression currentRegexp = respec.getRegexp();
            if (currentRegexp.isPrivate()) {
                continue;
            }
            regularExpressions.add(currentRegexp);
            if (currentRegexp instanceof RegexpChoice) {
                choices.add((RegexpChoice) currentRegexp);
            }
            new NfaBuilder(this, ignore).buildStates(currentRegexp);
            if (respec.getNextState() != null && !respec.getNextState().equals(this.name))
                currentRegexp.setNewLexicalState(lexerData.getLexicalState(respec.getNextState()));

            if (respec.getCodeSnippet() != null && !respec.getCodeSnippet().isEmpty()) {
                currentRegexp.setCodeSnippet(respec.getCodeSnippet());
            }
        }
        return choices;
    }
}
