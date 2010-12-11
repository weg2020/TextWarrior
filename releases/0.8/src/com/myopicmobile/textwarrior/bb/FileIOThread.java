/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import javax.microedition.io.file.FileConnection;

import net.rim.device.api.ui.UiApplication;

import com.myopicmobile.textwarrior.common.Flag;
import com.myopicmobile.textwarrior.common.ProgressObserver;
import com.myopicmobile.textwarrior.common.ProgressSource;
import com.myopicmobile.textwarrior.common.TextBuffer;

public abstract class FileIOThread extends Thread implements ProgressSource{
	protected FileConnection _doc;
	protected TextBuffer _buf;
	protected String _encoding;
	protected String _EOLchar;
	protected Flag _abortFlag;
	protected ProgressObserver _progressDialog;
	
	public FileIOThread(FileConnection doc, TextBuffer buf,
			String encoding, String EOLchar){
		_doc = doc;
        _buf = buf;
		_encoding = encoding;
		_EOLchar = EOLchar;
        _abortFlag = new Flag();
	}
	
	public final void forceStop(){
		_abortFlag.set();
	}

	public final int getCurrent(){
		return _buf.getProgress();
	}

	public final void registerObserver(ProgressObserver po){
		_progressDialog = po;
	}
	
	protected void notifyComplete(){
		UiApplication.getUiApplication().invokeLater(new Runnable(){
			public void run(){
				_progressDialog.onComplete();
			}
		});
	}

	protected void notifyError(final String msg){
		UiApplication.getUiApplication().invokeLater(new Runnable(){
			public void run(){
				_progressDialog.onError(msg);
			}
		});
	}
}
