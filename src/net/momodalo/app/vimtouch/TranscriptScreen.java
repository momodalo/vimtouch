package net.momodalo.app.vimtouch;

import android.util.Log;
import java.util.Arrays;

import android.graphics.Canvas;

/**
 * A TranscriptScreen is a screen that remembers data that's been scrolled. The
 * old data is stored in a ring buffer to minimize the amount of copying that
 * needs to be done. The transcript does its own drawing, to avoid having to
 * expose its internal data structures.
 */
public class TranscriptScreen implements Screen {
    private static final String TAG = "TranscriptScreen";

    /**
     * The width of the transcript, in characters. Fixed at initialization.
     */
    private int mColumns;

    /**
     * The total number of rows in the transcript and the screen. Fixed at
     * initialization.
     */
    private int mTotalRows;

    /**
     * Which row is currently the topmost line of the transcript. Used to
     * implement a circular buffer.
     */
    private int mHead;

    /**
     * The number of rows in the screen.
     */
    private int mScreenRows;

    /**
     * The data for both the screen and the transcript. The first mScreenRows *
     * mLineWidth characters are the screen, the rest are the transcript.
     * The low byte encodes the ASCII character, the high byte encodes the
     * foreground and background colors, plus underline and bold.
     */
    private UnicodeTranscript mData;

    /**
     * Create a transcript screen.
     *
     * @param columns the width of the screen in characters.
     * @param totalRows the height of the entire text area, in rows of text.
     * @param screenRows the height of just the screen, not including the
     *        transcript that holds lines that have scrolled off the top of the
     *        screen.
     */
    public TranscriptScreen(int columns, int totalRows, int screenRows,
            int foreColor, int backColor) {
        init(columns, totalRows, screenRows, foreColor, backColor);
    }
    
    private void init(int columns, int totalRows, int screenRows, int foreColor, int backColor) {
        mColumns = columns;
        mTotalRows = totalRows;
        mHead = 0;
        mScreenRows = screenRows;
        int totalSize = columns * totalRows;
        mData = new UnicodeTranscript(columns, totalRows, screenRows, foreColor, backColor);
        mData.blockSet(0, 0, mColumns, mScreenRows, ' ', foreColor, backColor);
    }

    public void finish() {
        mData = null;
    }

    public void setLineWrap(int row) {
        mData.setLineWrap(row);
    }

    /**
     * Store byte b into the screen at location (x, y)
     *
     * @param x X coordinate (also known as column)
     * @param y Y coordinate (also known as row)
     * @param b ASCII character to store
     * @param foreColor the foreground color
     * @param backColor the background color
     */
    public void set(int x, int y, byte b, int foreColor, int backColor) {
        mData.setChar(x, y, b, foreColor, backColor);
    }

    /**
     * Scroll the screen down one line. To scroll the whole screen of a 24 line
     * screen, the arguments would be (0, 24).
     *
     * @param topMargin First line that is scrolled.
     * @param bottomMargin One line after the last line that is scrolled.
     */
    public void scroll(int topMargin, int bottomMargin, int foreColor,
            int backColor) {
        mData.scroll(topMargin, bottomMargin);
    }

    /**
     * Block copy characters from one position in the screen to another. The two
     * positions can overlap. All characters of the source and destination must
     * be within the bounds of the screen, or else an InvalidParemeterException
     * will be thrown.
     *
     * @param sx source X coordinate
     * @param sy source Y coordinate
     * @param w width
     * @param h height
     * @param dx destination X coordinate
     * @param dy destination Y coordinate
     */
    public void blockCopy(int sx, int sy, int w, int h, int dx, int dy) {
        mData.blockCopy(sx, sy, w, h, dx, dy);
    }

    /**
     * Block set characters. All characters must be within the bounds of the
     * screen, or else and InvalidParemeterException will be thrown. Typically
     * this is called with a "val" argument of 32 to clear a block of
     * characters.
     *
     * @param sx source X
     * @param sy source Y
     * @param w width
     * @param h height
     * @param val value to set.
     */
    public void blockSet(int sx, int sy, int w, int h, int val,
            int foreColor, int backColor) {
        mData.blockSet(sx, sy, w, h, val, foreColor, backColor);
    }

    public char[] getLine(int row) {
        return mData.getLine(row);
    }

    /**
     * Draw a row of text. Out-of-bounds rows are blank, not errors.
     *
     * @param row The row of text to draw.
     * @param canvas The canvas to draw to.
     * @param x The x coordinate origin of the drawing
     * @param y The y coordinate origin of the drawing
     * @param renderer The renderer to use to draw the text
     * @param cx the cursor X coordinate, -1 means don't draw it
     */
    public final void drawText(int row, Canvas canvas, float x, float y,
            TextRenderer renderer, int cx, int selx1, int selx2, String imeText) {
        char[] line;
        byte[] color;
        try {
            line = mData.getLine(row);
            color = mData.getLineColor(row);
        } catch (IllegalArgumentException e) {
            // Out-of-bounds rows are blank.
            return;
        } catch (NullPointerException e) {
            // Attempt to draw on a finished transcript
            // XXX Figure out why this happens on Honeycomb
            return;
        }
        int defaultForeColor = mData.getDefaultForeColor();
        int defaultBackColor = mData.getDefaultBackColor();

        if (line == null) {
            // Line is blank.
            if (selx1 != selx2) {
                // We need to draw a selection
                char[] blank = new char[selx2-selx1];
                Arrays.fill(blank, ' ');
                renderer.drawTextRun(canvas, x, y, selx1, selx2-selx1,
                                blank, 0, 1, true,
                                defaultForeColor, defaultBackColor);
            } else if (cx != -1) {
                // We need to draw the cursor
                renderer.drawTextRun(canvas, x, y, cx, 1,
                                " ".toCharArray(), 0, 1, true,
                                defaultForeColor, defaultBackColor);
            }

            return;
        }

        int columns = mColumns;
        int lastForeColor = 0;
        int lastBackColor = 0;
        int runWidth = 0;
        int lastRunStart = -1;
        int lastRunStartIndex = -1;
        boolean forceFlushRun = false;
        char cHigh = 0;
        final int CURSOR_MASK = 0x10000;
        int column = 0;
        int index = 0;
        while (column < columns) {
            int foreColor, backColor;
            if (color != null) {
                foreColor = (color[column] >> 4) & 0xf;
                backColor = color[column] & 0xf;
            } else {
                foreColor = defaultForeColor;
                backColor = defaultBackColor;
            }
            int width;
            if (Character.isHighSurrogate(line[index])) {
                cHigh = line[index++];
                continue;
            } else if (Character.isLowSurrogate(line[index])) {
                width = UnicodeTranscript.charWidth(cHigh, line[index]);
            } else {
                width = UnicodeTranscript.charWidth(line[index]);
            }
            if (cx == column || (column >= selx1 && column <= selx2)) {
                // Set cursor background color:
                backColor |= CURSOR_MASK;
            }
            if (foreColor != lastForeColor || backColor != lastBackColor || (width > 0 && forceFlushRun)) {
                if (lastRunStart >= 0) {
                    renderer.drawTextRun(canvas, x, y, lastRunStart, runWidth,
                            line,
                            lastRunStartIndex, index - lastRunStartIndex,
                            (lastBackColor & CURSOR_MASK) != 0,
                            lastForeColor, lastBackColor);
                }
                lastForeColor = foreColor;
                lastBackColor = backColor;
                runWidth = 0;
                lastRunStart = column;
                lastRunStartIndex = index;
                forceFlushRun = false;
            }
            runWidth += width;
            column += width;
            index++;
            if (width > 1) {
                /* We cannot draw two or more East Asian wide characters in the
                   same run, because we need to make each wide character take
                   up two columns, which may not match the font's idea of the
                   character width */
                forceFlushRun = true;
            }
        }
        if (lastRunStart >= 0) {
            renderer.drawTextRun(canvas, x, y, lastRunStart, runWidth,
                    line,
                    lastRunStartIndex, index - lastRunStartIndex,
                    (lastBackColor & CURSOR_MASK) != 0,
                    lastForeColor, lastBackColor);
        }

        if (cx >= 0 && imeText.length() > 0) {
            int imeLength = Math.min(columns, imeText.length());
            int imeOffset = imeText.length() - imeLength;
            int imePosition = Math.min(cx, columns - imeLength);
            renderer.drawTextRun(canvas, x, y, imePosition, imeLength, imeText.toCharArray(),
                    imeOffset, imeLength, true, 0x0f, 0x00);
        }
    }

    /**
     * Get the count of active rows.
     *
     * @return the count of active rows.
     */
    public int getActiveRows() {
        return mData.getActiveRows();
    }

    /**
     * Get the count of active transcript rows.
     *
     * @return the count of active transcript rows.
     */
    public int getActiveTranscriptRows() {
        return mData.getActiveTranscriptRows();
    }

    public String getTranscriptText() {
        return internalGetTranscriptText(null, 0, -mData.getActiveTranscriptRows(), mColumns, mScreenRows);
    }

    private String internalGetTranscriptText(StringBuilder colors, int selX1, int selY1, int selX2, int selY2) {
        StringBuilder builder = new StringBuilder();
        UnicodeTranscript data = mData;
        int columns = mColumns;
        char[] line;
        byte[] rowColorBuffer = null;
        if (selY1 < -data.getActiveTranscriptRows()) {
            selY1 = -data.getActiveTranscriptRows();
        }
        if (selY2 >= mScreenRows) {
            selY2 = mScreenRows - 1;
        }
        for (int row = selY1; row <= selY2; row++) {
            int x1 = 0;
            int x2;
            if ( row == selY1 ) {
                x1 = selX1;
            }
            if ( row == selY2 ) {
                x2 = selX2 + 1;
                if (x2 > columns) {
                    x2 = columns;
                }
            } else {
                x2 = columns;
            }
            line = data.getLine(row, x1, x2);
            if (colors != null) {
                rowColorBuffer = data.getLineColor(row, x1, x2);
            }
            if (line == null) {
                if (!data.getLineWrap(row) && row < selY2 && row < mScreenRows - 1) {
                    builder.append('\n');
                    if (colors != null) {
                        colors.append((char) 0);
                    }
                }
                continue;
            }
            int lastPrintingChar = -1;
            int length = line.length;
            int i;
            for (i = 0; i < length; i++) {
                if (line[i] == 0) {
                    break;
                } else if (line[i] != ' ') {
                    lastPrintingChar = i;
                }
            }
            if (data.getLineWrap(row) && lastPrintingChar > -1 && x2 == columns) {
                // If the line was wrapped, we shouldn't lose trailing space
                lastPrintingChar = i - 1;
            }
            builder.append(line, 0, lastPrintingChar + 1);
            if (colors != null) {
                int column = 0;
                for (int j = 0; j < lastPrintingChar + 1; ++j) {
                    colors.append((char) rowColorBuffer[column]);
                    if (Character.isHighSurrogate(line[j])) {
                        column += UnicodeTranscript.charWidth(Character.toCodePoint(line[j], line[j+1]));
                        ++j;
                    } else {
                        column += UnicodeTranscript.charWidth(line[j]);
                    }
                }
            }
            if (!data.getLineWrap(row) && row < selY2 && row < mScreenRows - 1) {
                builder.append('\n');
                if (colors != null) {
                    colors.append((char) 0);
                }
            }
        }
        return builder.toString();
    }

    public void resize(int columns, int rows, int foreColor, int backColor) {
        init(columns, mTotalRows, rows, foreColor, backColor);
    }
}
