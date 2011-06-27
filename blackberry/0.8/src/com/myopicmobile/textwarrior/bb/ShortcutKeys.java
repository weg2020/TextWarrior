/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.ui.Keypad;
import net.rim.device.api.system.KeypadListener;

public class ShortcutKeys {
	public static boolean isSwitchPanel(char c, int status){
		return ((status & KeypadListener.STATUS_SHIFT) == KeypadListener.STATUS_SHIFT &&
				c == Keypad.KEY_ENTER);
	}

	public static boolean isTabHotkey(char c, int status){
		return ((status & KeypadListener.STATUS_SHIFT) == KeypadListener.STATUS_SHIFT &&
				c == Keypad.KEY_SPACE);
	}
}
