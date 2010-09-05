/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.FlowFieldManager;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.system.Characters;
import net.rim.device.api.i18n.ResourceBundle;

import com.myopicmobile.textwarrior.common.TextWarriorException;


public class SymbolPicker extends PopupScreen
implements TextWarriorStringsResource, FieldChangeListener{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	
	private final static String[] symbolTypes = {
		_appStrings.getString(SYMBOL_PICKER_GENERAL),
		/*_appStrings.getString(SYMBOL_PICKER_BUSINESS),
		_appStrings.getString(SYMBOL_PICKER_MATH),
		_appStrings.getString(SYMBOL_PICKER_LANGUAGE),
		_appStrings.getString(SYMBOL_PICKER_MISC)*/
		_appStrings.getString(SYMBOL_PICKER_COMPOUND)
	};
	
	private final static String[] generalSymbols = {
		"=", "&", "|", "\\", "%", "~", "^", /* programming symbols not on BlackBerry keyboards */
		"[", "]", "{", "}", "<", ">", "(", ")", /* parentheses */
		"!", "?", ":", ";", ".", ",", "/", "@", /* symbols on BlackBerry keyboards */
		"#", "$", "*", "+", "-", "_", "'", "\""
		};
/*
	private final static char[] mathSymbols = {
		Characters.PLUS_MINUS_SIGN, Characters.MULTIPLICATION_SIGN, Characters.DIVISION_SIGN,
		Characters.VULGAR_FRACTION_ONE_HALF, Characters.VULGAR_FRACTION_ONE_QUARTER, Characters.VULGAR_FRACTION_THREE_QUARTERS,
		Characters.SUPERSCRIPT_ONE, Characters.SUPERSCRIPT_TWO, Characters.SUPERSCRIPT_THREE, Characters.MIDDLE_DOT, Characters.NOT_SIGN,
		};

	private final static char[] businessSymbols = {
		Characters.EURO_SIGN, Characters.YEN_SIGN, Characters.POUND_SIGN, Characters.CENT_SIGN,
		Characters.COPYRIGHT_SIGN, Characters.REGISTERED_SIGN, Characters.TRADE_MARK_SIGN,
		Characters.CURRENCY_SIGN
	};
	
	private final static char[] languageSymbols = {
		'`', Characters.ACUTE_ACCENT, Characters.DIAERESIS,
		Characters.INVERTED_EXCLAMATION_MARK, Characters.INVERTED_QUESTION_MARK,
		Characters.LEFT_POINTING_DOUBLE_ANGLE_QUOTATION_MARK, Characters.RIGHT_POINTING_DOUBLE_ANGLE_QUOTATION_MARK
	};
	
	private final static char[] miscSymbols = {
		Characters.HORIZONTAL_ELLIPSIS, Characters.DEGREE_SIGN, Characters.MACRON,
		Characters.SECTION_SIGN, Characters.DAGGER, Characters.DOUBLE_DAGGER, Characters.PILCROW_SIGN,
		Characters.BULLET, Characters.EM_DASH, Characters.LATIN_CAPITAL_LETTER_O_WITH_STROKE
	};
*/
	private final static String[] compoundSymbols = {
		"==", "!=", "&&", "||", "<=", ">=",
		"++", "--", "<<", ">>", "->", 
		"()", "{}", "[]", "<>", "''", "\"\"", "tab"
	};
	
	private static FlowFieldManager _generalSymbols;
	/*
	private static FlowFieldManager _mathSymbols = createSymbols(mathSymbols);
	private static FlowFieldManager _businessSymbols = createSymbols(businessSymbols);
	private static FlowFieldManager _languageSymbols = createSymbols(languageSymbols);
	private static FlowFieldManager _miscSymbols = createSymbols(miscSymbols);
	*/
	private static FlowFieldManager _compoundSymbols;

	private ObjectChoiceField _symbolTypeSelect;
	private String _selectedSymbol;
	
	public SymbolPicker(){
		super(new FlowFieldManager());
		_generalSymbols = createSymbols(generalSymbols);
		_compoundSymbols = createSymbols(compoundSymbols);

		_symbolTypeSelect = new ObjectChoiceField(_appStrings.getString(SYMBOL_PICKER_LABEL), symbolTypes);
		_symbolTypeSelect.setChangeListener(this);
		_selectedSymbol = null;
		
		add(_symbolTypeSelect);
		// default is general symbols, since it is the first choice of _symbolTypeSelect
		add(_generalSymbols);
	}
	
	protected final FlowFieldManager createSymbols(String[] symbols){
		FlowFieldManager symContainer = new FlowFieldManager();
		for (int i = 0; i < symbols.length; ++i){
			ButtonField symButton =
				new ButtonField(symbols[i], ButtonField.CONSUME_CLICK){
				protected boolean navigationUnclick(int status, int time){
					super.navigationUnclick(status, time);
					onSymbolSelect(getLabel());
					return true;
				}
				
				protected boolean keyChar(char character, int status, int time){
					if(character == '\n'){
						onSymbolSelect(getLabel());
						return true;
					}
					
					return super.keyChar(character, status, time);
				}
			};
			symContainer.add(symButton);
		}
		return symContainer;
	}
	
	protected final FlowFieldManager createSymbols(char[] symbols){
		FlowFieldManager symContainer = new FlowFieldManager();
		for (int i = 0; i < symbols.length; ++i){
			String charholder = (new Character(symbols[i])).toString();
			ButtonField symButton =
				new ButtonField(charholder, ButtonField.CONSUME_CLICK){
				protected boolean navigationUnclick(int status, int time){
					super.navigationUnclick(status, time);
					onSymbolSelect(getLabel());
					return true;
				}
				
				protected boolean keyChar(char character, int status, int time){
					if(character == '\n'){
						onSymbolSelect(getLabel());
						return true;
					}

					return super.keyChar(character, status, time);
				}
			};
			symContainer.add(symButton);
		}
		return symContainer;
	}
	
	public void fieldChanged(Field field, int context){
		if(field == _symbolTypeSelect){
			// re-populate screen with selected symbol types
			deleteAll();
			add(_symbolTypeSelect);
			
			int choiceIndex = _symbolTypeSelect.getSelectedIndex();
			String choice = (String) _symbolTypeSelect.getChoice(choiceIndex);
			if (choice.equals(_appStrings.getString(SYMBOL_PICKER_GENERAL))){
				add(_generalSymbols);
			}
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_COMPOUND))){
				add(_compoundSymbols);
			}
			/*
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_BUSINESS))){
				add(_businessSymbols);
			}
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_MATH))){
				add(_mathSymbols);
			}
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_LANGUAGE))){
				add(_languageSymbols);
			}
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_MISC))){
				add(_miscSymbols);
			}
			*/
			else{
				TextWarriorException.assert(false,
     			 	"Invalid choice in insert symbol dialog");
			}
			updateLayout();
			//!! without explicit repainting, there would be a flicker
			// as the uncovered region of the screen below gets cleared
			// and then filled up a split second later
			((TextWarriorMainScreen) getScreenBelow()).forceEditorRepaint();
		}
	}
	
	private void clearSelectedSymbol(){
		_selectedSymbol = null;
	}

	public String getSelectedSymbol(){
		clearSelectedSymbol();
		UiApplication.getUiApplication().pushModalScreen(this);
		return _selectedSymbol;
	}
	
	protected void onSymbolSelect(String symbol){
		_selectedSymbol = symbol;
		
		// convert control characters to the appropriate code
		//TODO avoid hard-coding
		if (_selectedSymbol.equals("tab")){
			_selectedSymbol = (new Character('\t')).toString();
		}
		
		close();
     }
	
	protected boolean keyChar(char character, int status, int time){
		if (character == Characters.ESCAPE) {
			close();
			return true;
		}
		return super.keyChar(character, status, time); 
	}
}
