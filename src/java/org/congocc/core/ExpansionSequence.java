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

import java.util.*;
import org.congocc.parser.tree.*;

public class ExpansionSequence extends Expansion {

    /**
     * @return a List that includes child expansions that are
     *         inside of superfluous parentheses.
     */
    public List<Expansion> allUnits() {
        List<Expansion> result = new ArrayList<>();
        for (Expansion unit : getUnits()) {
            result.add(unit);
            if (unit.superfluousParentheses()) {
                result.addAll(unit.firstChildOfType(ExpansionSequence.class).allUnits());
            }
        }
        return result;
    }

    public Expansion firstNonEmpty() {
        for (Expansion unit : getUnits()) {
            if (unit instanceof ExpansionWithParentheses
                    && ((ExpansionWithParentheses) unit).superfluousParentheses()) {
                unit = unit.firstChildOfType(ExpansionSequence.class).firstNonEmpty();
                if (unit != null)
                    return unit;
            } else if (!unit.isPossiblyEmpty())
                return unit;
        }
        return null;
    }

    public boolean getSpecifiesLexicalStateSwitch() {
        for (Expansion unit : getUnits()) {
            if (unit.getSpecifiesLexicalStateSwitch()) {
                return true;
            }
            if (!unit.isPossiblyEmpty())
                break;
        }
        return false;
    }

    public boolean isAlwaysSuccessful() {
        if (!super.isAlwaysSuccessful())
            return false;
        for (Expansion unit : getUnits()) {
            if (!unit.isAlwaysSuccessful())
                return false;
        }
        return true;
    }

    public TokenSet getFirstSet() {
        if (firstSet == null) {
            firstSet = new TokenSet(getGrammar());
            for (Expansion child : getUnits()) {
                firstSet.or(child.getFirstSet());
                if (!child.isPossiblyEmpty()) {
                    break;
                }
            }
        }
        return firstSet;
    }

    public TokenSet getFinalSet() {
        TokenSet finalSet = new TokenSet(getGrammar());
        List<Expansion> children = getUnits();
        Collections.reverse(children);
        for (Expansion child : children) {
            finalSet.or(child.getFinalSet());
            if (!child.isPossiblyEmpty()) {
                break;
            }
        }
        return finalSet;
    }

    public boolean getRequiresScanAhead() {
        boolean foundNonEmpty = false;
        for (Expansion unit : getUnits()) {
            if (unit.isScanLimit())
                return true;
            if (!foundNonEmpty && (unit instanceof NonTerminal)) {
                NonTerminal nt = (NonTerminal) unit;
                if (nt.getHasScanLimit())
                    return true;
                if (nt.getProduction().getHasExplicitLookahead())
                    return true;
            }
            if (!unit.isPossiblyEmpty())
                foundNonEmpty = true;
        }
        Lookahead la = getLookahead();
        return la != null && la.getRequiresScanAhead();
    }

    public boolean getHasTokenActivation() {
        for (Expansion unit : getUnits()) {
            if (unit.getHasTokenActivation())
                return true;
            if (!unit.isPossiblyEmpty())
                break;
        }
        return false;
    }

    private Lookahead lookahead;

    public void setLookahead(Lookahead lookahead) {
        this.lookahead = lookahead;
    }

    public Lookahead getLookahead() {
        if (lookahead != null)
            return lookahead;
        for (Expansion unit : allUnits()) {
            if (unit instanceof NonTerminal) {
                NonTerminal nt = (NonTerminal) unit;
                return nt.getNestedExpansion().getLookahead();
            }
            if (unit.superfluousParentheses()) {
                ExpansionSequence seq = unit.firstChildOfType(ExpansionSequence.class);
                if (seq != null) {
                    return seq.getLookahead();
                }
            }
            if (unit.getMaximumSize() > 0)
                break;
        }
        return null;
    }

    public boolean getHasExplicitLookahead() {
        return lookahead != null;
    }

    public boolean isPossiblyEmpty() {
        for (Expansion e : getUnits()) {
            if (!e.isPossiblyEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int getMinimumSize() {
        int result = 0;
        for (Expansion unit : getUnits()) {
            int minUnit = unit.getMinimumSize();
            if (minUnit == Integer.MAX_VALUE)
                return Integer.MAX_VALUE;
            result += minUnit;
        }
        return result;
    }

    public int getMaximumSize() {
        int result = 0;
        for (Expansion exp : getUnits()) {
            int max = exp.getMaximumSize();
            if (max == Integer.MAX_VALUE)
                return Integer.MAX_VALUE;
            result += max;
        }
        return result;
    }

    /**
     * @return whether we have a scan limit, including an implicit one inside a
     *         nested NonTerminal
     */
    public boolean getHasScanLimit() {
        boolean atStart = true;
        for (Expansion unit : allUnits()) {
            if (unit.isScanLimit())
                return true;
            if (atStart && unit instanceof NonTerminal) {
                if (unit.getHasScanLimit())
                    return true;
            }
            if (!unit.isPossiblyEmpty())
                atStart = false;
        }
        return false;
    }

    /**
     * @return whether we have an <em>explicit</em> scan limit,
     *         i.e. <em>not including</em> one that is inside a NonTerminal
     *         expansion.
     */
    public boolean getHasExplicitScanLimit() {
        for (Expansion unit : getUnits()) {
            if (unit.isScanLimit()) {
                return true;
            }
        }
        return false;
    }

    public List<Expansion> getUnits() {
        return childrenOfType(Expansion.class);
    }

    public boolean potentiallyStartsWith(String productionName, Set<String> alreadyVisited) {
        boolean result = false;
        for (Expansion unit : getUnits()) {
            if (unit.potentiallyStartsWith(productionName, alreadyVisited)) result = true;
            if (!unit.isPossiblyEmpty()) break;
        }
        return result;
    }
}