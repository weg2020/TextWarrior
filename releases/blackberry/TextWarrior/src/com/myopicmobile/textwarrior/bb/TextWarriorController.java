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

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.EventInjector;
import net.rim.device.api.system.GlobalEventListener;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

import com.myopicmobile.textwarrior.common.LanguageC;
import com.myopicmobile.textwarrior.common.LanguageCpp;
import com.myopicmobile.textwarrior.common.LanguageNonProg;
import com.myopicmobile.textwarrior.common.LanguageJava;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Pair;
import com.myopicmobile.textwarrior.common.RowListener;
import com.myopicmobile.textwarrior.common.TextBuffer;
import com.myopicmobile.textwarrior.common.TextWarriorException;
import com.myopicmobile.textwarrior.common.TextWarriorOptions;
import com.myopicmobile.textwarrior.common.TokenScanner;

public class TextWarriorController
implements TextWarriorStringsResource, RowListener, GlobalEventListener{
	private TextWarriorMainScreen _mainScreen = null;
	private String _filename = null;
	private TextBuffer _theDoc;
	private TextWarriorOptions _options;

	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	// If you want to change this value, make sure to take into account the
	// MIN_GAP_SIZE of TextBuffer as well
	private static final int MAX_FILE_SIZE = 1073741824; //1GB
	
	public TextWarriorController(){
		// Order of init:
		// _options -> TextBuffer -> TextWarriorMainScreen -> TextWarriorOptionsScreen
		loadOptions();
		setModel(new TextBuffer());
		setView(new TextWarriorMainScreen(this));
		restoreOptions();
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
	
	private void restoreOptions(){
		setLanguage(_options.getSyntax());
		setTabSpaces(_options.getTabSpaces());
		setZoomSize(_options.getZoomSize());
	}
	
	private void setView(TextWarriorMainScreen s){
		_mainScreen = s;
        _mainScreen.setTitle(createTitle());
	}
	
	private void setModel(TextBuffer buf){
		_theDoc = buf;
	}

	/**
	 * _mainScreen must be initialized.
	 * Before that, use setModel(TextBuffer) instead. The difference is that
	 * this method notifies _mainScreen about the change in the text file loaded
	 * but setModel doesn't
	 * 
	 * @param buf
	 */
	private void changeModel(TextBuffer buf){
		setModel(buf);
        _mainScreen.onDocumentLoad(new DocumentProvider(_theDoc));
	}

	private void setFilename(String path) {
		_filename = path;
        _mainScreen.setTitle(createTitle());
	}

	public void onRowChange(int newRowIndex) {
		if (_options.isShowLineNumbers()){
			_mainScreen.setTitle(createTitle(newRowIndex));
		}
	}
	
	private String createTitle(){
		return createTitle(_mainScreen.getCaretRow());
	}
	
	private String createTitle(int rowIndex){
		String title = "";
		if (_options.isShowLineNumbers()){
			// covert to one-indexed row. see also goToLine(int)
			title = "(" + (rowIndex+1) + ") ";
		}

        if(_filename != null){
        	title += _filename.substring(
            		_filename.lastIndexOf(FileSystemBrowser.FILE_SEPARATOR) + 1);
        }
        else{
        	title += _appStrings.getString(FIELD_TITLE);
        }
        return title;
	}
	
	public TextWarriorMainScreen getMainScreen(){
		return _mainScreen;
	}
	
	public DocumentProvider getDocumentProvider(){
		return new DocumentProvider(_theDoc);
	}
	
	public TextWarriorOptions getOptions(){
		return _options;
	}
	
	public void newText(){
		changeModel(new TextBuffer());
		setFilename(null);
        _mainScreen.forceEditorRepaint();
	}
	
	public void open(String filename){
		try {
            FileConnection textFile = (FileConnection) Connector.open(filename, Connector.READ); 

            if (textFile.fileSize() > MAX_FILE_SIZE){
            	_mainScreen.showErrorDialog(_appStrings.getString(DIALOG_LARGE_FILE_OPEN_WARNING));
            }
            else {
                TextBuffer buf = new TextBuffer();
            	ReadThread readThread = new ReadThread(textFile, buf,
            			_options.getFileInputScheme(), _options.getLineBreakFormat());
        		ProgressDialog progress = new ProgressDialog(readThread, true, true);
        		readThread.registerObserver(progress);
        		readThread.start();
        		progress.doModal();
        		
        		if (readThread.isDone()){
    	            changeModel(buf);
    	            setFilename(filename);
        		}
            }
            textFile.close();
        }
		catch (IOException ex) {
			_mainScreen.showErrorDialog(_appStrings.getString(DIALOG_FILE_OPEN_ERROR)
					+ ex.getMessage()
					+ ". " + _appStrings.getString(DIALOG_FILE_READ_DENIED));
        }
        _mainScreen.forceEditorRepaint();
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
		boolean success = false;
		try {
	        FileConnection originalFile = (FileConnection) Connector.open(filename); 
            if (!askOverwrite(originalFile, overwriteSilently)){
            	return false;
            }
            //TODO unlikely, but check if another file exists with same name as tmpFile
	        FileConnection tmpFile = (FileConnection) Connector.open(filename + ".twrtmp"); 
            tmpFile.create();
            
        	WriteThread writeThread = new WriteThread(tmpFile, _theDoc,
        			_options.getFileOutputScheme(), _options.getLineBreakFormat());
    		ProgressDialog progress = new ProgressDialog(writeThread, true, true);
    		writeThread.registerObserver(progress);
    		writeThread.start();
    		progress.doModal();
    		
    		if (writeThread.isDone()){
    			if (originalFile.exists()){
    				originalFile.delete();
    			}
                String shortFileName =
                	filename.substring(filename.lastIndexOf(FileSystemBrowser.FILE_SEPARATOR) + 1);
    			tmpFile.rename(shortFileName);
	            setFilename(filename);
    		}
    		else{
    			// user cancelled operation or IO error occurred in WriteThread
    			tmpFile.delete();
    		}

            originalFile.close();
    		tmpFile.close();
    		success = writeThread.isDone();
	    }
		catch (IOException ex) { 
			_mainScreen.showErrorDialog(_appStrings.getString(DIALOG_FILE_SAVE_ERROR)
					+ ex.getMessage()
					+ ". " + _appStrings.getString(DIALOG_FILE_WRITE_DENIED));
	    }

        _mainScreen.forceEditorRepaint();
		return success;
	}

	/**
	 * 
	 * @param textFile
	 * @param overwriteSilently
	 * @return False if there exists a file with the same name and user opts to
	 * 		cancel operation; true otherwise
	 * @throws IOException
	 */
	private boolean askOverwrite(FileConnection textFile,
			boolean overwriteSilently) {
		if (textFile.exists() && !overwriteSilently){
			int confirmOverwrite = _mainScreen.ask(Dialog.D_YES_NO, 
					_appStrings.getString(DIALOG_CONFIRM_SAVE_OVERWRITE),
					Dialog.NO);
			if (confirmOverwrite == Dialog.NO){
				return false;
			}
		}
		return true;
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
	//TODO put in FindThread
	public boolean find(String searchText, boolean isCaseSensitive, boolean isMatchWord){
		int startingPosition = _mainScreen.getCaretPosition() + 1;
		int foundIndex = _theDoc.find(searchText, startingPosition,
				isCaseSensitive, isMatchWord);
		
		if (foundIndex != -1){
			_mainScreen.setTextSelection(foundIndex, searchText.length());
			_mainScreen.makeSelectionVisible();
			return true;
		}
		return false;
	}

	//TODO put in FindThread
	public boolean findBackwards(String searchText, boolean isCaseSensitive, boolean isMatchWord){
		int startingPosition;
		if (_mainScreen.isTextSelected()){
			startingPosition = _mainScreen.getSelectionAnchorPosition() - 1;
		}
		else{
			startingPosition = _mainScreen.getCaretPosition() - 1;
		}
		
		int foundIndex = _theDoc.findBackwards(searchText, startingPosition, isCaseSensitive, isMatchWord);
		
		if (foundIndex != -1){
			_mainScreen.setTextSelection(foundIndex, searchText.length());
			_mainScreen.makeSelectionVisible();
			return true;
		}
		return false;
	}

	public void replace(String string, boolean triggerRedraw){
		if (_mainScreen.isTextSelected()){
			_mainScreen.replace(string);
			if(triggerRedraw){
				_mainScreen.makeSelectionVisible();
			}
		}
	}
	
	public void insertIntoEditor(String string){
		_mainScreen.replace(string);
		_mainScreen.forceEditorRepaint();
	}

	public void replaceAll(String searchText, boolean isCaseSensitive,
			boolean isMatchWord, String replacementText){
		FindThread findThread = new FindThread(_theDoc,
			_mainScreen.getCaretPosition() + 1,
    		searchText, replacementText,
			isCaseSensitive, isMatchWord);
		ProgressDialog progress = new ProgressDialog(findThread, false, false);
		findThread.registerObserver(progress);
		findThread.start();
		progress.doModal();

		if (findThread.getReplacementCount() > 0){
			int caretChange = findThread.getStartOffsetChange();
			if (caretChange != 0){
				_mainScreen.setCaretPosition(
					Math.max(0, _mainScreen.getCaretPosition() + caretChange));
			}

			_mainScreen.setDirty(true);
			_mainScreen.selectText(false);
			_mainScreen.forceEditorRepaint();
		}
		String message = _appStrings.getString(FIND_PANEL_REPLACE_ALL_RESULT)
			+ findThread.getReplacementCount();
		_mainScreen.showInfoDialog(message);
	}

	public void eventOccurred(long guid, int data0, int data1,
			Object object0, Object object1){
		if( guid == Font.GUID_FONT_CHANGED){
			// if system font changes, update editor with new (size * zoom factor)
	        UiApplication.getUiApplication().invokeLater(new Runnable() { 
	            public void run() {
	    			onZoomSizeChanged(_options.getZoomSize());
	            }
	        });	
		}
	}
	
	public void setInputLocale() {
		// inject Alt+Enter, which is the BlackBerry shortcut to select input methods
		EventInjector.KeyCodeEvent enterDown =
			new EventInjector.KeyCodeEvent(EventInjector.KeyCodeEvent.KEY_DOWN,
					(char) Keypad.KEY_ENTER,
					KeypadListener.STATUS_ALT);
		EventInjector.KeyCodeEvent enterUp =
			new EventInjector.KeyCodeEvent(EventInjector.KeyCodeEvent.KEY_UP,
					(char) Keypad.KEY_ENTER,
					KeypadListener.STATUS_ALT);
		enterDown.post();
		enterUp.post();
	}

	public void onLanguageChanged(String lang){
		setLanguage(lang);
		_mainScreen.forceEditorRepaint();
	}
	
	public void setLanguage(String lang){
		_options.setSyntax(lang);
		if (lang.equals(TextWarriorOptions.SYNTAX_FIELD_C)){
			TokenScanner.setLanguage(LanguageC.getCharacterEncodings());
		}
		else if (lang.equals(TextWarriorOptions.SYNTAX_FIELD_CPP)){
			TokenScanner.setLanguage(LanguageCpp.getCharacterEncodings());
		}
		else if (lang.equals(TextWarriorOptions.SYNTAX_FIELD_JAVA)){
			TokenScanner.setLanguage(LanguageJava.getCharacterEncodings());
		}
		else{
			TextWarriorException.assert(lang.equals(TextWarriorOptions.SYNTAX_FIELD_NONE),
			 "Warning: Unsupported language set for syntax highlighting");
			TokenScanner.setLanguage(LanguageNonProg.getCharacterEncodings());
		}
	}
	
	public void onTabSpacesChanged(byte spaceCount){
		setTabSpaces(spaceCount);
		_mainScreen.forceEditorRepaint();
	}

	public void setTabSpaces(byte spaceCount){
		_options.setTabSpaces(spaceCount);
		_mainScreen.setTabSpaces(spaceCount);
	}

	public void onZoomSizeChanged(int zoomSize) {
		setZoomSize(zoomSize);
		_mainScreen.forceEditorRepaint();
	}

	public void setZoomSize(int zoomSize){
		_options.setZoomSize(zoomSize);
		_mainScreen.setZoomSize(zoomSize);
	}
	
	public void showLineNumbers(boolean set){
		_options.setShowLineNumbers(set);
        _mainScreen.setTitle(createTitle());
	}

	public void onFileInputSchemeChanged(String choice) {
		_options.setFileInputScheme(choice);
	}

	public void onFileOutputSchemeChanged(String choice) {
		_options.setFileOutputScheme(choice);
	}

	public void onLineBreakFormatChanged(String choice) {
		_options.setLineBreakFormat(choice);
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

	public void goToLine(int row) {
		// covert to zero-indexed row; see also createTitle(int)
		_mainScreen.setCaretRow(row-1);
		_mainScreen.makeSelectionVisible();
	}

	public void selectAll() {
    	_mainScreen.selectAllText();
		_mainScreen.makeSelectionVisible();
	}
	
}
