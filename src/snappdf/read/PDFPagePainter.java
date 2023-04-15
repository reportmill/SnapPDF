/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snappdf.read;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.*;
import snap.geom.*;
import snap.gfx.*;
import snap.gfx.Image;
import snappdf.*;

/**
 * This paints a PDFPage to a given Painter by parsing the page marking operators.
 * <p>
 * Currently unsupported:
 * - Ignores hyperlinks (annotations)
 * - Type 1 & Type 3 fonts
 * - Transparency blend modes other than /Normal
 * ...
 */
public class PDFPagePainter {

    // The page being painted
    PDFPage _page;

    // The PDF file for the page
    PDFFile _pfile;

    // The page bytes holding page operators and data to be painted
    byte[] _pageBytes;

    // A helper class to handle text processing
    PDFPageText _text;

    // The gstates of the page being parsed
    Stack<PDFGState> _gstates;

    // The current GState
    PDFGState _gstate;

    // The tokens of the page being parsed
    List<PageToken> _tokens;

    // The current token index
    int _index;

    // Current path
    Path2D _path = null;

    // Whether to clip after next draw op
    boolean _doClip;

    // save away the factory callback handler objects
    int _compatibilitySections = 0;

    // The painter
    Painter _pntr;

    /**
     * Creates a PDFPagePainter.
     */
    public PDFPagePainter(PDFPage aPage)
    {
        // Cache given page and page file
        _page = aPage;
        _pfile = aPage.getFile();

        // Initialize a text object
        _text = new PDFPageText(this);

        // Create the gstate list and the default gstate
        _gstates = new Stack<>();
        _gstates.push(_gstate = new PDFGState());
    }

    /**
     * Returns the painter.
     */
    public Painter getPainter()
    {
        return _pntr;
    }

    /**
     * Paints the page inside the given rect.
     */
    public void paint(Painter aPntr, Object aSource, Rect theDestBnds, Transform aTrans)
    {
        // Set painter and save painter state
        _pntr = aPntr;
        aPntr.save();

        // Get Source bounds (the natural bounds of the Page, Form or Pattern)
        Rect srcBnds;
        if (aSource instanceof PDFForm) {
            PDFForm form = (PDFForm) aSource;
            Rect bbox = form.getBBox();
            srcBnds = new Rect(0, 0, bbox.getWidth(), bbox.getHeight());
        }
        else if (aSource instanceof PDFPattern) {
            PDFPattern ptrn = (PDFPattern) aSource;
            srcBnds = new Rect(0, 0, ptrn.getBounds().getWidth(), ptrn.getBounds().getHeight());
        }
        else {
            Rect media = _page.getMediaBox();
            Rect crop = _page.getCropBox();
            srcBnds = media.getIntersectRect(crop);
            srcBnds.snap();
        }

        // Get Dest bounds
        Rect destBnds = theDestBnds != null ? theDestBnds : srcBnds;

        // Get flip transform (for page or pattern)
        if (aSource == null || aSource instanceof PDFPattern)
            concatenate(new Transform(destBnds.width / srcBnds.width, 0, 0, -destBnds.height / srcBnds.height,
                    destBnds.x, destBnds.getMaxY()));

        // If Transform provided, append it
        if (aTrans != null) concatenate(aTrans);

        // Make sure PageBytes is set (if missing, get from Page)
        if (_pageBytes == null) {
            PDFStream pstream = _page.getPageContentsStream();
            if (pstream == null) return;
            _pageBytes = pstream.decodeStream();
        }

        // Make sure Tokens is set (if missing, get from PageBytes)
        if (_tokens == null)
            _tokens = PageToken.getTokens(_pageBytes);

        // Initialize current path. Note: path is not part of GState and so is not saved/restored by gstate ops
        _path = null;
        _doClip = false;
        _compatibilitySections = 0;

        // Iterate over tokens
        for (int i = 0, iMax = _tokens.size(); i < iMax; i++) {
            PageToken token = getToken(i);
            _index = i;
            if (token.type == PageToken.PDFOperatorToken)
                paintOp(token);
        }

        // Restore painter state
        aPntr.restore();
    }

    /**
     * Returns the token at the given index.
     */
    private PageToken getToken(int index)
    {
        return _tokens.get(index);
    }

    /**
     * The meat and potatoes of the pdf parser. Translates the token list into a series of calls to either a Factory class,
     * which creates a Java2D object (like GeneralPath, Font, Image, GlyphVector, etc.), or the markup handler, which does
     * the actual drawing.
     */
    public void paintOp(PageToken aToken)
    {
        String op = aToken.getString();

        switch (op) {
            case "b": b(); break;      // Closepath, fill, stroke
            case "b*": b_x(); break;   // Closepath, fill, stroke (EO)
            case "B": B(); break;      // Fill, stroke
            case "B*": B_x(); break;   // Fill, stroke (EO)
            case "BT": BT(); break;    // Begin Text
            case "BX": BX(); break;
            case "BI": BI(); break;
            case "BDC": case "BMC": BDC(); break;
            case "c": c(); break;      // Curveto
            case "cm": cm(); break;    // Concat matrix
            case "cs": cs(); break;    // Set colorspace
            case "CS": CS(); break;    // Set stroke colorspace
            case "d": d(); break;      // Set dash
            case "Do": Do(); break;    // Do xobject
            case "DP": DP(); break;    // Marked content
            case "ET": ET(); break;    // End text
            case "EX": EX(); break;
            case "EMC": EMC(); break;
            case "f": case "F": f(); break;     // Fill
            case "f*": case "F*": f_x(); break; // Fill (EO)
            case "g": g(); break;      // Set gray
            case "gs": gs(); break;    // Extended graphics state
            case "G": G(); break;      // Set stroke gray
            case "h": h(); break;      // Closepath
            case "i": i(); break;      // Set flatness
            case "ID": ID(); break;
            case "j": j(); break;      // Set linejoin
            case "J": J(); break;      // Set linecap
            case "k": k(); break;      // Set cmyk
            case "K": K(); break;      // Set stroke cmyk
            case "l": l(); break;      // Lineto
            case "m": m(); break;      // Moveto
            case "M": M(); break;      // Set miterlimit
            case "MP": MP(); break;
            case "n": n(); break;      // Endpath
            case "q": q(); break;      // GSave
            case "Q": Q(); break;      // GRestore
            case "re": re(); break;    // Append rect
            case "rg": rg(); break;    // Set rgb color
            case "ri": ri(); break;    // Set render intent
            case "RG": RG(); break;    // Set stroke rgb color
            case "s": s(); break;      // Closepath
            case "sc": sc(); break;    // Set color in colorspace
            case "scn": scn(); break;  // Set color in colorspace
            case "sh": sh(); break;    // Set shader
            case "S": S(); break;      // Stroke path
            case "SC": SC(); break;    // Set stroke color in colorspace
            case "SCN": SCN(); break;  // Set stroke color in colorspace
            case "T*": T_x(); break;   // Move to next line
            case "Tc": Tc(); break;    // Set character spacing
            case "Td": Td(); break;    // Move relative to current line start
            case "TD": TD(); break;    // Move relative to current line start and set leading to -ty
            case "Tf": Tf(); break;    // Set font
            case "Tj": Tj(); break;    // Show text
            case "TJ": TJ(); break;    // Show text array
            case "TL": TL(); break;    // Set text leading
            case "Tm": Tm(); break;    // Set text matrix
            case "Tr": Tr(); break;    // Set text rendering mode
            case "Ts": Ts(); break;    // Set text rise
            case "Tw": Tw(); break;    // Set text word spacing
            case "Tz": Tz(); break;    // Set text horizontal scale factor
            //case "\'": case "\"": quote(); break;
            case "v": v(); break;      // Curveto
            case "w": w(); break;      // Set linewidth
            case "W": W(); break;      // Set clip
            case "W*": W_x(); break;   // Set clip (EO)
            case "y": y(); break;      // Curveto
            default: System.err.println("PDFPagePainter: Unknown op: " + op);
        }
    }

    /**
     * Closepath, fill, stroke
     */
    void b()
    {
        _path.setWinding(Shape.WIND_NON_ZERO);
        _path.close();
        fillPath();
        strokePath();
        didDraw();
    }

    /**
     * Closepath, fill, stroke (EO)
     */
    void b_x()
    {
        _path.setWinding(Shape.WIND_EVEN_ODD);
        _path.close();
        fillPath();
        strokePath();
        didDraw();
    }

    /**
     * Fill, stroke
     */
    void B()
    {
        _path.setWinding(Shape.WIND_NON_ZERO);
        fillPath();
        strokePath();
        didDraw();
    }

    /**
     * Fill, stroke (EO)
     */
    void B_x()
    {
        _path.setWinding(Shape.WIND_EVEN_ODD);
        fillPath();
        strokePath();
        didDraw();
    }

    /**
     * Begin Text
     */
    void BT()
    {
        _text.begin();
    }

    /**
     * BX start (possibly nested) compatibility section
     */
    void BX()
    {
        ++_compatibilitySections;
    }

    /**
     * BI inline images
     */
    void BI()
    {
        _index = parseInlineImage(_index + 1, _pageBytes);
    }

    /**
     * BDC, BMC
     */
    void BDC()  { }

    /**
     * Curveto.
     */
    void c()
    {
        getPoint(_index, _gstate.cp);
        _path.curveTo(getFloat(_index - 6), getFloat(_index - 5), getFloat(_index - 4), getFloat(_index - 3),
                _gstate.cp.x, _gstate.cp.y);
    }

    /**
     * Concat matrix
     */
    void cm()
    {
        Transform xfm = getTransform(_index);
        concatenate(xfm);
    }

    /**
     * Concat matrix
     */
    void concatenate(Transform xfm)
    {
        _pntr.transform(xfm);
    }

    /**
     * Set colorspace
     */
    void cs()
    {
        String space = getToken(_index - 1).getName();
        _gstate.colorSpace = PDFColorSpace.getColorspace(space, _page);
    }

    /**
     * Set stroke colorspace
     */
    void CS()
    {
        String space = getToken(_index - 1).getName();
        _gstate.scolorSpace = PDFColorSpace.getColorspace(space, _page);
    }

    /**
     * Set dash
     */
    void d()
    {
        float[] ld = _gstate.lineDash = getFloatArray(_index - 2);
        if (ld.length == 0)
            _gstate.lineDash = null;
        _gstate.dashPhase = getFloat(_index - 1);
        _gstate.stroke = null;
    }

    /**
     * Do xobject
     */
    void Do()
    {
        String name = getToken(_index - 1).getName();
        Object xobj = getXObject(name);
        if (xobj instanceof java.awt.Image)
            drawImage((java.awt.Image) xobj);
        else if (xobj instanceof PDFForm)
            executeForm((PDFForm) xobj);
        else throw new PDFException("Error reading XObject");
    }

    /**
     * Marked content
     */
    void DP()  { }

    /**
     * ET - End text
     */
    void ET()
    {
        _text.end();
    }

    /**
     * EMC
     */
    void EMC()  { }

    /**
     * EX
     */
    void EX()
    {
        if (--_compatibilitySections < 0) throw new PDFException("Unbalanced BX/EX operators");
    }

    /**
     * Fill
     */
    void f()
    {
        _path.setWinding(Shape.WIND_NON_ZERO);
        fillPath();
        didDraw();
    }

    /**
     * Fill (EO)
     */
    void f_x()
    {
        _path.setWinding(Shape.WIND_EVEN_ODD);
        fillPath();
        didDraw();
    }

    /**
     * Set gray
     */
    void g()
    {
        ColorSpace cspace = PDFColorSpace.getColorspace("DeviceGray", _page);
        _gstate.color = getColor(cspace, _index);
        _gstate.colorSpace = cspace;
    }

    /**
     * Extended graphics state
     */
    void gs()
    {
        Map exg = getExtendedGStateNamed(getToken(_index - 1).getName());
        readExtendedGState(exg);
    }

    /**
     * Set stroke gray
     */
    void G()
    {
        ColorSpace cspace = PDFColorSpace.getColorspace("DeviceGray", _page);
        _gstate.scolor = getColor(cspace, _index);
        _gstate.scolorSpace = cspace;
    }

    /**
     * Closepath
     */
    void h()
    {
        _path.close();

        // Reset Gstate current point
        _gstate.cp = _path.getLastPoint();
        if (_path.getLastSeg() == Seg.Close && _path.getPointCount() > 0)
            _gstate.cp = _path.getPoint(0);
    }

    /**
     * Set flatness
     */
    void i()
    {
        _gstate.flatness = getFloat(_index - 1);
    }

    /**
     * ID
     */
    void ID()  { }

    /**
     * Set linejoin
     */
    void j()
    {
        _gstate.lineJoin = getInt(_index - 1);
        _gstate.stroke = null;
    }

    /**
     * Set linecap
     */
    void J()
    {
        _gstate.lineCap = getInt(_index - 1);
        _gstate.stroke = null;
    }

    /**
     * Set cmyk
     */
    void k()
    {
        ColorSpace cspace = PDFColorSpace.getColorspace("DeviceCMYK", _page);
        Color acolor = getColor(cspace, _index);
        _gstate.colorSpace = cspace;
        _gstate.color = acolor;
    }

    /**
     * Set stroke cmyk
     */
    void K()
    {
        ColorSpace cspace = PDFColorSpace.getColorspace("DeviceCMYK", _page);
        Color acolor = getColor(cspace, _index);
        _gstate.scolorSpace = cspace;
        _gstate.scolor = acolor;
    }

    /**
     * Lineto
     */
    void l()
    {
        getPoint(_index, _gstate.cp);
        _path.lineTo(_gstate.cp.x, _gstate.cp.y);
    }

    /**
     * Moveto
     */
    void m()
    {
        getPoint(_index, _gstate.cp);
        if (_path == null)
            _path = new Path2D();
        _path.moveTo(_gstate.cp.x, _gstate.cp.y);
    }

    /**
     * Set miterlimit
     */
    void M()
    {
        _gstate.miterLimit = getFloat(_index - 1);
        _gstate.stroke = null;
    }

    /**
     * Marked content point
     */
    void MP()  { }

    /**
     * Endpath
     */
    void n()
    {
        didDraw();
    }

    /**
     * gsave
     */
    void q()  { _gstate = gsave(); }

    /**
     * grestore.
     */
    void Q()  { _gstate = grestore(); }

    /**
     * Append rectangle
     */
    void re()
    {
        // Get rect
        float x = getFloat(_index - 4), y = getFloat(_index - 3);
        float w = getFloat(_index - 2), h = getFloat(_index - 1);

        // Create new path and add rect and reset current point to start of rect
        if (_path == null)
            _path = new Path2D();
        _path.moveTo(x, y);
        _path.lineTo(x + w, y);
        _path.lineTo(x + w, y + h);
        _path.lineTo(x, y + h);
        _path.close();
        _gstate.cp.x = x;
        _gstate.cp.y = y;  // TODO: Check that this is what really happens in pdf
    }

    /**
     * Set render intent
     */
    void ri()
    {
        _gstate.renderingIntent = PDFGState.getRenderingIntentID(getToken(_index - 1).getString());
    }

    /**
     * Set rgb color
     */
    void rg()
    {
        ColorSpace cspace = PDFColorSpace.getColorspace("DeviceRGB", _page);
        _gstate.color = getColor(cspace, _index);
        _gstate.colorSpace = cspace;
    }

    /**
     * Set stroke rgb color
     */
    void RG()
    {
        ColorSpace cspace = PDFColorSpace.getColorspace("DeviceRGB", _page);
        _gstate.scolor = getColor(cspace, _index);
        _gstate.scolorSpace = cspace;
    }

    /**
     * Closepath
     */
    void s()
    {
        _path.close();
        strokePath();
        didDraw();
    }

    /**
     * Stroke path
     */
    void S()
    {
        strokePath();
        didDraw();
    }

    /**
     * Set color in colorspace
     */
    void sc()
    {
        _gstate.color = getColor(_gstate.colorSpace, _index);
    }

    /**
     * Set color in colorspace
     */
    void scn()
    {
        // Handle PatternSpace
        if (_gstate.colorSpace instanceof PDFColorSpaces.PatternSpace) { // && numops>=1) {

            System.err.println("PDFPagePainter: sc for PatternSpace not implemented");
        /*String pname = getToken(_index-1).getName();
        PDFPattern pat = PDFPattern.getPattern(pname, _page);
        gs.color = pat.getPaint();
        
        // this is really stupid.  change this around
        if(pat instanceof PDFPatterns.Tiling && gs.color==null) {
            // Uncolored tiling patterns require color values be passed. Note, that although you can draw them
            // any number of times in different colors, we only do it once (after which it will be cached)
            if (numops>1) {
                ColorSpace tileSpace=((PDFColorSpaces.PatternSpace)gs.colorSpace).tileSpace;
                if(tileSpace==null) tileSpace = gs.colorSpace;
                gs.color = getColor(tileSpace,_index-1, numops-1);
            }
            this.executePatternStream((PDFPatterns.Tiling)pat);
            gs.color = pat.getPaint();
        }*/
        }

        // Do normal version
        else _gstate.color = getColor(_gstate.colorSpace, _index);
    }

    /**
     * Set shader
     */
    void sh()
    {
        System.err.println("PDFPagePainter: Set shader (sh) not implemented");
    /*String shadename = getToken(_index-1).getName();
    java.awt.Paint oldPaint = gs.color;
    PDFPatterns.Shading shade = PDFPattern.getShading(shadename, _page);
    gs.color = shade.getPaint();  //save away old color
    // Get area to fill. If shading specifies bounds, use that, if not, use clip. else fill whole page.
    GeneralPath shadearea;
    if(shade.getBounds() != null)
        shadearea = new GeneralPath(shade.getBounds());
    else {
        Rectangle2D r = new Rectangle2D.Double(_bounds.x, _bounds.y, _bounds.width, _bounds.height);
        shadearea = gs.clip!=null? (GeneralPath)gs.clip.clone() : new GeneralPath(r);
        try { shadearea.transform(gs.trans.createInverse()); } // transform from page to user space
        catch(NoninvertibleTransformException e) { throw new PDFException("Invalid user space xform"); }
    }
    fillPath(gs, shadearea);
    gs.color = oldPaint;
    didDraw();*/
    }

    /**
     * Set stroke color in normal colorspaces
     */
    void SC()
    {
        _gstate.scolor = getColor(_gstate.scolorSpace, _index);
    }

    /**
     * Set strokecolor in normal colorspaces
     */
    void SCN()
    {
        SC();
    } // TODO: deal with weird colorspaces

    /**
     * Move to next line
     */
    void T_x()
    {
        _text.positionText(0, -_gstate.tleading);
    }

    /**
     * Set character spacing
     */
    void Tc()
    {
        _gstate.tcs = getFloat(_index - 1);
    }

    /**
     * Move relative to current line start
     */
    void Td()
    {
        float x = getFloat(_index - 2);
        float y = getFloat(_index - 1);
        _text.positionText(x, y);
    }

    /**
     * Move relative to current line start and set leading to -ty
     */
    void TD()
    {
        float x = getFloat(_index - 2);
        float y = getFloat(_index - 1);
        _text.positionText(x, y);
        _gstate.tleading = -y;
    }

    /**
     * Set font name and size
     */
    void Tf()
    {
        String fontalias = getToken(_index - 2).getName(); // name in dict is key, so lose leading /
        _gstate.font = getFontDictForAlias(fontalias);
        _gstate.fontSize = getFloat(_index - 1);
    }

    /**
     * Show text.
     */
    void Tj()
    {
        PageToken tok = getToken(_index - 1);
        int tloc = tok.getStart(), tlen = tok.getLength();
        _text.showText(tloc, tlen);
    }

    /**
     * Show text with spacing adjustment array
     */
    void TJ()
    {
        List tArray = (List) getToken(_index - 1).value;
        _text.showText(tArray);
    }

    /**
     * Set text leading
     */
    void TL()
    {
        _gstate.tleading = getFloat(_index - 1);
    }

    /**
     * Set text matrix
     */
    void Tm()
    {
        float a = getFloat(_index - 6), b = getFloat(_index - 5), c = getFloat(_index - 4), d = getFloat(_index - 3);
        float tx = getFloat(_index - 2), ty = getFloat(_index - 1);
        _text.setTextMatrix(a, b, c, d, tx, ty);
    }

    /**
     * Set text rendering mode
     */
    void Tr()
    {
        _gstate.trendermode = getInt(_index - 1);
    }

    /**
     * Set text rise
     */
    void Ts()
    {
        _gstate.trise = getFloat(_index - 1);
    }

    /**
     * Set text word spacing
     */
    void Tw()
    {
        _gstate.tws = getFloat(_index - 1);
    }

    /**
     * Set text horizontal scale factor
     */
    void Tz()
    {
        _gstate.thscale = getFloat(_index - 1) / 100f;
    }

    /**
     * Curveto (first control point is current point)
     */
    void v()
    {
        double cp1x = _gstate.cp.x, cp1y = _gstate.cp.y;
        Point cp2 = getPoint(_index - 2);
        getPoint(_index, _gstate.cp);
        _path.curveTo(cp1x, cp1y, cp2.x, cp2.y, _gstate.cp.x, _gstate.cp.y);
    }

    /**
     * Set linewidth
     */
    void w()
    {
        _gstate.lineWidth = getFloat(_index - 1);
        _gstate.stroke = null;
    }

    /**
     * Set clip
     */
    void W()
    {
        // Somebody at Adobe's been smoking crack. The clipping operation doesn't modify the clipping in the gstate.
        // Instead, the next path drawing operation will do that, but only AFTER it draws.
        // So a sequence like 0 0 99 99 re W f will fill the rect first and then set the clip path using the rect.
        // Because the W operation doesn't do anything, they had to introduce the 'n' operation, which is a drawing no-op,
        // in order to do a clip and not also draw the path. You might think it would be safe to just reset the clip here,
        // since the path it will draw is the same as the path it will clip to. However, there's at least one (admittedly
        // obscure) case I can think of where clip(path),draw(path)  is different from draw(path),clip(path):
        //     W* f  %eoclip, nonzero-fill
        // Note also, Acrobat considers it an error to have a W not immediately followed by drawing op (f,f*,F,s,S,B,b,n)
        if (_path != null) {
            _path.setWinding(Shape.WIND_NON_ZERO);
            _doClip = true;
        }
    }

    /**
     * Set clip (EO)
     */
    void W_x()
    {
        if (_path != null) {
            _path.setWinding(Shape.WIND_EVEN_ODD);
            _doClip = true;
        }
    }

    /**
     * Curveto (final point replicated)
     */
    void y()
    {
        Point cp1 = getPoint(_index - 2);
        getPoint(_index, _gstate.cp);
        _path.curveTo(cp1.x, cp1.y, _gstate.cp.x, _gstate.cp.y, _gstate.cp.x, _gstate.cp.y);
    }

/** quote */
//void quote()  { } //if(tlen==1 && parseTextOperator(c, i, numops, gs, pageBytes)) swallowedToken = true;
//void single_quote_close()  { _text.positionText(0, -gs.tleading); } // Fall through
//void double_quote_close() { gs.tws = getFloat(_index-3); gs.tcs = getFloat(_index-2); numops = 1;
//    _text.positionText(0, -gs.tleading); }

    /**
     * Called after drawing op.
     */
    void didDraw()
    {
        // If clipping operator preceeded drawing, update clip with drawing path.
        if (_doClip) {
            _pntr.clip(_path);
            _doClip = false;
        }

        // The current path and the current point are undefined after a draw
        _path = null;
    }

    /**
     * Returns the token at the given index as a float.
     */
    private float getFloat(int i)
    {
        return getToken(i).floatValue();
    }

    /**
     * Returns the token at the given index as an int.
     */
    private int getInt(int i)
    {
        return getToken(i).intValue();
    }

    /**
     * Returns the token at the given index as an array of floats
     */
    private float[] getFloatArray(int i)
    {
        List<PageToken> tokens = (List<PageToken>) getToken(i).value;
        float[] floatArray = new float[tokens.size()];
        for (int j = 0, jMax = tokens.size(); j < jMax; j++)
            floatArray[j] = tokens.get(j).floatValue();
        return floatArray;
    }

    /**
     * Returns a new point at the given index
     */
    private Point getPoint(int i)
    {
        double x = getFloat(i - 2);
        double y = getFloat(i - 1);
        return new Point(x, y);
    }

    /**
     * Gets the token at the given index as a point.
     */
    private void getPoint(int i, Point pt)
    {
        pt.x = getFloat(i - 2);
        pt.y = getFloat(i - 1);
    }

    /**
     * Returns the token at the given index as a transform.
     */
    private Transform getTransform(int i)
    {
        float a = getFloat(i - 6), b = getFloat(i - 5);
        float c = getFloat(i - 4), d = getFloat(i - 3);
        float tx = getFloat(i - 2), ty = getFloat(i - 1);
        return new Transform(a, b, c, d, tx, ty);
    }

    /**
     * Called with any of the set color ops to create new color from values in stream.
     */
    private Color getColor(ColorSpace space, int tindex)
    {
        int componentCount = space.getNumComponents();
        float[] colorComponents = new float[componentCount];
        for (int i = 0; i < componentCount; i++)
            colorComponents[i] = getFloat(tindex - (componentCount - i));
        return new Color(space, colorComponents, 1f);
    }

    /**
     * Converts the tokens & data inside a BI/EI block into an image and draws it.
     * Returns the index of the last token consumed.
     */
    private int parseInlineImage(int tIndex, byte[] pageBytes)
    {
        Map<String,Object> imageDict = new HashMap<>();
        imageDict.put("Subtype", "/Image");

        // Get the inline image key/value pairs and create a normal image dictionary
        for (int i = tIndex, iMax = _tokens.size(); i < iMax; ++i) {
            PageToken token = getToken(i);

            // Handle NameToken: Translate key, get value, add translated key/value pair to the real dict
            if (token.type == PageToken.PDFNameToken) {
                String key = translateInlineImageKey(token.getName());
                if (++i < iMax) {
                    token = getToken(i);
                    Object value = getInlineImageValue(token, pageBytes);
                    imageDict.put(key, value);
                }
            }

            // The actual inline data. Create stream with dict & data and create image. The image does not get cached.
            // The only way an inline image would ever get reused is if it were inside a form xobject.
            // First get a colorspace object.  Inline images can use any colorspace a regular image can.
            // Create stream, tell imageFactory to create image and draw it
            else if (token.type == PageToken.PDFInlineImageData) {
                Object space = imageDict.get("ColorSpace");
                ColorSpace imgCSpace = space != null ? PDFColorSpace.getColorspace(space, _page) : null;
                PDFStream imgStream = new PDFStream(pageBytes, token.getStart(), token.getLength(), imageDict);
                java.awt.Image image = PDFImage.getImage(imgStream, imgCSpace, _pfile);
                drawImage(image);
                return i; // return token index
            }
        }

        // should only get here on an error (like running out of tokens or having bad key/value pairs)
        throw new PDFException("Syntax error parsing inline image dictionary");
    }

    /**
     * The values for keys in inline images are limited to a small subset of names, numbers, arrays and maybe a dict.
     */
    private Object getInlineImageValue(PageToken token, byte[] pageBytes)
    {
        // Names (like /DeviceGray or /A85). Names can optionally be abbreviated.
        if (token.type == PageToken.PDFNameToken) {
            String abbrev = token.getName();
            for (String[] inlineImageValueAbbreviation : _inline_image_value_abbreviations) {
                if (inlineImageValueAbbreviation[0].equals(abbrev))
                    return '/' + inlineImageValueAbbreviation[1];
            }
            return '/' + abbrev;  // not found, so it's not an abbreviation.  We assume it's valid
        }

        // Numbers or bools
        if (token.type == PageToken.PDFNumberToken || token.type == PageToken.PDFBooleanToken)
            return token.value;

        // An array of numbers or names (for Filter or Decode)
        if (token.type == PageToken.PDFArrayToken) {
            List tokenarray = (List) token.value;
            List newarray = new ArrayList<>(tokenarray.size());
            // recurse
            for (Object o : tokenarray)
                newarray.add(getInlineImageValue((PageToken) o, pageBytes));
            return newarray;
        }

        // Hex strings for indexed color spaces
        if (token.type == PageToken.PDFStringToken)
            return token.byteArrayValue(pageBytes);

        // TODO: One possible key in an inline image is DecodeParms (DP). The normal decodeparms for an image is a dict.
        // The pdf spec doesn't give any information on the format of the dictionary.  Does it use the normal dictionary
        // syntax, or does it use the inline image key/value syntax? I have no idea, and I don't know how to generate a
        // pdf file that would have an inline image with a decodeparms dict.
        throw new PDFException("Error parsing inline image dictionary");
    }

    /**
     * map for translating inline image abbreviations into standard tokens
     */
    static final String[][] _inline_image_key_abbreviations = {
            {"BPC", "BitsPerComponent"}, {"CS", "ColorSpace"}, {"D", "Decode"}, {"DP", "DecodeParms"},
            {"F", "Filter"}, {"H", "Height"}, {"IM", "ImageMask"}, {"I", "Interpolate"}, {"W", "Width"}
    };

    /**
     * Looks up an abbreviation in the above map.
     */
    private String translateInlineImageKey(String abbreviation)
    {
        for (String[] inlineImageKeyAbbreviation : _inline_image_key_abbreviations) {
            if (inlineImageKeyAbbreviation[0].equals(abbreviation))
                return inlineImageKeyAbbreviation[1];
        }
        return abbreviation; // not found, so it's not an abbreviation
    }

    static final String[][] _inline_image_value_abbreviations = {
            {"G", "DeviceGray"}, {"RGB", "DeviceRGB"}, {"CMYK", "DeviceCMYK"}, {"I", "Indexed"}, {"AHx", "ASCIIHexDecode"},
            {"A85", "ASCII85Decode"}, {"LZW", "LZWDecode"}, {"Fl", "FlateDecode"}, {"RL", "RunLengthDecode"},
            {"CCF", "CCITTFaxDecode"}, {"DCT", "DCTDecode"}
    };

    /**
     * Execute form.
     */
    void executeForm(PDFForm aForm)
    {
        // Add form's resources to page resource stack
        _page.pushResources(aForm.getResources(_pfile));

        // Recurse back into this painter for form tokens and bytes
        PDFPagePainter ppntr = new PDFPagePainter(_page);
        ppntr._pageBytes = aForm.getBytes();
        ppntr._tokens = aForm.getTokens();
        ppntr.paint(_pntr, aForm, null, aForm.getTransform());

        // Restore old resources
        _page.popResources();
    }

    /**
     * A pattern could execute its pdf over and over, like a form (above) but for performance reasons,
     * we only execute it once and cache a tile. To do this, we temporarily set the markup handler in the file to a new
     * BufferedMarkupHander, add the pattern's resource dictionary and fire up the parser.
     */
    private void executePatternStream(PDFPattern.Tiling aPattern)
    {
        // Get pattern width & height
        int width = (int) Math.ceil(aPattern.getBounds().getWidth());
        int height = (int) Math.ceil(aPattern.getBounds().getHeight());

        // By adding the pattern's resources to page's resource stack, it means pattern will have access to resources
        // defined by the page.  I'll bet Acrobat doesn't allow you to do this, but it shouldn't hurt anything.
        _page.pushResources(aPattern.getResources());

        // Create image for pattern
        Image img = Image.get(width, height, true);
        Painter ipntr = img.getPainter();

        // Create PagePainter with pattern bounds and transform
        PDFPagePainter ppntr = new PDFPagePainter(_page);
        ppntr._pageBytes = aPattern.getContents();
        ppntr.paint(ipntr, aPattern, null, aPattern.getTransform());

        // Get the image and set the tile.  All the resources can be freed up now
        aPattern.setTile(img);
        System.out.println("PDFPagePainter.executePatternStream");
    }

    /**
     * Pull out anything useful from an extended gstate dictionary
     * The dict will have been read in, so values have been converted to appropriate types, like Integer, Float, List, etc.
     */
    private void readExtendedGState(Map<String, Object> exgstate)
    {
        PDFGState gs = _gstate;
        if (exgstate == null) return;

        // Iterate over entries
        for (Map.Entry<String, Object> entry : exgstate.entrySet()) {

            // Get key/value
            String key = entry.getKey();
            Object val = entry.getValue();

            //line width, line cap, line join, & miter limit
            if (key.equals("LW")) {
                gs.lineWidth = ((Number) val).floatValue();
                gs.stroke = null;
            }
            else if (key.equals("LC")) {
                gs.lineCap = ((Number) val).intValue();
                gs.stroke = null;
            }
            else if (key.equals("LJ")) {
                gs.lineJoin = ((Number) val).intValue();
                gs.stroke = null;
            }
            else if (key.equals("ML")) {
                gs.miterLimit = ((Number) val).floatValue();
                gs.stroke = null;
            }

            // Dash:       "/D  [ [4 2 5 5] 0 ]"
            else if (key.equals("D")) {
                List dash = (List) val;
                gs.dashPhase = ((Number) dash.get(1)).floatValue();
                List dashArray = (List) dash.get(0);
                int n = dashArray.size();
                gs.lineDash = new float[n];
                for (int i = 0; i < n; ++i) gs.lineDash[i] = ((Number) dashArray.get(i)).floatValue();
                gs.stroke = null;
            }

            // Rendering intent
            else if (key.equals("RI"))
                gs.renderingIntent = PDFGState.getRenderingIntentID((String) val);

                // Transparency blending mode
            else if (key.equals("BM")) {
                int bm = PDFComposite.getBlendModeID((String) val);
                if (bm != gs.blendMode) {
                    gs.blendMode = bm;
                    _pntr.setComposite(PDFComposite.getComposite(gs.blendMode));
                } //alphaChanged = true;
            }

            // Transparency - whether to treat alpha values as shape or transparency
            else if (key.equals("AIS")) {
                boolean ais = (Boolean) val;
                if (ais != gs.alphaIsShape) gs.alphaIsShape = ais; //alphaChanged = true;
            }

            // Soft mask
            else if (key.equals("SMask")) {
                if (val.equals("/None")) gs.softMask = null;
                else System.err.println("Soft mask being specified : " + val);
            }

            // Transparency - stroke alpha
            else if (key.equals("CA")) {
                float a = ((Number) val).floatValue();
                if (a != gs.salpha) gs.salpha = a; //alphaChanged = true;
            }

            // Transparency - nonstroke alpha
            else if (key.equals("ca")) {
                float a = ((Number) val).floatValue();
                if (a != gs.alpha) {
                    gs.alpha = a;
                    _pntr.setOpacity(gs.alpha);
                } //alphaChanged = true;
            }
            // Some other possible entries in this dict that are not currently handled include:
            // Font, BG, BG2, UCR, UCR2, OP, op, OPM, TR, TR2, HT, FL, SM, SA,TK
        }
    }

    /**
     * Accessors for the resource dictionaries.
     */
    private Map getExtendedGStateNamed(String name)
    {
        return (Map) _page.findResource("ExtGState", name);
    }

    /**
     * Returns the pdf Font dictionary for a given name (like "/f1").  You
     * can use the FontFactory to get interesting objects from the dictionary.
     */
    private Map getFontDictForAlias(String alias)
    {
        return (Map) _page.findResource("Font", alias);
    }

    /**
     * Like above, but for XObjects. XObjects can be Forms or Images. If the dictionary represents an Image, this routine
     * calls the ImageFactory to create a java.awt.Image. If it's a Form XObject, the object returned will be a PDFForm.
     */
    private Object getXObject(String pdfName)
    {
        PDFStream xobjStream = (PDFStream) _page.findResource("XObject", pdfName);

        if (xobjStream != null) {
            Map xobjDict = xobjStream.getDict();

            // Check to see if we went through this already
            Object cached = xobjDict.get("_rbcached_xobject_");
            if (cached != null)
                return cached;

            String type = (String) xobjDict.get("Subtype");
            if (type == null)
                throw new PDFException("Unknown xobject type");

            // Image XObject - pass it to the ImageFactory
            if (type.equals("/Image")) {
                // First check for a colorspace entry for the image, and create an awt colorspace.
                Object space = _page.getXRefObj(xobjDict.get("ColorSpace"));
                ColorSpace imageCSpace = space == null ? null : PDFColorSpace.getColorspace(space, _page);
                cached = PDFImage.getImage(xobjStream, imageCSpace, _pfile);
            }

            // A PDFForm just saves the stream away for later parsing
            else if (type.equals("/Form"))
                cached = new PDFForm(xobjStream);

            if (cached != null) {
                xobjDict.put("_rbcached_xobject_", cached);
                return cached;
            }
        }

        // Complain and return null
        System.err.println("Unable to get xobject named \"" + pdfName + "\"");
        return null;
    }

    /**
     * Stroke the current path with the current miter limit, color, etc.
     */
    void strokePath()
    {
        boolean setAlpha = _gstate.salpha != _gstate.alpha;
        if (setAlpha) _pntr.setOpacity(_gstate.salpha);

        _pntr.setColor(_gstate.scolor);
        _pntr.setStroke(_gstate.getStroke());
        _pntr.draw(_path);

        if (setAlpha) _pntr.setOpacity(_gstate.alpha);
    }

    /**
     * Fill the current path using the fill params in the gstate
     */
    void fillPath()
    {
        _pntr.setPaint(_gstate.color);
        _pntr.fill(_path);
    }

    /**
     * Establishes an image transform and tells markup engine to draw the image
     */
    public void drawImage(java.awt.Image anImg)
    {
        // In pdf, an image is defined as occupying the unit square no matter how many pixels wide or high
        // it is (image space goes from {0,0} - {1,1}). A pdf producer will scale up ctm to get whatever size they want.
        // We remove pixelsWide & pixelsHigh from scale since awt image space goes from {0,0} - {width,height}
        // Also note that in pdf image space, {0,0} is at the upper-, left.  Since this is flipped from all the other
        // primatives, we also include a flip here for consistency.
        int pixWide = anImg.getWidth(null);
        int pixHigh = anImg.getHeight(null);
        if (pixWide < 0 || pixHigh < 0)
            throw new PDFException("PDFPagePainter: Error loading image"); // Shouldn't happen

        AffineTransform ixform = new AffineTransform(1.0 / pixWide, 0.0, 0.0, -1.0 / pixHigh, 0, 1.0);
        Graphics2D g2d = (Graphics2D) _pntr.getNative();
        g2d.drawImage(anImg, ixform, null); // If fails with ImagingOpException, see RM14 sun_bug_4723021_workaround
    }

    /**
     * Pushes a copy of the current gstate onto the gstate stack and returns the new gstate.
     */
    PDFGState gsave()
    {
        PDFGState newstate = (PDFGState) _gstate.clone();
        _gstates.push(newstate);
        _pntr.save();
        return _gstate = newstate;
    }

    /**
     * Pops the current gstate from the gstate stack and returns the restored gstate.
     */
    PDFGState grestore()
    {
        // Pop last GState (but get it's clip)
        _gstate = _gstates.peek();
        _pntr.restore();
        return _gstate;
    }

}