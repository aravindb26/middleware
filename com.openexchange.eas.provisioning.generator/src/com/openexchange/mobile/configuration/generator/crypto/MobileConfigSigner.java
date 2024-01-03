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

package com.openexchange.mobile.configuration.generator.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedDataStreamGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import com.openexchange.java.Charsets;

/**
 * {@link MobileConfigSigner}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class MobileConfigSigner extends Writer {

    private final CMSSignedDataStreamGenerator generator;
    private final OutputStream outputStream;

    /**
     *
     * Initializes a new {@link MobileConfigSigner}.
     *
     * @param certificate The certificate to use for signing as raw {@link InputStream}
     * @param privateKey The private key of the certificate as raw {@link InputStream}
     * @param outputStream The {@link OutputStream} to write the signed data to
     * @param additionalCertificate The additional certificates, as raw {@link InputStream}, to include in the signed data, or null to omit
     * @throws IOException
     * @throws OperatorCreationException
     * @throws CMSException
     * @throws CertificateException
     */
    @SuppressWarnings("resource")
    public MobileConfigSigner(InputStream certificate, InputStream privateKey, OutputStream outputStream, InputStream additionalCertificate) throws IOException, OperatorCreationException, CMSException, CertificateException {
        //@formatter:off
        this(parseCertificate(Objects.requireNonNull(certificate, "certificate must not be null")),
             parsePrivateKey(Objects.requireNonNull(privateKey, "privateKey must not be null")),
             outputStream,
             additionalCertificate != null ? parseCertificates(additionalCertificate) : Collections.emptyList());
        //@formatter:on
    }

    /**
     * Initializes a new {@link MobileConfigSigner}.
     *
     * @param signingCertificate The {@link X509Certificate} to use for signing
     * @param privateKey The private key of the certificate
     * @param outputStream The {@link OutputStream} to write the signed data to
     * @throws IOException
     * @throws CertificateEncodingException
     * @throws OperatorCreationException
     * @throws CMSException
     */
    public MobileConfigSigner(X509Certificate signingCertificate, PrivateKey privateKey, OutputStream outputStream) throws IOException, CertificateEncodingException, OperatorCreationException, CMSException {
        this(signingCertificate, privateKey, outputStream, Collections.emptyList());
    }

    /**
     * Initializes a new {@link MobileConfigSigner}.
     *
     * @param signingCertificate The {@link X509Certificate} to use for signing
     * @param privateKey The private key of the certificate
     * @param outputStream The {@link OutputStream} to write the signed data to
     * @param additionalCertificates A collection of additional {@link X509Certificate}s to include into the signed message
     * @throws IOException
     * @throws CertificateEncodingException
     * @throws OperatorCreationException
     * @throws CMSException
     */
    @SuppressWarnings("resource")
    public MobileConfigSigner(X509Certificate signingCertificate, PrivateKey privateKey, OutputStream outputStream, Collection<X509Certificate> additionalCertificates) throws IOException, CertificateEncodingException, OperatorCreationException, CMSException {
        Objects.requireNonNull(signingCertificate, "signingCertificate must not be null");
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        Objects.requireNonNull(additionalCertificates, "additionalCertificates must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");

        ArrayList<X509Certificate> certificates = new ArrayList<>();
        certificates.add(signingCertificate);
        certificates.addAll(additionalCertificates);
        JcaCertStore certificateStore = new JcaCertStore(certificates);

        ContentSigner contentSigner = new JcaContentSignerBuilder(Constants.SIGNATURE_ALGORITHM_SHA256_RSA).setProvider(Constants.BC_PROVIDER).build(privateKey);

        this.generator = new CMSSignedDataStreamGenerator();
        //@formatter:off
        this.generator.addSignerInfoGenerator(
            new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder()
                    .setProvider(Constants.BC_PROVIDER)
                    .build())
            .setDirectSignature(true)
            .build(contentSigner, signingCertificate));
        //@formatter:on
        this.generator.addCertificates(certificateStore);
        this.outputStream = generator.open(outputStream, true);
    }

    /**
     * Parses a {@link X509Certificate} from the given {@link InputStream}
     *
     * @param certificateData The data ro parse the certificate from
     * @return The first found {@link X509Certificate} in the given data
     * @throws CertificateException
     */
    private static X509Certificate parseCertificate(InputStream certificateData) throws CertificateException {
        Collection<X509Certificate> certificates = parseCertificates(certificateData);
        return certificates.isEmpty() ? null : certificates.iterator().next();
    }

    /**
     * Parses a collection of {@link X509Certificate}s from the given {@link InputStream}
     *
     * @param certificateData The data to parse the certificates from
     * @return A collection of parsed {@link X509Certificate}s
     * @throws CertificateException
     */
    private static Collection<X509Certificate> parseCertificates(InputStream certificateData) throws CertificateException {
        ArrayList<X509Certificate> certificates = new ArrayList<X509Certificate>();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Collection<?> c = cf.generateCertificates(certificateData);
        Iterator<?> i = c.iterator();
        while (i.hasNext()) {
            X509Certificate cert = (X509Certificate) i.next();
            certificates.add(cert);
        }
        return certificates;
    }

    /**
     * Internal method to parse a {@link PrivateKey} from the given {@link InputStream}
     *
     * @param keyData The {@link InputStream} to parse the key from
     * @return The parsed {@link PrivateKey}
     * @throws IOException
     */
    private static PrivateKey parsePrivateKey(InputStream keyData) throws IOException {
        try (PEMParser parser = new PEMParser(new InputStreamReader(keyData, Charsets.US_ASCII))) {
            Object key = parser.readObject();
            if (key != null && (key instanceof PrivateKeyInfo)) {
                return new JcaPEMKeyConverter().setProvider(Constants.BC_PROVIDER).getPrivateKey((PrivateKeyInfo) key);
            }
        }
        return null;
    }

    /**
     * Returns the given char array as byte representation
     *
     * @param chars The char[] array to convert
     * @return The byte representation of the given char array
     */
    private byte[] toBytes(char[] chars) {
        ByteBuffer byteBuffer = Charsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    @Override
    public void close() throws IOException {
        this.outputStream.close();
    }

    @Override
    public void flush() throws IOException {
        this.outputStream.flush();
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        outputStream.write(toBytes(cbuf), off, len);
    }
}
