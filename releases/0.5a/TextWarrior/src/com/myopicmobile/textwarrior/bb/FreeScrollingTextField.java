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
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.XYRect;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.system.Clipboard;
// For BETA version: import com.myopicmobile.textwarrior.ScrollListener;
import com.myopicmobile.textwarrior.common.CharacterEncodings;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Pair;
import com.myopicmobile.textwarrior.common.TextWarriorException;
import com.myopicmobile.textwarrior.common.TokenScanner;

import java.util.Vector;


public final class FreeScrollingTextField extends Field{
	private TextWarriorController _controller;
	private boolean _isInSelectionMode;
	private int _tabLength = 4;
	
	private int _maxVisibleHeight;
	private int _maxVisibleWidth; // in pixels
	private int _XScrollOffset; 
	private int _logicalLineOffset;// the logical line number of the first line displayed on screen
	private int _logicalCharOffset;// the logical char offset of the first char displayed on screen
	
	private int _caretPosition; // the selected logical char where the caret is on.
	private int _selectionAnchor; // in selection mode, _caretPosition and _selectionAnchor enclose the selection
	//~~ private int _leftDrawHint; // keeps tracks of first char that have to be repainted
	//~~ private int _rightDrawHint; // keeps tracks of last char that have to be repainted
	private int _startGUIRow; // the GUI row to start painting from when paint() is called
	private int _endGUIRow; // the GUI row to end painting, inclusive, when paint() is called
	private int _partitionHint; // keeps tracks of areas that have to be reanalyzed for tokens
	private Vector _partitions; // stores info about token types in the currently visible area
/*	For BETA version
	private ScrollListener _topScrollListener;
	private ScrollListener _bottomScrollListener;
	private ScrollListener _leftScrollListener;
	private ScrollListener _rightScrollListener;
*/	
	/**
	 * --------------------------------- line length
	 * Hello World(\n)                 | 12
	 * This is a test of the caret(\n) | 28
	 * func|t|ions(\n)                 | 10
	 * of this program(EOF)            | 16
	 * ---------------------------------
	 * The figure above shows an example of a device screen to
	 * clarify the significance of the different member variables.
	 * This screen shows logical lines 36 to 39 of a hypothetical
	 * text file. Assume the first char on screen is the 257th char
	 * of the text file.
	 * Characters enclosed in parentheses are non-printable.
	 * The caret is before the char 't' of the word "functions".
	 * The following hold true:
	 * 
	 * _logicalLineOffset == 36
	 * GUI line 0 is the text "Hello World"
	 * _caretPosition == 257 + 12 + 28 + 5 == 302
	 * 
	 * 
     * Note: EOF (End Of File) is considered a char with a real length of 1
	 * 
	 */

	private static final int _emptyCaretWidth = 5;
	private static final int _foregroundColor = Color.BLACK;
	private static final int _backgroundColor = Color.WHITE;
	private static final int _selForegroundColor = Color.WHITE;
	private static final int _selBackgroundColor = Color.MAROON;
	private static final int _caretForegroundColor = Color.WHITE;
	private static final int _caretBackgroundColor = Color.BLUE;
	private static final int _commentColor = 0x003F7F5F;
	private static final int _keywordColor = 0x007F0055;
	private static final int _literalColor = 0x002A00FF;
	private static final int _preprocessorColor = Color.DARKRED;
	
	private static final int START_OF_FIELD = 0;
	private static final int END_OF_FIELD = Integer.MAX_VALUE;
	private static final int REPARTITIONIZE = 8;

	
	public FreeScrollingTextField(TextWarriorController c){
		_controller = c;
		_maxVisibleHeight = 0;
		_maxVisibleWidth = 0;
		setEditable(true);
		resetView();
	}
	
	public void resetView(){
		_XScrollOffset = 0;
		_logicalLineOffset = 0;
		_logicalCharOffset = 0;
		_caretPosition = 0;
		_isInSelectionMode = false;
		_selectionAnchor = -1;
		setDrawHints(START_OF_FIELD, END_OF_FIELD);
		partitionizeLater();
		_partitions = null;
	}
	
	public void forceRepaint(){
		partitionizeLater();
		makeCaretVisible();
		invalidate();
	}
	
    //---------------------------------------------------------------------
    //------------------------- Layout methods ----------------------------
	
	/**
	 * This field will consume all space given to it by the layout manager
	 * 
	 */
	protected void layout(int width, int height) {
		_maxVisibleHeight = height;
		_maxVisibleWidth = width;
		setExtent(width, height);
    }
    
	final protected int getMaxVisibleRows(){
		return _maxVisibleHeight / rowHeight();
	}
 
	final protected int rowHeight(){
		return getFont().getHeight();
	}
/* For BETA version	
	protected void scrollNotify(int rightLimit){
		_topScrollListener.scrollTop(_logicalLineOffset > 0);
		_bottomScrollListener.scrollBottom(!isEOFOnScreen());
		_leftScrollListener.scrollLeft(_XScrollOffset > 0);
		_rightScrollListener.scrollRight(rightLimit - _XScrollOffset > _maxVisibleWidth);
	}
	
	public void registerTopScrollListener(ScrollListener listener){
		_topScrollListener = listener;
	}

	public void registerBottomScrollListener(ScrollListener listener){
		_bottomScrollListener = listener;
	}
	
	public void registerLeftScrollListener(ScrollListener listener){
		_leftScrollListener = listener;
	}
	
	public void registerRightScrollListener(ScrollListener listener){
		_rightScrollListener = listener;
	}
*/	
    //---------------------------------------------------------------------
    //-------------------------- Paint methods ----------------------------
	
    protected void paint(Graphics graphics) { 
	     // re-analyze text if needed
	     if (_partitionHint == REPARTITIONIZE){
	         DocumentProvider hDoc = _controller.getDocumentProvider();
	    	 _partitions = partitionize(hDoc, _logicalLineOffset,
	    		 totalVisibleChars(getMaxVisibleRows()));
	    	 _partitionHint = 0;
	     }
	     
		 TextWarriorException.assert(!_partitions.isEmpty(),
			 	"No tokens to paint in TextWarrior.paint()");
		 
	     XYRect currentRegion = graphics.getClippingRect();
	     graphics.pushRegion(currentRegion, -_XScrollOffset, 0);
		 paintWithDrawHints(graphics);
         graphics.popContext();
    }
    
    /**
     * Precondition: Caret has been scrolled into screen
     * 
     * @param graphics
     */
    private final void paintWithDrawHints(Graphics graphics){
		 if (isDrawHintsInvalid()){
			 // paint() called by framework instead of programatically
		   	 XYRect paintRegion = graphics.getClippingRect();
			 if (paintRegion.X2() < _maxVisibleWidth &&
					 paintRegion.Y2() < _maxVisibleHeight){
				 // subregion given; redraw entire field instead
				 invalidate();
				 return;
			 }
		 
			 // entire region given; set corresponding draw hints
			 setDrawHints(START_OF_FIELD, END_OF_FIELD);
		 }
		 
    	 DocumentProvider hDoc = _controller.getDocumentProvider();

    	 //----------------------------------------------
    	 // set up draw boundaries based on draw hints
    	 //----------------------------------------------
    	 int currentGUIRow = _startGUIRow;
    	 // Last GUI row is only defined after layout() is called,
    	 // So the actual value of END_OF_FIELD can only be known here
		 if (_endGUIRow == END_OF_FIELD ||
				 _endGUIRow >= getMaxVisibleRows()){
			 _endGUIRow = getMaxVisibleRows() - 1;
		 }
		 int maxXadvance = 0;
		 
    	 //----------------------------------------------
    	 // set up partition coloring settings
    	 //----------------------------------------------
	     int currentChar = hDoc.seekLine(_startGUIRow + _logicalLineOffset);
		 int nextPartitionIndex = _partitions.size();
		 Pair nextPartition;
	     // find the partition with starting position maximally <= startChar
		 do{
			 --nextPartitionIndex;
			 nextPartition = (Pair) _partitions.elementAt(nextPartitionIndex);
		 }while(nextPartitionIndex > 0 &&
				 nextPartition.getFirst() > (currentChar - _logicalCharOffset));

	     int color = getPartitionColor(nextPartition.getSecond());
	     
	     // look-ahead to next partition
	     if (nextPartitionIndex < (_partitions.size()-1)){
			 ++nextPartitionIndex;
			 nextPartition = (Pair) _partitions.elementAt(nextPartitionIndex);
		 }
	     
    	 //----------------------------------------------
    	 // set up graphics settings
    	 //----------------------------------------------
	     int paintX = 0;
	     int paintY = 0;
	     
	     int oldColor = graphics.getColor();
	     int oldBackgroundColor = graphics.getBackgroundColor();
	     hDoc.seekLine(_startGUIRow + _logicalLineOffset);
	     
    	 //----------------------------------------------
    	 // start painting!
    	 //----------------------------------------------
         while (currentGUIRow <= _endGUIRow &&
        		 hDoc.hasNext()){
        	 // check if formatting changes are needed
        	 if (reachedNextPartition(currentChar - _logicalCharOffset, nextPartition)){
        	     color = getPartitionColor(nextPartition.getSecond());
        		 
        		 if (nextPartitionIndex < (_partitions.size()-1)){
        			 ++nextPartitionIndex;
        			 nextPartition = (Pair) _partitions.elementAt(nextPartitionIndex);
        		 }
        	 }

    		 // draw char, depending on whether non-default backgrounds are needed
        	 char c = hDoc.next();
        	 if (isSelecting() && inSelectionRange(currentChar)){
        		 paintX += drawCharAndBackground(graphics, c, paintX, paintY,
        				 _selForegroundColor, _selBackgroundColor);
        	 }
        	 else if (isAtCaret(currentChar)){
        		 paintX += drawCharAndBackground(graphics, c, paintX, paintY,
        				 _caretForegroundColor, _caretBackgroundColor);
        	 }
        	 else{
        		 paintX += drawChar(graphics, c, paintX, paintY, color);
        	 }
        	 
        	 ++currentChar;
        	 if (c == '\n'){
        		maxXadvance = Math.max(paintX, maxXadvance);
    	 		paintX = 0;
    	 		paintY += rowHeight();
    	 		++currentGUIRow;
        	 }
         } // end while

 		 maxXadvance = Math.max(paintX, maxXadvance);

    	 //----------------------------------------------
    	 // notify of scrolling extent changes
    	 //----------------------------------------------
/* Foe BETA version
  		 	scrollNotify(maxXadvance);
 */		 
    	 //----------------------------------------------
    	 // restore graphics context
    	 //----------------------------------------------
		 graphics.setBackgroundColor(oldBackgroundColor);
		 graphics.setColor(oldColor);
		 invalidateDrawHints();
    }
    
    private final int drawCharAndBackground(Graphics g, char c, int paintX, int paintY,
    	 int color, int backgroundColor){
    	 int advance = 0;
         g.setColor(color);
   	 	 g.setBackgroundColor(backgroundColor);
 	
    	 switch (c){
    	 	case '\n': // fall-through
    	 	case CharacterEncodings.EOF:
    			g.clear(paintX, paintY, _emptyCaretWidth, rowHeight());
    			advance = _emptyCaretWidth;
    	 		break;
    	 	case '\t':
    	 		for(int i = 0; i < _tabLength; ++i){
    	 			advance += getFont().getAdvance(' ');
    	 		}
    			g.clear(paintX, paintY, advance, rowHeight());
    	 		break;
    	 	default:
    			g.clear(paintX, paintY, getFont().getAdvance(c), rowHeight());
    			advance = g.drawText(c, paintX, paintY, DrawStyle.TOP | DrawStyle.LEFT, -1);
    	 		break;
    	 	}
        	 
		return advance;
    }
    
        
    private final int drawChar(Graphics g, char c, int paintX, int paintY, int color){
    	 int advance = 0;
         g.setColor(color);
 	
    	 switch (c){
    	 	case '\n': // fall-through
    	 	case CharacterEncodings.EOF:
    			advance = _emptyCaretWidth;
    	 		break;
    	 	case '\t':
    	 		for(int i = 0; i < _tabLength; ++i){
    	 			advance += getFont().getAdvance(' ');
    	 		}
    	 		break;
    	 	default:
    			advance = g.drawText(c, paintX, paintY, DrawStyle.TOP | DrawStyle.LEFT, -1);
    	 		break;
    	 	}
        	 
		return advance;
    }

    
    /**
     * Scrolls the text horizontally and/or vertically
     * if the caret is not in the visible text region.
     * Sets drawing and partitioning hints.
     * 
     * @return True if the drawing area was scrolled horizontally
     * 			or vertically in order to make the caret visible
     */
    public boolean makeCaretVisible(){
    	boolean scrolledVertical = makeCaretRowVisible();
    	boolean scrolledHorizontal = makeCaretColumnVisible();
    	if (scrolledHorizontal || scrolledVertical){
    		setDrawHints(START_OF_FIELD, END_OF_FIELD);
        	
        	if(scrolledVertical){
        		partitionizeLater();
        	}
        	
    		return true;
    	}
    	return false;
    }
    
    /**
     * Scrolls the text up or down if the caret is not in the visible text region.
     * 
     * @return True if the area was scrolled vertically; false otherwise
     */
    private boolean makeCaretRowVisible(){
		DocumentProvider hDoc = _controller.getDocumentProvider();
		int caretRow = hDoc.offsetToLineNumber(_caretPosition);
		
		boolean scrolled = false;
    	if (caretRow < _logicalLineOffset){
    		_logicalLineOffset = caretRow;
		    _logicalCharOffset = _controller.getDocumentProvider().seekLine(_logicalLineOffset);
		    scrolled = true;
    	}
    	else if (caretRow >= _logicalLineOffset + getMaxVisibleRows()){
		    _logicalLineOffset = caretRow - getMaxVisibleRows() + 1;
		    _logicalCharOffset = _controller.getDocumentProvider().seekLine(_logicalLineOffset);
		    scrolled = true;
    	}
    	
    	return scrolled;
    }
    
    /**
     * If the caret is not going to be visible horizontally,
     * set the appropriate scroll x-offset so that it does.
     * 
	 * If the selection is on a single line but cannot fit into the screen width,
	 * only the beginning part of the selection will be shown
	 * If the selection is longer than a line, show the current cursor
     * 
     * Preconditions: GUI row of char is scrolled on screen.
     * 
     * @return True if the area was scrolled vertically; false otherwise
     */
	private boolean makeCaretColumnVisible(){
        DocumentProvider hDoc = _controller.getDocumentProvider();
        
		Pair visibleRange;
		if (isSelecting() && isSingleLineSelection()){
			int beginX;
			int endX;
			if(_caretPosition < _selectionAnchor){
				beginX = getCharXExtent(hDoc, _caretPosition).getFirst();
				endX = getCharXExtent(hDoc, _selectionAnchor - 1).getSecond();
			}
			else{
				beginX = getCharXExtent(hDoc, _selectionAnchor).getFirst();
				endX = getCharXExtent(hDoc, _caretPosition).getSecond();
			}
			
			visibleRange = new Pair(beginX, endX);
		}
		else{
			visibleRange = getCharXExtent(hDoc, _caretPosition);
		}
		
	    int scrollXBegin = visibleRange.getFirst();
	    int scrollXEnd = visibleRange.getSecond();
	    
	    // Scroll region horizontally if needed
	    boolean scrolled = false;
	    if (scrollXEnd > (_XScrollOffset + _maxVisibleWidth)){
	    	_XScrollOffset = scrollXEnd - _maxVisibleWidth;
	    	scrolled = true;
	    }
	    
	    if (scrollXBegin < _XScrollOffset){
	    	_XScrollOffset = scrollXBegin;
	    	scrolled = true;
	    }
	    
	    return scrolled;
	}
	
    /**
     * Calculates the x-coordinate extent of targetChar.
     * Precondition: targetChar is on or after caretRow
     * 
     */
    protected Pair getCharXExtent(DocumentProvider hDoc, int targetChar){
		int caretRow = hDoc.offsetToLineNumber(_caretPosition);
	    int scrollXBegin = 0;
	    int scrollXEnd = 0;

	    int charCount = hDoc.seekLine(caretRow);

 		TextWarriorException.assert(charCount <= targetChar,
 				"In getCharXExtent: targetChar is on a row before caretRow ");
 		
	    while(charCount <= targetChar && hDoc.hasNext()){
	       	 scrollXBegin = scrollXEnd;
	       	 char c = hDoc.next();
	       	 switch (c){
	     	 	case '\n':
	     	 	case CharacterEncodings.EOF:
	     	 		scrollXEnd += _emptyCaretWidth;
	    	 		break;
	    	 	case '\t':
	    	 		for(int i = 0; i < _tabLength; ++i){
		    	 		scrollXEnd += getFont().getAdvance(' ');
	    	 		}
	    	 		break;
	    	 	default:
	    	 		scrollXEnd += getFont().getAdvance(c);
	    	 		break;
	       	 }
	   	 	 ++charCount;
	    }
	    
	    return new Pair(scrollXBegin, scrollXEnd);
    }

    /**
     * Override all invalidate functions to invalidate entire field
     * and set appropriate draw hints.
     * 
     * @param GUIrow
     */
    protected void invalidate(int x, int y, int width, int height) {
    	invalidate();
    }
    
    protected void invalidateAll(int x, int y, int width, int height) {
    	invalidate();
    }
    
    protected void invalidate() {
    	setDrawHints(START_OF_FIELD, END_OF_FIELD);
    	invalidateWithDrawHints();
    }
    
    /**
     * Invalidate rows from _startGUIRow to _endGUIRow
     */
    private void invalidateWithDrawHints() {
    	int invalidateHeight;
    	if (_endGUIRow == END_OF_FIELD){
    		// if _maxVisibleHeight is not an integer multiple
    		// of row height, there will be some forgotten areas
    		// at the bottom of the screen. This calculation
    		// takes care of that case.
    		invalidateHeight = _maxVisibleHeight -
    			(_startGUIRow * rowHeight());
    	}
    	else{
    		invalidateHeight = rowHeight() * 
    			(_endGUIRow - _startGUIRow + 1);
    	}

    	super.invalidate(0, _startGUIRow * rowHeight(),
    			_maxVisibleWidth, invalidateHeight);
    }
    

    //---------------------------------------------------------------------
    //------------------------- Event handlers ----------------------------
    
    /**
     * Sets draw hints and triggers redrawing
     * Precondition: caret is at the point of insertion
     */
    protected boolean keyChar(char c, int status, int time) {
    	// Let superclass handle cut, copy, paste and selectionDelete shortcut keys
    	boolean handled = super.keyChar(c, status, time);
    	
    	// Handle user-defined shortcuts
		if (ShortcutKeys.isSwitchPanel(c, status)){
			// pass shortcut keystrokes upstream to manager
			return false;
        }
		if (ShortcutKeys.isTabHotkey(c, status)){
			c = '\t';
        }
    	
    	if (!handled && CharacterEncodings.isPrintable(c)){
    		// delete currently selected text, if any
    		if (isSelecting()){
    			realSelectionDelete();
    			realSelect(false);
    		}
    		
    		DocumentProvider hDoc = _controller.getDocumentProvider();
    		if(c == '\b' && !isSelecting() && _caretPosition != 0){
    			// Delete the char BEFORE the caret when in non-selection mode.
    			// Ignore '\b' when in selection mode,
    			// which is the standard BlackBerry behaviour.
        		hDoc.deleteAt(_caretPosition-1);
        		onMovementLeft();
        		
        		// mark rest of screen from caret for repainting
        		int startGUIRow = hDoc.offsetToLineNumber(_caretPosition) - 
        			_logicalLineOffset;
        		setDrawHints(startGUIRow, END_OF_FIELD);
			}
    		else{
        		if(c == '\n'){
            		// mark rest of screen from caret for repainting
            		int startGUIRow = hDoc.offsetToLineNumber(_caretPosition) - 
            			_logicalLineOffset;
            		setDrawHints(startGUIRow, END_OF_FIELD);
        		}
        		
        		hDoc.insertBefore(c, _caretPosition);
        		onMovementRight();
    		}
    		
    		setDirty(true);
    		handled = true;
			
			partitionizeLater();
	        makeCaretVisible();
	        invalidateWithDrawHints();
    	}
    	
    	return handled;
	}

    /**
     * Triggers redrawing
     */
    protected boolean navigationMovement(int dx, int dy, int status, int time){
    	if ((status & KeypadListener.STATUS_SHIFT) ==
    		KeypadListener.STATUS_SHIFT &&
    		!isSelecting() ){
    		// implement standard BlackBerry behaviour by entering selection mode
    		select(true);
        }
    	else{
	    	if (dy > 0){
	    		onMovementDown();
	    	}
	    	else if (dy < 0){
	    		onMovementUp();
	    	}
	    	
	    	if (dx > 0){
	    		onMovementRight();
	    	}
	    	else if (dx < 0){
	    		onMovementLeft();
	    	}
	
	        makeCaretVisible();
	        invalidateWithDrawHints();
    	}
    	return true;
    }
    
    /**
     * Sets draw hints
     */
    protected void onMovementDown(){
    	if (!caretOnLastRowOfFile()){
    		DocumentProvider hDoc = _controller.getDocumentProvider();
    	    int currCaretRow = hDoc.offsetToLineNumber(_caretPosition);
    		int newCaretRow = currCaretRow + 1;
    		
    		int currCaretColumn = getGUIcolumn(_caretPosition);
    		int currCaretRowLength = hDoc.lineLength(currCaretRow);
    		int newCaretRowLength = hDoc.lineLength(newCaretRow);
    		
    		if (currCaretColumn < newCaretRowLength){
	    		// Position at the same column as old row.
	    		_caretPosition += currCaretRowLength;
	    	}
	    	else{
	    		// Old column does not exist in the next row (next row is too short).
	    		// Position at end of next row instead.
	    		_caretPosition += currCaretRowLength - currCaretColumn + newCaretRowLength - 1;
	    	}
    		
    		setDrawHints(currCaretRow - _logicalLineOffset,
    				newCaretRow - _logicalLineOffset);
    	}
    }

    /**
     * Sets draw hints
     */
    protected void onMovementUp(){
    	if (!caretOnFirstRowOfFile()){
    		DocumentProvider hDoc = _controller.getDocumentProvider();
    	    int currCaretRow = hDoc.offsetToLineNumber(_caretPosition);
    		int newCaretRow = currCaretRow - 1;
    		
    		int currCaretColumn = getGUIcolumn(_caretPosition);
    		int newCaretRowLength = hDoc.lineLength(newCaretRow);

	    	if (currCaretColumn < newCaretRowLength){
	    		// Position at the same column as old row.
	    		_caretPosition -= newCaretRowLength;
	    	}
	    	else{
	    		// Old column does not exist in the previous row (previous row is too short).
	    		// Position at end of previous row instead.
	    		_caretPosition -= (currCaretColumn + 1);
	    	}

    		setDrawHints(newCaretRow - _logicalLineOffset,
    				currCaretRow - _logicalLineOffset);
    	}
    }

    /**
     * Sets draw hints
     */
    protected void onMovementRight(){
    	if(!caretOnEOF()){
    		DocumentProvider hDoc = _controller.getDocumentProvider();
    		int currCaretRow = hDoc.offsetToLineNumber(_caretPosition);
    		++_caretPosition;
    		int newCaretRow = hDoc.offsetToLineNumber(_caretPosition);
    		setDrawHints(currCaretRow - _logicalLineOffset,
    				newCaretRow - _logicalLineOffset);
    	}
    }

    /**
     * Sets draw hints
     */
    protected void onMovementLeft(){
    	if(_caretPosition > 0){
    		DocumentProvider hDoc = _controller.getDocumentProvider();
    		int currCaretRow = hDoc.offsetToLineNumber(_caretPosition);
    		--_caretPosition;
    		int newCaretRow = hDoc.offsetToLineNumber(_caretPosition);
    		setDrawHints(newCaretRow - _logicalLineOffset,
    				currCaretRow - _logicalLineOffset);
    	}
    }

	/**
	 * Consume all button unclicks
	 */
	protected boolean navigationUnclick(int status, int time){
		return true;
	}
	
	
    //---------------------------------------------------------------------
    //----------------------- Cut, copy, paste ----------------------------

    // Indirectly triggers redrawing through selectionDelete()
	public void selectionCut(Clipboard cb) throws IllegalStateException{
		selectionCopy(cb);
		selectionDelete();
	}

    // Indirectly triggers redrawing by select(false) call by the framework
	public void selectionCopy(Clipboard cb) throws IllegalStateException{
		if(isSelecting()){
			Pair range = getSelectionRange();
			int totalChars = range.getSecond() - range.getFirst();
			int copyPoint = range.getFirst();
			
			DocumentProvider hDoc = _controller.getDocumentProvider();
			char[] contents = hDoc.getChars(copyPoint, totalChars);
			cb.put(new String(contents));
		}
	}

    /**
     * Triggers redrawing
     */
	public boolean paste(Clipboard cb){
		replace(cb.toString());
		makeCaretVisible();
		invalidateWithDrawHints();
		return true;
	}
    
	/**
	 * Sets drawing and partitioning hints
	 * 
	 * @param replacementText
	 * @return
	 */
	public boolean replace(String replacementText){
		if (isSelecting()){
			realSelectionDelete();
			realSelect(false);
		}
		
		if(replacementText.length() > 0){
			DocumentProvider hDoc = _controller.getDocumentProvider();
			hDoc.insertBefore(replacementText.toCharArray(), _caretPosition);
			/* If the framework didn't clear the entire field when the user chooses
			 * "Paste" from the main menu, the following statement would ensure that
			 * only the required areas would be repainted. However, because the
			 * framework _does_ clear the entire field, the entire field has to be
			 * marked for repainting, which the statement after this comment does.
			 * 
			setDrawHints(hDoc.offsetToLineNumber(_caretPosition) - _logicalLineOffset,
					END_OF_FIELD);
			*/
			setDrawHints(START_OF_FIELD, END_OF_FIELD);
			_caretPosition += replacementText.length();
	
			partitionizeLater();
			setDirty(true);
		}
		return true;
	}

    /**
     * Triggers redrawing
     */
	public void selectionDelete(){
		realSelectionDelete();
		invalidateWithDrawHints();
	}

	/**
	 * Sets drawing and partitioning hints
	 */
	private void realSelectionDelete(){
		if(isSelecting()){
			Pair range = getSelectionRange();
			int totalChars = range.getSecond() - range.getFirst();
			int deletionPoint = range.getFirst();
			
			DocumentProvider hDoc = _controller.getDocumentProvider();
			hDoc.deleteAt(deletionPoint, totalChars);
			_caretPosition = deletionPoint;
			
			/* If the framework didn't clear the entire field when the user chooses
			 * "Cut" from the main menu, the following statement would ensure that
			 * only the required areas would be repainted. However, because the
			 * framework _does_ clear the entire field, the entire field has to be
			 * marked for repainting, which the statement after this comment does.
			 * 
			setDrawHints(hDoc.offsetToLineNumber(_caretPosition) - _logicalLineOffset,
				END_OF_FIELD);
			*/
			setDrawHints(START_OF_FIELD, END_OF_FIELD);
			
			partitionizeLater();
			setDirty(true);
		}
	}
	
	public boolean isSelectionCopyable(){
		return _isInSelectionMode;
	}
	
	public boolean isPasteable(){
		return true;
	}
	
	public boolean isSelectionDeleteable(){
		return _isInSelectionMode;
	}
	

    //---------------------------------------------------------------------
    //------------------------- Text Selection ----------------------------

	public boolean isSelectable(){
		return true;
	}
	
	public boolean isSelecting(){
		return _isInSelectionMode;
	}
	
	/**
	 * Triggers redrawing if select state changed
	 */
	public void select(boolean mode){
		if(realSelect(mode)){
	        invalidateWithDrawHints();
		}
	}
	
	/**
	 * Sets draw hints if select state changed
	 * @param mode
	 * @return If select state changed
	 */
	private boolean realSelect(boolean mode){
		if(!isSelecting() && mode){
			/* If the framework didn't clear the entire field when the user chooses
			 * "Select" from the main menu, the following statements would ensure that
			 * only the required areas would be repainted. However, because the
			 * framework _does_ clear the entire field, the entire field has to be
			 * marked for repainting, which the statement after this comment does.
			 *
			DocumentProvider hDoc = _controller.getDocumentProvider();
			int caretGUIRow = hDoc.offsetToLineNumber(_caretPosition) -
				_logicalLineOffset;
			setDrawHints(caretGUIRow, caretGUIRow);
			*/
			setDrawHints(START_OF_FIELD, END_OF_FIELD);
			
			// selected the single character that the caret is on
			_selectionAnchor = _caretPosition;
			_isInSelectionMode = mode;
	        return true;
		}
		else if (isSelecting() && !mode){
			/* If the framework didn't clear the entire field when the user chooses
			 * "Cancel selection" from the main menu, the following statements would ensure that
			 * only the required areas would be repainted. However, because the
			 * framework _does_ clear the entire field, the entire field has to be
			 * marked for repainting, which the statement after this comment does.
			 *
			setSelectionDrawHints();
			*/
			setDrawHints(START_OF_FIELD, END_OF_FIELD);
			_selectionAnchor = -1;
			_isInSelectionMode = mode;
			return true;
		}
		else{
			return false;
		}
	}

	/**
	 * Sets drawing hints
	 */
    public void selectAll(){
    	_isInSelectionMode = true;
    	_selectionAnchor = 0;
    	_caretPosition = _controller.getDocumentProvider().docLength() - 1;
		setSelectionDrawHints();
    }

	/**
	 * Sets drawing hints
	 */
	public void setSelectionRange(int beginPosition, int numChars){
		//TODO check validity of input
		// set drawing hints to remove previous selection or caret
		if(isSelecting()){
			setSelectionDrawHints();
		}
		else{
			DocumentProvider hDoc = _controller.getDocumentProvider();
			int caretGUIRow = hDoc.offsetToLineNumber(_caretPosition) -
				_logicalLineOffset;
			setDrawHints(caretGUIRow, caretGUIRow);
		}
		
    	_isInSelectionMode = true;
		_selectionAnchor = beginPosition;
		_caretPosition = _selectionAnchor + numChars - 1;
		setSelectionDrawHints();
	}

	/**
	 * Triggers redrawing
	 */	
	public void makeSelectionVisible(){
		makeCaretVisible();
        invalidateWithDrawHints();
	}
	
    /**
     * Returns the asymmetric range of the current selection.
     * If there is no selection, or if only EOF is selected,
     * an empty range will be returned.
     * 
     * @return Range of selection [Pair.first, Pair.second)
     */
    final protected Pair getSelectionRange(){
    	if (_selectionAnchor == -1){
    		// return empty range
    		return new Pair(0, 0);
    	}
    	
    	int selectionBegin = -1;
    	int selectionEnd = -1;

    	if (_caretPosition < _selectionAnchor){
    	    // asymmetric range
    		selectionBegin = _caretPosition;
    		selectionEnd = _selectionAnchor;
    	}
		else{
			// symmetric range
    		selectionBegin = _selectionAnchor;
    		selectionEnd = _caretPosition + 1;
		}
    	
    	int onePastEOF =  _controller.getDocumentProvider().docLength();
    	if (selectionEnd == onePastEOF){
    		//don't select EOF; rewind selectionEnd
    		--selectionEnd;
    	}
    	
    	return new Pair(selectionBegin, selectionEnd);
	}

    public int getSelectionAnchorPosition(){
		return _selectionAnchor;
	}
    
    final protected boolean inSelectionRange(int logicalCharOffset){
		 TextWarriorException.assert(_selectionAnchor >= 0,
 			 	"Select mode on but _selectionAnchor not set");
    	
    	if (_caretPosition < _selectionAnchor){
    	    // asymmetric range
    		return (_caretPosition <= logicalCharOffset &&
    				logicalCharOffset < _selectionAnchor);
    	}
		else{
			// symmetric range
			return (_selectionAnchor <= logicalCharOffset &&
    				logicalCharOffset <= _caretPosition);
		}
	}
    
	protected boolean isSingleLineSelection(){
		DocumentProvider hDoc = _controller.getDocumentProvider();
	    int caretRow = hDoc.offsetToLineNumber(_caretPosition);
	    int lineStart = hDoc.seekLine(caretRow);
		int lineLength = hDoc.lineLength(caretRow);
		
		int inclusiveBound = (_caretPosition < _selectionAnchor) ? _selectionAnchor - 1 : _selectionAnchor;
		
		return (inclusiveBound >= lineStart &&
				inclusiveBound < lineStart + lineLength);
	}

	private void setSelectionDrawHints(){
		DocumentProvider hDoc = _controller.getDocumentProvider();
		int startRow, endRow;
		if (_caretPosition >= _selectionAnchor){
			startRow = hDoc.offsetToLineNumber(_selectionAnchor);
			endRow = hDoc.offsetToLineNumber(_caretPosition);
		}
		else{
			startRow = hDoc.offsetToLineNumber(_caretPosition);
			endRow = Math.min(_logicalLineOffset + getMaxVisibleRows(),
					hDoc.offsetToLineNumber(_selectionAnchor));
		}
			
		setDrawHints(startRow - _logicalLineOffset,
				endRow - _logicalLineOffset);	
	}
	
    //---------------------------------------------------------------------
    //------------------------- Caret methods -----------------------------
    
    public void setCursorPosition(int position){
    	if(position >= 0 &&
    			position < _controller.getDocumentProvider().docLength()){
    		_caretPosition = position;
    	}
    	else{
     		TextWarriorException.assert(false,
				"Attempt to set caret to an invalid position");
    	}
    }
    
    public int getCursorPosition(){
    	return _caretPosition;
    }

    final protected boolean isAtCaret(int logicalCharOffset){
    	return (_caretPosition == logicalCharOffset);
    }
    
    /**
     * Precondition: logicalCharIndex is within the text region displayed on screen
     * 
     * @return The GUI row number where logicalCharIndex appears on
     * 
     */
    protected int getGUIrow(int logicalCharIndex){
	    int caretRow = _controller.getDocumentProvider().offsetToLineNumber(logicalCharIndex);
	    int GUIRow = caretRow - _logicalLineOffset;

 		TextWarriorException.assert(caretRow >= _logicalLineOffset &&
 				caretRow < (_logicalLineOffset + getMaxVisibleRows()),
 				"Caret not visible");
    	
 		return GUIRow;
    }

    /**
     * Preconditions: logicalCharIndex is within the text region displayed on screen
     * 
     * @return The GUI column number where logicalCharIndex appears on
     * 
     */
    protected int getGUIcolumn(int logicalCharIndex){
    	int currentRow = getGUIrow(logicalCharIndex);
	    int prevRowsCharCount = totalVisibleChars(currentRow);
 		return logicalCharIndex - _logicalCharOffset - prevRowsCharCount;
    }

    final protected boolean caretOnFirstRowOfFile(){
    	return (_caretPosition < _controller.getDocumentProvider().lineLength(0));
    }

    final protected boolean caretOnLastRowOfFile(){
    	if (isEOFOnScreen()){
        	int lastCharDisplayed = _logicalCharOffset + (totalVisibleChars(getMaxVisibleRows())) - 1;
        	return (getGUIrow(_caretPosition) == getGUIrow(lastCharDisplayed));
    	}

    	return false;
    }
    
    final protected boolean caretOnEOF(){
    	return (_caretPosition == (_controller.getDocumentProvider().docLength()-1));
    }

    final protected boolean isEOFOnScreen(){
    	int lastCharDisplayed = _logicalCharOffset + (totalVisibleChars(getMaxVisibleRows())) - 1;
    	int EOFIndex = _controller.getDocumentProvider().docLength() - 1;
    	return (lastCharDisplayed == EOFIndex);
    }
    
    //---------------------------------------------------------------------
    //------------------------- Formatting methods ------------------------
    
    final private void partitionizeLater(){
    	_partitionHint = REPARTITIONIZE;
	}

    protected Vector partitionize(DocumentProvider hDoc, int startingLine, int totalChar){
	    hDoc.seekLine(startingLine);
    	return TokenScanner.tokenize(hDoc, totalChar);
    }
    
    final protected boolean reachedNextPartition(int charIndex, Pair partition){
    	return (charIndex == partition.getFirst());
    }

    final protected int getPartitionColor(int partitionType){
    	int color;
		switch(partitionType){
		 case TokenScanner.NORMAL:
		     color = _foregroundColor;
			 break;
		 case TokenScanner.KEYWORD:
		     color = _keywordColor;
			 break;
		 case TokenScanner.COMMENT:
		     color = _commentColor;
			 break;
		 case TokenScanner.CHAR_LITERAL:
		 case TokenScanner.STRING_LITERAL:
		     color = _literalColor;
			 break;
		 case TokenScanner.PREPROCESSOR:
		     color = _preprocessorColor;
			 break;
		 default:
			 TextWarriorException.assert(false,
			 	"Invalid token type in GenericTextField");
		     color = _foregroundColor;
			 break;
		}
		return color;
    }

    private final boolean isDrawHintsInvalid(){
    	return (_startGUIRow == END_OF_FIELD &&
		 _endGUIRow == START_OF_FIELD);
    }
    
    private final void invalidateDrawHints(){
		 _startGUIRow = END_OF_FIELD;
		 _endGUIRow = START_OF_FIELD;
    }
    
    /**
     * Does not check validity of endGUIRow.
     * It is up to the caller to ensure endGUIRow < maxVisibleRows
     * 
     * @param startGUIRow
     * @param endGUIRow
     */
    private void setDrawHints(int startGUIRow, int endGUIRow){
    	// Clamp start value to be non-negative
    	if(startGUIRow < 0){
    		startGUIRow = 0;
    	}
    	
    	if (startGUIRow < _startGUIRow){
        		_startGUIRow = startGUIRow;
    	}
    	
		if(endGUIRow > _endGUIRow){
    		_endGUIRow = endGUIRow;
    	}
    }
    
    /**
     * Returns the total number of chars that appear from GUI row 0 to row (totalRows - 1).
     * 
     * @return Total characters from GUI row 0 to GUI row (totalRows - 1)
     */
    protected int totalVisibleChars(int totalRows){
    	int charCount = 0;
    	int row = 0;
    	while (row < totalRows){
    		int charsOnRow = _controller.getDocumentProvider().lineLength(_logicalLineOffset + row);
    		if (charsOnRow == 0){
    			break; // went beyond the end of file
    		}
    		charCount += charsOnRow;
    		++row;
    	}

    	return charCount;
    }
	
	final public void setTabSpaces(int spaceCount){
		_tabLength = spaceCount;
	}

    //---------------------------------------------------------------------
    //-------------------------- Focus methods ----------------------------
    
    public boolean isFocusable() { 
        return true; 
    }
    
	protected void drawFocus(Graphics graphics,boolean on){
		//do nothing
	}

	
	/* THE FOLLOWING METHODS ARE DEPRECATED!!
	 * 
	 * Archived paint-related methods that paints only chars that have changed.
	 * Works only if the clipping region in graphics is the entire field all the time.
	 * 
	 *
	   private final void paintWithDrawHints(Graphics graphics){
	    	 DocumentProvider hDoc = _controller.getDocumentProvider();

	    	 //----------------------------------------------
	    	 // set up draw boundaries based on draw hints
	    	 //----------------------------------------------
	    	 int currentRow, startChar;
			 if (_leftDrawHint == NO_DRAW){
				 return;
			 }
			 else if (_leftDrawHint == START_OF_FIELD){
				 currentRow = 0;
				 startChar = _logicalCharOffset;
			 }
			 else{
				 currentRow = hDoc.offsetToLineNumber(_leftDrawHint);
				 startChar = _leftDrawHint;
			 }

	    	 int endRow, endChar;
			 // assumes if (_rightDrawHint == NO_DRAW), (_leftDrawHint == NO_DRAW) also
	    	 // and is caught above already
			 if (_rightDrawHint == END_OF_FIELD){
				 endRow = getMaxVisibleRows();
				 endChar = Integer.MAX_VALUE;
			 }
			 else if (_rightDrawHint == END_OF_ROW){
				 endRow = hDoc.offsetToLineNumber(_caretPosition) + 1;
				 endChar = Integer.MAX_VALUE;
			 }
			 else{
				 endRow = Integer.MAX_VALUE;
				 endChar = _rightDrawHint;
			 }
			 
	    	 //----------------------------------------------
	    	 // set up partition coloring settings
	    	 //----------------------------------------------
			 int currentToken = _partitions.size() - 1;
		     Pair nextPartition = (Pair) _partitions.elementAt(currentToken);
		     // find the partition with starting position maximally <= startChar
			 while(nextPartition.getFirst() > (startChar-_logicalCharOffset) &&
					 currentToken > 0){
				 --currentToken;
				 nextPartition = (Pair) _partitions.elementAt(currentToken);
			 }

	    	 //----------------------------------------------
	    	 // set up graphics settings
	    	 //----------------------------------------------
		     int paintX = 0;
		     int paintY = currentRow * rowHeight();
		     
		     int oldColor = graphics.getColor();
		     int oldBackgroundColor = graphics.getBackgroundColor();
		     int color = getPartitionColor(nextPartition.getSecond());
		     
	    	 //----------------------------------------------
	    	 // start painting!
	    	 //----------------------------------------------
		     int currentChar = hDoc.seekLine(currentRow);
	         while (currentRow < endRow &&
	        		 currentChar <= endChar &&
	        		 hDoc.hasNext()){
	        	 // check if formatting changes are needed
	        	 if (reachedNextPartition(currentChar - _logicalCharOffset, nextPartition)){
	        	     color = getPartitionColor(nextPartition.getSecond());
	        		 
	        		 if (currentToken < (_partitions.size()-1)){
	        			 ++currentToken;
	        			 nextPartition = (Pair) _partitions.elementAt(currentToken);
	        		 }
	        	 }
	        	 
	        	 // print char and its background if it is within the range to redraw
	        	 char c = hDoc.next();
	        	 if(currentChar < startChar){
	        		 paintX += drawCharDryRun(c);
	        	 }
	        	 else{
	        		 if(currentChar == startChar &&
	        			(_rightDrawHint == END_OF_FIELD || _rightDrawHint == END_OF_ROW)){
	        			 clearWithDrawHints(graphics, paintX, paintY);
	        		 }

	        		 // draw char, depending on whether non-default backgrounds are needed
		        	 if (isSelecting() && inSelectionRange(currentChar)){
		        		 paintX += drawCharAndBackground(graphics, c, paintX, paintY,
		        				 _selForegroundColor, _selBackgroundColor);
		        	 }
		        	 else if (isAtCaret(currentChar)){
		        		 paintX += drawCharAndBackground(graphics, c, paintX, paintY,
		        				 _caretForegroundColor, _caretBackgroundColor);
		        	 }
		        	 else{
		        		 paintX += drawChar(graphics, c, paintX, paintY, color);
		        	 }
	        	 }
	        	 
	        	 ++currentChar;
	        	 if (c == '\n'){
	    	 		paintX = 0;
	    	 		paintY += rowHeight();
	    	 		++currentRow;
	        	 }
	         } // end while

	    	 //----------------------------------------------
	    	 // tidy up
	    	 //----------------------------------------------
	         
	         //restore original graphics settings
			 graphics.setBackgroundColor(oldBackgroundColor);
			 graphics.setColor(oldColor);
			 
			 // mark field display as up-to-date
			 setDrawHints(NO_DRAW, NO_DRAW);
	    }
	   */
	
    
    /**
     * Clears screen from paintX until the end of row.
     * Additionally, if the drawHint indicates to redraw to the
     * END_OF_FIELD, the area below paintY will be cleared too
     * 
     * @param graphics
     * @param paintX
     * @param beginGUIrow
     * @param endGUIrow
     */
    /*
    private void clearWithDrawHints(Graphics graphics, int paintX, int paintY){
    	if(_rightDrawHint == END_OF_FIELD ||
    		_rightDrawHint == END_OF_ROW){
    		graphics.setBackgroundColor(_backgroundColor);
    		graphics.clear(paintX, paintY, _maxVisibleWidth-paintX, rowHeight());
    		}

		if(_rightDrawHint == END_OF_FIELD){
			graphics.clear(0, paintY+rowHeight(),
					_maxVisibleWidth, _maxVisibleHeight-paintY-rowHeight());
		}
    
    }
    
    private final int drawCharDryRun(char c){
    	if (c == '\n' || c == CharacterEncodings.EOF){
    		 return _emptyCaretWidth;
    	}
    	else if (c == '\t'){
	 		int advance = 0;
	 		for(int i = 0; i < _tabLength; ++i){
    			advance += getFont().getAdvance(' ');
	 		}
	 		return advance;
    	}
    	else{
			return getFont().getAdvance(c);
    	}
    }
    */
    
    /**
     * Sets the area to redrawn to be from min(leftHint, _leftDrawHint)
     * to max(rightHint, rightDrawHint)
     * 
     * @param leftHint
     * @param rightHint
     */
	/*
     //~~
    private void setDrawHints(int leftHint, int rightHint){
    	// Lazily assume rightHint is NO_DRAW when leftHint is the same
    	if(leftHint == NO_DRAW){
    		_leftDrawHint = NO_DRAW;
    		_rightDrawHint = NO_DRAW;
    		return;
    	}
    	
    	switch(_leftDrawHint){
    	case NO_DRAW:
       		_leftDrawHint = leftHint;
       		break;
    	case END_OF_FIELD:
    		break;
    	case END_OF_ROW:
			TextWarriorException.assert(false, "Illegal state in setDrawHints");
    		break;
    	default:
    		if (leftHint == END_OF_FIELD ||
    			leftHint > _leftDrawHint){
        		_leftDrawHint = leftHint;
    		}
    		break;
    	}
    	
    	switch(_rightDrawHint){
    	case NO_DRAW:
       		_rightDrawHint = rightHint;
       		break;
    	case END_OF_FIELD:
    		break;
    	case END_OF_ROW:
    		//!! also check if endOfRow char < _rightDrawHint
    		if (rightHint == END_OF_FIELD){
    			_rightDrawHint = END_OF_FIELD;
    		}
    		break;
    	default:
    		//!! also check if _rightDrawHint < endOfRow char
    		if (rightHint == END_OF_FIELD || rightHint == END_OF_ROW ||
    			rightHint > _rightDrawHint){
        		_rightDrawHint = rightHint;
    		}
    		break;
    	}
    }
    */
}
