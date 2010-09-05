/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

public class TextWarriorException extends Exception {
	private static final boolean NDEBUG = true; // set to true to suppress assertions
	
	public TextWarriorException(String msg){
		super(msg);
	}
	
	static public void assert(boolean condition, String details){
		if (!NDEBUG && !condition){
			/* BlackBerry dialog way of displaying errors
		        UiApplication.getUiApplication().invokeLater(new Runnable()
		        {
		            public void run()
		            {
		                Dialog.alert(message);
		            } 
		        });
		    */
			 System.err.print("TextWarrior assertion failed: ");
			 System.err.println(details);
		}
	}
}
