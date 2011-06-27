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

import com.myopicmobile.textwarrior.common.Language;
import com.myopicmobile.textwarrior.common.TextWarriorException;


public class SymbolPicker extends PopupScreen
implements TextWarriorStringsResource, FieldChangeListener{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	//TODO translate names of categories
	private final static String[] symbolTypes = {
		_appStrings.getString(SYMBOL_PICKER_STANDARD),
		_appStrings.getString(SYMBOL_PICKER_BUSINESS),
		_appStrings.getString(SYMBOL_PICKER_MATH),
		_appStrings.getString(SYMBOL_PICKER_LETTERS),
		_appStrings.getString(SYMBOL_PICKER_TYPESETTING),
		_appStrings.getString(SYMBOL_PICKER_BLACKBERRY),
		_appStrings.getString(SYMBOL_PICKER_COMPOUND)
	};
	
	private final static char[] standardSymbols = {
		/* programming symbols not on BlackBerry keyboards */
		'=', '&', '|', '\\', '%', '~', '^',
		/* parentheses */
		'[', ']', '{', '}', '<', '>', '(', ')',
		/* Western European punctuation*/
		'«', '»', '“', '”', '‘', '’', '¡', '¿',
		/* symbols on BlackBerry keyboards */
		'!', '?', ':', ';', '.', ',', '/', '@',
		'#', '$', '*', '+', '-', '_', '\'', '"'
		};

	private final static char[] businessSymbols = {
		Characters.EURO_SIGN, Characters.YEN_SIGN, Characters.POUND_SIGN, Characters.CENT_SIGN,
		Characters.COPYRIGHT_SIGN, Characters.REGISTERED_SIGN, Characters.TRADE_MARK_SIGN,
		Characters.CURRENCY_SIGN
	};
	
	private final static char[] mathSymbols = {
		Characters.PLUS_MINUS_SIGN, Characters.MULTIPLICATION_SIGN,
		Characters.DIVISION_SIGN, Characters.MIDDLE_DOT,
		Characters.NOT_SIGN, Characters.DEGREE_SIGN,
		Characters.VULGAR_FRACTION_ONE_HALF, Characters.VULGAR_FRACTION_ONE_QUARTER,
		Characters.VULGAR_FRACTION_THREE_QUARTERS,
		Characters.SUPERSCRIPT_ONE, Characters.SUPERSCRIPT_TWO, Characters.SUPERSCRIPT_THREE,
		Characters.GREEK_CAPITAL_LETTER_GAMMA, Characters.GREEK_CAPITAL_LETTER_DELTA,
		Characters.GREEK_CAPITAL_LETTER_THETA, Characters.GREEK_CAPITAL_LETTER_LAMDA,
		Characters.MICRO_SIGN, Characters.GREEK_CAPITAL_LETTER_XI, 
		Characters.GREEK_CAPITAL_LETTER_PI, Characters.GREEK_CAPITAL_LETTER_SIGMA, 
		Characters.GREEK_CAPITAL_LETTER_PHI, Characters.GREEK_CAPITAL_LETTER_PSI,
		Characters.GREEK_CAPITAL_LETTER_OMEGA
		};

	// For Latin languages
	private final static char[] lettersSymbols = {
		/* capital letters */
		'Æ', 'Á', 'Â', 'Ä', 'À', 'Å', 'Ã', 'Ç', 'Ð',
		'É', 'Ê', 'Ë', 'È', 'Í', 'Î', 'Ï', 'Ì', 'Ñ',
		'Ó', 'Ô', 'Ö', 'Ò', 'Ø', 'Õ', 'Œ', 'Þ',
		'Ú', 'Û', 'Ü', 'Ù', 'Ý',
		/* small letters */
		'æ', 'á', 'â', 'ä', 'à', 'å', 'ã', 'ç', 'ð',
		'é', 'ê', 'ë', 'è', 'í', 'î', 'ï', 'ì', 'ñ',
		'ó', 'ô', 'ö', 'ò', 'ø', 'õ', 'ß', 'þ', 'œ',
		'ú', 'û', 'ü', 'ù', 'ý', 'ÿ'
	};
	
	private final static char[] typesettingSymbols = {
		Characters.HORIZONTAL_ELLIPSIS, Characters.EM_DASH,
		Characters.SECTION_SIGN, Characters.BROKEN_BAR,
		Characters.DAGGER, Characters.DOUBLE_DAGGER, Characters.PILCROW_SIGN,
		Characters.BULLET, Characters.WHITE_BULLET,
		Characters.MACRON, '´', '`', '¨',
	};

	private final static char[] blackBerrySymbols = {
		/* horoscope */
		Characters.ARIES, Characters.TAURUS, Characters.GEMINI, Characters.CANCER,
		Characters.LEO, Characters.VIRGO, Characters.LIBRA, Characters.SCORPIUS,
		Characters.SAGITTARIUS, Characters.CAPRICORN, Characters.AQUARIUS, Characters.PISCES,
		/* contacts */
		Characters.BLACK_TELEPHONE, Characters.TELEPHONE_LOCATION_SIGN,
		Characters.ENVELOPE,
		/* arrows */
		Characters.BLACK_UP_POINTING_SMALL_TRIANGLE, Characters.WHITE_UP_POINTING_SMALL_TRIANGLE,
		Characters.BLACK_UP_POINTING_TRIANGLE, Characters.WHITE_UP_POINTING_TRIANGLE,
		Characters.BLACK_DOWN_POINTING_SMALL_TRIANGLE, Characters.WHITE_DOWN_POINTING_SMALL_TRIANGLE,
		Characters.BLACK_DOWN_POINTING_TRIANGLE, Characters.WHITE_DOWN_POINTING_TRIANGLE,
		Characters.BLACK_LEFT_POINTING_POINTER, Characters.WHITE_LEFT_POINTING_POINTER,
		Characters.BLACK_LEFT_POINTING_SMALL_TRIANGLE, Characters.WHITE_LEFT_POINTING_SMALL_TRIANGLE,
		Characters.BLACK_LEFT_POINTING_TRIANGLE, Characters.WHITE_LEFT_POINTING_TRIANGLE,
		Characters.BLACK_RIGHT_POINTING_POINTER, Characters.WHITE_RIGHT_POINTING_POINTER,
		Characters.BLACK_RIGHT_POINTING_SMALL_TRIANGLE, Characters.WHITE_RIGHT_POINTING_SMALL_TRIANGLE,
		Characters.BLACK_RIGHT_POINTING_TRIANGLE, Characters.WHITE_RIGHT_POINTING_TRIANGLE,
		/* boxes */
		Characters.BLACK_SMALL_SQUARE,
		Characters.BOX_DRAWINGS_LIGHT_DOWN_AND_RIGHT, Characters.BOX_DRAWINGS_LIGHT_UP_AND_RIGHT,
		Characters.BOX_DRAWINGS_LIGHT_VERTICAL,
		Characters.BALLOT_BOX, Characters.BALLOT_BOX_WITH_CHECK,
		Characters.BALLOT_X, Characters.CHECK_MARK,
		/* card suits */
		Characters.BLACK_SPADE_SUIT, Characters.WHITE_SPADE_SUIT,
		Characters.BLACK_DIAMOND_SUIT, Characters.WHITE_DIAMOND_SUIT,
		Characters.BLACK_CLUB_SUIT, Characters.WHITE_CLUB_SUIT,
		Characters.BLACK_HEART_SUIT, Characters.WHITE_HEART_SUIT,
		/* graphics */
		Characters.BLACK_SUN_WITH_RAYS, Characters.CLOUD, Characters.LIGHTNING,
		Characters.SNOWMAN, Characters.UMBRELLA, Characters.SMILE
	};

	private final static String[] compoundSymbols = {
		"==", "!=", "&&", "||", "<=", ">=",
		"++", "--", "<<", ">>", "->", 
		"()", "{}", "[]", "<>", "''", "\"\"",
		_appStrings.getString(SYMBOL_PICKER_BUTTON_TAB)
	};


	private FlowFieldManager _standardSymbols;
	private FlowFieldManager _businessSymbols;
	private FlowFieldManager _mathSymbols;
	private FlowFieldManager _lettersSymbols;
	private FlowFieldManager _typesettingSymbols;
	private FlowFieldManager _blackBerrySymbols;
	private FlowFieldManager _compoundSymbols;

	private ObjectChoiceField _symbolTypeSelect;
	private String _selectedSymbol;
	
	public SymbolPicker(){
		super(new FlowFieldManager());
		_standardSymbols = createSymbols(standardSymbols);
		_mathSymbols = createSymbols(mathSymbols);
		_businessSymbols = createSymbols(businessSymbols);
		_lettersSymbols = createSymbols(lettersSymbols);
		_typesettingSymbols = createSymbols(typesettingSymbols);
		_blackBerrySymbols = createSymbols(blackBerrySymbols);
		_compoundSymbols = createSymbols(compoundSymbols);

		_symbolTypeSelect = new ObjectChoiceField(_appStrings.getString(SYMBOL_PICKER_LABEL), symbolTypes);
		_symbolTypeSelect.setChangeListener(this);
		_selectedSymbol = null;

		add(_symbolTypeSelect);
		// default is general symbols, since it is the first choice of _symbolTypeSelect
		add(_standardSymbols);
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

	protected final FlowFieldManager createSymbols(char[] charSymbols){
		String[] stringSymbols = new String[charSymbols.length];
		for (int i = 0; i < charSymbols.length; ++i){
			stringSymbols[i] = (new Character(charSymbols[i])).toString();
		}
		return createSymbols(stringSymbols);
	}
	
	public void fieldChanged(Field field, int context){
		if(field == _symbolTypeSelect){
			// re-populate screen with selected symbol types
			deleteAll();
			add(_symbolTypeSelect);
			
			int choiceIndex = _symbolTypeSelect.getSelectedIndex();
			String choice = (String) _symbolTypeSelect.getChoice(choiceIndex);
			if (choice.equals(_appStrings.getString(SYMBOL_PICKER_STANDARD))){
				add(_standardSymbols);
			}
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_COMPOUND))){
				add(_compoundSymbols);
			}
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_BLACKBERRY))){
				add(_blackBerrySymbols);
			}
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_BUSINESS))){
				add(_businessSymbols);
			}
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_MATH))){
				add(_mathSymbols);
			}
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_LETTERS))){
				add(_lettersSymbols);
			}
			else if (choice.equals(_appStrings.getString(SYMBOL_PICKER_TYPESETTING))){
				add(_typesettingSymbols);
			}
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
		
		// convert textual descriptions to the corresponding character code
		if (_selectedSymbol.equals(_appStrings.getString(SYMBOL_PICKER_BUTTON_TAB))){
			_selectedSymbol = (new Character(Language.TAB)).toString();
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
