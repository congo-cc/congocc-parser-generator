/* Copyright (c) 2008-2022 Jonathan Revusky, revusky@congocc.org
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

import java.util.*;

import org.congocc.Grammar;
import org.congocc.parser.tree.EndOfFile;
import org.congocc.parser.tree.RegexpChoice;
import org.congocc.parser.tree.RegexpStringLiteral;
import org.congocc.parser.tree.TokenProduction;

/**
 * Base object that contains lexical data. 
 * It contains LexicalStateData objects that contain
 * the data for each lexical state. The LexicalStateData
 * objects hold the data related to generating the NFAs 
 * for the respective lexical states.
 */
public class LexerData {
    private Grammar grammar;
    private List<LexicalStateData> lexicalStates = new ArrayList<>();
    private List<RegularExpression> regularExpressions = new ArrayList<>();
    
    public LexerData(Grammar grammar) {
        this.grammar = grammar;
        RegularExpression reof = new EndOfFile();
        reof.setGrammar(grammar);
        reof.setLabel("EOF");
        regularExpressions.add(reof);
    }
    
    public String getTokenName(int ordinal) {
        if (ordinal < regularExpressions.size()) {
            return regularExpressions.get(ordinal).getLabel();
        }
        return grammar.getExtraTokenNames().get(ordinal-regularExpressions.size());
    }

    public String getLexicalStateName(int index) {
        return lexicalStates.get(index).getName();
    }

    public void addLexicalState(String name) {
        lexicalStates.add(new LexicalStateData(grammar, name));
    }

    public LexicalStateData getLexicalState(String name) {
        for (LexicalStateData state : lexicalStates) {
            if (state.getName().equals(name)) {
                return state;
            }
        }
        return null;
    }

    public int getMaxNfaStates() {
        int result = 0;
        for (LexicalStateData lsd : lexicalStates) {
            result = Math.max(result, lsd.allStates.size());
        }
        return result;
    }

    public RegularExpression getRegularExpression(int idx) {
        if (idx == Integer.MAX_VALUE) return null;
        return regularExpressions.get(idx);
    }

    public List<RegularExpression> getRegularExpressions() {
        return regularExpressions;
    }

    public boolean getHasLexicalStateTransitions() {
        return getNumLexicalStates() > 1 && 
               regularExpressions.stream().anyMatch(re->re.getNewLexicalState()!=null);
    }

    public boolean getHasTokenActions() {
        return regularExpressions.stream().anyMatch(re->re.getCodeSnippet()!=null);
    }

    public int getLexicalStateIndex(String lexicalStateName) {
        for (int i = 0; i < lexicalStates.size(); i++) {
            LexicalStateData state = lexicalStates.get(i);
            if (state.getName().equals(lexicalStateName)) {
                return i;
            }
        }
        return -1;
    }
    
    public int getNumLexicalStates() {
        return lexicalStates.size();
    }

    public List<LexicalStateData> getLexicalStates() {
        return lexicalStates;
    }

    public void addRegularExpression(RegularExpression regexp) {
        regexp.setOrdinal(regularExpressions.size());
        regularExpressions.add(regexp);
    }
    
    public void ensureStringLabels() {
        for (ListIterator<RegularExpression> it = regularExpressions.listIterator();it.hasNext();) {
            RegularExpression regexp = it.next();
            if (!isJavaIdentifier(regexp.getLabel())) {
                String label = "_TOKEN_" + it.previousIndex();
                if (regexp instanceof RegexpStringLiteral) {
                    String s= ((RegexpStringLiteral)regexp).getImage().toUpperCase();
                    if (isJavaIdentifier(s) && !regexpLabelAlreadyUsed(s)) label = s;
                }
                regexp.setLabel(label);
            }
        }
    }
   
    static public boolean isJavaIdentifier(String s) {
        if (s.length() == 0) return false;
        for (int i=0; i<s.length(); i++) {
            int ch = s.codePointAt(i);
            if (i==0 && !Character.isJavaIdentifierStart(ch)) return false;
            if (!Character.isJavaIdentifierPart(ch)) return false;
            if (ch > 0xFFFF) i++;
        }
        return true;
    }
   
    private boolean regexpLabelAlreadyUsed(String label) {
        for (RegularExpression regexp : regularExpressions) {
            if (label.contentEquals(regexp.getLabel())) return true;
        }
        return false;
    }

    
    public String getStringLiteralLabel(String image) {
        for (RegularExpression regexp : regularExpressions) {
            if (regexp instanceof RegexpStringLiteral) {
                if (regexp.getImage().equals(image)) {
                    return regexp.getLabel();
                }
            }
        }
        return null;
    }

    public int getTokenCount() {
        return regularExpressions.size() + grammar.getExtraTokenNames().size();
    }
    
    public TokenSet getMoreTokens() {
        return getTokensOfKind("MORE");
    } 

    public TokenSet getSkippedTokens() {
        return getTokensOfKind("SKIP");
    }
    
    public TokenSet getUnparsedTokens() {
        return getTokensOfKind("UNPARSED");
    }

    public TokenSet getRegularTokens() {
        TokenSet result = getTokensOfKind("TOKEN");
        for (RegularExpression re : regularExpressions) {
            if (re.getTokenProduction() == null) {
                result.set(re.getOrdinal());
            }
        }
        return result;
    }

    private TokenSet getTokensOfKind(String kind) {
        TokenSet result = new TokenSet(grammar);
        for (RegularExpression re : regularExpressions) {
            TokenProduction tp = re.getTokenProduction();
            if (tp != null && tp.getKind().equals(kind)) {
                result.set(re.getOrdinal());
            } 
        }
        return result;
    }

    public void buildData() {
        for (TokenProduction tokenProduction : grammar.descendants(TokenProduction.class)) {
            for (String lexStateName : tokenProduction.getLexicalStateNames()) {
                LexicalStateData lexState = getLexicalState(lexStateName);
                lexState.addTokenProduction(tokenProduction);
            }
        }
        List<RegexpChoice> choices = new ArrayList<RegexpChoice>();
        for (LexicalStateData lexState : lexicalStates) {
            choices.addAll(lexState.process());
        }
        for (RegexpChoice choice : choices) {
            checkUnmatchability(choice);
        }
        for (LexicalStateData lsd : getLexicalStates()) {
            if (lsd.isEmpty()) {
                grammar.addError("Error: Lexical State " + lsd.getName() + " does not contain any token types!");
            }
        }
    }

    //What about the case of a regexp existing in multiple lexical states? REVISIT (JR)
    static private void checkUnmatchability(RegexpChoice choice) {
        for (RegularExpression curRE : choice.getChoices()) {
            if (!(curRE).isPrivate() && curRE.getOrdinal() > 0 && curRE.getOrdinal() < choice.getOrdinal()
                    && curRE.getLexicalState() == choice.getLexicalState()) {
                choice.getGrammar().addWarning(choice, "Regular Expression choice : " + curRE.getLabel()
                        + " can never be matched as : " + choice.getLabel());
            }
        }
    }
}
