package net.momodalo.app.vimtouch;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputMethodManager;

public class VimInputConnection extends BaseInputConnection
{
    private boolean DEBUG = false;
    protected static final String LOGTAG = "VimInputConnection";

    private TermView mView;
    private boolean mBatchMode;

    public VimInputConnection(TermView view) {
        super(view, true);
        mView = view;
        initEditable(Exec.getCurrentLine(0));
    }
    
    @Override
    public boolean beginBatchEdit() {
        mBatchMode = true;
        return true;
    }

    @Override
    public boolean endBatchEdit() {
        mBatchMode = false;
        return true;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        mView.dispatchKeyEvent(event);
        mView.lateCheckInserted();
        return true;
    }

    private void syncEditable() {
        Editable editable = getEditable();

        if(!editable.toString().equals(Exec.getCurrentLine(0))) {
            setEditable(Exec.getCurrentLine(0));
        }

        int end = Selection.getSelectionEnd(editable);
        int col = Exec.getCursorCol()>0?Exec.getCurrentLine(Exec.getCursorCol()).length():0;

        if(col != end){
            setSelection(col,col);
        }
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        if(DEBUG) Log.e(LOGTAG, "setComposingText " + text);
        if(!Exec.isInsertMode()){
            resetIME();
            return true;
        }

        Editable editable = getEditable();

        syncEditable();

        int start = getComposingSpanStart(editable);
        int end = getComposingSpanEnd(editable);
        if (start < 0 || end < 0) {
            int col = Exec.getCursorCol()>0?Exec.getCurrentLine(Exec.getCursorCol()).length():0;
            setSelection(col,col);
            //start = Selection.getSelectionStart(editable);
            //end = Selection.getSelectionEnd(editable);
            start = col;
            end = col;
        }
        if (end < start) {
            int temp = end;
            end = start;
            start = temp;
        }
        super.setComposingText(text, newCursorPosition);
        Exec.lineReplace(editable.toString());
        end = Selection.getSelectionEnd(editable);
        int wide = end>0?editable.subSequence(0,end-1).toString().getBytes().length:0;
        Exec.setCursorCol(wide+1);
        Exec.updateScreen();
        return true;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        if(DEBUG) Log.e(LOGTAG, "commitText " + text);
        if(!Exec.isInsertMode()) {
            resetIME();
            return true;
        }
        setComposingText(text, newCursorPosition);
        finishComposingText();
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int leftLength, int rightLength) {
        if(DEBUG) Log.e(LOGTAG, "deleteSurroundingText " + leftLength +" "+ rightLength);
        if(!Exec.isInsertMode()){
            resetIME();
            return true;
        }
        Editable editable = getEditable();

        syncEditable();

        boolean res = super.deleteSurroundingText(leftLength, rightLength);

        Exec.lineReplace(editable.toString());
        int end = Selection.getSelectionEnd(editable);
        int wide = end>0?editable.subSequence(0,end).toString().getBytes().length:0;
        Exec.setCursorCol(wide+1);
        Exec.updateScreen();

        return res;
    }

    public void setEditable(String contents) {
        Editable editable = getEditable();
        editable.removeSpan(this);
        editable.replace(0, editable.length(), contents);
        editable.setSpan(this, 0, contents.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        Selection.setSelection(editable, contents.length());
    }

    public void initEditable(String contents) {
        Editable editable = getEditable();
        editable.replace(0, editable.length(), contents);
        editable.setSpan(this, 0, contents.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        Selection.setSelection(editable, contents.length());
    }


    @Override
    public boolean setSelection(int start, int end) {
        boolean result = super.setSelection(start, end);
        return result;
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        boolean result = super.setComposingRegion(start, end);
        return result;
    }

    private ExtractedTextRequest mUpdateRequest;
    private final ExtractedText mUpdateExtract = new ExtractedText();
    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest req, int flags) {
        if(DEBUG) Log.e(LOGTAG, "getExtractedText");
        if(!Exec.isInsertMode()) {
            resetIME();
            return null;
        }
        if (req == null)
            return null;

        final Editable content = getEditable();
        if (content == null)
            return null;

        if ((flags & GET_EXTRACTED_TEXT_MONITOR) != 0)
            mUpdateRequest = req;

        syncEditable();

        ExtractedText extract = new ExtractedText();
        extract.flags = 0;
        extract.partialStartOffset = -1;
        extract.partialEndOffset = -1;

        extract.selectionStart = Selection.getSelectionStart(content);
        extract.selectionEnd = Selection.getSelectionEnd(content);
        extract.startOffset = 0;

        try {
            extract.text = content.toString();
        } catch (IndexOutOfBoundsException iob) {
            Log.d(LOGTAG,
                  "IndexOutOfBoundsException thrown from getExtractedText(). start: "
                  + Selection.getSelectionStart(content)
                  + " end: " + Selection.getSelectionEnd(content));
            return null;
        }
        if(DEBUG) Log.e(LOGTAG, "getExtractedText result " + extract);
        return extract;
    }

    public void resetIME() {
        Context context = mView.getContext();
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.restartInput(mView);
    }

    public void notifyTextChange() {
        String text = Exec.getCurrentLine(0);
        if (!mBatchMode) {
            if (!text.contentEquals(getEditable())) {
                setEditable(text);
            }
        }

        Editable content = getEditable();

        Context context = mView.getContext();
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);

        int newEnd = Exec.getCursorCol()>0?Exec.getCurrentLine(Exec.getCursorCol()).length():0;
        int end = Selection.getSelectionEnd(content);

        if(newEnd != end){
            setSelection( newEnd, newEnd);
        }
        if (mUpdateRequest == null){
            resetIME();
            return;
        }

        mUpdateExtract.flags = 0;

        mUpdateExtract.partialStartOffset = 0;
        mUpdateExtract.partialEndOffset = 1;

        // Faster to not query for selection
        mUpdateExtract.selectionStart = newEnd;
        mUpdateExtract.selectionEnd = newEnd;

        mUpdateExtract.text = text;
        mUpdateExtract.startOffset = 0;

        imm.updateExtractedText(mView, mUpdateRequest.token, mUpdateExtract);
   }

    public CharSequence getTextBeforeCursor(int length, int flags) {
        if(DEBUG) Log.e(LOGTAG, "getTextBeforeCursor " + length);
        if(!Exec.isInsertMode()){
            resetIME();
            return null;
        }
        syncEditable();

        int col = Exec.getCursorCol();
        if(col == 0) return "";
        String line = Exec.getCurrentLine(col);
        if(line.length() == 0) return "";
        if(DEBUG) Log.e(LOGTAG, "getTextBeforeCursor result " + line);

        if(length > line.length()) return line;
        else return line.subSequence(line.length() - length, line.length());
    }

    public CharSequence getTextAfterCursor(int length, int flags) {
        if(DEBUG) Log.e(LOGTAG, "getTextAfterCursor " + length);
        if(!Exec.isInsertMode()){
            resetIME();
            return null;
        }
        syncEditable();

        String line = Exec.getCurrentLine(0);
        if(line.length() == 0) return "";

        String before = Exec.getCurrentLine(Exec.getCursorCol());

        String after = line.substring(before.length());
        if(DEBUG) Log.e(LOGTAG, "getTextAfterCursor result " + after);
        if(length > after.length()) return after;
        return after.subSequence(0 , length);

    }
}

