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

package com.openexchange.net.ssl.config.impl.internal;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.net.ssl.config.UserAwareSSLConfigurationService;
import com.openexchange.user.UserService;

/**
 * {@link UserAwareSSLConfigurationImplTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.3
 */
public class UserAwareSSLConfigurationImplTest {

    @Mock
    private UserService userService;

    @Mock
    private ContextService contextService;

    @Mock
    private ConfigView configView;

    @Mock
    private Context context;

    @Mock
    private LeanConfigurationService configService;

    private UserAwareSSLConfigurationService userAwareSSLConfigurationService;

    private final int userId = 111;

    private final int contextId = 2;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        Mockito.when(this.contextService.getContext(contextId)).thenReturn(this.context);
        Mockito.when(I(this.context.getContextId())).thenReturn(I(this.contextId));
        Mockito.when(this.configService.getProperty(SSLProperties.TRUST_LEVEL)).thenReturn("false");
    }

    @Test
    public void testIsAllowedToDefineTrustLevel_contextIdInvalid_returnFalse() {
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService);

        boolean trustAll = this.userAwareSSLConfigurationService.isAllowedToDefineTrustLevel(userId, 0);

        assertFalse(trustAll);
    }

    @Test
    public void testIsAllowedToDefineTrustLevel_userIdInvalid_returnFalse() {
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService);

        boolean trustAll = this.userAwareSSLConfigurationService.isAllowedToDefineTrustLevel(0, contextId);

        assertFalse(trustAll);
    }

    @SuppressWarnings("boxing")
    @Test
    public void testIsAllowedToDefineTrustLevel_propertyNotAvailabled_returnFalse() {
        Mockito.when(this.configService.getBooleanProperty(userId, contextId, UserAwareSSLConfigurationService.USER_CONFIG_ENABLED_PROPERTY)).thenReturn(Boolean.FALSE);
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService);

        boolean trustAll = this.userAwareSSLConfigurationService.isAllowedToDefineTrustLevel(userId, contextId);

        assertFalse(trustAll);
    }

    @SuppressWarnings("boxing")
    @Test
    public void testIsAllowedToDefineTrustLevel_disabledByConfig_returnFalse() {
        Mockito.when(this.configService.getBooleanProperty(userId, contextId, UserAwareSSLConfigurationService.USER_CONFIG_ENABLED_PROPERTY)).thenReturn(Boolean.FALSE);
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService);

        boolean trustAll = this.userAwareSSLConfigurationService.isAllowedToDefineTrustLevel(userId, contextId);

        assertFalse(trustAll);
    }

    @SuppressWarnings("boxing")
    @Test
    public void testIsAllowedToDefineTrustLevel_enabledByConfig_returnTrue() {
        Mockito.when(this.configService.getBooleanProperty(userId, contextId, UserAwareSSLConfigurationService.USER_CONFIG_ENABLED_PROPERTY)).thenReturn(Boolean.TRUE);
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService);

        boolean trustAll = this.userAwareSSLConfigurationService.isAllowedToDefineTrustLevel(userId, contextId);

        assertTrue(trustAll);
    }

    @Test
    public void testIsTrustAll_userNotAllowed_returnFalse() {
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService) {

            @Override
            public boolean isAllowedToDefineTrustLevel(int userId, int contextId) {
                return false;
            }
        };

        boolean trustAll = this.userAwareSSLConfigurationService.isTrustAll(this.userId, this.contextId);

        assertFalse(trustAll);
    }

    @Test
    public void testIsTrustAll_userAttributeNotSet_returnFalse() throws OXException {
        Mockito.when(this.userService.getUserAttribute(UserAwareSSLConfigurationService.USER_ATTRIBUTE_NAME, userId, this.context)).thenReturn(null);
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService) {

            @Override
            public boolean isAllowedToDefineTrustLevel(int userId, int contextId) {
                return true;
            }
        };

        boolean trustAll = this.userAwareSSLConfigurationService.isTrustAll(this.userId, this.contextId);

        assertFalse(trustAll);
    }

    @Test
    public void testIsTrustAll_userAttributeSetToFalse_returnFalse() throws OXException {
        Mockito.when(this.userService.getUserAttribute(UserAwareSSLConfigurationService.USER_ATTRIBUTE_NAME, userId, this.context)).thenReturn("false");
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService) {

            @Override
            public boolean isAllowedToDefineTrustLevel(int userId, int contextId) {
                return true;
            }
        };

        boolean trustAll = this.userAwareSSLConfigurationService.isTrustAll(this.userId, this.contextId);

        assertFalse(trustAll);
    }

    @Test
    public void testIsTrustAll_userAttributeSetToTrue_returnTrue() throws OXException {
        Mockito.when(this.userService.getUserAttribute(UserAwareSSLConfigurationService.USER_ATTRIBUTE_NAME, userId, this.context)).thenReturn("true");
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService) {

            @Override
            public boolean isAllowedToDefineTrustLevel(int userId, int contextId) {
                return true;
            }
        };

        boolean trustAll = this.userAwareSSLConfigurationService.isTrustAll(this.userId, this.contextId);

        assertTrue(trustAll);
    }

    @Test
    public void testSetTrustAll_userIdNotCorrect_notSet() throws OXException {
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService) {

            @Override
            public boolean isAllowedToDefineTrustLevel(int userId, int contextId) {
                return false;
            }
        };
        this.userAwareSSLConfigurationService.setTrustAll(0, context, true);

        Mockito.verify(this.userService, Mockito.never()).setUserAttribute(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), (Context) ArgumentMatchers.any());
    }

    @Test
    public void testSetTrustAll_contextIdNotCorrect_notSet() throws OXException {
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService) {

            @Override
            public boolean isAllowedToDefineTrustLevel(int userId, int contextId) {
                return true;
            }
        };
        this.userAwareSSLConfigurationService.setTrustAll(userId, null, true);

        Mockito.verify(this.userService, Mockito.never()).setUserAttribute(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), (Context) ArgumentMatchers.any());
    }

    @Test
    public void testSetTrustAll_everythingOk_set() throws OXException {
        this.userAwareSSLConfigurationService = new UserAwareSSLConfigurationImpl(userService, contextService, configService) {

            @Override
            public boolean isAllowedToDefineTrustLevel(int userId, int contextId) {
                return true;
            }
        };

        this.userAwareSSLConfigurationService.setTrustAll(userId, context, true);

        Mockito.verify(this.userService, Mockito.times(1)).setUserAttribute(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), (Context) ArgumentMatchers.any());
    }
}
