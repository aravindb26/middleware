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

package com.openexchange.ajax.requesthandler.annotation.restricted;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AbstractAJAXActionAnnotationProcessor;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction.Type;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;
import com.openexchange.session.restricted.RestrictedAccessCheck;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tools.RestrictedActionUtil;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link RestrictedActionAnnotationProcessor}
 * Checks session for restricted authentication. If present, verifies scope
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.4
 */
public class RestrictedActionAnnotationProcessor extends AbstractAJAXActionAnnotationProcessor<RestrictedAction> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RestrictedActionAnnotationProcessor.class);

    /**
     * Initializes a new {@link RestrictedActionAnnotationProcessor}.
     */
    public RestrictedActionAnnotationProcessor() {
        super();
    }

    @Override
    protected Class<RestrictedAction> getAnnotation() {
        return RestrictedAction.class;
    }

    @Override
    protected void doProcess(RestrictedAction restrictedAction, AJAXActionService action, AJAXRequestData requestData, ServerSession session) throws OXException {
        Object passedScopes = session.getParameter(Session.PARAM_RESTRICTED);
        Object isOAuth = session.getParameter(Session.PARAM_IS_OAUTH);

        if (passedScopes == null && isOAuth == null) {
            return; // Not a restricted session or an oAuth access, just return
        }
        processRestrictedRequest(requestData, session, action, restrictedAction, RestrictedActionUtil.obtainPassedScopes(session, requestData));
    }

    /**
     * Processes the restricted request
     *
     * @param requestData The {@link AJAXRequestData}
     * @param session The session to check
     * @param action The {@link AJAXActionService}
     * @param restrAction The {@link RestrictedAction} annotation of the action
     * @param requiredScopes The required scopes
     * @param passedScope The scope
     * @throws OXException in case the session is not authorized
     */
    private static void processRestrictedRequest(AJAXRequestData requestData, ServerSession session, AJAXActionService action, RestrictedAction restrictedAction, Scope passedScope) throws OXException {
        // Check if this action requires full auth and rejects the request if not
        if (restrictedAction.requiresFullAuth()) {
            throw RestrictedActionExceptionCodes.ACCESS_DENIED.create("Access with restricted session unavailable");
        }
        // Perform custom checks if present
        if (restrictedAction.hasCustomRestrictedAccessCheck()) {
            applyCustomRestrictedAccessChecks(action, requestData, session, passedScope);
            return;
        }
        // Get the required scopes
        Set<String> requiredScopes = RestrictedActionUtil.getRequiredScope(restrictedAction);
        // Return if no required and passed scopes are present
        if (requiredScopes.isEmpty() && passedScope == null) {
            return; //Neither required scopes present nor passed scope was obtained; no session restrictions apply
        }
        // Indicates an empty RestrictedAction
        if (Type.EMPTY.equals(restrictedAction.type()) && requiredScopes.isEmpty()) {
            throw RestrictedActionExceptionCodes.NO_SCOPES.create(action.getClass().getSimpleName());
        }
        Set<String> restrictedScopes = new HashSet<>(passedScope.get());
        // Union and intersection
        restrictedScopes.retainAll(requiredScopes);
        // At least one must apply
        if (restrictedScopes.isEmpty()) {
            throw RestrictedActionExceptionCodes.INSUFFICIENT_SCOPES.create(toString(requiredScopes));
        }
    }

    /**
     * Applies the custom OAuth scope checks
     *
     * @param action The {@link AJAXActionService}
     * @param requestData The {@link AJAXRequestData}
     * @param session The session to check
     * @param scope The scope
     * @throws OXException
     */
    private static void applyCustomRestrictedAccessChecks(AJAXActionService action, AJAXRequestData requestData, ServerSession session, Scope scope) throws OXException {
        for (Method method : action.getClass().getMethods()) {
            if (false == method.isAnnotationPresent(RestrictedAccessCheck.class)) {
                continue;
            }
            if (false == hasCustomRestrictedAccessCheckSignature(method)) {
                LOG.warn("Method ''{}.{}'' is annotated with @RestrictedAccessCheck but its signature is invalid!", action.getClass(), method.getName());
                continue;
            }
            invokeMethod(method, action, requestData, session, scope);
            return;
        }
        throw RestrictedActionExceptionCodes.ACCESS_DENIED.create("No custom access check invoked for \"" + action.getClass().getName() + "\"");
    }

    /**
     * Checks the specified method against the specified action and invokes the method if applicable.
     * Upon successful invocation of the <code>method</code> this method returns; otherwise an exception
     * is thrown.
     *
     * @param method The method to check and optionally invoke
     * @param action The JSON action
     * @param requestData The request data
     * @param session The session
     * @param scope The scope
     * @throws OXException if an error is occurred during method execution/invocation or if there are insufficient scopes requested/enabled for this action/method
     */
    private static void invokeMethod(Method method, AJAXActionService action, AJAXRequestData requestData, ServerSession session, Scope scope) throws OXException {
        try {
            if (((Boolean) method.invoke(action, requestData, session, scope)).booleanValue()) {
                return;
            }
            throw RestrictedActionExceptionCodes.INSUFFICIENT_SCOPES.create("");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OXException) {
                throw (OXException) cause;
            }
            throw RestrictedActionExceptionCodes.UNEXPECTED_ERROR.create(cause.getMessage(), cause);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            LOG.error("Could not check scope", e);
            throw RestrictedActionExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Checks whether the method annotated with {@link RestrictedAccessCheck} has the correct signature.
     *
     * @param method The method to check
     * @return <code>true</code> if the method is valid, <code>false</code> otherwise
     */
    private static boolean hasCustomRestrictedAccessCheckSignature(Method method) {
        if (false == Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        if (false == method.getReturnType().isAssignableFrom(boolean.class)) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 3) {
            return parameterTypes[0].isAssignableFrom(AJAXRequestData.class) && parameterTypes[1].isAssignableFrom(ServerSession.class) && parameterTypes[2].isAssignableFrom(Scope.class);
        }
        return false;
    }

    /**
     * Converts the specified strings to a space separated string
     *
     * @param strings The strings in a set
     * @return The string
     */
    private static String toString(Set<String> strings) {
        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            sb.append(s).append(" ");
        }
        if (sb.length() == 0) {
            return sb.toString();
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
