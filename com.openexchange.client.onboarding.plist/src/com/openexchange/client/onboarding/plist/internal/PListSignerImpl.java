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

package com.openexchange.client.onboarding.plist.internal;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSProcessableFile;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.util.Store;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.client.onboarding.OnboardingExceptionCodes;
import com.openexchange.client.onboarding.plist.PListSigner;
import com.openexchange.client.onboarding.plist.osgi.Services;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.session.Session;

/**
 * {@link PListSignerImpl}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public final class PListSignerImpl implements PListSigner {

    /**
     * Initializes a new {@link PListSignerImpl}.
     */
    public PListSignerImpl() {
        super();
    }

    @Override
    public IFileHolder signPList(IFileHolder toSign, Session session) throws OXException {
        return signPList(toSign, session.getUserId(), session.getContextId());
    }

    @Override
    public IFileHolder signPList(IFileHolder toSign, int userId, int contextId) throws OXException {
        ConfigViewFactory viewFactory = Services.getService(ConfigViewFactory.class);
        if (null == viewFactory) {
            throw ServiceExceptionCode.absentService(ConfigViewFactory.class);
        }
        ConfigView view = viewFactory.getView(userId, contextId);
        ConfigurationService configService = Services.getService(ConfigurationService.class);
        if (null == configService) {
            throw ServiceExceptionCode.absentService(ConfigurationService.class);
        }

        // Check if enabled
        boolean enabled = configService.getBoolProperty("com.openexchange.client.onboarding.plist.signature.enabled", false);
        if (false == enabled) {
            return toSign;
        }

        // Get & check needed parameters
        String storeName = configService.getProperty("com.openexchange.client.onboarding.plist.pkcs12store.filename");
        String password = configService.getProperty("com.openexchange.client.onboarding.plist.pkcs12store.password");
        String alias = view.get("com.openexchange.client.onboarding.plist.signkey.alias", String.class);
        if (Strings.isEmpty(storeName) || Strings.isEmpty(password) || Strings.isEmpty(alias)) {
            return toSign;
        }

        return signPList(toSign, storeName, password, alias);
    }

    private IFileHolder signPList(IFileHolder toSign, String storeName, String password, String alias) throws OXException {
        IFileHolder input = toSign;

        ThresholdFileHolder sink = null;
        boolean error = true;
        try {
            PrivateKey privKey = getPrivateKey(storeName, password, alias);
            Certificate[] certChain = getCertificateChain(storeName, password, alias);
            if (null == privKey || null == certChain) {
                throw OnboardingExceptionCodes.SIGN_ERROR.create(alias);
            }
            X509Certificate cert = (X509Certificate) certChain[0];
            if (null == cert) {
                throw OnboardingExceptionCodes.SIGN_ERROR.create(alias);
            }
            Store<?> certs = new JcaCertStore(Arrays.asList(certChain));

            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            JcaSimpleSignerInfoGeneratorBuilder builder = new JcaSimpleSignerInfoGeneratorBuilder();
            gen.addSignerInfoGenerator(builder.build("SHA256withRSA", privKey, cert));
            gen.addCertificates(certs);

            CMSTypedData data;
            {
                // Ensure to deal with an instance of ThresholdFileHolder to yield appropriate processable data
                if (input instanceof ThresholdFileHolder) {
                    ThresholdFileHolder tfh = (ThresholdFileHolder) input;
                    data = toProcessableData(tfh);
                } else {
                    ThresholdFileHolder tfh = new ThresholdFileHolder(input);
                    input.close();
                    input = tfh;
                    data = toProcessableData(tfh);
                }
            }

            // Sign it
            CMSSignedData signed = gen.generate(data, true);
            ContentInfo contentInfo = signed.toASN1Structure();

            // Flush signed content to a new ThresholdFileHolder
            sink = new ThresholdFileHolder();
            sink.setContentType(input.getContentType());
            sink.setDisposition(input.getDisposition());
            sink.setName(input.getName());
            ASN1OutputStream aOut = ASN1OutputStream.create(sink.asOutputStream());
            aOut.writeObject(contentInfo);
            aOut.flush();
            error = false; // Avoid preliminary closing
            return sink;
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw OnboardingExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (error) {
                Streams.close(sink);
            }
            Streams.close(input);
        }
    }

    private CMSTypedData toProcessableData(ThresholdFileHolder tfh) throws OXException {
        File tempFile = tfh.getTempFile();
        return null == tempFile ? new CMSProcessableByteArray(tfh.toByteArray()) : new CMSProcessableFile(tempFile);
    }

    private Certificate[] getCertificateChain(String storeName, String password, String alias) throws Exception {
        FileInputStream fis = null;
        try {
            KeyStore store = KeyStore.getInstance("PKCS12");
            fis = new FileInputStream(storeName);
            store.load(fis, password.toCharArray());
            return store.getCertificateChain(alias);
        } catch (Exception e) {
            throw OnboardingExceptionCodes.KEYSTORE_ERROR.create(e, storeName);
        } finally {
            Streams.close(fis);
        }
    }

    private PrivateKey getPrivateKey(String storeName, String password, String alias) throws OXException {
        FileInputStream fis = null;
        try {
            KeyStore store = KeyStore.getInstance("PKCS12");
            fis = new FileInputStream(storeName);
            store.load(fis, password.toCharArray());
            return (PrivateKey) store.getKey(alias, password.toCharArray());
        } catch (Exception e) {
            throw OnboardingExceptionCodes.KEYSTORE_ERROR.create(e, storeName);
        } finally {
            Streams.close(fis);
        }
    }

}
