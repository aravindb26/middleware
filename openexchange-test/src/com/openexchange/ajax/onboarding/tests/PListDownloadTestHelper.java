/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.ajax.onboarding.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import xmlwise.Plist;
import xmlwise.XmlParseException;

/**
 * {@link PListDownloadTestHelper}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.1
 */
public class PListDownloadTestHelper {

    /**
     * Initializes a new {@link PListDownloadTestHelper}.
     */
    public PListDownloadTestHelper() {
        super();
    }

    private static final String[] PLIST_BASIC_KEYS = new String[] { "PayloadIdentifier", "PayloadType", "PayloadUUID", "PayloadVersion", "PayloadDisplayName", "PayloadContent" };
    private static final String[] PLIST_MAIL_KEYS = new String[] { "PayloadType", "PayloadUUID", "PayloadIdentifier", "PayloadVersion", "EmailAccountDescription", "EmailAccountName", "EmailAccountType", "EmailAddress", "IncomingMailServerAuthentication", "IncomingMailServerHostName", "IncomingMailServerPortNumber", "IncomingMailServerUseSSL", "IncomingMailServerUsername", "OutgoingMailServerAuthentication", "OutgoingMailServerHostName", "OutgoingMailServerPortNumber", "OutgoingMailServerUseSSL", "OutgoingMailServerUsername" };
    private static final String[] PLIST_EAS_KEYS = new String[] { "PayloadType", "PayloadUUID", "PayloadIdentifier", "PayloadVersion", "UserName", "EmailAddress", "Host", "SSL" };
    private static final String[] PLIST_DAV_KEYS = new String[] { "PayloadType", "PayloadUUID", "PayloadIdentifier", "PayloadVersion", "PayloadOrganization", "CardDAVUsername", "CardDAVHostName", "CardDAVUseSSL", "CardDAVAccountDescription" };

    protected void testMailDownload(String url, String host) throws IOException, SAXException, ParserConfigurationException, TransformerException, XmlParseException {

        Map<String, Object> properties = testDownload(host, url);
        if (properties == null) {
            //Scenario is probably deactivated
            System.err.println("Unable to test mail Download. Mail Scenario is probably deactivated.");
            return;
        }
        for (String key : PLIST_MAIL_KEYS) {
            assertTrue(properties.keySet().contains(key), "Plist does not contain the following property: " + key);
            assertNotNull(properties.get(key), "The property " + key + " is null");
        }
    }

    protected void testEASDownload(String url, String host) throws IOException, SAXException, ParserConfigurationException, TransformerException, XmlParseException {

        Map<String, Object> properties = testDownload(host, url);
        if (properties == null) {
            //Scenario is probably deactivated
            System.err.println("Unable to test EAS Download. EAS Scenario is probably deactivated.");
            return;
        }

        for (String key : PLIST_EAS_KEYS) {
            assertTrue(properties.keySet().contains(key), "Plist does not contain the following property: " + key);
            assertNotNull(properties.get(key), "The property " + key + " is null");
        }
    }

    protected void testDavDownload(String url, String host) throws IOException, SAXException, ParserConfigurationException, TransformerException, XmlParseException {
        Map<String, Object> properties = testDownload(host, url);

        if (properties == null) {
            //Scenario is probably deactivated
            System.err.println("Unable to test Dav Download. Dav Scenario is probably deactivated.");
            return;
        }
        for (String key : PLIST_DAV_KEYS) {
            assertTrue(properties.keySet().contains(key), "Plist does not contain the following property: " + key);
            assertNotNull(properties.get(key), "The property " + key + " is null");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> testDownload(String host, String url) throws TransformerException, XmlParseException, ParserConfigurationException, SAXException, IOException {
        CloseableHttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier((hostname, session) -> true).build();
        CloseableHttpResponse resp = client.execute(new HttpGet(host + url));
        try {
            assertEquals(200, resp.getStatusLine().getStatusCode());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(resp.getEntity().getContent());
            assertNotNull(doc);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String plist = writer.getBuffer().toString().replaceAll("\n|\r", "");
            Map<String, Object> properties = Plist.fromXml(plist);
            assertNotNull(properties);

            //Test basic values
            for (String key : PLIST_BASIC_KEYS) {
                assertTrue(properties.keySet().contains(key));
            }

            for (Object o : properties.values()) {
                assertNotNull(o);
            }

            properties = (Map<String, Object>) ((ArrayList<Object>) properties.get("PayloadContent")).get(0);
            assertNotNull(properties);

            return properties;
        } finally {
            resp.close();
        }
    }
}
