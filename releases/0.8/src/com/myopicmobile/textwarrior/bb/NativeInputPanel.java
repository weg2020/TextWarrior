/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.i18n.Locale;
import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.FontFamily;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.component.LabelField;

import com.myopicmobile.textwarrior.common.Language;

//TODO extract superclass with FindPanel
public class NativeInputPanel extends Manager
implements TextWarriorStringsResource, FieldChangeListener{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	private static String INPUT_SELECT_ICON = "TextWarrior_inputLocale.png";
	private final static int HINTS_FONT_SIZE = 5;
	private final static int PANEL_COLOR = Color.BLACK;
	private final static int PANEL_BACKGROUND_COLOR_TOP = Color.GHOSTWHITE;
	private final static int PANEL_BACKGROUND_COLOR_MID = Color.SILVER;
	private final static int PANEL_BACKGROUND_COLOR_BOTTOM = Color.DARKGRAY;
	private final static int PANEL_CORNER_RADIUS = 4;
	private final static int FIELD_PADDING = 6;
	
	private TextWarriorController _controller;
	private SingleLineTextField _nativeInput;
	private BitmapButton _inputSelectField;
	private LabelField _keyboardShortcutHints;
	private Locale _prevLocale;

	public NativeInputPanel(TextWarriorController c){
		super(Manager.NO_HORIZONTAL_SCROLL | Field.USE_ALL_WIDTH);
		_controller = c;
		_nativeInput = new SingleLineTextField();
		_nativeInput.setChangeListener(this);
		
		Bitmap inputSelectBitmap = Bitmap.getBitmapResource(INPUT_SELECT_ICON);
		_inputSelectField = new BitmapButton(inputSelectBitmap);
		_inputSelectField.setChangeListener(this);
		
		add(_nativeInput);
		add(_inputSelectField);
		createHintsBar();
	}
	
	private void createHintsBar(){
		_keyboardShortcutHints = new LabelField(_appStrings.getString(FIND_PANEL_SHORTCUT_INSTRUCTIONS),
				Field.FIELD_HCENTER);
		// hints should have smaller font size
		FontFamily currentFontFamily = getFont().getFontFamily(); 
        Font hintsFont = currentFontFamily.getFont(Font.PLAIN, HINTS_FONT_SIZE, Ui.UNITS_pt); 
        _keyboardShortcutHints.setFont(hintsFont);
        
        add(_keyboardShortcutHints);
	}
	/**
	 * Layout the input bar and a input select drop-down list to the right
	 * 
	 * This field will consume all width given to it
	 *
	 */
	protected void sublayout(int width, int height){
		// 3 horizontal paddings: left, right and between text field and choice field
		int availableWidth = width - (3 * FIELD_PADDING);
		// input select field gets to layout first
		layoutChild(_inputSelectField, availableWidth, height);
		// text field gets the leftovers
		layoutChild(_nativeInput, availableWidth - _inputSelectField.getWidth(), height);
		// hints get its own row
		layoutChild(_keyboardShortcutHints, width, height);
		

		setPositionChild(_nativeInput, FIELD_PADDING, FIELD_PADDING);
		setPositionChild(_inputSelectField,
				width - FIELD_PADDING - _inputSelectField.getWidth(),
				FIELD_PADDING);
		// center align hints label on next row
		int row0Height = Math.max(_nativeInput.getHeight(), _inputSelectField.getHeight());
		setPositionChild(_keyboardShortcutHints,
				(width - _keyboardShortcutHints.getWidth()) / 2,
				row0Height + 2 * FIELD_PADDING);
		
		// set extent of entire panel
		// 3 vertical paddings: top, bottom and between text field and hints field
		setExtent(width,
			_nativeInput.getHeight() + _keyboardShortcutHints.getHeight()
			+ 3 * FIELD_PADDING);
	}

	protected void subpaint(Graphics graphics) {
		int oldColor = graphics.getColor();
		graphics.setColor(PANEL_COLOR);
		
        drawPanelBackground(graphics);
        int fieldCount = getFieldCount();
        for(int i = 0; i < fieldCount; ++i){
        	paintChild(graphics, getField(i));
        }
        
		graphics.setColor(oldColor);
    }
	
	private void drawPanelBackground(Graphics graphics) {
		// Rectangle with a rounded bottom. Gradient shading from top to bottom.
		int[] xPts = {
				0,
				getWidth(),
				getWidth(),
				getWidth(),
				getWidth()-2*PANEL_CORNER_RADIUS,
				2*PANEL_CORNER_RADIUS,
				0,
				0};

		int[] yPts = {
				0,
				0,
				getHeight()-PANEL_CORNER_RADIUS,
				getHeight(),
				getHeight(),
				getHeight(),
				getHeight(),
				getHeight()-PANEL_CORNER_RADIUS};

		byte[] pointTypes = {
				Graphics.CURVEDPATH_END_POINT,
				Graphics.CURVEDPATH_END_POINT,
				Graphics.CURVEDPATH_END_POINT,
				Graphics.CURVEDPATH_QUADRATIC_BEZIER_CONTROL_POINT,
				Graphics.CURVEDPATH_END_POINT,
				Graphics.CURVEDPATH_END_POINT,
				Graphics.CURVEDPATH_QUADRATIC_BEZIER_CONTROL_POINT,
				Graphics.CURVEDPATH_END_POINT};
		
		int[] colors = {
				PANEL_BACKGROUND_COLOR_TOP,
				PANEL_BACKGROUND_COLOR_TOP,
				PANEL_BACKGROUND_COLOR_MID,
				PANEL_BACKGROUND_COLOR_MID,
				PANEL_BACKGROUND_COLOR_BOTTOM,
				PANEL_BACKGROUND_COLOR_BOTTOM,
				PANEL_BACKGROUND_COLOR_MID,
				PANEL_BACKGROUND_COLOR_MID,
			};
		
		graphics.drawShadedFilledPath(xPts, yPts, pointTypes, colors, (int[]) null);
	}
	
	/**
	 * Overridden method does not move the focus out of this manager
	 * This function will thus always return 0.
	 * 
	 */
	protected int moveFocus(int amount, int status, int time){
		super.moveFocus(amount, status, time);
		return 0;
	}
	
	public void fieldChanged(Field field, int context){
		if (field == _inputSelectField){
			_controller.setInputLocale();
		}
		else if (field == _nativeInput){
			String input = _nativeInput.getText();
			if(input.length() > 0){
				_controller.insertIntoEditor(input);
				_nativeInput.clear();
			}
			else{
				_controller.insertIntoEditor((new Character(Language.NEWLINE)).toString());
			}
		}
	}

	protected void onFocus(int direction) {
		restoreContext();
		super.onFocus(direction);
	}
	
	protected void onUnfocus() {
		restoreContext();
		super.onUnfocus();
	}

	//TODO consider refactoring with TextWarriorApplication.restoreContext()
    public void restoreContext(){
    	Locale temp =  Locale.getDefaultInputForSystem();
    	Locale.setDefaultInputForSystem(_prevLocale);
    	_prevLocale = temp;
    }
    
	// Field changes made in the panel should never be saved.
	// Override isDirty() and isMuddy() regardless of the state of the fields within
	public boolean isDirty(){
		return false;
	}
	
	public boolean isMuddy(){
		return false;
	}
}
