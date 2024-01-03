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

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Iterator;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedDataParser;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

/**
 * {@link TestSignatureVerifier} - A PKCS#7 signature verifier
 * <p>
 * Verifies PKCS#7 signatures for testing purpose only(!),
 * because this class only validates the signature within a given data, but does not verify/validate the signer's certificate
 * </p>
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Grüdelbach</a>
 * @since v8.0.0
 */
public class TestSignatureVerifier {

    private static final String BC_PROVIDER = "BC";

    /**
     * Verifies the given data and it's signature
     *
     * @param data The data including the signature to verify
     * @return <code>True</code> if the given data was verified, <code>false</code> otherwise
     * @throws OperatorCreationException
     * @throws CMSException
     * @throws IOException
     * @throws CertificateException
     */
    @SuppressWarnings("unchecked")
    public boolean verify(InputStream data) throws OperatorCreationException, CMSException, IOException, CertificateException {

        //Parse the data
        CMSSignedDataParser parser = new CMSSignedDataParser(new JcaDigestCalculatorProviderBuilder().setProvider(BC_PROVIDER).build(), data);
        parser.getSignedContent().drain();

        //Get signatures
        Store<?> certificates = parser.getCertificates();
        Collection<SignerInformation> signerInformations = parser.getSignerInfos().getSigners();
        Iterator<?> signerIterator = signerInformations.iterator();
        if (!signerIterator.hasNext()) {
            //No signatures found
            return false;
        }

        //Iterator over all signatures
        while (signerIterator.hasNext()) {
            SignerInformation signer = (SignerInformation) signerIterator.next();
            Collection<?> certCollection = certificates.getMatches(signer.getSID());

            //Get the Certificate for the signer
            Iterator<?> certIterator = certCollection.iterator();
            X509CertificateHolder certificate = (X509CertificateHolder) certIterator.next();

            boolean verified = signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC_PROVIDER).build(certificate));
            if (!verified) {
                return false;
            }
        }
        return true;
    }
}
