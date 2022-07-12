/* Copyright (c) 2008-2022 Jonathan Revusky, revusky@congocc.org

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.congocc.core;

import java.util.List;
import java.util.Set;

public class ExpansionChoice extends Expansion {
    public List<Expansion> getChoices() {
        return childrenOfType(Expansion.class);
    }
    
    public TokenSet getFirstSet() {
         if (firstSet == null) {
            firstSet = new TokenSet(getGrammar());
            for (Expansion choice : getChoices()) {
                firstSet.or(choice.getLookaheadExpansion().getFirstSet());
            }
         }
         return firstSet;
    }
    
    public TokenSet getFinalSet() {
        TokenSet finalSet = new TokenSet(getGrammar());
        for (Expansion choice : getChoices()) {
            finalSet.or(choice.getFinalSet());
        }
        return finalSet;
    }
    
    
    public boolean isPossiblyEmpty() {
         for (Expansion e : getChoices()) {
             if (e.isPossiblyEmpty()) {
                 return true;
             }
         }
         return false;
    }
 
    public boolean isAlwaysSuccessful() {
        if (!super.isAlwaysSuccessful()) return false;
        for (Expansion choice : getChoices()) {
            if (choice.isAlwaysSuccessful()) return true;
        }
        return false;
    }
    
    public int getMinimumSize() {
        int result = Integer.MAX_VALUE;
        for (Expansion choice : getChoices()) {
           int choiceMin = choice.getMinimumSize();
           if (choiceMin ==0) return 0;
           result = Math.min(result, choiceMin);
        }
        return result;
    }
 
    public int getMaximumSize() {
        int result = 0;
        for (Expansion exp : getChoices()) {
            result = Math.max(result, exp.getMaximumSize());
            if (result == Integer.MAX_VALUE) break;
        }
        return result;
    }
    
    public boolean getSpecifiesLexicalStateSwitch() {
        for (Expansion choice : getChoices()) {
            if (choice.getSpecifiesLexicalStateSwitch()) return true;
        }
        return false;
    }

    public boolean potentiallyStartsWith(String productionName, Set<String> alreadyVisited) {
        for (Expansion choice : getChoices()) {
            if (choice.potentiallyStartsWith(productionName, alreadyVisited)) return true;
        }
        return false;
    }
}
