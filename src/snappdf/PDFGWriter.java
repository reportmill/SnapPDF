/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snappdf;

/**
 * PDF Writer.
 */
public class PDFGWriter extends PDFWriter {

    /**
     * Creates PDFGWriter.
     */
    public PDFGWriter()
    {
        initWriter();
    }

    /**
     * Returns PDF Bytes.
     */
    @Override
    protected byte[] getBytes()
    {
        // Finish writer
        finishWriter();

        // Do normal version
        return super.getBytes();
    }
}