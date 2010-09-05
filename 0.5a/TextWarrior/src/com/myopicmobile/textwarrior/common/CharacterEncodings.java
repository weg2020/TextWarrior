/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

import com.myopicmobile.textwarrior.common.TokenScanner;

import java.util.Hashtable;

/**
 * Singleton class containing default symbols and no operators or keywords
 * 
 */
public abstract class CharacterEncodings {
	public final static int EOF = '\uFFFF';
	public final static int NULL_CHAR = '\u0000';
	public static int UNIX = 0;
	public static int MAC = 1;
	public static int WINDOWS = 2;
	public static int UNKNOWN = 3;
	
	/**
	 * Assumes ASCII char set
	 * 
	 * @param c
	 * @return
	 */
	//TODO better support for unicode and
	// making it non-static yet working with FreeScrollingTextField.paint()
	public static boolean isPrintable(char c){
		return ( (c >= '\u0020' && c <= '\u007E') ||
			c == '\n' || c == '\t' || c == '\b');
	}

	protected Hashtable _keywords;
	protected Hashtable _operators;
	
	
	protected void registerKeywords(String[] keywords){
		_keywords = new Hashtable(keywords.length);
		for(int i = 0; i < keywords.length; ++i){
			_keywords.put(keywords[i], new Integer(TokenScanner.KEYWORD));
		}
	}
	
	protected void registerOperators(char[] operators){
		_operators = new Hashtable(operators.length);
		for(int i = 0; i < operators.length; ++i){
			_operators.put(new Character(operators[i]), new Integer(TokenScanner.OPERATOR));
		}
	}
	
	public final boolean isOperator(char c){
		return _operators.containsKey(new Character(c));
	}
	
	public final boolean isKeyword(String s){
		return _keywords.containsKey(s);
	}

	public boolean isWhitespace(char c){
		return (c == ' ' || c == '\n'|| c == '\t' ||
			c == '\r' || c == '\f' || c == EOF);
	}
	
	public boolean isCharQuote(char c){
		return (c == '\'');
	}

	public boolean isStringQuote(char c){
		return (c == '"');
	}

	public boolean isPreprocessor(char c){
		return (c == '#');
	}
	
	public boolean isComment(char c){
		return (c == '/');
	}

	public boolean isComment(char c0, char c1){
		return (c0 == '/' && c1 == '/');
	}
	
	public boolean isMultiLineComment(char c){
		return (c == '*');
	}

	public boolean isMultiLineComment(char c0, char c1){
		return (c0 == '/' && c1 == '*');
	}

	public boolean isMultiLineCommentEnd(char c0, char c1){
		return (c0 == '*' && c1 == '/');
	}
}
