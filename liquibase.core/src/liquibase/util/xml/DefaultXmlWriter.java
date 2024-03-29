package liquibase.util.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

public class DefaultXmlWriter implements XmlWriter {

    @Override
    public void write(Document doc, OutputStream outputStream) throws IOException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try {
                factory.setAttribute("indent-number", 4);
            } catch (Exception e) {
                //guess we can't set it, that's ok
            }

            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            //need to nest outputStreamWriter to get around JDK 5 bug.  See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
            transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(outputStream, "utf-8")));
        } catch (TransformerException e) {
            throw new IOException(e.getMessage());
        }
    }
}
