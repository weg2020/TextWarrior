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
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.RichTextField;

public class TextWarriorHelpScreen extends MainScreen
implements TextWarriorStringsResource{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);

	public TextWarriorHelpScreen(){
		setTitle(_appStrings.getString(HELP_TITLE));
		add(new RichTextField(_appStrings.getString(HELP_TEXT), FOCUSABLE));
	}
}