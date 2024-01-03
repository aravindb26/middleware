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

package com.openexchange.java.security.osgi;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.Provider;
import org.osgi.framework.BundleContext;
import com.openexchange.config.ConfigurationService;
import com.openexchange.java.security.internal.JavaSecurityProviderReplacer;
import com.openexchange.osgi.MultipleServiceTracker;
import com.openexchange.startup.SignalStartedService;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link JavaSecurityProviderReplaceTrigger}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class JavaSecurityProviderReplaceTrigger extends MultipleServiceTracker {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(JavaSecurityProviderReplaceTrigger.class);
    }

    private ScheduledTimerTask timerTask;

    /**
     * Initializes a new {@link JavaSecurityProviderReplaceTrigger}.
     *
     * @param context The bundle context
     */
    public JavaSecurityProviderReplaceTrigger(BundleContext context) {
        super(context, SignalStartedService.class, TimerService.class, ConfigurationService.class);
    }

    @Override
    protected void onAllAvailable() {
        // Initialize security providers
        ConfigurationService configService = getTrackedService(ConfigurationService.class);
        boolean useAllProvider = configService.getBoolProperty("com.openexchange.java.security.useAllProvider", false);
        if (configService.getBoolProperty("com.openexchange.java.security.forceReplace", false)) {
            LoggerHolder.LOG.info("Going to replace Java Security Providers since forced through configuration...");
            JavaSecurityProviderReplacer.replaceAndStartTimer(useAllProvider, getTrackedService(TimerService.class));
        } else {
            if (configService.getBoolProperty("com.openexchange.java.security.denyReplace", false)) {
                LoggerHolder.LOG.info("Denied replacement of Java Security Providers through configuration...");
            } else if (isMethodSynchronized()) {
                LoggerHolder.LOG.info("Going to replace Java Security Providers since \"java.security.Provider.getService(String, String)\" is synchronized...");
                JavaSecurityProviderReplacer.replaceAndStartTimer(useAllProvider, getTrackedService(TimerService.class));
            } else {
                LoggerHolder.LOG.info("No replacement of Java Security Providers necessary...");
            }
        }
    }

    private boolean isMethodSynchronized() {
        try {
            Method method = Provider.class.getDeclaredMethod("getService", String.class, String.class);
            return Modifier.isSynchronized(method.getModifiers());
        } catch (NoSuchMethodException e) {
            LoggerHolder.LOG.info("Found no \"java.security.Provider.getService(String, String)\" method. Leaving Java Security Providers unchanged...", e);
        } catch (Exception e) {
            LoggerHolder.LOG.error("Error while checking \"java.security.Provider.getService(String, String)\" method. Leaving Java Security Providers unchanged...", e);
        }
        return false;
    }

    @Override
    protected boolean serviceRemoved(Object service) {
        // Stop timer task
        ScheduledTimerTask timerTask = this.timerTask;
        if (timerTask != null) {
            this.timerTask = null;
            timerTask.cancel();
        }

        // Restore security providers
        JavaSecurityProviderReplacer.restoreIfNecessary();

        return true;
    }

}
