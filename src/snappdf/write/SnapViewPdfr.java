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
    private static final SnapViewPdfr<View> _viewPdfr = new SnapViewPdfr<>();

    /**
     * Writes a given View hierarchy to a PDF file (recursively).
     */
    public void writePDF(T aView, PDFWriter aWriter)
    {
        PDFPageWriter pageWriter = aWriter.getPageWriter();

        // Save the graphics state
        pageWriter.gsave();

        // Write transform
        if (aView.isLocalToParentSimple())
            pageWriter.append("1 0 0 1 ").append(aView.getX()).append(' ').append(aView.getY()).appendln(" cm");
        else pageWriter.writeTransform(aView.getLocalToParent());

        // Set View opacity
        double opacity = aView.getOpacity();
        if (opacity != 1) {
            double oldOpacity = pageWriter.getOpacity();
            pageWriter.setOpacity(opacity * oldOpacity);
        }

        // If view has effect, forward to it
        if (aView.getEffect() != null)
            SnapEffectPdfr.writeViewEffect(aView, aWriter);

        // Otherwise, do basic writeViewAll
        else writeViewAll(aView, aWriter);

        // Restore graphics state
        pageWriter.grestore();
    }

    /**
     * Writes the View and then the View's children.
     */
    protected void writeViewAll(T aView, PDFWriter aWriter)
    {
        // Write View fills
        writeView(aView, aWriter);

        // Write View children
        if (aView instanceof ParentView parentView)
            writeViewChildren(parentView, aWriter);
    }

    /**
     * Writes a given View hierarchy to a PDF file (recursively).
     */
    protected void writeView(T aView, PDFWriter aWriter)
    {
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
    protected void writeViewChildren(ParentView parentView, PDFWriter aWriter)
    {
        for (View child : parentView.getChildren()) {
            if (child.isVisible())
                getPdfr(child).writePDF(child, aWriter);
        }
    }

    /**
     * Writes a given View stroke.
     */
    private static void writeViewStroke(View aView, PDFWriter aWriter)
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
    private static void writeViewFill(View aView, PDFWriter aWriter)
    {
        Paint paint = aView.getFill();
        Shape shape = aView.getBoundsShape();
        SnapPaintPdfr.writeFillShapeWithPaint(aWriter, shape, paint);
    }

    /**
     * Returns the View pdfr for a View.
     */
    public static SnapViewPdfr<View> getPdfr(View aView)
    {
        if (aView instanceof TextView) return SnapViewPdfrs._textViewPdfr;
        if (aView instanceof ImageView) return SnapViewPdfrs._imgViewPdfr;
        if (aView instanceof PageView) return SnapViewPdfrs._pageViewPdfr;
        return _viewPdfr;
    }

}