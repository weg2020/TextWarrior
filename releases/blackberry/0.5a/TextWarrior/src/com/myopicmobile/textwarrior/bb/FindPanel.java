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
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.FontFamily;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Field;

public class FindPanel extends Manager
implements TextWarriorStringsResource, FieldChangeListener{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	private static String FIND_ICON = "TextWarrior_arrow_right.png";
	private static String FIND_BACKWARDS_ICON = "TextWarrior_arrow_left.png";
	private static String REPLACE_ICON = "TextWarrior_replace.png";
	private static String REPLACE_ALL_ICON = "TextWarrior_replace_all.png";
	private final static int FONT_SIZE = 8;
	private final static int HINTS_FONT_SIZE = 5;
	private final static int PANEL_COLOR = Color.BLACK;
	private final static int PANEL_BACKGROUND_COLOR = Color.SILVER;
	private final static int FIELD_PADDING = 4;

	private TextWarriorController _controller;
	private SingleLineTextField _searchedText;
	private SingleLineTextField _replacementText;
	private BitmapButton _findNext;
	private BitmapButton _findPrevious;
	private BitmapButton _replace;
	private BitmapButton _replaceAll;
	private CheckboxField _isCaseSensitive;
	private CheckboxField _isMatchWholeWord;
	private LabelField _findLabel;
	private LabelField _replaceLabel;
	private LabelField _keyboardShortcutHints;
	
	public FindPanel(TextWarriorController c){
		super(Manager.NO_HORIZONTAL_SCROLL | Field.USE_ALL_WIDTH);
		_controller = c;
		createFindBar();
		createReplaceBar();
		createOptionsBar();
		createHintsBar();

        // set default font
		FontFamily currentFontFamily = getFont().getFontFamily();
        setFont(currentFontFamily.getFont(Font.PLAIN, FONT_SIZE, Ui.UNITS_pt)); 
	}
	
	private void createFindBar(){
		_findLabel = new LabelField(_appStrings.getString(FIND_PANEL_FIND_LABEL));
		_searchedText = new SingleLineTextField();
		_searchedText.setChangeListener(this);

		Bitmap findNextBitmap = Bitmap.getBitmapResource(FIND_ICON);
	    _findNext = new BitmapButton(findNextBitmap);
		_findNext.setChangeListener(this);

		Bitmap findPreviousBitmap = Bitmap.getBitmapResource(FIND_BACKWARDS_ICON);
	    _findPrevious = new BitmapButton(findPreviousBitmap);
		_findPrevious.setChangeListener(this);

		add(_findLabel);
		add(_searchedText);
		add(_findPrevious);
		add(_findNext);
	}
	
	private void createReplaceBar(){
		_replaceLabel = new LabelField(_appStrings.getString(FIND_PANEL_REPLACE_LABEL));
		_replacementText = new SingleLineTextField(); 
		_replacementText.setChangeListener(this);
		
		Bitmap replaceBitmap = Bitmap.getBitmapResource(REPLACE_ICON);
	    _replace = new BitmapButton(replaceBitmap); 
		_replace.setChangeListener(this);


		Bitmap replaceAllBitmap = Bitmap.getBitmapResource(REPLACE_ALL_ICON);
	    _replaceAll = new BitmapButton(replaceAllBitmap);
		_replaceAll.setChangeListener(this);
			
		add(_replaceLabel);
		add(_replacementText);
		add(_replace);
		add(_replaceAll);
	}
	
	private void createOptionsBar(){
		_isCaseSensitive = new CheckboxField(_appStrings.getString(FIND_PANEL_CASE_SENSITIVE), false);
		_isMatchWholeWord = new CheckboxField(_appStrings.getString(FIND_PANEL_WHOLE_WORD), false);
		
		add(_isCaseSensitive);
		add(_isMatchWholeWord);
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
	 * Layout the find bar, replace bar, options bar and hints bar.
	 * The bars are all vertically stacked.
	 * The find bar and replace bar comprise of a label, a text box, and 2 buttons arranged in a row
	 * 
	 * This field will consume all width given to it
	 *
	 */
	protected void sublayout(int width, int height){
		// account for right padding of 2 right-most buttons
		int availableWidth = width - (2 * FIELD_PADDING);
		// buttons get to layout first
		int maxButtonWidth = layoutButtons(availableWidth, height);
		// first few rows have 2 buttons each, occupying maxButtonWidth
		availableWidth -= 2 * maxButtonWidth;
		// account for left and right padding of label
		availableWidth -= 2 * FIELD_PADDING;
		// layout labels with available space
		int maxLabelWidth = layoutLabels(availableWidth, height);
		availableWidth -= maxLabelWidth + FIELD_PADDING;
		// account for right padding of text boxes
		availableWidth -= FIELD_PADDING;
		// remaining space goes to text boxes
		layoutTextBoxes(availableWidth, height);
		
		//other rows consume max width
		layoutChild(_isCaseSensitive, width, height);
		layoutChild(_isMatchWholeWord, width, height);
		layoutChild(_keyboardShortcutHints, width, height);
		
		
		//TODO UGLY! refactor soon
		//set positions of find bar fields
		int x = FIELD_PADDING;
		int y = FIELD_PADDING;
		// right align label
		setPositionChild(_findLabel,
				x + maxLabelWidth - _findLabel.getWidth(),
				y);
		x += maxLabelWidth + FIELD_PADDING;
		setPositionChild(_searchedText, x, y);
		x += _searchedText.getWidth() + FIELD_PADDING;
		setPositionChild(_findPrevious, x, y);
		x += maxButtonWidth + FIELD_PADDING;
		setPositionChild(_findNext, x, y);
		

		//set positions of replace bar fields
		x = FIELD_PADDING;
		//TODO assumes max height of a field is from _findLabel field. Doesn't handle vertical alignment
		y += _findLabel.getHeight() + FIELD_PADDING + 2;
		// right align label
		setPositionChild(_replaceLabel,
				x + maxLabelWidth - _replaceLabel.getWidth(),
				y);
		x += maxLabelWidth + FIELD_PADDING;
		setPositionChild(_replacementText, x, y);
		x += _replacementText.getWidth() + FIELD_PADDING;
		setPositionChild(_replace, x, y);
		x += maxButtonWidth + FIELD_PADDING;
		setPositionChild(_replaceAll, x, y);

		//set positions of options bar fields
		//TODO assumes max height of a field is from _findLabel field. Doesn't handle vertical alignment
		y += _findLabel.getHeight() + FIELD_PADDING + 2;
		setPositionChild(_isCaseSensitive, FIELD_PADDING, y);
		// right align second option box
		setPositionChild(_isMatchWholeWord,
				width - _isMatchWholeWord.getWidth() - FIELD_PADDING,
				y);
		
		// set positions of hints bar
		y += _isMatchWholeWord.getHeight() + FIELD_PADDING;
		// center align hints label
		setPositionChild(_keyboardShortcutHints,
				(width - _keyboardShortcutHints.getWidth()) / 2,
				y);
		
		// set extent of entire find panel
		y += _keyboardShortcutHints.getHeight() + FIELD_PADDING;
		setExtent(width, y);
	}
	
	/**
	 * 
	 * @param width
	 * @param height
	 * @return Maximum width of a button
	 */
	protected int layoutButtons(int width, int height){
		Field[] buttons = {_findNext, _findPrevious, _replace, _replaceAll};
		int maxButtonWidth = 0;
		
		for(int i = 0; i < buttons.length; ++i){
			layoutChild(buttons[i], width, height);
			if(buttons[i].getWidth() > maxButtonWidth){
				maxButtonWidth = buttons[i].getWidth();
			}
		}
		
		return maxButtonWidth;
	}
	
	/**
	 * 
	 * @param width
	 * @param height
	 * @return Maximum width of a label
	 */
	protected int layoutLabels(int width, int height){
		Field[] labels = {_findLabel, _replaceLabel};
		int maxLabelWidth = 0;
		
		for(int i = 0; i < labels.length; ++i){
			layoutChild(labels[i], width, height);
			if(labels[i].getWidth() > maxLabelWidth){
				maxLabelWidth = labels[i].getWidth();
			}
		}
		
		return maxLabelWidth;
	}
	
	protected void layoutTextBoxes(int width, int height){
		layoutChild(_searchedText, width, height);
		layoutChild(_replacementText, width, height);
	}
	
	protected void subpaint(Graphics graphics) {
		int oldColor = graphics.getColor();
		int oldBackgroundColor = graphics.getBackgroundColor();
		graphics.setColor(PANEL_COLOR);
        graphics.setBackgroundColor(PANEL_BACKGROUND_COLOR);
        graphics.clear();
        
        int fieldCount = getFieldCount();
        for(int i = 0; i < fieldCount; ++i){
        	paintChild(graphics, getField(i));
        }
        
		graphics.setColor(oldColor);
        graphics.setBackgroundColor(oldBackgroundColor);
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
		if (field == _findNext || field == _searchedText){
			_controller.find(getSearchText(), isCaseSensitive(), isMatchWord());
		}
		else if (field == _replace || field == _replacementText){
			_controller.replace(getReplacementText());
		}
		else if (field == _findPrevious){
			_controller.findBackwards(getSearchText(), isCaseSensitive(), isMatchWord());
		}
		else if (field == _replaceAll){
			_controller.replaceAll(getSearchText(), isCaseSensitive(),
					isMatchWord(), getReplacementText());
		}
	}
	
	// Field changes made in FindPanel should never be saved.
	// Override isDirty() and isMuddy() regardless of the state of the fields within
	public boolean isDirty(){
		return false;
	}
	
	public boolean isMuddy(){
		return false;
	}
	
	public final String getSearchText(){
		return _searchedText.getText();
	}
	
	public final String getReplacementText(){
		return _replacementText.getText();
	}
	
	public final boolean isCaseSensitive(){
		return _isCaseSensitive.getChecked();
	}
	
	public final boolean isMatchWord(){
		return _isMatchWholeWord.getChecked();
	}
}
