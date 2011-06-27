/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.LabelField;

import java.lang.Math;

import com.myopicmobile.textwarrior.common.DocumentProvider;

/**
 * MainScreen and its subclasses cannot have sublayout() overridden easily.
 * This class is a workaround to implement a custom sublayout method for arranging
 * the fields in the work area, the editor, find panel, status bar and scrollbars.
 *
 * This class contains a primitive sliding animation for displaying and hiding
 * the find panel. In release versions, the animation is disabled.
 * 
 */
public class TextWarriorWorkScreen extends Manager{
	private LabelField _titleBar;
	private String _title;
	private final static int TITLEBAR_COLOR = Color.WHITE;
	private final static int TITLEBAR_BGCOLOR = Color.BLACK;
	private FreeScrollingTextField _textField;
	private FindPanel _findPanel;
	private NativeInputPanel _nativeInputPanel;
	private boolean _isFindMode;
	private boolean _isNativeInputMode;

	/* constants and variables for the sliding animation of the find panel */
	private final static long FIND_PANEL_SLIDE_TIME = 0; // in milliseconds;
											//set to 0 to disable animation
	private final static int EDGE_WIDTH = 2;
    private long _animationStartTime;
	private int _findPanelVisibleHeight;
	private int _findPanelTargetHeight;
	

	public TextWarriorWorkScreen(TextWarriorController c){
	    super(Manager.NO_VERTICAL_SCROLL | Manager.NO_HORIZONTAL_SCROLL);
	    _titleBar = new LabelField(null, USE_ALL_WIDTH | DrawStyle.ELLIPSIS){
	    	protected void paint(Graphics graphics){
	    		int fg = graphics.getColor();
	    		int bg = graphics.getBackgroundColor();
	    		graphics.setColor(TITLEBAR_COLOR);
	    		graphics.setBackgroundColor(TITLEBAR_BGCOLOR);
	    		
	    		graphics.clear();
	    		super.paint(graphics);
	    		
	    		graphics.setBackgroundColor(bg);
	    		graphics.setColor(fg);
	    	}
	    };
		_textField = new FreeScrollingTextField(c.getDocumentProvider(), c);
		_findPanel = new FindPanel(c);
		_nativeInputPanel = new NativeInputPanel(c);
		_isFindMode = false;
		_isNativeInputMode = false;

		// _textField is supposed to get the focus on startup, so add it first
		add(_textField);
		add(_findPanel);
		add(_nativeInputPanel);	
		add(_titleBar);

		_animationStartTime = 0;
		_findPanelVisibleHeight = 0;
		_findPanelTargetHeight = 0;
	}

	public void setTitle(String title) {
		_title = title;
		UiApplication.getUiApplication().invokeLater(new Runnable(){
			public void run(){
				_titleBar.setText(_title);
			}
		});
	}
	
	protected void sublayout(int width, int height){
		if (findPanelSlideNeeded()){
            if (_animationStartTime == 0) {
                _animationStartTime = System.currentTimeMillis();
            }
            
            long timeElapsed = System.currentTimeMillis() - _animationStartTime;
            float visibleProportion;
        	if(_findPanelVisibleHeight < _findPanelTargetHeight){
        		visibleProportion = (float) Math.min(1.0,
        				(float)timeElapsed / (float)FIND_PANEL_SLIDE_TIME);
        	}
        	else{
        		visibleProportion = (float) Math.max(0.0,
        				1.0 - ((float)timeElapsed / (float)FIND_PANEL_SLIDE_TIME));
        	}

        	_findPanelVisibleHeight = (int) (visibleProportion * _findPanel.getHeight());

        	if (_findPanelVisibleHeight == _findPanelTargetHeight){
        		// Animation done. Reset animation start time
        		_animationStartTime = 0;
        	}
        }
		else{
			_findPanelVisibleHeight = _findPanelTargetHeight;
		}
		
		
		layoutChild(_titleBar, width, height);
		layoutChild(_findPanel, width, height - _titleBar.getHeight());
		layoutChild(_nativeInputPanel, width, height - _titleBar.getHeight());


		int inputPanelHeight;
		if(_isNativeInputMode){
			setPositionChild(_nativeInputPanel, 0,
					 _findPanelVisibleHeight + _titleBar.getHeight());
			inputPanelHeight = _nativeInputPanel.getHeight();
		}
		else{
			// hide native input panel
			setPositionChild(_nativeInputPanel, 0, -_nativeInputPanel.getHeight());
			inputPanelHeight = 0;
		}

		layoutChild(_textField,
			width - EDGE_WIDTH * 2, /*leave space for the borders */
			height - _titleBar.getHeight() - _findPanelVisibleHeight - inputPanelHeight - EDGE_WIDTH * 2);
		
    	// negative vertical offset to simulate find panel sliding up/down from the top
		setPositionChild(_findPanel, 0,
				 _titleBar.getHeight() - (_findPanel.getHeight()-_findPanelVisibleHeight));
		setPositionChild(_textField, EDGE_WIDTH,
				 _titleBar.getHeight() + _findPanelVisibleHeight + inputPanelHeight + EDGE_WIDTH);
		// set _titleBar last so that it paints over _findPanel
		setPositionChild(_titleBar, 0, 0);

		setExtent(width, height);
		// make caret visible in case new layout obscured it
		_textField.forceRepaint();

        if (findPanelSlideNeeded()){
        	animateNextFrame();
        }
	}
	
	private final boolean findPanelSlideNeeded() {
		return (_findPanelVisibleHeight != _findPanelTargetHeight) &&
								FIND_PANEL_SLIDE_TIME > 0;
	}
	
	protected void animateNextFrame(){
        UiApplication.getUiApplication().invokeLater(new Runnable() { 
            public void run() {
                updateLayout();
            }
        });	
	}

	public void findPanelDisplay(boolean visible) {
		if(_isFindMode && !visible){
			_findPanelTargetHeight = 0;
			_textField.setFocus();
			_isFindMode = false;
			animateNextFrame();
		}
		else if(!_isFindMode && visible){
			_findPanelTargetHeight = _findPanel.getHeight();
			_findPanel.setFocus();
			_isFindMode = true;
			animateNextFrame();
		}
	}

	public void nativeInputPanelDisplay(boolean visible) {
		if(_isNativeInputMode && !visible){
			_textField.setFocus();
			_isNativeInputMode = false;
			animateNextFrame();
		}
		else if(!_isNativeInputMode && visible){
			_nativeInputPanel.setFocus();
			_isNativeInputMode = true;
			animateNextFrame();
		}
	}
	
	public void togglePanelFocus(){
		if(_findPanel.isFocus() || _nativeInputPanel.isFocus()){
			_textField.setFocus();
		}
		else if (_textField.isFocus()){
			if(_isFindMode){
				_findPanel.setFocus();
			}
			else if (_isNativeInputMode){
				_nativeInputPanel.setFocus();
			}
		}
	}
	
	public boolean findMode(){
		return _isFindMode;
	}

	public boolean nativeInputMode() {
		return _isNativeInputMode;
	}
	
	public void setZoomSize(int zoomSize){
		double proportion = zoomSize / 100.0;
		double baseHeight = getFont().getHeight();
		int newHeight = (int) (proportion * baseHeight);
		_textField.setTypefaceSize(newHeight);
	}

	/*******************************
	 * Delegating functions 
	 ******************************/
	
	public void onDocumentLoad(DocumentProvider hDoc){
		_textField.changeDocumentProvider(hDoc);
	}
	
	public void forceEditorRepaint(){
		_textField.forceRepaint();
	}

	public void selectAllText(){
		_textField.selectAll();
	}
	
	public int getCaretPosition(){
		return _textField.getCaretPosition();
	}

	public void setCaretPosition(int i) {
		_textField.setCaretPosition(i);
	}

	public void setCaretRow(int row){
		_textField.setCaretRow(row);
	}
	
	public int getCaretRow(){
		return _textField.getCaretRow();
	}
	
	public int getSelectionAnchorPosition(){
		return _textField.getSelectionAnchor();
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

	public void selectText(boolean on){
		_textField.select(on);
	}

	public void replace(String string){
		_textField.replace(string);
	}
	
	public void setTabSpaces(int spaceCount){
		_textField.setTabSpaces(spaceCount);
	}
}
