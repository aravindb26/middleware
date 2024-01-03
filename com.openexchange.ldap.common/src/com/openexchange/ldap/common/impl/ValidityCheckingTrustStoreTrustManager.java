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

package com.openexchange.ldap.common.impl;

import java.io.Serializable;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;

/**
 * {@link ValidityCheckingTrustStoreTrustManager} is a {@link X509TrustManager} which can check the validity of given certificate chains
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class ValidityCheckingTrustStoreTrustManager implements X509TrustManager, Serializable {

    private static final long serialVersionUID = 4347791736436145554L;
    private static final X509Certificate[] NO_CERTIFICATES = new X509Certificate[0];

    private final boolean examineValidityDates;
    private final X509TrustManager[] trustManagers;

    /**
     * Initializes a new {@link ValidityCheckingTrustStoreTrustManager}.
     *
     * @param trustStore The {@link KeyStore} to use
     * @param examineValidityDates Whether to validate the certificates or not
     * @throws CertificateException In case the {@link X509TrustManager}s couldn't be initialized
     */
    public ValidityCheckingTrustStoreTrustManager(KeyStore trustStore, final boolean examineValidityDates) throws CertificateException {
        this.examineValidityDates = examineValidityDates;
        try {
            final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(trustStore);
            // @formatter:off
            trustManagers = Arrays.asList(factory.getTrustManagers()).stream()
                                                            .map(tm -> (X509TrustManager) tm)
                                                            .toArray(X509TrustManager[]::new);
            // @formatter:on
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new CertificateException(LDAPCommonErrorCodes.INVALID_CONFIG.create(e));
        }
    }


    /**
     * Performs a validity checks on the given certificates if configured
     *
     * @param chain The certificate chain to check
     * @throws CertificateException in case one of the certificates is invalid
     */
    private void checkCertificates(X509Certificate[] chain) throws CertificateException {
        if (examineValidityDates) {
            for (final X509Certificate c : chain) {
                c.checkValidity();
            }
        }
    }

    @Override()
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        checkCertificates(chain);
        for (final X509TrustManager m : trustManagers) {
            m.checkClientTrusted(chain, authType);
        }
    }

    @Override()
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        checkCertificates(chain);
        for (final X509TrustManager m : trustManagers) {
            m.checkServerTrusted(chain, authType);
        }
    }

    @Override()
    public X509Certificate[] getAcceptedIssuers() {
        return NO_CERTIFICATES;
    }

}
