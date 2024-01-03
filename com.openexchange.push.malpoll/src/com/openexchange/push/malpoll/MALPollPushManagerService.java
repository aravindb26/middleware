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

package com.openexchange.push.malpoll;

import static com.openexchange.java.Autoboxing.I;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViews;
import com.openexchange.exception.OXException;
import com.openexchange.push.PushListener;
import com.openexchange.push.PushManagerService;
import com.openexchange.push.malpoll.services.MALPollServiceRegistry;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.session.Session;

/**
 * {@link MALPollPushManagerService} - The MAL poll {@link PushManagerService} for primary mail account.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MALPollPushManagerService implements PushManagerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MALPollPushManagerService.class);

    private static volatile boolean startTimerTaskPerListener;

    /**
     * Sets whether to start a timer task per listener.
     *
     * @param startTimerTaskPerListener <code>true</code> to start a timer task per listener; otherwise <code>false</code>
     */
    public static void setStartTimerTaskPerListener(final boolean startTimerTaskPerListener) {
        MALPollPushManagerService.startTimerTaskPerListener = startTimerTaskPerListener;
    }

    private final String name;

    /**
     * Initializes a new {@link MALPollPushManagerService}.
     */
    public MALPollPushManagerService() {
        super();
        name = "MAL Poll Push Manager";
    }

    @Override
    public PushListener startListener(final Session session) throws OXException {
        int contextId = session.getContextId();
        int userId = session.getUserId();
        if (false == isMalPollPushEnabledForUser(userId, contextId)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.info("Denied starting MAL Poll listener for user {} in context {} with session {} since disabled via configuration.", I(userId), I(contextId), session.getSessionID(), new Throwable("MAL Poll start listener trace"));
            } else {
                LOGGER.info("Denied starting MAL Poll listener for user {} in context {} with session {} since disabled via configuration.", I(userId), I(contextId), session.getSessionID());
            }
            return null;
        }
        final MALPollPushListener pushListener = MALPollPushListener.newInstance(session, startTimerTaskPerListener);
        if (MALPollPushListenerRegistry.getInstance().addPushListener(contextId, userId, pushListener)) {
            pushListener.open();
            return pushListener;
        }
        return null;
    }

    @Override
    public boolean stopListener(final Session session) {
        return MALPollPushListenerRegistry.getInstance().removePushListener(
            session.getContextId(),
            session.getUserId());
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Checks if MAL Poll push is enabled for given user
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return <code>true</code> if MAL Poll push is enabled for user, <code>false</code> otherwise
     * @throws OXException On error
     */
    private boolean isMalPollPushEnabledForUser(int userId, int contextId) throws OXException {
        ConfigViewFactory factory = MALPollServiceRegistry.getServiceRegistry().getService(ConfigViewFactory.class, false);
        if (factory == null) {
            throw ServiceExceptionCode.absentService(ConfigViewFactory.class);
        }

        ConfigView view = factory.getView(userId, contextId);
        return ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.push.malpoll.enabled", false, view);
    }

}
