/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.component.TextField;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Keypad;

public class SingleLineTextField extends HorizontalFieldManager {
	private final static int BACKGROUND_COLOR = Color.WHITE;
	private TextField _textField;
	
	public SingleLineTextField() {
	    super(Manager.NO_VERTICAL_SCROLL | Manager.HORIZONTAL_SCROLL);
	    _textField = new TextField(TextField.NO_NEWLINE){
	    	// Use custom symbol screen; no need for BlackBerry symbol screen
	        protected boolean isSymbolScreenAllowed(){
				return false;
			}
	    };
	        
	    add(_textField);
	}

	public String getText() {
	    return _textField.getText();
	}
	
	public void clear(){
		_textField.setText(null);
	}
	
	/**
	 * Will consume all width given to it
	 */
	public void sublayout(int width, int height) {
		super.sublayout(width, height);
		setExtent(width, _textField.getHeight());
	}
	
	public void paint(Graphics g) {
		int oldColor = g.getBackgroundColor();
		g.setBackgroundColor(BACKGROUND_COLOR);
		g.clear();
		super.paint(g);
		g.setBackgroundColor(oldColor);
	}
	
	protected boolean keyChar(char character, int status, int time){
		if (ShortcutKeys.isSwitchPanel(character, status)){
			// pass shortcut keystrokes upstream to manager
            return false;
        } 
		else if (character == Keypad.KEY_ESCAPE &&
				_textField.getTextLength() > 0){
			clear();
			return true;
		}
		else if (character == Keypad.KEY_ENTER){
			fieldChangeNotify(0);
			return true;
		}
        return super.keyChar(character, status, time); 
	}
	
	/**
	 * Consume all button unclicks
	 * @param character
	 * @param status
	 * @param time
	 * @return
	 */
	protected boolean navigationUnclick(int status, int time){
		return true;
	}
}
