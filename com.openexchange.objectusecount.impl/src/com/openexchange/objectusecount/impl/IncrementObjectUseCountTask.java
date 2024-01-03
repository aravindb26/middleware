package com.openexchange.objectusecount.impl;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contact.provider.composition.IDBasedContactsAccessFactory;
import com.openexchange.contact.provider.composition.IDMangling;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.search.ContactsSearchObject;
import com.openexchange.java.Strings;
import com.openexchange.objectusecount.BatchIncrementArguments;
import com.openexchange.objectusecount.IncrementArguments;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.session.Session;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.user.User;
import com.openexchange.user.UserService;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * {@link IncrementObjectUseCountTask} - The task to execute in order to increment use-counts according to specified {@link IncrementArguments arguments}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
class IncrementObjectUseCountTask extends AbstractTask<Void> {

    private final static Logger LOG = LoggerFactory.getLogger(IncrementArguments.class);

    private final IncrementArguments arguments;
    private final Session session;
    private final ObjectUseCountServiceImpl serviceImpl;

    /**
     * Initializes a new {@link IncrementObjectUseCountTask}.
     */
    IncrementObjectUseCountTask(IncrementArguments arguments, Session session, ObjectUseCountServiceImpl serviceImpl) {
        super();
        this.arguments = arguments;
        this.session = session;
        this.serviceImpl = serviceImpl;
    }

    @Override
    public Void call() throws OXException {
        int userId = arguments.getUserId();
        if (userId > 0) {
            // By user identifier
            UserService userService = serviceImpl.services.getService(UserService.class);
            if (null == userService) {
                throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(UserService.class);
            }
            User user = userService.getUser(userId, session.getContextId());
            TIntIntMap object2folder = new TIntIntHashMap(2);
            object2folder.put(user.getContactId(), FolderObject.SYSTEM_LDAP_FOLDER_ID);
            serviceImpl.legacyService.incrementUseCount(object2folder, session.getUserId(), session.getContextId(), arguments.getCon());
        }

        int accountId = arguments.getAccountId();
        if (ObjectUseCountService.DEFAULT_ACCOUNT != accountId) {
            // use generic use count
            int module = arguments.getModuleId();
            String folder = arguments.getFolder();
            String object = arguments.getObject();
            if (0 < module && Strings.isNotEmpty(folder, object)) {
                serviceImpl.genericService.incrementUseCount(session, module, accountId, folder, object);
            } else {
                LOG.debug("Could not increment use count for account {}, mandatory values are not set. module: {}, folder: {}, object: {}",
                    I(accountId), I(module), null == folder ? "null" : folder, null == object ? "null" : object);
            }
        }

        Collection<String> mailAddresses = arguments.getMailAddresses();
        if (null != mailAddresses && !mailAddresses.isEmpty()) {
            // By mail address(es)
            IDBasedContactsAccessFactory contactsAccessFactory = serviceImpl.services.getService(IDBasedContactsAccessFactory.class);
            if (null == contactsAccessFactory) {
                throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(IDBasedContactsAccessFactory.class);
            }

            TIntIntMap object2folder = new TIntIntHashMap(mailAddresses.size());
            IDBasedContactsAccess contactsAccess = contactsAccessFactory.createAccess(session);
            try {
                contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, new ContactField[] { ContactField.FOLDER_ID, ContactField.OBJECT_ID });
                for (String mail : mailAddresses) {
                    ContactsSearchObject search = new ContactsSearchObject();
                    search.setAllEmail(mail);
                    search.setOrSearch(true);
                    List<Contact> it = contactsAccess.searchContacts(search);
                    for (Contact c : it) {
                        String folder = c.getFolderId();
                        int contactAccountId = IDMangling.getAccountId(folder);
                        if (ObjectUseCountService.DEFAULT_ACCOUNT == contactAccountId) {
                            object2folder.put(c.getObjectID(), c.getParentFolderID());
                        } else {
                            String folderId = IDMangling.getRelativeFolderId(folder);
                            String objectId = c.getId();
                            serviceImpl.genericService.incrementUseCount(session, ObjectUseCountService.CONTACT_MODULE, contactAccountId, folderId, objectId);
                        }
                    }
                }
            } finally {
                contactsAccess.finish();
            }
            serviceImpl.legacyService.incrementUseCount(object2folder, session.getUserId(), session.getContextId(), arguments.getCon());
        }

        if (arguments instanceof BatchIncrementArguments) {
            BatchIncrementArguments batchArguments = (BatchIncrementArguments) arguments;
            serviceImpl.batchIncrementObjectUseCount(batchArguments.getCounts(), batchArguments.getGenericCounts(), session.getUserId(), session.getContextId(), arguments.getCon());
        } else {
            int objectId = arguments.getObjectId();
            int folderId = arguments.getFolderId();
            if (objectId > 0 && folderId > 0) {
                // By object/folder identifier
                TIntIntMap object2folder = new TIntIntHashMap(2);
                object2folder.put(objectId, folderId);
                serviceImpl.legacyService.incrementUseCount(object2folder, session.getUserId(), session.getContextId(), arguments.getCon());
            }
        }

        return null;
    }
}