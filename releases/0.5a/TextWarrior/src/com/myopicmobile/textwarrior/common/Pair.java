/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.common;

public class Pair {
	private int _first;
	private int _second;
	
	public Pair(int x, int y){
		_first = x;
		_second = y;
	}
	
	public int getFirst(){
		return _first;
	}
	
	public int getSecond(){
		return _second;
	}
}
