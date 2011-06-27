/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Keypad;

/**
 * A button that has a bitmap as a background.
 * A workaround for devices running on API 4.5 and earlier,
 * since Field.setBackground() is not available
 *
 */
public class BitmapButton extends BitmapField{
	public BitmapButton(Bitmap bitmap){
		 super(bitmap);
	}
	
	public boolean isFocusable(){
		return true;
	}
	
	protected void onUnfocus() { 
        super.onUnfocus();
        invalidate(); 
    }
	
	protected boolean navigationClick(int status, int time){
		fieldChangeNotify(0);
		return true;
	}
	
	protected boolean keyChar(char character, int status, int time){
		if (ShortcutKeys.isSwitchPanel(character, status)){
			// pass shortcut keystrokes upstream to manager
            return false;
        }
		if (character == Keypad.KEY_ENTER) { 
            fieldChangeNotify(0); 
            return true;
        } 
        return super.keyChar(character, status, time); 
	}
}
