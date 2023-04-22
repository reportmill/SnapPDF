/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snappdf.write;
import java.util.*;

import snap.geom.Rect;
import snap.geom.Shape;
import snap.gfx.*;
import snappdf.*;
import snap.util.*;

/**
 * All drawing happens in PDFPage object. Contents is a stream to which all the marking functions get appended.
 */
public class PDFPageWriter extends PDFWriterBase {

    // The pdf file this page is part of
    PDFFile _pfile;

    // The XRef table for file
    PDFXTable _xtable;

    // The master writer for this page writer
    PDFWriter _writer;

    // The pdf media box for this page
    Rect _mediaBox;

    // The pdf crop box for this page
    Rect _cropBox;

    // The graphics state stack
    PDFGStateStack _gstack = new PDFGStateStack();

    // List of pdf annotations for this page
    List<PDFAnnotation> _annotations;

    // List of pdf resources for this page 
    Map _resources;

    /**
     * Creates a PDF page for the page tree and pfile.
     */
    public PDFPageWriter(PDFFile aFile, PDFWriter aWriter)
    {
        // Cache PDF file and add this to pages tree and xref table
        _writer = aWriter;
        _pfile = aFile;
        _xtable = _pfile.getXRefTable();
        _pfile._pageTree.addPage(this);
        _xtable.addObject(this);

        // Create resources
        _resources = new Hashtable(4);
        _resources.put("Font", _xtable.getRefString(aWriter.getFonts()));
        _resources.put("XObject", _xtable.getRefString(aWriter.getImageRefs()));
        _resources.put("ProcSet", "[/PDF /Text /ImageC /ImageI /ImageB]");
        _xtable.addObject(_resources, true);
    }

    /**
     * Sets the media box for the page.
     */
    public void setMediaBox(Rect aRect)
    {
        _mediaBox = aRect != null ? aRect.clone() : null;
    }

    /**
     * Sets the crop box for the page.
     */
    public void setCropBox(Rect aRect)
    {
        _cropBox = aRect != null ? aRect.clone() : null;
    }

    /**
     * Sets the given color to be the current fill color.
     */
    public void setFillColor(Color aColor)
    {
        // If value already set, just return
        if (Objects.equals(aColor, _gstack.getFillColor())) return;

        // Set color in gstate and write it. Set opacity separate, since there is no set-rgba op
        _gstack.setFillColor(aColor);
        append(aColor.getRed()).append(' ').append(aColor.getGreen()).append(' ').append(aColor.getBlue()).appendln(" rg");
        setFillOpacity(aColor.getAlpha());
    }

    /**
     * Sets the given color to be the current stroke color.
     */
    public void setStrokeColor(Color aColor)
    {
        // If value already set, just return
        if (Objects.equals(aColor, _gstack.getStrokeColor())) return;

        // Set color in gstate and write it. Set opacity separate, since there is no set-rgba op
        _gstack.setStrokeColor(aColor);
        append(aColor.getRed()).append(' ').append(aColor.getGreen()).append(' ').append(aColor.getBlue()).appendln(" RG");
        setStrokeOpacity(aColor.getAlpha());
    }

    /**
     * Sets the opacity for all drawing.
     */
    public void setOpacity(double anOpacity)
    {
        // If value already set, just return
        if (anOpacity == _gstack.getShapeOpacity()) return;

        // Change global opacity causes change in both stroke/fill (clear vals in gstate to force change)
        _gstack.setShapeOpacity(anOpacity);
        double oso = _gstack.getStrokeOpacity();
        _gstack.setStrokeOpacity(-1);
        setStrokeOpacity(oso);
        double ofo = _gstack.getFillOpacity();
        _gstack.setFillOpacity(-1);
        setFillOpacity(ofo);
    }

    /**
     * Sets the opacity for fill operations. Stupidly, there's no setOpacity or setRGBAColor op, so we modify gstate
     * parameter dict directly using generic gstate operator gs, which takes a name of a gstate map in page's
     * ExtGState map (we have to add this silly little gstate map manually for each unique opacity).
     */
    protected void setFillOpacity(double aValue)
    {
        // If value already set, just return
        if (aValue == _gstack.getFillOpacity()) return;

        // Get unique name for gstate parameter dict for alpha value and add param dict for it, if needed
        double absOpacity = aValue * _gstack.getShapeOpacity();
        String name = "ca" + Math.round(absOpacity * 255);
        if (getExtGStateMap().get(name) == null)
            getExtGStateMap().put(name, MapUtils.newMap("ca", absOpacity));

        // Set alpha using this map (use BX & EX so this won't choke pre 1.4 readers) and update gstate value
        appendln("BX /" + name + " gs EX");
        _gstack.setFillOpacity(aValue);
    }

    /**
     * Sets the opacity for stroke operations.
     */
    protected void setStrokeOpacity(double aValue)
    {
        // If value already set, just return
        if (aValue == _gstack.getStrokeOpacity()) return;

        // Get unique name for gstate parameter dict for alpha value and add param dict for it, if needed
        double absOpacity = aValue * _gstack.getShapeOpacity();
        String name = "CA" + Math.round(absOpacity * 255);
        if (getExtGStateMap().get(name) == null)
            getExtGStateMap().put(name, MapUtils.newMap("CA", absOpacity));

        // Set alpha using this map (use BX & EX so this won't choke pre 1.4 readers) and update gstate value
        appendln("BX /" + name + " gs EX");
        _gstack.setStrokeOpacity(aValue);
    }

    /**
     * Sets the given line width to be the current line width.
     */
    public void setStrokeWidth(double aWidth)
    {
        // If value already set, just return
        if (aWidth == _gstack.getStrokeWidth()) return;

        // Set stroke width
        _gstack.setStrokeWidth(aWidth);
        append(aWidth).appendln(" w");
    }

    /**
     * Sets the line cap: 0=butt, 1=round, 2=square.
     */
    public void setLineCap(int aLineCap)
    {
        // If value already set, just return
        if (aLineCap == _gstack.getLineCap()) return;

        // Set line cap
        _gstack.setLineCap(aLineCap);
        append(aLineCap).appendln(" J");
    }

    /**
     * Sets the line join: 0=miter, 1=round, 2=bevel.
     */
    public void setLineJoin(int aLineJoin)
    {
        // If value already set, just return
        if (aLineJoin == _gstack.getLineJoin()) return;

        // Set line join
        _gstack.setLineJoin(aLineJoin);
        append(aLineJoin).appendln(" j");
    }

    /**
     * Sets the given font to be current font.
     */
    public void setFont(Font aFont)
    {
        PDFFontEntry fontEntry = _writer.getFontEntry(aFont, 0);
        append('/');
        append(fontEntry.getPDFName());
        append(' ');
        append(aFont.getSize());
        appendln(" Tf");
    }

    /**
     * Sets the char spacing.
     */
    public void setCharSpacing(double aSpacing)
    {
        append(aSpacing);
        appendln(" Tc");
    }

    /**
     * Write image.
     */
    public void writeClip(Shape aShape)
    {
        writePath(aShape);
        appendln("W n");
    }

    /**
     * Writes a draw string.
     */
    public void writeDrawString(String aStr, double aX, double aY)
    {
        // Apply CTM - image coords are flipped from page coords ( (0,0) at upper-left )
        gsave();
        writeTransform(1, 0, 0, -1, aX, aY);

        // Output PDF begin text operator
        appendln("BT");

        // Write start char
        append('(');

        // Iterate over run chars
        int len = aStr.length();
        for (int i = 0; i < len; i++) {
            char c = aStr.charAt(i);

            // If char is more than 256, complain
            if (c >= 256)
                System.err.println("PDFPageWriter.writeDrawString: Extended chars not supported yet");

            // Handle special chars for PDF string (might need to do backspace (\b) and form-feed (\f), too)
            if (c == '\t') {
                if (_writer.getIncludeNewlines()) append("\\t");
                continue;
            }
            if (c == '\n') {
                if (_writer.getIncludeNewlines()) append("\\n");
                continue;
            }
            if (c == '\r') {
                if (_writer.getIncludeNewlines()) append("\\r");
                continue;
            }
            if (c == '(' || c == ')' || c == '\\')
                append('\\');

            // Write the char
            append(c);
        }

        // Write end chars
        appendln(") Tj");

        // End Text
        appendln("ET");

        // Restore gstate
        grestore();
    }

    /**
     * Writes an image in given bounds.
     */
    public void writeImage(Image anImage, double aX, double aY)
    {
        writeDrawImage(anImage, aX, aY, anImage.getWidth(), anImage.getHeight());
    }

    /**
     * Writes an image in given bounds.
     */
    public void writeDrawImage(Image anImage, double x, double y, double width, double height)
    {
        // Get image and image bounds (just return if missing or invalid)
        if (anImage == null) return;

        // Add image
        _writer.addImage(anImage);

        // Gsave
        gsave();

        // Apply CTM - image coords are flipped from page coords ( (0,0) at upper-left )
        writeTransform(width, 0, 0, -height, x, y + height);

        // Do image
        appendln("/" + _writer.getImageName(anImage) + " Do");

        // Grestore
        grestore();
    }

    /**
     * Write image.
     */
    public void writeDrawImage(Image anImage, Rect destBounds, Shape aClipShape)
    {
        // Get image data (just return if missing) and image name and add image
        String iname = _writer.getImageName(anImage);
        _writer.addImage(anImage);

        // Get PDF page and gsave
        gsave();

        // Apply clip if provided
        if (aClipShape != null)
            writeClip(aClipShape);

        // Apply CTM - image coords are flipped from page coords ( (0,0) at upper-left )
        double width = destBounds.getWidth();
        double height = destBounds.getHeight();
        writeTransform(width, 0, 0, -height, destBounds.x, destBounds.getMaxY());

        // Do image and grestore
        appendln("/" + iname + " Do");
        grestore();
    }

    /**
     * Saves the current graphics state of the writer.
     */
    public void gsave()
    {
        _gstack.gsave();
        appendln("q");
    }

    /**
     * Restores the last graphics state of the writer.
     */
    public void grestore()
    {
        _gstack.grestore();
        appendln("Q");
    }

    /**
     * Returns the number of annotations.
     */
    public int getAnnotationCount()
    {
        return _annotations != null ? _annotations.size() : 0;
    }

    /**
     * Returns the specific page annotation at the given index.
     */
    public PDFAnnotation getAnnotation(int anIndex)
    {
        return _annotations.get(anIndex);
    }

    /**
     * Adds an annotation to the page.
     */
    public void addAnnotation(PDFAnnotation anAnnot)
    {
        // Add annotation to list
        if (_annotations == null) _annotations = new ArrayList();
        _annotations.add(anAnnot);

        // If Widget, add to Catalog.AcroForm dict, otherwise, just add to XRef Table
        if (anAnnot instanceof PDFAnnotation.Widget)
            _writer.addAcroFormField((PDFAnnotation.Widget) anAnnot);
        else _xtable.addObject(anAnnot);
    }

    /**
     * Returns the resource dict for this page.
     */
    public Map getResourcesDict()
    {
        return _resources;
    }

    /**
     * Returns the named resource dict for this page.
     */
    public Map getResourceMap(String aResourceName)
    {
        // Get map from resources (create and add it, if absent)
        Map map = (Map) _resources.get(aResourceName);
        if (map == null)
            _resources.put(aResourceName, map = new Hashtable());
        return map;
    }

    /**
     * Returns the ExtGState dict for this page.
     */
    public Map getExtGStateMap()
    {
        return getResourceMap("ExtGState");
    }

    /**
     * Adds a new colorspace to the resource dict and returns the name by which it's referred.
     */
    public String addColorspace(Object cspace)
    {
        // Get colorspace dictionary from resources (create and add it, if absent)
        Map map = getResourceMap("ColorSpace");
        String ref = _xtable.addObject(cspace);
        String name;

        // Only add colorspace once per page
        if (map.containsValue(ref))
            name = (String) MapUtils.getKey(map, cspace);

            // Create Colorspace name (eg., /Cs1, /Cs2, etc.) and add to the colorspace map
        else {
            name = "Cs" + (map.size() + 1);
            map.put(name, ref);
        }

        // Return name
        return name;
    }

    /**
     * Adds a new pattern to the resource dict and returns the name by which it's referred.
     */
    public String addPattern(Object aPattern)
    {
        // Get colorspace dictionary from resources (create and add it, if absent)
        Map map = getResourceMap("Pattern");
        String ref = _xtable.addObject(aPattern);
        String name;

        // Only add pattern once per page
        if (map.containsValue(ref))
            name = (String) MapUtils.getKey(map, aPattern);

            // Get pattern name (eg., /P1, P2, etc.) and add to pattern dict (PDF version neeeds to be 1.3)
        else {
            name = "P" + (map.size() + 1);
            map.put(name, ref);
        }

        // Return name
        return name;
    }

    /**
     * Resolves page references for page annotations.
     */
    public void resolvePageReferences(PDFPageTree pages)
    {
        for (int i = 0, iMax = getAnnotationCount(); i < iMax; i++)
            getAnnotation(i).resolvePageReferences(pages, _xtable, this);
    }

    /**
     * Creates a stream for page contents bytes.
     */
    protected PDFStream createStream()
    {
        // Get bytes and clear buffer
        byte bytes[] = toByteArray();
        _source.reset();

        // See if we need to compress
        boolean compressed = false;
        if (_writer.getCompress()) {
            byte bytes2[] = _writer.getBytesEncoded(bytes, 0, bytes.length);
            if (bytes2.length < bytes.length) {
                bytes = bytes2;
                compressed = true;
            }
        }

        // Create stream for bytes and if compressed, add filter
        PDFStream stream = new PDFStream(bytes, null);
        if (compressed)
            stream.addFilter("/FlateDecode");

        // Return stream
        return stream;
    }

    /**
     * Writes the page contents to the pdf buffer.
     */
    public void writePDF(PDFWriter aWriter)
    {
        // Write page basic info
        aWriter.append("<< /Type /Page /Parent ").appendln(_xtable.getRefString(_pfile.getPagesTree()));

        // Add stream if it's there instead of contents
        PDFStream stream = createStream();
        if (stream != null)
            aWriter.append("/Contents ").appendln(_xtable.addObject(stream, true));

        // Write page media box
        if (!_mediaBox.isEmpty())
            aWriter.append("/MediaBox ").append(_mediaBox).appendln();

        // Write page crop box
        if (_cropBox != null && !_cropBox.isEmpty())
            aWriter.append("/CropBox ").append(_cropBox).appendln();

        // Write page resources
        aWriter.append("/Resources ").appendln(_xtable.getRefString(_resources));

        // Write page annotations: Create new list of xrefs and write
        if (_annotations != null) {
            aWriter.append("/Annots ");
            List axrefs = new ArrayList();
            for (PDFAnnotation a : _annotations) axrefs.add(_xtable.getRefString(a));
            aWriter.writeXRefEntry(axrefs);
        }

        // Finish page
        aWriter.append(">>");
    }

}