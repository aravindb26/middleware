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

package com.openexchange.usecount.json.action;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.json.ImmutableJSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.ContactService;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contact.provider.composition.IDBasedContactsAccessFactory;
import com.openexchange.contactcollector.ContactCollectorService;
import com.openexchange.exception.OXException;
import com.openexchange.group.GroupService;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.ldap.LdapExceptionCode;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.objectusecount.IncrementArguments;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.principalusecount.Args;
import com.openexchange.principalusecount.PrincipalUseCountService;
import com.openexchange.resource.ResourceService;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.id.IDMangler;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.usecount.json.Type;
import com.openexchange.user.UserExceptionCode;
import com.openexchange.user.UserService;


/**
 * {@link IncrementUseCountAction} - Performs the "increment" action of the use-count module.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
@RestrictedAction(module = "contacts", type = RestrictedAction.Type.READ)
public class IncrementUseCountAction extends AbstractUseCountAction {

    private final JSONObject statusSuccess;

    /**
     * Initializes a new {@link IncrementUseCountAction}.
     *
     * @param services The service look-up
     */
    public IncrementUseCountAction(ServiceLookup services) {
        super(services);
        statusSuccess = ImmutableJSONObject.immutableFor(new JSONObject(2).putSafe("success", Boolean.TRUE));
    }

    @Override
    protected AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException, JSONException {
        JSONValue jBody = requireJSONBody(requestData);
        if (jBody.isArray()) {
            throw AjaxExceptionCodes.INVALID_REQUEST_BODY.create(JSONObject.class.getName(), JSONArray.class.getName());
        }

        JSONObject jUseCountIncrement = jBody.toObject();

        Type type = getTypeFrom(jUseCountIncrement);
        switch (type) {
            case GROUP:
                incrementGroupUseCount(jUseCountIncrement, session);
                break;
            case RESOURCE:
                incrementResourceUseCount(jUseCountIncrement, session);
                break;
            case USER:
                incrementUserUseCount(jUseCountIncrement, session);
                break;
            case CONTACT:
                incrementContactUseCount(jUseCountIncrement, session);
                break;
            default:
                throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create("type", type);
        }

        return new AJAXRequestResult(statusSuccess, "json");
    }

    private static final ContactField[] CONTACT_FIELDS_ID = new ContactField[] { ContactField.OBJECT_ID };

    private void incrementContactUseCount(JSONObject jUseCountIncrement, ServerSession session) throws OXException {
        String mail = jUseCountIncrement.optString("mail", null);
        if (Strings.isNotEmpty(mail)) {
            // By E-Mail address
            ContactCollectorService collectorService = getContactCollectorService();
            if (collectorService == null) {
                throw ServiceExceptionCode.absentService(ContactCollectorService.class);
            }
            try {
                Collection<InternetAddress> addresses = new ArrayList<InternetAddress>(Arrays.<InternetAddress> asList(new QuotedInternetAddress(mail)));
                collectorService.memorizeAddresses(addresses, com.openexchange.contactcollector.Args.builder(session).withIncrementUseCount(true).withAsync(false).build());
            } catch (AddressException e) {
                throw MimeMailException.handleMessagingException(e);
            }
        } else {
            // By object/folder identifier
            ObjectUseCountService objectUseCountService = getObjectUseCountService();

            // Check parameters
            String sId = jUseCountIncrement.optString("id", null);
            if (sId == null) {
                throw AjaxExceptionCodes.MISSING_FIELD.create("id");
            }
            String sFolderId = jUseCountIncrement.optString("folderId", null);
            if (sFolderId == null) {
                throw AjaxExceptionCodes.MISSING_FIELD.create("folderId");
            }

            int accountId = jUseCountIncrement.optInt("accountId", -1);
            int moduleId = jUseCountIncrement.optInt("moduleId", 0);
            if (accountId >= 0 && moduleId > 0) {
                // Check contact existence
                if (ObjectUseCountService.CONTACT_MODULE == moduleId) {
                    checkContactExists(session, new ContactID(sFolderId, sId));
                }

                List<String> folderIdComponents = IDMangler.unmangle(sFolderId);
                if (folderIdComponents.size() > 1) {
                    sFolderId = folderIdComponents.get(folderIdComponents.size() - 1);
                }

                // Increment contact's use-count
                objectUseCountService.incrementUseCount(session, IncrementArguments.builderWithObject(moduleId, accountId, sFolderId, sId).setThrowException(true).build());
            } else {
                int objectId = Strings.parsePositiveInt(sId);
                int folderId = Strings.parsePositiveInt(sFolderId);
                if (objectId >= 0 && folderId >= 0) {
                    // Client provided numeric identifiers --> Internal contact storage
                    // Check contact existence
                    ContactService contactService = services.getOptionalService(ContactService.class);
                    if (contactService != null) {
                        try {
                            contactService.getContact(session, Integer.toString(folderId), Integer.toString(objectId), CONTACT_FIELDS_ID);
                        } catch (OXException e) {
                            if (ContactExceptionCodes.CONTACT_NOT_FOUND.equals(e)) {
                                throw e;
                            }
                        }
                    }

                    // Increment contact's use-count
                    objectUseCountService.incrementUseCount(session, IncrementArguments.builderWithInternalObject(objectId, folderId).setThrowException(true).build());
                } else {
                    // Apparently non-numeric identifiers
                    List<String> folderIdComponents = IDMangler.unmangle(sFolderId);
                    if (folderIdComponents.size() != 3) {
                        // No contact folder identifier
                        throw AjaxExceptionCodes.INVALID_PARAMETER.create("folderId");
                    }
                    if (ContactsAccount.ID_PREFIX.equals(folderIdComponents.get(0)) == false) {
                        // No contact folder identifier
                        throw AjaxExceptionCodes.INVALID_PARAMETER.create("folderId");
                    }

                    accountId = Strings.parsePositiveInt(folderIdComponents.get(1));
                    if (accountId < 0) {
                        // Invalid account identifier in composite folder identifier
                        throw AjaxExceptionCodes.INVALID_PARAMETER.create("folderId");
                    }

                    // Check contact existence
                    checkContactExists(session, new ContactID(sFolderId, sId));

                    // Increment contact's use-count
                    String folder = folderIdComponents.get(2);
                    objectUseCountService.incrementUseCount(session, IncrementArguments.builderWithObject(ObjectUseCountService.CONTACT_MODULE, accountId, folder, sId).setThrowException(true).build());
                }
            }
        }
    }

    private void checkContactExists(ServerSession session, ContactID contactID) throws OXException {
        IDBasedContactsAccess access = services.getServiceSafe(IDBasedContactsAccessFactory.class).createAccess(session);
        try {
            access.set(ContactsParameters.PARAMETER_FIELDS, CONTACT_FIELDS_ID);
            access.getContact(contactID);
        } finally {
            access.finish();
        }
    }

    private void incrementUserUseCount(JSONObject jUseCountIncrement, ServerSession session) throws OXException {
        ObjectUseCountService objectUseCountService = getObjectUseCountService();
        int userId = getIdFrom(jUseCountIncrement);
        if (userId <= 0) {
            throw AjaxExceptionCodes.INVALID_PARAMETER.create("id");
        }

        // Check user existence
        UserService userService = services.getOptionalService(UserService.class);
        if (userService != null && userService.exists(userId, session.getContextId()) == false) {
            throw UserExceptionCode.USER_NOT_FOUND.create(I(userId), I(session.getContextId()));
        }

        // Increment user's use-count
        objectUseCountService.incrementUseCount(session, IncrementArguments.builderWithUserId(userId).setThrowException(true).build());
    }

    private void incrementGroupUseCount(JSONObject jUseCountIncrement, ServerSession session) throws OXException {
        PrincipalUseCountService service = getPrincipalUseCountService();
        int groupId = getIdFrom(jUseCountIncrement);
        if (groupId < 0) { // yepp, less than 0 (zero) since there is virtual group all-groups-and-users with artificial identifier 0 (zero)
            throw AjaxExceptionCodes.INVALID_PARAMETER.create("id");
        }

        // Check group existence
        GroupService groupService = services.getOptionalService(GroupService.class);
        if (groupService != null && groupService.exists(session.getContext(), groupId) == false) {
            throw LdapExceptionCode.GROUP_NOT_FOUND.create(Integer.valueOf(groupId), Integer.valueOf(session.getContextId())).setPrefix("GRP");
        }

        // Increment group's use-count
        service.increment(session, Args.builderForIncrement(groupId).withAsync(false).build());
    }

    private void incrementResourceUseCount(JSONObject jUseCountIncrement, ServerSession session) throws OXException {
        PrincipalUseCountService service = getPrincipalUseCountService();
        int resourceId = getIdFrom(jUseCountIncrement);
        if (resourceId <= 0) {
            throw AjaxExceptionCodes.INVALID_PARAMETER.create("id");
        }

        // Check resource existence
        ResourceService resourceService = services.getOptionalService(ResourceService.class);
        if (resourceService != null) {
            try {
                resourceService.getResource(resourceId, session.getContext());
            } catch (OXException e) {
                if (e.equalsCode(LdapExceptionCode.RESOURCE_NOT_FOUND.getNumber(), "RES")) {
                    throw e;
                }
            }
        }

        // Increment resource's use-count
        service.increment(session, Args.builderForIncrement(resourceId).withAsync(false).build());
    }

}
