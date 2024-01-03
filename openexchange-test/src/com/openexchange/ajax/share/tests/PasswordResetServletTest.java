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

package com.openexchange.ajax.share.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.ajax.smtptest.MailManager;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.java.util.UUIDs;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.share.recipient.GuestRecipient;
import com.openexchange.testing.httpclient.models.MailData;

/**
 * {@link PasswordResetServletTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.8.0
 */
@Execution(ExecutionMode.SAME_THREAD)
public final class PasswordResetServletTest extends ShareTest {

    private OCLGuestPermission guestPermission;
    private FolderObject folder;
    private String shareURL;
    private GuestClient guestClient;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        OCLGuestPermission lGuestPermission = createNamedGuestPermission();
        /*
         * create folder shared to guest user
         */
        int module = randomModule();
        EnumAPI api = randomFolderAPI();
        folder = insertSharedFolder(randomFolderAPI(), module, getDefaultFolder(module), lGuestPermission);
        /*
         * check permissions
         */
        OCLPermission matchingPermission = null;
        for (OCLPermission permission : folder.getPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created folder found");
        checkPermissions(lGuestPermission, matchingPermission);
        /*
         * discover & check share
         */
        ExtendedPermissionEntity lGuest = discoverGuestEntity(api, module, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(lGuestPermission, lGuest);
        /*
         * check access to share
         */
        shareURL = discoverShareURL(lGuestPermission.getApiClient(), lGuest);
        guestClient = resolveShare(shareURL, ((GuestRecipient) lGuestPermission.getRecipient()).getEmailAddress(), ((GuestRecipient) lGuestPermission.getRecipient()).getPassword());
        guestClient.checkShareModuleAvailable();
        this.guestPermission = lGuestPermission;
    }

    @Test
    public void testResetPassword_passwordReset() throws Exception {
        // http://localhost/ajax/share/1100ba1e0f0652b8849d7f3f066049e390589313a77026ef
        URI shareUrl = new URI(shareURL);
        String[] pathSegments = shareUrl.getPath().split("/");
        String token = null;
        for (String segment : pathSegments) {
            Matcher matcher = Pattern.compile("[a-f0-9]{48}", Pattern.CASE_INSENSITIVE).matcher(segment);
            if (matcher.matches()) {
                token = matcher.group();
                break;
            }
        }
        if (token == null) {
            fail("got no token from share link");
        }
        try (CloseableHttpClient httpClient = guestClient.getSession().getHttpClient()) {
            // http://localhost/ajax/share/reset/password?share=1100ba1e0f0652b8849d7f3f066049e390589313a77026ef&confirm=FIMvTtnmQ7Dv_N97CRENJy6rTYw
            HttpGet resetPasswordRequest = new HttpGet(new URIBuilder().setScheme(getClient().getProtocol()).setHost(getClient().getHostname()).setPath("/ajax/share/reset/password").setParameter("share", token).build());
            try (CloseableHttpResponse resetPasswordResp = httpClient.execute(resetPasswordRequest)) {
                EntityUtils.consume(resetPasswordResp.getEntity());
            }
            Thread.sleep(1000);
            PWResetData resetData = getConfirmationToken();
            HttpPost confirmPWReset = new HttpPost(new URIBuilder().setScheme(getClient().getProtocol()).setHost(getClient().getHostname()).setPath("/ajax/share/reset/password").build());
            String newPW = UUIDs.getUnformattedStringFromRandom();
            List<BasicNameValuePair> params = new ArrayList<>(3);
            params.add(new BasicNameValuePair("share", resetData.getShareToken()));
            params.add(new BasicNameValuePair("confirm", resetData.getConfirmationToken()));
            params.add(new BasicNameValuePair("password", newPW));
            confirmPWReset.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse confirmResponse = httpClient.execute(confirmPWReset);
            EntityUtils.consume(confirmResponse.getEntity());
            assertEquals(302, confirmResponse.getStatusLine().getStatusCode(), "Response was no redirect");
            Header locationHeader = confirmResponse.getFirstHeader(HttpHeaders.LOCATION);
            assertNotNull(locationHeader, "Missing location header");
            URI location = new URI(locationHeader.getValue());
            String[] kvPairs = location.getRawFragment().split("&");
            String sessionId = null;
            for (String pair : kvPairs) {
                String[] splitted = pair.split("=");
                if (splitted.length == 2) {
                    if ("session".equals(splitted[0])) {
                        sessionId = URLDecoder.decode(splitted[1], "UTF-8");
                    }
                }
            }

            assertNotNull(sessionId, "Missing session ID in redirect location");

            // Login again to verify
            GuestClient guestClient = resolveShare(shareURL, ((GuestRecipient) guestPermission.getRecipient()).getEmailAddress(), newPW);
            guestClient.checkShareAccessible(guestPermission);
            guestClient.logout();
        }
    }

    private PWResetData getConfirmationToken() throws Exception {
        List<MailData> messages = new MailManager(guestPermission.getApiClient()).getMails();
        assertEquals(2, messages.size());
        @SuppressWarnings("unchecked") Optional<Map<String, String>> optHeader = messages.stream().map(m -> (Map<String, String>) m.getHeaders()).filter(h -> h.get("X-Open-Xchange-Share-Reset-PW-URL") != null).findAny();
        String url = optHeader.get().get("X-Open-Xchange-Share-Reset-PW-URL");
        assertNotNull(url, "Missing X-Open-Xchange-Share-Reset-PW-URL in confirmation mail");
        String query = new URI(url).getRawQuery();
        String[] kvPairs = query.split("&");
        PWResetData pwResetData = new PWResetData();
        for (String kvPair : kvPairs) {
            String[] kv = kvPair.split("=");
            if (kv.length == 2) {
                if ("confirm".equals(kv[0])) {
                    pwResetData.setConfirmationToken(URLDecoder.decode(kv[1], "UTF-8"));
                } else if ("share".equals(kv[0])) {
                    pwResetData.setShareToken(URLDecoder.decode(kv[1], "UTF-8"));
                }
            }
        }

        assertNotNull(pwResetData.getShareToken(), "Cannot extract share token from URL: " + url);
        assertNotNull(pwResetData.getConfirmationToken(), "Cannot extract confirmation token from URL: " + url);
        return pwResetData;
    }

    /**
     *
     * {@link PWResetData}
     *
     * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
     * @since 7.8.0
     */
    private static final class PWResetData {

        private String shareToken;
        private String confirmationToken;

        /**
         * Initializes a new {@link PasswordResetServletTest.PWResetData}.
         */
        public PWResetData() {
            super();
        }

        /**
         * Gets the shareToken
         *
         * @return The shareToken
         */
        public String getShareToken() {
            return shareToken;
        }

        /**
         * Gets the confirmationToken
         *
         * @return The confirmationToken
         */
        public String getConfirmationToken() {
            return confirmationToken;
        }

        /**
         * Sets the shareToken
         *
         * @param shareToken The shareToken to set
         */
        public void setShareToken(String shareToken) {
            this.shareToken = shareToken;
        }

        /**
         * Sets the confirmationToken
         *
         * @param confirmationToken The confirmationToken to set
         */
        public void setConfirmationToken(String confirmationToken) {
            this.confirmationToken = confirmationToken;
        }
    }

}
