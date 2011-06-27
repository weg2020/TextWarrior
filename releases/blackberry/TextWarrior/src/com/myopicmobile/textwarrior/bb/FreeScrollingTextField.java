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
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.FontFamily;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.XYRect;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.system.Clipboard;
import com.myopicmobile.textwarrior.common.Language;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Pair;
import com.myopicmobile.textwarrior.common.RowListener;
import com.myopicmobile.textwarrior.common.TextWarriorException;
import com.myopicmobile.textwarrior.common.TokenScanner;
import com.myopicmobile.textwarrior.common.ColorScheme;

import java.util.Vector;


/**
 * Responsibilities
 * 1. Display of text
 * 2. Keep track and display of caret position and selection range
 * 3. Keep track of areas to repaint and repartitionize
 * 4. Keep track of row offset and corresponding char offset of frame on screen
 * 5. Know own dimensions, tab length, whether more areas to be scrolled
 * 6. Interpret user and BB shortcut keystrokes
 * 7. Can be called to reset view and set cursor position and selection range
 * 
 * Inner class controller responsibilities
 * 1. Navigation logic
 * 2. Keep track of selection mode
 * 3. Schedule areas to repaint and repartitionize in response to edits and navigation
 * 4. Cut, copy, paste, delete and insert
 * 5. Notify rowListeners when caret row changes
 * 
 * @author prokaryx
 *
 */
public final class FreeScrollingTextField extends Field{
	protected DocumentProvider _hDoc; // the model in MVC
	protected TextFieldController _fieldController; // the controller in MVC
	protected RowListener _rLis;
	protected int _tabLength = 4;
	
	protected int _maxVisibleHeight = 0; // in pixels
	protected int _maxVisibleWidth = 0; // in pixels
	
	/* the row number of the first line displayed; 0 is the first line of file */
	protected int _rowOffset;
	/* the offset of the first char displayed; 0 is the first char of file */
	protected int _charOffset;
	/* the selected char where the caret is on */
	protected int _caretPosition;
	/* in selection mode, _caretPosition and _selectionAnchor enclose the selection */
	protected int _selectionAnchor;

	/* the row to start painting from when paint() is called */
	private int _beginPaintRow = 0;
	/* the row to end painting, inclusive, when paint() is called */
	private int _endPaintRow = 0;
	private int _XScrollOffset;
	/* tokens (keywords, comments, etc.) in the currently visible area */
	protected Vector _partitions;
	/* true if partitions have changed and the edited area needs to be re-parsed */
	private boolean _partitionsDirtied = false;

	// enum values for painting hints 
	private static final int START_OF_FIELD = -1;
	private static final int END_OF_FIELD = Integer.MAX_VALUE;

	protected static final int _emptyCaretWidth = 5;

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
	 * _rowOffset == 36
	 * GUI line 0 is the text "Hello World"
	 * _caretPosition == 257 + 12 + 28 + 5 == 302
	 * 
	 * 
     * Note: EOF (End Of File) is considered a char with a real length of 1
	 * 
	 */

	
	public FreeScrollingTextField(DocumentProvider hDoc, RowListener rLis){
		_fieldController = new TextFieldController();
		_hDoc = hDoc;
		_rLis = rLis;
		setEditable(true);
		initView();
	}
	
	public void changeDocumentProvider(DocumentProvider hDoc){
		_hDoc = hDoc;
		resetView();
	}
	
	public void resetView(){
		initView();
		_fieldController.realSelect(false, false);
		_rLis.onRowChange(0);
	}

	private void initView() {
		_XScrollOffset = 0;
		_rowOffset = 0;
		_charOffset = 0;
		_caretPosition = 0;
		_selectionAnchor = -1;
		_partitions = null;
		setPartitionsDirty(true);
		setDrawHints(START_OF_FIELD, END_OF_FIELD);
	}
	
	public void forceRepaint(){
		setPartitionsDirty(true);
		makeCaretVisible();
		invalidate();
	}
	
    //---------------------------------------------------------------------
    //------------------------- Layout methods ----------------------------
	
	/**
	 * This field will consume all space given to it by the layout manager
	 */
	protected void layout(int width, int height) {
		_maxVisibleHeight = height;
		_maxVisibleWidth = width;
		setExtent(width, height);
    }
    
	final protected int getNumVisibleWholeRows(){
		return _maxVisibleHeight / rowHeight();
	}

	final protected boolean partialLastRow(){
		return (_maxVisibleHeight % rowHeight()) > 0;
	}
	
	final protected int rowHeight(){
		return getFont().getHeight();
	}

    //---------------------------------------------------------------------
    //-------------------------- Paint methods ----------------------------
	
    protected void paint(Graphics graphics) { 
	    if (partitionsDirtied()){
	    	_partitions = TokenScanner.tokenize(_hDoc, _charOffset, totalVisibleChars());
	    	setPartitionsDirty(false);
	    }

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
    private void paintWithDrawHints(Graphics graphics){
		if (frameworkInitiatedPaint() &&
			  !ensureWholeFieldRedrawn(graphics)){
		    // relies on short-circuit evaluation
		   	return;
		}

    	resolvePaintBoundaries();
		 
    	//----------------------------------------------
    	// set up partition coloring settings
    	//----------------------------------------------
	    int currentChar = _hDoc.getStartCharOfRow(_beginPaintRow);
		int partitionIndex = 0;
	     
	    // There must be at least one partition to paint, even for an empty file,
	    // where the partition is the EOF sentinel character
		TextWarriorException.assert(!_partitions.isEmpty(),
		 	"No tokens to paint in TextWarrior.paint()");

		Pair nextPartition = (Pair) _partitions.elementAt(partitionIndex++);
		Pair currPartition;
		do{
			currPartition = nextPartition;
			if(partitionIndex < _partitions.size()){
				nextPartition = (Pair) _partitions.elementAt(partitionIndex++);
			}
			else{
				nextPartition = null;
			}
		}
		while(nextPartition != null &&
				nextPartition.getFirst() <= (currentChar - _charOffset));
	    int partColor = getPartitionColor(currPartition.getSecond());

		
    	//----------------------------------------------
    	// set up graphics settings
    	//----------------------------------------------
	    int paintX = 0;
	    int paintY = 0;
	    
	    int oldColor = graphics.getColor();
	    int oldBackgroundColor = graphics.getBackgroundColor();
	     
    	//----------------------------------------------
    	// start painting!
    	//----------------------------------------------
	    _hDoc.seekChar(currentChar);
        while (_beginPaintRow <= _endPaintRow &&
        		_hDoc.hasNext()){
        	// check if formatting changes are needed
        	if (reachedNextPartition(currentChar-_charOffset, nextPartition)){
    			currPartition = nextPartition;
        	    partColor = getPartitionColor(currPartition.getSecond());

    			if(partitionIndex < _partitions.size()){
    				nextPartition = (Pair) _partitions.elementAt(partitionIndex++);
    			}
    			else{
    				nextPartition = null;
    			}
        	}


        	char c = _hDoc.next();
        	if (paintX < _XScrollOffset + _maxVisibleWidth){
		    	int charColor = partColor;
		    	if (inSelectionRange(currentChar)){
		    		charColor = ColorScheme.selForegroundColor;
		    		drawCharBackground(graphics,
		    			c,
		    			paintX,
		    			paintY,
		    			ColorScheme.selBackgroundColor);
		    	}
		    	else if (atCaret(currentChar)){
		    		charColor = ColorScheme.caretForegroundColor;
		    		drawCharBackground(graphics,
		    			c,
		    			paintX,
		    			paintY,
		    			ColorScheme.caretBackgroundColor);
		    	}
		    	
		    	paintX += drawChar(graphics, c, paintX, paintY, charColor);
        	}
        	 
        	++currentChar;
        	if (c == Language.NEWLINE){
    	 		paintX = 0;
    	 		paintY += rowHeight();
    	 		++_beginPaintRow;
        	}
        } // end while

    	//----------------------------------------------
    	// restore graphics context
    	//----------------------------------------------
		graphics.setBackgroundColor(oldBackgroundColor);
		graphics.setColor(oldColor);
		clearDrawHints();
    }

    /**
     * Used exclusively by paint() when it is called by framework.
     * Ensures that all calls to paint() by the framework lead to
     * a repainting of the entire field, if the clippingRect given
     * does not start at the top left corner of the field
     * 
     * @param graphics
     * @return
     */
	private boolean ensureWholeFieldRedrawn(Graphics graphics) {
		 if (graphics.getTranslateX() != getContentLeft() ||
				 graphics.getTranslateY() != getContentTop()){
			 // subregion given; ignore and redraw entire field later
			 invalidate();
			 return false;
		 }

		 // entire region given; mark entire field to be painted
		 setDrawHints(START_OF_FIELD, END_OF_FIELD);
		 return true;
	}

    /**
     * Resolve START_OF_FIELD and END_OF_FIELD to actual row values.
     * Results are valid only after the field has been layout,
     * which is guaranteed to happen before paint() is invoked.
     */
	private void resolvePaintBoundaries() {
	     if (_beginPaintRow < _rowOffset || _beginPaintRow == START_OF_FIELD){
	    	 _beginPaintRow = _rowOffset;
		 }
	     
	     if (getNumVisibleWholeRows() == 0){
	    	 // field not layout yet; unable to resolve paint boundaries
	    	 _endPaintRow = 0;
	     }
	     else if (_endPaintRow == END_OF_FIELD ||
				 (_endPaintRow - _rowOffset) >= getNumVisibleWholeRows() ){
			 _endPaintRow = _rowOffset + getNumVisibleWholeRows() - 1;
			 if(partialLastRow()){
				 ++_endPaintRow;
			 }
		 }
	}
    
    private void drawCharBackground(Graphics g, char c, int paintX, int paintY,
    	int backgroundColor){
    	g.setBackgroundColor(backgroundColor);
 	
    	switch (c){
	 	case Language.NEWLINE: // fall-through
	 	case Language.EOF:
			g.clear(paintX, paintY, _emptyCaretWidth, rowHeight());
	 		break;
	 	case Language.TAB:
			g.clear(paintX, paintY, _tabLength*(getFont().getAdvance(' ')), rowHeight());
	 		break;
	 	default:
			g.clear(paintX, paintY, getFont().getAdvance(c), rowHeight());
	 		break;
    	}
    }
    
        
    private int drawChar(Graphics g, char c, int paintX, int paintY, int color){
    	 int advance = 0;
         g.setColor(color);
 	
    	 switch (c){
    	 	case Language.NEWLINE: // fall-through
    	 	case Language.EOF:
    			advance = _emptyCaretWidth;
    	 		break;
    	 	case Language.TAB:
    	 		advance = _tabLength * getFont().getAdvance(' ');
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
        		setPartitionsDirty(true);
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
		int caretRow = _hDoc.getRowIndex(_caretPosition);
int rowHeight =  getNumVisibleWholeRows();
rowHeight++;
		boolean scrolled = false;
    	if (caretRow < _rowOffset){
    		_rowOffset = caretRow;
		    _charOffset = _hDoc.seekLine(_rowOffset);
		    scrolled = true;
    	}
    	else if (getNumVisibleWholeRows() > 0 &&
    			caretRow >= _rowOffset + getNumVisibleWholeRows()){
		    _rowOffset = caretRow - getNumVisibleWholeRows() + 1;
		    _charOffset = _hDoc.seekLine(_rowOffset);
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
	 * If the selection is longer than a line, show the current caret
     * 
     * Preconditions: GUI row of char is scrolled on screen.
     * 
     * @return True if the area was scrolled vertically; false otherwise
     */
	private boolean makeCaretColumnVisible(){
		Pair visibleRange = getSelectionX_Extent();
		
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
	
	private Pair getSelectionX_Extent(){
        Pair X_extent;
        
		if (isSelecting() && isSingleLineSelection()){
			int beginX;
			int endX;
			if(_caretPosition < _selectionAnchor){
				beginX = getCharX_Extent(_caretPosition).getFirst();
				endX = getCharX_Extent(_selectionAnchor - 1).getSecond();
			}
			else{
				beginX = getCharX_Extent(_selectionAnchor).getFirst();
				endX = getCharX_Extent(_caretPosition).getSecond();
			}
			
			X_extent = new Pair(beginX, endX);
		}
		else{
			X_extent = getCharX_Extent(_caretPosition);
		}
		return X_extent;
	}
	
    /**
     * Calculates the x-coordinate extent of targetChar.
     * 
     */
    private Pair getCharX_Extent(int charOffset){
		int rowIndex = _hDoc.getRowIndex(charOffset);
 		TextWarriorException.assert(rowIndex != -1,
			"In getCharX_Extent: Invalid charOffset given");
	    int charCount = _hDoc.seekLine(rowIndex);
	    int scrollXBegin = 0;
	    int scrollXEnd = 0;

 		
	    while(charCount <= charOffset && _hDoc.hasNext()){
	       	 scrollXBegin = scrollXEnd;
	       	 char c = _hDoc.next();
	       	 switch (c){
	     	 	case Language.NEWLINE:
	     	 	case Language.EOF:
	     	 		scrollXEnd += _emptyCaretWidth;
	    	 		break;
	    	 	case Language.TAB:
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
     * Invalidate rows from _beginPaintRow to _endPaintRow
     */
    private void invalidateWithDrawHints() {
		int guiRowOffset;
		if (_beginPaintRow == START_OF_FIELD){
			guiRowOffset = 0;
		}
		else{
			guiRowOffset = _beginPaintRow - _rowOffset;
		}
		
    	int invalidateHeight;
    	if (_endPaintRow == END_OF_FIELD){
    		// if _maxVisibleHeight is not an integer multiple
    		// of row height, there will be some forgotten areas
    		// at the bottom of the screen. This calculation
    		// takes care of that case.
    		invalidateHeight = _maxVisibleHeight - (guiRowOffset*rowHeight());
    	}
    	else{
    		invalidateHeight = (_endPaintRow - _beginPaintRow + 1) * rowHeight();
    	}

    	int y = guiRowOffset * rowHeight();
    	super.invalidate(0, y,
    			_maxVisibleWidth, invalidateHeight);
    }
    

    //---------------------------------------------------------------------
    //------------------------- Event handlers ----------------------------
    
    /**
     * Sets draw hints and triggers redrawing
     * Precondition: caret is at the point of insertion
     */
    protected boolean keyChar(char c, int status, int time) {
    	// Superclass will handle BlackBerry cut, copy, paste and delete keystrokes
    	boolean handled = super.keyChar(c, status, time);
    	
    	// Handle user-defined keystrokes
		if (ShortcutKeys.isSwitchPanel(c, status)){
			// pass shortcut keystrokes upstream to manager
			return false;
        }
		if (ShortcutKeys.isTabHotkey(c, status)){
			c = Language.TAB;
        }
    	
    	if (!handled && TokenScanner.getLanguage().isPrintable(c)){
    		_fieldController.onPrintableChar(c);
    		handled = true;
    	}

    	return handled;
	}

    /**
     * Triggers redrawing
     */
    protected boolean navigationMovement(int dx, int dy, int status, int time){
    	if (shiftKeyDown(status) && !isSelecting() ){
    		// implement standard BlackBerry behaviour by entering selection mode
    		select(true);
        }
    	else{
	    	if (dy > 0){
	    		_fieldController.onMovementDown();
	    	}
	    	else if (dy < 0){
	    		_fieldController.onMovementUp();
	    	}
	    	
	    	if (dx > 0){
	    		_fieldController.onMovementRight();
	    	}
	    	else if (dx < 0){
	    		_fieldController.onMovementLeft();
	    	}
	
	    	//TODO consider moving to TextFieldController
	        makeCaretVisible();
	        invalidateWithDrawHints();
    	}
    	return true;
    }

	private boolean shiftKeyDown(int status){
		return (status & KeypadListener.STATUS_SHIFT) ==
    		KeypadListener.STATUS_SHIFT;
	}

	protected boolean navigationUnclick(int status, int time){
		return true; // consume all button unclicks
	}
	
	
    //---------------------------------------------------------------------
    //----------------------- Cut, copy, paste ----------------------------

    /**
     * Indirectly triggers redrawing through selectionDelete()
     * 
     * If called by the framework, redrawing is also triggered as a result of
	 * framework calling select(false)
     */
	public void selectionCut(Clipboard cb) throws IllegalStateException{
		_fieldController.realCopy(cb);
		_fieldController.realSelectionDelete(true);
	}

    /**
     * If called by the framework, redrawing is also triggered as a result of
	 * framework calling select(false)
     */
	public void selectionCopy(Clipboard cb) throws IllegalStateException{
		_fieldController.realCopy(cb);
	}

    /**
     * Triggers redrawing
     */
	public boolean paste(Clipboard cb){
		_fieldController.realPaste(cb.toString(), true);
		return true;
	}
	
	public void replace(String replacementText){
		_fieldController.realPaste(replacementText, false);
	}
	
    /**
     * Triggers redrawing.
     * 
     * If called by the framework, redrawing is also triggered as a result of
	 * framework calling select(false)
     */
	public void selectionDelete(){
		_fieldController.realSelectionDelete(true);
	}

	public boolean isSelectionCopyable(){
		return isSelecting();
	}
	
	public boolean isPasteable(){
		return true;
	}
	
	public boolean isSelectionDeleteable(){
		return isSelecting();
	}
	

    //---------------------------------------------------------------------
    //------------------------- Text Selection ----------------------------

	public boolean isSelectable(){
		return true;
	}
	
	public final boolean isSelecting(){
		return _fieldController.isSelecting();
	}
	
	/**
	 * Triggers redrawing if select state changed
	 */
	public void select(boolean mode){
		_fieldController.realSelect(mode, true);
	}

	/**
	 * Sets drawing hints
	 */
    public void selectAll(){
		_fieldController.realSelect(true, false);
    	_selectionAnchor = 0;
    	_caretPosition = _hDoc.docLength() - 1;
		_rLis.onRowChange(_hDoc.getRowIndex(_caretPosition));
		setSelectionDrawHints();
    }

	/**
	 * Sets drawing hints
	 */
	public void setSelectionRange(int beginPosition, int numChars){
		// set drawing hints to unhighlight previous selection or caret
		if(isSelecting()){
			setSelectionDrawHints();
		}
		else{
			int caretRow = _hDoc.getRowIndex(_caretPosition);
			setDrawHints(caretRow, caretRow);
		}

		_fieldController.realSelect(true, false);
		_selectionAnchor = beginPosition;
		_caretPosition = _selectionAnchor + numChars - 1;
		_rLis.onRowChange(_hDoc.getRowIndex(_caretPosition));
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
    public Pair getSelectionRange(){
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
    	
    	int onePastEOF =  _hDoc.docLength();
    	if (selectionEnd == onePastEOF){
    		//don't select EOF; rewind selectionEnd
    		--selectionEnd;
    	}
    	
    	return new Pair(selectionBegin, selectionEnd);
	}

    public int getSelectionAnchor(){
		return _selectionAnchor;
	}
    
    protected boolean inSelectionRange(int logicalCharOffset){
		if(_selectionAnchor < 0){
			return false;
		}
    	
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
		int caretRow = _hDoc.getRowIndex(_caretPosition);
		int anchorRow = (_caretPosition < _selectionAnchor) ?
				_hDoc.getRowIndex(_selectionAnchor-1) :
				_hDoc.getRowIndex(_selectionAnchor);

		return (caretRow == anchorRow && anchorRow != -1);
	}

	private void setSelectionDrawHints(){
		int startRow, endRow;
		if (_caretPosition < _selectionAnchor){
			startRow = _hDoc.getRowIndex(_caretPosition);
			endRow = _hDoc.getRowIndex(_selectionAnchor-1);
		}
		else{
			startRow = _hDoc.getRowIndex(_selectionAnchor);
			endRow = _hDoc.getRowIndex(_caretPosition);
		}

		setDrawHints(startRow, endRow);	
	}
	
    //---------------------------------------------------------------------
    //------------------------- Caret methods -----------------------------
    
    public void setCaretRow(int rowIndex){
    	int newRow = rowIndex;
    	int charOffset = _hDoc.getStartCharOfRow(newRow);
    	if(charOffset != -1){
    		_caretPosition = charOffset;
    	}
    	else{
    		// invalid rowIndex given
     		if (rowIndex < 0){
        		_caretPosition = 0;
        		newRow = 0;
     		}
     		else{
     			// set to last row
     			newRow = _hDoc.getRowIndex(_hDoc.docLength()-1);
     			_caretPosition = _hDoc.getStartCharOfRow(newRow);
     		}
    	}
		_rLis.onRowChange(newRow);
    }
    
    public int getCaretRow(){
    	return _hDoc.getRowIndex(_caretPosition);
    }
    
    public int getCaretPosition(){
    	return _caretPosition;
    }
    
    // Does not do check validity of i
	public void setCaretPosition(int i) {
		_caretPosition = i;
	}

    final protected boolean atCaret(int charOffset){
    	return (_caretPosition == charOffset);
    }

    /**
     * Preconditions: charOffset is within the text region displayed on screen
     * 
     * @return The GUI column number where charOffset appears on
     * 
     */
    protected int getColumn(int charOffset){
	    int row = _hDoc.getRowIndex(charOffset);
	    int firstCharOfRow = _hDoc.getStartCharOfRow(row);
 		return charOffset - firstCharOfRow;
    }

    final protected boolean caretOnFirstRowOfFile(){
    	int firstCharOfSecondRow = _hDoc.getStartCharOfRow(1);
    	
    	if (firstCharOfSecondRow > 0){
    		return (_caretPosition < firstCharOfSecondRow);
    	}
    	else{
    		// file only has one line
    		return true;
    	}
    }

    final protected boolean caretOnLastRowOfFile(){
	    int caretRow = _hDoc.getRowIndex(_caretPosition);

	    // if next row does not exist, we are at the last row
    	return (_hDoc.getStartCharOfRow(caretRow+1) == -1);
    }
    
    final protected boolean caretOnEOF(){
    	return (_caretPosition == (_hDoc.docLength()-1));
    }
    
    //---------------------------------------------------------------------
    //------------------------- Formatting methods ------------------------
	protected void setPartitionsDirty(boolean set) {
		_partitionsDirtied = set;
	}
	
	protected boolean partitionsDirtied() {
		return _partitionsDirtied;
	}
	
    private boolean reachedNextPartition(int charIndex, Pair partition){
    	return (partition == null) ? false : (charIndex == partition.getFirst());
    }

    final protected int getPartitionColor(int partitionType){
    	int color;
		switch(partitionType){
		 case TokenScanner.NORMAL:
		     color = ColorScheme.foregroundColor;
			 break;
		 case TokenScanner.KEYWORD:
		     color = ColorScheme.keywordColor;
			 break;
		 case TokenScanner.COMMENT:
		     color = ColorScheme.commentColor;
			 break;
		 case TokenScanner.CHAR_LITERAL:
		 case TokenScanner.STRING_LITERAL:
		     color = ColorScheme.literalColor;
			 break;
		 case TokenScanner.PREPROCESSOR:
		     color = ColorScheme.preprocessorColor;
			 break;
		 default:
			 TextWarriorException.assert(false,
			 	"Invalid token type in GenericTextField");
		     color = ColorScheme.foregroundColor;
			 break;
		}
		return color;
    }

    private final boolean frameworkInitiatedPaint(){
		// Use draw hints to indirectly determine if paint() was called by
    	// framework or programatically by this class (using an explicit call
    	// to invalidate()).
    	// When paint() is called programatically, the draw hints would have
    	// valid values. Therefore, check for invalid values here.
    	return (_beginPaintRow == END_OF_FIELD &&
		 _endPaintRow == START_OF_FIELD);
    }
    
    private final void clearDrawHints(){
		 _beginPaintRow = END_OF_FIELD;
		 _endPaintRow = START_OF_FIELD;
    }
    
    /**
     * Does not check validity of startRow and endRow
     * 
     * @param startRow
     * @param endRow
     */
    private void setDrawHints(int startRow, int endRow){
    	if(startRow == START_OF_FIELD || startRow < _beginPaintRow){
        	_beginPaintRow = startRow;
    	}
    	
		if(endRow > _endPaintRow){
    		_endPaintRow = endRow;
    	}
    }
    
    /**
     * Returns the total number of chars that appear from GUI row 0
     * to row (getMaxVisibleRows() - 1).
     * 
     * @return Total characters on screen
     */
    protected int totalVisibleChars(){
    	int charCount = 0;
    	int row = 0;
    	int lastRow = getNumVisibleWholeRows();
    	if(partialLastRow()){
    		++lastRow;
    	}

    	while (row < lastRow){
    		int charsOnRow = _hDoc.lineLength(row+_rowOffset);
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

	public void setTypefaceSize(int pixelSize) {
		FontFamily currentFontFamily = getFont().getFontFamily();
        setFont(currentFontFamily.getFont(Font.PLAIN, pixelSize, Ui.UNITS_px));
	}
	
    //---------------------------------------------------------------------
    //-------------------------- Focus methods ----------------------------
    
    public boolean isFocusable() { 
        return true; 
    }
    
	protected void drawFocus(Graphics graphics,boolean on){
		//do nothing
	}


	
	
	
	
	
    //*********************************************************************
    //************************** Control logic ****************************
    //*********************************************************************
	
	private class TextFieldController{
		protected boolean _isInSelectionMode = false;
		//TODO replace direct manipulation of outer class private member variables
		//	with getters and setters
		
	    //- TextFieldController -----------------------------------------------
	    //---------------------------- Key presses ----------------------------
		protected void onPrintableChar(char c) {
			// delete currently selected text, if any
			if (isSelecting()){
				setSelectionDrawHints(); // unhighlight current selection
				realSelectionDelete(false);
				realSelect(false, false);
			}
			
			if(c == Language.BACKSPACE){
				if (_caretPosition > 0){
					// Delete the char BEFORE the caret
					char deleted = _hDoc.getChar(_caretPosition-1);
					_hDoc.deleteAt(_caretPosition - 1);
					onMovementLeft();
					
					// mark rest of screen from caret for repainting
					setDrawHints(_hDoc.getRowIndex(_caretPosition), END_OF_FIELD);
					if (deleted == Language.NEWLINE){
			    		_rLis.onRowChange(_hDoc.getRowIndex(_caretPosition)-1);
					}
				}
			}
			else{
				if(c == Language.NEWLINE){
					// mark rest of screen from caret for repainting
					setDrawHints(_hDoc.getRowIndex(_caretPosition), END_OF_FIELD);
				}
				
				_hDoc.insertBefore(c, _caretPosition);
				onMovementRight();
			}
			
			setDirty(true);
			setPartitionsDirty(true);
			makeCaretVisible();
			invalidateWithDrawHints();
		}
		
	    /**
	     * Sets draw hints
	     */
	    protected void onMovementDown(){
	    	if (!caretOnLastRowOfFile()){
	    	    int currCaretRow = _hDoc.getRowIndex(_caretPosition);
	    		int newCaretRow = currCaretRow + 1;
	    		
	    		int currCaretColumn = getColumn(_caretPosition);
	    		int currCaretRowLength = _hDoc.lineLength(currCaretRow);
	    		int newCaretRowLength = _hDoc.lineLength(newCaretRow);
	    		
	    		if (currCaretColumn < newCaretRowLength){
		    		// Position at the same column as old row.
		    		_caretPosition += currCaretRowLength;
		    	}
		    	else{
		    		// Old column does not exist in the next row (next row is too short).
		    		// Position at end of next row instead.
		    		_caretPosition +=
		    			currCaretRowLength - currCaretColumn + newCaretRowLength - 1;
		    	}
	    		
	    		_rLis.onRowChange(newCaretRow);
	    		setDrawHints(currCaretRow, newCaretRow);
	    	}
	    }

	    /**
	     * Sets draw hints
	     */
	    protected void onMovementUp(){
	    	if (!caretOnFirstRowOfFile()){
	    	    int currCaretRow = _hDoc.getRowIndex(_caretPosition);
	    		int newCaretRow = currCaretRow - 1;
	    		
	    		int currCaretColumn = getColumn(_caretPosition);
	    		int newCaretRowLength = _hDoc.lineLength(newCaretRow);

		    	if (currCaretColumn < newCaretRowLength){
		    		// Position at the same column as old row.
		    		_caretPosition -= newCaretRowLength;
		    	}
		    	else{
		    		// Old column does not exist in the previous row
		    		// because previous row is too short.
		    		// Position at end of previous row instead.
		    		_caretPosition -= (currCaretColumn + 1);
		    	}

	    		_rLis.onRowChange(newCaretRow);
	    		setDrawHints(newCaretRow, currCaretRow);
	    	}
	    }

	    /**
	     * Sets draw hints
	     */
	    protected void onMovementRight(){
	    	if(!caretOnEOF()){
	    		int currCaretRow = _hDoc.getRowIndex(_caretPosition);
	    		++_caretPosition;
	    		int newCaretRow = _hDoc.getRowIndex(_caretPosition);
	    		setDrawHints(currCaretRow, newCaretRow);
	    		if(currCaretRow != newCaretRow){
	    			_rLis.onRowChange(newCaretRow);
	    		}
	    	}
	    }

	    /**
	     * Sets draw hints
	     */
	    protected void onMovementLeft(){
	    	if(_caretPosition > 0){
	    		int currCaretRow = _hDoc.getRowIndex(_caretPosition);
	    		--_caretPosition;
	    		int newCaretRow = _hDoc.getRowIndex(_caretPosition);
	    		setDrawHints(newCaretRow, currCaretRow);
	    		if(currCaretRow != newCaretRow){
	    			_rLis.onRowChange(newCaretRow);
	    		}
	    	}
	    }

	    //- TextFieldController -----------------------------------------------
	    //-------------------------- Selection mode ---------------------------
		protected final boolean isSelecting(){
			return _isInSelectionMode;
		}
		
		/**
		 * Change select mode. triggerRedraw parameter can indicate whether
		 * repainting should take place if select mode changes.
		 * 
		 * @param mode
		 * @param triggerRedraw If true, sets draw hints and triggers
		 * 						repainting when select state changes
		 */
		private void realSelect(boolean mode, boolean triggerRedraw){
			boolean changed = false;
			
			if(!_isInSelectionMode && mode){
				// selected the single character that the caret is on
				_selectionAnchor = _caretPosition;
				_isInSelectionMode = mode;
				changed = true;
			}
			else if (_isInSelectionMode && !mode){
				_selectionAnchor = -1;
				_isInSelectionMode = mode;
				changed = true;
			}

			if(triggerRedraw && changed){
				/* The framework will clear the entire field when the user chooses
				 * "Select" or "Cancel selection" from the main menu, hence
				 * the entire field has to be marked for repainting.
				 */
				setDrawHints(START_OF_FIELD, END_OF_FIELD);
				invalidateWithDrawHints();
			}
		}

	    //- TextFieldController -----------------------------------------------
	    //------------------------ Cut, copy, paste ---------------------------
		private void realCopy(Clipboard cb) {
			if(_isInSelectionMode){
				Pair range = getSelectionRange();
				int totalChars = range.getSecond() - range.getFirst();
				int copyPoint = range.getFirst();

				char[] contents = _hDoc.getChars(copyPoint, totalChars);
				cb.put(new String(contents));
			}
		}
		
		/**
		 * Sets partitioning hints
		 * 
		 * @param triggerRedraw If true, sets drawing hints and triggers 
		 * 						repainting if delete successful
		 */
		private void realSelectionDelete(boolean triggerRedraw){
			if(_isInSelectionMode){
				Pair range = getSelectionRange();
				int totalChars = range.getSecond() - range.getFirst();
				int deletionPoint = range.getFirst();

				_hDoc.deleteAt(deletionPoint, totalChars);
				_caretPosition = deletionPoint;

				setPartitionsDirty(true);
				setDirty(true);
				_rLis.onRowChange(_hDoc.getRowIndex(_caretPosition));

				if(triggerRedraw){
					/* The framework will clear the entire field when the user
					 * chooses "Cut" from the main menu, hence the entire field
					 * has to be marked for repainting.
					 */
					setDrawHints(START_OF_FIELD, END_OF_FIELD);
					invalidateWithDrawHints();
				}
			}
		}
		
		/**
		 * Sets partitioning hints
		 * 
		 * @param replacementText
		 * @param triggerRedraw If true, sets drawing hints and triggers
		 * 						repainting is paste was successful
		 */
		private void realPaste(String replacementText, boolean triggerRedraw){
			if(replacementText.length() > 0){
				if (_isInSelectionMode){
					setSelectionDrawHints(); // unhighlight selection
					realSelectionDelete(false);
					realSelect(false, false);
				}
				
				_hDoc.insertBefore(replacementText.toCharArray(), _caretPosition);

				_caretPosition += replacementText.length();
		
				setPartitionsDirty(true);
				setDirty(true);
				_rLis.onRowChange(_hDoc.getRowIndex(_caretPosition));
				
				if(triggerRedraw){
					/* The framework will clear the entire field when the user
					 * chooses "Paste" from the main menu, hence the entire field
					 * has to be marked for repainting.
					 */
					setDrawHints(START_OF_FIELD, END_OF_FIELD);
					makeCaretVisible();
					invalidateWithDrawHints();
				}
			}
		}//end realPaste
	}//end inner class

}
