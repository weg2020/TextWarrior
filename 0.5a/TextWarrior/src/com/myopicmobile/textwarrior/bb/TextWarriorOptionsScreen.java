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
import net.rim.device.api.ui.component.ObjectChoiceField;

import com.myopicmobile.textwarrior.common.TextWarriorException;
import com.myopicmobile.textwarrior.common.TextWarriorOptions;

public class TextWarriorOptionsScreen extends MainScreen
implements TextWarriorStringsResource{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	private TextWarriorController _controller;
	
	private final static String[] supportedSyntax = {
		_appStrings.getString(OPTIONS_NO_SYNTAX_COLOR),
		"Java", "C", "C++"
	};

	private final static Integer[] tabSpacesChoices = {
		new Integer(1), new Integer(2), new Integer(4),
		new Integer(8), new Integer(16)
	};
	
	private ObjectChoiceField _syntaxSelectField;
	private ObjectChoiceField _tabSpacesChoiceField;

	public TextWarriorOptionsScreen(TextWarriorController c){
		_controller = c;
		
		createSyntaxSelectField(c.getOptions().get_syntax());
		createTabSpacesField(c.getOptions().get_tabSpaces());
		setTitle(_appStrings.getString(OPTIONS_TITLE));
	}
	
	protected void createSyntaxSelectField(byte syntax){
		String language;
		//TODO close coupling of cases with supportedSyntax[]
		if (syntax == TextWarriorOptions.SYNTAX_JAVA){
			language = "Java";
		}
		else if (syntax == TextWarriorOptions.SYNTAX_C){
			language = "C";	
		}
		else if (syntax == TextWarriorOptions.SYNTAX_CPP){
			language = "C++";
		}
		else{
			language = _appStrings.getString(OPTIONS_NO_SYNTAX_COLOR);
		}
		_syntaxSelectField = new ObjectChoiceField(_appStrings.getString(OPTIONS_LABEL_SYNTAX_COLOR),
				supportedSyntax, language);
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
	
	private void onLanguageChanged(){
		int choiceIndex = _syntaxSelectField.getSelectedIndex();
		String choice = (String) _syntaxSelectField.getChoice(choiceIndex);
		if (choice.equals(_appStrings.getString(OPTIONS_NO_SYNTAX_COLOR))){
			_controller.setLanguage(TextWarriorOptions.SYNTAX_NONE);
		}
		else if (choice.equals("Java")){
			_controller.setLanguage(TextWarriorOptions.SYNTAX_JAVA);
		}
		else if (choice.equals("C")){
			_controller.setLanguage(TextWarriorOptions.SYNTAX_C);	
		}
		else if (choice.equals("C++")){
			_controller.setLanguage(TextWarriorOptions.SYNTAX_CPP);
		}
		else{
			TextWarriorException.assert(false,
 			 	"Invalid choice in syntax coloring options");
		}
	}

	private void onTabSpacesChanged(){
		int choiceIndex = _tabSpacesChoiceField.getSelectedIndex();
		byte spaceCount =
			((Integer) _tabSpacesChoiceField.getChoice(choiceIndex)).byteValue();
		_controller.setTabSpaces(spaceCount);
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
