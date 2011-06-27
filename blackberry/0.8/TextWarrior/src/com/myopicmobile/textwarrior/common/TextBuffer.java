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

import com.myopicmobile.textwarrior.common.Language;
import com.myopicmobile.textwarrior.common.TextWarriorException;


//TODO Have all methods work with charOffsets and move all gap handling to logicalToRealIndex()
public class TextBuffer {
	// gap size must be > 0 to insert into full buffers successfully
	protected final static int MIN_GAP_SIZE = 50;
	protected char[] _contents;
	protected int _gapStartIndex;
	protected int _gapEndIndex; // one past end of gap
	protected FileFormatConverter _textMetadata;
	protected String _originalFormat;
	protected String _originalEOLType;
	private TextBufferCache _cache;

	public TextBuffer(){
		_contents = new char[MIN_GAP_SIZE + 1]; // extra char for EOF
		_contents[MIN_GAP_SIZE] = Language.EOF;
		_gapStartIndex = 0;
		_gapEndIndex = MIN_GAP_SIZE;
		_textMetadata = new FileFormatConverter();
		_cache = new TextBufferCache();
		_originalFormat = TextWarriorOptions.TEXT_ENCODING_UTF8;
		_originalEOLType = TextWarriorOptions.LINE_BREAK_LF;
	}

	public void read(InputStream byteStream, int fileSize, String encoding,
			String EOLstyle, Flag abort)
	throws IOException{
		_originalFormat = encoding;
		if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_AUTO)){
			_originalFormat = _textMetadata.getEncodingScheme(byteStream);
		}

		_originalEOLType = EOLstyle;
		if(EOLstyle.equals(TextWarriorOptions.LINE_BREAK_AUTO)){
			_originalEOLType = _textMetadata.getEOLType(byteStream, _originalFormat);
		}
		
		
		int bufferSize = fileSize;
		if(_originalFormat.equals(TextWarriorOptions.TEXT_ENCODING_UTF16BE) ||
				_originalFormat.equals(TextWarriorOptions.TEXT_ENCODING_UTF16LE)){
			bufferSize /= 2; // 2 bytes in the file == 1 char
		}
		
		char[] newBuffer = new char[bufferSize + MIN_GAP_SIZE + 1]; // extra char for EOF
		int convertedSize = _textMetadata.readAndConvert(byteStream, newBuffer,
				_originalFormat, _originalEOLType, abort);
		_contents = newBuffer;
		initGap(convertedSize);
	}
	
	public void write(OutputStream byteStream, String encoding, String EOLstyle, Flag abort)
	throws IOException{
		String enc = encoding;
		if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_AUTO)){
			enc = _originalFormat;
		}
		String EOL = EOLstyle;
		if(EOLstyle.equals(TextWarriorOptions.LINE_BREAK_AUTO)){
			EOL = _originalEOLType;
		}
		
		_textMetadata.writeAndConvert(byteStream, new DocumentProvider(this), enc, EOL, abort);
		_originalFormat = enc;
		_originalEOLType = EOL;
	}

	/*
	 * Returns the current number of bytes processed by
	 * the read or write operation.
	 * 
	 * Usually called by other threads to observe progress
	 */
	final public int getProgress(){
		return _textMetadata.getProgress();
	}

	/**
	 * All callers of read() and write() MUST call this method after the
	 * read/write calls return. This ensures correct reporting of progress for
	 * a subsequent read/write operation.
	 *
	 * See ReadThread.run() and WriteThread.run() also.
	 */
	final public void clearProgress(){
		_textMetadata.clearProgress();
	}
	
	/**
	 * Get the offset of the first character of targetLine,
	 * counting from the beginning of the text.
	 * 
	 * @param targetRow The index of the row of interest
	 * @return The character offset of targetRow,
	 * or -1 if the line does not exist
	 */
	public int getCharOffset(int targetRow){
		// start search from nearest known rowIndex~charOffset pair
		Pair cachedEntry = _cache.getNearestRow(targetRow);
		int cachedRow = cachedEntry.getFirst();
		int cachedOffset = cachedEntry.getSecond();

		int offset;
		if (targetRow > cachedRow){
			offset = findCharOffset(targetRow, cachedRow, cachedOffset);
		}
		else if (targetRow < cachedRow){
			offset = findCharOffsetBackward(targetRow, cachedRow, cachedOffset);
		}
		else{
			offset = cachedOffset;
		}
		
		if (offset >= 0){
			// seek successful
			_cache.updateEntry(targetRow, offset);
		}

		return offset;
	}

	/**
	 * Precondition: startingOffset is the offset of startingLine
	 * 
	 * @param targetRow
	 * @param startRow
	 * @param startOffset
	 * @return
	 */
	private int findCharOffset(int targetRow, int startRow, int startOffset){
		int workingRow = startRow;
		int offset = logicalToRealIndex(startOffset);

		TextWarriorException.assert(isValid(startOffset),
			"findCharOffsetBackward: Invalid startingOffset given");
		
		while((workingRow < targetRow) && (offset < _contents.length)){
			if (_contents[offset] == Language.NEWLINE){
				++workingRow;
			}
			++offset;
			
			// skip the gap
			if(offset == _gapStartIndex){
				offset = _gapEndIndex;
			}
		}

		if (workingRow != targetRow){
			return -1;
		}
		return realToLogicalIndex(offset);
	}

	/**
	 * Precondition: startingOffset is the offset of startingLine
	 * 
	 * @param targetRow
	 * @param startRow
	 * @param startOffset
	 * @return
	 */
	private int findCharOffsetBackward(int targetRow, int startRow, int startOffset){
		if (targetRow == 0){
			return 0;
		}

		TextWarriorException.assert(isValid(startOffset),
			"findCharOffsetBackward: Invalid startingOffset given");
		
		int workingRow = startRow;
		int offset = logicalToRealIndex(startOffset);
		while(workingRow > (targetRow-1) && offset >= 0){ 
			// skip behind the gap
			if(offset == _gapEndIndex){
				offset = _gapStartIndex;
			}
			--offset;

			if (_contents[offset] == Language.NEWLINE){
				--workingRow;
			}

		}
		
		int charOffset;
		if (offset >= 0){
			// now at the '\n' of the line before targetRow
			charOffset = realToLogicalIndex(offset);
			++charOffset;
		}
		else{
			TextWarriorException.assert(false,
				"findCharOffsetBackward: Invalid cache entry or row arguments");
			charOffset = -1;
		}

		return charOffset;
	}

	public int getRowIndex(int charOffset){
		if(!isValid(charOffset)){
			return -1;
		}
		
		Pair cachedEntry = _cache.getNearestCharOffset(charOffset);
		int row = cachedEntry.getFirst();
		int offset = logicalToRealIndex(cachedEntry.getSecond());
		int targetOffset = logicalToRealIndex(charOffset);
		// Store the (row, charOffset) entry nearest to the targetOffset,
		// to be updated in the cache at the end of the function,
		// in anticipation of future lookups near the targetOffset
		int lastKnownRow = -1;
		int lastKnownCharOffset = -1;
		
		if (targetOffset > offset){
			// search forward
			while((offset < targetOffset) && (offset < _contents.length)){			
				if (_contents[offset] == Language.NEWLINE){
					++row;
					lastKnownRow = row;
					lastKnownCharOffset = realToLogicalIndex(offset) + 1;
				}
				
				++offset;
				// skip the gap
				if(offset == _gapStartIndex){
					offset = _gapEndIndex;
				}
			}
		}
		else if (targetOffset < offset){
			// search backward
			while((offset > targetOffset) && (offset > 0)){
				// skip behind the gap
				if(offset == _gapEndIndex){
					offset = _gapStartIndex;
				}
				--offset;
				
				if (_contents[offset] == Language.NEWLINE){
					lastKnownRow = row;
					lastKnownCharOffset = realToLogicalIndex(offset) + 1;
					--row;
				}
			}
		}


		if (offset == targetOffset){
			if(lastKnownRow != -1){
				// found a new (rowIndex, charOffset)
				_cache.updateEntry(lastKnownRow, lastKnownCharOffset);
			}
			return row;
		}
		else{
			return -1;
		}
	}
	
	/**
	 * Searches for target, starting from start (inclusive),
	 * wrapping around to the beginning of document and
	 * stopping at start (exclusive).
	 * 
	 * @param target
	 * @param start
	 * @param isCaseSensitive
	 * @param isWholeWord
	 * @return charOffset of found string; -1 if not found
	 */
	public int find(String target, int start,
		boolean isCaseSensitive, boolean isWholeWord){
		if(start >= getTextLength()){
			start = 0;
		}
		TextWarriorException.assert(start >= 0,
			"Invalid startCharOffset given to TextBuffer.find");

		int foundOffset = -1;
		// search towards end of doc first...
		foundOffset = realFind(target, start, getTextLength(),
				isCaseSensitive, isWholeWord);
		// ...then from beginning of doc
		if(foundOffset < 0){
			foundOffset = realFind(target, 0, start,
					isCaseSensitive, isWholeWord);
		}
		
		return foundOffset;
	}
	
	private int realFind(String target, int start, int end,
			boolean isCaseSensitive, boolean isWholeWord) {
		int offset = logicalToRealIndex(start);
		int searchLimit = logicalToRealIndex(end);
		while(offset < searchLimit){
			if(equals(target, offset, isCaseSensitive) &&
			(!isWholeWord || isSandwichedByWhitespace(offset, target.length())) ){
				break;
			}
			
			++offset;
			// skip the gap
			if(offset == _gapStartIndex){
				offset = _gapEndIndex;
			}
		}

		if (offset < searchLimit){
			return realToLogicalIndex(offset);
		}
		else{
			return -1;
		}
	}

	/**
	 * Searches backwards from startCharOffset (inclusive), wrapping around to
	 * the end of document and stopping at startCharOffset
	 * 
	 * @param target
	 * @param start
	 * @param isCaseSensitive
	 * @param isWholeWord
	 * @return
	 */
	public int findBackwards(String target, int start,
		boolean isCaseSensitive, boolean isWholeWord){
		if(start < 0){
			start = getTextLength() - 1;
		}
		TextWarriorException.assert(start < getTextLength(),
				"Invalid charOffset given to TextBuffer.find");

		int foundOffset = -1;
		// search towards beginning of doc first...
		foundOffset = realFindBackwards(target, start, -1,
				isCaseSensitive, isWholeWord);
		// ...then from end of doc
		if(foundOffset < 0){
			foundOffset = realFindBackwards(target, getTextLength()-1, start,
					isCaseSensitive, isWholeWord);
		}

		return foundOffset;
	}
	
	/**
	 * Searches backwards from startCharOffset (inclusive)
	 * to endCharOffset (inclusive)
	 * 
	 * @param target
	 * @param start
	 * @param end
	 * @param isCaseSensitive
	 * @param isWholeWord
	 * @return
	 */
	private int realFindBackwards(String target, int start, int end,
			boolean isCaseSensitive, boolean isWholeWord) {
		int offset = logicalToRealIndex(start);
		int searchLimit = logicalToRealIndex(end);

		while(offset > searchLimit){
			if(equals(target, offset, isCaseSensitive) &&
				(!isWholeWord || isSandwichedByWhitespace(offset, target.length()) )){
				break;
			}

			// skip behind the gap
			if(offset == _gapEndIndex){
				offset = _gapStartIndex;
			}
			--offset;
		}
		
		if (offset > searchLimit){
			return realToLogicalIndex(offset);
		}
		else{
			return -1;
		}
	}
	
	/*
	 * Replace all starting from start, wrapping around the document
	 * Returns (replacement count, new position of start because of changes in
	 * document length after replacements)
	 */
	public Pair replaceAll(String searchText, int start,
			boolean isCaseSensitive, boolean isWholeWord, String replacementText){
		int replacementCount = 0;
		int mark = start;
		
		final char[] replacement = replacementText.toCharArray();
		// replace towards end of doc first...
		int foundIndex = realFind(searchText, mark, getTextLength(),
				isCaseSensitive, isWholeWord);
		while (foundIndex != -1){
			delete(foundIndex, searchText.length());
			insert(replacement, foundIndex);
			++replacementCount;
			foundIndex = realFind(searchText, foundIndex+1, getTextLength(),
					isCaseSensitive, isWholeWord);
		}
		// ...then from beginning of doc
		foundIndex = realFind(searchText, 0, mark,
				isCaseSensitive, isWholeWord);
		while (foundIndex != -1){
			delete(foundIndex, searchText.length());
			insert(replacement, foundIndex);
			// adjust mark because of differences in doc length
			// after word replacement
			mark += replacementText.length() - searchText.length();
			++replacementCount;
			foundIndex = realFind(searchText, foundIndex+1, mark,
					isCaseSensitive, isWholeWord);
		}

		return new Pair(replacementCount, mark - start);
	}
	
	/**
	 * startingPosition represents an index of _contents[], not a charOffset!
	 * 
	 * @param target
	 * @param startingPosition
	 * @param isCaseSensitive
	 * @return
	 */
	protected boolean equals(String target, int startingPosition, boolean isCaseSensitive){
		for(int i = 0, j = startingPosition;
			i < target.length() && j < _contents.length;
			++i){
			if (isCaseSensitive &&
					target.charAt(i) != _contents[j]){
				return false;
			}
			// for case-insensitive search, compare both strings in lower case
			if (!isCaseSensitive &&
					Character.toLowerCase(target.charAt(i)) != 
					Character.toLowerCase(_contents[j])){
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
	 * is bounded by whitespace.
	 * 
	 * startingPosition represents an index of _contents[], not a charOffset!
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
		
		Language charSet = TokenScanner.getLanguage();
		boolean startWithWhitespace = false;
		if (startPosition == 0){
			startWithWhitespace = true;
		}
		else{
			char leftLimit = _contents[startPosition - 1];
			startWithWhitespace = charSet.isWhitespace(leftLimit);
		}
		
		boolean endWithWhitespace = false;
		if (endPosition == _contents.length){
			endWithWhitespace = true;
		}
		else{
			char rightLimit = _contents[endPosition];
			endWithWhitespace = charSet.isWhitespace(rightLimit);
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
		int pos = getCharOffset(targetLine);
		
		if (pos != -1){
			pos = logicalToRealIndex(pos);
			//TODO consider adding check for (pos < _contents.length)
			// in case EOF is not properly set
			while(_contents[pos] != Language.NEWLINE &&
			 _contents[pos] != Language.EOF){
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
	 * @param charOffset
	 * @return The char at charOffset. If charOffset is invalid,
	 * the result is undefined.
	 */
	public char getChar(int charOffset){
		return _contents[logicalToRealIndex(charOffset)];
	}
	

	/**
	 * Gets up to maxChars number of chars starting at logicalIndex
	 * 
	 * @param charOffset
	 * @return The chars starting from charOffset, up to a maximum
	 * of maxChars, or an empty array if charOffset is invalid
	 */
	public char[] getChars(int charOffset, int maxChars){
		if(!isValid(charOffset)){
			return new char[0];
		}
		int realIndex = logicalToRealIndex(charOffset);
		char[] chars = new char[maxChars];
		
		for (int i = 0; i < maxChars; ++i){
			if (realIndex < _contents.length){
				chars[i] = _contents[realIndex];
				++realIndex;
				// skip the gap
				if(realIndex == _gapStartIndex){
					realIndex = _gapEndIndex;
				}
			}
			else{
				chars[i] = Language.NULL_CHAR;
			}
		}
		
		return chars;
	}

	/**
	 * Insert all characters in c into position charOffset.
	 * If charOffset is invalid, nothing happens.
	 * 
	 * @param c
	 * @param charOffset
	 */
	public void insert(char[] c, int charOffset){
		if(!isValid(charOffset)){
			return;
		}
		
		int insertIndex = logicalToRealIndex(charOffset);
		
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

			_contents[_gapStartIndex] = c[i];
			++_gapStartIndex;
		}
		
		_cache.invalidateCache(charOffset);
	}

	/**
	 * Deletes up to maxChars number of char starting from position charOffset, inclusive.
	 * If charOffset is invalid, nothing happens.
	 * 
	 * @param c
	 * @param charOffset
	 */
	public void delete(int charOffset, int maxChars){
		if(!isValid(charOffset)){
			return;
		}
		
		int deleteIndex = logicalToRealIndex(charOffset);
		
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

		_cache.invalidateCache(charOffset);
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
		_contents[toPosition--] = Language.EOF; // mark end of file
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
		char[] temp = new char[newSize];
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
	}
	
	/**
	 * 
	 * @return Number of char in text document,
	 * including the EOF sentinel char, but excluding the gap
	 */
	final public int getTextLength(){
		return _contents.length - gapSize();
	}
	
	final public boolean isValid(int logicalIndex){
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
}
