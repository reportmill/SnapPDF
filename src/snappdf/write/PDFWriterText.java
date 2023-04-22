package snappdf.write;
import snap.gfx.*;
import snap.text.TextBox;
import snap.text.TextBoxLine;
import snap.text.TextBoxRun;
import snap.text.TextStyle;
import snappdf.PDFWriter;
import java.util.Objects;

/**
 * PDFWriter utility methods for writing text. This would be a simple matter of using the PDF set-font and show-text
 * operators, except that we need to embed PDF type3 fonts (really char paths) for all chars printed in non-standard
 * fonts. We do this by tracking used chars in the PDFText.FontEntry class. Used chars in the ASCII range (0-255)
 * make up a base font "Font0", while chars beyond 255 get written out as separate PDF fonts for each block of 256
 * ("Font0.1", "Font0.2", etc.).
 */
public class PDFWriterText {

    /**
     * Writes the given text run.
     */
    public static void writeText(PDFWriter aWriter, TextBox aTextBox)
    {
        // If textbox doesn't render all text in bounds, add clip
        //if(layout.endIndex()<layout.length()) { aWriter.print("0 0 "); aWriter.print(aTextBox.getWidth());
        //    aWriter.print(' '); aWriter.print(aTextBox.getHeight()); aWriter.println(" re W n"); }

        // Get page writer
        PDFPageWriter pwriter = aWriter.getPageWriter();

        // Flip coordinate system since pdf font transforms are flipped
        pwriter.gsave();
        pwriter.append("1 0 0 -1 0 ");
        pwriter.append(aTextBox.getHeight());
        pwriter.appendln(" cm");

        // Output PDF begin text operator
        pwriter.appendln("BT");

        // Iterate over lines and write
        TextBoxRun lastRun = null;
        for (TextBoxLine line : aTextBox.getLines()) {
            if (line.getY() > aTextBox.getHeight()) break;   // If line below text, bail
            lastRun = writeLine(aWriter, aTextBox, line, lastRun);
        }

        // End Text
        pwriter.appendln("ET");

        // Restore unflipped transform
        pwriter.grestore();

        // If any underlining in TextBox, add underlining ops
        if (aTextBox.isUnderlined()) for (TextBoxRun run : aTextBox.getUnderlineRuns(null)) {

            // Set stroke and stroke width
            TextStyle style = run.getStyle();
            TextBoxLine line = run.getLine();
            pwriter.setStrokeColor(style.getColor());
            pwriter.setStrokeWidth(line.getUnderlineStroke());

            // Get line end points
            double x0 = run.getX(), y0 = line.getBaseline() - line.getUnderlineY();
            double x1 = run.getMaxX();
            if (run.getEnd() == line.getEndCharIndex()) x1 = line.getX() + line.getWidthNoWhiteSpace();
            pwriter.moveTo(x0, y0);
            pwriter.lineTo(x1, y0);
            pwriter.appendln("S");
        }
    }

    /**
     * Writes the given TextBoxLine to pdf.
     */
    public static TextBoxRun writeLine(PDFWriter aWriter, TextBox aTextBox, TextBoxLine aLine, TextBoxRun aLastRun)
    {
        // Iterate over line runs and writeRun()
        TextBoxRun lastRun = aLastRun;
        for (TextBoxRun run : aLine.getRuns()) {
            writeRun(aWriter, aTextBox, aLine, run, lastRun);
            lastRun = run;
        }

        // Return last run
        return lastRun;
    }

    /**
     * Writes the given TextBoxRun to pdf.
     */
    public static void writeRun(PDFWriter aWriter, TextBox aText, TextBoxLine aLine, TextBoxRun aRun, TextBoxRun aLastRun)
    {
        // Get pdf page
        PDFPageWriter pPage = aWriter.getPageWriter();
        TextStyle style = aRun.getStyle();
        TextStyle lastStyle = aLastRun != null ? aLastRun.getStyle() : null;

        // If colorChanged, have writer setFillColor
        if (lastStyle == null || !lastStyle.getColor().equals(style.getColor()))
            pPage.setFillColor(aRun.getColor());

        // Get last x & y
        double lastX = aLastRun == null ? 0 : aLastRun.getX();
        double lastY = aLastRun == null ? aText.getHeight() : aLastRun.getLine().getBaseline();

        // Set the current text point
        double runX = aRun.getX() - lastX;
        double runY = lastY - aLine.getBaseline(); // Flip y coordinate
        pPage.append(runX).append(' ').append(runY).appendln(" Td");

        // Get current run font, whether FontChanged and current font entry (base font entry for font, if font has changed)
        Font font = style.getFont();
        boolean fontChanged = lastStyle == null || !lastStyle.getFont().equals(style.getFont());
        PDFFontEntry fontEntry = fontChanged ? aWriter.getFontEntry(font, 0) : aWriter.getFontEntry();

        // If char spacing has changed, set charSpace
        if (style.getCharSpacing() != (aLastRun == null ? 0 : lastStyle.getCharSpacing())) {
            pPage.append(style.getCharSpacing());
            pPage.appendln(" Tc");
        }

        // If run outline has changed, configure text rendering mode
        if (!Objects.equals(style.getBorder(), aLastRun == null ? null : lastStyle.getBorder())) {
            Border border = style.getBorder();
            if (border == null)
                pPage.appendln("0 Tr");
            else {
                pPage.setStrokeColor(border.getColor());
                pPage.setStrokeWidth(border.getWidth());
                if (aRun.getColor().getAlpha() > 0) {
                    pPage.setFillColor(style.getColor());
                    pPage.appendln("2 Tr");
                }
                else pPage.appendln("1 Tr");
            }
        }

        // Get length - just return if zero
        int length = aRun.length();
        if (length == 0) return;

        // Iterate over run chars
        for (int i = 0; i < length; i++) {

            // Get char and whether in ascii range
            char c = aRun.charAt(i);
            boolean inAsciiRange = c < 256;

            // If char is less than 256, just mark it present in fontEntry chars
            if (inAsciiRange) {
                fontEntry._chars[c] = true;
                if (fontEntry.getCharSet() != 0) {
                    fontChanged = true;
                    fontEntry = aWriter.getFontEntry(font, 0);
                }
            }

            // If char beyond 255, replace c with its index in fontEntry uchars array (add it if needed)
            else {

                // Get index of char - if not found, add it
                int charIndex = fontEntry._uchars.indexOf(c);
                if (charIndex < 0) {
                    charIndex = fontEntry._uchars.size();
                    fontEntry._uchars.add(c);
                }

                // If char set changed, reset font entry
                int charSetIndex = charIndex / 256 + 1;
                if (fontEntry.getCharSet() != charSetIndex) {
                    fontChanged = true;
                    fontEntry = aWriter.getFontEntry(font, charSetIndex);
                }

                // Replace char with char index
                c = (char) (charIndex % 256);
            }

            // If font changed, end current text show block, set new font, and start new text show block
            if (fontChanged) {
                if (i > 0) pPage.appendln(") Tj");
                pPage.append('/');
                pPage.append(fontEntry.getPDFName());
                pPage.append(' ');
                pPage.append(font.getSize());
                pPage.appendln(" Tf");
                pPage.append('(');
                aWriter.setFontEntry(fontEntry);
                fontChanged = false;
            }

            // If first char, open paren
            else if (i == 0)
                pPage.append('(');

            // Handle special chars for PDF string (might need to do backspace (\b) and form-feed (\f), too)
            if (c == '\t') {
                if (!inAsciiRange || aWriter.getIncludeNewlines())
                    pPage.append("\\t");
                continue;
            }
            if (c == '\n') {
                if (!inAsciiRange || aWriter.getIncludeNewlines())
                    pPage.append("\\n");
                continue;
            }
            if (c == '\r') {
                if (!inAsciiRange || aWriter.getIncludeNewlines())
                    pPage.append("\\r");
                continue;
            }
            if (c == '\b') {
                if (!inAsciiRange)
                    pPage.append("\\b");
                continue;
            }
            if (c == '\f') {
                if (!inAsciiRange)
                    pPage.append("\\f");
                continue;
            }
            if (c == '(' || c == ')' || c == '\\')
                pPage.append('\\');

            // Write the char
            pPage.append(c);
        }

        // If run is hyphenated, add hyphen
        if (aLine.isHyphenated() && aRun == aLine.getRunLast()) pPage.append('-');

        // End last text show block
        pPage.appendln(") Tj");
    }

}