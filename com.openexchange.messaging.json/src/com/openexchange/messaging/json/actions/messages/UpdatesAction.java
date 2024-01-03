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

package com.openexchange.messaging.json.actions.messages;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.caching.Cache;
import com.openexchange.exception.OXException;
import com.openexchange.messaging.json.MessagingMessageParser;
import com.openexchange.messaging.json.MessagingMessageWriter;
import com.openexchange.messaging.registry.MessagingServiceRegistry;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link UpdatesAction}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class UpdatesAction extends AbstractMessagingAction {

    public UpdatesAction(final MessagingServiceRegistry registry, final MessagingMessageWriter writer, final MessagingMessageParser parser) {
        super(registry, writer, parser);
    }

    public UpdatesAction(final MessagingServiceRegistry registry, final MessagingMessageWriter writer, final MessagingMessageParser parser, final Cache cache) {
        super(registry, writer, parser, cache);
    }



    @Override
    protected AJAXRequestResult doIt(final MessagingRequestData messagingRequestData, final ServerSession session) throws JSONException, IOException, OXException {
        return new AJAXRequestResult(new JSONArray());
    }

}
