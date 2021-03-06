/*
 * Copyright (c) 2011 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

/**
 * Singleton class that represents a non-programming language without keywords, 
 * operators etc.
 */
public class LanguageNonProg extends Language{
	private static Language _theOne = null;
	
	private final static String[] keywords = {};
	
	private final static char[] operators = {};


	public static Language getCharacterEncodings(){
		if(_theOne == null){
			_theOne = new LanguageNonProg();
		}
		return _theOne;
	}
	
	private LanguageNonProg(){
		super.registerKeywords(keywords);
		super.registerOperators(operators);
	}
	
	public boolean isPreprocessor(char c){
		return false;
	}
	
	public boolean isCharQuote(char c){
		return false;
	}

	public boolean isStringQuote(char c){
		return false;
	}

	public boolean isComment(char c){
		return false;
	}

	public boolean isComment(char c0, char c1){
		return false;
	}

	public boolean isMultiLineComment(char c){
		return false;
	}
	
	public boolean isMultiLineComment(char c0, char c1){
		return false;
	}
	
	public boolean isProgLang(){
		return false;
	}
}
