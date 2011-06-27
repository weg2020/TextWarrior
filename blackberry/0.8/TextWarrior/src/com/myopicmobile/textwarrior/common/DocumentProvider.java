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
 * Iterator class to abstract accessing the chars of the underlying
 * text object.
 * 
 * The usage procedure is as follows:
 * 1. Call seekLine(lineNumber) to mark the position to start iterating
 * 2. Call hasNext() to see if there are any more char
 * 3. Call next() to get the next char
 *
 * Limitation: If there is more than 1 DocumentProvider pointing to the
 * same Document, any changes made to the Document by one DocumentProvider will
 * not be notified to the other DocumentProviders. Implement a publish/subscribe
 * interface if the need arises.
 */
public class DocumentProvider {
	/**
	 * Current position in the text.
	 * Range [ 0, _theText.getTextLength() )
	 */
	private int _currIndex;
	private TextBuffer _theText;
	
	public DocumentProvider(TextBuffer buf){
		_currIndex = 0;
		_theText = buf;
	}
	
	private DocumentProvider(){
		// does not make sense to create an iterator
		// without an associated text buffer
	}
	
	public char[] getChars(int logicalIndex, int maxChars){
		return _theText.getChars(logicalIndex, maxChars);
	}
	
	public char getChar(int charOffset){
		if(_theText.isValid(charOffset)){
			return _theText.getChar(charOffset);
		}
		else{
			return Language.NULL_CHAR;
		}
	}
	
	public int getRowIndex(int charOffset){
		return _theText.getRowIndex(charOffset);
	}
	
	public int getStartCharOfRow(int rowIndex){
		return _theText.getCharOffset(rowIndex);
	}
	
	/**
	 * Sets the current character to be first character on startingLine.
	 * 
	 * If the line does not exist, hasNext() will return false,
	 * and _currIndex will be set to -1.
	 * 
	 * @param startingLine
	 * @return The index of the first character on startingLine,
	 * 			or -1 if startingLine does not exist
	 */
	public int seekLine(int startingLine){
		_currIndex = _theText.getCharOffset(startingLine);
		return _currIndex;
	}
	
	public int seekChar(int startingChar){
		if(_theText.isValid(startingChar)){
			_currIndex = startingChar;
		}
		else{
			_currIndex = -1;
		}
		return _currIndex;
	}
	
	public boolean hasNext(){
		return (_currIndex >= 0 &&
				_currIndex < _theText.getTextLength());
	}
	
	/**
	 * Returns the next character and moves the iterator forward.
	 * Does not do bounds-checking. It is the responsibility of the caller
	 * to check hasNext() first.
	 * 
	 * @return Next character
	 */
	public char next(){
		char nextChar = _theText.getChar(_currIndex);
		++_currIndex;
		return nextChar;
	}
	
	/**
	 * Inserts c into insertionPoint index.
	 * 
	 * @param offset
	 * @param c
	 */
	public void insertBefore(char c, int insertionPoint){
		char[] a = new char[1];
		a[0] = c;
		_theText.insert(a, insertionPoint);
	}

	/**
	 * Inserts characters of cArray into insertionPoint index.
	 * 
	 * @param offset
	 * @param c
	 */
	public void insertBefore(char[] cArray, int insertionPoint){
		_theText.insert(cArray, insertionPoint);
	}

	/**
	 * Deletes the character at deletionPoint index.
	 * 
	 * @param offset
	 * @param c
	 */
	public void deleteAt(int deletionPoint){
		_theText.delete(deletionPoint, 1);
	}
	

	public void deleteAt(int deletionPoint, int maxChars){
		_theText.delete(deletionPoint, maxChars);
	}
	
	public int lineLength(int lineNumber){
		return _theText.getLineLength(lineNumber);
	}
	
	public int docLength(){
		return _theText.getTextLength();
	}
}
