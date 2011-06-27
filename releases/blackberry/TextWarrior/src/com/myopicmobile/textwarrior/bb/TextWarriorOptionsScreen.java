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
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.ObjectChoiceField;

import com.myopicmobile.textwarrior.common.TextWarriorException;
import com.myopicmobile.textwarrior.common.TextWarriorOptions;

public class TextWarriorOptionsScreen extends MainScreen
implements TextWarriorStringsResource{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	private TextWarriorController _controller;
	
	private final static String[] supportedSyntax = {
		_appStrings.getString(OPTIONS_SYNTAX_NO_COLOR),
		TextWarriorOptions.SYNTAX_FIELD_JAVA,
		TextWarriorOptions.SYNTAX_FIELD_C,
		TextWarriorOptions.SYNTAX_FIELD_CPP
	};

	private final static Integer[] tabSpacesChoices = {
		new Integer(1), new Integer(2), new Integer(4),
		new Integer(8), new Integer(16)
	};
	
	private final static Integer[] zoomSizeChoices = {
		new Integer(50), new Integer(75), new Integer(100), new Integer(125),
		new Integer(150), new Integer(175), new Integer(200), new Integer(300),
		new Integer(400)
	};
	
	private final static String[] encodingSchemes = {
		TextWarriorOptions.TEXT_ENCODING_AUTO,
		TextWarriorOptions.TEXT_ENCODING_ASCII,
		TextWarriorOptions.TEXT_ENCODING_UTF8,
		TextWarriorOptions.TEXT_ENCODING_UTF16BE,
		TextWarriorOptions.TEXT_ENCODING_UTF16LE
	};

	private final static String[] lineBreakStyles = {
		TextWarriorOptions.LINE_BREAK_AUTO,
		TextWarriorOptions.LINE_BREAK_LF,
		TextWarriorOptions.LINE_BREAK_CR,
		TextWarriorOptions.LINE_BREAK_CRLF
	};
	
	private ObjectChoiceField _syntaxSelectField;
	private ObjectChoiceField _tabSpacesChoiceField;
	private ObjectChoiceField _zoomSizeChoiceField;
	private CheckboxField _showLineNumbersField;
	private ObjectChoiceField _fileInputSchemeField;
	private ObjectChoiceField _fileOutputSchemeField;
	private ObjectChoiceField _lineBreakFormatField;

	public TextWarriorOptionsScreen(TextWarriorController c){
		_controller = c;
		
		createSyntaxSelectField(c.getOptions().getSyntax());
		createTabSpacesField(c.getOptions().getTabSpaces());
		createZoomSizeField(c.getOptions().getZoomSize());
		createShowLineNumbersField(c.getOptions().isShowLineNumbers());
		createFileInputSchemeField(c.getOptions().getFileInputScheme());
		createFileOutputSchemeField(c.getOptions().getFileOutputScheme());
		createLineBreakFormatField(c.getOptions().getLineBreakFormat());
		setTitle(_appStrings.getString(OPTIONS_TITLE));
	}

	protected void createSyntaxSelectField(String lang){
		String progLanguage = lang;
		if (lang.equals(TextWarriorOptions.SYNTAX_FIELD_NONE)){
			// localize the word "none"
			progLanguage = _appStrings.getString(OPTIONS_SYNTAX_NO_COLOR);
		}

		_syntaxSelectField = new ObjectChoiceField(_appStrings.getString(OPTIONS_LABEL_SYNTAX_COLOR),
				supportedSyntax, progLanguage);
		_syntaxSelectField.setChangeListener(new FieldChangeListener(){
			public void fieldChanged(Field field, int context){
				onLanguageChanged();
			}
		});
		add(_syntaxSelectField);
	}
	
	protected void createTabSpacesField(byte spacesCount){
		int initialIndex = 0;
		while(initialIndex < tabSpacesChoices.length){
			if(tabSpacesChoices[initialIndex].byteValue() == spacesCount){
				break;
			}
			++initialIndex;
		}
		
		if(initialIndex >= tabSpacesChoices.length){
			TextWarriorException.assert(false,
					"Invalid tab space option");
			initialIndex = 0; // set to default
		}
		
		_tabSpacesChoiceField = new ObjectChoiceField(_appStrings.getString(OPTIONS_LABEL_TAB_SPACES),
				tabSpacesChoices, initialIndex);
		_tabSpacesChoiceField.setChangeListener(new FieldChangeListener(){
			public void fieldChanged(Field field, int context){
				onTabSpacesChanged();
			}
		});
		add(_tabSpacesChoiceField);
	}
	
	//TODO extract method with createTabSpacesField
	private void createZoomSizeField(int zoomSize) {
		int initialIndex = 0;
		while(initialIndex < zoomSizeChoices.length){
			if(zoomSizeChoices[initialIndex].intValue() ==  zoomSize){
				break;
			}
			++initialIndex;
		}
		
		if(initialIndex >= zoomSizeChoices.length){
			TextWarriorException.assert(false,
					"Invalid zoom size option");
			initialIndex = 2; // set to default
		}
		
		_zoomSizeChoiceField = new ObjectChoiceField(_appStrings.getString(OPTIONS_LABEL_ZOOM_SIZE),
				zoomSizeChoices, initialIndex);
		_zoomSizeChoiceField.setChangeListener(new FieldChangeListener(){
			public void fieldChanged(Field field, int context){
				onZoomSizeChanged();
			}
		});
		add(_zoomSizeChoiceField);
	}

	protected void createShowLineNumbersField(boolean set) {
		_showLineNumbersField = new CheckboxField(
				_appStrings.getString(OPTIONS_LABEL_SHOW_LINE_NUMBERS), set);
		_showLineNumbersField.setChangeListener(new FieldChangeListener(){
			public void fieldChanged(Field field, int context){
				_controller.showLineNumbers(_showLineNumbersField.getChecked());
			}
		});
		add(_showLineNumbersField);
	}

	private void createFileInputSchemeField(String fileInputScheme) {
		_fileInputSchemeField = new ObjectChoiceField(_appStrings.getString(OPTIONS_LABEL_FILE_INPUT_FORMAT),
				encodingSchemes, fileInputScheme);
		_fileInputSchemeField.setChangeListener(new FieldChangeListener(){
			public void fieldChanged(Field field, int context){
				onFileInputSchemeChanged();
			}
		});
		add(_fileInputSchemeField);
	}
	
	private void createFileOutputSchemeField(String fileOutputScheme) {
		_fileOutputSchemeField = new ObjectChoiceField(_appStrings.getString(OPTIONS_LABEL_FILE_OUTPUT_FORMAT),
				encodingSchemes, fileOutputScheme);
		_fileOutputSchemeField.setChangeListener(new FieldChangeListener(){
			public void fieldChanged(Field field, int context){
				onFileOutputSchemeChanged();
			}
		});
		add(_fileOutputSchemeField);
	}
	
	private void createLineBreakFormatField(String lineBreakFormat) {
		_lineBreakFormatField = new ObjectChoiceField(_appStrings.getString(OPTIONS_LABEL_LINE_TERMINATOR_STYLE),
				lineBreakStyles, lineBreakFormat);
		_lineBreakFormatField.setChangeListener(new FieldChangeListener(){
			public void fieldChanged(Field field, int context){
				onLineBreakFormatChanged();
			}
		});
		add(_lineBreakFormatField);
	}
	
	

	/*******************************************************************
	 * Field change listeners
	 *******************************************************************/
	private void onLanguageChanged(){
		int choiceIndex = _syntaxSelectField.getSelectedIndex();
		String choice = (String) _syntaxSelectField.getChoice(choiceIndex);
		if (choice.equals(_appStrings.getString(OPTIONS_SYNTAX_NO_COLOR))){
			// convert back to English reprsentation of "None"
			choice = TextWarriorOptions.SYNTAX_FIELD_NONE;
		}
		_controller.onLanguageChanged(choice);
	}

	private void onTabSpacesChanged(){
		int choiceIndex = _tabSpacesChoiceField.getSelectedIndex();
		byte spaceCount =
			((Integer) _tabSpacesChoiceField.getChoice(choiceIndex)).byteValue();
		_controller.onTabSpacesChanged(spaceCount);
	}

	protected void onZoomSizeChanged() {
		int choiceIndex = _zoomSizeChoiceField.getSelectedIndex();
		int zoomSize =
			((Integer) _zoomSizeChoiceField.getChoice(choiceIndex)).intValue();
		_controller.onZoomSizeChanged(zoomSize);
	}

	protected void onFileInputSchemeChanged() {
		int choiceIndex = _fileInputSchemeField.getSelectedIndex();
		String choice = (String) _fileInputSchemeField.getChoice(choiceIndex);
		_controller.onFileInputSchemeChanged(choice);
	}
	
	protected void onFileOutputSchemeChanged() {
		int choiceIndex = _fileOutputSchemeField.getSelectedIndex();
		String choice = (String) _fileOutputSchemeField.getChoice(choiceIndex);
		_controller.onFileOutputSchemeChanged(choice);
	}
	
	protected void onLineBreakFormatChanged() {
		int choiceIndex = _lineBreakFormatField.getSelectedIndex();
		String choice = (String) _lineBreakFormatField.getChoice(choiceIndex);
		_controller.onLineBreakFormatChanged(choice);
	}
	
	/**
	 * Suppress save prompt.
	 * Save user options automatically before exiting
	 */
	public boolean onClose(){
		_controller.saveOptions();
		close();
		return true;
	}
}
