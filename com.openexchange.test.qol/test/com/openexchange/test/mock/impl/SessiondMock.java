
package com.openexchange.test.mock.impl;

import static org.mockito.ArgumentMatchers.any;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.authentication.SessionEnhancement;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.UUIDs;
import com.openexchange.session.Session;
import com.openexchange.sessiond.AddSessionParameter;
import com.openexchange.sessiond.SessiondService;

@NonNullByDefault
public class SessiondMock {

    public static SessiondMock builder() {
        return new SessiondMock();
    }

    private final SessiondService sessiondService;

    public SessiondService build() {
        return sessiondService;
    }

    public SessiondMock() {
        sessiondService = Mockito.mock(SessiondService.class);
    }

    public SessiondMock withSessionMap() throws OXException {
        Map<String, Session> sessionMap = new HashMap<String, Session>();
        PowerMockito.doAnswer(addSessionAnswer(sessionMap)).when(sessiondService).addSession(any(AddSessionParameter.class));
        PowerMockito.doAnswer(getSessionAnswer(sessionMap)).when(sessiondService).getSession(any(String.class));
        return this;
    }

    private Answer<Session> getSessionAnswer(final Map<String, Session> sessionMap) {
        return new Answer<Session>() {

            @Override
            public Session answer(@Nullable InvocationOnMock invocation) throws Throwable {
                String sessionId = invocation.getArgument(0);
                return sessionMap.get(sessionId);
            }
        };
    }

    private Answer<Session> addSessionAnswer(final Map<String, Session> sessionMap) {
        return new Answer<Session>() {

            @Override
            public Session answer(@Nullable InvocationOnMock invocation) throws Throwable {
                AddSessionParameter parameterObject = invocation.getArgument(0);

                Session session = SessionMock.builder().sessionId(UUIDs.getUnformattedStringFromRandom()).randomToken(UUIDs.getUnformattedStringFromRandom()).parameters().build();
                // only one method left with 7.10.3
                List<SessionEnhancement> enhancements = parameterObject.getEnhancements();
                for (final SessionEnhancement enhancement : enhancements) {
                    if (enhancement != null) {
                        enhancement.enhanceSession(session);
                    }
                }
                sessionMap.put(session.getSessionID(), session);
                return session;
            }

        };
    }
}
