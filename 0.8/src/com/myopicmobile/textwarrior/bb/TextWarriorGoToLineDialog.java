/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.Characters;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.PopupScreen;

public class TextWarriorGoToLineDialog extends PopupScreen
implements TextWarriorStringsResource{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	protected BasicEditField _rowInputField;
	
	public TextWarriorGoToLineDialog(){
		super(new HorizontalFieldManager());
		add(new LabelField(_appStrings.getString(MENU_GO_TO_LINE)));
		_rowInputField = new BasicEditField(FIELD_RIGHT | BasicEditField.FILTER_NUMERIC){
	    	public boolean keyChar(char c, int status, int time){
	            if (c == Characters.ENTER){
	                close();
	                return true;
	            }
	            else if (c == Characters.ESCAPE){
	            	_rowInputField.clear(0);
	                close();
	                return true;
	            }
	            else{
	                return super.keyChar(c, status, time);
	            }
	        }
	
	    	
	        public boolean navigationUnclick(int status, int time){
	            close();
	            return true;
	        }
		};
		add(_rowInputField);
	}

    public int getLine(){
    	UiApplication.getUiApplication().pushModalScreen(this);
    	if (_rowInputField.getText().length() > 0){
    		return Integer.parseInt(_rowInputField.getText());
    	}
    	else{
    		return -1;
    	}
    }
}
