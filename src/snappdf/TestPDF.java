package snappdf;
import snap.gfx.*;
import snap.text.TextBlock;
import snap.text.TextStyle;
import snap.util.SnapUtils;
import snap.view.*;

/**
 * A custom class.
 */
public class TestPDF {

    public static void main(String[] args)
    {
        DocView doc = new DocView();
        PageView page = new PageView();
        page.setPrefSize(792, 612);
        doc.setPage(page);

        // Add Rect
        RectView rect = new RectView(320, 320, 200, 200);
        rect.setFill(Color.RED);
        rect.setBorder(Color.BLUE, 2);
        page.addChild(rect);

        // Add Text
        TextView textView = new TextView();
        textView.setBounds(380, 80, 300, 200);
        textView.setWrapLines(true);
        textView.setRotate(-20);
        textView.setBorder(Color.GREEN, 1);
        textView.setFill(new Color("#AACCEE33"));
        page.addChild(textView);

        // Create/set RichText doc
        TextBlock richText = textView.getTextBlock();
        richText.addChars("Why is the world in love again, why are they marching hand in hand?");
        richText.setTextStyleValue(TextStyle.Font_Prop, new Font("Arial Bold", 24), 0, richText.length());
        richText.setTextStyleValue(TextStyle.UNDERLINE_KEY, 1, 20, 30);

        // Add Image
        ImageView imageView = new ImageView("/Users/jeff/DesktopStack/Images/Daisy.jpg");
        imageView.setBounds(36, 36, 320, 480);
        imageView.setOpacity(.8);
        page.addChild(imageView, 0);

        byte[] bytes = new PDFWriter().getBytes(doc);
        SnapUtils.writeBytes(bytes, "/tmp/test.pdf");
        GFXEnv.getEnv().openURL("/tmp/test.pdf");
    }

}