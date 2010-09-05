/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.file.*;
import javax.microedition.io.Connector;
import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.component.Dialog;

import com.myopicmobile.textwarrior.common.CharacterEncodingsC;
import com.myopicmobile.textwarrior.common.CharacterEncodingsCpp;
import com.myopicmobile.textwarrior.common.CharacterEncodingsEmpty;
import com.myopicmobile.textwarrior.common.CharacterEncodingsJava;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.TextBuffer;
import com.myopicmobile.textwarrior.common.TextWarriorException;
import com.myopicmobile.textwarrior.common.TextWarriorOptions;
import com.myopicmobile.textwarrior.common.TokenScanner;

public class TextWarriorController implements TextWarriorStringsResource{
	private TextWarriorMainScreen _mainScreen = null;
	private String _filename;
	private TextBuffer _theDoc;
	private DocumentProvider _theDocHandle; //TODO should not be a singleton member var.
	// in future, multiple text fields should be able to
	// refer to the same text through different DocumentProviders
	private TextWarriorOptions _options;
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	
	//TODO 1GB; should take gap size into account as well
	private static final long MAX_FILE_SIZE = 1073741824;
	
	public TextWarriorController(){
		_filename = null;
		loadOptions();
		setModel(new TextBuffer());
		// _options must be created before creating TextWarriorMainScreen,
		// which in turn creates TextWarriorOptionsScreen
		setView(new TextWarriorMainScreen(this));
		// initOptions() must be called after TextWarriorMainScreen is created
		// so that it can set the options
		initOptions();
	}
	
	private void loadOptions(){
		_options = new TextWarriorOptions();
		try{
			_options.loadFromPersistentStorage();
		}
		catch (TextWarriorException ex){
			 TextWarriorException.assert(false,
					 "Error reading from options file");
		}
	}
	
	private void initOptions(){
		setLanguage(_options.get_syntax());
		setTabSpaces(_options.get_tabSpaces());
	}
	
	private void setView(TextWarriorMainScreen s){
		_mainScreen = s;
	}

	private void setModel(TextBuffer buf){
		_theDoc = buf;
		_theDocHandle = new DocumentProvider(buf);
	}
	
	public TextWarriorMainScreen getMainScreen(){
		return _mainScreen;
	}
	
	public void newText(){
		_filename = null;
		setModel(new TextBuffer());
        _mainScreen.resetEditor();
        _mainScreen.setTitle(_appStrings.getString(FIELD_TITLE));
	}
	
	public void open(String filename){
		try {
            FileConnection textFile = (FileConnection) Connector.open(filename, Connector.READ); 
            
            long fileSize = textFile.fileSize();
            
            if (fileSize > MAX_FILE_SIZE){
            	_mainScreen.showErrorDialog(_appStrings.getString(DIALOG_LARGE_FILE_OPEN_WARNING));
            }
            else {
                InputStream textInputStream = textFile.openInputStream();
                TextBuffer text = new TextBuffer();
                text.read(textInputStream, (int) fileSize);
                textInputStream.close();
                setModel(text);

            	_filename = filename;
                String shortFileName = _filename.substring(_filename.lastIndexOf(FileSystemBrowser.FILE_SEPARATOR) + 1); 
                _mainScreen.setTitle(shortFileName);
                _mainScreen.resetEditor();
            }
            textFile.close();
        }
		catch (IOException ex) { 
			_mainScreen.showErrorDialog(_appStrings.getString(DIALOG_FILE_OPEN_ERROR) + ex.getMessage() +
					". " + _appStrings.getString(DIALOG_FILE_READ_DENIED));
        } 
		catch (TextWarriorException ex) { 
			_mainScreen.showErrorDialog(_appStrings.getString(DIALOG_FILE_OPEN_ERROR) + ex.getMessage());
        }
	}
	
	/**
	 * Saves current text into file with filename.
	 * Creates a new file if the file with filename does not exist.
	 * Silently overwrites existing files if overwrite is true;
	 * warns user if overwrite is false.
	 * 
	 * @param filename
	 */
	public boolean save(String filename, boolean overwriteSilently){
		try { 
	        FileConnection textFile = (FileConnection) Connector.open(filename); 
            if (textFile.exists()) {
            	if(!overwriteSilently){
            		int confirmOverwrite = _mainScreen.ask(Dialog.D_YES_NO, 
            				_appStrings.getString(DIALOG_CONFIRM_SAVE_OVERWRITE), Dialog.NO);
            		if (confirmOverwrite == Dialog.NO){
            			return false;
            		}
            	}
                textFile.delete();
            } 
            textFile.create();
            
            OutputStream textOutputStream = textFile.openOutputStream();
            _theDoc.write(textOutputStream);
            textOutputStream.close();
            textFile.close();

            _filename = filename;
            String shortFileName = _filename.substring(_filename.lastIndexOf(FileSystemBrowser.FILE_SEPARATOR) + 1); 
            _mainScreen.setTitle(shortFileName);
            return true;
	    }
		catch (IOException ex) { 
			_mainScreen.showErrorDialog(_appStrings.getString(DIALOG_FILE_SAVE_ERROR) + ex.getMessage() +
					". " + _appStrings.getString(DIALOG_FILE_WRITE_DENIED));
	    }
		catch (TextWarriorException ex) { 
			_mainScreen.showErrorDialog(_appStrings.getString(DIALOG_FILE_SAVE_ERROR) + ex.getMessage());
	    } 
		return false;
	}
	
	/**
	 * Saves current text into existing _filename.
	 * If _filename is null, prompt user for a filename
	 * 
	 */
	public boolean save(){
		if(_filename == null){
			return _mainScreen.onSaveAs();
		}
		
		return save(_filename, true);
	}
	
	public boolean find(String searchText, boolean isCaseSensitive, boolean isMatchWord){
		int startingPosition = _mainScreen.getCursorPosition() + 1;
		int foundIndex = _theDoc.find(searchText, startingPosition, isCaseSensitive, isMatchWord);
		
		if (foundIndex != -1){
			_mainScreen.setTextSelection(foundIndex, searchText.length());
			_mainScreen.makeSelectionVisible();
			return true;
		}
		return false;
	}
	
	public boolean findBackwards(String searchText, boolean isCaseSensitive, boolean isMatchWord){
		int startingPosition;
		if (_mainScreen.isTextSelected()){
			startingPosition = _mainScreen.getSelectionAnchorPosition() - 1;
		}
		else{
			startingPosition = _mainScreen.getCursorPosition() - 1;
		}
		
		int foundIndex = _theDoc.findBackwards(searchText, startingPosition, isCaseSensitive, isMatchWord);
		
		if (foundIndex != -1){
			_mainScreen.setTextSelection(foundIndex, searchText.length());
			_mainScreen.makeSelectionVisible();
			return true;
		}
		return false;
	}
	
	public void replace(String string){
		if (_mainScreen.isTextSelected()){
			_mainScreen.replace(string);
			_mainScreen.makeSelectionVisible();
		}
	}

	// implementing in terms of multiple calls to find(...) and replace(...)
	// is too slow because of multiple updating of text selection in GUI
	public void replaceAll(String searchText, boolean isCaseSensitive,
			boolean isMatchWord, String replacementText){
		_mainScreen.selectTextMode(false);
		
		int startingPosition = _mainScreen.getCursorPosition() + 1;
		int foundIndex = _theDoc.find(searchText, startingPosition, isCaseSensitive, isMatchWord);
		int replacementCount = 0;
		while (foundIndex != -1){
			++replacementCount;
			_theDoc.delete(foundIndex, searchText.length());
			_theDoc.insert(replacementText.toCharArray(), foundIndex);
			startingPosition = foundIndex + replacementText.length() + 1;
			foundIndex = _theDoc.find(searchText, startingPosition,
				isCaseSensitive, isMatchWord);
		}

		if (replacementCount > 0){
			_mainScreen.setCursorPosition(startingPosition - 1);
			_mainScreen.setDirty(true);
			_mainScreen.forceEditorRepaint();
		}
		
		String message;
		if (replacementCount == 1){ 
			message = replacementCount +
				_appStrings.getString(FIND_PANEL_REPLACE_ALL_SINGULAR_RESULT);
		}
		else{
			message = replacementCount +
				_appStrings.getString(FIND_PANEL_REPLACE_ALL_MULTIPLE_RESULTS);
		}
		_mainScreen.showInfoDialog(message);
	}
	
	public DocumentProvider getDocumentProvider(){
		return _theDocHandle;
	}
	
	public TextWarriorOptions getOptions(){
		return _options;
	}
	
	public void setLanguage(byte lang){
		_options.set_syntax(lang);
		switch(lang){
		case TextWarriorOptions.SYNTAX_C:
			TokenScanner.setLanguage(CharacterEncodingsC.getCharacterEncodings());
			break;
			
		case TextWarriorOptions.SYNTAX_CPP:
			TokenScanner.setLanguage(CharacterEncodingsCpp.getCharacterEncodings());
			break;
			
		case TextWarriorOptions.SYNTAX_JAVA:
			TokenScanner.setLanguage(CharacterEncodingsJava.getCharacterEncodings());
			break;
			
		case TextWarriorOptions.SYNTAX_NONE: //fall-through
		default:
			TokenScanner.setLanguage(CharacterEncodingsEmpty.getCharacterEncodings());
			break;
		}

		_mainScreen.forceEditorRepaint();
	}
	
	public void setTabSpaces(byte spaceCount){
		_options.set_tabSpaces(spaceCount);
		
		//TODO don't store tabLength in FreeScrollingTextField; request it from _options
		_mainScreen.setTabSpaces(spaceCount);
		_mainScreen.forceEditorRepaint();
	}
	
	public void saveOptions(){
		try{
			_options.saveToPersistentStorage();
		}
		catch (TextWarriorException ex){
			 TextWarriorException.assert(false,
					 "Error saving to options file");
		}
	}
}
