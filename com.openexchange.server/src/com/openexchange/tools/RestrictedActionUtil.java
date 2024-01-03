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

package com.openexchange.tools;

import java.util.HashSet;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction.Type;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedActionExceptionCodes;
import com.openexchange.ajax.requesthandler.oauth.OAuthConstants;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.SystemContentType;
import com.openexchange.folderstorage.addressbook.AddressDataContentType;
import com.openexchange.folderstorage.database.contentType.CalendarContentType;
import com.openexchange.folderstorage.database.contentType.ContactsContentType;
import com.openexchange.folderstorage.database.contentType.InfostoreContentType;
import com.openexchange.folderstorage.database.contentType.TaskContentType;
import com.openexchange.folderstorage.mail.contentType.DraftsContentType;
import com.openexchange.folderstorage.mail.contentType.MailContentType;
import com.openexchange.folderstorage.mail.contentType.SentContentType;
import com.openexchange.folderstorage.mail.contentType.SpamContentType;
import com.openexchange.folderstorage.mail.contentType.TrashContentType;
import com.openexchange.groupware.modules.Module;
import com.openexchange.java.Strings;
import com.openexchange.oauth.provider.resourceserver.OAuthAccess;
import com.openexchange.session.Session;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link RestrictedActionUtil} - A static helper class to deal with restricted action stuff
 * such as content-types and scopes for
 * <code>calendar</code>, <code>contacts</code>, <code>files</code>, <code>mail</code> and <code>tasks</code>.
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Schuerholz</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class RestrictedActionUtil {

    /**
     * Checks whether the specified request is restricted by checking whether one of the following applies:
     * <li>the request contains the property {@link OAuthConstants#PARAM_OAUTH_ACCESS}</li>
     * <li>the session has the session parameter {@link Session#PARAM_RESTRICTED}</li>
     * <li>the session has the session parameter {@link Session#PARAM_IS_OAUTH}</li>
     *
     * @param requestData The request
     * @return <code>true</code> if the request is restricted; <code>false</code> otherwise.
     */
    public static boolean isRequestRestricted(AJAXRequestData requestData) {
        ServerSession session = requestData.getSession();
        if (session == null) {
            return false;
        }
        //@formatter:off
        return requestData.containsProperty(OAuthConstants.PARAM_OAUTH_ACCESS) ||
            session.getParameter(Session.PARAM_RESTRICTED) != null || 
            session.getParameter(Session.PARAM_IS_OAUTH) != null;
        //@formatter:on
    }

    /**
     * Obtains the {@link Scope} from the specified session and request data.
     *
     * @param session The session
     * @param requestData The request data
     * @return the passed scope from the client or <code>null</code> if no scope was passed and the session is not restricted
     * @throws OXException if the provided parameters do not conform to the restriction standards
     */
    public static Scope obtainPassedScopes(ServerSession session, AJAXRequestData requestData) throws OXException {
        Object passedScopes = session.getParameter(Session.PARAM_RESTRICTED);
        if (passedScopes != null) {
            // Check the scopes of authentication for this session
            if (false == (passedScopes instanceof String)) {
                throw RestrictedActionExceptionCodes.UNKNOWN_SESSION_TYPE.create();
            }
            return Scope.newInstance(Strings.splitByComma((String) passedScopes, new HashSet<String>()));
        }

        Object isOAuth = session.getParameter(Session.PARAM_IS_OAUTH);
        if (isOAuth == null) {
            return null; // Both restricted parameter and oauth parameter are null, no restrictions apply
        }
        OAuthAccess oAuthAccess = requestData.getProperty(OAuthConstants.PARAM_OAUTH_ACCESS);
        if (oAuthAccess == null) {
            throw RestrictedActionExceptionCodes.OAUTH_ACCESS_MISSING.create();
        }
        return oAuthAccess.getScope();
    }

    /**
     * Extracts the required scope from the specified restricted action
     *
     * @param restrAction The restricted action
     * @return A {@link Set} with the required scope, or an empty set if the action is not restricted or has no restriction type
     */
    public static Set<String> getRequiredScope(RestrictedAction restrictedAction) {
        if (restrictedAction == null) {
            return ImmutableSet.of();
        }
        String module = restrictedAction.module();
        if (Strings.isEmpty(module)) {
            return ImmutableSet.of();
        }
        return ImmutableSet.of(restrictedAction.type().getScope(module));
    }

    /**
     * Retrieves from the specified class the {@link RestrictedAction} annotation
     * (if present) and then returns the required scopes (if any)
     *
     * @param clazz The class from which to extract the required scopes
     * @return A set with the required scopes
     */
    public static Set<String> getRequiredScopes(Class<?> clazz) {
        return getRequiredScope(clazz.getAnnotation(RestrictedAction.class));
    }

    /**
     * Gets the {@link ContentType}s valid for the given read oauth scope
     *
     * @param scope The read oauth scope
     * @return A set of valid {@link ContentType}s
     */
    public static Set<ContentType> contentTypesForReadScope(String scope) {
        return contentTypesForModule(Module.getForName(Type.READ.getModule(scope)));
    }

    /**
     * Gets the {@link ContentType}s valid for the given write oauth scope
     *
     * @param scope The write oauth scope
     * @return A set of valid {@link ContentType}s
     */
    public static Set<ContentType> contentTypesForWriteScope(String scope) {
        return contentTypesForModule(Module.getForName(Type.WRITE.getModule(scope)));
    }

    /**
     * Returns the a set with the content types that pass to the specified module
     * 
     * @param module The module
     * @return The content types
     */
    private static Set<ContentType> contentTypesForModule(Module module) {
        if (module == null) {
            return ImmutableSet.of();
        }
        switch (module) {
            case CONTACTS:
                return ImmutableSet.of(AddressDataContentType.getInstance(), ContactsContentType.getInstance());
            case CALENDAR:
                return ImmutableSet.of(CalendarContentType.getInstance(), com.openexchange.folderstorage.calendar.contentType.CalendarContentType.getInstance());
            case TASK:
                return ImmutableSet.of((ContentType) TaskContentType.getInstance());
            case MAIL:
                return ImmutableSet.of(MailContentType.getInstance(), DraftsContentType.getInstance(), SentContentType.getInstance(), TrashContentType.getInstance(), SpamContentType.getInstance());
            case INFOSTORE:
                return ImmutableSet.of((ContentType) InfostoreContentType.getInstance());
            default:
                return ImmutableSet.of();
        }
    }

    /**
     * Checks whether write operations are permitted for the given folder content type and OAuth oauthAccess.
     *
     * @param contentType The content type
     * @param scope The scope
     * @return <code>true</code> if write operations are permitted
     * @throws OXException If write operations are not permitted due to missing required scope(s).
     */
    public static boolean mayWriteWithScope(ContentType contentType, Scope scope) throws OXException {
        String requiredScope = writeScopeForContentType(contentType);
        if (requiredScope != null && scope.has(requiredScope)) {
            return true;
        }
        throw RestrictedActionExceptionCodes.INSUFFICIENT_SCOPES.create(requiredScope);
    }

    /**
     * Checks whether read operations are permitted for the given folder content type and OAuth oauthAccess.
     *
     * @param contentType The content type
     * @param scope The scope
     * @return <code>true</code> if read operations are permitted
     * @throws OXException If read operations are not permitted due to missing required scope(s).
     */
    public static boolean mayReadWithScope(ContentType contentType, Scope scope) throws OXException {
        if (SystemContentType.getInstance().equals(contentType)) {
            return true; // always allow reading "system" folders
        }
        String requiredScope = readScopeForContentType(contentType);
        if (requiredScope != null && scope.has(requiredScope)) {
            return true;
        }
        throw RestrictedActionExceptionCodes.INSUFFICIENT_SCOPES.create(requiredScope);
    }

    /**
     * Checks whether write operations are permitted for the given folder content types and OAuth oauthAccess.
     *
     * @param contentTypes The content types
     * @param scope The scope
     * @return <code>true</code> if write operations are permitted
     * @throws OXException If write operations are not permitted due to missing required scope(s).
     */
    public static boolean mayWriteWithScope(Set<ContentType> contentTypes, Scope scope) throws OXException {
        for (ContentType contentType : contentTypes) {
            mayWriteWithScope(contentType, scope);
        }
        return true;
    }

    /**
     * Checks whether read operations are permitted for the given folder content types and OAuth oauthAccess.
     *
     * @param contentTypes The content types
     * @param scope The scope
     * @return <code>true</code> if read operations are permitted
     * @throws OXException If read operations are not permitted due to missing required scope(s).
     */
    public static boolean mayReadWithScope(Set<ContentType> contentTypes, Scope scope) throws OXException {
        for (ContentType contentType : contentTypes) {
            mayReadWithScope(contentType, scope);
        }
        return true;
    }

    /**
     * 
     * Gets the scope consisting of the prefix <code>read_</code> and the content type.
     *
     * @param contentType The content type.
     * @return The scope.
     */
    public static String readScopeForContentType(ContentType contentType) {
        if (contentType == AddressDataContentType.getInstance() || contentType == ContactsContentType.getInstance()) {
            return Type.READ.getScope(Module.CONTACTS.getName());
        } else if (contentType == CalendarContentType.getInstance()) {
            return Type.READ.getScope(Module.CALENDAR.getName());
        } else if (contentType == com.openexchange.folderstorage.calendar.contentType.CalendarContentType.getInstance()) {
            return Type.READ.getScope(Module.CALENDAR.getName());
        } else if (contentType == TaskContentType.getInstance()) {
            return Type.READ.getScope(Module.TASK.getName());
        } else if (contentType == MailContentType.getInstance() || contentType == DraftsContentType.getInstance() || contentType == SentContentType.getInstance() || contentType == SpamContentType.getInstance() || contentType == TrashContentType.getInstance()) {
            return Type.READ.getScope(Module.MAIL.getName());
        } else if (contentType == InfostoreContentType.getInstance()) {
            return Type.READ.getScope(Module.INFOSTORE.getName());
        }

        return null;
    }

    /**
     * 
     * Gets the scope consisting of the prefix <code>write_</code> and the content type.
     *
     * @param contentType The content type.
     * @return The scope.
     */
    public static String writeScopeForContentType(ContentType contentType) {
        if (contentType == AddressDataContentType.getInstance() || contentType == ContactsContentType.getInstance()) {
            return Type.WRITE.getScope(Module.CONTACTS.getName());
        } else if (contentType == CalendarContentType.getInstance()) {
            return Type.WRITE.getScope(Module.CALENDAR.getName());
        } else if (contentType == com.openexchange.folderstorage.calendar.contentType.CalendarContentType.getInstance()) {
            return Type.WRITE.getScope(Module.CALENDAR.getName());
        } else if (contentType == TaskContentType.getInstance()) {
            return Type.WRITE.getScope(Module.TASK.getName());
        } else if (contentType == MailContentType.getInstance() || contentType == DraftsContentType.getInstance() || contentType == SentContentType.getInstance() || contentType == SpamContentType.getInstance() || contentType == TrashContentType.getInstance()) {
            return Type.WRITE.getScope(Module.MAIL.getName());
        } else if (contentType == InfostoreContentType.getInstance()) {
            return Type.WRITE.getScope(Module.INFOSTORE.getName());
        }

        return null;
    }

    /**
     * Initialises a new {@link RestrictedActionUtil}.
     */
    private RestrictedActionUtil() {
        throw new IllegalStateException("Utility class");
    }
}
