/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.ContextMenu;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ObjectListField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.TextField;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.system.Characters;
import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.util.SimpleSortingVector;
import net.rim.device.api.util.StringComparator;
import java.util.Enumeration;
import java.io.IOException;
import javax.microedition.io.file.*;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileSystemRegistry;

import com.myopicmobile.textwarrior.common.TextWarriorException;
 
/**
 * This browser assumes "file:///" to have directory depth 0,
 * although it cannot be opened in BlackBerry systems.
 * 
 * The real root directories like "file:///SDCard/" have directory depth 1
 *
 */
public class FileSystemBrowser extends MainScreen implements TextWarriorStringsResource{ 
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME); 
    private ObjectListField _fileList;
    private VerticalFieldManager _fileListContainer;
    private BasicEditField _filenameInputField;
    private LabelField _selectDirLabel;
    private LabelField _currentDirLabel; 
    private String _currentDir; 
    private String _currentFile;
    private int _dirDepth;
    private int _selectMode;
    
    public static char FILE_SEPARATOR = '/';
    private static String PATH_PREFIX = "file:///";
    private static String GO_UP_DIR = _appStrings.getString(FILE_SYSTEM_BROWSER_GO_UP);
    private final static int SELECT_FILE_MODE = 0;
    private final static int SPECIFY_FILENAME_MODE = 1;
 
    public FileSystemBrowser(){
    	super(NO_HORIZONTAL_SCROLL | NO_VERTICAL_SCROLL);

        createFileBrowserField();
        createLabels();
        createFilenameInputField();
        
    	// default mode is file selection mode
    	setTitle(_appStrings.getString(FILE_SYSTEM_BROWSER_TITLE_SELECT_FILE));
        _selectMode = SELECT_FILE_MODE;
        addSelectFileFields();
    }
 
    protected void createFileBrowserField(){
        _fileList = new ObjectListField(){
        	public boolean keyChar(char c, int status, int time){
                if (c == Characters.ENTER){
                    onFileItemSelect();
                    return true;
                }
                else if (c == Characters.BACKSPACE){
                    onGotoParentDir();
                    return true;
                }
                else{
                    return super.keyChar(c, status, time);
                }
            }

        	
            public boolean navigationClick(int status, int time){
                onFileItemSelect();
                return true;
            }
            
            
        	public ContextMenu getContextMenu(){
        		ContextMenu menu = super.getContextMenu();
        		menu.addItem(new MenuItem(_appStrings.getString(FILE_SYSTEM_BROWSER_MENU_SELECT), 10, 10) { 
                    public void run() { 
                        onFileItemSelect();
                    }
                });
        		
        		return menu;
        	}
        };
        

        loadRootDir();
        _dirDepth = 0;
        
        _fileListContainer = new VerticalFieldManager(VERTICAL_SCROLL | VERTICAL_SCROLLBAR);
        _fileListContainer.add(_fileList);   
    }
    
    protected void createLabels(){
        _selectDirLabel = new LabelField(_appStrings.getString(FILE_SYSTEM_BROWSER_LABEL_DIR));	
        _currentDirLabel = new LabelField(_currentDir, DrawStyle.ELLIPSIS | DrawStyle.TRUNCATE_BEGINNING);
    }
    
    protected void createFilenameInputField(){
        _filenameInputField = new BasicEditField(_appStrings.getString(FILE_SYSTEM_BROWSER_FILENAME_LABEL),
        		"", TextField.DEFAULT_MAXCHARS, BasicEditField.FILTER_FILENAME){
        	public boolean keyChar(char c, int status, int time){
                if (c == Characters.ENTER){
                	onFilenameConfirm();
                    return true;
                }
                
                return super.keyChar(c, status, time);
            }
        };
    }
    
    private void setSelectFileMode(){
    	_currentFile = null;
    	//refresh current directory contents
    	if(_currentDir == PATH_PREFIX){
    		loadRootDir();
    	}
    	else{
    		loadCurrentDir();
    	}
    	
        if (_selectMode != SELECT_FILE_MODE){
        	_selectMode = SELECT_FILE_MODE;
        	setTitle(_appStrings.getString(FILE_SYSTEM_BROWSER_TITLE_SELECT_FILE));
        	deleteAll();
        	addSelectFileFields();
        }
    }
    
    private void addSelectFileFields(){
        add(_selectDirLabel);
        add(_currentDirLabel);
        add(new SeparatorField());
        add(_fileListContainer);    	
    }

    private void setSpecifyFilenameMode(){
    	_currentFile = null;
        _filenameInputField.clear(0);
    	//refresh current directory contents
    	if(_currentDir == PATH_PREFIX){
    		loadRootDir();
    	}
    	else{
    		loadCurrentDir();
    	}
        
        if (_selectMode != SPECIFY_FILENAME_MODE){
            _selectMode = SPECIFY_FILENAME_MODE;
        	setTitle(_appStrings.getString(FILE_SYSTEM_BROWSER_TITLE_SAVE_FILE));
            deleteAll();
            addSpecifyFilenameFields();
        }
    }
    
    private void addSpecifyFilenameFields(){
        add(_filenameInputField);
        add(new SeparatorField());
        add(_selectDirLabel);
        add(_currentDirLabel);
        add(new SeparatorField());
        add(_fileListContainer);	
    }

    public String getSelectedFile(){
    	setSelectFileMode();
    	UiApplication.getUiApplication().pushModalScreen(this);
    	return getFullPath();
    }
    
    public String getSaveFilename(){
    	setSpecifyFilenameMode();
    	UiApplication.getUiApplication().pushModalScreen(this);
    	return getFullPath();
    }
    
    private void onFileItemSelect(){
    	String choice = (String) _fileList.get(_fileList, _fileList.getSelectedIndex());
    	if (choice.equals(GO_UP_DIR)){
    		onGotoParentDir();
    		updateCurrentDirLabel();
    	}
    	else{
    		try {
    	        FileConnection fileConnection = (FileConnection)Connector.open(_currentDir + choice); 
    	        if (fileConnection.isDirectory()) {
    	    		_currentDir += choice;
    	    		++_dirDepth;
    	    		updateCurrentDirLabel();
    	        	loadCurrentDir();
    	        } 
    	        else { 
    	        	if (_selectMode == SELECT_FILE_MODE){
    	        		// file selected! exit
    	        		_currentFile = choice;
    	        		close();
    	        	}
    	        	else if (_selectMode == SPECIFY_FILENAME_MODE){
    	        		// display selected filename in "save as..." input field
    	        		_filenameInputField.setText(choice);
    	        	}
    	        }
            }
    	    catch (IOException ex) {
    	    	Dialog.alert(ex.getMessage());
    	    }
    	}
    }
    
    private void onFilenameConfirm(){
        _currentFile = _filenameInputField.getText();
        close();
    }
    
    private void loadRootDir(){
        _currentDir = PATH_PREFIX;
        _currentFile = null;
        Enumeration rootDirs = FileSystemRegistry.listRoots();
        populateFileList(rootDirs);
    }
    
    private void loadCurrentDir(){
    	try {
	        FileConnection fileConnection = (FileConnection)Connector.open(_currentDir); 
	        TextWarriorException.assert(fileConnection.isDirectory(),
	        		"Attempt to display a path that is not a directory");
	        Enumeration directoryEnumerator = fileConnection.list(); 
	        populateFileList(directoryEnumerator);
        }
	    catch (IOException ex) {
	    	Dialog.inform(ex.getMessage());
	    }

    }
 
    private void populateFileList(Enumeration files){
        SimpleSortingVector dirVector = new SimpleSortingVector();
        SimpleSortingVector fileVector = new SimpleSortingVector();
        dirVector.setSortComparator(StringComparator.getInstance(true));
        fileVector.setSortComparator(StringComparator.getInstance(true));
        
        while(files.hasMoreElements()) { 
        	String entry = (String) files.nextElement();
        	if (isDirectoryLabel(entry)){
                dirVector.addElement(entry);	
        	}
        	else{
                fileVector.addElement(entry);
        	}
        }
        
        dirVector.reSort();
        fileVector.reSort();
        String[] directoryContents = new String[dirVector.size() + fileVector.size()];
        // display directories before files
        dirVector.copyInto(directoryContents);
        for(int i = dirVector.size(); i < directoryContents.length; ++i){
        	directoryContents[i] = (String) fileVector.elementAt(i - dirVector.size()); 
        }
        
        _fileList.set(directoryContents);
        // add option to go up directory hierarchy
        if(hasParentDir()){
        	_fileList.insert(0, GO_UP_DIR);
        }
    }
    
    public boolean hasParentDir(){
    	return (_dirDepth > 0);
    }
    
    public void onGotoParentDir(){
    	if (hasParentDir()){
    		--_dirDepth;
    		
    		if(_dirDepth == 0){
    			loadRootDir();
    		}
    		else{
		    	TextWarriorException.assert(_currentDir.charAt(_currentDir.length()-1) == FILE_SEPARATOR,
		    		"Invalid directory path");
		    	//TODO make more efficient construction of parent path
		    	// lob off the terminating file separator.
		    	_currentDir = _currentDir.substring(0, _currentDir.length()-2);
		        // The next file separator will occur at the parent
		    	_currentDir = _currentDir.substring(0, _currentDir.lastIndexOf(FILE_SEPARATOR) + 1);
		    	
		    	// refresh file list
		    	loadCurrentDir();
    		}
    	}
    }
    
    private String getFullPath(){
    	if(_currentFile != null && _currentFile.length() > 0){
    		return _currentDir + _currentFile;
    	}

    	return null;
    }

    protected void makeMenu(Menu menu, int instance){
		super.makeMenu(menu, instance);
		menu.add(new MenuItem(_appStrings.getString(FILE_SYSTEM_BROWSER_MENU_FILENAME_CONFIRM), 5, 5) { 
            public void run() {
            	onFilenameConfirm();
            }
        });
	}
	
    protected void updateCurrentDirLabel(){
    	_currentDirLabel.setText(_currentDir);
    }

    //TODO flaky way of determining if a directory entry is a directory
    protected boolean isDirectoryLabel(String label){
    	return label.endsWith("/");
    }
    
	// Field changes made in FindSystemBrowser should never be saved.
	// Override isDirty() and isMuddy() regardless of the state of the fields within
	public boolean isDirty(){
		return false;
	}
	
	public boolean isMuddy(){
		return false;
	}
}
