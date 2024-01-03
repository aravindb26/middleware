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

package com.openexchange.passwordchange.impl.script;

import static com.openexchange.passwordchange.impl.script.ScriptPasswordChangeProperties.BASE64_KEY;
import static com.openexchange.passwordchange.impl.script.ScriptPasswordChangeProperties.SCRIPT_PATH_KEY;
import static com.openexchange.passwordchange.impl.script.ScriptPasswordExceptionCode.PASSWORD_FAILED_WITH_MSG;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.passwordchange.PasswordChangeEvent;
import com.openexchange.passwordchange.common.ConfigAwarePasswordChangeService;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.encoding.Base64;
import com.openexchange.user.User;

/**
 * {@link ScriptPasswordChangeService}
 *
 * @author <a href="mailto:manuel.kraft@open-xchange.com">Manuel Kraft</a>
 */
public final class ScriptPasswordChangeService extends ConfigAwarePasswordChangeService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ScriptPasswordChangeService.class);
    private static final int RANKING = 1000;
    private static final String PROVIDER_ID = "script";

    /**
     * Initializes a new {@link ScriptPasswordChangeService}
     *
     * @param services The service lookup
     * @throws OXException If services are missing
     */
    public ScriptPasswordChangeService(ServiceLookup services) throws OXException {
        super(services);
    }

    @Override
    public int getRanking() {
        return RANKING;
    }

    @Override
    protected String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    protected void update(final PasswordChangeEvent event) throws OXException {
        String shellScriptToExecute = configService.getProperty(event.getSession().getUserId(), event.getContext().getContextId(), SCRIPT_PATH_KEY);
        if (Strings.isEmpty(shellScriptToExecute)) {
            String message = "Shell script could not be found. Please check property '" + SCRIPT_PATH_KEY.getFQPropertyName() + "'";
            LOG.error(message);
            throw PASSWORD_FAILED_WITH_MSG.create(message);
        }

        User user = userService.getUser(event.getSession().getUserId(), event.getContext());
        String username = user.getLoginInfo();
        if (null == username) {
            LOG.error("User name is null.");
            throw PASSWORD_FAILED_WITH_MSG.create("User name is null.");
        }
        String oldPassword = event.getOldPassword();
        if (null == oldPassword) {
            LOG.error("Old password is null.");
            throw PASSWORD_FAILED_WITH_MSG.create("Old password is null.");
        }
        String newPassword = event.getNewPassword();
        if (null == newPassword) {
            LOG.error("New password is null.");
            throw PASSWORD_FAILED_WITH_MSG.create("New password is null.");
        }
        final String cid = Integer.toString(event.getContext().getContextId());
        final String userid = Integer.toString(user.getId());

        // Convert UTF-8 bytes of String to base64
        boolean asBase64 = configService.getBooleanProperty(event.getSession().getUserId(), event.getContext().getContextId(), BASE64_KEY);
        if (asBase64) {
            username = Base64.encode(username);
            oldPassword = Base64.encode(oldPassword);
            newPassword = Base64.encode(newPassword);
        }

        /*
         * Update passwd via executing a shell script
         *
         * Following values must be passed to the script in given order:
         *
         * 0. cid - Context ID
         * 1. user - Username of the logged in user
         * 2. userid - User ID of the logged in user
         * 3. oldPassword - Old user password
         * 4. newPassword - New user password
         */

        final String[] cmd = new String[11];
        cmd[0] = shellScriptToExecute; // the script, after that, the parameters
        cmd[1] = "--cid";
        cmd[2] = cid;
        cmd[3] = "--username";
        cmd[4] = username;
        cmd[5] = "--userid";
        cmd[6] = userid;
        cmd[7] = "--oldpassword";
        cmd[8] = oldPassword;
        cmd[9] = "--newpassword";
        cmd[10] = newPassword; //

        LOG.debug("Executing following command to change password: {}", Arrays.toString(cmd));

        try {
            final int ret = executePasswordUpdateShell(cmd);
            if (ret != 0) {
                LOG.error("Passwordchange script returned exit code != 0, ret={}", Integer.valueOf(ret));
                switch (ret) {
                    case 1 -> throw ScriptPasswordExceptionCode.PASSWORD_FAILED.create(" failed with return code " + ret + " ");
                    case 2 -> throw ScriptPasswordExceptionCode.PASSWORD_SHORT.create();
                    case 3 -> throw ScriptPasswordExceptionCode.PASSWORD_WEAK.create();
                    case 4 -> throw ScriptPasswordExceptionCode.PASSWORD_NOUSER.create();
                    case 5 -> throw ScriptPasswordExceptionCode.LDAP_ERROR.create();
                    default -> throw ServiceExceptionCode.IO_ERROR.create();
                }
            }
        } catch (IOException e) {
            LOG.error("IO error while changing password for user {} in context {}\n", username, cid, e);
            throw ServiceExceptionCode.IO_ERROR.create(e);
        } catch (InterruptedException e) {
            // Restore the interrupted status; see http://www.ibm.com/developerworks/java/library/j-jtp05236/index.html
            Thread.currentThread().interrupt();
            LOG.error("Error while changing password for user {} in context {}\n", username, cid, e);
            throw ServiceExceptionCode.IO_ERROR.create(e);
        }

    }

    private int executePasswordUpdateShell(final String[] cmd) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd);

        InputStream stdout = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            stdout = proc.getInputStream();
            isr = new InputStreamReader(stdout, StandardCharsets.UTF_8);
            br = new BufferedReader(isr);

            for (String line; (line = br.readLine()) != null;) {
                LOG.debug("PWD CHANGE: {}", line);
            }
            Streams.close(br, isr, stdout);
            br = null;
            isr = null;
            stdout = null;

            return proc.waitFor();
        } finally {
            Streams.close(br, isr, stdout);
        }
    }

}
