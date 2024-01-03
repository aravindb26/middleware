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

package com.openexchange.secret.impl;

import static com.openexchange.java.Autoboxing.I;
import java.security.GeneralSecurityException;
import com.openexchange.crypto.CryptoErrorMessage;
import com.openexchange.crypto.CryptoService;
import com.openexchange.crypto.EncryptedData;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.secret.Decrypter;
import com.openexchange.secret.RankingAwareSecretService;
import com.openexchange.secret.SecretEncryptionService;
import com.openexchange.secret.SecretEncryptionStrategy;
import com.openexchange.secret.SecretExceptionCodes;
import com.openexchange.secret.SecretService;
import com.openexchange.secret.osgi.tools.WhiteboardSecretService;
import com.openexchange.session.Session;

/**
 * {@link CryptoSecretEncryptionService} - The {@link SecretEncryptionService} backed by {@link CryptoService}.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class CryptoSecretEncryptionService<T> implements SecretEncryptionService<T> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CryptoSecretEncryptionService.class);

    private final TokenList tokenList;

    private final SecretEncryptionStrategy<T> strategy;

    private final CryptoService crypto;

    /**
     * The {@link SecretService} reference with the highest ranking.
     * <p>
     * See {@link WhiteboardSecretService} implementation.
     */
    private final RankingAwareSecretService secretService;

    private final PasswordSecretService passwordSecretService;

    /**
     * Initializes a new {@link CryptoSecretEncryptionService}.
     *
     * @param crypto The crypto service reference
     * @param secretService The fall-back secret service
     * @param strategy The strategy to use
     * @param tokenList The token list
     */
    public CryptoSecretEncryptionService(final CryptoService crypto, final RankingAwareSecretService secretService, final SecretEncryptionStrategy<T> strategy, final TokenList tokenList) {
        super();
        this.crypto = crypto;
        this.secretService = secretService;
        this.strategy = strategy;
        this.tokenList = tokenList;
        passwordSecretService = new PasswordSecretService();
    }

    @Override
    public String encrypt(final Session session, final String toEncrypt) throws OXException {
        /*-
         * Check currently applicable SecretService
         *
         * Is it greater than or equal to default ranking of zero?
         * If not use token-based entry
         */
        String secret = (secretService.getRanking() >= 0) ? secretService.getSecret(session) : tokenList.peekLast().getSecret(session);
        checkSecret(secret);
        return crypto.encrypt(toEncrypt, secret);
    }

    @Override
    public String decrypt(final Session session, final String toDecrypt) throws OXException {
        if (Strings.isEmpty(toDecrypt)) {
            return toDecrypt;
        }
        return decrypt(session, toDecrypt, null);
    }

    @Override
    public String decrypt(final Session session, final String toDecrypt, final T customizationNote) throws OXException {
        // Ranking and secret from currently highest-ranked SecretService implementation
        int ranking = secretService.getRanking();
        String secretFromSecretService = secretService.getSecret(session);
        Integer iUserId = I(session.getUserId());
        Integer iContextId = I(session.getContextId());
        /*
         * Check with currently applicable SecretService
         */
        boolean checkedWithApplicableSecretService = false;
        if (ranking >= 0) { // Greater than or equal to default ranking of zero
            String decrypted = decryptWithSecretService(secretService, session, toDecrypt, secretFromSecretService, customizationNote, true);
            if (Strings.isNotEmpty(decrypted)) {
                return decrypted;
            }
            // Ignore and try other
            checkedWithApplicableSecretService = true;
        }
        /*
         * Use token-based entries for decryption
         *
         * Try with last list entry first
         */
        String secretForEncryption = ranking >= 0 ? secretFromSecretService : tokenList.peekLast().getSecret(session);
        for (int i = tokenList.size() - 1; i >= 0; i--) {
            SecretService current = tokenList.get(i);
            String decrypted = decryptWithSecretService(current, session, toDecrypt, secretForEncryption, customizationNote, !checkedWithApplicableSecretService && tokenList.size() - 1 == i);
            if (Strings.isNotEmpty(decrypted)) {
                return decrypted;
            }
            // Ignore and try other
        }
        /*
         * Try to decrypt "the old way"
         */
        LOG.debug("Failed to decrypt. Retrying with former crypt mechanism (user={}, context={})", iUserId, iContextId);
        String decrypted = null;
        if (customizationNote instanceof Decrypter decrypter) {
            try {
                decrypted = decrypter.getDecrypted(session, toDecrypt);
                if (decrypted != null) {
                    LOG.debug("Decrypted password with former crypt mechanism");
                }
            } catch (OXException x) {
                // Ignore and try other
                LOG.debug("Failed to decrypt with former crypt mechanism (user={}, context={})", iUserId, iContextId, x);
            }
        }
        if (decrypted == null) {
            try {
                decrypted = decrypthWithPasswordSecretService(toDecrypt, session);
            } catch (OXException x) {
                LOG.debug("Failed to decrypt using explicit password-based SecretService '{}'", passwordSecretService.getClass().getName(), x);
                decrypted = decryptAfterException(x, session, toDecrypt, session.getPassword());
                // Ignore and try other
            }
        }
        if (decrypted == null) {
            checkSecret(secretFromSecretService);
            try {
                decrypted = decrypthWithCryptoService(toDecrypt, secretFromSecretService);
            } catch (OXException e) {
                LOG.debug("Failed last attempt to decrypt using privileged SecretService '{}' (user={}, context={})", secretService.getClass().getName(), iUserId, iContextId, e);
                decrypted = decryptAfterException(e, session, toDecrypt, secretFromSecretService);
                if (null == decrypted) {
                    // No more fall-backs available
                    throw e;
                }
            }
        }
        /*
         * At last, re-crypt password using current secret service & store it
         */
        store(recrypt(decrypted, secretForEncryption), customizationNote);
        LOG.debug("Updated encrypted string using currently applicable SecretService '{}' (user={}, context={})", tokenList.peekLast(), iUserId, iContextId);
        /*
         * Return decrypted string
         */
        return decrypted;
    }

    /*
     * ============================== HELPERS ==============================
     */
    private void checkSecret(String secret) throws OXException {
        if (Strings.isEmpty(secret)) {
            throw SecretExceptionCodes.EMPTY_SECRET.create();
        }
    }

    private void store(String recrypted, T customizationNote) throws OXException {
        strategy.update(recrypted, customizationNote);
    }

    private String recrypt(String decrypted, String secret) throws OXException {
        checkSecret(secret);
        return crypto.encrypt(decrypted, secret);
    }

    private String decrypthWithPasswordSecretService(String toDecrypt, Session session) throws OXException {
        String secret = passwordSecretService.getSecret(session);
        if (Strings.isEmpty(secret)) {
            return null;
        }
        String decrypted = crypto.decrypt(toDecrypt, secret);
        LOG.debug("Decrypted password with former crypt mechanism");
        return decrypted;
    }

    private String decrypthWithCryptoService(String toDecrypt, String secret) throws OXException {
        String decrypted = crypto.decrypt(toDecrypt, secret);
        LOG.debug("Decrypted password with crypto service");
        return decrypted;
    }

    /**
     * Decrypts the given string with the secret service
     *
     * @param current The secret service to use
     * @param session The session
     * @param toDecrypt The string to decrypt
     * @param secretToUse The secret to use if the string need to be recrypted
     * @param customizationNote Additional data in case the string got recrypted and needs to be updated
     * @param checkSecret Whether an empty secret shall throw an error or not
     * @return The decrypted string or <code>null</code>
     * @throws OXException In case of unexpected error
     */
    private String decryptWithSecretService(SecretService current, Session session, String toDecrypt, String secretToUse, T customizationNote, boolean checkSecret) throws OXException {
        String secret = current.getSecret(session);
        if (Strings.isEmpty(secret)) {
            if (checkSecret) {
                throw SecretExceptionCodes.EMPTY_SECRET.create();
            }
            return null;
        }
        try {
            return decrypthWithCryptoService(toDecrypt, secret);
        } catch (OXException x) {
            LOG.debug("Failed to decrypt using next SecretService '{}' (user={}, context={})", current, I(session.getUserId()), I(session.getContextId()), x);
            String decrypted = decryptAfterException(x, session, toDecrypt, secret);
            if (Strings.isNotEmpty(decrypted)) {
                store(recrypt(decrypted, secretToUse), customizationNote);
                return decrypted;
            }
            // Ignore and try other
        }
        return null;
    }

    /**
     * Tries to decrypts a given string after an exception.
     * Uses the
     * <li> Legacy encryption</li>
     * <li> OldStyleDecrypt</li>
     * to decrypt the given string
     *
     * @param e To check for {@link CryptoErrorMessage#LegacyEncryption}
     * @param session The user session
     * @param toDecrypt The string to decrypt
     * @param secret The secret for decryption
     * @return The decrypted string or <code>null</code>
     */
    @SuppressWarnings("deprecation")
    private String decryptAfterException(OXException e, Session session, String toDecrypt, String secret) {
        if (CryptoErrorMessage.LegacyEncryption.equals(e)) {
            try {
                LOG.info("Legacy encryption detected for user={} in context={}. Try to recrypt.", I(session.getUserId()), I(session.getContextId()));
                return crypto.decrypt(new EncryptedData(toDecrypt, null), secret, false);
            } catch (OXException ex) {
                LOG.debug("Unable to decrypt using crypto service for user={} in context={}.", I(session.getUserId()), I(session.getContextId()), ex);
            }
        }
        try {
            LOG.debug("Try to decrypted password with former crypt mechanism");
            return OldStyleDecrypt.decrypt(toDecrypt, secret);
        } catch (GeneralSecurityException gse) {
            LOG.debug("Failed to decrypt using old-style decryptor user={} in context={}", I(session.getUserId()), I(session.getContextId()), gse);
        }
        return null;
    }

    /*
     * ============================== CLASS ==============================
     */

    @Override
    public String toString() {
        return tokenList.toString();
    }

}
