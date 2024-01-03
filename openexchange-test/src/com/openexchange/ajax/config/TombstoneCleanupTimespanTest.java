package com.openexchange.ajax.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.testing.httpclient.models.ConfigResponse;
import com.openexchange.testing.httpclient.modules.ConfigApi;

/**
 * {@link TombstoneCleanupTimespanTest}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v8.0.0
 */
public class TombstoneCleanupTimespanTest extends AbstractConfigAwareAPIClientSession {

    private static final String TIMESPAN_PROPERTY_PATH = "/tombstoneCleanup/timespan";

    @Test
    public void testTimespan() throws Throwable {
        ConfigApi configApi = new ConfigApi(getApiClient());
        ConfigResponse response = configApi.getConfigNode(TIMESPAN_PROPERTY_PATH);
        assertNotNull(response.getData(), String.format("Config tree does not contain property: %s", TIMESPAN_PROPERTY_PATH));
        assertTrue(response.getData() instanceof Number);
    }
    
    @Test
    public void testTimespanFallback() throws Throwable {
        super.setUpConfiguration();
        ConfigApi configApi = new ConfigApi(getApiClient());
        ConfigResponse response = configApi.getConfigNode(TIMESPAN_PROPERTY_PATH);
        assertNotNull(response.getData(), String.format("Config tree does not contain property: %s", TIMESPAN_PROPERTY_PATH));
        assertTrue(response.getData() instanceof Number);
        long timespan = ((Number) response.getData()).longValue();
        assertEquals(7257600000L, timespan, "Config tree property contains wrong default value");

    }
    
    @Override
    protected Map<String, String> getNeededConfigurations() {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("com.openexchange.database.tombstone.cleanup.timespan", "invalid_value");
        return configuration;
    }

}
