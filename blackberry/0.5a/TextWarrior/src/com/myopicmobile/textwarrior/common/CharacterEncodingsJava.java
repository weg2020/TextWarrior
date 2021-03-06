/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

/**
 * Singleton class containing the symbols and operators of the Java language
 *
 */
public class CharacterEncodingsJava extends CharacterEncodings{
	private static CharacterEncodings _theOne = null;
	
	private final static String[] keywords = {
		"void", "boolean", "byte", "char", "short", "int", "long", "float", "double", "strictfp",
		"import", "package", "new", "class", "interface", "extends", "implements", "enum",
		"public", "private", "protected", "static", "abstract", "final", "native", "volatile",
		"assert", "try", "throw", "throws", "catch", "finally", "instanceof", "super", "this",
		"if", "else", "for", "do", "while", "switch", "case", "default",
		"continue", "break", "return", "synchronized", "transient",
		"true", "false", "null"
		};
	
	private final static char[] operators = {
		'(', ')', '{', '}', '.', ',', ';', '=', '+', '-',
		'/', '*', '&', '!', '|', ':', '[', ']', '<', '>',
		'?', '~', '%', '^'};


	public static CharacterEncodings getCharacterEncodings(){
		if(_theOne == null){
			_theOne = new CharacterEncodingsJava();
		}
		return _theOne;
	}
	
	private CharacterEncodingsJava(){
		super.registerKeywords(keywords);
		super.registerOperators(operators);
	}
	
	/**
	 * Java has no preprocessors. Override base class implementation
	 */
	public boolean isPreprocessor(char c){
		return false;
	}
}
