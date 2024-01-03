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

package com.openexchange.exception.interception.internal;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.exception.interception.OXExceptionInterceptor;
import com.openexchange.exception.interception.Responsibility;

/**
 * Registry that handles all registered {@link OXExceptionInterceptor} for processing within {@link OXException} creation
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a> Thread-safe collection
 * @since 7.6.1
 */
public class OXExceptionInterceptorRegistration {

    /** Logger for this class **/
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(OXExceptionInterceptorRegistration.class);

    /** Singleton instance for this registration **/
    private static final OXExceptionInterceptorRegistration instance = new OXExceptionInterceptorRegistration();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static OXExceptionInterceptorRegistration getInstance() {
        return instance;
    }

    // ----------------------------------------------------------------------------------------------------------------------------

    /** Comparator to sort registered {@link OXExceptionInterceptor} **/
    private final Comparator<OXExceptionInterceptor> comparator;

    /** List with all registered interceptors **/
    private final List<OXExceptionInterceptor> interceptors;

    private OXExceptionInterceptorRegistration() {
        super();
        this.interceptors = new CopyOnWriteArrayList<OXExceptionInterceptor>();
        this.comparator = new Comparator<OXExceptionInterceptor>() {

            @Override
            public int compare(OXExceptionInterceptor o1, OXExceptionInterceptor o2) {
                int rank1 = o1.getRanking();
                int rank2 = o2.getRanking();
                return (rank1 < rank2 ? -1 : (rank1 == rank2 ? 0 : 1));
            }
        };
    }

    /**
     * Adds an {@link OXExceptionInterceptor} to intercept exception throwing. If an interceptor should be added where a similar one is
     * already registered for (means ranking, module and action is equal) it won't be added.
     *
     * @param {@link OXExceptionInterceptor} to add
     * @return <code>true</code> if interceptor is added; otherwise <code>false</code>
     */
    public synchronized boolean put(OXExceptionInterceptor interceptor) {
        if (interceptor == null) {
            LOG.error("Interceptor to add might not be null!");
            return false;
        }

        if (isResponsibleInterceptorRegistered(interceptor)) {
            LOG.error("Interceptor for the given ranking {} and desired module/action combination already registered! Discard the new one from type: {}", I(interceptor.getRanking()), interceptor.getClass());
            return false;
        }
        this.interceptors.add(interceptor);
        return true;
    }

    /**
     * Checks if an {@link OXExceptionInterceptor} with same ranking and module/action combination is already registered
     *
     * @param interceptorCandidate The {@link OXExceptionInterceptor} that might be added
     * @return boolean<code>true</code> if a {@link OXExceptionInterceptor} is already registered for the given ranking and module/action combination, otherwise <code>false</code>
     */
    public boolean isResponsibleInterceptorRegistered(OXExceptionInterceptor interceptorCandidate) {
        for (OXExceptionInterceptor interceptor : this.interceptors) {
            if (interceptor.getRanking() != interceptorCandidate.getRanking()) {
                continue;
            }
            for (Responsibility responsibility : interceptor.getResponsibilities()) {
                for (Responsibility candidateResponsibility : interceptorCandidate.getResponsibilities()) {
                    if (responsibility.implies(candidateResponsibility)) {
                        // There is another interceptor with the same ranking covering the same responsibility
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Removes an {@link OXExceptionInterceptor} to not intercept exception throwing
     *
     * @param interceptor - {@link OXExceptionInterceptor} to remove
     */
    public synchronized void remove(final OXExceptionInterceptor interceptor) {
        this.interceptors.remove(interceptor);
    }

    /**
     * Returns all registered {@link OXExceptionInterceptor}s ranked
     *
     * @return a ranked list with all registered {@link OXExceptionInterceptor}s
     */
    public List<OXExceptionInterceptor> getRegisteredInterceptors() {
        // Add all interceptors
        List<OXExceptionInterceptor> lInterceptors = new ArrayList<OXExceptionInterceptor>(this.interceptors);

        // Now order them according to service ranking
        Collections.sort(lInterceptors, comparator);
        return lInterceptors;
    }

    /**
     * Returns all {@link OXExceptionInterceptor}s that are responsible for this module/action combination ranked.
     *
     * @param module The module to get the responsible interceptors for
     * @param action The action to get the responsible interceptors for
     * @return A ranked list with all registered {@link OXExceptionInterceptor}s that are responsible for the given module/action
     *         combination
     */
    public List<OXExceptionInterceptor> getResponsibleInterceptors(String module, String action) {
        // Collect responsible interceptors
        List<OXExceptionInterceptor> lInterceptors = null;
        for (OXExceptionInterceptor interceptor : this.interceptors) {
            if (interceptor.isResponsible(module, action)) {
                if (lInterceptors == null) {
                    lInterceptors = new ArrayList<OXExceptionInterceptor>(2);
                }
                lInterceptors.add(interceptor);
            }
        }

        // None collected
        if (lInterceptors == null) {
            return Collections.emptyList();
        }

        // Sort by ranking & return
        if (lInterceptors.size() > 1) {
            Collections.sort(lInterceptors, comparator);
        }
        return lInterceptors;
    }
}
