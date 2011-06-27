/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/********************************************************
 * 
 * Helper class to convert between different line
 * terminator formats
 * 
 * XXtoUTF16BE methods normalise all line terminator types
 * to '\n' as a side-effect.
 * 
 * UTF16BEtoXX methods assume the line terminator type is '\n',
 * add a BOM to the output stream as a side-effect and
 * do not check the validity of the input UTF16 chars.
 * Malformed surrogate pairs and illegal Unicode values
 * will be copied over as is.
 * 
 * UTF16toUTF16BE does not check the validity of the input chars.
 * Malformed surrogate pairs and illegal Unicode values
 * will be copied over as is.
 *
 *******************************************************/
public class FileFormatConverter {
	private int _unitsDone = 0;
	
	// not synchronized; other threads calling this may get outdated values
	// but it should be all right if only an approximate value is needed 
	public int getProgress(){
		return _unitsDone;
	}
	
	public synchronized void clearProgress(){
		_unitsDone = 0;
	}
	
	/**
	 * Returns TEXT_ENCODING_UTF8 if there is no byte-order mark
	 * 
	 * @param byteStream
	 * @return
	 * @throws IOException
	 */
	public String getEncodingScheme(InputStream byteStream)
	throws IOException{
		byte[] byteOrderMark = {0, 0, 0};

		TextWarriorException.assert(byteStream.markSupported(),
		 "Error: InputStream does not support rewinding. File read will be truncated");
		
		byteStream.mark(3);
		for(int i = 0; i < 3; ++i){
			byteOrderMark[i] = (byte) byteStream.read();
		}
		byteStream.reset();
		
		if (byteOrderMark[0] == (byte) 0xFE &&
			byteOrderMark[1] == (byte) 0xFF){
			return TextWarriorOptions.TEXT_ENCODING_UTF16BE;
		}
		else if (byteOrderMark[0] == (byte) 0xFF &&
				byteOrderMark[1] == (byte) 0xFE){
			return TextWarriorOptions.TEXT_ENCODING_UTF16LE;
		}
		// Uncomment if UTF-8 should not be the default encoding
		/*
		else if (byteOrderMark[0] == (byte) 0xEF &&
				byteOrderMark[1] == (byte) 0xBB &&
				byteOrderMark[2] == (byte) 0xBF){
			return TextWarriorOptions.TEXT_ENCODING_UTF8;
		}
		*/
		else{
			return TextWarriorOptions.TEXT_ENCODING_UTF8;
		}
	}
	
	public String getEOLType(InputStream byteStream, String encoding)
	throws IOException{
		TextWarriorException.assert(byteStream.markSupported(),
		 "Error: InputStream does not support rewinding. File read will be truncated");

		String EOLType = null;
		byteStream.mark(Integer.MAX_VALUE);
		int c;
		int prev = 0;
		
		if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF16BE)){
			byteStream.read(); // discard upper byte
		}
		
		while (EOLType == null &&
		(c = byteStream.read()) != -1){
			if (c == '\n' && prev != '\r'){
				EOLType = TextWarriorOptions.LINE_BREAK_LF;
			}
			if (prev == '\r'){
				if(c == '\n'){
					EOLType = TextWarriorOptions.LINE_BREAK_CRLF;
				}
				else{
					EOLType = TextWarriorOptions.LINE_BREAK_CR;
				}
			}

			prev = c;
			if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF16BE) ||
			encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF16LE)){
				byteStream.read(); // discard upper byte
			}
		}
		
		byteStream.reset();
		
		if (EOLType == null){
			return TextWarriorOptions.LINE_BREAK_LF;
		}
		else{
			return EOLType;
		}
	}

	private void stripByteOrderMark(InputStream byteStream)
	throws IOException{
		TextWarriorException.assert(byteStream.markSupported(),
		 "Error: InputStream does not support rewinding. File read will be truncated");
		
		byteStream.mark(1);
		
		byte firstByte = (byte) byteStream.read();
		if (firstByte == (byte) 0xFE ||
		firstByte == (byte) 0xFF){
			// UTF-16
			byteStream.read(); // discard 2nd BOM byte
			_unitsDone += 2;
		}
		else if (firstByte == (byte) 0xEF){
			// UTF-8
			byteStream.read(); // discard 2nd BOM byte
			byteStream.read(); // discard 3rd BOM byte
			_unitsDone += 3;
		}
		else{
			byteStream.reset();
		}
	}
	
	public void writeByteOrderMark(OutputStream byteStream, String encoding)
	throws IOException{
		if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF16BE)){
	    	byteStream.write(0xFE);
	    	byteStream.write(0xFF);
		}
		else if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF16LE)){
	    	byteStream.write(0xFF);
	    	byteStream.write(0xFE);
		}
		else if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF8)){
	    	byteStream.write(0xEF);
	    	byteStream.write(0xBB);
	    	byteStream.write(0xBF);
		}
	}
	
	/**
	 * Reads bytes from byteStream into buffer, converting to UTF-16BE encoding
	 * and normalising all line terminators to UNIX style '\n'
	 * 
	 * @param byteStream
	 * @param buffer
	 * @param encoding Cannot be Auto! Call getEncodingScheme() to find the
	 * 			exact type first.
	 * @param lock Other threads can set this to zero
	 * 			to abort the read operation while it is in progress
	 * @return size of converted text
	 * @throws TextWarriorException
	 */
	synchronized public int readAndConvert(InputStream byteStream,
	char[] buffer, String encoding, String EOLchar, Flag abort)
	throws IOException{
		_unitsDone = 0;
		
		if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_ASCII)){
			return ASCIItoUTF16BE(byteStream, buffer, EOLchar, abort);
		}
		else if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF16BE)){
			return UTF16toUTF16BE(byteStream, buffer, true, EOLchar, abort);
		}
		else if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF16LE)){
			return UTF16toUTF16BE(byteStream, buffer, false, EOLchar, abort);
		}
		else if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF8)){
			return UTF8toUTF16BE(byteStream, buffer, EOLchar, abort);
		}
		
		return 0;
	}
	
	private int ASCIItoUTF16BE(InputStream byteStream,
	char[] buffer, String EOLchar, Flag abort)
	throws IOException{
		int currCharRead;
		int totalChar = 0;
	
		while((currCharRead = byteStream.read()) != -1 && !abort.isSet()){
			++_unitsDone;
			if (currCharRead == '\r'){
				if(EOLchar.equals(TextWarriorOptions.LINE_BREAK_CRLF)){
					//TODO assert there is a valid '/n' after this '/r'
					byteStream.read(); // discard the next char '\n'
					++_unitsDone;
				}
				currCharRead = '\n';
			}
			
			buffer[totalChar++] = (char) currCharRead;
		}

		return totalChar;
	}

	private int UTF16toUTF16BE(InputStream byteStream,
	char[] buffer, boolean isBigEndian, String EOLchar, Flag abort)
	throws IOException{
		int byte0, byte1;
		int totalChar = 0;
		char currCharRead;

		stripByteOrderMark(byteStream);
		while((byte0 = byteStream.read()) != -1
		&& (byte1 = byteStream.read()) != -1
		&& !abort.isSet()){
			_unitsDone += 2;
			//TODO place conditional outside while loop
			if(isBigEndian){
				currCharRead = (char) (byte1 | (byte0 << 8));
			}
			else{
				currCharRead = (char) (byte0 | (byte1 << 8));
			}
			
			if (currCharRead == '\r'){
				if(EOLchar.equals(TextWarriorOptions.LINE_BREAK_CRLF)){
					//TODO assert there is a valid '/n' after this '/r'
					byteStream.read(); // discard the next 2 bytes representing '\n'
					byteStream.read();
					++_unitsDone;
				}
				currCharRead = '\n';
			}
			
			buffer[totalChar++] = currCharRead;
		}

		return totalChar;		
	}
	
	synchronized public void writeAndConvert(OutputStream byteStream,
	DocumentProvider hDoc, String encoding, String EOLchar, Flag abort)
	throws IOException{
		_unitsDone = 0;
		hDoc.seekChar(0);
		
		if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_ASCII)){
			UTF16BEtoASCII(byteStream, hDoc, EOLchar, abort);
		}
		else if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF16BE)){
			UTF16BEtoUTF16(byteStream, hDoc, true, EOLchar, abort);
		}
		else if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF16LE)){
			UTF16BEtoUTF16(byteStream, hDoc, false, EOLchar, abort);
		}
		else if(encoding.equals(TextWarriorOptions.TEXT_ENCODING_UTF8)){
			UTF16BEtoUTF8(byteStream, hDoc, EOLchar, abort);
		}
	}
	
	private void UTF16BEtoASCII(OutputStream byteStream,
	DocumentProvider hDoc, String EOLchar, Flag abort)
	throws IOException{
		while(hDoc.hasNext() && !abort.isSet()){
			char curr = hDoc.next();
	    	++_unitsDone;
	    	
	    	if(curr == Language.EOF){
	    		break;
	    	}

			// convert '\n' to desired line terminator symbol
	    	if (curr == '\n' &&
	    	EOLchar.equals(TextWarriorOptions.LINE_BREAK_CRLF)){
		    	byteStream.write('\r');
	    	}
	    	else if (curr == '\n' &&
	    	EOLchar.equals(TextWarriorOptions.LINE_BREAK_CR)){
	    		curr = '\r';
	    	}
	    	byteStream.write(curr);
		}
	}
	
	private void UTF16BEtoUTF16(OutputStream byteStream,
	DocumentProvider hDoc, boolean isBigEndian, String EOLchar, Flag abort)
	throws IOException{
		String bom = isBigEndian ? TextWarriorOptions.TEXT_ENCODING_UTF16BE
				: TextWarriorOptions.TEXT_ENCODING_UTF16LE;
		writeByteOrderMark(byteStream, bom);

		while(hDoc.hasNext() && !abort.isSet()){
			char curr = hDoc.next();
	    	++_unitsDone;

	    	if(curr == Language.EOF){
	    		break;
	    	}

			// convert '\n' to desired line terminator symbol
	    	if (curr == '\n' &&
	    	EOLchar.equals(TextWarriorOptions.LINE_BREAK_CRLF)){
	    		if(isBigEndian){
		    		byteStream.write(0); byteStream.write('\r');
		    	}
	    		else{
		    		byteStream.write('\r'); byteStream.write(0);
	    		}
	    	}
	    	else if (curr == '\n' &&
	    	EOLchar.equals(TextWarriorOptions.LINE_BREAK_CR)){
	    		curr = '\r';
	    	}
			//TODO place conditional outside for loop
	    	if(isBigEndian){
	    		byteStream.write(curr >>> 8);
	    		byteStream.write(curr & 0xFF);
	    	}
	    	else{
	    		byteStream.write(curr & 0xFF);
	    		byteStream.write(curr >>> 8);
	    	}
		}
	}
	
	
	/*******
	 * The following UTF encoding form conversion methods were modified from
	 * an algorithm by Richard Gillam, pp. 543, Unicode Demystified, 2003
	 * 
	 */
	
	// Lookup table to keep track of how many more bytes left to process to get a
	// character. First index is the number of bytes of a UTF-8 character that
	// has been processed. Second index is the top 5 bits of the current byte.
	// Result of the lookup is the number of bytes left to process, or
	// -1 == illegal lead byte; -2 == illegal trailing byte
	private static final byte[][] states = {
		// 00 08 10 18 20 28 30 38 40 48 50 58 60 68 70 78
		// 80 88 90 98 A0 A8 B0 B8 C0 C8 D0 D8 E0 E8 F0 F8
		{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		-1, -1, -1, -1, -1, -1, -1, -1, 1, 1, 1, 1, 2, 2, 3, -1},
		{-2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
		 0, 0, 0, 0, 0, 0, 0, 0, -2, -2, -2, -2, -2, -2, -2, -2},
		{-2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
		 1, 1, 1, 1, 1, 1, 1, 1, -2, -2, -2, -2, -2, -2, -2, -2},
		{-2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
		 2, 2, 2, 2, 2, 2, 2, 2, -2, -2, -2, -2, -2, -2, -2, -2},
		};
	
	private static final byte[] masks = { 0x7F, 0x1F, 0x0F, 0x07 };

	
	private int UTF8toUTF16BE(InputStream byteStream,
	char[] buffer, String EOLchar, Flag abort)
	throws IOException{
		int currByte;
		int utf32Char = 0;
		int totalChar = 0;
		int state = 0;
		byte mask = 0;

		stripByteOrderMark(byteStream);
		while((currByte = byteStream.read()) != -1 && !abort.isSet()){
			++_unitsDone;
			state = states[state][currByte >>> 3];
			
			switch(state){
			case 0:
				utf32Char += currByte & 0x7F;
				
				if (utf32Char == '\r'){
					if(EOLchar.equals(TextWarriorOptions.LINE_BREAK_CRLF)){
						//TODO assert there is a valid '/n' after this '/r'
						byteStream.read(); // discard the next char '\n'
						++_unitsDone;
					}
					utf32Char = '\n';
				}
				
				if(utf32Char <= 0xFFFF){
					buffer[totalChar++] = (char) utf32Char;
				}
				else{
					// not in the BMP; split into surrogate pair
					buffer[totalChar++] = (char) ((utf32Char >> 10) + 0xD7C0);
					buffer[totalChar++] = (char) ((utf32Char & 0x03FF) + 0xDC00);
				}
				utf32Char = 0;
				mask = 0;
				break;
				
			case 1: // fall-through
			case 2: // fall-through
			case 3:
				if (mask == 0){
					mask = masks[state];
				}
				utf32Char += currByte & mask;
				utf32Char <<= 6;
				mask = (byte) 0x3F;
				break;
				
			case -2: //fall-through
				//TODO keep this byte and use it for the next while iteration
			case -1:
				//TODO replace malformed sequence with the Unicode replacement char 0xFFFD
				// Since FFFD in UTF-16 requires a surrogate pair,
				// and TextWarrior cannot handle suurogate pairs yet, use '?' instead
				/*
				buffer[totalChar++] = 0xDBBF;
				buffer[totalChar++] = 0xDFFD;
				*/
				buffer[totalChar++] = '?';
				state = 0;
				utf32Char = 0;
				mask = 0;
				break;
			}
		}

		return totalChar;
	}
	
	private void UTF16BEtoUTF8(OutputStream byteStream,
	DocumentProvider hDoc, String EOLchar, Flag abort)
	throws IOException{
		writeByteOrderMark(byteStream, TextWarriorOptions.TEXT_ENCODING_UTF8);
		while(hDoc.hasNext() && !abort.isSet()){
			int utf32Char = 0;
			char curr = hDoc.next();
	    	++_unitsDone;

	    	if(curr == Language.EOF){
	    		break;
	    	}
	    	
			if(curr < 0xD800 || curr > 0xDFFF){
				utf32Char = curr;
			}
			else{
				// combine surrogate pair to UTF-32 value
				utf32Char = (curr-0xD7C0) << 10;
				utf32Char += hDoc.next() & 0x03FF;
			}
	    	
			// Encode variable number of UTF-8 bytes depending on the UTF-32 value
		    if (utf32Char < 0x80){
		    	// convert '\n' to desired line terminator symbol
		    	if (utf32Char == '\n' &&
		    	EOLchar.equals(TextWarriorOptions.LINE_BREAK_CRLF)){
					byteStream.write('\r');
		    	}
		    	else if (utf32Char == '\n' &&
		    	EOLchar.equals(TextWarriorOptions.LINE_BREAK_CR)){
		    		utf32Char = '\r';
		    	}
		    	byteStream.write(utf32Char);
		    }
		    else if (utf32Char < 0x800){
		    	byteStream.write((utf32Char >> 6) + 0xC0);
		    	byteStream.write((utf32Char & 0x3F) + 0x80);
		    }
		    else if (utf32Char < 0x10000){
		    	byteStream.write((utf32Char >> 12) + 0xE0);
		    	byteStream.write(((utf32Char >> 6) & 0x3F) + 0x80);
		    	byteStream.write((utf32Char & 0x3F) + 0x80);
		    }
		    else{
		    	byteStream.write((utf32Char >> 18) + 0xF0);
		    	byteStream.write(((utf32Char >> 12) & 0x3F) + 0x80);
		    	byteStream.write(((utf32Char >> 6) & 0x3F) + 0x80);
		    	byteStream.write((utf32Char & 0x3F) + 0x80);
		    }
		}
	}
	
}