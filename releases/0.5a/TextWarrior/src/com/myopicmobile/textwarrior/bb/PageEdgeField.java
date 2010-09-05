/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import com.myopicmobile.textwarrior.common.ScrollListener;
import com.myopicmobile.textwarrior.common.TextWarriorException;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;

/**
 * A border for pages that can be straight or jagged.
 * Direction of the edge can be set to top, bottom,
 * left or right edge.
 *
 */
public class PageEdgeField extends Field implements ScrollListener{
	protected int _direction;
	protected boolean _isJagged = true;
	protected int[] _xVertices;
	protected int[] _yVertices;
	
	public PageEdgeField(int direction){
		TextWarriorException.assert(direction == ScrollListener.TOP ||
				direction == ScrollListener.BOTTOM ||
				direction == ScrollListener.LEFT ||
				direction == ScrollListener.RIGHT,
			 	"Invalid direction for PageEdgeField");
		_direction = direction;
	}
	
	/**
	 * This field will consume all space given to it by the layout manager
	 * Postcondition: _xVertices and _yVertices will have at least 1 element
	 * 
	 */
	protected void layout(int width, int height) {
		setExtent(width, height);
		
		if(_direction == ScrollListener.TOP ||
				_direction == ScrollListener.BOTTOM){
			generateJaggedLine(width, height-1, _direction);
		}
		else if (_direction == ScrollListener.LEFT ||
				_direction == ScrollListener.RIGHT){
			generateJaggedLine(height, width-1, _direction);
		}
    }
	
	/**
	 * Side-effect of allocating space for _xVertices and _yVertices
	 * 
	 * @param axisLength
	 * @param axisWidth
	 * @param direction
	 */
	protected void generateJaggedLine(int axisLength,
			int axisWidth, int normalDirection){
		
		int vertexCount = (axisWidth > 0) ? axisLength/axisWidth + 1: 1;
		_xVertices = new int[vertexCount];
		_yVertices = new int[vertexCount];
		
		int[] axis;
		int[] normal;
		if(normalDirection == ScrollListener.TOP ||
				normalDirection == ScrollListener.BOTTOM){
			axis = _xVertices;
			normal = _yVertices;
		}
		else{
			axis = _yVertices;
			normal = _xVertices;
		}
		
		// set first vertex
		int axisCoord = 0;
		int normalCoord;
		if(normalDirection == ScrollListener.TOP ||
				normalDirection == ScrollListener.LEFT){
			normalCoord = axisWidth;
		}
		else{
			normalCoord = 0;
		}
		
		// generate rest of vertices
		for(int i = 0; i < vertexCount-1; ++i){
			axis[i] = axisCoord;
			normal[i] = normalCoord;
			normalCoord ^= axisWidth; // toggle between 0 and jaggedWidth
			axisCoord += axisWidth;
		}
		
		// add terminating vertex
		axis[vertexCount-1] = axisLength-1;
		if(_direction == ScrollListener.TOP ||
				_direction == ScrollListener.LEFT){
			normal[vertexCount-1] = Math.max(axisWidth, 0);
		}
		else{
			normal[vertexCount-1] = 0;
		}
	}
	
	protected void paint(Graphics graphics) {
		if(_isJagged){
			// deprecated. Replace with drawOutlinedPath for API 5.0 and above
			graphics.drawPathOutline(_xVertices, _yVertices, null, null, false);
		}
	}
	

    public boolean isFocusable() { 
        return false; 
    }
    
	public boolean isJagged(){
		return _isJagged;
	}
	
	public void setJagged(boolean on){
		_isJagged = on;
	}
	
	public void scrollTop(boolean on){
		switchJaggedState(on);
	}
	
	public void scrollBottom(boolean on){
		switchJaggedState(on);
	}
	
	public void scrollLeft(boolean on){
		switchJaggedState(on);
	}
	
	public void scrollRight(boolean on){
		switchJaggedState(on);
	}
	
	private final void switchJaggedState(boolean on){
		if(on && !_isJagged){
			_isJagged = true;
			this.invalidate();
		}
		else if(!on && _isJagged){
			_isJagged = false;
			this.invalidate();
		}
	}
}
