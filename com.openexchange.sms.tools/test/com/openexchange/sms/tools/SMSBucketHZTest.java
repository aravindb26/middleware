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

package com.openexchange.sms.tools;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableFactory;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.hazelcast.serialization.CustomPortable;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.sms.tools.internal.SMSBucket;
import com.openexchange.sms.tools.internal.SMSBucketServiceImpl;
import com.openexchange.sms.tools.osgi.Services;

/**
 * {@link SMSBucketHZTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.1
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class SMSBucketHZTest {

    @Mock
    private ConfigViewFactory factory;

    @Mock
    private ConfigView view;

    private static HazelcastInstance hz1;

    private static HazelcastInstance hz2;

    private SMSBucketService smsBucketService;
    private SMSBucketService smsBucketService2;

    private static Session fake;

    @BeforeClass
    public static void initHazelcast() {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
        config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1");
        config.getSerializationConfig().addPortableFactory(CustomPortable.FACTORY_ID, new PortableFactory() {

            @Override
            public Portable create(int classId) {
                return new SMSBucket();
            }
        });
        // defaults from samlAuthnRequestInfos.properties
        MapConfig mapConfig = new MapConfig("smsBucketHZ");
        mapConfig.setBackupCount(1);
        mapConfig.setAsyncBackupCount(0);
        mapConfig.setReadBackupData(false);
        mapConfig.setMaxIdleSeconds(120000);
        config.addMapConfig(mapConfig);
        // other tests may also spawn hz instances. check for running instances to ensure correct test results.
        checkForRunningHZInstances();
        hz1 = Hazelcast.newHazelcastInstance(config);
        hz2 = Hazelcast.newHazelcastInstance(config);
    }

    @AfterClass
    public static void shutdownHazelcast() {
        if (hz1 != null) {
            hz1.shutdown();
        }
        if (hz2 != null) {
            hz2.shutdown();
        }
    }

    @Before
    public void initSMSBucketService() throws OXException {

        Services.setServiceLookup(new ServiceLookup() {

            @SuppressWarnings({ "unchecked", "synthetic-access" })
            @Override
            public <S> S getService(Class<? extends S> clazz) {
                return (S) factory;
            }

            @Override
            public <S> S getOptionalService(Class<? extends S> clazz) {
                return null;
            }
        });
        PowerMockito.when(factory.getView(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(view);
        PowerMockito.when(view.get(SMSConstants.SMS_USER_LIMIT_ENABLED, boolean.class)).thenReturn(B(true));
        PowerMockito.when(view.get(SMSConstants.SMS_USER_LIMIT_REFRESH_INTERVAL, String.class)).thenReturn("2");
        PowerMockito.when(view.get(SMSConstants.SMS_USER_LIMIT_PROPERTY, String.class)).thenReturn("3");

        // Create fake session with userID and contextId of 1
        fake = PowerMockito.mock(Session.class, new Answer<Integer>() {

            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return I(1);
            }
        });

        smsBucketService = new SMSBucketServiceImpl(hz1);
        smsBucketService2 = new SMSBucketServiceImpl(hz2);
    }

    @Test(expected = OXException.class)
    public void testSMSBucket() throws OXException {
        /*
         * Request tokens until the an exception is thrown by alternating between the two services
         */
        for (int x = 3; x > -5; x--) {
            SMSBucketService service = x % 2 == 0 ? smsBucketService : smsBucketService2;
            assertEquals("Unexpected amount", x, service.getSMSToken(fake));
            assertTrue(x > -1);
        }
    }
    
    private static void checkForRunningHZInstances() {
        final int MAX_RETRY = 30;
        int counter=0;
        Set<HazelcastInstance> instances = Hazelcast.getAllHazelcastInstances();
        while (!instances.isEmpty() && counter < MAX_RETRY) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            } finally {
                counter++;
            }
        }
        if (!instances.isEmpty()) {
            fail("There are already running Hazelcast instances.");
        }
    }

}
