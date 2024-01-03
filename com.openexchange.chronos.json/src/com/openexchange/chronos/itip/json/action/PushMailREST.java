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

package com.openexchange.chronos.itip.json.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.scheduling.common.MailPushListener;
import com.openexchange.chronos.scheduling.common.MailPushListener.PushMail;
import com.openexchange.exception.OXException;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.mail.mime.converters.MimeMessageConverter;
import com.openexchange.rest.services.annotation.Role;
import com.openexchange.rest.services.annotation.RoleAllowed;
import com.openexchange.server.ServiceLookup;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.ThreadPools;

/**
 * 
 * {@link PushMailREST}
 *
 * @author <a href="mailto:martin.herfurthr@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.6
 */
@Path("/chronos/v1/itip/pushmail")
@RoleAllowed(Role.BASIC_AUTHENTICATED)
public class PushMailREST {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {

        static final Logger LOGGER = LoggerFactory.getLogger(PushMailREST.class);
    }

    protected final ServiceLookup services;

    /**
     * Initializes a new {@link PushMailREST}.
     *
     * @param services The service lookup
     */
    public PushMailREST(ServiceLookup services) {
        this.services = services;
    }

    /**
     * Notifies about a new iTIP or rather iMIP mail
     *
     * @param json The payload as JSON
     * @return <code>200</code> if the payload was paresed sucessfully, <code>400</code> if not
     * @throws OXException In case the message can't be parsed
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response notify(JSONObject json) throws OXException {
        PushMail pushMail = new PushMail();
        try {
            if (json.has("event")) {
                String event = json.getString("event");
                if (false == "messageNew".equalsIgnoreCase(event)) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
                pushMail.setEvent(event);
            }
            if (json.has("body") && json.has("folder")) {
                try (ByteArrayInputStream is = new ByteArrayInputStream(json.getString("body").getBytes(StandardCharsets.UTF_8))) {
                    MimeMessage msg = new MimeMessage(MimeDefaultSession.getDefaultSession(), is);
                    MailMessage mailMessage = MimeMessageConverter.convertMessage(msg);
                    mailMessage.setFolder(json.getString("folder"));
                    pushMail.setMail(mailMessage);
                    if (json.has("user")) {
                        pushMail.setUser(json.getString("user"));
                    }
                    notify(pushMail);
                    return Response.ok().build();
                }
            }
        } catch (JSONException | MessagingException | IOException e) {
            LoggerHolder.LOGGER.debug("Bad request: {}", e.getMessage(), e);
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    private void notify(PushMail pushMail) {
        ThreadPools.submitElseExecute(new AbstractTask<Void>() {

            @Override
            public Void call() throws Exception {
                MailPushListener mailPushListener = services.getService(MailPushListener.class);
                if (null == mailPushListener) {
                    LoggerHolder.LOGGER.info("Unable to handle push message. Missing service {}", MailPushListener.class.getSimpleName());
                } else {
                    try {
                        mailPushListener.pushMail(pushMail);
                    } catch (OXException e) {
                        LoggerHolder.LOGGER.debug("Error while processing: {}", e.getMessage(), e);
                        throw e;
                    }
                }
                return null;
            }
        });
    }

}
