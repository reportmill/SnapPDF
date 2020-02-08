package snappdf;
import snap.swing.AWT;
import snappdf.write.PDFPageWriter;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * A Graphics2D implementation for SnapPDF.
 */
public class PDFGraphics2D extends Graphics2D {

    // The PDFWriter
    private PDFGWriter _writer;

    /**
     * Override to write to PDFWriter.
     */
    @Override
    public void draw(Shape aShape)
    {
        // Get PDF page and write stroke path
        PDFPageWriter pdfPage = _writer.getPageWriter();
        pdfPage.writePath(AWT.awtToSnapShape(aShape));

        // Set stroke color and width
        //pdfPage.setStrokeColor(AWT.get(aColor));
        //pdfPage.setStrokeWidth(aStroke.getWidth());

        // Write dash array
        //if(aStroke.getDashArray()!=null && aStroke.getDashArray().length>1)
        //    pdfPage.append('[').append(snap.gfx.Stroke.getDashArrayString(aStroke.getDashArray(), " ")).append("] ")
        //            .append(aStroke.getDashOffset()).appendln(" d");

        // Write stroke operator
        pdfPage.appendln("S");
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs)
    {
        return false;
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y)
    {

    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform)
    {

    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform)
    {

    }

    @Override
    public void drawString(String str, int x, int y)
    {

    }

    @Override
    public void drawString(String str, float x, float y)
    {

    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y)
    {

    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer)
    {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer)
    {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer)
    {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer)
    {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer)
    {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer)
    {
        return false;
    }

    @Override
    public void dispose()  { }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y)
    {

    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y)
    {

    }

    @Override
    public void fill(Shape s)
    {

    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke)
    {
        return false;
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration()
    {
        return null;
    }

    @Override
    public void setComposite(Composite comp)
    {

    }

    @Override
    public void setPaint(Paint paint)
    {

    }

    @Override
    public void setStroke(Stroke s)
    {

    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue)
    {

    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey)
    {
        return null;
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints)
    {

    }

    @Override
    public void addRenderingHints(Map<?, ?> hints)
    {

    }

    @Override
    public RenderingHints getRenderingHints()
    {
        return null;
    }

    @Override
    public Graphics create()
    {
        return null;
    }

    @Override
    public void translate(int x, int y)
    {

    }

    @Override
    public Color getColor()
    {
        return null;
    }

    @Override
    public void setColor(Color c)
    {

    }

    @Override
    public void setPaintMode()
    {

    }

    @Override
    public void setXORMode(Color c1)
    {

    }

    @Override
    public Font getFont()
    {
        return null;
    }

    @Override
    public void setFont(Font font)
    {

    }

    @Override
    public FontMetrics getFontMetrics(Font f)
    {
        return null;
    }

    @Override
    public Rectangle getClipBounds()
    {
        return null;
    }

    @Override
    public void clipRect(int x, int y, int width, int height)
    {

    }

    @Override
    public void setClip(int x, int y, int width, int height)
    {

    }

    @Override
    public Shape getClip()
    {
        return null;
    }

    @Override
    public void setClip(Shape clip)
    {

    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy)
    {

    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2)
    {

    }

    @Override
    public void fillRect(int x, int y, int width, int height)
    {

    }

    @Override
    public void clearRect(int x, int y, int width, int height)
    {

    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {

    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {

    }

    @Override
    public void drawOval(int x, int y, int width, int height)
    {

    }

    @Override
    public void fillOval(int x, int y, int width, int height)
    {

    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {

    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {

    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints)
    {

    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints)
    {

    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints)
    {

    }

    @Override
    public void translate(double tx, double ty)
    {

    }

    @Override
    public void rotate(double theta)
    {

    }

    @Override
    public void rotate(double theta, double x, double y)
    {

    }

    @Override
    public void scale(double sx, double sy)
    {

    }

    @Override
    public void shear(double shx, double shy)
    {

    }

    @Override
    public void transform(AffineTransform Tx)
    {

    }

    @Override
    public void setTransform(AffineTransform Tx)
    {

    }

    @Override
    public AffineTransform getTransform()
    {
        return null;
    }

    @Override
    public Paint getPaint()
    {
        return null;
    }

    @Override
    public Composite getComposite()
    {
        return null;
    }

    @Override
    public void setBackground(Color color)
    {

    }

    @Override
    public Color getBackground()
    {
        return null;
    }

    @Override
    public Stroke getStroke()
    {
        return null;
    }

    @Override
    public void clip(Shape s)
    {

    }

    @Override
    public FontRenderContext getFontRenderContext()
    {
        return null;
    }
}
