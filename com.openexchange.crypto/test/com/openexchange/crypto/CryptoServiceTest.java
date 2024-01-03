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

package com.openexchange.crypto;

import static com.openexchange.crypto.CryptoErrorMessage.BadPassword;
import static com.openexchange.crypto.CryptoErrorMessage.MalformedEncryptedData;
import static com.openexchange.crypto.CryptoErrorMessage.SecurityException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.crypto.internal.CryptoServiceImpl;
import com.openexchange.exception.OXException;

/**
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class CryptoServiceTest {

    protected String payload = "Hello World!";
    protected String payloadSpecial = "H\u00d4\u00f8\u03a9ll\u00d4\u00f8\u03a9 &/() w\u00d4\u00f8\u03a9RLD!";
    protected String password = "passwordpasswordpasswordpassword";
    protected String passwordSpecial = "p\u00c0\u00dfw\u00d4\u00f8\u03a9rd;";
    protected String badPassword = "passwordpasswordpasswordpassword1";

    protected CryptoService cryptoService;

    protected final byte[] salt = new byte[] { 0x34, 0x11, 0x45, 0x03, 0x04, 0x05, 0x06, 0x43, 0x23, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0e };

    @Before
    public void setUp() {
        LeanConfigurationService leanConfigService = Mockito.mock(LeanConfigurationService.class);
        when(leanConfigService.getIntProperty(CryptoProperty.memory)).thenReturn(1024);
        when(leanConfigService.getIntProperty(CryptoProperty.iterations)).thenReturn(2);
        when(leanConfigService.getIntProperty(CryptoProperty.lanes)).thenReturn(1);
        cryptoService = new CryptoServiceImpl(leanConfigService);
    }

    @After
    public void tearDown() {
        cryptoService = null;
    }

    @Test
    public void testEncrypt() throws Exception {
        String encrypted = cryptoService.encrypt(payload, password);
        assertFalse("Payload was not encrypted.", encrypted.equals(payload));
        String[] split = encrypted.split("\\$");
        assertEquals("Encrypted data is missing parts", 6, split.length);

        encrypted = cryptoService.encrypt(payloadSpecial, passwordSpecial);
        assertFalse("Payload was not encrypted.", encrypted.equals(payloadSpecial));
        split = encrypted.split("\\$");
        assertEquals("Encrypted data is missing parts", 6, split.length);

        // Assert argon2 settings
        assertEquals(1024, Integer.parseInt(split[2]));
        assertEquals(2, Integer.parseInt(split[3]));
        assertEquals(1, Integer.parseInt(split[4]));
    }

    @Test
    public void testDecrypt() throws Exception {
        String encrypted = cryptoService.encrypt(payload, password);
        String decrypted = cryptoService.decrypt(encrypted, password);
        assertEquals("Payload not decrypted.", payload, decrypted);

        encrypted = cryptoService.encrypt(payloadSpecial, passwordSpecial);
        decrypted = cryptoService.decrypt(encrypted, passwordSpecial);
        assertEquals("payload not decrypted.", payloadSpecial, decrypted);
    }

    @Test
    public void testWrongPassword() throws Exception {
        final String encrypted = cryptoService.encrypt(payload, password);
        try {
            cryptoService.decrypt(encrypted, badPassword);
            fail("Exception expected.");
        } catch (OXException e) {
            assertTrue("Wrong exception thrown.", BadPassword.equals(e));
        }
    }

    @Test
    public void testExceptionForOldMechanism() throws Exception {
        // Encrypt with old method
        EncryptedData encrypted = cryptoService.encrypt(payload, password, false);
        try {
            // Decrypt with old mechanism
            cryptoService.decrypt(encrypted.data(), password);
            fail("Exception expected.");
        } catch (OXException e) {
            assertTrue("Wrong exception thrown.", CryptoErrorMessage.LegacyEncryption.equals(e));
        }
    }

    @Test
    public void testSaltUsageLegacy() throws Exception {
        EncryptedData encryptedData = cryptoService.encrypt(payload, password, true);
        String decryptedData = cryptoService.decrypt(encryptedData, password, true);
        assertEquals("Payload not decrypted", payload, decryptedData);

        encryptedData = cryptoService.encrypt(payload, password, false);
        decryptedData = cryptoService.decrypt(encryptedData, password, false);
        assertEquals("Payload not decrypted", payload, decryptedData);

        encryptedData = cryptoService.encrypt(payload, password, true);
        encryptedData = new EncryptedData(encryptedData.data(), salt);
        try {
            decryptedData = cryptoService.decrypt(encryptedData, password, true);
            fail("Exception expected.");
        } catch (OXException e) {
            assertTrue("Wrong exception thrown.", BadPassword.equals(e));
        }
    }

    @Test
    public void testMalformedEncryptedDataMissingAPart() throws OXException {
        String encrypted = cryptoService.encrypt(payloadSpecial, passwordSpecial);
        String[] split = encrypted.split("\\$");

        try {
            cryptoService.decrypt(split[0] + "$" + split[2], encrypted);
            fail("Exception expected.");
        } catch (OXException e) {
            assertTrue("Wrong exception thrown.", MalformedEncryptedData.equals(e));
        }
    }

    @Test
    public void testMalformedEncryptedData() {
        try {
            cryptoService.decrypt(payload, password);
            fail("Exception expected.");
        } catch (OXException e) {
            assertTrue("Wrong exception thrown.", MalformedEncryptedData.equals(e));
        }
    }

    @Test
    public void testMoreMalformedEncryptedData() {
        try {
            cryptoService.decrypt("a", password);
            fail("Exception expected.");
        } catch (OXException e) {
            assertTrue("Wrong exception thrown.", SecurityException.equals(e));
        }
    }

}
