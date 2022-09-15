package snappdf;
import snap.swing.AWT;
import snap.swing.AWTImageUtils;

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
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.util.Stack;

/**
 * A Graphics2D implementation to generate PDF from Swing apps (Uses PDPainter).
 */
public class PDGraphics2D extends Graphics2D {

    // The PDPainter
    private PDPainter _painter;

    // The stack of PDGraphics2D
    private Stack<PDGraphics2D> _gstack;

    // A shared graphics object
    private Graphics2D _g2d;

    /**
     * Creates a PDGraphics2D.
     */
    public PDGraphics2D(int aW, int aH)
    {
        _painter = new PDPainter(aW, aH);
        _gstack = new Stack<>();
        _gstack.add(this);

        _g2d = AWTImageUtils.getBufferedImage(5, 5, true).createGraphics();
    }

    /**
     * Creates a PDGraphics2D with given painter.
     */
    protected PDGraphics2D(PDPainter aPntr, Stack<PDGraphics2D> gStack)
    {
        _painter = aPntr;
        _gstack = gStack;
    }

    /**
     * Returns a PDF byte array for a given RMDocument.
     */
    public byte[] getBytesPDF()
    {
        return _painter.getBytesPDF();
    }

    @Override
    public Color getColor()
    {
        return AWT.snapToAwtColor(_painter.getColor());
    }

    @Override
    public void setColor(Color aColor)
    {
        checkGStack();
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
        checkGStack();
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
        checkGStack();
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
        checkGStack();
        _painter.setFont(AWT.awtToSnapFont(aFont));
        _g2d.setFont(aFont);
    }

    @Override
    public FontMetrics getFontMetrics(Font aFont)
    {
        return _g2d.getFontMetrics(aFont);
    }

    @Override
    public AffineTransform getTransform()
    {
        return AWT.snapToAwtTrans(_painter.getTransform());
    }

    @Override
    public void transform(AffineTransform aTrans)
    {
        checkGStack();
        _painter.transform(AWT.awtToSnapTrans(aTrans));
    }

    @Override
    public void setTransform(AffineTransform aTrans)
    {
        checkGStack();
        _painter.setTransform(AWT.awtToSnapTrans(aTrans));
    }

    @Override
    public void translate(int x, int y)
    {
        checkGStack();
        _painter.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty)
    {
        checkGStack();
        _painter.translate(tx, ty);
    }

    @Override
    public void rotate(double theta)
    {
        checkGStack();
        _painter.rotate(Math.toDegrees(theta));
    }

    @Override
    public void rotate(double theta, double aX, double aY)
    {
        checkGStack();
        _painter.rotateAround(Math.toDegrees(theta), aX, aY);
    }

    @Override
    public void scale(double sx, double sy)
    {
        checkGStack();
        _painter.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy)
    {
        checkGStack();
        System.err.println("PDGraphics2D.shear: Not implemented");
    }

    @Override
    public Shape getClip()
    {
        checkGStack();
        return AWT.snapToAwtShape(_painter.getClip());
    }

    @Override
    public Rectangle getClipBounds()
    {
        checkGStack();
        return AWT.snapToAwtRect(_painter.getClipBounds()).getBounds();
    }

    @Override
    public void clip(Shape aShape)
    {
        checkGStack();
        _painter.clip(AWT.awtToSnapShape(aShape));
    }

    @Override
    public void clipRect(int x, int y, int width, int height)
    {
        checkGStack();
        _painter.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height)
    {
        checkGStack();
        System.err.println("PDGraphics2D.setClip: Not implemented");
    }

    @Override
    public void setClip(Shape clip)
    {
        checkGStack();
        System.err.println("PDGraphics2D.setClip: Not implemented");
    }

    /**
     * Override to write to PDFWriter.
     */
    @Override
    public void draw(Shape aShape)
    {
        checkGStack();
        _painter.draw(AWT.awtToSnapShape(aShape));
    }

    @Override
    public void fill(Shape aShape)
    {
        checkGStack();
        _painter.fill(AWT.awtToSnapShape(aShape));
    }

    @Override
    public void drawString(String str, int x, int y)
    {
        checkGStack();
        _painter.drawString(str, x, y);
    }

    @Override
    public void drawString(String str, float x, float y)
    {
        checkGStack();
        _painter.drawString(str, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y)
    {
        checkGStack();
        drawString(iterator, (float) x, (float) y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y)
    {
        checkGStack();
        System.err.println("PDGraphics2D.drawString: Not implemented for AttributedCharacterIterator");
    }

    @Override
    public void drawGlyphVector(GlyphVector aGV, float aX, float aY)
    {
        checkGStack();

        // Translate to location
        _painter.translate(aX, aY);

        // Iterate over glyphs and fill
        for (int i = 0, iMax = aGV.getNumGlyphs(); i < iMax; i++) {
            Shape glyph = aGV.getGlyphOutline(i);
            fill(glyph);
        }

        // Translate back
        _painter.translate(-aX, -aY);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs)
    {
        checkGStack();
        _painter.drawImage(AWT.awtToSnapImage(img), AWT.awtToSnapTrans(xform));
        return true;
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y)
    {
        checkGStack();
        System.err.println("PDGraphics2D.drawImage: Not implemented for BufferedImageOp");
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer)
    {
        checkGStack();
        _painter.drawImage(AWT.awtToSnapImage(img), x, y);
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer)
    {
        checkGStack();
        _painter.drawImage(AWT.awtToSnapImage(img), x, y, width, height);
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer)
    {
        checkGStack();
        _painter.drawImage(AWT.awtToSnapImage(img), x, y);
        System.err.println("PDGraphics2D.drawImage: Not implemented for background color");
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer)
    {
        checkGStack();
        _painter.drawImage(AWT.awtToSnapImage(img), x, y, width, height);
        System.err.println("PDGraphics2D.drawImage: Not implemented for background color");
        return true;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer)
    {
        checkGStack();
        double sw = sx2 - sx1, sh = sy2 - sy1;
        double dw = dx2 - dx1, dh = dy2 - dy1;
        _painter.drawImage(AWT.awtToSnapImage(img), sx1, sy1, sw, sh, dx1, dy1, dw, dh);
        return true;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer)
    {
        checkGStack();
        double sw = sx2 - sx1, sh = sy2 - sy1;
        double dw = dx2 - dx1, dh = dy2 - dy1;
        _painter.drawImage(AWT.awtToSnapImage(img), sx1, sy1, sw, sh, dx1, dy1, dw, dh);
        System.err.println("PDGraphics2D.drawImage: Not implemented for background color");
        return true;
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform)
    {
        System.err.println("PDGraphics2D.drawImage: Not implemented for RenderedImage");
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform)
    {
        System.err.println("PDGraphics2D.drawImage: Not implemented for RenderedImage");
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        checkGStack();
        _painter.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(int x, int y, int width, int height)
    {
        checkGStack();
        _painter.fillRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height)
    {
        checkGStack();
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
    public void setComposite(Composite comp)
    {
        System.err.println("PDGraphics2D.setComposite: Not implemented");
    }

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
        return _g2d.getRenderingHint(hintKey);
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue)
    {
        _g2d.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public RenderingHints getRenderingHints()
    {
        return _g2d.getRenderingHints();
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints)
    {
        _g2d.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints)
    {
        _g2d.addRenderingHints(hints);
    }

    @Override
    public Graphics create()
    {
        checkGStack();
        PDGraphics2D clone = new PDGraphics2D(_painter, _gstack);
        _painter.save();
        _gstack.add(clone);
        return clone;
    }

    @Override
    public void setPaintMode()
    {
    }

    @Override
    public void setXORMode(Color c1)
    {
        System.err.println("PDGraphics2D.setXORMode: Not implemented");
    }

    @Override
    public boolean hit(Rectangle aRect, Shape aShape, boolean onStroke)
    {
        return aShape.intersects(aRect);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration()
    {
        return _g2d.getDeviceConfiguration();
    }

    @Override
    public FontRenderContext getFontRenderContext()
    {
        return _g2d.getFontRenderContext();
    }

    @Override
    public void dispose()
    {
        if (_gstack.peek() == this) {
            _gstack.pop();
            _painter.restore();
        }
    }

    /**
     * Make sure that current graphics is the one doing the painting.
     */
    protected void checkGStack()
    {
        while (_gstack.peek() != this) {
            _gstack.pop();
            _painter.restore();
        }
    }

    /**
     * Generate PDF for Pageable.
     */
    public static byte[] getPDFForPageable(Pageable aPageable)
    {
        // Get PageCount and first PageFormat
        int pageCount = aPageable.getNumberOfPages();
        PageFormat pageFmt0 = aPageable.getPageFormat(0);

        // Create new PDGraphics2D
        int width = (int) pageFmt0.getWidth();
        int height = (int) pageFmt0.getHeight();
        PDGraphics2D gfx = new PDGraphics2D(width, height);

        // Iterate over pages and paint to Graphics
        for (int i = 0; i < pageCount; i++) {
            // If successive page, add page to writer
            if (i > 0) gfx._painter.getWriter().addPage();

            // Get Printable and PageFormat for page and paint to Graphics
            Printable printable = aPageable.getPrintable(i);
            PageFormat pageFmt = aPageable.getPageFormat(i);
            try {
                printable.print(gfx, pageFmt, i);
            }
            catch (PrinterException e) {
                throw new RuntimeException(e);
            }
        }

        // Return PDF bytes
        return gfx.getBytesPDF();
    }
}
