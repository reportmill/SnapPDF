/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snappdf.write;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.gfx.*;
import snappdf.PDFWriter;

/**
 * Utility methods to write PDF for fill/stroke of paths with paints.
 */
public class SnapPaintPdfr {

    /**
     * Get rid of this.
     */
    public static void writeShapeStroke(Shape aShape, Stroke aStroke, Color aColor, PDFWriter aWriter)
    {
        writeDrawShapeWithPaintAndStroke(aWriter, aShape, aColor, aStroke);
    }

    /**
     * Writes a given shape stroke.
     */
    public static void writeDrawShapeWithPaintAndStroke(PDFWriter aWriter, Shape aShape, Paint aPaint, Stroke aStroke)
    {
        // Get PDF page and write stroke path
        PDFPageWriter pdfPage = aWriter.getPageWriter();
        pdfPage.writePath(aShape);

        // Get color
        Color color = aPaint instanceof Color ? (Color) aPaint : null;
        if (color == null) {
            System.err.println("SnapPaintPdfr.writeDrawShapeWithPaintAndStroke: Non-color not supported");
            color = Color.BLACK;
        }

        // Set stroke color and width
        pdfPage.setStrokeColor(color);
        pdfPage.setStrokeWidth(aStroke.getWidth());

        // Write dash array
        if (aStroke.getDashArray() != null && aStroke.getDashArray().length > 1)
            pdfPage.append('[').append(Stroke.getDashArrayString(aStroke.getDashArray(), " ")).append("] ")
                    .append(aStroke.getDashOffset()).appendln(" d");

        // Write stroke operator
        pdfPage.appendln("S");
    }

    /**
     * Writes PDF for a plain RMFill.
     */
    public static void writeFillShapeWithPaint(PDFWriter aWriter, Shape aShape, Paint aPaint)
    {
        // Handle GradientPaint
        //if (aPaint instanceof GradientPaint)
        //    writeGradientFill(aView, (GradientPaint)aFill, aWriter); else

        // aPaint ImagePaint
        if (aPaint instanceof ImagePaint)
            writeImagePaint(aWriter, (ImagePaint) aPaint, aShape, aShape.getBounds());

            // Handle color fill
        else if (aPaint instanceof Color) {
            writeShapeFill(aShape, (Color) aPaint, aWriter);
        }
    }

    /**
     * Writes PDF for a plain RMFill.
     */
    public static void writeShapeFill(Shape aShape, Color aColor, PDFWriter aWriter)
    {
        // Get shape path and PDF page and write path
        PDFPageWriter pdfPage = aWriter.getPageWriter();
        pdfPage.writePath(aShape);

        // Set fill color and write fill operator
        pdfPage.setFillColor(aColor);
        pdfPage.append('f');

        // If path winding rule odd, write odd fill operator
        //if(path.getWindingRule()==RMPath.WIND_EVEN_ODD) pdfPage.append('*');

        // End line
        pdfPage.appendln();
    }

    /**
     * Writes pdf for the path filled with a shading pattern defined by the RMGradientFill
     */
    /*public static void writeGradientFill(View aView, GradientPaint aFill, RMPDFWriter aWriter)
    {
        // Get View path and PDF page and write path
        Shape path = aView.getPath();
        PDFPage pdfPage = aWriter.getPDFPage();
        pdfPage.writePath(path);

        // Get the xref table so we can add objects to it
        PDFXTable xref = aWriter.getXRefTable();

        // Set the fill colorspace to the pattern colorspace (subspace is always rgb)
        pdfPage.appendln("/Pattern cs");

        // Create pdf functions that interpolate linearly between color stops
        int stopCount = aFill.getStopCount();
        ArrayList fns = new ArrayList(stopCount);
        Map function = null;
        String outerBounds = "", outerDomain = "", encode = "";
        for(int i=0; i<stopCount-1; ++i) {
            function = new Hashtable(5);
            Color c0 = aFill.getStopColor(i);
            Color c1 = aFill.getStopColor(i+1);
            double d0 = aFill.getStopOffset(i);
            double d1 = aFill.getStopOffset(i+1);
            function.put("FunctionType", "2");
            function.put("Domain", "[0 1]");
            function.put("N", "1");
            function.put("C0", c0);
            function.put("C1", c1);
            fns.add(function);

            // add endpoints to Domain & Bounds arrays of stitching function
            if(i==0)
                outerDomain += d0;
            else outerBounds += " " + d0;
            if(i==stopCount-2)
                outerDomain += " " + d1;

            // all input to sub-functions mapped to range 0-1
            encode += "0 1 ";
        }

        // If there are multiple stops, create a stitching function to combine all the functions
        if(stopCount>2) {
            function = new Hashtable(5);
            function.put("FunctionType", "3");
            function.put("Functions", fns);
            function.put("Domain", "[" + outerDomain + "]");
            function.put("Bounds", "[" + outerBounds + "]");
            function.put("Encode", "[" + encode + "]");
        }

        // Create a shading dictionary for the gradient
        Map shading = new Hashtable(4);
        boolean isRadial = aFill.isRadial();

        shading.put("ShadingType", isRadial? "3" : "2");  // radial or axial shading
        shading.put("ColorSpace", "/DeviceRGB");  // rgb colorspace
        shading.put("AntiAlias", "true");
        shading.put("Function", xref.addObject(function));

        // Get gradient paint and start/end
        GradientPaint gpnt = aFill.copyFor(aView.getBoundsInside());
        Point startPt = Point.get(gpnt.getStartX(), gpnt.getStartY()), endPt = Point.get(gpnt.getEndX(), gpnt.getEndY());

        // In pdf, coordinates of the gradient axis are defined in pattern space.  Pattern space is the same as the
        // page's coordinate system, and doesn't get affected by changes to the ctm. Since the RMGradient returns
        // points in the View's coordinate system, we have to transform them into pattern space (page space).
        View page = aView.getParent(PageView.class);
        Transform patternSpaceTransform = aView.getTransformToParent(page);
        patternSpaceTransform.transform(startPt);
        patternSpaceTransform.transform(endPt);

        // add in flip
        startPt.y = page.getFrameMaxY() - startPt.y;
        endPt.y = page.getFrameMaxY() - endPt.y;

        // Add the newly calculated endpoints to the shading dictionary
        List coords = new ArrayList(4); coords.add(startPt.getX()); coords.add(startPt.getY());
        if(isRadial) {
            coords.add(0d); // start radius = 0
            coords.add(coords.get(0)); coords.add(coords.get(1)); // end point is same as start point
            coords.add(endPt.getDistance(startPt)); // end radius is the distance between the start & end points
            shading.put("Extend", "[false true]"); // set radial shading to extend beyond end circle
        }
        else {
            coords.add(new Double(endPt.getX()));
            coords.add(new Double(endPt.getY()));
        }
        shading.put("Coords", coords);

        // Create a new pattern dictionary for the gradient
        Map pat = new Hashtable(10);
        pat.put("Type", "/Pattern");
        pat.put("PatternType", "2");
        pat.put("Shading", xref.addObject(shading)); // pat.put("Matrix", patternSpaceTransform);

        // Set the pattern for fills
        pdfPage.append('/').append(pdfPage.addPattern(pat)).appendln(" scn");

        // Write fill operator
        pdfPage.append('f');

        // If path winding rule odd, write odd fill operator
        //if(path.getWindingRule()==RMPath.WIND_EVEN_ODD) pdfPage.append('*');

        // Write trailing newline
        pdfPage.appendln();
    }*/

    /**
     * Writes given ImagePaint to a PDFWriter.
     */
    public static void writeImagePaint(PDFWriter aWriter, ImagePaint anImageFill, Shape aPath, Rect bounds)
    {
        // Get image (just return if missing or invalid) and name
        Image img = anImageFill.getImage();
        if (img == null) return;
        String iname = aWriter.getImageName(img);
        aWriter.addImage(img);

        // Get PDF page
        PDFPageWriter pdfPage = aWriter.getPageWriter();

        // Gsave
        pdfPage.gsave();

        // If path was provided, clip to it
        if (aPath != null) {
            pdfPage.writePath(aPath);
            pdfPage.appendln(" W n");
        }

        // If scaled, translate to View center, scale and return
        /*if(anImageFill.getScaleX()!=1 || anImageFill.getScaleY()!=1) {

            // Get View width and height
            double width = bounds.getWidth();
            double height = bounds.getHeight();

            // Get transform with translate to View center, scale, and translate back
            Transform t = new Transform(); t.translate(-width/2, -height/2);
            t.scale(anImageFill.getScaleX(), anImageFill.getScaleY());
            t.translate(width/2, height/2);

            // Apply transform
            pdfPage.transform(t);

            // Transform bounds to enclose rotated and scaled image space
            t.invert(); t.transform(bounds);

            // If not STYLE_TILE, scale enclosing bounds by image fill scale
            if(!anImageFill.isTiled()) {
                Transform t2 = new Transform(); t2.translate(-width/2, -height/2);
                t2.scale(anImageFill.getScaleX(), anImageFill.getScaleY());
                t2.translate(width/2, height/2);
                t2.transform(bounds);
            }
        }*/

        // If fill style tile, stamp image edge 2 edge, left 2 right, top 2 bottom (could use PDF tiling patterns instead)
        /*if(anImageFill.isTiled()) {

            // Get image width/height, which becomes scale of image coords (except PDF images write out scale of 1x1)
            double width = anImageFill.getImageWidth();
            double height = anImageFill.getImageHeight();

            // Get starting x and y
            double startX = bounds.x + anImageFill.getX(); while(startX>bounds.x) startX -= width;
            double startY = bounds.y + anImageFill.getY(); while(startY>bounds.y) startY -= height;

            // Iterate left to right over View width and top to bottom over View height
            for(double x=startX, xMax=bounds.getMaxX(); x<xMax; x+=width) {
                for(double y=startY, yMax=bounds.getMaxY(); y<yMax; y+=height) {

                    // Gsave, scale CTM, Do image and Grestore
                    pdfPage.gsave();
                    pdfPage.transform(width, 0, 0, -height, x, height + y);
                    pdfPage.appendln("/" + idata.getName() + " Do");
                    pdfPage.grestore();
                }
            }
        }*/

        // All other fillStyles just smack image in imageBounds
        //else {

        // Get image bounds width and height
        double width = bounds.width, height = bounds.height;

        // Apply CTM - image coords are flipped from page coords ( (0,0) at upper-left )
        pdfPage.writeTransform(width, 0, 0, -height, anImageFill.getX() + bounds.x, anImageFill.getY() + bounds.getMaxY());

        // Do image
        pdfPage.appendln("/" + iname + " Do");
        //}

        // Grestore
        pdfPage.grestore();

        // If image has alpha, declare output to be PDF-1.4
        //if(idata.hasAlpha() && idata.getSamplesPerPixel()==4) aWriter.setVersion(1.4f);
    }

}