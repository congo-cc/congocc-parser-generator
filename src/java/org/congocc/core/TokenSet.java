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

import org.congocc.Grammar;

/**
 * A class to represent a set of Token types.
 * Will probably eventually move this into the Token.java.ftl as 
 * something available to all generated parsers.
 */

public class TokenSet extends BitSet {
	
	private static final long serialVersionUID = 1L;
	
	private Grammar grammar;

	private boolean incomplete;

	public TokenSet(Grammar grammar) {
		this.grammar = grammar;
	}

	public TokenSet(Grammar grammar, boolean incomplete) {
		this.grammar=grammar;
		this.incomplete = incomplete;
	}

	public boolean isIncomplete() {
		return incomplete;
	}

	public void setIncomplete(boolean incomplete) {
		this.incomplete = incomplete;
	}
	
	public long[] toLongArray() {
	    long[] ll = super.toLongArray();
	    int numKinds = grammar.getLexerData().getTokenCount();
	    if (ll.length < 1+numKinds/64) {
	        ll = Arrays.copyOf(ll, 1 + numKinds/64);
	    }
	    return ll; 
	}
	
	public List<String> getTokenNames() {
		List<String> names = new ArrayList<>();
		int tokCount = grammar.getLexerData().getTokenCount();
		for (int i = 0; i<tokCount; i++) {
			if (get(i)) {
				names.add(grammar.getLexerData().getTokenName(i));
			}
		}
		return names;
	}
	
	public String getFirstTokenName() {
		int tokCount = grammar.getLexerData().getTokenCount();
		for (int i=0; i<tokCount; i++) {
			if (get(i)) {
				return grammar.getLexerData().getTokenName(i);
			}
		}
		return null;
	}


    public List<String> getTokenSetNames() {
        int tokenCount = grammar.getLexerData().getTokenCount();
        List<String> result = new ArrayList<>(tokenCount);
        for (int i=0; i<tokenCount; i++) {
            if (get(i)) {
                result.add(grammar.getLexerData().getTokenName(i));
            }
        }
        return result;
    }
 	
	public String getCommaDelimitedTokens() {
		if (cardinality() <=1) {
			return getFirstTokenName();
		}
		StringBuilder result = new StringBuilder();
		for (String name : getTokenNames()) {
			result.append(name);
			result.append(", ");
		}
		result.setLength(result.length() -2);
		return result.toString();
	}

	public void not() {
		flip(0, grammar.getLexerData().getTokenCount());
	}
}
