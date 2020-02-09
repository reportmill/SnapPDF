package snappdf;
import snap.gfx.*;
import snappdf.write.PDFFontEntry;
import snappdf.write.PDFPageWriter;

/**
 * A painter that writes all drawing to PDF.
 */
public class PDPainter extends PainterImpl {

    // The PDF writer
    private PDFWriter _writer;

    /** Sets the current font. */
    public void setFont(Font aFont)
    {
        // Do normal version
        super.setFont(aFont);

        // Write set font
        PDFFontEntry fontEntry = _writer.getFontEntry(aFont, 0);
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.append('/');
        pdfPage.append(fontEntry.getPDFName());
        pdfPage.append(' ');
        pdfPage.append(aFont.getSize());
        pdfPage.appendln(" Tf");
    }

    /** Sets the current paint. */
    public void setPaint(Paint aPaint)
    {
        // Do normal version
        super.setPaint(aPaint);

        // Handle GradientPaint
        //if(aPaint instanceof GradientPaint) writeGradientFill(aView, (GradientPaint)aFill, aWriter); else

        // Handle ImagePaint fill
        //if(aPaint instanceof ImagePaint)
        //    SnapPaintPdfr.writeImagePaint(aWriter, (ImagePaint)fill, aView.getBoundsShape(), aView.getBoundsLocal());

        // Handle color fill
        if(aPaint instanceof Color) { Color color = (Color)aPaint;
            PDFPageWriter pdfPage = _writer.getPageWriter();
            pdfPage.setFillColor(color);
        }
    }

    /** Sets the current stroke. */
    public void setStroke(Stroke aStroke)
    {
        // Do normal version
        super.setStroke(aStroke);

    }

    /** Sets the opacity. */
    public void setOpacity(double aValue)
    {
        // Do normal version
        super.setOpacity(aValue);

        // Write opacity
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.setOpacity(aValue);
    }

    /** Stroke the given shape. */
    public void draw(Shape aShape)
    {
        // Do normal version
        super.draw(aShape);

        // Get PDF page, write path and stroke operator
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.writePath(aShape);
        pdfPage.appendln("S");
    }

    /** Fill the given shape. */
    public void fill(Shape aShape)
    {
        // Do normal version
        super.fill(aShape);

        // Get PDF page, write path and fill operator
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.writePath(aShape);
        pdfPage.append('f');
    }

    /** Draw image with transform. */
    public void drawImage(Image anImg, Transform aTrans)
    {
        // Do normal version
        super.drawImage(anImg, aTrans);

    }

    /** Draw image in rect. */
    public void drawImage(Image img, double sx, double sy, double sw, double sh, double dx, double dy, double dw, double dh)
    {
        // Do normal version
        super.drawImage(img, sx, sy, sw, sh, dx, dy, dw, dh);

    }

    /** Draw string at location with char spacing. */
    public void drawString(String aStr, double aX, double aY, double cs)
    {
        // Do normal version
        super.drawString(aStr, aX, aY, cs);

    }

    /**
     * Transform by transform.
     */
    public void setTransform(Transform aTrans)
    {
        // Do normal version
        super.setTransform(aTrans);

        System.err.println("PDPainter.setTransform: Not implemented");
    }

    /**
     * Transform by transform.
     */
    public void transform(Transform aTrans)
    {
        // Do normal version
        super.transform(aTrans);

        // If not rotated/scaled, write simple translation matrix
        PDFPageWriter pdfPage = _writer.getPageWriter();
        if(aTrans.isSimple())
            pdfPage.append("1 0 0 1 ").append(aTrans.getX()).append(' ').append(aTrans.getY()).appendln(" cm");

            // If rotated/scaled, write full transform
        else pdfPage.writeTransform(aTrans);
    }

    /**
     * Clip by shape.
     */
    public void clip(Shape aShape)
    {
        // Do normal version
        super.clip(aShape);

    }

    /**
     * Saves the graphics state.
     */
    public void save()
    {
        // Do normal version
        super.save();

        // Save the graphics transform
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.gsave();
    }

    /**
     * Restores the graphics state.
     */
    public void restore()
    {
        // Do normal version
        super.restore();

        // Restore the graphics transform
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.grestore();
    }
}
