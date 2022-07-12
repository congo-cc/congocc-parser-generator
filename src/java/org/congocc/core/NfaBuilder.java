/* Copyright (c) 2020-2022 Jonathan Revusky, revusky@congocc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

 package org.congocc.core;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.congocc.Grammar;
import org.congocc.parser.Node;
import org.congocc.parser.tree.*;

/**
 * A Visitor object that builds an Nfa start and end state from a Regular expression. This is a
 * result of refactoring some legacy code that used all static methods. NB. This
 * class and the visit methods must be public because of the use of reflection.
 * Ideally, it would all be private and package-private.
 * 
 * @author revusky
 */
class NfaBuilder extends Node.Visitor {

    private NfaState start, end;
    private boolean ignoreCase;
    private LexicalStateData lexicalState;
    private Grammar grammar;

    NfaBuilder(LexicalStateData lexicalState, boolean ignoreCase) {
        this.lexicalState = lexicalState;
        this.grammar = lexicalState.getGrammar();
        this.ignoreCase = ignoreCase;
    }

    void buildStates(RegularExpression regularExpression) {
        visit(regularExpression);
        end.setType(regularExpression);
        lexicalState.getInitialState().addEpsilonMove(start);
    }

    void visit(CharacterList charList) {
        List<CharacterRange> ranges = orderedRanges(charList, ignoreCase);
        start = new NfaState(lexicalState);
        end = new NfaState(lexicalState);
        for (CharacterRange cr : ranges) {
            start.addRange(cr.left, cr.right);
        }
        start.setNextState(end);
    }

    void visit(OneOrMoreRegexp oom) {
        NfaState startState = new NfaState(lexicalState);
        NfaState finalState = new NfaState(lexicalState);
        visit(oom.getRegexp());
        startState.addEpsilonMove(this.start);
        this.end.addEpsilonMove(this.start);
        this.end.addEpsilonMove(finalState);
        this.start = startState;
        this.end = finalState;
    }

    void visit(RegexpChoice choice) {
        List<RegularExpression> choices = choice.getChoices();
        if (choices.size() == 1) {
            visit(choices.get(0));
            return;
        }
        NfaState startState = new NfaState(lexicalState);
        NfaState finalState = new NfaState(lexicalState);
        for (RegularExpression curRE : choices) {
            visit(curRE);
            startState.addEpsilonMove(this.start);
            this.end.addEpsilonMove(finalState);
        }
        this.start = startState;
        this.end = finalState;
    }

    void visit(RegexpStringLiteral stringLiteral) {
        NfaState state = end = start = new NfaState(lexicalState);
        for (int ch : stringLiteral.getImage().codePoints().toArray()) {
            state.setCharMove(ch, grammar.isIgnoreCase() || ignoreCase);
            end = new NfaState(lexicalState);
            state.setNextState(end);
            state = end;
        }
    }

    void visit(ZeroOrMoreRegexp zom) {
        NfaState startState = new NfaState(lexicalState);
        NfaState finalState = new NfaState(lexicalState);
        visit(zom.getRegexp());
        startState.addEpsilonMove(this.start);
        startState.addEpsilonMove(finalState);
        this.end.addEpsilonMove(finalState);
        this.end.addEpsilonMove(this.start);
        this.start = startState;
        this.end = finalState;
    }

    void visit(ZeroOrOneRegexp zoo) {
        NfaState startState = new NfaState(lexicalState);
        NfaState finalState = new NfaState(lexicalState);
        visit(zoo.getRegexp());
        startState.addEpsilonMove(this.start);
        startState.addEpsilonMove(finalState);
        this.end.addEpsilonMove(finalState);
        this.start = startState;
        this.end = finalState;
    }

    void visit(RegexpRef ref) {
        // REVISIT. Can the states generated
        // here be reused?
        visit(ref.getRegexp());
    }

    void visit(RegexpSequence sequence) {
        if (sequence.getUnits().size() == 1) {
            visit(sequence.getUnits().get(0));
        }
        NfaState startState = new NfaState(lexicalState);
        NfaState finalState = new NfaState(lexicalState);
        NfaState prevStartState = null;
        NfaState prevEndState = null;
        for (RegularExpression re : sequence.getUnits()) {
            visit(re);
            if (prevStartState == null) {
                startState.addEpsilonMove(this.start);
            } else {
                prevEndState.addEpsilonMove(this.start);
            }
            prevStartState = this.start;
            prevEndState = this.end;
        }
        this.end.addEpsilonMove(finalState);
        this.start = startState;
        this.end = finalState;
    }

    void visit(RepetitionRange repRange) {
        List<RegularExpression> units = new ArrayList<RegularExpression>();
        RegexpSequence seq;
        int i;
        for (i = 0; i < repRange.getMin(); i++) {
            units.add(repRange.getRegexp());
        }
        if (repRange.hasMax() && repRange.getMax() == -1) { // Unlimited
            ZeroOrMoreRegexp zom = new ZeroOrMoreRegexp();
            zom.setGrammar(grammar);
            zom.setRegexp(repRange.getRegexp());
            units.add(zom);
        }
        while (i++ < repRange.getMax()) {
            ZeroOrOneRegexp zoo = new ZeroOrOneRegexp();
            zoo.setGrammar(grammar);
            zoo.setRegexp(repRange.getRegexp());
            units.add(zoo);
        }
        seq = new RegexpSequence();
        seq.setGrammar(grammar);
        seq.setOrdinal(Integer.MAX_VALUE);
        for (RegularExpression re : units) {
            seq.addChild(re);
        }
        visit(seq);
    }

    static private List<CharacterRange> orderedRanges(CharacterList charList, boolean caseNeutral) {
        BitSet bs = rangeListToBS(charList.getDescriptors());
        if (caseNeutral) {
            BitSet upperCaseDiffPoints = (BitSet) bs.clone();
            BitSet lowerCaseDiffPoints = (BitSet) bs.clone();
            upperCaseDiffPoints.and(upperCaseDiffSet);
            lowerCaseDiffPoints.and(lowerCaseDiffSet);
            upperCaseDiffPoints.stream().forEach(ch -> bs.set(Character.toUpperCase(ch)));
            lowerCaseDiffPoints.stream().forEach(ch -> bs.set(Character.toLowerCase(ch)));
        }
        if (charList.isNegated()) {
            bs.flip(0, 0x110000);
        }
        return bsToRangeList(bs);
    }

    // BitSet that holds which characters are not the same in lower case
    static private BitSet lowerCaseDiffSet = caseDiffSetInit(false);
    // BitSet that holds which characters are not the same in upper case
    static private BitSet upperCaseDiffSet = caseDiffSetInit(true);

    static private BitSet caseDiffSetInit(boolean upper) {
        BitSet result = new BitSet();
        for (int ch = 0; ch <= 0x16e7f; ch++) {
            int converted = upper ? Character.toUpperCase(ch) : Character.toLowerCase(ch);
            if (converted != ch) {
                result.set(ch);
            }
        }
        return result;
    }

    // Convert a list of CharacterRange's to a BitSet
    static private BitSet rangeListToBS(List<CharacterRange> ranges) {
        BitSet result = new BitSet();
        for (CharacterRange range : ranges) {
            result.set(range.left, range.right+1);
        }
        return result;
    }

    //Convert a BitSet to a list of CharacterRange's
    static private List<CharacterRange> bsToRangeList(BitSet bs) {
        List<CharacterRange> result = new ArrayList<>();
        if (bs.isEmpty()) return result;
        int curPos = 0;
        while (curPos >=0) {
            int left = bs.nextSetBit(curPos);
            int right = bs.nextClearBit(left) -1;
            result.add(new CharacterRange(left, right));
            curPos = bs.nextSetBit(right+1);
        }
        return result;
    }
}
