package snappdf.read;
import java.io.InputStream;
import java.util.*;

import snap.gfx.ColorSpace;
import snappdf.*;
import snappdf.read.PDFColorSpaces.*;

/**
 * Color/ColorSpace utility methods for PDF.
 */
public class PDFColorSpace {

    static ColorSpace _cmykSpace = null;

    /**
     * Colorspace constants
     */
    public static final int DeviceGrayColorspace = 1;
    public static final int CalibratedGrayColorspace = 2;
    public static final int DeviceRGBColorspace = 3;
    public static final int CalibratedRGBColorspace = 4;
    public static final int DeviceCMYKColorspace = 5;
    public static final int LabColorspace = 6;
    public static final int IndexedColorspace = 7;
    public static final int ICCBasedColorspace = 8;
    public static final int SeparationColorspace = 9;
    public static final int DeviceNColorspace = 10;
    public static final int PatternColorspace = 11;
    public static final int UnknownColorspace = -1;

    /**
     * Create a colorspace object from one of the above space IDs.
     * <p>
     * The value of "params" can be as follows:
     * Device spaces - ignored
     * CIE spaces -  a Map
     * ICC spaces -  a PDF Stream
     * Indexed spaces - a Map with keys 'base", "hival", and "lookup"
     * Pattern - null
     * Separation - a Map with "Colorant", "Base", & "TintTransform"
     * DeviceN - - a Map with "Colorants", "Base", "TintTransform", & "Attributes"
     * <p>
     * A colorspace can be specified in several ways. If a colorspace needs no arguments, in can just be the name.
     * Otherwise, it is an entry in the page's resource dictionary. The format of the object in the resource dictionary
     * is dependent upon the type of the colorspace. Once we figure out what kind of colorspace it is and the parameters,
     * we can call the ColorFactory to make an awt ColorSpace object.
     * <p>
     * Note that colorspaces in pdf are usually specified as strings (for built-ins) or arrays (eg. [/CalRGB << ... >>])
     */
    public static ColorSpace getColorspace(Object csobj, PDFPage page)
    {
        int type = -1;
        Object params = null;
        List cslist = null;

        // Strings are either a name of a built in color space or the name of a colorspace resource
        if (csobj instanceof String) {
            String pdfName = (String) csobj;

            // TODO:This shouldn't really be necessary
            if (pdfName.charAt(0) == '/')
                pdfName = pdfName.substring(1);

            // All Device spaces are subject to possible remapping through a Default space
            if (pdfName.startsWith("Device")) {
                String defName = "Default" + pdfName.substring(6);
                Object resource = page.findResource("ColorSpace", defName);
                if (resource != null)
                    return getColorspace(resource, page);
            }

            // Device... & Pattern are all fully specified by the name
            if (pdfName.equals("DeviceGray")) type = PDFColorSpace.DeviceGrayColorspace;
            else if (pdfName.equals("DeviceRGB")) type = PDFColorSpace.DeviceRGBColorspace;
            else if (pdfName.equals("DeviceCMYK")) type = PDFColorSpace.DeviceCMYKColorspace;
            else if (pdfName.equals("Pattern")) type = PDFColorSpace.PatternColorspace;
            else {
                // Look up the name in the resource dictionary and try again
                Object resource = page.findResource("ColorSpace", pdfName);
                if (resource != null)
                    return getColorspace(resource, page);
            }
        }

        else if (csobj instanceof List) {
            cslist = (List) csobj;
            // The usual format is [/SpaceName obj1 obj2...]
            // We do color space cacheing by adding the colorspace as the last object in the list.
            // The normal map cacheing strategy is to add an element with key _rbcached_..., but Adobe, in their inifinite
            // wisdom, decided that color spaces should be arrays instead of dictionaries, like everything else.
            // TODO:  Make sure not to export this extra element when saving pdf
            Object cachedObj = cslist.get(cslist.size() - 1);
            if (cachedObj instanceof ColorSpace)
                return (ColorSpace) cachedObj;

            String pdfName = ((String) cslist.get(0)).substring(1);
            params = cslist.size() > 1 ? page.getXRefObj(cslist.get(1)) : null;

            if (pdfName.equals("CalGray")) type = PDFColorSpace.CalibratedGrayColorspace;
            else if (pdfName.equals("CalRGB")) type = PDFColorSpace.CalibratedRGBColorspace;
            else if (pdfName.equals("Lab")) type = PDFColorSpace.LabColorspace;
            else if (pdfName.equals("ICCBased")) type = PDFColorSpace.ICCBasedColorspace;
            else if (pdfName.equals("Pattern")) {
                type = PDFColorSpace.PatternColorspace;
                if (params != null) params = getColorspace(params, page);
            }
            else if (pdfName.equals("Separation")) {
                type = PDFColorSpace.SeparationColorspace;
                Map paramDict = new Hashtable(2);
                paramDict.put("Colorant", cslist.get(1));
                paramDict.put("Base", getColorspace(page.getXRefObj(cslist.get(2)), page));
                paramDict.put("TintTransform", PDFFunction.getInstance(page.getXRefObj(cslist.get(3)), page.getFile()));
                params = paramDict;
            }
            else if (pdfName.equals("DeviceN")) {
                type = PDFColorSpace.DeviceNColorspace;
                Map paramDict = new Hashtable(2);
                paramDict.put("Colorants", page.getXRefObj(cslist.get(1)));
                paramDict.put("Base", getColorspace(page.getXRefObj(cslist.get(2)), page));
                paramDict.put("TintTransform", PDFFunction.getInstance(page.getXRefObj(cslist.get(3)), page.getFile()));
                if (cslist.size() > 4)
                    paramDict.put("Attributes", page.getXRefObj(cslist.get(4)));
                params = paramDict;
            }

            else if (pdfName.equals("Indexed")) {
                type = PDFColorSpace.IndexedColorspace;
                //  [/Indexed basecolorspace hival <hex clut>]
                // params set above is the base colorspace. Turn it into a real colorspace
                // NB: this is recursive and there's no check for this illegal sequence, which causes infinite recursion:
                //   8 0 obj [/Indexed  8 0 R  1 <FF>] endobj
                // Also note that in the time it took to write this comment, you could have put in a check for this case.
                if (cslist.size() != 4)
                    throw new PDFException("Wrong number of elements in colorspace definition");

                if (params instanceof String && (((String) params).charAt(0) == '/'))
                    params = ((String) params).substring(1);

                ColorSpace base = getColorspace(params, page);
                Object hival = cslist.get(2);
                byte lookup_table[];

                if (!(hival instanceof Number))
                    throw new PDFException("Illegal Colorspace definition " + cslist);

                // The lookuptable is next
                Object val = page.getXRefObj(cslist.get(3));
                if (val instanceof PDFStream)
                    lookup_table = ((PDFStream) val).decodeStream();

                else if (val instanceof String) {
                    // NB: For historical reasons, PDFReader doesn't interpret string as hex, but just leaves it alone.
                    // It probably makes sense to move much of this parsing back into PDFReader and let javacc handle it.
                    lookup_table = PageToken.getPDFHexString((String) val);
                }

                // In the case of inline images, the pagepainter has already done the conversion.
                else if (val instanceof byte[])
                    lookup_table = (byte[]) val;

                else throw new PDFException("Can't read color lookup table");

                // Build a dictionary to pass to the colorFactory
                Map paramDict = new Hashtable(3);
                paramDict.put("Base", base);
                paramDict.put("HiVal", hival);
                paramDict.put("Lookup", lookup_table);
                params = paramDict;
            }
        }

        // If ColorSpace not found, complain
        if (type == -1) throw new PDFException("Unknown colorspace : " + csobj);

        // Create ColorSpace, cache and return
        ColorSpace outerSpace = createColorSpace(type, params);
        if (cslist != null) cslist.add(outerSpace);
        return outerSpace;
    }

    /**
     * Get cached generic deviceCMYK space. Should probably cache all, but since this so big, we'll start here.
     */
    private static ColorSpace getDeviceCMYK()
    {
        // If already set, just return
        if (_cmykSpace != null) return _cmykSpace;

        // Get profile file stream and create/return space
        InputStream s = PDFColorSpace.class.getResourceAsStream("CMYK.icc");
        return _cmykSpace = ColorSpace.createColorSpaceICC(s);
    }

    /**
     * Create ColorSpace from type and/or Map.
     */
    private static ColorSpace createColorSpace(int type, Object params)
    {
        //TODO: recheck this mapping
        switch (type) {

            // The device spaces
            case DeviceGrayColorspace:
                return ColorSpace.getInstance(ColorSpace.CS_GRAY);
            case DeviceRGBColorspace:
                return DeviceRGB.get();
            case DeviceCMYKColorspace:
                return getDeviceCMYK();

            // The CIE spaces. TODO: Get appropriate .pf files for these to match the spec
            case CalibratedGrayColorspace:
                return createColorSpace(DeviceGrayColorspace, null);
            case CalibratedRGBColorspace:
                return createColorSpace(DeviceRGBColorspace, null);

            // ICC Based space - a CIE space that is specified in the pdf stream
            case ICCBasedColorspace:
                PDFStream s = (PDFStream) params;
                Map iccinfo = s.getDict();

                // Create ColorSpace from ICC data
                ColorSpace cspace = null;
                try {
                    byte iccdata[] = s.decodeStream();
                    cspace = ColorSpace.createColorSpaceICC(iccdata);
                }
                catch (Exception e) {
                    System.err.println("Error reading colorspace");
                }

                // Sanity check
                if (cspace != null) {
                    Object ncomps = iccinfo.get("N");
                    if (ncomps != null && cspace.getNumComponents() != ((Number) ncomps).intValue())
                        System.err.println("Error reading embedded colorspace.  Wrong number of components.");
                    return cspace;
                }

                // Otherwise check for alternate
                Object alternate = iccinfo.get("Alternate");  //TODO:real implementation
                System.err.println("Couldn't load ICC color space .  Need to use alternate " + alternate);
                break;

            // Handle Indexed ColorSpace
            case IndexedColorspace:
                Map indexdict = (Map) params;
                return new IndexedColorSpace((ColorSpace) indexdict.get("Base"),
                        ((Number) indexdict.get("HiVal")).intValue(), (byte[]) indexdict.get("Lookup"));

            // Handle SeparationColorspace
            case SeparationColorspace:
                Map sepdict = (Map) params;
                return new SeparationColorSpace((String) sepdict.get("Colorant"), (ColorSpace) sepdict.get("Base"),
                        (PDFFunction) sepdict.get("TintTransform"));

            // Handle DeviceNColorspace
            case DeviceNColorspace:
                Map devndict = (Map) params;
                return new DeviceNColorSpace((List) devndict.get("Colorants"), (ColorSpace) devndict.get("Base"),
                        (PDFFunction) devndict.get("TintTransform"), (Map) devndict.get("Attributes"));

            // Handle PatternColorspace
            case PatternColorspace:
                if (params instanceof ColorSpace)
                    return new PatternSpace((ColorSpace) params);
                return new PatternSpace();

            // Handle anything else
            default:
                System.err.println("This is getting boring.  Need to implement colorspace id=" + type);
        }

        // Return a default.  The parser's going to barf if the number of parameters passed to a
        // sc operation doesn't match the number of components in this space.   Don't say you weren't warned.
        return ColorSpace.getInstance(ColorSpace.CS_sRGB);
    }

}