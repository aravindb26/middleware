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


package com.openexchange.pns.transport.webhooks;

import static com.openexchange.java.Autoboxing.I;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViews;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.push.PushClientChecker;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.webhooks.WebhookInfo;
import com.openexchange.webhooks.WebhookProperty;
import com.openexchange.webhooks.WebhookRegistry;

/**
 * {@link WebhookPushClientChecker} - Allows mail push for registered Webhooks.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class WebhookPushClientChecker implements PushClientChecker {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebhookPushClientChecker.class);

    private final ServiceLookup services;

    /**
     * Initializes a new {@link WebhookPushClientChecker}.
     *
     * @param services The service look-up
     */
    public WebhookPushClientChecker(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public boolean isAllowed(String clientId, Session session) throws OXException {
        if (null == session || Strings.isEmpty(clientId)) {
            // Unable to check
            return false;
        }

        String webhookId = clientId;
        Optional<WebhookInfo> optConfiguredWebhook = services.getServiceSafe(WebhookRegistry.class).getConfiguredWebhook(webhookId);
        if (optConfiguredWebhook.isEmpty()) {
            // No such Webhook configuration available
            LOG.warn("Denied Webhook mail push registration: No such Webhook for identifier {} in configuration", webhookId);
            return false;
        }

        ConfigViewFactory viewFactory = services.getServiceSafe(ConfigViewFactory.class);
        ConfigView view = viewFactory.getView(session.getUserId(), session.getContextId());
        String sEnabledIds = ConfigViews.getDefinedStringPropertyFrom(WebhookProperty.ENABLED_IDS.getFQPropertyName(), WebhookProperty.ENABLED_IDS.getDefaultValue().toString(), view).trim();
        if (Strings.isEmpty(sEnabledIds) || !new HashSet<>(Arrays.asList(Strings.splitBy(sEnabledIds, ',', true))).contains(webhookId)) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Denied Webhook mail push registration: Webhook {} not contained in enabled Webhooks for user {} in context {}: {}", webhookId, I(session.getUserId()), I(session.getContextId()), sEnabledIds);
            } else {
                LOG.warn("Denied Webhook mail push registration: Webhook {} not contained in enabled Webhooks for user {} in context {}", webhookId, I(session.getUserId()), I(session.getContextId()));
            }
            return false;
        }

        // All fine
        return true;
    }

}
