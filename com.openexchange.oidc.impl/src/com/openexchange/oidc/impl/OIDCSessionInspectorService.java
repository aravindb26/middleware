package com.openexchange.oidc.impl;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.oidc.OIDCBackend;
import com.openexchange.oidc.osgi.OIDCBackendRegistry;
import com.openexchange.oidc.state.StateManagement;
import com.openexchange.oidc.tools.OIDCTools;
import com.openexchange.session.Reply;
import com.openexchange.session.Session;
import com.openexchange.session.inspector.Reason;
import com.openexchange.session.inspector.SessionInspectorService;
import com.openexchange.session.oauth.RefreshResult;
import com.openexchange.session.oauth.SessionOAuthTokenService;
import com.openexchange.sessiond.ExpirationReason;
import com.openexchange.sessiond.SessionExceptionCodes;

/**
 * {@link OIDCSessionInspectorService} Is triggered on each Request, that comes
 * with a {@link Session} parameter. Is used to check on expired OAuth tokens, if storage of
 * those is enabled for the current backend.
 *
 * @author <a href="mailto:vitali.sjablow@open-xchange.com">Vitali Sjablow</a>
 * @since v7.10.0
 */
public class OIDCSessionInspectorService extends AbstractOIDCTokenRefreshTriggerer<Reply> implements SessionInspectorService{

    private static final Logger LOG = LoggerFactory.getLogger(OIDCSessionInspectorService.class);

    /**
     * Initializes a new {@link OIDCSessionInspectorService}.
     *
     * @param oidcBackendRegistry The OIDC back-end registry
     * @param tokenService The token service
     * @param stateManagement The state management
     */
    public OIDCSessionInspectorService(OIDCBackendRegistry oidcBackendRegistry, SessionOAuthTokenService tokenService, StateManagement stateManagement) {
        super(oidcBackendRegistry, tokenService, stateManagement);
    }

    @Override
    public Reply onSessionHit(Session session, HttpServletRequest request, HttpServletResponse response) throws OXException {
        if (session.getParameter(OIDCTools.IDTOKEN) == null) {
            // session not managed by us
            LOG.debug("Skipping unmanaged session '{}'", session.getSessionID());
            return Reply.NEUTRAL;
        }

        Optional<OIDCBackend> optBackend = this.loadBackendForSession(session);
        if (optBackend.isEmpty()) {
            LOG.warn("Unable to load OIDC backend for session '{}' due to missing path parameter", session.getSessionID());
            return Reply.NEUTRAL;
        }

        try {
            return triggerCheckOrRefreshTokens(session, optBackend.get());
        } catch (OXException e) {
            if (SessionExceptionCodes.SESSION_EXPIRED.equals(e)) {
                // ignore session expired errors
                throw e;
            }
            LOG.error("Error while checking OAuth tokens for session '{}'", session.getSessionID(), e);
            // try to perform request anyway on best effort
            return Reply.NEUTRAL;
        } catch (InterruptedException e) {
            LOG.warn("Thread was interrupted while checking session OAuth tokens");
            // keep interrupted state
            Thread.currentThread().interrupt();
            return Reply.STOP;
        }
    }

    @Override
    protected Reply handleSuccessResult(Session session, OIDCBackend backend, RefreshResult result) {
        LOG.debug("Returning neutral reply for session '{}' due to successful token refresh result: {}", session.getSessionID(), result.getSuccessReason().name());
        return Reply.NEUTRAL;
    }

    @Override
    protected Reply handleErrorResult(Session session, OIDCBackend backend, RefreshResult result, boolean sessionRemoved) throws OXException {
        if (sessionRemoved) {
            OXException oxe = SessionExceptionCodes.SESSION_EXPIRED.create(session.getSessionID());
            oxe.setProperty(SessionExceptionCodes.OXEXCEPTION_PROPERTY_SESSION_EXPIRATION_REASON, ExpirationReason.OAUTH_TOKEN_REFRESH_FAILED.getIdentifier());
            throw oxe;
        }

        // try to perform request anyway on best effort
        return Reply.NEUTRAL;
    }

    @Override
    public Reply onSessionMiss(String sessionId, HttpServletRequest request, HttpServletResponse response) throws OXException {
        return Reply.NEUTRAL;
    }

    @Override
    public Reply onAutoLoginFailed(Reason reason, HttpServletRequest request, HttpServletResponse response) throws OXException {
        return Reply.NEUTRAL;
    }

}
