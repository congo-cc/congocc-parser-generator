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

/**
 * A convenience base class for defining empty expansions, i.e. that 
 * do not consume any tokens. 
 */

abstract public class EmptyExpansion extends Expansion {
    public boolean getRequiresScanAhead() {
        return false;
    }
    
    public boolean isPossiblyEmpty() {
        return true;
    }
    
    public TokenSet getFirstSet() {return new TokenSet(getGrammar());}
    
    public TokenSet getFinalSet() {return new TokenSet(getGrammar());}
     
    public int getMinimumSize() {return 0;}

    public int getMaximumSize() {return 0;}

    public boolean getSpecifiesLexicalStateSwitch() {return false;}

}