/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import com.myopicmobile.textwarrior.common.CharacterEncodings;
import com.myopicmobile.textwarrior.common.TextWarriorException;

public class TextBuffer {
	protected final static int MIN_GAP_SIZE = 50; // must be > 0 to insert into full buffers successfully
	protected final static int INVALID_INDEX = -16;
	protected byte[] _contents;
	protected int _gapStartIndex;
	protected int _gapEndIndex; // one past end of gap
	protected FileFormatConverter _textMetadata;
	
	/*
	 * Store the last seeked line and its corresponding index so that future
	 * lookups can start from the cached position instead of the beginning
	 * of the file
	 */
	protected int _cachedLine;
	protected int _cachedLineIndex;

	public TextBuffer(){
		_contents = new byte[MIN_GAP_SIZE + 1]; // extra byte for EOF
		_contents[MIN_GAP_SIZE] = (byte) CharacterEncodings.EOF;
		_gapStartIndex = 0;
		_gapEndIndex = MIN_GAP_SIZE;
		_textMetadata = new FileFormatConverter();
		invalidateCache();
	}

	public void read(InputStream byteStream, int fileSize) throws TextWarriorException{
		byte[] newBuffer = new byte[fileSize + MIN_GAP_SIZE + 1]; // extra byte for EOF
		// resulting gap may be > MIN_GAP_SIZE if Windows-style newlines are stripped
		int convertedSize = _textMetadata.readAndConvert(byteStream, newBuffer);
		_contents = newBuffer;
		initGap(convertedSize);
	}
	
	public void write(OutputStream byteStream) throws TextWarriorException{
		int textSize = closeGap();
		_textMetadata.writeAndConvert(byteStream, _contents, textSize);
	}
	
	/**
	 * Get the logical index of the first character of targetLine,
	 * counting from the beginning of the text.
	 * 
	 * @param targetLine The number of the line of interest
	 * @return The logical index where the line begins,
	 * or -1 if the line does not exist
	 */
	public int seekLine(int targetLine){
		int offset = getFirstValidRealIndex();
		
		// search from the cached position,
		// if it is faster than searching from the start of file
		if (isCacheValid()){
			int midpoint = _cachedLine / 2;
			if ( targetLine >= _cachedLine){
				// search from the cached position
				offset = findLineIndex(targetLine, _cachedLine, _cachedLineIndex);
			}
			else if (midpoint <= targetLine && targetLine < _cachedLine){
				// search backwards from the cached position
				offset = findLineIndexBackward(targetLine, _cachedLine, _cachedLineIndex);
			}
			else{
				// search from the beginning
				offset = findLineIndex(targetLine, 0, offset);
			}
		}
		else{
			offset = findLineIndex(targetLine, 0, offset);
		}
		
		if (offset != -1){
			// seek successful
			_cachedLine = targetLine;
			_cachedLineIndex = offset;
		}
		return realToLogicalIndex(offset);
	}
	
	/**
	 * Precondition: startingOffset is the offset of startingLine
	 * 
	 * @param targetLine
	 * @param startingLine
	 * @param startingOffset
	 * @return
	 */
	private int findLineIndex(int targetLine, int startingLine, int startingOffset){
		int lineCount = startingLine;
		int offset = startingOffset;

		TextWarriorException.assert(startingOffset < _gapStartIndex ||
				startingOffset >= _gapEndIndex,
				"startingOffset given when searching forwards is in the gap");
		
		while((lineCount < targetLine) && (offset < _contents.length)){
			if (((char) _contents[offset]) == '\n'){
				++lineCount;
			}
			++offset;
			
			// skip the gap
			if(offset == _gapStartIndex){
				offset = _gapEndIndex;
			}
		}

		if (lineCount != targetLine){
			return -1;
		}
		return offset;
	}
	
	/**
	 * Precondition:
	 * 1. startingOffset is the offset of startingLine
	 * 2. startingOffset is not in the gap
	 * 3. targetLine <= startingLine
	 * 
	 * @param targetLine
	 * @param startingLine
	 * @param startingOffset
	 * @return
	 */
	private int findLineIndexBackward(int targetLine, int startingLine, int startingOffset){
		int workingLine = startingLine;
		int offset = startingOffset;
		
		if (targetLine == 0){
			// return first valid position
			offset = getFirstValidRealIndex();
		}
		else{
			TextWarriorException.assert(startingOffset < _gapStartIndex ||
					startingOffset >= _gapEndIndex,
					"startingOffset given when searching backwards is in the gap");
			
			while((workingLine > (targetLine-1)) && (offset > 0)){
				// skip behind the gap
				if(offset == _gapEndIndex){
					offset = _gapStartIndex;
				}
				--offset;
				
				if (((char) _contents[offset]) == '\n'){
					--workingLine;
				}

			}
			
			if(workingLine == (targetLine-1)){
				// now at the '\n' of the line before targetLine.
				++offset;
				// skip the gap
				if(offset == _gapStartIndex){
					offset = _gapEndIndex;
				}
			}
			else{
				// Cannot reach targetLine.
				// The text file has less than startingLine-targetLine lines.
				// Arguments given to this function are wrong.
				TextWarriorException.assert(false,
						"Cannot find specified line when searching backwards");
				offset = -1;
			}
		}
		
		return offset;
	}
	
	public int offsetToLineNumber(int logicalIndex){
		if(!isValid(logicalIndex)){
			return -1;
		}
		
		int targetIndex = logicalToRealIndex(logicalIndex);
		int lineCount = 0;
		int offset = getFirstValidRealIndex();
		
		// search from the cached position,
		// if it is faster than searching from the start of file
		if (isCacheValid()){
			int midpoint = _cachedLineIndex / 2;
			if ( targetIndex >= _cachedLineIndex){
				// init to search forward from the cached position
				lineCount = _cachedLine;
				offset = _cachedLineIndex;
			}
			else if (midpoint <= targetIndex && targetIndex < _cachedLineIndex){
				// search backwards from the cached position
				lineCount = _cachedLine;
				offset = _cachedLineIndex;
				while((offset > targetIndex) && (offset > 0)){
					// skip behind the gap
					if(offset == _gapEndIndex){
						offset = _gapStartIndex;
					}
					--offset;
					
					if (((char) _contents[offset]) == '\n'){
						// update cache
						_cachedLine = lineCount;
						_cachedLineIndex = offset+1;
						if(_cachedLineIndex == _gapStartIndex){
							_cachedLineIndex = _gapEndIndex;
						}
						
						--lineCount;
					}
				}
			} // end backwards search; will not enter forward search below
			else{
			// search forward from the beginning. init done already.
			}
		}
		
		// search forward from init position
		while((offset < targetIndex) && (offset < _contents.length)){			
			if (((char) _contents[offset]) == '\n'){
				++lineCount;
				
				// update cache
				_cachedLine = lineCount;
				_cachedLineIndex = offset+1;
				if(_cachedLineIndex == _gapStartIndex){
					_cachedLineIndex = _gapEndIndex;
				}
			}
			
			++offset;
			// skip the gap
			if(offset == _gapStartIndex){
				offset = _gapEndIndex;
			}
		}

		if (offset != targetIndex){
			return -1;
		}
		return lineCount;
	}
	
	/**
	 * Precondition: startingPosition is not in the gap
	 * 
	 * @param target
	 * @param startingPosition
	 * @param isCaseSensitive
	 * @param isWholeWord
	 * @return
	 */
	public int find(String target, int startingPosition,
		boolean isCaseSensitive, boolean isWholeWord){
		if(startingPosition >= getTextLength()){
			startingPosition = 0; // wrap-around search
		}
		TextWarriorException.assert(startingPosition >= 0, "Invalid logicalIndex given to TextBuffer.find");
		
		int offset = logicalToRealIndex(startingPosition);
		// mark start position so search can stop after one wrap-around pass of the document
		int lastSearchChar = offset;
		boolean found = false;
		
		while(!found && offset < _contents.length){
			found = equals(target, offset, isCaseSensitive) &&
				(!isWholeWord || isSandwichedByWhitespace(offset, target.length()) );
			
			++offset;
			// skip the gap
			if(offset == _gapStartIndex){
				offset = _gapEndIndex;
			}
		}
		
		if (!found){
			// search from beginning of document
			offset = getFirstValidRealIndex();
			//TODO refactor duplication
			while(!found && offset < lastSearchChar){	
				found = equals(target, offset, isCaseSensitive) &&
					(!isWholeWord || isSandwichedByWhitespace(offset, target.length()) );
				
				++offset;
				// skip the gap
				if(offset == _gapStartIndex){
					offset = _gapEndIndex;
				}
			}
		}
		
		
		if (found){
			// skip behind the gap
			if(offset == _gapEndIndex){
				offset = _gapStartIndex;
			}
			--offset; //went one-past desired char in last iteration of while

			return realToLogicalIndex(offset);
		}
		return -1;
	}
	
	/**
	 * Precondition: startingPosition is not in the gap
	 * 
	 * @param target
	 * @param startingPosition
	 * @param isCaseSensitive
	 * @param isWholeWord
	 * @return
	 */
	public int findBackwards(String target, int startingPosition,
		boolean isCaseSensitive, boolean isWholeWord){
		if(startingPosition < 0){
			startingPosition = getTextLength() - 1; // wrap-around search
		}
		TextWarriorException.assert(startingPosition < (getTextLength() - 1),
				"Invalid logicalIndex given to TextBuffer.find");
		
		int offset = logicalToRealIndex(startingPosition);
		// mark start position so search can stop after one wrap-around pass of the document
		int lastSearchChar = offset;
		boolean found = false;
		
		while(!found && offset >= 0){
			found = equals(target, offset, isCaseSensitive) &&
				(!isWholeWord || isSandwichedByWhitespace(offset, target.length()) );

			// skip behind the gap
			if(offset == _gapEndIndex){
				offset = _gapStartIndex;
			}
			--offset;
		}
		
		if (!found){
			// search from end of document
			offset = _contents.length - 1;
			//TODO refactor duplication
			while(!found && offset > lastSearchChar){	
				found = equals(target, offset, isCaseSensitive) &&
					(!isWholeWord || isSandwichedByWhitespace(offset, target.length()) );
				
				// skip behind the gap
				if(offset == _gapEndIndex){
					offset = _gapStartIndex;
				}
				--offset;
			}
		}
		
		
		if (found){
			++offset; //went one-past desired char in last iteration of while
			// skip the gap
			if(offset == _gapStartIndex){
				offset = _gapEndIndex;
			}

			return realToLogicalIndex(offset);
		}
		return -1;
	}
	
	protected boolean equals(String target, int startingPosition, boolean isCaseSensitive){
		for(int i = 0, j = startingPosition;
			i < target.length() && j < _contents.length;
			++i){
			if (isCaseSensitive &&
					target.charAt(i) != (char) _contents[j]){
				return false;
			}
			// for case-insensitive search, compare both strings in lower case
			if (!isCaseSensitive &&
					Character.toLowerCase(target.charAt(i)) != 
					Character.toLowerCase((char) _contents[j])){
				return false;
			}
			
			if (i == (target.length() - 1)){
				// all characters matched
				return true;
			}
			
			++j;
			// skip the gap
			if (j == _gapStartIndex){
				j = _gapEndIndex;
			}
		}
		// ran out of chars to compare
		return false;
	}
	
	/**
	 * Checks if a word starting at startPosition and length
	 * is bounded by whitespace. Takes gap into account.
	 * 
	 * @param startPosition
	 * @param endPosition
	 * @return
	 */
	protected boolean isSandwichedByWhitespace(int startPosition, int length){
		int endPosition = startPosition + length;
		if (isInGap(endPosition)){
			endPosition += gapSize();
		}
		
		boolean startWithWhitespace = false;
		if (startPosition == 0){
			startWithWhitespace = true;
		}
		else{
			char leftLimit = (char) _contents[startPosition - 1];
			startWithWhitespace = isWhitespace(leftLimit);
		}
		
		boolean endWithWhitespace = false;
		if (endPosition == _contents.length){
			endWithWhitespace = true;
		}
		else{
			char rightLimit = (char) _contents[endPosition];
			endWithWhitespace = isWhitespace(rightLimit);
		}
		
		return (startWithWhitespace && endWithWhitespace);
	}
	
	/**
	 * Finds the number of char on the specified line.
	 * All valid lines contain at least one char, which may be a non-printable
	 * one like \n, \t or EOF.
	 * 
	 * If the line does not exist, 0 is returned.
	 * 
	 * @param startingLine
	 * @return The number of chars in targetLine, or 0 if the line does not exist.
	 */
	public int getLineLength(int targetLine){
		int lineLength = 0;
		int pos = seekLine(targetLine);
		
		if (pos != -1){
			pos = logicalToRealIndex(pos);
			//TODO consider adding check for (pos < _contents.length) in case EOF is not properly set
			while( ((char) _contents[pos]) != '\n' &&
			 ((char) _contents[pos]) != CharacterEncodings.EOF){
				++lineLength;
				++pos;
				
				// skip the gap
				if(pos == _gapStartIndex){
					pos = _gapEndIndex;
				}
			}
			++lineLength; // account for the line terminator char
		}
		
		return lineLength;
	}
	
	/**
	 * Gets the char at logicalIndex
	 * Does not do bounds-checking.
	 * 
	 * @param logicalIndex
	 * @return The char at logicalIndex. If logicalIndex is invalid,
	 * the result is undefined.
	 */
	public char getChar(int logicalIndex){
		/*
		if(!isValid(logicalIndex)){
			return CharacterEncodings.NULL_CHAR;
		}
		*/
		
		int realIndex = logicalToRealIndex(logicalIndex);
		//TODO take unicode char into account
		return (char) _contents[realIndex];
	}
	

	/**
	 * Gets up to maxChars number of chars starting at logicalIndex
	 * 
	 * @param logicalIndex
	 * @return The chars starting from logicalIndex, up to a maximum
	 * of maxChars, or an empty array if the logicalIndex is invalid
	 */
	public char[] getChars(int logicalIndex, int maxChars){
		if(!isValid(logicalIndex)){
			return new char[0];
		}
		int realIndex = logicalToRealIndex(logicalIndex);
		char[] chars = new char[maxChars];
		
		for (int i = 0; i < maxChars; ++i){
			if (realIndex < _contents.length){
				//TODO take unicode char into account
				chars[i] = (char) _contents[realIndex];
				++realIndex;
				// skip the gap
				if(realIndex == _gapStartIndex){
					realIndex = _gapEndIndex;
				}
			}
			else{
				chars[i] = CharacterEncodings.NULL_CHAR;
			}
		}
		
		return chars;
	}

	/**
	 * Insert all characters in c into position logicalIndex.
	 * If logicalIndex is invalid, nothing happens.
	 * 
	 * @param c
	 * @param logicalIndex
	 */
	public void insert(char[] c, int logicalIndex){
		if(!isValid(logicalIndex)){
			return;
		}
		
		int insertIndex = logicalToRealIndex(logicalIndex);
		
		// shift gap to insertion point
		if (insertIndex != _gapEndIndex){
			if (isBeforeGap(insertIndex)){
				shiftGapLeft(insertIndex);
			}
			else{
				shiftGapRight(insertIndex);
			}
		}

		for (int i = 0; i < c.length; ++i){
			if (gapSize() == 0){
				reallocBuffer(_gapStartIndex);
			}
	
			//TODO take unicode char into account
			_contents[_gapStartIndex] = (byte) c[i];
			++_gapStartIndex;
		}
		
		invalidateCache();
	}

	/**
	 * Deletes up to maxChars number of char starting from position logicalIndex.
	 * If logicalIndex is invalid, nothing happens.
	 * 
	 * @param c
	 * @param logicalIndex
	 */
	public void delete(int logicalIndex, int maxChars){
		if(!isValid(logicalIndex)){
			return;
		}
		
		int deleteIndex = logicalToRealIndex(logicalIndex);
		
		// shift gap to deletion point
		if (deleteIndex != _gapEndIndex){
			if (isBeforeGap(deleteIndex)){
				shiftGapLeft(deleteIndex);
			}
			else{
				shiftGapRight(deleteIndex);
			}
		}

		// increase gap size
		for(int i = 0;
			i < maxChars && _gapEndIndex < (_contents.length-1);
			++i){
			++_gapEndIndex;
		}
		invalidateCache();
	}
	
	/**
	 * Adjusts gap so that _gapStartIndex is at newStart
	 * 
	 * @param newStart
	 */
	final protected void shiftGapLeft(int newGapStart){
		while(_gapStartIndex > newGapStart){
			--_gapEndIndex;
			--_gapStartIndex;
			_contents[_gapEndIndex] = _contents[_gapStartIndex];
		}
	}

	/**
	 * Adjusts gap so that _gapEndIndex is at newEnd
	 * 
	 * @param newStart
	 */
	final protected void shiftGapRight(int newGapEnd){
		while(_gapEndIndex < newGapEnd){
			_contents[_gapStartIndex] = _contents[_gapEndIndex];
			++_gapStartIndex;
			++_gapEndIndex;
		}
	}
	
	/**
	 * Create gap at start of buffer and tack a EOF at the end
	 * Precondition: real contents are from _contents[0] to _contents[convertedSize-1]
	 * 
	 * @param convertedSize
	 */
	protected void initGap(int convertedSize){
		int toPosition = _contents.length - 1;
		_contents[toPosition--] = (byte) CharacterEncodings.EOF; // mark end of file
		int fromPosition = convertedSize - 1;
		while(fromPosition >= 0){
			_contents[toPosition--] = _contents[fromPosition--];
		}
		_gapStartIndex = 0;
		_gapEndIndex = toPosition + 1; // went one-past in the while loop
	}

	/**
	 * Compacts all contents to the start of buffer, leaving the gap and EOF at the end
	 * Postcondition: real contents are from _contents[0] to _contents[convertedSize-1]
	 * 
	 * @param convertedSize Size of actual content, excluding the gap and EOF
	 */ 
	protected int closeGap(){
		shiftGapRight(_contents.length - 1);
		return _gapStartIndex;
	}
	
	/**
	 * Copies _contents into a buffer larger in size by INITIAL_GAP_SIZE bytes.
	 * A gap is inserted such that newBuffer[_gapStartIndex] == oldBuffer[newGapStart-1]
	 * 
	 * This makes space to insert characters between oldBuffer[newGapStart-1]
	 * and oldBuffer[newGapStart]
	 * 
	 * @param newGapStart
	 */
	protected void reallocBuffer(int newGapStart){
		//TODO handle new size > MAX_INT or allocation failure
		int newSize = _contents.length + MIN_GAP_SIZE;
		byte[] temp = new byte[newSize];
		int i = 0;
		while(i < newGapStart){
			temp[i] = _contents[i];
			++i;
		}
		
		while(i < _contents.length){
			temp[i + MIN_GAP_SIZE] = _contents[i];
			++i;
		}
		
		_contents = temp;
		_gapStartIndex = newGapStart;
		_gapEndIndex = newGapStart + MIN_GAP_SIZE;
		invalidateCache();
	}
	
	/**
	 * 
	 * @return Number of char in text document, including EOF
	 * but excluding the gap
	 */
	final public int getTextLength(){
		return _contents.length - gapSize();
	}
	
	final protected boolean isValid(int logicalIndex){
		if(logicalIndex >= 0 && logicalIndex < getTextLength()){
			return true;
		}
		
		TextWarriorException.assert(false,
				"Invalid logicalIndex given to TextBuffer");
		return false;
	}
	
	final protected int gapSize(){
		return _gapEndIndex - _gapStartIndex;
	}
	
	final protected int logicalToRealIndex(int i){
		if (isBeforeGap(i)){
			return i;
		}
		else{
			return i + gapSize(); 
		}
	}
	
	/**
	 * If i is negative, i will be returned
	 * 
	 * @param i
	 * @return
	 */
	final protected int realToLogicalIndex(int i){
		if (isBeforeGap(i)){
			return i;
		}
		else{
			return i - gapSize(); 
		}
	}
	
	final protected boolean isBeforeGap(int i){
		return i < _gapStartIndex;
	}

	final protected boolean isInGap(int i){
		return (i >= _gapStartIndex && i < _gapEndIndex);
	}
	
	final protected int getFirstValidRealIndex(){
		return (_gapStartIndex == 0) ? _gapEndIndex : 0;
	}
	
	final protected void invalidateCache(){
		_cachedLine = INVALID_INDEX;
		_cachedLineIndex = INVALID_INDEX;
	}
	
	final protected boolean isCacheValid(){
		return (_cachedLine >= 0);
	}

	//TODO should move to CharacterEncodings hierarchy
	public boolean isWhitespace(char c){
		return (c == ' ' || c == '\n'|| c == '\t' ||
			c == CharacterEncodings.EOF);
	}
	/********************************************************
	 * 
	 * Helper class to convert between different line
	 * terminator formats
	 *
	 *******************************************************/
	private class FileFormatConverter {
		
		private int _newlineStyle;
		private boolean _isUnicode;
		
		public FileFormatConverter(){
			_newlineStyle = CharacterEncodings.UNKNOWN;
			_isUnicode = false;
		}
		
		/**
		 * Reads bytes from byteStream into buffer,
		 * normalising all line terminators to UNIX style '\n'
		 * 
		 * @param byteStream
		 * @param buffer\
		 * @return size of converted text
		 * @throws TextWarriorException
		 */
		public int readAndConvert(InputStream byteStream, byte[] buffer)
		throws TextWarriorException{
			try {
				int currCharRead;
				int prevCharRead = -1;
				int i = 0;
				while((currCharRead = byteStream.read()) != -1){
					buffer[i] = (byte) currCharRead;
				
					if (_newlineStyle == CharacterEncodings.UNIX){
						++i;
						// read remaining bytes into _parsedContents
						i += byteStream.read(buffer, i, buffer.length-i);
					}
					
					else if (_newlineStyle == CharacterEncodings.MAC &&
					((char) currCharRead == '\r')){
						buffer[i] = (byte) '\n';
					}
					
					else if (_newlineStyle == CharacterEncodings.WINDOWS &&
					((char) currCharRead == '\r')){
						//TODO assume next byte is '\n' and read it in
						// discard '\r'
						buffer[i] = (byte) byteStream.read();
					}
					
					else{
						// line terminator style unknown
						if(((char) prevCharRead == '\r') &&
						((char) currCharRead == '\n')){
							_newlineStyle = CharacterEncodings.WINDOWS;
							// replace \r\n with \n
							buffer[i] = 0;
							--i;
							buffer[i] = (byte) '\n';
						}
						else if(((char) prevCharRead == '\r') &&
						((char) currCharRead != '\n')){
							// replace \r with \n and possibly current char too
							_newlineStyle = CharacterEncodings.MAC;
							buffer[i-1] = (byte) '\n';
							if ((char) currCharRead == '\r'){
								buffer[i] = (byte) '\n';
							}
						}
						else if ((char) currCharRead == '\n'){
							_newlineStyle = CharacterEncodings.UNIX;
						}
						
						prevCharRead = currCharRead;
					}
					
					// ** don't forget to increment i!
					++i;
				}
				
				if (_newlineStyle == CharacterEncodings.UNKNOWN){
					// Encountered file with no newlines or some other line terminators
					// other than /n, /r or /r/n
					// Set it to UNIX style line terminators
					_newlineStyle = CharacterEncodings.UNIX;
				}
				
				return i;
			}
			catch (IOException ex) { 
				throw new TextWarriorException(ex.getMessage());
	        } 
		}
		

		public void writeAndConvert(OutputStream byteStream,
		byte[] buffer, int textSize) throws TextWarriorException{
			try {
				if (_newlineStyle == CharacterEncodings.UNIX ||
					_newlineStyle == CharacterEncodings.UNKNOWN){
					byteStream.write(buffer, 0, textSize);
				}
				else if (_newlineStyle == CharacterEncodings.MAC){
					for(int i = 0; i < textSize; ++i){
						if(((char) buffer[i]) == '\n'){
							byteStream.write('\r');
						}
						else{
							byteStream.write(buffer[i]);
						}
					}
				}
				else if (_newlineStyle == CharacterEncodings.WINDOWS){
					for(int i = 0; i < textSize; ++i){
						if(((char) buffer[i]) == '\n'){
							byteStream.write('\r');
							byteStream.write('\n');
						}
						else{
							byteStream.write(buffer[i]);
						}
					}
				}
				else{
					TextWarriorException.assert(false,
						"Invalid line terminator type");
				}
			}
			catch (IOException ex) { 
				throw new TextWarriorException(ex.getMessage());
	        }	
		}
	}// end inner class
}
