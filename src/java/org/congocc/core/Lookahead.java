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

import org.congocc.parser.BaseNode;
import org.congocc.parser.tree.*;


public class Lookahead extends BaseNode {
    private Name LHS;
    private Expansion expansion, nestedExpansion, upToExpansion;
    private boolean negated, semanticLookaheadNested;
    private Expression semanticLookahead;

    public Name getLHS() {return LHS;}

    public void setLHS(Name LHS) {this.LHS = LHS;}

    public Expansion getExpansion() {return expansion;}

    public void setExpansion(Expansion expansion) {this.expansion=expansion;}

    public Expansion getNestedExpansion() {return nestedExpansion;}

    public void setNestedExpansion(Expansion nestedExpansion) {this.nestedExpansion = nestedExpansion;}

    public Expression getSemanticLookahead() {return semanticLookahead;}

    public void setSemanticLookahead(Expression semanticLookahead) {this.semanticLookahead = semanticLookahead;}

    public boolean isSemanticLookaheadNested() {return semanticLookaheadNested;}

    public void setSemanticLookaheadNested(boolean semanticLookaheadNested) {this.semanticLookaheadNested = semanticLookaheadNested;}

    public boolean isNegated() {return negated;}

    public void setNegated(boolean negated) {this.negated = negated;}

    public Expansion getUpToExpansion() { return upToExpansion;}

    public void setUpToExpansion(Expansion upToExpansion) {this.upToExpansion = upToExpansion;}


    public boolean isAlwaysSuccessful() {
        return !hasSemanticLookahead() && (getAmount() == 0 || getLookaheadExpansion().isPossiblyEmpty()); 
    }

    public boolean getRequiresScanAhead() {
        return !getLookaheadExpansion().isPossiblyEmpty() || isSemanticLookaheadNested();
//        return !getLookaheadExpansion().isPossiblyEmpty() && getAmount() > 1;
//          return !isAlwaysSuccessful() && getAmount() >1;
    }

    public boolean hasSemanticLookahead() {
        return getSemanticLookahead() != null;
    }
    
    public Expansion getLookaheadExpansion() {
        Expansion result = getNestedExpansion();
        if (result != null) {
            return result;
        }
        return expansion;
    }

    public boolean getHasExplicitNumericalAmount() {
        return firstChildOfType(TokenType.INTEGER_LITERAL) != null;
    }

    public int getAmount() {
        IntegerLiteral it = firstChildOfType(IntegerLiteral.class);
        if (it!=null) return it.getValue();
        if (this instanceof LegacyLookahead) {
            if (getNestedExpansion() == null && hasSemanticLookahead()) return 0;
        }
        return Integer.MAX_VALUE;
    }

    public LookBehind getLookBehind() {
        return firstChildOfType(LookBehind.class);
    }

}