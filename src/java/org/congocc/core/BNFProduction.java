/* Copyright (c) 2022 Jonathan Revusky, revusky@congocc.org

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
import org.congocc.parser.BaseNode;
import org.congocc.parser.Token;
import org.congocc.parser.tree.*;
import static org.congocc.parser.CongoCCConstants.TokenType.*;

public class BNFProduction extends BaseNode {
    private Expansion expansion, recoveryExpansion;
    private String lexicalState, name, leadingComments = "";
    private boolean implicitReturnType;
    /**
     * The NonTerminal nodes which refer to this production.
     */
    private List<NonTerminal> referringNonTerminals;
    
    public Expansion getExpansion() {
        return expansion;
    }

    public void setExpansion(Expansion expansion) {
        this.expansion = expansion;
    }

    public Expansion getRecoveryExpansion() {return recoveryExpansion;}

    public void setRecoveryExpansion(Expansion recoveryExpansion) {this.recoveryExpansion = recoveryExpansion;}

    public String getLexicalState() {
        return lexicalState;
    }

    public void setLexicalState(String lexicalState) { 
        this.lexicalState = lexicalState; 
    }

    public String getName() {
        return name;
    }

    public String getFirstSetVarName() {
        return getName() + "_FIRST_SET";
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public boolean isImplicitReturnType() {return implicitReturnType;}

    public void setImplicitReturnType(boolean implicitReturnType) {
        this.implicitReturnType = implicitReturnType;
    }

    public List<NonTerminal> getReferringNonTerminals() {
        if (referringNonTerminals == null) {
           referringNonTerminals = getGrammar().descendants(NonTerminal.class, nt->nt.getName().equals(name));
        }
        return referringNonTerminals;
    }


    public TreeBuildingAnnotation getTreeNodeBehavior() {
        return firstChildOfType(TreeBuildingAnnotation.class);
    }

    public TreeBuildingAnnotation getTreeBuildingAnnotation() {
        return firstChildOfType(TreeBuildingAnnotation.class);
    }

    public boolean getHasScanLimit() {
        return expansion instanceof ExpansionSequence && ((ExpansionSequence) expansion).getHasScanLimit();
    }

    public boolean getHasExplicitLookahead() {
        return expansion.getHasExplicitLookahead();
    }

    public Lookahead getLookahead() {
        return expansion.getLookahead();
    }

    public CodeBlock getJavaCode() {
       return firstChildOfType(CodeBlock.class);
    }

    /**
     * Can this production be matched by an empty string?
     */
    public boolean isPossiblyEmpty() {
        return getExpansion().isPossiblyEmpty();
    }

    public boolean isAlwaysSuccessful() {
        return getExpansion().isAlwaysSuccessful();
    }

    public boolean isOnlyForLookahead() {
        TreeBuildingAnnotation tba = getTreeBuildingAnnotation();
        return tba!=null && "scan".equals(tba.getNodeName());
    }

    public String getLookaheadMethodName() {
        return getGrammar().generateIdentifierPrefix("check") + name;
    }

    public String getNodeName() {
        TreeBuildingAnnotation tba = getTreeBuildingAnnotation();
        if (tba != null) {
             String nodeName = tba.getNodeName();
             if (nodeName != null && !nodeName.equals("abstract") 
                 && !nodeName.equals("interface")
                 && !nodeName.equals("void")
                 && !nodeName.equals("scan")) {
                return nodeName;
             }
        }
        return this.getName();
    }

    public ThrowsList getThrowsList() {
        return firstChildOfType(ThrowsList.class);
    }
    
    public FormalParameters getParameterList() {
        return firstChildOfType(FormalParameters.class);
    }

    public String getLeadingComments() {
        return leadingComments;
    }


    public String getReturnType() {
        if (isImplicitReturnType()) {
            return getNodeName();
        }
        ReturnType rt = firstChildOfType(ReturnType.class);
        return rt == null ? "void" : rt.getAsString();
    }

    public String getAccessModifier() {
        for (Token t : childrenOfType(Token.class)) {
           TokenType type = t.getType();
           if (type == PRIVATE) {
               return "private";
           }
           else if (type == PROTECTED) {
               return "protected";
           }
           else if (type == PACKAGE) {
               return "";
           }
        }
        return "public";
    }

    
    public void adjustFirstToken(Token t) {
        //FIXME later. Not very urgent.
/*        
        Token firstToken = firstChildOfType(Token.class);
        if (firstToken != t) {

        }
        if (firstChildOfType(Token.class) !== t)
        this.leadingComments = t.getLeadingComments();
*/
    }

    private TokenSet firstSet, finalSet;
    
    public TokenSet getFirstSet() {
        if (firstSet == null) {
           firstSet = getExpansion().getFirstSet();
        }
        return firstSet;
    }

    public TokenSet getFinalSet() {
          if (finalSet == null) {
              finalSet = getExpansion().getFinalSet();
          }
          return finalSet;
    }

    /**
     * Does this production potentially have left recursion?
     */
    public boolean isLeftRecursive() {
        return getExpansion().potentiallyStartsWith(getName(), new HashSet<>());
    }
}