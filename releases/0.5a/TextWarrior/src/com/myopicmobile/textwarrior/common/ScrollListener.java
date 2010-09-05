/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

/**
 * Listens for scroll events
 *
 */
public interface ScrollListener {
	public static final int TOP = 0;
	public static final int BOTTOM = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;

	/**
	 * Scroll event at the top edge
	 * @param on True to notify that the top of the field has been reached
	 */
	public void scrollTop(boolean on);
	
	/**
	 * Scroll event at the bottom edge
	 * @param on True to notify that the bottom of the field has been reached
	 */
	public void scrollBottom(boolean on);
	

	/**
	 * Scroll event at the left edge
	 * @param on True to notify that the left edge of the field has been reached
	 */
	public void scrollLeft(boolean on);
	
	/**
	 * Scroll event at the right edge
	 * @param on True to notify that the right edge of the field has been reached
	 */
	public void scrollRight(boolean on);
}
