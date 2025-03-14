/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snappdf;
import java.util.*;

import snap.gfx.*;
import snap.util.ListUtils;
import snap.util.SnapUtils;
import snap.view.*;
import snap.viewx.TextPane;
import snap.web.WebURL;

/**
 * Views a PDF file.
 */
public class XRefView extends ViewOwner {

    // The PDF file source
    Object _src;

    // The PDFFile
    PDFFile _pfile;

    // The page index
    int _pindex;

    // The ImageView
    ImageView _imageView;

    // The XRef BrowserView
    BrowserView _browser;

    // The XRef TextArea
    TextArea _xtextArea;

    // The PDF TextArea
    TextArea _ptextArea;

    /**
     * Creates a new XRefView.
     */
    public XRefView(Object aSource)
    {
        setSource(aSource);
    }

    /**
     * Returns the name.
     */
    public String getName()
    {
        return _name;
    }

    String _name = "Sample.pdf";

    /**
     * Sets the name.
     */
    public void setName(String aString)
    {
        _name = aString;
        if (isUISet()) setViewText("NameLabel", "PDF Inspector:  " + _name);
    }

    /**
     * Returns the source.
     */
    public Object getSource()
    {
        return _src;
    }

    /**
     * Sets the source.
     */
    public void setSource(Object aSource)
    {
        if (aSource == _src) return;
        _src = aSource;

        if (_src instanceof WebURL)
            setName(((WebURL) _src).getFilename());

        // Get source bytes and reset file
        byte bytes[] = SnapUtils.getBytes(_src);
        PDFFile pfile = bytes != null ? new PDFFile(bytes) : null;
        setPDFFile(pfile);
    }

    /**
     * Returns the PDF file.
     */
    public PDFFile getPDFFile()
    {
        return _pfile;
    }

    /**
     * Sets the PDF file.
     */
    public void setPDFFile(PDFFile aFile)
    {
        if (aFile == _pfile) return;
        _pfile = aFile;
        getUI();

        // Set image
        resetImage();

        // Set Browser items
        List<Object> items = new PDFResolver().getChildren(_pfile);
        _browser.setItems(items);
        _browser.setSelIndex(0);

        // Set Text
        String str = getFileString();
        _ptextArea.setText(str);

    }

    /**
     * Resets the image.
     */
    void resetImage()
    {
        // For TeaVM
    /*TextArea text = new TextArea(); text.setFont(new Font("Arial", 92)); text.setFill(Color.WHITE);
    text.setPadding(40,40,40,40); text.setTextColor(Color.GRAY);
    text.setAlign(Pos.CENTER); text.setSize(612,792); text.setWrapText(true);
    String str = "Render\nnot yet available"; text.setText(str);
    Image img = ViewUtils.getImage(text);*/

        Image img = _pfile != null ? _pfile.getPage(_pindex).getImage() : null;
        _imageView.setImage(img);
    }

    /**
     * Returns the page.
     */
    public int getPage()
    {
        return _pindex;
    }

    /**
     * Sets the page.
     */
    public void setPage(int anIndex)
    {
        if (anIndex == _pindex) return;
        _pindex = anIndex;
        resetImage();
    }

    /**
     * Returns the number of pages in file.
     */
    public int getPageCount()
    {
        return _pfile != null ? _pfile.getPageCount() : 0;
    }

    /**
     * Returns the UI.
     */
    protected void initUI()
    {
        // Get TabView
        TabView tabView = getView("TabView", TabView.class);
        tabView.getTab(0).getContent().setBorder(null);
        tabView.getTab(1).getContent().setBorder(null);

        // Configure NameLabel
        Label label = getView("NameLabel", Label.class);
        label.setEffect(new ShadowEffect());
        label.setTextColor(Color.WHITE);
        label.setText("PDF Inspector: Sample.pdf");
        label.getParent().setFill(new Color(.9));
        label.getParent().setBorder(Color.BLACK, 1);

        // Get/configure ImageView
        _imageView = getView("ImageView", ImageView.class); //_imageView.getParent().setPrefSize(820,940);
        _imageView.setFill(Color.WHITE);
        _imageView.setBorder(Color.BLACK, 1);
        _imageView.addEventHandler(e -> imageViewDidMouseRelease());

        // Get/configure BrowserView
        _browser = getView("BrowserView", BrowserView.class);
        _browser.setResolver(new PDFResolver());
        _browser.setPrefColCount(4);

        // Create/add XRef TextArea
        TextPane xtextPane2 = new TextPane();
        View xtextPaneUI2 = xtextPane2.getUI();
        xtextPaneUI2.setGrowHeight(true);
        _xtextArea = xtextPane2.getTextArea();
        _xtextArea.setFont(Font.Arial14);
        SplitView bsplit = getView("BrowserSplit", SplitView.class);
        bsplit.addItem(xtextPaneUI2);

        // Create/add PDFTextArea
        TextPane ptextPane = new TextPane();
        View ptextPaneUI = ptextPane.getUI();
        ptextPaneUI.setGrowWidth(true);
        _ptextArea = ptextPane.getTextArea();
        _ptextArea.setFont(Font.Arial14);
        tabView.addTab("Text", ptextPaneUI);

        // Set PFile
        getUI().addEventHandler(this::handleDragDropEvent, DragDrop);
    }

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        Object item = _browser.getSelItem();
        if (item instanceof TreeNode) item = ((TreeNode) item).content;
        _xtextArea.setText(getEntryText(item));
    }

    /**
     * Called when UI gets DragDrop event.
     */
    private void handleDragDropEvent(ViewEvent anEvent)
    {
        // If no file, just bail
        if (_pfile == null) return;

        // Handle DragDrop
        anEvent.acceptDrag();
        ClipboardData cdata = anEvent.getClipboard().getFiles().get(0);
        if (!cdata.isLoaded()) {
            cdata.addLoadListener(cd -> droppedClipboardFinishedLoading(cdata));
            anEvent.dropComplete();
            return;
        }
        WebURL url = cdata.getSourceURL();
        setSource(url);
        anEvent.dropComplete();
    }

    private void droppedClipboardFinishedLoading(ClipboardData aCD)
    {
        byte[] bytes = aCD.getBytes();
        setName(aCD.getName());
        setSource(bytes);
    }

    private void imageViewDidMouseRelease()
    {
        if (_pfile == null) return;
        int nextPageIndex = (getPage() + 1) % getPageCount();
        setPage(nextPageIndex);
    }

    /**
     * Returns XRef entry text.
     */
    public String getEntryText(Object anItem)
    {
        // If no file, return empty string
        if (_pfile == null) return "";

        // Get item string
        Object item = _pfile.getXRefObj(anItem);

        // Handle Map
        if (item instanceof Map)
            return PDFUtils.getDictString((Map) item);

        // Handle List
        if (item instanceof List) {
            List list = (List) item;
            StringBuffer sb = new StringBuffer("[\n");
            for (Object itm : list) sb.append("    ").append(itm).append('\n');
            sb.append("]");
            return sb.toString();
        }

        // Handle XRef table
        if (item instanceof PDFXTable)
            return _pfile.getReader().getXRefString();

        // Handle stream
        if (item instanceof PDFStream) {
            PDFStream stream = (PDFStream) item;
            StringBuffer sb = new StringBuffer(getEntryText(stream.getDict()));
            String text = stream.getText();
            sb.append("\nstream\n").append(text);
            if (!text.endsWith("\n")) sb.append('\n');
            sb.append("endstream");
            return sb.toString();
        }

        // Handle anything else
        return item != null ? item.toString() : "(null)";
    }

    /**
     * Returns the type string for PDF object.
     */
    public String getTypeString(Object anObj)
    {
        if (anObj instanceof PDFXEntry) {
            PDFXEntry xref = (PDFXEntry) anObj;
            Object obj = _pfile.getXRefObj(xref);
            String str = getTypeString(obj) + " (" + xref.toString() + ")";
            return str;
        }

        if (anObj instanceof PDFStream) {
            PDFStream stream = (PDFStream) anObj;
            Map dict = stream.getDict();
            return getTypeString(dict).replace("Dict", "Stream");
        }

        if (anObj instanceof Map) {
            Map map = (Map) anObj;
            String str = "Dict";
            String type = (String) map.get("Type");
            if (type != null) str = type.substring(1) + ' ' + str;
            String subtype = (String) map.get("Subtype");
            if (subtype != null) str = subtype.substring(1) + ' ' + str;
            return str;
        }
        if (anObj instanceof List) return "Array";
        if (anObj instanceof String) return "String";
        if (anObj instanceof Number) return "Num";
        return null;
    }

    /**
     * Returns the file string.
     */
    public String getFileString()
    {
        // If no file, return empty string
        if (_pfile == null) return "";

        // Get string builder for file bytes
        byte bytes[] = _pfile != null ? _pfile.getBytes() : null;
        String str = bytes != null ? new String(bytes) : "";
        StringBuilder sb = new StringBuilder(str);

        // Strip out binary between "stream" and "endstream" strings
        for (int i = sb.indexOf("stream"); i > 0; i = sb.indexOf("stream", i)) {
            int end = sb.indexOf("endstream", i + 6);
            if (end < 0) break;
            boolean binary = false;
            for (int j = i + 6; j < end; j++)
                if (isBinary(sb.charAt(j))) {
                    binary = true;
                    break;
                }
            if (binary) {
                sb.delete(i + 7, end);
                i = i + 17;
            }
            else i = end + 10;
        }

        // Return string
        return sb.toString();
    }

    /**
     * Returns whether given char is binary.
     */
    static boolean isBinary(char c)
    {
        return Character.isISOControl(c) || !Character.isDefined(c);
    }

    /**
     * Main method.
     */
    public static void main(String args[])
    {
        //snaptea.TV.set();
        // Get default doc source
        Object src = args.length > 0 ? args[0] : null;
        if (src == null) src = WebURL.getURL(XRefView.class, "Sample.pdf");
        WebURL url = WebURL.getURL(src);
        if (url.getFile() == null) url = null;

        // Create Viewer
        XRefView xrv = new XRefView(url);
        xrv.getUI().setPrefSize(1000, 1000);
        xrv.getWindow().setTitle(url.getFilename() + " - " + url.getFilenameSimple());
        //if(SnapUtils.isTeaVM) xrv.getWindow().setMaximized(true);
        xrv.setWindowVisible(true);
    }

    /**
     * A TreeResolver.
     */
    private class PDFResolver extends TreeResolver {

        /**
         * Returns the parent of given item.
         */
        public Object getParent(Object anItem)
        {
            if (anItem instanceof PDFFile) return null;
            if (anItem instanceof PDFXTable) return _pfile;
            if (anItem instanceof PDFXEntry) return _pfile.getXRefTable();
            if (anItem instanceof TreeNode) return ((TreeNode) anItem).parent;
            return false;
        }

        /**
         * Whether given object is a parent (has children).
         */
        public boolean isParent(Object anItem)
        {
            Object item = anItem instanceof TreeNode ? ((TreeNode) anItem).content : anItem;
            if (item instanceof PDFXEntry) {
                PDFXEntry xref = (PDFXEntry) item;
                item = _pfile.getXRefObj(xref);
                if (item instanceof PDFStream)
                    item = ((PDFStream) item).getDict();
            }

            if (anItem instanceof PDFFile) return true;
            if (anItem instanceof PDFXTable) return true;
            if (item instanceof Map) return true;
            if (item instanceof List) return true;
            return false;
        }

        /**
         * Returns the children.
         */
        public List<Object> getChildren(Object aParent)
        {
            Object parent = aParent instanceof TreeNode ? ((TreeNode) aParent).content : aParent;
            if (parent instanceof PDFXEntry) {
                PDFXEntry xref = (PDFXEntry) parent;
                parent = _pfile.getXRefObj(xref);
                if (parent instanceof PDFStream)
                    parent = ((PDFStream) parent).getDict();
            }

            // Handle PDFFile
            if (aParent instanceof PDFFile) {
                PDFFile pfile = (PDFFile) aParent;

                // Get TreeNode for Info, Catalog and Pages dicts and add to nodes list
                Object infoXRef = pfile.getTrailer().get("Info");
                Object catXRef = pfile.getTrailer().get("Root");
                TreeNode info = getNode(pfile, pfile.getInfoDict(), "Info Dict (" + infoXRef + ')');
                TreeNode catalog = getNode(pfile, pfile.getCatalogDict(), "Catalog Dict (" + catXRef + ')');
                Object pagesXRef = pfile.getCatalogDict().get("Pages");
                Map pagesDict = pfile._pagesDict;
                TreeNode pages = getNode(pfile, pagesDict, "Pages Dict (" + pagesXRef + ')');

                // Add to new nodes list
                List<Object> nodes = new ArrayList<>();
                Collections.addAll(nodes, info, catalog, pages);

                // Create pages nodes and add to nodes list
                List<PDFXEntry> pagesArray = (List<PDFXEntry>) pagesDict.get("Kids");
                for (int i = 0; i < pagesArray.size(); i++) {
                    PDFXEntry page = pagesArray.get(i);
                    TreeNode node = getNode(pfile, page, "Page " + (i + 1) + " (" + page + ')');
                    nodes.add(node);
                }

                // Create Trailer dict node
                TreeNode trailer = getNode(pfile, pfile.getTrailer(), "Trailer Dict");
                Collections.addAll(nodes, pfile.getXRefTable(), trailer);

                // Return nodes array
                return nodes;
            }

            // Handle XRef Table
            if (aParent instanceof PDFXTable) {
                PDFXTable xtable = (PDFXTable) aParent;
                return (List<Object>) (List<?>) xtable.getXRefs();
            }

            // Handle Map
            if (parent instanceof Map) {
                Map<String, Object> map = (Map) parent;
                TreeNode[] nodes = new TreeNode[map.size()];
                List<String> keys = new ArrayList(map.keySet());
                for (int i = 0; i < keys.size(); i++) {
                    String key = keys.get(i);
                    Object obj = map.get(key);
                    String str = obj instanceof PDFXEntry || obj instanceof Map || obj instanceof List ?
                            getTypeString(obj) : '(' + String.valueOf(obj) + ')';
                    nodes[i] = getNode(aParent, obj, '/' + key + ' ' + str);
                }
                return Arrays.asList(nodes);
            }

            // Handle List
            if (parent instanceof List) {
                List<Object> list = (List<Object>) parent;
                return ListUtils.map(list, obj -> getNode(aParent, obj, getTypeString(obj)));
            }

            // Return null, since parent type not supported
            return null;
        }

        /**
         * Returns the text to be used for given item.
         */
        public String getText(Object anItem)
        {
            if (anItem instanceof PDFXTable) return "XRef Table";
            if (anItem instanceof PDFXEntry) return getTypeString(anItem);
            if (anItem instanceof TreeNode) return ((TreeNode) anItem).text;
            return anItem.toString();
        }
    }

    /**
     * A class to hold a tree node.
     */
    private static class TreeNode {
        String text;
        Object content, parent;

        public TreeNode(Object aPar, Object anObj, String aStr)
        {
            parent = aPar;
            content = anObj;
            text = aStr;
        }
    }

    static TreeNode getNode(Object aPar, Object anObj, String aStr)
    {
        return new TreeNode(aPar, anObj, aStr);
    }

}