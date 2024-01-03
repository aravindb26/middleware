
package com.openexchange.test.mock.impl;

import org.mockito.Mockito;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.test.mock.impl.SessionMock.Builder;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

@NonNullByDefault
public enum ServerSessionMock {
    ;

    public static class ServerSessionBuilder extends Builder<ServerSessionBuilder, ServerSession> {

        private final ServerSession session;

        public ServerSessionBuilder() {
            super();
            session = Mockito.mock(ServerSession.class);
        }

        @Override
        public ServerSessionBuilder getThis() {
            return this;
        }

        @Override
        public ServerSession getSession() {
            return session;
        }

        public ServerSessionBuilder context(Context context) {
            Mockito.when(session.getContext()).thenReturn(context);
            return getThis();
        }

        public ServerSessionBuilder user(User user) {
            Mockito.when(session.getUser()).thenReturn(user);
            return getThis();
        }

        public ServerSessionBuilder userConfiguration(UserConfiguration userConfiguration) {
            Mockito.when(session.getUserConfiguration()).thenReturn(userConfiguration);
            return getThis();
        }

        public ServerSessionBuilder userSettingMail(UserSettingMail UserSettingMail) {
            Mockito.when(session.getUserSettingMail()).thenReturn(UserSettingMail);
            return getThis();
        }

        public ServerSessionBuilder anonymous(boolean isAnonymous) {
            Mockito.when(session.isAnonymous()).thenReturn(isAnonymous);
            return getThis();
        }

        public ServerSessionBuilder userPermissionBits(UserPermissionBits getUserPermissionBits) {
            Mockito.when(session.getUserPermissionBits()).thenReturn(getUserPermissionBits);
            return getThis();
        }
    }

    public static ServerSessionMock.ServerSessionBuilder builder() {
        return new ServerSessionMock.ServerSessionBuilder();
    }
}
