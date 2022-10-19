package snappdf;
import java.util.*;

import snappdf.write.PDFPageTree;

/**
 * Represents a PDF file.
 */
public class PDFFile {

    // The PDF version being generated
    protected double _version = 1.4f;

    // The XRefTable
    public PDFXTable _xtable;

    // Encyption dictionary
    protected PDFCodec _securityHandler;

    // The reader
    protected PDFReader _reader;

    // The trailer dictionary
    protected Map _trailer;

    // Info dict
    public Map<String, String> _infoDict = new Hashtable(4);

    // Catalog dict
    public Map _catalogDict = new Hashtable(4);

    // The PDF file pages
    public Map _pagesDict;

    // Cached PDFPage instances
    Map<Integer, PDFPage> _pages = new Hashtable(4);

    // File identifier
    private byte _fileId[] = null;
    protected List<String> _fileIds;

    // Pages tree
    public PDFPageTree _pageTree;

    /**
     * Creates a new PDFFile.
     */
    public PDFFile()
    {
    }

    /**
     * Creates a new PDFFile.
     */
    public PDFFile(byte theBytes[])
    {
        _reader = new PDFReader(this, theBytes);
        _reader.readFile();
    }

    /**
     * Gets the pdf version as a float.
     */
    public double getVersion()
    {
        return _version;
    }

    /**
     * Sets the version of the pdf being generated.
     * Current default is 1.4, but generated pdf is really 1.2 unless there is image alpha, annotations, patterns or
     * encryption (for 128 bit key).
     */
    public void setVersion(double aVersion)
    {
        _version = Math.max(_version, aVersion);
    }

    /**
     * Returns the version of pdf being generated.
     */
    public String getVersionString()
    {
        int major = (int) _version, minor = (int) Math.round((_version - major) * 10);
        return "PDF-" + major + '.' + minor;
    }

    /**
     * Gets the pdf version as a float.
     */
    public void setVersionString(String aStr)
    {
        // parser guarantees that this string looks like %PDF-xxxx
        try {
            setVersion(Double.parseDouble(aStr.substring(5)));
        }
        catch (NumberFormatException nfe) {
            throw new PDFException("Illegal PDF version header");
        }
    }

    /**
     * Returns the cross reference table.
     */
    public PDFXTable getXRefTable()
    {
        return _xtable;
    }

    /**
     * Returns the list of PDFEntry objects from XRef table.
     */
    public List<PDFXEntry> getXRefs()
    {
        return _xtable.getXRefs();
    }

    /**
     * Returns the individual XRef at given index.
     */
    public PDFXEntry getXRef(int anIndex)
    {
        return _xtable.getXRef(anIndex);
    }

    /**
     * Given an object, check to see if its an indirect reference - if so, resolve the reference.
     */
    public Object getXRefObj(Object anObj)
    {
        return _xtable.getXRefObj(anObj);
    }

    /**
     * Returns the PDF reader.
     */
    public PDFReader getReader()
    {
        return _reader;
    }

    /**
     * Returns the PDF reader bytes.
     */
    public byte[] getBytes()
    {
        return _reader.getBytes();
    }

    /**
     * Returns the trailer dictionary.
     */
    public Map getTrailer()
    {
        return _trailer;
    }

    /**
     * Returns the number of pages in this file.
     */
    public int getPageCount()
    {
        Object obj = _pagesDict.get("Count");
        return (Integer) getXRefObj(obj); // Can Count really be a reference?
    }

    /**
     * Returns an individual PDF page for the given page index.
     */
    public PDFPage getPage(int aPageIndex)
    {
        PDFPage page = _pages.get(aPageIndex);
        if (page == null)
            _pages.put(aPageIndex, page = new PDFPage(this, aPageIndex));
        return page;
    }

    /**
     * Clears the page cache.
     */
    public void clearPageCache()
    {
        _pages.clear();
    }

    /**
     * Returns the PDF file's info dictionary.
     */
    public Map<String, String> getInfoDict()
    {
        return _infoDict;
    }

    /**
     * Returns the catalog dictionary.
     */
    public Map getCatalogDict()
    {
        return _catalogDict;
    }

    /**
     * Returns the PDF file's pages tree.
     */
    public PDFPageTree getPagesTree()
    {
        return _pageTree;
    }

    /**
     * Returns the author of the pdf file.
     */
    public String getAuthor()
    {
        return _infoDict.get("Author");
    }

    /**
     * Sets the author of the pdf file.
     */
    public void setAuthor(String aStr)
    {
        addInfoDictValue("Author", aStr);
    }

    /**
     * Returns the creator of the pdf file.
     */
    public String getCreator()
    {
        return _infoDict.get("Creator");
    }

    /**
     * Sets the creator of the pdf file.
     */
    public void setCreator(String aStr)
    {
        addInfoDictValue("Creator", aStr);
    }

    /**
     * Adds an InfoDict value.
     */
    public void addInfoDictValue(String aKey, String aValue)
    {
        String str = aValue;
        if (!str.startsWith("("))
            str = "(" + str + ")";
        _infoDict.put(aKey, str);
    }

    /**
     * Generates and returns a unique file identifier.
     */
    public byte[] getFileID()
    {
        if (_fileId != null) return _fileId;
        return _fileId = PDFEnv.getEnv().getFileID(this);
    }

    /**
     * Returns the file identifier as a hex string.
     */
    public String getFileIDString()
    {
        byte id_bytes[] = getFileID();
        StringBuffer sb = new StringBuffer("<");
        for (int i = 0, iMax = id_bytes.length; i < iMax; i++) {
            int c1 = (id_bytes[i] >> 4) & 0xf;
            sb.append((char) (c1 < 10 ? '0' + c1 : 'a' + (c1 - 10)));
            int c2 = id_bytes[i] & 0xf;
            sb.append((char) (c2 < 10 ? '0' + c2 : 'a' + (c2 - 10)));
        }
        sb.append('>');
        return sb.toString();
    }
}