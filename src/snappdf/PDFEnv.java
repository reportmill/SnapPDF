package snappdf;
import snap.geom.Rect;
import snap.gfx.Image;
import snap.gfx.Painter;
import snap.util.SnapUtils;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A class to allow certain functionality to be pluggable depending on platform (desktop/web).
 */
public class PDFEnv {

    // The shared instance
    private static PDFEnv _shared;

    /**
     * Generates and returns a unique file identifier.
     */
    public byte[] getFileID(PDFFile aFile)
    {
        byte fileId[] = new byte[16];
        new Random().nextBytes(fileId);
        return fileId;
    }

    /**
     * Set everything to the default implementations and return an Image for this page.
     */
    public Image getImage(PDFPage aPage)
    {
        return null;
    }

    /**
     * Paints the page to given painter, scaled to fit given rectangle.
     */
    public void paint(PDFPage aPage, Painter aPntr, Rect aRect)
    {
        System.err.println("PDFEnv.paint: Not implemented");
    }

    /**
     * Creates a new new PDF encryptor. Both the owner and user passwords are optional.
     */
    public PDFCodec newEncryptor(byte fileID[], String ownerP, String userP, int permissionFlags)
    {
        System.err.println("PDFEnv.newEncryptor: Not implemented");
        return null;
    }

    /**
     * Returns an instance of the appropriate PDFCodec.
     */
    public PDFCodec getEncryptor(Map encryptionDict, List<String> fileID, double pdfversion)
    {
        System.err.println("PDFEnv.getEncryptor: Not implemented");
        return null;
    }

    /**
     * Returns the shared instance.
     */
    public static PDFEnv getEnv()
    {
        // If already set, just return
        if (_shared != null) return _shared;

        // Use generic for TEAVM, otherwise Swing version
        String cname = SnapUtils.getPlatform() == SnapUtils.Platform.TEAVM ? "snappdf.PDFEnv" : "snappdf.PDFEnvSwing";

        // Try to get/set class name instance
        try {
            return _shared = (PDFEnv) Class.forName(cname).newInstance();
        }
        catch (Exception e) {
            System.err.println("PDFEnv.getEnv: Can't set env: " + cname + ", " + e);
            return _shared = new PDFEnv();
        }
    }
}
