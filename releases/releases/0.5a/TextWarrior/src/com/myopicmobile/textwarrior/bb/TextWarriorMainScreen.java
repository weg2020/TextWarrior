/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.system.EventInjector;
import net.rim.device.api.system.KeypadListener;


public final class TextWarriorMainScreen extends MainScreen
implements TextWarriorStringsResource{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	private static final int NORMAL_PRIORITY = 20;
	private static final int HIGH_PRIORITY = 10;
	
	private TextWarriorController _controller;
	private TextWarriorWorkScreen _workScreen;
	private FileSystemBrowser _fileBrowserScreen;
	private TextWarriorOptionsScreen _optionsScreen;
	private SymbolPicker _symbolPicker;
	private boolean _isFindMode;
	
	
	public TextWarriorMainScreen(TextWarriorController c){
		super(NO_HORIZONTAL_SCROLL | NO_VERTICAL_SCROLL);
		_isFindMode = false;
		_controller = c;
		_workScreen = new TextWarriorWorkScreen(c);
		_fileBrowserScreen = new FileSystemBrowser();
		_optionsScreen = new TextWarriorOptionsScreen(c);
		_symbolPicker = new SymbolPicker();
		
		add(_workScreen);
		setTitle(_appStrings.getString(FIELD_TITLE));
	}
	
	protected void makeMenu(Menu menu, int instance) { 
        super.makeMenu(menu, instance);

		menu.add(new MenuItem(_appStrings.getString(MENU_NEW), 0x00000000, NORMAL_PRIORITY) { 
			public void run() { 
			_controller.newText();
			} 
        }); 
        
        menu.add(new MenuItem(_appStrings.getString(MENU_OPEN), 0x00000001, HIGH_PRIORITY) { 
            public void run() { 
            	onOpen();
            }
        }); 
        
        if(isDirty()){
	        menu.add(new MenuItem(_appStrings.getString(MENU_SAVE), 0x00000002, NORMAL_PRIORITY) { 
	            public void run() {
	            	onSave();
	            } 
	        });
        }
        
        menu.add(new MenuItem(_appStrings.getString(MENU_SAVE_AS), 0x00000003, NORMAL_PRIORITY) { 
            public void run() { 
            	onSaveAs();
            }
        });
        
        
        String findMenuLabel;
        // Display "Hide find panel" option if find mode is active
        if(_isFindMode){
        	menu.add(new MenuItem(_appStrings.getString(MENU_SWITCH_PANEL), 0x00010010, NORMAL_PRIORITY) { 
                public void run() {
                	_workScreen.togglePanelFocus();
                } 
            });
        	findMenuLabel = _appStrings.getString(MENU_FIND_PANEL_HIDE);
        }
        else{
        	findMenuLabel = _appStrings.getString(MENU_FIND_PANEL_SHOW);
        }
        menu.add(new MenuItem(findMenuLabel, 0x00010011, NORMAL_PRIORITY) { 
        	public void run() {
        		_isFindMode = !_isFindMode;
        		_workScreen.showFindPanel(_isFindMode);
        	}
        });
/*        
        menu.add(new MenuItem(_appStrings.getString(MENU_UNDO), 0x00020020, NORMAL_PRIORITY) { 
            public void run() {
            	Dialog.inform("Not implemented yet");
            }
        });
*/       
        menu.add(new MenuItem(_appStrings.getString(MENU_SELECT_ALL), 0x00040040, NORMAL_PRIORITY) { 
            public void run() { 
            	_workScreen.selectAllText();
    			_workScreen.makeSelectionVisible();
            }
        }); 

        menu.add(new MenuItem(_appStrings.getString(MENU_INSERT_SYMBOL), 0x00040041, NORMAL_PRIORITY) { 
        	public void run() {
        		onInsertSymbol();
        	}
		});
		
        menu.add(new MenuItem(_appStrings.getString(MENU_OPTIONS), 0x00050050, NORMAL_PRIORITY) { 
			public void run() { 
		    	UiApplication.getUiApplication().pushModalScreen(_optionsScreen);
			} 
        }); 
        
        menu.add(new MenuItem(_appStrings.getString(MENU_HELP), 0x00050051, NORMAL_PRIORITY) { 
			public void run() {
		    	UiApplication.getUiApplication().pushScreen(new TextWarriorHelpScreen());
			}
        });

        menu.add(new MenuItem(_appStrings.getString(MENU_ABOUT), 0x00050052, NORMAL_PRIORITY) { 
			public void run() { 
		    	UiApplication.getUiApplication().pushScreen(new TextWarriorAboutScreen());
			} 
        });
	}
	
	
	protected void onOpen(){
		// prompt to save if current file is modified
		if (isDirty() && (onSavePrompt() == false)){
			// Cancel open file operation if save cancelled.
			return;
		}

    	String filename = _fileBrowserScreen.getSelectedFile();
    	if(filename != null){
    		_controller.open(filename);
    	}
	}
	
	protected boolean onSave(){
		if (_controller.save()){
			setDirty(false);
			return true;
		}
		
		return false;
	}

	/**
	 * Cancels save operation silently if empty filename is given.
	 * 
	 * @return
	 */
	protected boolean onSaveAs(){
    	String filename = _fileBrowserScreen.getSaveFilename();
    	if(filename != null){
    		if (_controller.save(filename, false)){
    			setDirty(false);
    			return true;
    		}
    	}
    	return false;
	}
	
	protected void onInsertSymbol(){
		String symbol = _symbolPicker.getSelectedSymbol();
		if(symbol != null){
			for (int i = 0; i < symbol.length(); ++i){
				char c = symbol.charAt(i);
    			EventInjector.KeyEvent eDown =
    				new EventInjector.KeyEvent(EventInjector.KeyEvent.KEY_DOWN,
    						c,
    						KeypadListener.STATUS_NOT_FROM_KEYPAD);
    			eDown.post();
			}
		}
	}
	
	public void showErrorDialog(String msg){
		Dialog.alert(msg);
	}

	public void showInfoDialog(String message){
		Dialog.inform(message);
	}
	
	public int ask(int type, String message, int defaultChoice){
		return Dialog.ask(type, message, defaultChoice);
	}
	
	protected boolean keyChar(char character, int status, int time){
		// consume shortcut keys
		if (ShortcutKeys.isSwitchPanel(character, status)) {
			if(_isFindMode){
				_workScreen.togglePanelFocus();
			}
			return true;
		}
		return super.keyChar(character, status, time); 
	}

	/*******************************
	 * Delegating functions 
	 ******************************/
	
	public void resetEditor(){
        setDirty(false);
		_workScreen.resetEditor();
	}

	public void forceEditorRepaint(){
		_workScreen.forceEditorRepaint();
	}
	
	public int getCursorPosition(){
		return _workScreen.getCursorPosition();
	}

	public void setCursorPosition(int position){
		_workScreen.setCursorPosition(position);
	}
	
	public int getSelectionAnchorPosition(){
		return _workScreen.getSelectionAnchorPosition();
	}
	
	public void setTextSelection(int beginPosition, int numChars){
		_workScreen.setTextSelection(beginPosition, numChars);
	}
	
	public void makeSelectionVisible(){
		_workScreen.makeSelectionVisible();
	}
	
	public boolean isTextSelected(){
		return _workScreen.isTextSelected();
	}
	
	public void selectTextMode(boolean on){
		_workScreen.selectTextMode(on);
	}
	
	public void replace(String string){
		_workScreen.replace(string);
	}
	
	public void setTabSpaces(int spaceCount){
		_workScreen.setTabSpaces(spaceCount);
	}
}

