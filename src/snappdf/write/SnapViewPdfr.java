/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snappdf.write;
import snap.geom.Shape;
import snap.gfx.*;
import snappdf.PDFWriter;
import snap.view.*;

/**
 * This base class to write PDF for View subclasses.
 */
public class SnapViewPdfr<T extends View> {

    // Shared SnapViewPdfr
    static SnapViewPdfr _viewPdfr = new SnapViewPdfr();

    /**
     * Writes a given View hierarchy to a PDF file (recursively).
     */
    public void writePDF(T aView, PDFWriter aWriter)
    {
        // Write view
        writeViewBefore(aView, aWriter);

        // If view has effect, forward to it
        if (aView.getEffect() != null) SnapEffectPdfr.writeViewEffect(aView, aWriter);

            // Otherwise, do basic writeViewAll
        else writeViewAll(aView, aWriter);

        // Write View after children
        writeViewAfter(aView, aWriter);
    }

    /**
     * Writes a given View hierarchy to a PDF file (recursively).
     */
    protected void writeViewBefore(T aView, PDFWriter aWriter)
    {
        // Get page
        PDFPageWriter pdfPage = aWriter.getPageWriter();

        // Save the graphics transform
        pdfPage.gsave();

        // If not rotated/scaled, write simple translation matrix
        if (aView.isLocalToParentSimple())
            pdfPage.append("1 0 0 1 ").append(aView.getX()).append(' ').append(aView.getY()).appendln(" cm");

            // If rotated/scaled, write full transform
        else pdfPage.writeTransform(aView.getLocalToParent());
    }

    /**
     * Writes the View and then the View's children.
     */
    protected void writeViewAll(T aView, PDFWriter aWriter)
    {
        // Write View fills
        writeView(aView, aWriter);

        // Write View children
        writeViewChildren(aView, aWriter);
    }

    /**
     * Writes a given View hierarchy to a PDF file (recursively).
     */
    protected void writeView(T aView, PDFWriter aWriter)
    {
        // Get pdf page
        PDFPageWriter pdfPage = aWriter.getPageWriter();

        // Set View opacity
        pdfPage.setOpacity(aView.getOpacityAll());

        // Clip to bounds???
        //pageBuffer.print(aView.getBoundsInside()); pageBuffer.println(" re W n"));

        // If fill set, write pdf
        if (aView.getFill() != null)
            writeViewFill(aView, aWriter);

        // If border set, write pdf
        if (aView.getBorder() != null)
            writeViewStroke(aView, aWriter);
    }

    /**
     * Writes a given View hierarchy to a PDF file (recursively).
     */
    protected void writeViewChildren(View aView, PDFWriter aWriter)
    {
        // Write children
        ParentView pview = aView instanceof ParentView ? (ParentView) aView : null;
        if (pview == null) return;
        for (int i = 0, iMax = pview.getChildCount(); i < iMax; i++) {
            View child = pview.getChild(i);
            if (child.isVisible())
                getPdfr(child).writePDF(child, aWriter);
        }
    }

    /**
     * Writes a given View hierarchy to a PDF file (recursively).
     */
    protected void writeViewAfter(T aView, PDFWriter aWriter)
    {
        // Get pdf page
        PDFPageWriter pwriter = aWriter.getPageWriter();

        // Restore graphics state
        pwriter.grestore();

        // Add link, if it's there (What happens with rotated or skewed Views?)
    /*if(aView.getURL() != null) {
        Rect frame = aView.getBoundsInside(); aView.convertRectToView(frame, null);
        frame.setY(aView.getPageShape().getHeight() - frame.getMaxY());
        PDFAnnotation link = new PDFAnnotation.Link(frame, aView.getURL());
        pwriter.addAnnotation(link);
    }*/
    }

    /**
     * Writes a given View stroke.
     */
    protected void writeViewStroke(View aView, PDFWriter aWriter)
    {
        Shape shape = aView.getBoundsShape();
        Border border = aView.getBorder();
        Color strokeColor = border.getColor();
        double strokeWidth = border.getWidth();
        SnapPaintPdfr.writeDrawShapeWithPaintAndStroke(aWriter, shape, strokeColor, Stroke.getStroke(strokeWidth));
    }

    /**
     * Writes a given View fill.
     */
    protected void writeViewFill(View aView, PDFWriter aWriter)
    {
        Paint paint = aView.getFill();
        Shape shape = aView.getBoundsShape();
        SnapPaintPdfr.writeFillShapeWithPaint(aWriter, shape, paint);
    }

    /**
     * Returns the View pdfr for a View.
     */
    public static SnapViewPdfr getPdfr(View aView)
    {
        if (aView instanceof TextView) return SnapViewPdfrs._textViewPdfr;
        if (aView instanceof ImageView) return SnapViewPdfrs._imgViewPdfr;
        if (aView instanceof PageView) return SnapViewPdfrs._pageViewPdfr;
        return _viewPdfr;
    }

}