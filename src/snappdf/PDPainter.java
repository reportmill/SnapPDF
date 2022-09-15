package snappdf;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.geom.Transform;
import snap.gfx.*;
import snappdf.write.PDFPageWriter;
import snappdf.write.SnapPaintPdfr;

/**
 * A painter that writes all drawing to PDF.
 */
public class PDPainter extends PainterImpl {

    // The PDF writer
    private PDFWriter _writer;

    /**
     * Constructor for PDFWriter.
     */
    public PDPainter(PDFWriter aWriter)
    {
        _writer = aWriter;
    }

    /**
     * Constructor for page size.
     */
    public PDPainter(double aW, double aH)
    {
        _writer = new PDFWriter();
        _writer.initWriter();
        _writer.setPageSize(aW, aH);
        _writer.addPage();

        // Clip to given doc
        super.clipRect(0, 0, aW, aH);
    }

    /**
     * Returns the writer.
     */
    public PDFWriter getWriter()
    {
        return _writer;
    }

    /**
     * Returns a PDF byte array for a given RMDocument.
     */
    public byte[] getBytesPDF()
    {
        _writer.finishWriter();
        return _writer.getBytes();
    }

    /**
     * Sets the current font.
     */
    public void setFont(Font aFont)
    {
        // Do normal version
        super.setFont(aFont);

        // Write set font
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.setFont(aFont);
    }

    /**
     * Sets the current paint.
     */
    public void setPaint(Paint aPaint)
    {
        // Do normal version
        super.setPaint(aPaint);

        // Write paint
        PDFPageWriter pdfPage = _writer.getPageWriter();
        if (aPaint instanceof Color) {
            Color color = (Color) aPaint;
            pdfPage.setFillColor(color);
            pdfPage.setStrokeColor(color);
        }

        else System.err.println("PDPainter.setPaint: Paint type not implemented " + aPaint.getClass().getSimpleName());
    }

    /**
     * Sets the current stroke.
     */
    public void setStroke(Stroke aStroke)
    {
        // Do normal version
        super.setStroke(aStroke);

        // Set stroke
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.setStrokeWidth(aStroke.getWidth());
    }

    /**
     * Sets the opacity.
     */
    public void setOpacity(double aValue)
    {
        // Do normal version
        super.setOpacity(aValue);

        // Write opacity
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.setOpacity(aValue);
    }

    /**
     * Stroke the given shape.
     */
    public void draw(Shape aShape)
    {
        // Do normal version
        super.draw(aShape);

        // Get current paint and fill shape
        Paint paint = getPaint();
        Stroke stroke = getStroke();
        SnapPaintPdfr.writeDrawShapeWithPaintAndStroke(_writer, aShape, paint, stroke);
    }

    /**
     * Fill the given shape.
     */
    public void fill(Shape aShape)
    {
        // Do normal version
        super.fill(aShape);

        // Get current paint and fill shape
        Paint paint = getPaint();
        SnapPaintPdfr.writeFillShapeWithPaint(_writer, aShape, paint);
    }

    /**
     * Draw image with transform.
     */
    public void drawImage(Image anImg, Transform aTrans)
    {
        // Do normal version
        super.drawImage(anImg, aTrans);

        // If rotated, complain
        if (aTrans.isRotated()) {
            System.err.println("PDPainter.drawImage: Not implemented for rotated dest bounds");
        }

        // Get dest bounds
        Rect srcBnds = new Rect(0, 0, anImg.getWidth(), anImg.getHeight());
        Rect dstBnds = srcBnds.copyFor(aTrans).getBounds();
        drawImage(anImg, srcBnds.x, srcBnds.y, srcBnds.width, srcBnds.height,
                dstBnds.x, dstBnds.y, dstBnds.width, dstBnds.height);
    }

    /**
     * Draw image in rect.
     */
    public void drawImage(Image img, double sx, double sy, double sw, double sh, double dx, double dy, double dw, double dh)
    {
        // Do normal version
        super.drawImage(img, sx, sy, sw, sh, dx, dy, dw, dh);

        // Get page writer, image and image bounds (just return if missing or invalid)
        PDFPageWriter pwriter = _writer.getPageWriter();

        // If source bounds not image, complain
        if (sx != 0 || sy != 0 || sw != img.getWidth() || sh != img.getHeight()) {
            System.err.println("PDPainter.drawImage: Not implemented for custom source bounds");
        }

        // Get dest bounds
        Rect dstBnds = new Rect(dx, dy, dw, dh);
        pwriter.writeDrawImage(img, dstBnds, null);
    }

    /**
     * Draw string at location with char spacing.
     */
    public void drawString(String aStr, double aX, double aY, double cs)
    {
        // Do normal version
        super.drawString(aStr, aX, aY, cs);

        // DrawString
        PDFPageWriter pwriter = _writer.getPageWriter();
        pwriter.writeDrawString(aStr, aX, aY);
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
        if (aTrans.isSimple())
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

        // Write clip
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.writeClip(aShape);
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
