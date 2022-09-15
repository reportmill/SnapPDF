/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snappdf.write;
import java.util.*;

import snap.gfx.*;
import snappdf.*;

/**
 * A class to write images.
 */
public class PDFWriterImage {

    /**
     * Writes the PDF to embed the actual image bytes.
     */
    public static void writeImage(PDFWriter aWriter, Image anImage)
    {
        // Get image bytes: If JPG use jpg file bytes, otherwise get bytes RGB
        boolean isJPG = anImage.getType().equals("jpg");
        byte bytes[] = isJPG ? anImage.getBytes() : anImage.getBytesRGB();

        // Get image color space and whether image is jpg
        String colorspace = "/DeviceRGB";
        int bps = 8;
        if (isJPG) {
            ImageUtils.ImageInfo info = ImageUtils.getInfoJPG(bytes);
            bps = info.bps;
            if (info.spp != 3) colorspace = info.spp == 2 ? "/DeviceGray" : "/DeviceCMYK";
        }

        // Create image dictionary
        Map imageDict = new Hashtable(8);
        imageDict.put("Type", "/XObject");
        imageDict.put("Subtype", "/Image");
        imageDict.put("Name", "/" + aWriter.getImageName(anImage));
        imageDict.put("Width", anImage.getPixWidth());
        imageDict.put("Height", anImage.getPixHeight());
        imageDict.put("BitsPerComponent", bps);
        imageDict.put("ColorSpace", colorspace);

        // If JPG CMYK, put in bogus decode entry
        if (isJPG && colorspace.equals("/DeviceCMYK"))
            imageDict.put("Decode", "[1 0 1 0 1 0 1 0]");

        // If image has alpha channel, create soft-mask dictionary
        byte alpha[] = anImage.hasAlpha() ? getBytesAlpha8(anImage) : null;
        if (alpha != null) {

            // Create soft-mask dict with basic attributes
            Map softMask = new Hashtable();
            softMask.put("Type", "/XObject");
            softMask.put("Subtype", "/Image");
            softMask.put("Width", anImage.getPixWidth());
            softMask.put("Height", anImage.getPixHeight());
            softMask.put("BitsPerComponent", 8);
            softMask.put("ColorSpace", "/DeviceGray");

            // Create alpha bytes stream, xref and add to parent image dict
            PDFStream smask = new PDFStream(alpha, softMask);
            String smaskXRef = aWriter.getXRefTable().addObject(smask);
            imageDict.put("SMask", smaskXRef);
        }

        // Create stream for image (bytes + info dict) and write. If JPG, add DCTDecode filter.
        PDFStream istream = new PDFStream(bytes, imageDict);
        if (isJPG) istream.addFilter("/DCTDecode");
        aWriter.writeStream(istream);
    }

    /**
     * Returns the image data's raw alpha bytes as byte array.
     */
    private static byte[] getBytesAlpha8(Image anImage)
    {
        // Get samples per pixel, bits per sample, whether image is color and bytes
        byte rgba[] = anImage.getBytesRGBA();
        int len = rgba.length / 4;
        byte alpha[] = new byte[len];
        boolean allOpaque = true;
        for (int i = 0; i < len; i++) {
            alpha[i] = rgba[i * 4 + 3];
            if (allOpaque && alpha[i] != -1) allOpaque = false;
        }
        return allOpaque ? null : alpha;
    }

}