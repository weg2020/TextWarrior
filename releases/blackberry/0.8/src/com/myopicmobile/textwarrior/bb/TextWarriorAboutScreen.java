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
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.NullField;

public class TextWarriorAboutScreen extends MainScreen
implements TextWarriorStringsResource{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);

	public TextWarriorAboutScreen(){
		setTitle(_appStrings.getString(ABOUT_TITLE));
		// add a NullField and make label focusable to get it to scroll vertically
		add(new LabelField(_appStrings.getString(ABOUT_TEXT),
				DrawStyle.HCENTER | FOCUSABLE){
			protected void drawFocus(Graphics graphics, boolean on){
			//do nothing
			}
		});
		add(new NullField(FOCUSABLE));
	}
}
