/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.UiApplication;

/*For BETA version
import com.myopicmobile.textwarrior.common.ScrollListener;
*/
import java.lang.Math;

/**
 * MainScreen and its subclasses cannot have sublayout() overridden easily.
 * This class is a workaround to implement a custom sublayout method for arranging
 * the fields in the work area, the editor, find panel, status bar and scrollbars.
 *
 */
public class TextWarriorWorkScreen extends Manager{
	private FreeScrollingTextField _textField;
	private FindPanel _findPanel;
/*For BETA version
	private PageEdgeField _topEdge;
	private PageEdgeField _bottomEdge;
	private PageEdgeField _leftEdge;
	private PageEdgeField _rightEdge;
*/
	// constants and variables for the sliding animation of the find panel
	private final static long FIND_PANEL_SLIDE_TIME = 0; // in milliseconds;
											//set to 0 to disable animation
	private final static int EDGE_WIDTH = 4;
    private long _animationStartTime; 
	private int _panelVisibleHeight;
	private int _panelTargetVisibleHeight;


	public TextWarriorWorkScreen(TextWarriorController c){
	    super(Manager.NO_VERTICAL_SCROLL | Manager.NO_HORIZONTAL_SCROLL);
		_textField = new FreeScrollingTextField(c);
		_findPanel = new FindPanel(c);
/*For BETA version
		_topEdge = new PageEdgeField(ScrollListener.TOP);
		_bottomEdge = new PageEdgeField(ScrollListener.BOTTOM);
		_leftEdge = new PageEdgeField(ScrollListener.LEFT);
		_rightEdge = new PageEdgeField(ScrollListener.RIGHT);
*/
		// _textField is supposed to get the focus on startup, so add it first
		add(_textField);
		add(_findPanel);
/*For BETA version		
		add(_topEdge);
		add(_bottomEdge);
		add(_leftEdge);
		add(_rightEdge);

		// register page edges to be informed of scroll changes
		_textField.registerTopScrollListener(_topEdge);
		_textField.registerBottomScrollListener(_bottomEdge);
		_textField.registerLeftScrollListener(_leftEdge);
		_textField.registerRightScrollListener(_rightEdge);
*/		
		_animationStartTime = 0;
		_panelVisibleHeight = 0;
		_panelTargetVisibleHeight = 0;
	}
	
	protected void sublayout(int width, int height){		
		boolean slideNeeded = (_panelVisibleHeight != _panelTargetVisibleHeight) &&
								FIND_PANEL_SLIDE_TIME > 0;
								
		if (slideNeeded){
            if (_animationStartTime == 0) {
                _animationStartTime = System.currentTimeMillis();
            }
            
            long timeElapsed = System.currentTimeMillis() - _animationStartTime;
            float visibleProportion;
        	if(_panelVisibleHeight < _panelTargetVisibleHeight){
        		visibleProportion = (float) Math.min(1.0,
        				(float)timeElapsed / (float)FIND_PANEL_SLIDE_TIME);
        	}
        	else{
        		visibleProportion = (float) Math.max(0.0,
        				1.0 - ((float)timeElapsed / (float)FIND_PANEL_SLIDE_TIME));
        	}
        	
        	_panelVisibleHeight = (int) (visibleProportion * _findPanel.getHeight());

        	if (_panelVisibleHeight == _panelTargetVisibleHeight){
        		// Animation done. Reset animation start time
        		_animationStartTime = 0;
        		slideNeeded = false;
        	}
        }
		else{
			_panelVisibleHeight = _panelTargetVisibleHeight;
		}
		
		layoutChild(_findPanel, width, height);
		layoutChild(_textField,
				width - EDGE_WIDTH * 2, /*leave space for the borders */
				height - _panelVisibleHeight - EDGE_WIDTH * 2 /* ditto */ );
/*For BETA version
 		layoutChild(_topEdge,
				width - EDGE_WIDTH*2,
				EDGE_WIDTH);
		layoutChild(_bottomEdge,
				width - EDGE_WIDTH*2,
				EDGE_WIDTH);
		layoutChild(_leftEdge,
				EDGE_WIDTH,
				height - _panelVisibleHeight - EDGE_WIDTH*2);
		layoutChild(_rightEdge,
				EDGE_WIDTH,
				height - _panelVisibleHeight - EDGE_WIDTH*2);
*/		
		
    	// negative vertical offset to simulate panel sliding up/down from the top
		setPositionChild(_findPanel, 0, -(_findPanel.getHeight()-_panelVisibleHeight));
		setPositionChild(_textField, EDGE_WIDTH, _panelVisibleHeight + EDGE_WIDTH);
/*For BETA version
		setPositionChild(_topEdge, EDGE_WIDTH, _panelVisibleHeight);
		setPositionChild(_bottomEdge, EDGE_WIDTH, height - EDGE_WIDTH);
		setPositionChild(_leftEdge, 0, _panelVisibleHeight + EDGE_WIDTH);
		setPositionChild(_rightEdge, width - EDGE_WIDTH, _panelVisibleHeight + EDGE_WIDTH);
*/

		setExtent(width, height);
        _textField.forceRepaint(); // make caret visible
        
        if (slideNeeded){
        	animateNextFrame();
        }
	}
	
	public void showFindPanel(boolean show){
		if(show){
			_panelTargetVisibleHeight = _findPanel.getHeight();
			_findPanel.setFocus();
		}
		else{
			_panelTargetVisibleHeight = 0;
			_textField.setFocus();
		}
		
		animateNextFrame();
	}
	
	protected void animateNextFrame(){
        UiApplication.getUiApplication().invokeLater(new Runnable() { 
            public void run() {
                updateLayout();
            }
        });	
	}
	
	public void togglePanelFocus(){
		if(_findPanel.isFocus()){
			_textField.setFocus();
		}
		else if (_textField.isFocus()){
			_findPanel.setFocus();
		}
	}

	/*******************************
	 * Delegating functions 
	 ******************************/
	
	public void resetEditor(){
		_textField.resetView();
	}
	
	public void forceEditorRepaint(){
		_textField.forceRepaint();
	}

	public void selectAllText(){
		_textField.selectAll();
	}
	
	public int getCursorPosition(){
		return _textField.getCursorPosition();
	}

	public void setCursorPosition(int position){
		_textField.setCursorPosition(position);
	}
	
	public int getSelectionAnchorPosition(){
		return _textField.getSelectionAnchorPosition();
	}
	
	public void setTextSelection(int beginPosition, int numChars){
		_textField.setSelectionRange(beginPosition, numChars);
	}
	
	public void makeSelectionVisible(){
		_textField.makeSelectionVisible();
	}
	
	public boolean isTextSelected(){
		return _textField.isSelecting();
	}
	
	public void selectTextMode(boolean on){
		_textField.select(on);
	}
	
	public void replace(String string){
		_textField.replace(string);
	}
	
	public void setTabSpaces(int spaceCount){
		_textField.setTabSpaces(spaceCount);
	}
}
