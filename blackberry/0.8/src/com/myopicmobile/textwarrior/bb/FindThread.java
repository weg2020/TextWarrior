/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import net.rim.device.api.ui.UiApplication;

import com.myopicmobile.textwarrior.common.Pair;
import com.myopicmobile.textwarrior.common.ProgressObserver;
import com.myopicmobile.textwarrior.common.ProgressSource;
import com.myopicmobile.textwarrior.common.TextBuffer;

class FindThread extends Thread implements ProgressSource{
	protected TextBuffer _buf;
	protected String _searchText;
	protected String _replacementText;
	protected int _start;
	protected boolean _isCaseSensitive;
	protected boolean _isMatchWord;
	protected Pair _result = new Pair(0,0);
	protected ProgressObserver _progressDialog;
	
	public FindThread(TextBuffer buf, int start,
			String searchText, String replacementText,
			boolean isCaseSensitive, boolean isMatchWord){
        _buf = buf;
        _start = start;
        _searchText = searchText;
        _replacementText = replacementText;
        _isCaseSensitive = isCaseSensitive;
        _isMatchWord = isMatchWord;
	}
	
	public void run(){
		_result = _buf.replaceAll(_searchText, _start,
				_isCaseSensitive, _isMatchWord, _replacementText);
    	notifyComplete();
	}

	public int getReplacementCount(){
		return _result.getFirst();
	}

	public int getStartOffsetChange(){
		return _result.getSecond();
	}
	
	public final void forceStop(){
		// not stoppable
	}

	public final int getMin(){
		return 0;
	}
	
	public final int getMax(){
		return _buf.getTextLength();
	}
	
	public final int getCurrent(){
		return 0;
	}

	public final void registerObserver(ProgressObserver po){
		_progressDialog = po;
	}

	public final boolean isDone(){
		// unused method; search operations are not cancellable
		return false;
	}

	protected void notifyComplete(){
		UiApplication.getUiApplication().invokeLater(new Runnable(){
			public void run(){
				_progressDialog.onComplete();
			}
		});
	}
}