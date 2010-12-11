/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

import com.myopicmobile.textwarrior.common.Language;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Pair;
import com.myopicmobile.textwarrior.common.TextWarriorException;

import java.util.Vector;

/**
 * Responsibilities:
 * 1. Storing the currently used syntax and encoding set
 * 2. Lexical analysis
 * 
 * @author prokaryx
 *
 */
public class TokenScanner implements Runnable {
	private static Language _language = null;
	
	public final static int NORMAL = 0;
	public final static int KEYWORD = 1;
	public final static int OPERATOR = 2;
	public final static int PREPROCESSOR = 3;
	public final static int COMMENT = 4;
	public final static int MULTILINE_COMMENT = 5;
	public final static int CHAR_LITERAL = 6;
	public final static int STRING_LITERAL = 7;
	public final static int UNKNOWN = 8;
	private final static int MAX_KEYWORD_LENGTH = 31;
	
	public void run(){
		//TODO make token scanning a separate thread
	}
	
	public static void setLanguage(Language c){
		_language = c;
	}
	
	public static Language getLanguage(){
		return _language;
	}
	
	/**
	 * Scans hDoc for tokens, reading up to a maximum of maxChars.
	 * The offset in hDoc to start analysing is assumed to be set
	 * prior to calling this function.
	 * 
	 * The result is a collection of Pairs, where pair.first is
	 * the start position of the token, and pair.second is
	 * the type of the token.
	 * 
	 * @param hDoc
	 * @return A collection containing information about the tokens in hDoc
	 */
	public static Vector tokenize(DocumentProvider hDoc, int offset, int maxChars){
		Vector tokens = new Vector();
		if(!_language.isProgLang()){
			tokens.addElement(new Pair(0, NORMAL));
			return tokens;
		}
		
		char[] candidateWord = new char[MAX_KEYWORD_LENGTH];
		int currentCharInWord = 0;

		int partitionStartPosition = 0;
		int workingPosition = 0;
		int state = UNKNOWN;
		char prevChar = 0;

	    hDoc.seekChar(offset);
		while (hDoc.hasNext() && workingPosition < maxChars){
			char currentChar = hDoc.next();
			
			switch(state){
			case UNKNOWN: //fall-through
			case NORMAL: //fall-through
			case KEYWORD:
				
				if (_language.isComment(prevChar, currentChar) ||
					_language.isStringQuote(currentChar) ||
					_language.isCharQuote(currentChar) ||
					_language.isPreprocessor(currentChar)){
					
					if (_language.isComment(prevChar, currentChar)){
						// account for previous '/' char
						partitionStartPosition = workingPosition - 1;
						//TODO consider less greedy approach and avoid adding token for previous operator '/'
						if(((Pair) tokens.lastElement()).getFirst() == partitionStartPosition){
							tokens.removeElementAt(tokens.size() - 1);
						}
					}
					else{
						partitionStartPosition = workingPosition;
					}

					// If a quotation, preprocessor directive or comment appears mid-word,
					// mark the chars preceding it as NORMAL, if the previous partition isn't already NORMAL
					if(currentCharInWord > 0 && state != NORMAL){
						tokens.addElement(new Pair(workingPosition - currentCharInWord, NORMAL));
					}
					
					// change state
					if (_language.isComment(currentChar)){
						state = COMMENT;
					}
					else if (_language.isMultiLineComment(currentChar)){
						state = MULTILINE_COMMENT;
					}
					else if (_language.isStringQuote(currentChar)){
						state = STRING_LITERAL;	
					}
					else if (_language.isCharQuote(currentChar)){
						state = CHAR_LITERAL;	
					}
					else if (_language.isPreprocessor(currentChar)){
						state = PREPROCESSOR;
					}

					tokens.addElement(new Pair(partitionStartPosition, state));
					currentCharInWord = 0;
				}
				else if (_language.isWhitespace(currentChar) ||
						_language.isOperator(currentChar)){
					// full word obtained; check if it is a keyword
					if (currentCharInWord > 0 &&
							currentCharInWord <= MAX_KEYWORD_LENGTH){
						String lookupWord = new String(candidateWord, 0, currentCharInWord);
						boolean foundKeyword = _language.isKeyword(lookupWord);
						
						if( foundKeyword && (state == NORMAL || state == UNKNOWN)){
							partitionStartPosition = workingPosition - currentCharInWord;
							state = KEYWORD;
							tokens.addElement(new Pair(partitionStartPosition, state));
						}
						else if ( !foundKeyword && (state == KEYWORD || state == UNKNOWN)){
							partitionStartPosition = workingPosition - currentCharInWord;
							state = NORMAL;
							tokens.addElement(new Pair(partitionStartPosition, state));
						}
						currentCharInWord = 0;
					}
					
					// set state to NORMAL if encountered an operator
					// and previous state was not NORMAL
					if (_language.isOperator(currentChar) &&
							(state == KEYWORD || state == UNKNOWN)){
						partitionStartPosition = workingPosition;
						state = NORMAL;
						tokens.addElement(new Pair(partitionStartPosition, state));
					}
				}
				else {
					// collect non-whitespace chars up to MAX_KEYWORD_LENGTH
					if (currentCharInWord < MAX_KEYWORD_LENGTH){
						candidateWord[currentCharInWord] = currentChar;
					}
					currentCharInWord++;
				}
				break;
				
			
			case COMMENT: // fall-through
			case PREPROCESSOR:
				if (currentChar == '\n'){
					state = UNKNOWN;
				}
				break;
				
				
			case CHAR_LITERAL:
				// also handles escape sequence \'
				if ((currentChar == '\'' && prevChar != '\\') ||
					currentChar == '\n'){
					state = UNKNOWN;
				}
				// consume \\ by assigning currentChar as something else
				// so that it would not be treated as an escape char in the
				// next iteration
				else if (currentChar == '\\' && prevChar == '\\'){
					currentChar = ' ';
				}
				break;
				
				
			case STRING_LITERAL:
				// also handles escape sequence \"
				if ((currentChar == '"' && prevChar != '\\') ||
					currentChar == '\n'){
					state = UNKNOWN;
				}
				// consume \\ by assigning currentChar as something else
				// so that it would not be treated as an escape char in the
				// next iteration
				else if (currentChar == '\\' && prevChar == '\\'){
					currentChar = ' ';
				}
				break;
				
				
			default:
				TextWarriorException.assert(false, "Invalid state in TokenScanner");
				break;
			}
			++workingPosition;
			prevChar = currentChar;
		}
		
		if (tokens.isEmpty()){
			// return value cannot be empty
			tokens.addElement(new Pair(0, NORMAL));
		}
		return tokens;
	}
}
