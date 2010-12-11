/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

import javax.microedition.rms.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.myopicmobile.textwarrior.common.TextWarriorException;

public class TextWarriorOptions { 
	public static final String SYNTAX_FIELD_NONE = "None";
	public static final String SYNTAX_FIELD_C = "C";
	public static final String SYNTAX_FIELD_CPP = "C++";
	public static final String SYNTAX_FIELD_JAVA = "Java";

	public static final String TEXT_ENCODING_AUTO = "Auto";
	public static final String TEXT_ENCODING_ASCII = "ASCII";
	public static final String TEXT_ENCODING_UTF8 = "UTF-8";
	public static final String TEXT_ENCODING_UTF16BE = "UTF-16BE";
	public static final String TEXT_ENCODING_UTF16LE = "UTF-16LE";
	
	public static final String LINE_BREAK_AUTO = "Auto";
	public static final String LINE_BREAK_LF = "Unix";
	public static final String LINE_BREAK_CR = "MacOS9";
	public static final String LINE_BREAK_CRLF = "Windows";
	public static final String LINE_BREAK_LS = "Unicode LS"; // \u2028
	public static final String LINE_BREAK_NEL = "Unicode NEL"; // \u0085
	
	private static final String SETTINGS_FILE = "TextWarriorOptions";
	
	private String _version = "0.8";
	private String _fileInputScheme = TEXT_ENCODING_AUTO;
	private String _fileOutputScheme = TEXT_ENCODING_AUTO;
	private String _lineBreakFormat = LINE_BREAK_AUTO;
	private String _syntax = SYNTAX_FIELD_NONE;
	private byte _tabSpaces = 4; // [1, max size of byte]
	private int _zoomSize = 100; // [1, 200]
	//TODO implement options below
	private String _fontType = "";
	private boolean _showLineNumbers = false;
	private boolean _showCursorMargin = false;
	private boolean _wordWrap = false;
	private boolean _openLastFileOnStartup = false;
	private String _lastFile = "";
	
	public void loadFromPersistentStorage() throws TextWarriorException{
		try{
			RecordStore db = RecordStore.openRecordStore(SETTINGS_FILE, true);
			
			if (db.getNumRecords() > 0){
				//TODO assumes all data is stored at record id 1
				ByteArrayInputStream bais = new ByteArrayInputStream(db.getRecord(1));
				DataInputStream dis = new DataInputStream(bais);
				// maintain the same read order as the complementary saveTo... function
				_version = dis.readUTF();
				_fileInputScheme = dis.readUTF();
				_fileOutputScheme = dis.readUTF();
				_lineBreakFormat = dis.readUTF();
				_syntax = dis.readUTF();
				_tabSpaces = dis.readByte();
				_zoomSize = dis.readInt();
				_fontType = dis.readUTF();
				_showLineNumbers = dis.readBoolean();
				_showCursorMargin = dis.readBoolean();
				_wordWrap = dis.readBoolean();
				_openLastFileOnStartup = dis.readBoolean();
				_lastFile = dis.readUTF();
			}
			db.closeRecordStore();
		}
		catch(RecordStoreException rse) {
			throw new TextWarriorException(rse.getMessage());
	    }
		catch(IOException ex) {
			throw new TextWarriorException(ex.getMessage());
	    }
	}
	
	public void saveToPersistentStorage() throws TextWarriorException{
		try{
			RecordStore db = RecordStore.openRecordStore(SETTINGS_FILE, true);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			// maintain the same write order as the complementary loadTo... function
			dos.writeUTF(_version);
			dos.writeUTF(_fileInputScheme);
			dos.writeUTF(_fileOutputScheme);
			dos.writeUTF(_lineBreakFormat);
			dos.writeUTF(_syntax);
			dos.writeByte(_tabSpaces);
			dos.writeInt(_zoomSize);
			dos.writeUTF(_fontType);
			dos.writeBoolean(_showLineNumbers);
			dos.writeBoolean(_showCursorMargin);
			dos.writeBoolean(_wordWrap);
			dos.writeBoolean(_openLastFileOnStartup);
			dos.writeUTF(_lastFile);
			
			byte[] b = baos.toByteArray();
			if (db.getNumRecords() == 0){
				db.addRecord(b, 0, b.length);
			}
			else{
				//TODO assumes all data is to be stored at record id 1
				db.setRecord(1, b, 0, b.length);
			}
			db.closeRecordStore();
		}
		catch(RecordStoreException rse) {
			throw new TextWarriorException(rse.getMessage());
	    }
		catch(IOException ex) {
			throw new TextWarriorException(ex.getMessage());
	    }
	}
/*
	public String getVersion() {
		return _version;
	}

	public void setVersion(String version) {
		_version = version;
	}
*/
	public String getSyntax() {
		return _syntax;
	}

	public void setSyntax(String syntax) {
		_syntax = syntax;
	}

	public byte getTabSpaces() {
		return _tabSpaces;
	}

	public void setTabSpaces(byte tabSpaces) {
		_tabSpaces = tabSpaces;
	}

	public int getZoomSize() {
		return _zoomSize;
	}

	public void setZoomSize(int zoomSize) {
		_zoomSize = zoomSize;
	}

	public boolean isShowLineNumbers() {
		return _showLineNumbers;
	}

	public void setShowLineNumbers(boolean showLineNumbers) {
		_showLineNumbers = showLineNumbers;
	}
	
	public String getFileInputScheme() {
		return _fileInputScheme;
	}

	public void setFileInputScheme(String fileInputScheme) {
		_fileInputScheme = fileInputScheme;
	}

	public String getFileOutputScheme() {
		return _fileOutputScheme;
	}

	public void setFileOutputScheme(String fileOutputScheme) {
		_fileOutputScheme = fileOutputScheme;
	}

	public String getLineBreakFormat() {
		return _lineBreakFormat;
	}

	public void setLineBreakFormat(String lineBreakFormat) {
		_lineBreakFormat = lineBreakFormat;
	}
	
/*
	public String getFontType() {
		return _fontType;
	}

	public void setFontType(String fontType) {
		_fontType = fontType;
	}
	
	public boolean isWordWrap() {
		return _wordWrap;
	}

	public void setWordWrap(boolean wordWrap) {
		_wordWrap = wordWrap;
	}

	public boolean isOpenLastFileOnStartup() {
		return _openLastFileOnStartup;
	}

	public void setOpenLastFileOnStartup(boolean openLastFileOnStartup) {
		_openLastFileOnStartup = openLastFileOnStartup;
	}

	public String getLastFile() {
		return _lastFile;
	}

	public void setLastFile(String lastFile) {
		_lastFile = lastFile;
	}
*/
}
