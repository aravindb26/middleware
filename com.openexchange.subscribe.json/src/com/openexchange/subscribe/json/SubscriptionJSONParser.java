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

package com.openexchange.subscribe.json;

import java.util.Map;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.datatypes.genericonf.json.FormContentParser;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.subscribe.Subscription;
import com.openexchange.subscribe.SubscriptionErrorMessage;
import com.openexchange.subscribe.SubscriptionSource;
import com.openexchange.subscribe.SubscriptionSourceDiscoveryService;

/**
 * {@link SubscriptionJSONParser}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class SubscriptionJSONParser {

    private final SubscriptionSourceDiscoveryService discovery;

    /**
     * Initializes a new {@link SubscriptionJSONParser}.
     *
     * @param discovery
     */
    public SubscriptionJSONParser(SubscriptionSourceDiscoveryService discovery) {
        super();
        this.discovery = discovery;
    }

    public Subscription parse(JSONObject object) throws JSONException, OXException {
        Subscription subscription = new Subscription();
        if (object.has("id")) {
            subscription.setId(object.getInt("id"));
        }
        if (object.has("folder")) {
            String folderId = object.getString("folder");
            subscription.setFolderId(adjustFolderId(folderId));
        }
        if (object.has("enabled")) {
            subscription.setEnabled(object.getBoolean("enabled"));
        }
        if (object.has("source")) {
            SubscriptionSource source = discovery.getSource(object.getString("source"));
            subscription.setSource(source);
            if (source != null) {
                JSONObject config = object.optJSONObject(subscription.getSource().getId());
                if (config != null) {
                    Map<String, Object> configuration = FormContentParser.parse(config, source.getFormDescription());
                    subscription.setConfiguration(configuration);
                }
            }
        }
        return subscription;
    }

    /**
     * Adjusts a client-supplied folder identifier where the targeted groupware object is located at to be used within the internal
     * subscription service.
     * <p/>
     * This can be used to still allow composite folder identifiers (for contacts folders) in the API.
     * 
     * @param requestedFolderId The folder identifier as requested by the client
     * @return The optionally adjusted folder identifier (in <i>unmangled</i> form ) for further usage, or an empty {@link Optional} instance if not applicable
     * @throws OXException In case folder ID can't be parsed
     */
    public static String adjustFolderId(String requestedFolderId) throws OXException {
        if (-1 != Strings.parsePositiveInt(requestedFolderId)) {
            return requestedFolderId; // use numerical identifier as-is
        }
        /*
         * adjust to relative id if folder originates from default contacts account
         */
        try {
            if (ContactsAccount.DEFAULT_ACCOUNT.getAccountId() == com.openexchange.contact.provider.composition.IDMangling.getAccountId(requestedFolderId)) {
                return com.openexchange.contact.provider.composition.IDMangling.getRelativeFolderId(requestedFolderId);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(SubscriptionJSONParser.class).debug("Couldn't extract contact account id from {}", requestedFolderId, e);
        }
        throw SubscriptionErrorMessage.ParseException.create("Unable to parse folder ID");
    }

}
