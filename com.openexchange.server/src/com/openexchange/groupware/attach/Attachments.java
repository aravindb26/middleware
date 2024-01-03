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

package com.openexchange.groupware.attach;

import java.util.Optional;
import com.openexchange.chronos.provider.CalendarAccount;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.database.provider.DBPoolProvider;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.attach.impl.AttachmentBaseImpl;
import com.openexchange.groupware.attach.impl.OverridableAttachmentAuthorization;
import com.openexchange.groupware.attach.impl.OverridableAttachmentListener;
import com.openexchange.groupware.contact.ContactsAttachment;
import com.openexchange.groupware.tasks.TaskAttachmentListener;
import com.openexchange.groupware.tasks.TaskAuthorization;
import com.openexchange.java.Strings;
import com.openexchange.tools.service.ServicePriorityConflictException;
import com.openexchange.tools.service.SpecificServiceChooser;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public abstract class Attachments {

    private static final TIntObjectMap<SpecificServiceChooser<AttachmentAuthorization>> authz = new TIntObjectHashMap<SpecificServiceChooser<AttachmentAuthorization>>(3);
    private static final TIntObjectMap<SpecificServiceChooser<AttachmentListener>> listener = new TIntObjectHashMap<SpecificServiceChooser<AttachmentListener>>(3);

    private static final AttachmentBaseImpl impl = new AttachmentBaseImpl(new DBPoolProvider());

    static {
        try {
            final SpecificServiceChooser<AttachmentAuthorization> taskAuth = new SpecificServiceChooser<AttachmentAuthorization>();
            taskAuth.registerForEverything(new TaskAuthorization(), 0);

            final SpecificServiceChooser<AttachmentAuthorization> contactAuth = new SpecificServiceChooser<AttachmentAuthorization>();
            contactAuth.registerForEverything(new ContactsAttachment(), 0);

            final SpecificServiceChooser<AttachmentAuthorization> appointmentAuth = new SpecificServiceChooser<AttachmentAuthorization>();

            authz.put(Types.TASK, taskAuth);
            authz.put(Types.CONTACT, contactAuth);
            authz.put(Types.APPOINTMENT, appointmentAuth);


            final SpecificServiceChooser<AttachmentListener> taskListener = new SpecificServiceChooser<AttachmentListener>();
            taskListener.registerForEverything(new TaskAttachmentListener(), 0);

            final SpecificServiceChooser<AttachmentListener> contactListener = new SpecificServiceChooser<AttachmentListener>();
            contactListener.registerForEverything(new ContactsAttachment(), 0);

            final SpecificServiceChooser<AttachmentListener> appointmentListener = new SpecificServiceChooser<AttachmentListener>();

            listener.put(Types.TASK, taskListener);
            listener.put(Types.CONTACT, contactListener);
            listener.put(Types.APPOINTMENT, appointmentListener);

            impl.addAuthorization(new OverridableAttachmentAuthorization(taskAuth), Types.TASK);
            impl.registerAttachmentListener(new OverridableAttachmentListener(taskListener) ,Types.TASK);
            impl.addAuthorization(new OverridableAttachmentAuthorization(contactAuth), Types.CONTACT);
            impl.registerAttachmentListener(new OverridableAttachmentListener(contactListener) ,Types.CONTACT);
            impl.addAuthorization(new OverridableAttachmentAuthorization(appointmentAuth), Types.APPOINTMENT);
            impl.registerAttachmentListener(new OverridableAttachmentListener(appointmentListener),Types.APPOINTMENT);


        } catch (ServicePriorityConflictException e) {
            // Doesn't happen
            e.printStackTrace();
        }
    }

    public static SpecificServiceChooser<AttachmentAuthorization> getAuthorizationChooserForModule(final int module) {
        return authz.get(module);
    }

    public static SpecificServiceChooser<AttachmentListener> getListenerChooserForModule(final int module) {
        return listener.get(module);
    }


    public static AttachmentBase getInstance(){
        return impl;
    }

    public static AttachmentBase getInstance(final DBProvider provider) {
        return new AttachmentBaseImpl(provider);
    }

    public static AttachmentBase getInstance(DBProvider provider, boolean applyDefaults) {
        if (applyDefaults) {
            AttachmentBaseImpl attachmentBase = new AttachmentBaseImpl(impl);
            attachmentBase.setProvider(provider);
            return attachmentBase;
        }
        return new AttachmentBaseImpl(provider);
    }

    /**
     * Adjusts a client-supplied folder identifier where the targeted groupware object is located at to be used within the internal
     * attachment storage services.
     * <p/>
     * This can be used to still allow composite folder identifiers (for contacts- and calendar folders) in the API.
     * 
     * @param requestedFolderId The folder identifier as requested by the client
     * @return The optionally adjusted folder identifier (in <i>unmangled</i> form ) for further usage, or an empty {@link Optional} instance if not applicable
     */
    public static Optional<String> adjustFolderId(String requestedFolderId) {
        if (-1 != Strings.parsePositiveInt(requestedFolderId)) {
            return Optional.empty(); // use numerical identifier as-is
        }
        /*
         * adjust to relative id if folder originates from default contacts account
         */
        try {
            if (ContactsAccount.DEFAULT_ACCOUNT.getAccountId() == com.openexchange.contact.provider.composition.IDMangling.getAccountId(requestedFolderId)) {
                return Optional.of(com.openexchange.contact.provider.composition.IDMangling.getRelativeFolderId(requestedFolderId));
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(Attachments.class).debug("Couldn't extract contact account id from {}", requestedFolderId, e);
        }
        /*
         * adjust to relative id if folder originates from default calendar account
         */
        try {
            if (CalendarAccount.DEFAULT_ACCOUNT.getAccountId() == com.openexchange.chronos.provider.composition.IDMangling.getAccountId(requestedFolderId)) {
                return Optional.of(com.openexchange.chronos.provider.composition.IDMangling.getRelativeFolderId(requestedFolderId));
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(Attachments.class).debug("Couldn't extract calendar account id from {}", requestedFolderId, e);
        }
        return Optional.empty();
    }

}
