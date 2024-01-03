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

package com.openexchange.ajax.session;

import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.l;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.HttpCookie;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.java.Charsets;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.test.common.test.json.JSONAssertion;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.tools.encoding.Base64;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests the login. This assumes autologin is allowed and cookie timeout is one week.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.org">Francisco Laguna</a>
 */
public class LoginTest extends AbstractLoginTest {

    String expiresAttribute = "Expires=";
    DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz", java.util.Locale.US);

    public LoginTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        createClient();
    }

    // Success Cases
    @Test
    public void testSuccessfulLoginReturnsSession() throws Exception {
        assertResponseContains("session");
    }

    /*
     * Response now lacks the random unless configured otherwise via login.properties:com.openexchange.ajax.login.randomToken=false
     */
    @Test
    public void testSuccessfulLoginLacksRandom() throws Exception {
        assertResponseLacks("random");
    }

    @Test
    public void testSuccessfulLoginSetsSecretCookie() throws Exception {
        rawLogin(testUser);
        boolean found = currentClient.getCookieStore().getCookies().stream().anyMatch(c -> c.getName().startsWith("open-xchange-secret"));
        assertTrue(found, "Missing secret cookie: " + getCookiesAsString());
    }

    private String getCookiesAsString() {
        return currentClient.getCookieStore().getCookies().stream().map(c -> c.getName()).collect(Collectors.joining(","));
    }

    @Test
    public void testSuccessfulLoginDoesSetSessionCookieTTL_withStaySignedIn() throws Exception {
        // Changed behaviour with 7.10.3. "session"-cookie is now always set during login, but without TTL if 'staySignedIn' was not set
        rawLogin(testUser, true);
        // @formatter:off
        Optional<HttpCookie> optCookie = currentClient.getCookieStore()
                                     .getCookies()
                                     .stream()
                                     .filter(cookie -> cookie.getName().startsWith("open-xchange-session") && cookie.getMaxAge() > 0)
                                     .findAny();
        // @formatter:on
        assertTrue(optCookie.isPresent(), "open-xchange-session cookie not found");
        long minutes = (long) Math.ceil(optCookie.get().getMaxAge() / 60d); // round up to the next minute
        assertTrue(TimeUnit.DAYS.toMinutes(7) <= minutes, "Found unexpected cookie ttl: " + optCookie.get().getMaxAge());
    }

    @Test
    public void testSuccessfulLoginDoesSetSessionCookieTTL_withoutStaySignedIn() throws Exception {
        // Changed behaviour with 7.10.3. "session"-cookie is now always set during login, but without TTL if 'staySignedIn' was not set
        rawLogin(testUser, false);

        // @formatter:off
        boolean foundAndTTLNotSet = currentClient.getCookieStore()
                                                 .getCookies()
                                                 .stream()
                                                 .filter(cookie -> cookie.getName().startsWith("open-xchange-session"))
                                                 .allMatch(cookie -> cookie.getMaxAge() < 0);
        // @formatter:on

        assertTrue(foundAndTTLNotSet, "open-xchange-session cookie not found or a ttl is set");
    }

    @Test
    public void testSecretCookiesDifferPerClientID() throws Exception {
        inModule("login");
        raw("login", "name", testUser.getLogin(), "password", testUser.getPassword(), "client", "testclient1");
        raw("login", "name", testUser.getLogin(), "password", testUser.getPassword(), "client", "testclient2");
        // @formatter:off
        long counter = currentClient.getCookieStore()
                                    .getCookies()
                                    .stream()
                                    .filter(cookie -> cookie.getName().startsWith("open-xchange-secret"))
                                    .count();
        // @formatter:on
        assertTrue(counter == 2, "Missing secret cookie: " + getCookiesAsString());
    }

    @Test
    public void testSecretCookieLifetimeIsLongerThanADay() throws Exception {
        rawLogin(testUser);
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = TimeTools.D("tomorrow").toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        // @formatter:off
        boolean correctExpiry = currentClient.getCookieStore()
                                     .getCookies()
                                     .stream()
                                     .filter(cookie -> cookie.getName().startsWith("open-xchange-session") && cookie.getMaxAge() > 0)
                                     .map(cookie -> today.plus(TimeUnit.SECONDS.toDays(cookie.getMaxAge()), ChronoUnit.DAYS))
                                     .allMatch(exp -> exp.isAfter(tomorrow));
        // @formatter:on
        assertTrue(correctExpiry);
    }

    @Test
    public void testSuccessfulLoginAllowsSubsequentRequests() throws Exception {
        asUser(testUser);
        inModule("quota");
        call("filestore"); // Send some request.

        assertNoError();
    }

    @Test
    public void testRefreshSecretActionResetsSecretCookieLifetime() throws Exception {
        rawLogin(testUser);

        // @formatter:off
        Optional<Long> oldExp = currentClient.getCookieStore()
                                     .getCookies()
                                     .stream()
                                     .filter(cookie -> cookie.getName().startsWith("open-xchange-secret") && cookie.getMaxAge() > 0)
                                     .map(cookie -> L(cookie.getMaxAge()))
                                     .findAny();
        // @formatter:on

        assertTrue(oldExp.isPresent(), "Precondition: Should find secret cookie first");

        Thread.sleep(2000);
        raw(AJAXServlet.ACTION_REFRESH_SECRET, AJAXServlet.PARAMETER_SESSION, rawResponse.getString(AJAXServlet.PARAMETER_SESSION));

        // @formatter:off
        Optional<Long> newExp = currentClient.getCookieStore()
                                     .getCookies()
                                     .stream()
                                     .filter(cookie -> cookie.getName().startsWith("open-xchange-secret") && cookie.getMaxAge() > 0)
                                     .map(cookie -> L(cookie.getMaxAge()))
                                     .findAny();
        // @formatter:on

        assertTrue(newExp.isPresent(), "Precondition: Should find secret cookie after renewal");
        assertTrue(l(newExp.get()) - l(oldExp.get()) < 2, "Refreshed secret cookie should have newer expiry date");
    }

    // Error Cases
    @Test
    public void testWrongCredentials() throws Exception {
        inModule("login");
        call("login", "name", "foo", "password", "bar");

        assertError();
    }

    @Test
    public void testNonExistingSessionIDOnSubsequentRequests() throws Exception {
        asUser(testUser);
        inModule("quota");
        call("filestore", "session", "1234567"); // Send some request, and override the sessionID.

        assertError();
    }

    @Test
    public void testSessionIDAndSecretMismatch() throws Exception {
        asUser(testUser);
        String sessionID = currentClient.getSessionID();

        asUser(testUser2);
        inModule("quota");
        call("filestore", "session", sessionID); // Send some request with user 1 session. Secrets will differ.

        assertError();
    }

    @Test
    public void testCookieHashSalt() throws Exception {
        rawLogin(testUser);
        String agent = currentClient.getUserAgent();
        String salt = "replaceMe1234567890";
        for (HttpCookie cookie : currentClient.getCookieStore().getCookies()) {
            if (cookie.getName().startsWith("open-xchange-secret")) {
                assertEquals("open-xchange-secret-" + getHash(agent, salt), cookie.getName().split("=")[0], "Bad cookie hash.");
            } else if (cookie.getName().startsWith("open-xchange-session")) {
                assertEquals("open-xchange-session-" + getHash(agent, salt), cookie.getName().split("=")[0], "Bad cookie hash.");
            }
        }
    }

    private void assertResponseContains(String key) throws Exception {
        rawLogin(testUser);
        assertRaw(new JSONAssertion().isObject().hasKey(key));
    }

    private void assertResponseLacks(String key) throws Exception {
        rawLogin(testUser);
        assertRaw(new JSONAssertion().isObject().lacksKey(key));
    }

    private void rawLogin(TestUser user) throws Exception {
        rawLogin(user, true);
    }

    private void rawLogin(TestUser user, boolean staySignedIn) throws Exception {
        inModule("login");
        raw("login", "name", user.getLogin(), "password", user.getPassword(), "staySignedIn", String.valueOf(staySignedIn));
    }

    private String getHash(String agent, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(agent.getBytes(Charsets.UTF_8));
        md.update("open-xchange-appsuite".getBytes(Charsets.UTF_8));
        md.update(salt.getBytes());
        return Pattern.compile("\\W").matcher(Base64.encode(md.digest())).replaceAll("");
    }

}
