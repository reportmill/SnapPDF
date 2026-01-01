package snappdf.write;
import snap.gfx.*;
import snap.text.*;
import snappdf.PDFWriter;

import java.util.List;
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
    public static void writeText(PDFWriter aWriter, TextLayout textLayout)
    {
        // If text doesn't render all text in bounds, add clip
        //if(layout.endIndex()<layout.length()) { aWriter.print("0 0 "); aWriter.print(textModel.getWidth());
        //    aWriter.print(' '); aWriter.print(textModel.getHeight()); aWriter.println(" re W n"); }

        // Get page writer
        PDFPageWriter pwriter = aWriter.getPageWriter();

        // Flip coordinate system since pdf font transforms are flipped
        pwriter.gsave();
        pwriter.append("1 0 0 -1 0 ");
        pwriter.append(textLayout.getHeight());
        pwriter.appendln(" cm");

        // Output PDF begin text operator
        pwriter.appendln("BT");

        // Iterate over lines and write
        TextRun lastRun = null;
        for (TextLine line : textLayout.getLines()) {

            // If line below text, bail
            if (line.getY() > textLayout.getHeight())
                break;

            // Iterate over runs and write
            TextRun[] runs = line.getRuns();
            for (TextRun run : runs) {
                writeRun(aWriter, textLayout, line, run, lastRun);
                lastRun = run;
            }
        }

        // End Text
        pwriter.appendln("ET");

        // Restore unflipped transform
        pwriter.grestore();

        // If any underlining in text, add underlining ops
        if (textLayout.isUnderlined()) {

            // Get underline runs
            List<TextRun> underlineRuns = textLayout.getUnderlineRuns(null);

            // Iterate over underline runs
            for (TextRun run : underlineRuns) {

                // Set stroke and stroke width
                TextStyle style = run.getTextStyle();
                TextLine line = run.getLine();
                pwriter.setStrokeColor(style.getColor());
                pwriter.setStrokeWidth(line.getUnderlineStroke());

                // Get line end points
                double x0 = line.getTextX() + run.getX();
                double y0 = line.getTextBaseline() - line.getUnderlineY();
                double x1 = line.getTextX() + run.getMaxX();
                if (run.getEndCharIndex() == line.getEndCharIndex())
                    x1 -= run.getTrailingWhitespaceWidth();
                pwriter.moveTo(x0, y0);
                pwriter.lineTo(x1, y0);
                pwriter.appendln("S");
            }
        }
    }

    /**
     * Writes the given text run to pdf.
     */
    public static void writeRun(PDFWriter aWriter, TextLayout textModel, TextLine textLine, TextRun textRun, TextRun lastRun)
    {
        // Get pdf page
        PDFPageWriter pPage = aWriter.getPageWriter();
        TextStyle style = textRun.getTextStyle();
        TextStyle lastStyle = lastRun != null ? lastRun.getTextStyle() : null;

        // If colorChanged, have writer setFillColor
        if (lastStyle == null || !lastStyle.getColor().equals(style.getColor()))
            pPage.setFillColor(textRun.getColor());

        // Get last x & y
        double lastX = lastRun == null ? 0 : lastRun.getLine().getTextX() + lastRun.getX();
        double lastY = lastRun == null ? textModel.getHeight() : lastRun.getLine().getTextBaseline();

        // Set the current text point
        double runX = textLine.getTextX() + textRun.getX() - lastX;
        double runY = lastY - textLine.getTextBaseline(); // Flip y coordinate
        pPage.append(runX).append(' ').append(runY).appendln(" Td");

        // Get current run font, whether FontChanged and current font entry (base font entry for font, if font has changed)
        Font font = style.getFont();
        boolean fontChanged = lastStyle == null || !lastStyle.getFont().equals(style.getFont());
        PDFFontEntry fontEntry = fontChanged ? aWriter.getFontEntry(font, 0) : aWriter.getFontEntry();

        // If char spacing has changed, set charSpace
        if (style.getCharSpacing() != (lastStyle == null ? 0 : lastStyle.getCharSpacing())) {
            pPage.append(style.getCharSpacing());
            pPage.appendln(" Tc");
        }

        // If run outline has changed, configure text rendering mode
        if (!Objects.equals(style.getBorder(), lastStyle == null ? null : lastStyle.getBorder())) {
            Border border = style.getBorder();
            if (border == null)
                pPage.appendln("0 Tr");
            else {
                pPage.setStrokeColor(border.getColor());
                pPage.setStrokeWidth(border.getWidth());
                if (textRun.getColor().getAlpha() > 0) {
                    pPage.setFillColor(style.getColor());
                    pPage.appendln("2 Tr");
                }
                else pPage.appendln("1 Tr");
            }
        }

        // Get length - just return if zero
        int length = textRun.length();
        if (length == 0) return;

        // Iterate over run chars
        for (int i = 0; i < length; i++) {

            // Get char and whether in ascii range
            char c = textRun.charAt(i);
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
        if (textLine.isHyphenated() && textRun == textLine.getLastRun()) pPage.append('-');

        // End last text show block
        pPage.appendln(") Tj");
    }

}