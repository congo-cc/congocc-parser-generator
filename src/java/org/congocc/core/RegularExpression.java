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
import org.congocc.parser.tree.CodeBlock;
import org.congocc.parser.tree.Name;
import org.congocc.parser.tree.TokenProduction;

/**
 * An abstract base class from which all the AST nodes that
 * are regular expressions inherit.
 */

public abstract class RegularExpression extends Expansion {

    public RegularExpression(Grammar grammar) {
        super(grammar);
    }
    
    public RegularExpression() {
    }
    
   /**
     * The ordinal value assigned to the regular expression. It is used for
     * internal processing and passing information between the parser and the
     * lexical analyzer.
     */
    private int id;

    private LexicalStateData newLexicalState;

    private CodeBlock codeSnippet;

    public CodeBlock getCodeSnippet() {
        return codeSnippet;
    }

    void setCodeSnippet(CodeBlock codeSnippet) {
        this.codeSnippet = codeSnippet;
    }

    protected boolean getIgnoreCase() {
        TokenProduction tp = firstAncestorOfType(TokenProduction.class);
        if (tp !=null) return tp.isIgnoreCase();
        return getGrammar().isIgnoreCase();//REVISIT
    }

    /**
     * The LHS to which the token value of the regular expression is assigned.
     * This can be null.
     */
    private Name lhs;

    /**
     * This flag is set if the regular expression has a label prefixed with the #
     * symbol - this indicates that the purpose of the regular expression is
     * solely for defining other regular expressions.
     */
    private boolean _private = false;

    protected TokenProduction getTokenProduction() {
        return firstAncestorOfType(TokenProduction.class);
    }

    public final String getLabel() {
    	String label = super.getLabel();
    	if (label != null && label.length() != 0) {
    	    return label;
    	}
  	    if (id == 0) {
 	        return "EOF";
 	    }
  	    return String.valueOf(id);
    }

    public int getOrdinal() {
        return id;
    }

    protected final void setOrdinal(int id) {
        this.id =  id;
    }

    public Name getLHS() {
        return lhs;
    }
    
    public void setLHS(Name lhs) {
        this.lhs = lhs;
    }

    public LexicalStateData getLexicalState() {
        List<LexicalStateData> states = getGrammar().getLexerData().getLexicalStates();
        LexicalStateData result = states.get(0);
        for (LexicalStateData ls : states) {
            if (ls.containsRegularExpression(this)) {
                result = ls;
            }
        }
        return result;
        
    }
    
    public void setNewLexicalState(LexicalStateData newLexicalState) {
        this.newLexicalState = newLexicalState;
    }

    public LexicalStateData getNewLexicalState() {
        return newLexicalState;
    }
 
    public boolean isPrivate() {
        return this._private;
    }

    public String getImage() {
        return null;
    }
    
    public void setPrivate(boolean _private) {
        this._private = _private;
    }
    
    public String getGeneratedClassName() {
        if (generatedClassName.equals("Token")) {
            generatedClassName = getLabel();
        }
        return generatedClassName;
    }
    
    public void setGeneratedClassName(String generatedClassName) {
        this.generatedClassName = generatedClassName;
    }

    public String getGeneratedSuperClassName() {
        return generatedSuperClassName;
    }

    public void setGeneratedSuperClassName(String generatedSuperClassName) {
        this.generatedSuperClassName = generatedSuperClassName;
    }
    
    private String generatedClassName = "Token", generatedSuperClassName;
    
    
    public TokenSet getFirstSet() {
    	if (firstSet== null) {
    		firstSet = new TokenSet(getGrammar());
    		firstSet.set(getOrdinal());
    	}
        return firstSet;
    }
    
    public TokenSet getFinalSet() {
        return getFirstSet();	
    }
    
    
    final public boolean isPossiblyEmpty() {
    	return false;
    }
    
    final public boolean getRequiresScanAhead() {
    	return false;
    }
    
    final public int getMinimumSize() {
        return 1;
    }

    final public int getMaximumSize() {
        return 1;
    }
    
    abstract public boolean matchesEmptyString();
}


