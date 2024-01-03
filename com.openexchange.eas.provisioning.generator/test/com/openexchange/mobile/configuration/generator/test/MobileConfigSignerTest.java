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

package com.openexchange.mobile.configuration.generator.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Before;
import org.junit.Test;
import com.openexchange.mobile.configuration.generator.crypto.MobileConfigSigner;

/**
 * {@link MobileConfigSignerTest}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v8.0.0
 */
public class MobileConfigSignerTest {

    private static final String TEST_CERTIFICATE_RESOURCE = "test.crt";
    private static final String TEST_KEY_RESOURCE = "test.testkey";

    @Before
    public void Initialzie() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Opens a {@link InputStream} to a resource
     *
     * @param resource The name of the resource to open
     * @return The {@link InputStream} to the resource of the given name
     */
    private InputStream openResource(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    /**
     * Loads a data from a resource
     *
     * @param resource The name of the internal resource to load the data from
     * @return The data as string loaded from the given resource
     * @throws IOException
     * @throws CertificateException
     */
    private String loadDataFromResources(String resource) throws IOException {
        String testData = null;
        try (InputStream testConfigStream = getClass().getResourceAsStream(resource)) {
            byte[] buffer = new byte[testConfigStream.available()];
            testConfigStream.read(buffer);
            testData = new String(buffer);
        }
        return testData;
    }

    /***
     * Signs data
     *
     * @param data The data to sign
     * @param signingCertificate The {@link InputStream} pointing to the certificate data
     * @param privateKey The {@link InputStream} pointing to the private key
     * @param additionCertificates The {@link InputStream} pointing to additional certificates which should get included
     * @return The Signed data
     * @throws OperatorCreationException
     * @throws IOException
     * @throws CMSException
     * @throws CertificateException
     * @throws NoSuchProviderException
     */
    private ByteArrayOutputStream signData(String data, //@formatter:off
        InputStream signingCertificate,
        InputStream privateKey,
        InputStream additionCertificates) throws OperatorCreationException, IOException, CMSException, CertificateException { //@formatter:on

        //Writing the test data to a ByteArrayOutputStream while signing it with the test certificate
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (MobileConfigSigner mobileConfigSigner = new MobileConfigSigner(signingCertificate, privateKey, bos, additionCertificates)) {
            char[] cbuf = data.toCharArray();
            mobileConfigSigner.write(cbuf, 0, cbuf.length);
        }
        return bos;
    }

    /**
     * Tests signing and verifying some test data
     *
     * @throws Exception
     */
    @Test
    public void testSignAndVerify() throws Exception {
        try (InputStream cert = openResource(TEST_CERTIFICATE_RESOURCE); /*@formatter:off*/
             InputStream privateKey = openResource(TEST_KEY_RESOURCE);
             InputStream additionalCerts = openResource(TEST_CERTIFICATE_RESOURCE)){ /*@formatter:on*/

            //Signing the test data
            String testData = loadDataFromResources("eas.mobileconfig");

            ByteArrayOutputStream bos = signData(testData, cert, privateKey, additionalCerts);

            //...and verify the signature
            boolean verified = new TestSignatureVerifier().verify(new ByteArrayInputStream(bos.toByteArray()));
            assertTrue("The signature must be verifiable", verified);
        }
    }

    /**
     * Tests that verifying some manipulated data fails
     *
     * @throws Exception
     */
    @Test
    public void testSignModifiyVerifyFails() throws Exception {
        try (InputStream cert = openResource(TEST_CERTIFICATE_RESOURCE); /*@formatter:off*/
             InputStream privateKey = openResource(TEST_KEY_RESOURCE);
             InputStream additionalCerts = openResource(TEST_CERTIFICATE_RESOURCE)){ /*@formatter:on*/

            //Signing the test data
            final String testData = loadDataFromResources("eas.mobileconfig");
            ByteArrayOutputStream bos = signData(testData, cert, privateKey, additionalCerts);

            //Manipulate the signed data, so that the verification fails
            byte[] manipulatedData = bos.toByteArray();
            char[] manipulation = "This message is manipulated".toCharArray();
            final int offset = 500; //some offset because we do not want to mess up the CMS/PKS#7 header, but only want to manipulate the data
            for (int i = 0; i < manipulation.length; i++) {
                manipulatedData[offset + i] = (byte) manipulation[i];
            }

            //Verifying manipulated data MUST fail!
            boolean verified = new TestSignatureVerifier().verify(new ByteArrayInputStream(manipulatedData));
            assertFalse("The signature verification of manipulated data MUST fail!", verified);
        }
    }
}
