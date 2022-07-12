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

import java.util.Set;

import org.congocc.parser.tree.*;

public class NonTerminal extends Expansion {
    
    private Name LHS;
    public Name getLHS() {return LHS;}
    public void setLHS(Name LHS) {this.LHS=LHS;}

    /**
     * The production this non-terminal corresponds to.
     */
    public BNFProduction getProduction() {
        return getGrammar().getProductionByName(getName());
    }

    public Expansion getNestedExpansion() {
        return getProduction().getExpansion();
    }

    public boolean getHasTokenActivation() {
        return getNestedExpansion().getHasTokenActivation();
    }

    public Lookahead getLookahead() {
        return getNestedExpansion().getLookahead();
    }

    public InvocationArguments getArgs() {
        return firstChildOfType(InvocationArguments.class);
    }

    public String getName() {
        return firstChildOfType(TokenType.IDENTIFIER).getImage();
    }
    
    /**
     * The basic logic of when we scan to the end of 
     * a NonTerminal, ignoring any nested lookahead or scan limits.
     */
    public boolean getScanToEnd() {
        if (isInsideLookahead()) return true;
        ExpansionSequence parent = (ExpansionSequence) getNonSuperfluousParent();
        if (!parent.isAtChoicePoint()) return true;
        if (parent.getHasExplicitNumericalLookahead() || parent.getHasExplicitScanLimit()) return true;
        return parent.firstNonEmpty() != this;
    }

    public TokenSet getFirstSet() {
        if (firstSet == null) {
            firstSet = getProduction().getExpansion().getFirstSet();
        }
        return firstSet;
     }
     private int reEntries;     
     public TokenSet getFinalSet() {
          ++reEntries;
          TokenSet result = reEntries == 1 ? getProduction().getExpansion().getFinalSet() : new TokenSet(getGrammar());
          --reEntries;
          return result;
     }
     
     public boolean isPossiblyEmpty() {
         return getProduction().isPossiblyEmpty();
     }

     public boolean isAlwaysSuccessful() {
         return getProduction().isAlwaysSuccessful();
     }
     
     // REVISIT. Is this disposition really correct?
     private boolean inMinimumSize, inMaximumSize;
     
     public int getMinimumSize() {
         if (inMinimumSize) return Integer.MAX_VALUE;
         inMinimumSize = true;
         int result = getProduction().getExpansion().getMinimumSize();
         inMinimumSize = false;
         return result;
     }

     public int getMaximumSize() {
         if (inMaximumSize) {
             return Integer.MAX_VALUE;
         }
         inMaximumSize = true;
         int result = getProduction().getExpansion().getMaximumSize(); 
         inMaximumSize = false;
         return result;
     }
     
     public boolean getHasScanLimit() {
         return getProduction().getHasScanLimit();
     }

     public boolean getSpecifiesLexicalStateSwitch() {
         return getProduction().getLexicalState() != null || getNestedExpansion().getSpecifiesLexicalStateSwitch();
     }

     private boolean needsLeftRecursionCheck;

     public boolean getNeedsLeftRecursionCheck() {return needsLeftRecursionCheck;}

     public boolean potentiallyStartsWith(String productionName, Set<String> alreadyVisited) {
         if (productionName.equals(getName())) {
             needsLeftRecursionCheck = true;
             return true;
         }
         if (alreadyVisited.contains(getName())) return false;
         alreadyVisited.add(getName());
         return getNestedExpansion().potentiallyStartsWith(productionName, alreadyVisited);
     }

     public boolean isSingleToken() {
         return super.isSingleToken() && getProduction().getExpansion().isSingleToken();
     }
}