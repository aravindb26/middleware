package com.openexchange.database.tombstone.cleanup.config;

import static com.openexchange.java.Autoboxing.L;
import com.openexchange.config.ConfigTools;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.database.tombstone.cleanup.osgi.Services;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.ReadOnlyValue;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.session.Session;
import com.openexchange.user.User;

/**
 * {@link TombstoneCleanupTimespan}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v8.0.0
 */
public class TombstoneCleanupTimespan implements PreferencesItemService {

    @Override
    public String[] getPath() {
        return new String[] { "tombstoneCleanup", "timespan" };
    }

    @Override
    public IValueHandler getSharedValue() {
        return new ReadOnlyValue() {

            @Override
            public boolean isAvailable(UserConfiguration userConfig) {
                LeanConfigurationService leanConfig = Services.getService(LeanConfigurationService.class);
                return leanConfig.getBooleanProperty(TombstoneCleanupConfig.ENABLED);
            }

            @Override
            public void getValue(Session session, Context ctx, User user, UserConfiguration userConfig, Setting setting) throws OXException {
                LeanConfigurationService leanConfig = Services.getService(LeanConfigurationService.class);
                String timespanStr = leanConfig.getProperty(TombstoneCleanupConfig.TIMESPAN).trim();
                long timespan = ConfigTools.parseTimespan(timespanStr);
                if (timespan < 1) {
                    timespan = ConfigTools.parseTimespan(TombstoneCleanupConfig.TIMESPAN.getDefaultValue(String.class));
                }
                setting.setSingleValue(L(timespan));
            }
        };
    }

}
