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

package com.openexchange.ajax.passwordchange;

import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.PasswordChangeBody;
import com.openexchange.testing.httpclient.modules.PasswordchangeApi;
import com.openexchange.user.UserExceptionMessage;

/**
 * {@link PasswordChangeTest} - Tests the UPDATE request on password
 * change servlet in combination with an external password change script. Especially
 * verify that UTF-8 characters reach the script.
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 *
 */
public final class PasswordChangeTest extends AbstractConfigAwareAPIClientSession {

    @Override
    @BeforeEach
    public void setUp(TestInfo info) throws Exception {
        super.setUp(info);
        changeConfigWithOwnClient(testUser, ImmutableMap.of(//
            "com.openexchange.passwordchange.db.enabled", Boolean.TRUE.toString()));
    }

    @Test
    public void testScriptBasedUpdate() throws Exception {
        /*
         * Setup user
         */
        changeConfigWithOwnClient(testUser, ImmutableMap.of(//
            "com.openexchange.passwordchange.script.enabled", Boolean.TRUE.toString(), //
            "com.openexchange.passwordchange.script.base64", Boolean.TRUE.toString() ,//
            "com.openexchange.passwordchange.script.shellscript", ""));
        PasswordchangeApi api = new PasswordchangeApi(getApiClient());
        PasswordChangeBody body = new PasswordChangeBody();
        body.setNewPassword("(\u0298\u203f\u0298)");
        body.setOldPassword(testUser.getPassword());
        /*
         * Perform update and check for error from the script based service
         */
        CommonResponse response = api.updatePassword(body);
        assertThat(response.getError(), is(not(nullValue())));
        assertThat(response.getError() + ":" + response.getErrorDesc() + "\n" + response.getErrorStack(), response.getError(), is("Failed to change password for any reason."));
    }

    @Test
    public void testUpdate() throws Exception {
        /*
         * Setup change
         */
        PasswordchangeApi api = new PasswordchangeApi(getApiClient());
        PasswordChangeBody body = new PasswordChangeBody();
        body.setNewPassword("(\u0298\u203f\u0298)");
        body.setOldPassword(testUser.getPassword());
        /*
         * Perform update and expect change password
         */
        CommonResponse response = api.updatePassword(body);
        assertThat(response.getError(), is(nullValue()));
    }

    @Test
    public void testUpdate_MissingOldPwd() throws Exception {
        /*
         * Setup change
         */
        PasswordchangeApi api = new PasswordchangeApi(getApiClient());
        PasswordChangeBody body = new PasswordChangeBody();
        body.setNewPassword("(\u0298\u203f\u0298)");
        body.setOldPassword("");
        /*
         * Perform update and expect change password
         */
        CommonResponse response = api.updatePassword(body);
        assertThat(response.getError(), is(not(nullValue())));
        assertThat(response.getError(), is(UserExceptionMessage.MISSING_CURRENT_PASSWORD_DISPLAY));
    }

    @Test
    public void testUpdate_MissingNewPwd() throws Exception {
        /*
         * Setup change
         */
        PasswordchangeApi api = new PasswordchangeApi(getApiClient());
        PasswordChangeBody body = new PasswordChangeBody();
        body.setNewPassword("");
        body.setOldPassword(testUser.getPassword());
        /*
         * Perform update and expect change password
         */
        CommonResponse response = api.updatePassword(body);
        assertThat(response.getError(), is(not(nullValue())));
        assertThat(response.getError(), is(UserExceptionMessage.MISSING_NEW_PASSWORD_DISPLAY));
    }

    @Test
    public void testUpdate_TooShortNewPwd() throws Exception {
        /*
         * Setup change
         */
        PasswordchangeApi api = new PasswordchangeApi(getApiClient());
        PasswordChangeBody body = new PasswordChangeBody();
        body.setNewPassword("123");
        body.setOldPassword(testUser.getPassword());
        /*
         * Perform update and expect change password
         */
        CommonResponse response = api.updatePassword(body);
        assertThat(response.getError(), is(not(nullValue())));
        assertThat(response.getError(), is(String.format(UserExceptionMessage.INVALID_MIN_LENGTH_DISPLAY, I(4))));
    }

    @Test
    public void testUpdate_WrongOldPwd() throws Exception {
        /*
         * Setup change
         */
        PasswordchangeApi api = new PasswordchangeApi(getApiClient());
        PasswordChangeBody body = new PasswordChangeBody();
        body.setNewPassword("(\u0298\u203f\u0298)");
        body.setOldPassword(testUser.getPassword() + "false");
        /*
         * Perform update and expect change password
         */
        CommonResponse response = api.updatePassword(body);
        assertThat(response.getError(), is(not(nullValue())));
        assertThat(response.getError(), is(UserExceptionMessage.INCORRECT_CURRENT_PASSWORD_DISPLAY));
    }
}
