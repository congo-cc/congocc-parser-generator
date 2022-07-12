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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.congocc.Grammar;
import org.congocc.parser.BaseNode;
import org.congocc.parser.Node;
import org.congocc.parser.tree.*;

/**
 * Describes expansions - entities that may occur on the right hand sides of
 * productions. This is the base class of a bunch of other more specific
 * classes.
 */
abstract public class Expansion extends BaseNode {

    private TreeBuildingAnnotation treeNodeBehavior;

    private String label = "";

    protected TokenSet firstSet;

    public int getIndex() {
        return parent.indexOf(this);
    }

    public Expansion(Grammar grammar) {
        setGrammar(grammar);
    }

    public Expansion() {
    }

    public BNFProduction getContainingProduction() {
        return firstAncestorOfType(BNFProduction.class);
    }

    private String scanRoutineName, firstSetVarName;

    public String getLabel() {
        return label;
    }

    final boolean hasLabel() {
        return label.length() > 0;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private boolean tolerantParsing;

    /**
     * If we hit a parsing error in this expansion, do we try to recover? This is
     * only used in fault-tolerant mode, of course!
     */
    public boolean isTolerantParsing() {
        return tolerantParsing;
    }

    public void setTolerantParsing(boolean tolerantParsing) {
        this.tolerantParsing = tolerantParsing;
    }

    public String toString() {
        String result = "[" + getSimpleName() + " on line " + getBeginLine() + ", column " + getBeginColumn();
        String inputSource = getInputSource();
        if (inputSource != null) {
            result += " of ";
            result += inputSource;
        }
        return result + "]";
    }

    public Expansion getNestedExpansion() {
        return null;
    }

    public boolean getIsRegexp() {
        return this instanceof RegularExpression;
    }

    public TreeBuildingAnnotation getTreeNodeBehavior() {
        if (treeNodeBehavior == null) {
            if (this.getParent() instanceof BNFProduction) {
                return ((BNFProduction) getParent()).getTreeNodeBehavior();
            }
        }
        return treeNodeBehavior;
    }

    public void setTreeNodeBehavior(TreeBuildingAnnotation treeNodeBehavior) {
        if (getGrammar().getTreeBuildingEnabled()) {
            this.treeNodeBehavior = treeNodeBehavior;
            if (treeNodeBehavior != null) {
                getGrammar().addNodeType(null, treeNodeBehavior.getNodeName());
            }
        }
    }

    /**
     * This method is a bit hairy because of the need to deal with 
     * superfluous parentheses.
     * @return Is this expansion at a choice point?
     */

    public boolean isAtChoicePoint() {
        Node parent = getParent();
        if (parent instanceof ExpansionChoice || parent instanceof OneOrMore || parent instanceof ZeroOrMore
            || parent instanceof ZeroOrOne || parent instanceof BNFProduction) {
                return true;
            }
        if (!(parent instanceof ExpansionWithParentheses)) {
            return false;
        }
        ExpansionSequence grandparent = (ExpansionSequence) parent.getParent();
        if (!grandparent.isAtChoicePoint()) {
            return false;
        }
        for (Expansion exp : grandparent.getUnits()) {
            if (exp == parent) return true;
            if (exp.getMaximumSize()>0) break;
        }
        return false;
    }

    /**
     * @return the first ancestor that is not (directly) inside superfluous
     *         parentheses. (Yes, this is a bit hairy and I'm not 100% sure it's
     *         correct!) I really need to take a good look at all this handling of
     *         expansions inside parentheses.
     */

    public Node getNonSuperfluousParent() {
        Node parent = getParent();
        if (!(parent instanceof Expansion) || !((Expansion) parent).superfluousParentheses()) {
            return parent;
        }
        ExpansionSequence grandparent = (ExpansionSequence) parent.getParent();
        return grandparent.getNonSuperfluousParent();
    }

    /**
     * @return the lexical state to switch into to parse this expansion. 
     */
    public String getSpecifiedLexicalState() {
        Node parent = getParent();
        if (parent instanceof BNFProduction) {
            return ((BNFProduction) parent).getLexicalState();
        }
        return null;
    }

    public TokenActivation getTokenActivation() {
        return firstChildOfType(TokenActivation.class);
    }

    private CodeBlock customErrorRecoveryBlock;

    public CodeBlock getCustomErrorRecoveryBlock() {
        return customErrorRecoveryBlock;
    }

    public void setCustomErrorRecoveryBlock(CodeBlock customErrorRecoveryBlock) {
        this.customErrorRecoveryBlock = customErrorRecoveryBlock;
    }

    /**
     * Is this expansion superfluous parentheses?
     */
    public final boolean superfluousParentheses() {
        return this.getClass() == ExpansionWithParentheses.class && firstChildOfType(ExpansionSequence.class) != null;
    }

    public boolean beginsSequence() {
        if (getParent() instanceof ExpansionSequence) {
            ExpansionSequence seq = (ExpansionSequence) getParent();
            for (Expansion child : seq.childrenOfType(Expansion.class)) {
                if (child == this)
                    return true;
                if (!child.isPossiblyEmpty())
                    return false;
            }
        }
        return false;
    }

    public boolean isInsideLookahead() {
        return firstAncestorOfType(Lookahead.class) != null;
    }

    public Lookahead getLookahead() {
        return null;
    }

    public boolean getHasExplicitLookahead() {
        return getLookahead() != null;
    }

    public boolean getHasExplicitNumericalLookahead() {
        Lookahead la = getLookahead();
        return la != null && la.getHasExplicitNumericalAmount();
    }

    /**
     * Does this expansion have a separate lookahead expansion?
     */

    public boolean getHasSeparateSyntacticLookahead() {
        Lookahead la = getLookahead();
        return la != null && la.getNestedExpansion() != null;
    }

    /**
     * Do we do a syntactic lookahead using this expansion itself as the lookahead
     * expansion?
     */
    public boolean getHasImplicitSyntacticLookahead() {
        if (!this.isAtChoicePoint())
            return false;
        if (getHasSeparateSyntacticLookahead())
            return false;
        if (this.isAlwaysSuccessful())
            return false;
        if (getHasScanLimit()) {
            return true;
        }
        if (getHasExplicitNumericalLookahead() && getLookaheadAmount() <= 1)
            return false;
        if (getMaximumSize() <= 1) {
            return false;
        }
        Lookahead la = getLookahead();
        return la != null && la.getAmount() > 1;
    }

    private boolean scanLimit;
    private int scanLimitPlus;

    public boolean isScanLimit() {
        return scanLimit;
    }

    public void setScanLimit(boolean scanLimit) {
        this.scanLimit = scanLimit;
    }

    public int getScanLimitPlus() {
        return scanLimitPlus;
    }

    public void setScanLimitPlus(int scanLimitPlus) {
        this.scanLimitPlus = scanLimitPlus;

    }

    public boolean getRequiresScanAhead() {
        Lookahead la = getLookahead();
        if (la != null && la.getRequiresScanAhead())
            return true;
        // if (this.getParent() instanceof org.congocc.parser.tree.Assertion) return
        // true;
        return getHasGlobalSemanticActions();
    }

    public final boolean hasNestedSemanticLookahead() {
        for (Expansion expansion : descendants(Expansion.class)) {
            if (expansion.getHasSemanticLookahead() && expansion.getLookahead().isSemanticLookaheadNested()) {
                return true;
            }
        }
        return false;
    }

    public final boolean getRequiresPredicateMethod() {
        if (isInsideLookahead() || !isAtChoicePoint()) {
            return false;
        }
        if (getLookahead() != null) {
            return true;
        }
        if (isPossiblyEmpty()) {
            return false;
        }
        //if (getHasImplicitSyntacticLookahead() && !isSingleToken()) {
        if (getHasImplicitSyntacticLookahead()) {
            return true;
        }
        if (getHasTokenActivation() || getSpecifiedLexicalState() != null) {
            return true;
        }
        if (getSpecifiesLexicalStateSwitch()) {
            return true;
        }
        return getHasGlobalSemanticActions();
    }

    public Expansion getLookaheadExpansion() {
        Lookahead la = getLookahead();
        Expansion exp = la == null ? null : la.getNestedExpansion();
        return exp != null ? exp : this;
    }

    public boolean isAlwaysSuccessful() {
        if (getHasSemanticLookahead() || getHasLookBehind() || !isPossiblyEmpty()) {
            return false;
        }
        if (firstChildOfType(Failure.class) != null) {
            return false;
        }
        Lookahead la = getLookahead();
        return la == null || la.getNestedExpansion() == null || la.getNestedExpansion().isPossiblyEmpty();
    }

    public boolean getHasGlobalSemanticActions() {
        List<CodeBlock> blocks = descendants(CodeBlock.class, cb -> cb.isAppliesInLookahead());
        return !blocks.isEmpty();
    }

    public int getLookaheadAmount() {
        Lookahead la = getLookahead();
        if (la != null)
            return la.getAmount();
        return getRequiresScanAhead() ? Integer.MAX_VALUE : 1; // A bit kludgy, REVISIT
    }

    public boolean getHasSemanticLookahead() {
        Lookahead la = getLookahead();
        return la != null && la.hasSemanticLookahead();
    }

    public boolean getHasScanLimit() {
        return false; // Only an ExpansionSequence can have a scan limit.
    }

    public Expansion getUpToExpansion() {
        Lookahead la = getLookahead();
        return la == null ? null : la.getUpToExpansion();
    }

    public Expression getSemanticLookahead() {
        return getHasSemanticLookahead() ? getLookahead().getSemanticLookahead() : null;
    }

    public boolean getHasLookBehind() {
        return getLookahead() != null && getLookahead().getLookBehind() != null;
    }

    public LookBehind getLookBehind() {
        return getLookahead() != null ? getLookahead().getLookBehind() : null;
    }

    public boolean isNegated() {
        return getLookahead() != null && getLookahead().isNegated();
    }

    public String getFirstSetVarName() {
        if (firstSetVarName == null) {
            if (this.getParent() instanceof BNFProduction) {
                firstSetVarName = ((BNFProduction) getParent()).getFirstSetVarName();
            } else {
                Grammar g = getGrammar();
                String prefix = g.generateIdentifierPrefix("first_set");

                firstSetVarName = g.generateUniqueIdentifier(prefix, this);
            }
        }
        return firstSetVarName;
    }

    public String getFinalSetVarName() {
        String result = getFirstSetVarName();
        String prefix = getGrammar().generateIdentifierPrefix("first_set");

        if (result.startsWith(prefix)) {
            return result.replaceFirst("first", "final");
        }
        return result.replace("_FIRST_SET", "_FINAL_SET");
    }

    public String getFollowSetVarName() {
        String result = getFirstSetVarName();
        String prefix = getGrammar().generateIdentifierPrefix("first_set");

        if (result.startsWith(prefix)) {
            return result.replaceFirst("first", "follow");
        }
        return result.replace("_FIRST_SET", "_FOLLOW_SET");
    }

    public String getScanRoutineName() {
        if (scanRoutineName == null) {
            if (this.getParent() instanceof BNFProduction) {
                BNFProduction prod = (BNFProduction) getParent();
                scanRoutineName = prod.getLookaheadMethodName();
            } else {
                Grammar g = getGrammar();
                String prefix = g.generateIdentifierPrefix("check");

                scanRoutineName = g.generateUniqueIdentifier(prefix, this);
            }
        }
        return scanRoutineName;
    }

    public String getPredicateMethodName() {
        Grammar g = getGrammar();
        String checkPrefix = g.generateIdentifierPrefix("check");
        String scanPrefix = g.generateIdentifierPrefix("scan");
        return getScanRoutineName().replace(checkPrefix, scanPrefix);
    }

    public String getRecoverMethodName() {
        Grammar g = getGrammar();
        String checkPrefix = g.generateIdentifierPrefix("check");
        String recoverPrefix = g.generateIdentifierPrefix("recover");
        return getScanRoutineName().replace(checkPrefix, recoverPrefix);
    }

    public String getRecoverToMethodName() {
        Grammar g = getGrammar();
        String checkPrefix = g.generateIdentifierPrefix("check");
        String recoverToPrefix = g.generateIdentifierPrefix("recover_to");
        return getScanRoutineName().replace(checkPrefix, recoverToPrefix);
    }

    public int getFinalSetSize() {
        return getFinalSet().cardinality();
    }

    abstract public TokenSet getFirstSet();

    abstract public TokenSet getFinalSet();

    public boolean getHasFullFollowSet() {
        return !getFollowSet().isIncomplete();
    }

    public boolean getSpecifiesLexicalStateSwitch() {
        return false;
    };

    public boolean getHasTokenActivation() {
        return firstChildOfType(TokenActivation.class) != null;
    }

    /**
     * @return Can this expansion be matched by the empty string.
     */
    abstract public boolean isPossiblyEmpty();

    /**
     * @return whether this Expansion is always matched by exactly one token
     * AND there is no funny business like lexical state switches a FAIL
     * or an up-to-here marker
     */
    public boolean isSingleToken() {
        if (isPossiblyEmpty() || getMaximumSize() > 1 || getHasScanLimit() || getSpecifiesLexicalStateSwitch())
            return false;
        if (getLookahead() != null)
            return false;
        if (firstDescendantOfType(Failure.class) != null || firstDescendantOfType(TokenActivation.class) != null)
            return false;
        if (!descendants(Expansion.class, exp->exp.getSpecifiesLexicalStateSwitch()).isEmpty())
            return false;
        if (!descendants(Expansion.class, exp->exp.getHasLookBehind()).isEmpty())
            return false;
        if (hasNestedSemanticLookahead()) 
            return false;
        if (getHasGlobalSemanticActions())
            return false;
        return true;
    }

    /**
     * @return the minimum number of tokens that this expansion consumes.
     */
    abstract public int getMinimumSize();

    /**
     * @return the maximum number of tokens that this expansion consumes.
     */
    abstract public int getMaximumSize();

    private Expansion getPreceding() {
        Node parent = getParent();
        if (parent instanceof ExpansionSequence) {
            List<Expansion> siblings = parent.childrenOfType(Expansion.class);
            int index = siblings.indexOf(this);
            while (index > 0) {
                Expansion exp = siblings.get(index - 1);
                if (exp.getMaximumSize() > 0) {
                    return exp;
                }
                index--;
            }
        }
        return null;
    }

    public Expansion getFollowingExpansion() {
        Node parent = getParent();
        if (parent instanceof ExpansionSequence) {
            List<Expansion> siblings = parent.childrenOfType(Expansion.class);
            int index = siblings.indexOf(this);
            if (index < siblings.size() - 1)
                return siblings.get(index + 1);
        }
        if (parent instanceof Expansion) {
            return ((Expansion) parent).getFollowingExpansion();
        }
        return null;
    }

    public TokenSet getFollowSet() {
        TokenSet result = new TokenSet(getGrammar());
        if (isAtEndOfLoop()) {
            result.or(firstLoopAncestor().getFirstSet());
        }
        Expansion following = this;
        do {
            following = following.getFollowingExpansion();
            if (following == null) {
                result.setIncomplete(true);
                break;
            }
            result.or(following.getFirstSet());
        } while (following.isPossiblyEmpty());
        return result;
    }

    private boolean isAtEndOfLoop() {
        if (this instanceof ZeroOrMore || this instanceof OneOrMore)
            return true;
        Node parent = getParent();
        if (parent instanceof ExpansionSequence) {
            List<Expansion> siblings = parent.childrenOfType(Expansion.class);
            int index = siblings.indexOf(this);
            for (int i = index + 1; i < siblings.size(); i++) {
                if (!siblings.get(i).isPossiblyEmpty())
                    return false;
            }
        }
        if (parent instanceof Expansion) {
            return ((Expansion) parent).isAtEndOfLoop();
        }
        return false;
    }

    private Expansion firstLoopAncestor() {
        Expansion result = this;
        while (!(result instanceof ZeroOrMore || result instanceof OneOrMore)) {
            Node parent = result.getParent();
            if (parent instanceof Expansion)
                result = (Expansion) parent;
            else
                return null;
        }
        return result;
    }

    public Boolean isBeforeLexicalStateSwitch() {
        // We return a null if we don't have full info.
        Expansion following = this;
        do {
            following = following.getFollowingExpansion();
            if (following == null)
                return null;
            if (following.getSpecifiesLexicalStateSwitch())
                return true;
        } while (following.isPossiblyEmpty());
        return false;
    }

    public boolean getRequiresRecoverMethod() {
        if (isInsideLookahead()) {
            return false;
        }
        if (getContainingProduction() != null && getContainingProduction().isOnlyForLookahead()) {
            return false;
        }
        if (isTolerantParsing() || getParent() instanceof BNFProduction) {
            return true;
        }
        Expansion preceding = getPreceding();
        return preceding != null && preceding.isTolerantParsing() && !(preceding instanceof RegularExpression);
    }

    /**
     * Whether this expansion can start with a given production
     * This is the default implementation that always returns false.
     */
    public boolean potentiallyStartsWith(String productionName, Set<String> alreadyVisited) {
        return false;
    }

    final public boolean potentiallyStartsWith(String productionName) {
        return potentiallyStartsWith(productionName, new HashSet<>());
    }

    /*
     * This section indicates whether this expansion has a child name associated with it,
     * and whether that relates to a single value or a list of values.
     */
    private String childName;
    private boolean multipleChildren;

    public String getChildName() { return childName; }
    public void setChildName(String name) { childName = name; }
    public boolean isMultipleChildren() { return multipleChildren; }
    public void setMultipleChildren(boolean multiple) { multipleChildren = multiple; }
}
