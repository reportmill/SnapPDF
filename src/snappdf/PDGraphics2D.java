package snappdf;
import snap.swing.AWT;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * A Graphics2D implementation to generate PDF from Swing apps (Uses PDPainter).
 */
public class PDGraphics2D extends Graphics2D {

    // The PDPainter
    private PDPainter _painter;

    @Override
    public Color getColor()
    {
        return AWT.snapToAwtColor(_painter.getColor());
    }

    @Override
    public void setColor(Color aColor)
    {
        _painter.setColor(AWT.awtToSnapColor(aColor));
    }

    @Override
    public Paint getPaint()
    {
        return AWT.snapToAwtPaint(_painter.getPaint());
    }

    @Override
    public void setPaint(Paint aPaint)
    {
        _painter.setPaint(AWT.awtToSnapPaint(aPaint));
    }

    @Override
    public Stroke getStroke()
    {
        return AWT.snapToAwtStroke(_painter.getStroke());
    }

    @Override
    public void setStroke(Stroke aStroke)
    {
        _painter.setStroke(AWT.awtToSnapStroke(aStroke));
    }

    @Override
    public Font getFont()
    {
        return AWT.snapToAwtFont(_painter.getFont());
    }

    @Override
    public void setFont(Font aFont)
    {
        _painter.setFont(AWT.awtToSnapFont(aFont));
    }

    @Override
    public FontMetrics getFontMetrics(Font aFont)
    {
        return null;
    }

    @Override
    public AffineTransform getTransform()
    {
        return AWT.snapToAwtTrans(_painter.getTransform());
    }

    @Override
    public void transform(AffineTransform aTrans)
    {
        _painter.transform(AWT.awtToSnapTrans(aTrans));
    }

    @Override
    public void setTransform(AffineTransform aTrans)
    {
        _painter.setTransform(AWT.awtToSnapTrans(aTrans));
    }

    @Override
    public void translate(int x, int y)
    {
        _painter.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty)
    {
        _painter.translate(tx, ty);
    }

    @Override
    public void rotate(double theta)
    {
        _painter.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y)
    {
        _painter.rotateAround(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy)
    {
        _painter.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy)
    {
        System.err.println("PDGraphics2D.shear: Not implemented");
    }

    @Override
    public Shape getClip()
    {
        return AWT.snapToAwtShape(_painter.getClip());
    }

    @Override
    public Rectangle getClipBounds()
    {
        return AWT.snapToAwtRect(_painter.getClipBounds()).getBounds();
    }

    @Override
    public void clip(Shape aShape)
    {
        _painter.clip(AWT.awtToSnapShape(aShape));
    }

    @Override
    public void clipRect(int x, int y, int width, int height)
    {
        _painter.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height)
    {
        System.err.println("PDGraphics2D.setClip: Not implemented");
    }

    @Override
    public void setClip(Shape clip)
    {
        System.err.println("PDGraphics2D.setClip: Not implemented");
    }

    /**
     * Override to write to PDFWriter.
     */
    @Override
    public void draw(Shape aShape)
    {
        _painter.draw(AWT.awtToSnapShape(aShape));
    }

    @Override
    public void fill(Shape aShape)
    {
        _painter.fill(AWT.awtToSnapShape(aShape));
    }

    @Override
    public void drawString(String str, int x, int y)
    {
        _painter.drawString(str, x, y);
    }

    @Override
    public void drawString(String str, float x, float y)
    {
        _painter.drawString(str, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y)
    {
        drawString(iterator, (float)x, (float)y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y)
    {
        System.err.println("PDGraphics2D.drawString AttribChar: Not implemented");
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y)
    {

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
    public void drawRenderedImage(RenderedImage img, AffineTransform xform)
    {

    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform)
    {

    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        _painter.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(int x, int y, int width, int height)
    {
        _painter.fillRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height)
    {
        _painter.clearRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {
        RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight);
        draw(rect);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {
        RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight);
        fill(rect);
    }

    @Override
    public void drawOval(int x, int y, int width, int height)
    {
        Ellipse2D oval = new Ellipse2D.Double(x, y, width, height);
        draw(oval);
    }

    @Override
    public void fillOval(int x, int y, int width, int height)
    {
        Ellipse2D oval = new Ellipse2D.Double(x, y, width, height);
        fill(oval);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {
        Arc2D arc = new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN);
        draw(arc);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {
        Arc2D arc = new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN);
        fill(arc);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints)
    {
        Polygon poly = new Polygon(xPoints, yPoints, nPoints);
        draw(poly);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints)
    {
        Polygon poly = new Polygon(xPoints, yPoints, nPoints);
        draw(poly);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints)
    {
        Polygon poly = new Polygon(xPoints, yPoints, nPoints);
        fill(poly);
    }

    @Override
    public Composite getComposite()
    {
        return null;
    }

    @Override
    public void setComposite(Composite comp)  { }

    @Override
    public Color getBackground()
    {
        return null;
    }

    @Override
    public void setBackground(Color color)
    {
        System.err.println("PDGraphics2D.setBackground: Not implemented");
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy)
    {
        System.err.println("PDGraphics2D.copyArea: Not implemented");
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey)
    {
        return null;
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue)
    {

    }

    @Override
    public RenderingHints getRenderingHints()  { return null; }

    @Override
    public void setRenderingHints(Map<?, ?> hints)  { }

    @Override
    public void addRenderingHints(Map<?, ?> hints)  {  }

    @Override
    public Graphics create()
    {
        return null;
    }

    @Override
    public void setPaintMode()  { }

    @Override
    public void setXORMode(Color c1)  { }

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
    public FontRenderContext getFontRenderContext()
    {
        return null;
    }

    @Override
    public void dispose()  { }
}
