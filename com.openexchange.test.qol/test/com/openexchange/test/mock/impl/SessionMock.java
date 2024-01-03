
package com.openexchange.test.mock.impl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.session.Session;
import static org.mockito.ArgumentMatchers.*;

@NonNullByDefault
public enum SessionMock {
    ;

    public abstract static class Builder<T extends Builder<T, V>, V extends Session> {

        public abstract T getThis();

        public T authId(String authId) {
            Mockito.when(getSession().getAuthId()).thenReturn(authId);
            return getThis();
        }

        public T client(String client) {
            Mockito.when(getSession().getClient()).thenReturn(client);
            return getThis();
        }

        public T contextId(int contextId) {
            Mockito.when(getSession().getContextId()).thenReturn(contextId);
            return getThis();
        }

        public T hash(String hash) {
            Mockito.when(getSession().getHash()).thenReturn(hash);
            return getThis();
        }

        public T localIp(String localIp) {
            Mockito.when(getSession().getLocalIp()).thenReturn(localIp);
            return getThis();
        }

        public T login(String login) {
            Mockito.when(getSession().getLogin()).thenReturn(login);
            return getThis();
        }

        public T loginName(String loginName) {
            Mockito.when(getSession().getLoginName()).thenReturn(loginName);
            return getThis();
        }

        public T parameters() {
            Map<String, Object> parameters = new ConcurrentHashMap<String, Object>();
            PowerMockito.doAnswer(setParameterAnswer(parameters)).when(getSession()).setParameter(any(String.class), any(Object.class));
            PowerMockito.doAnswer(getParameterAnswer(parameters)).when(getSession()).getParameter(any(String.class));
            PowerMockito.doAnswer(getParameterNamesAnswer(parameters)).when(getSession()).getParameterNames();
            return getThis();
        }

        private Answer<Set<String>> getParameterNamesAnswer(final Map<String, Object> parameters) {
            return new Answer<Set<String>>() {

                @Override
                public Set<String> answer(@Nullable InvocationOnMock invocation) throws Throwable {
                    return parameters.keySet();
                }
            };
        }

        private Answer<Object> getParameterAnswer(final Map<String, Object> parameters) {
            return new Answer<Object>() {

                @Override
                public Object answer(@Nullable InvocationOnMock invocation) throws Throwable {
                    String key = invocation.getArgument(0);
                    return parameters.get(key);
                }
            };
        }

        private Answer<Void> setParameterAnswer(final Map<String, Object> parameters) {
            return new Answer<Void>() {

                @Override
                public @Nullable Void answer(@Nullable InvocationOnMock invocation) throws Throwable {
                    String key = invocation.getArgument(0);
                    Object value = invocation.getArgument(1);
                    if (null == value) {
                        parameters.remove(key);
                    } else {
                        parameters.put(key, value);
                    }
                    return null;
                }
            };
        }


        public T password(String password) {
            Mockito.when(getSession().getPassword()).thenReturn(password);
            return getThis();
        }

        public T randomToken(String random) {
            Mockito.when(getSession().getRandomToken()).thenReturn(random);
            return getThis();
        }

        public T secret(String secret) {
            Mockito.when(getSession().getSecret()).thenReturn(secret);
            return getThis();
        }

        public T sessionId(String sessionId) {
            Mockito.when(getSession().getSessionID()).thenReturn(sessionId);
            return getThis();
        }

        public T userId(int userId) {
            Mockito.when(getSession().getUserId()).thenReturn(userId);
            return getThis();
        }

        public T userLogin(String usrLogin) {
            Mockito.when(getSession().getUserlogin()).thenReturn(usrLogin);
            return getThis();
        }

        public V build() {
            return getSession();
        }

        public abstract V getSession();
    }

    public static class SessionBuilder extends Builder<SessionBuilder, Session> {

        private final Session session;

        public SessionBuilder() {
            super();
            this.session  = Mockito.mock(Session.class);
        }

        @Override
        public SessionBuilder getThis() {
            return this;
        }

        @Override
        public Session getSession() {
            return session;
        }

    }

    public static SessionMock.SessionBuilder builder() {
        return new SessionMock.SessionBuilder();
    }

}
