/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snappdf.write;
import snap.geom.Rect;

import java.util.*;

import snappdf.PDFXTable;
import snap.util.StringUtils;

/**
 * This class represents a PDF annotation (like a hyperlink).
 */
public class PDFAnnotation {

    // Annotation map
    protected Map _map = new Hashtable();

    // Highlight modes
    public static final char HighlightNone = 'N';
    public static final char HighlightInvert = 'I';
    public static final char HighlightOutline = 'O';
    public static final char HighlightPush = 'P';

    /**
     * Creates a new annotation for the given rect.
     */
    public PDFAnnotation(Rect aRect)
    {
        _map.put("Type", "/Annot");
        String r = "[" + (int) aRect.x + " " + (int) aRect.y + " " + (int) aRect.getMaxX() + " " + (int) aRect.getMaxY() + "]";
        _map.put("Rect", r);
    }

    /**
     * Sets the type of the annotation.
     */
    public void setType(String s)
    {
        _map.put("Subtype", s);
    }

    /**
     * Sets the highlight mode of the annotaiton.
     */
    public void setHighlightMode(char h)
    {
        _map.put("H", "/" + h);
    }

    /**
     * Sets whether the annotation has a border.
     */
    public void setHasBorder(boolean b)
    {
        if (b) _map.remove("Border");
        else _map.put("Border", "[0 0 0]");
    }

    /**
     * Tells the annotation to resolve page references.
     */
    public void resolvePageReferences(PDFPageTree pages, PDFXTable xref, PDFPageWriter aPW)
    {
    }

    /**
     * Returns the annotation map.
     */
    public Map getAnnotationMap()
    {
        return _map;
    }

    /**
     * An inner class (and annotation subclass) to support hyperlinks.
     */
    public static class Link extends PDFAnnotation {
        int _page = -1;

        public Link(Rect aRect, String aUrl)
        {
            super(aRect);
            setType("/Link");

            // Handle special "Page:"
            if (aUrl.startsWith("Page:")) {
                if (aUrl.startsWith("Page:Next")) _page = 99999;
                else if (aUrl.startsWith("Page:Back")) _page = -99999;
                else _page = StringUtils.intValue(aUrl) - 1;
            }

            // add url action to annotation dictionary
            else {
                Map urlAction = new Hashtable();
                urlAction.put("Type", "/Action");
                urlAction.put("S", "/URI");
                urlAction.put("URI", '(' + aUrl + ')');
                _map.put("A", urlAction);
            }
            setHasBorder(false);
        }

        public void resolvePageReferences(PDFPageTree pages, PDFXTable xref, PDFPageWriter aPW)
        {
            if (_page == 99999) _page = pages.indexOf(aPW) + 1;
            if (_page == -99999) _page = pages.indexOf(aPW) - 1;
            if (_page >= 0) {
                PDFPageWriter page = pages.getPage(_page);
                String ref = xref.getRefString(page);
                _map.put("Dest", "[" + ref + " /XYZ null null null]");
            }
        }
    }

    /**
     * An annotation subclass to support widgets. See PDF Spec "Widget Annotations", p 408 & sec 12.7.
     */
    public static class Widget extends PDFAnnotation {

        public Widget(Rect aRect, String aStr)
        {
            super(aRect);
            setType("/Widget");
            setHasBorder(true);
            _map.put("F", 4); // Annotation flags
            _map.put("FT", "/Tx");    // Field Type = TextFields
        }
    }

    /**
     * An annotation subclass to support FreeText. See PDF Spec "FreeText Annotations", p 395 & sec 12.5.6.6.
     */
    public static class FreeText extends PDFAnnotation {

        public FreeText(Rect aRect, String aStr)
        {
            super(aRect);
            setType("/Text");
            if (aStr != null && aStr.length() > 0)
                _map.put("Contents", '(' + aStr + ')');
        }
    }

}