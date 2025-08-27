/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snappdf.read;
import java.util.*;
import snap.gfx.*;
import snappdf.PDFException;

/**
 * Current settings in a page.
 */
public class PDFGState implements Cloneable {

    // The current point X/Y
    double currentPointX, currentPointY;

    // The current color
    Color color = Color.BLACK;

    // The current color space
    ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);

    // The current color rendering intent
    int renderingIntent = RelativeColorimetricIntent;

    // The current stroke color
    Color scolor = Color.BLACK;

    // The current stroke color space
    ColorSpace scolorSpace = colorSpace;

    // The transparency parameters
    int blendMode = PDFComposite.NormalBlendMode;
    boolean alphaIsShape = false;
    float alpha = 1;  // non-stroke alpha
    float salpha = 1; // stroke alpha
    Object softMask = null;

    // The current stroke parameters
    float lineWidth = 1;
    int lineCap = 0;
    int lineJoin = 0;
    float miterLimit = 10;
    float[] lineDash = null;
    float dashPhase = 0;
    float flatness = 0;

    // A Stroke representation of the above
    Stroke stroke = new Stroke(1, Stroke.Cap.Square, Stroke.Join.Miter, 10);

    // The current font dictionary
    Map font;

    // The current font size
    float fontSize = 12;

    // The current text character spacing
    float tcs = 0;

    // The current text word spacing
    float tws = 0;

    // The current text leading
    float tleading = 0;

    // The curent text rise
    float trise = 0;

    // Text horizontal scale factor (in PDF "Tz 100" means scale=1)
    float thscale = 1;

    // The text rendering mode
    int trendermode = 0;

    // Text rendering mode constants
    public final int PDFFillTextMode = 0;
    public final int PDFStrokeTextMode = 1;
    public final int PDFFillStrokeMode = 2;
    public final int PDFInvisibleTextMode = 3;
    public final int PDFFillClipTextMode = 4;
    public final int PDFStrokeClipTextTextMode = 5;
    public final int PDFFillStrokeClipTextMode = 6;
    public final int PDFClipTextMode = 7;

    // Line Cap constants
    public static final int PDFButtLineCap = 0;
    public static final int PDFRoundLineCap = 1;
    public static final int PDFSquareLineCap = 2;

    // Line Join constants
    public static final int PDFMiterJoin = 0;
    public static final int PDFRoundJoin = 1;
    public static final int PDFBevelJoin = 2;

    // Rendering intents constants
    public static final int AbsoluteColorimetricIntent = 0;
    public static final int RelativeColorimetricIntent = 1;
    public static final int SaturationIntent = 2;
    public static final int PerceptualIntent = 3;

    /**
     * Creates a Stroke from GState settings.
     */
    public Stroke getStroke()
    {
        // If stroke already set, just return
        if (stroke != null) return stroke;

        // Convert from pdf constants to awt constants
        PDFGState gs = this;
        Stroke.Cap cap = switch (gs.lineCap) {
            case PDFButtLineCap -> Stroke.Cap.Butt;
            case PDFRoundLineCap -> Stroke.Cap.Round;
            case PDFSquareLineCap -> Stroke.Cap.Square;
            default -> Stroke.Cap.Square;
        };

        Stroke.Join join = switch (gs.lineJoin) {
            case PDFMiterJoin -> Stroke.Join.Miter;
            case PDFRoundJoin -> Stroke.Join.Round;
            case PDFBevelJoin -> Stroke.Join.Bevel;
            default -> Stroke.Join.Round;
        };

        // Create stroke
        return stroke = new Stroke(gs.lineWidth, cap, join, gs.miterLimit, gs.lineDash, gs.dashPhase);
    }

    /**
     * Standard clone implementation.
     */
    @Override
    public PDFGState clone()
    {
        try { return (PDFGState) super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
    }

    public static int getRenderingIntentID(String pdfName)
    {
        return switch (pdfName) {
            case "/AbsoluteColorimetric" -> AbsoluteColorimetricIntent;
            case "/RelativeColorimetric" -> RelativeColorimetricIntent;
            case "/Saturation" -> SaturationIntent;
            case "/Perceptual" -> PerceptualIntent;
            default -> throw new PDFException("Unknown rendering intent name \"" + pdfName + "\"");
        };
    }
}