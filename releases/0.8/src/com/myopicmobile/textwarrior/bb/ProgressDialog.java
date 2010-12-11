/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import com.myopicmobile.textwarrior.common.ProgressObserver;
import com.myopicmobile.textwarrior.common.ProgressSource;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.GaugeField;

public class ProgressDialog extends Dialog
implements ProgressObserver, TextWarriorStringsResource{
	private static ResourceBundle _appStrings = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
	private GaugeField _progressBar;
	private ProgressSource _src;
	private int _pollingThreadId = -1;

	private final static int POLL_PERIOD = 150; // in milliseconds

	public ProgressDialog(ProgressSource src, boolean showProgress, boolean cancellable){
		super(_appStrings.getString(PROGRESS_DIALOG_MESSAGE),
			null, null, 0, null);
		
		_src = src;
		if (_src.getMin() == _src.getMax()){
			// GaugeField cannot have min == max; use a default implementation
			_progressBar = new GaugeField();
		}
		else{
			_progressBar = new GaugeField(null,
				_src.getMin(), _src.getMax(), src.getMin(), GaugeField.PERCENT);
		}
		
		//TODO remove flag; show progress for all dialogs
		if(showProgress){
			add(_progressBar);
		}
		if(cancellable){
			add(new ButtonField(_appStrings.getString(PROGRESS_DIALOG_CANCEL),
					ButtonField.CONSUME_CLICK | FIELD_HCENTER){
	
	        	public boolean keyChar(char c, int status, int time){
	                if (c == Keypad.KEY_ENTER || c == Keypad.KEY_ESCAPE){
	    				_src.forceStop();
	    				close();
	    				return true;
	                }
	                return super.keyChar(c, status, time);
	            }
	
	        	protected boolean navigationUnclick(int status, int time){
					_src.forceStop();
					close();
	        		return true;
	        	}
			});
		}
	}

	//TODO investigate whether progressBar is still redrawn if TextWarrior is not
	// the active application
	protected void onUiEngineAttached(boolean attached){
		super.onUiEngineAttached(attached);
		if(attached){
			_pollingThreadId = UiApplication.getUiApplication().invokeLater(
				new Runnable(){ 
					public void run(){_progressBar.setValue(_src.getCurrent());}},
		    POLL_PERIOD, true);
		}
		else{
			if(_pollingThreadId != -1){
				UiApplication.getUiApplication().cancelInvokeLater(_pollingThreadId);
				_pollingThreadId = -1;
			}
		}
	}
	
	public void onComplete(){
		close();
	}
	
	public void onError(String message){//TODO exercise this branch
		Dialog.alert(_appStrings.getString(PROGRESS_DIALOG_ERROR) + message);
		// error should have caused _src to stop; no need to _src.stop() here
		close();
	}
	
	public boolean keyChar(char c, int status, int time){
        if (c == Keypad.KEY_ESCAPE){
        	// ProgressDialog cannot be closed with escape key
			return true;
        }
        return super.keyChar(c, status, time);
    }
}
