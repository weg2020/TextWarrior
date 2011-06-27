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
	public static final byte SYNTAX_NONE = 0;
	public static final byte SYNTAX_C = 1;
	public static final byte SYNTAX_CPP = 2;
	public static final byte SYNTAX_JAVA = 3;
	
	private static final String SETTINGS_FILE = "TextWarriorOptions";
	
	private String _version = "";
	private byte _syntax = SYNTAX_NONE; // none, c, cpp or java
	private byte _tabSpaces = 4; // [1, max size of byte]
	private int _zoomSize = 100; // [1, 200]
	private String _fontType = "";
	private boolean _showStatusBar = false;
	private boolean _showLineNumbers = false;
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
				_syntax = dis.readByte();
				_tabSpaces = dis.readByte();
				_zoomSize = dis.readInt();
				_fontType = dis.readUTF();
				_showStatusBar = dis.readBoolean();
				_showLineNumbers = dis.readBoolean();
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
			dos.writeByte(_syntax);
			dos.writeByte(_tabSpaces);
			dos.writeInt(_zoomSize);
			dos.writeUTF(_fontType);
			dos.writeBoolean(_showStatusBar);
			dos.writeBoolean(_showLineNumbers);
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

	public String get_version() {
		return _version;
	}

	public void set_version(String version) {
		_version = version;
	}

	public byte get_syntax() {
		return _syntax;
	}

	public void set_syntax(byte syntax) {
		_syntax = syntax;
	}

	public byte get_tabSpaces() {
		return _tabSpaces;
	}

	public void set_tabSpaces(byte tabSpaces) {
		_tabSpaces = tabSpaces;
	}

	public int get_zoomSize() {
		return _zoomSize;
	}

	public void set_zoomSize(int zoomSize) {
		_zoomSize = zoomSize;
	}

	public String get_fontType() {
		return _fontType;
	}

	public void set_fontType(String fontType) {
		_fontType = fontType;
	}

	public boolean is_showStatusBar() {
		return _showStatusBar;
	}

	public void set_showStatusBar(boolean showStatusBar) {
		_showStatusBar = showStatusBar;
	}

	public boolean is_showLineNumbers() {
		return _showLineNumbers;
	}

	public void set_showLineNumbers(boolean showLineNumbers) {
		_showLineNumbers = showLineNumbers;
	}

	public boolean is_wordWrap() {
		return _wordWrap;
	}

	public void set_wordWrap(boolean wordWrap) {
		_wordWrap = wordWrap;
	}

	public boolean is_openLastFileOnStartup() {
		return _openLastFileOnStartup;
	}

	public void set_openLastFileOnStartup(boolean openLastFileOnStartup) {
		_openLastFileOnStartup = openLastFileOnStartup;
	}

	public String get_lastFile() {
		return _lastFile;
	}

	public void set_lastFile(String lastFile) {
		_lastFile = lastFile;
	}
}
