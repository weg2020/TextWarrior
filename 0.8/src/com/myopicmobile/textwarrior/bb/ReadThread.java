/*
 * Copyright (c) 2010 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.bb;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.file.FileConnection;

import com.myopicmobile.textwarrior.common.TextBuffer;
import com.myopicmobile.textwarrior.common.TextWarriorException;


class ReadThread extends FileIOThread{
	private boolean _isDone = false;
	
	public ReadThread(FileConnection doc, TextBuffer buf,
	String encoding, String EOLchar){
		super(doc, buf, encoding, EOLchar);
	}
	
	public void run(){
		try{
			_isDone = false;
			_abortFlag.clear();
			
            InputStream textByteStream = _doc.openInputStream();
            _buf.read(textByteStream, (int) _doc.fileSize(), _encoding, _EOLchar, _abortFlag);
            textByteStream.close();

            if(!_abortFlag.isSet()){
				_isDone = true;
            	notifyComplete();
            }
		}
		catch (IOException ex) {
			notifyError(ex.toString());
	    }
        // Reset progress for next read/write operation
        _buf.clearProgress();
	}
	
	public final int getMin(){
		return 0;
	}
	
	public final int getMax(){
		int fileSize = -1;
		try{
			fileSize = (int) _doc.fileSize();
		}
		catch (IOException ex){
			TextWarriorException.assert(false,
			 "ReadThread: Cannot get file size");
		}
		return fileSize;
	}
	
	public final boolean isDone(){
		return _isDone;
	}
}