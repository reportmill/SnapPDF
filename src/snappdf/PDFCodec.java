package snappdf;
import java.util.Map;

/**
 * An interface for PDF decrypt.
 */
public interface PDFCodec {

    /**
     * Decrypt strings & streams using the algorithm from the encryption dictionary.
     */
    default Object decryptObject(Object o, int objNum, int generationNum)
    {
        throw new RuntimeException("PDFCode.decryptObject: Not implemented");
    }

    /**
     * Called to cache shared encrypt values.
     */
    default void startEncrypt(int oNum, int gNum)
    {
        throw new RuntimeException("PDFCode.startEncrypt: Not implemented");
    }

    /**
     * Returns the contents of the pdf string, encrypted
     */
    default byte[] encryptString(String s)
    {
        throw new RuntimeException("PDFCode.encryptString: Not implemented");
    }

    /**
     * Returns a new copy of the input buffer, encrypted.
     */
    default byte[] encryptBytes(byte aBuffer[])
    {
        throw new RuntimeException("PDFCode.encryptBytes: Not implemented");
    }

    /**
     * Returns the encryption dictionary.
     */
    default Map getEncryptionDict()
    {
        throw new RuntimeException("PDFCode.getEncryptionDict: Not implemented");
    }

}
