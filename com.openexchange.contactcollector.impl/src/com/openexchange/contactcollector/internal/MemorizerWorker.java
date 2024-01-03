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

package com.openexchange.contactcollector.internal;

import static com.openexchange.java.Autoboxing.I;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;
import javax.mail.internet.idn.IDNA;
import com.openexchange.config.ConfigurationService;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contact.provider.composition.IDBasedContactsAccessFactory;
import com.openexchange.contact.provider.composition.IDMangling;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.alias.UserAliasStorage;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.search.Order;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.objectusecount.BatchIncrementArguments;
import com.openexchange.objectusecount.BatchIncrementArguments.Builder;
import com.openexchange.objectusecount.IncrementArguments;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.preferences.ServerUserSetting;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.session.UserAndContext;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.threadpool.behavior.CallerRunsBehavior;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.user.UserService;

/**
 * {@link MemorizerWorker}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MemorizerWorker {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MemorizerWorker.class);

    /*-
     * Member stuff
     */

    final ServiceLookup services;
    final BlockingQueue<MemorizerTask> queue;
    private final AtomicReference<Future<Object>> mainFutureRef;
    final AtomicBoolean flag;
    final ReadWriteLock readWriteLock;

    /**
     * Initializes a new {@link MemorizerWorker}.
     */
    public MemorizerWorker(ServiceLookup services) {
        super();
        this.services = services;
        readWriteLock = new ReentrantReadWriteLock();
        this.flag = new AtomicBoolean(true);
        this.queue = new LinkedBlockingQueue<MemorizerTask>();
        final ThreadPoolService tps = ThreadPools.getThreadPool();
        mainFutureRef = new AtomicReference<Future<Object>>();
        mainFutureRef.set(tps.submit(ThreadPools.task(new MemorizerCallable(), "ContactCollector"), CallerRunsBehavior.<Object> getInstance()));
    }

    /**
     * Closes this worker.
     */
    public void close() {
        final Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            flag.set(false);
            final Future<Object> mainFuture = mainFutureRef.get();
            if (null != mainFuture && mainFutureRef.compareAndSet(mainFuture, null)) {
                mainFuture.cancel(true);
            }
            queue.clear();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Submits specified task.
     *
     * @param memorizerTask The task
     */
    public void submit(final MemorizerTask memorizerTask) {
        final Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            if (!flag.get()) {
                // Shut-down in the meantime
                return;
            }

            Future<Object> f = mainFutureRef.get();
            if (!isDone(f)) {
                // Worker thread is running; offer task
                queue.offer(memorizerTask);
                return;
            }

            /*-
             * Upgrade lock manually
             *
             * Must unlock first to obtain write lock
             */
            readLock.unlock();
            final Lock writeLock = readWriteLock.writeLock();
            writeLock.lock();
            try {
                if (!flag.get()) {
                    // Shut-down in the meantime
                    return;
                }

                f = mainFutureRef.get();
                if (!isDone(f)) {
                    // Worker thread got initialized meanwhile; offer task
                    queue.offer(memorizerTask);
                    return;
                }

                // Grab thread pool service
                ThreadPoolService tps = ThreadPools.getThreadPool();

                // Offer task
                queue.offer(memorizerTask);

                // Start new thread for processing tasks from queue
                f = tps.submit(ThreadPools.task(new MemorizerCallable(), "ContactCollector"), CallerRunsBehavior.<Object> getInstance());
                mainFutureRef.set(f);
            } finally {
                /*
                 * Downgrade lock
                 */
                readLock.lock();
                writeLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
    }

    private static boolean isDone(final Future<Object> f) {
        return ((null == f) || f.isDone());
    }

    private final class MemorizerCallable implements Callable<Object> {

        MemorizerCallable() {
            super();
        }

        private final void waitForTasks(final List<MemorizerTask> tasks) throws InterruptedException {
            waitForTasks(tasks, 10);
        }

        private final void pollForTasks(final List<MemorizerTask> tasks) throws InterruptedException {
            waitForTasks(tasks, 0);
        }

        private final void waitForTasks(final List<MemorizerTask> tasks, final int timeoutSeconds) throws InterruptedException {
            /*
             * Wait for a task to become available
             */
            MemorizerTask task = timeoutSeconds <= 0 ? queue.poll() : queue.poll(timeoutSeconds, TimeUnit.SECONDS);
            if (null == task) {
                return;
            }
            tasks.add(task);
            /*
             * Gather possibly available tasks but don't wait
             */
            while ((task = queue.poll()) != null) {
                tasks.add(task);
            }
        }

        @Override
        public Object call() throws Exception {
            /*
             * Stay active as long as flag is true
             */
            List<MemorizerTask> tasks = new ArrayList<MemorizerTask>();
            Set<UserAndContext> alreadyCleanedUp = new HashSet<>();
            while (flag.get()) {
                /*
                 * Wait for IDs
                 */
                tasks.clear();
                waitForTasks(tasks);
                if (tasks.isEmpty()) {
                    /*
                     * Wait time elapsed and no new tasks were offered
                     */
                    Lock writeLock = readWriteLock.writeLock();
                    writeLock.lock();
                    try {
                        /*
                         * Still no new tasks?
                         */
                        pollForTasks(tasks);
                        if (tasks.isEmpty()) {
                            return null;
                        }
                    } finally {
                        writeLock.unlock();
                    }
                }
                /*
                 * Process tasks
                 */
                alreadyCleanedUp.clear();
                for (MemorizerTask task : tasks) {
                    Session session = task.getSession();
                    if (alreadyCleanedUp.add(UserAndContext.newInstance(session))) {
                        ContactCleanUp.performContactCleanUp(session, services);
                    }
                    handleTask(task, session, true, services);
                }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Handles specified memorizer task.
     *
     * @param memorizerTask The memorizer task
     * @param async Whether this method is executed asynchronously or not
     * @param services The service look-up
     * @throws OXException If exception is supposed to be thrown
     */
    static void handleTask(MemorizerTask memorizerTask, boolean async, ServiceLookup services) throws OXException {
        handleTask(memorizerTask, memorizerTask.getSession(), async, services);
    }

    /**
     * Handles specified memorizer task.
     *
     * @param memorizerTask The memorizer task
     * @param session The session associated with given task
     * @param async Whether this method is executed asynchronously or not
     * @param services The service look-up
     * @throws OXException If exception is supposed to be thrown
     */
    static void handleTask(MemorizerTask memorizerTask, Session session, boolean async, ServiceLookup services) throws OXException  {
        Session sessionToUse = session == null ? memorizerTask.getSession() : session;
        if (!isEnabled(sessionToUse)) {
            return;
        }

        int folderId = getFolderId(sessionToUse);
        if (folderId == 0) {
            return;
        }

        // Get associated context
        boolean throwException = async == false;
        Context ctx;
        try {
            ContextService contextService = services.getOptionalService(ContextService.class);
            if (null == contextService) {
                LOG.warn("Contact collector run aborted: missing context service");
                return;
            }
            ctx = contextService.getContext(sessionToUse.getContextId());
        } catch (OXException e) {
            if (throwException) {
                throw e;
            }
            LOG.error("Contact collector run aborted.", e);
            return;
        }

        // Strip by well-known aliases
        final Set<InternetAddress> addresses;
        {
            Collection<InternetAddress> tmp = memorizerTask.getAddresses();
            addresses = tmp instanceof Set ? (Set<InternetAddress>) tmp : new LinkedHashSet<InternetAddress>(tmp);
        }
        try {
            UserService userService = services.getOptionalService(UserService.class);
            if (null == userService) {
                LOG.warn("Contact collector run aborted: missing user service");
                return;
            }

            UserAliasStorage aliasStorage = services.getOptionalService(UserAliasStorage.class);
            Set<InternetAddress> aliases = AliasesProvider.getInstance().getAliases(userService.getUser(sessionToUse.getUserId(), ctx), ctx, aliasStorage);
            addresses.removeAll(aliases);
        } catch (OXException e) {
            if (throwException) {
                throw e;
            }
            LOG.error("Contact collector run aborted.", e);
            return;
        }

        if (addresses.isEmpty()) {
            return;
        }

        /*
         * memorize contacts and increment use counts as needed
         */
        List<IncrementArguments> incrementArgs = memorizeContacts(services, sessionToUse, addresses, Integer.toString(folderId), memorizerTask.isIncrementUseCount(), async);
        if (incrementArgs != null && incrementArgs.isEmpty() == false) {
            for (IncrementArguments incrementArguments : incrementArgs) {
                try {
                    incrementUseCounts(services, sessionToUse, incrementArguments);
                } catch (OXException e) {
                    if (throwException) {
                        throw e;
                    }
                    if (incrementArguments instanceof BatchIncrementArguments) {
                        LOG.warn("Failed to batch increment use count for user {} in context {}", I(sessionToUse.getUserId()), I(sessionToUse.getContextId()), e);
                    } else {
                        LOG.warn("Failed to increment use count for user {} in context {}", I(sessionToUse.getUserId()), I(sessionToUse.getContextId()), e);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private static final ContactField[] FIELDS = { ContactField.OBJECT_ID, ContactField.FOLDER_ID, ContactField.LAST_MODIFIED };

	private static final ContactField[] SEARCH_FIELDS = { ContactField.EMAIL1, ContactField.EMAIL2, ContactField.EMAIL3 };

    /**
     * Creates contacts in the user's contact collect folder for the supplied set of mail addresses unless such contacts already exist in
     * this or another folder the user has access to. Additionally prepares use count increment arguments for the newly created or already
     * existing contacts for subsequent usage.
     *
     * @param services A service lookup reference
     * @param session The user's session
     * @param addresses The addresses to memorize
     * @param contactCollectFolderId The identifier of the user's contact collect folder, or <code>null</code> if there is none
     * @param prepareUseCountArgs Whether to prepare use-count arguments
     * @param async Whether use-count is meant to be incremented asynchronously or not
     * @return The prepared increment arguments targeting the matching contacts or empty list in case <code>prepareUseCountArgs</code> has been set to <code>false</code>
     * @throws OXException If non-async execution and an error occurs
     */
    private static List<IncrementArguments> memorizeContacts(ServiceLookup services, Session session, Collection<InternetAddress> addresses, String contactCollectFolderId, boolean prepareUseCountArgs, boolean async) throws OXException {
        if (async) {
            BatchIncrementArgumentsCollector collector = BatchIncrementArgumentsCollector.instanceFor(prepareUseCountArgs);
            for (InternetAddress address : addresses) {
                try {
                    memorizeContact(services, session, contactCollectFolderId, address, collector);
                } catch (Exception e) {
                    LOG.warn("Error memorizing {}, skipping.", address, e);
                }
            }
            return collector.getCollectedArguments();
        }

        ListIncrementArgumentsCollector collector = ListIncrementArgumentsCollector.instanceFor(prepareUseCountArgs ? addresses.size() : 0);
        for (InternetAddress address : addresses) {
            try {
                memorizeContact(services, session, contactCollectFolderId, address, collector);
            } catch (ParseException e) {
                throw MimeMailException.handleMessagingException(e);
            } catch (UnsupportedEncodingException e) {
                throw OXException.general("Unsupported encoding", e);
            }
        }
        return collector.getCollectedArguments();
    }

    /**
     * Creates a contact in the user's contact collect folder for the supplied mail address unless such a contact already exists in
     * this or another folder the user has access to. Additionally adds use count increment arguments for the newly created or already
     * existing contacts for subsequent usage to the given builder instance.
     *
     * @param services A service lookup reference
     * @param session The user's session
     * @param addresses The addresses to memorize
     * @param contactCollectFolderId The identifier of the user's contact collect folder, or <code>null</code> if there is none
     * @param collector The use count increment arguments collector to use
     */
    private static void memorizeContact(ServiceLookup services, Session session, String contactCollectFolderId, InternetAddress address, IncrementArgumentsCollector collector) throws ParseException, UnsupportedEncodingException, OXException {
        /*
         * lookup existing contacts with this mail address
         */
        Contact memorizedContact = transformInternetAddress(address, session);
        List<Contact> existingContacts = searchContacts(services, session, memorizedContact.getEmail1(), collector.notCollect());
        if (existingContacts.isEmpty() && null != contactCollectFolderId) {
            /*
             * create in contact collect folder & add corresponding increment argument
             */
            memorizedContact = createContact(services, session, contactCollectFolderId, memorizedContact);
            if (collector.collect()) {
                collector.add(memorizedContact.getObjectID(), memorizedContact.getParentFolderID());
            }
        } else {
            /*
             * prepare increment arguments for matching existing contacts, suitable for the underlying account
             */
            if (collector.collect()) {
                for (Contact existingContact : existingContacts) {
                    String folderId = existingContact.getFolderId();
                    int accountId = IDMangling.getAccountId(folderId);
                    if (ObjectUseCountService.DEFAULT_ACCOUNT == accountId) {
                        collector.add(existingContact.getObjectID(), existingContact.getParentFolderID());
                    } else {
                        collector.add(accountId, IDMangling.getRelativeFolderId(folderId), existingContact.getId());
                    }
                }
            }
        }
    }

    /**
     * Increments the use counts using the supplied batch increment arguments.
     *
     * @param services A service lookup reference
     * @param session The user's session
     * @param incrementArguments The increment arguments to pass to the use count service
     * @throws OXException If use-count incrementation fails
     */
    private static void incrementUseCounts(ServiceLookup services, Session session, IncrementArguments incrementArguments) throws OXException {
        services.getServiceSafe(ObjectUseCountService.class).incrementUseCount(session, incrementArguments);
    }

    private static Contact createContact(ServiceLookup services, Session session, String folderId, Contact contact) throws OXException {
        IDBasedContactsAccess contactsAccess = services.getServiceSafe(IDBasedContactsAccessFactory.class).createInternalAccess(session);
        try {
            contactsAccess.createContact(folderId, contact);
            return contact;
        } finally {
            contactsAccess.finish();
        }
    }

    private static List<Contact> searchContacts(ServiceLookup services, Session session, String email, boolean onlyExistenceNeeded) throws OXException {
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.OR);
        for (ContactField field : SEARCH_FIELDS) {
            SingleSearchTerm term = new SingleSearchTerm(SingleOperation.EQUALS);
            term.addOperand(new ContactFieldOperand(field));
            term.addOperand(new ConstantOperand<String>(email));
            searchTerm.addSearchTerm(term);
        }
        IDBasedContactsAccess contactsAccess = services.getServiceSafe(IDBasedContactsAccessFactory.class).createAccess(session);
        try {
            contactsAccess.set(ContactsParameters.PARAMETER_FIELDS, FIELDS);
            contactsAccess.set(ContactsParameters.PARAMETER_ORDER_BY, ContactField.USE_COUNT);
            contactsAccess.set(ContactsParameters.PARAMETER_ORDER, Order.DESCENDING);
            contactsAccess.set(ContactsParameters.PARAMETER_RIGHT_HAND_LIMIT, I(onlyExistenceNeeded ? 1 : getSearchLimit(services)));
            return contactsAccess.searchContacts(null, searchTerm);
        } finally {
            contactsAccess.finish();
        }
    }

    private static volatile Integer searchLimit;
    static int getSearchLimit(ServiceLookup services) {
        Integer tmp = searchLimit;
        if (null == tmp) {
            synchronized (MemorizerWorker.class) {
                tmp = searchLimit;
                if (null == tmp) {
                    int defaultLimit = 5;
                    ConfigurationService configurationService = services.getOptionalService(ConfigurationService.class);
                    if (null == configurationService) {
                        return defaultLimit;
                    }
                    tmp = Integer.valueOf(configurationService.getIntProperty("com.openexchange.contactcollector.searchLimit", defaultLimit));
                    searchLimit = tmp;
                }
            }
        }
        return tmp.intValue();
    }

    static boolean isEnabled(final Session session) {
        try {
            return ServerSessionAdapter.valueOf(session).getUserPermissionBits().isCollectEmailAddresses();
        } catch (OXException e) {
            LOG.error("", e);
        }
        return false;
    }

    static int getFolderId(final Session session) {
        try {
            final Integer folder = ServerUserSetting.getInstance().getContactCollectionFolder(session.getContextId(), session.getUserId());
            return null == folder ? 0 : folder.intValue();
        } catch (OXException e) {
            LOG.error("", e);
            return 0;
        }
    }

    private static Contact transformInternetAddress(final InternetAddress address, final Session session) throws ParseException, UnsupportedEncodingException {
        Contact retval = new Contact();
        retval.setParentFolderID(getFolderId(session));
        final String addr = decodeMultiEncodedValue(IDNA.toIDN(address.getAddress()));
        retval.setEmail1(addr);
        if (Strings.isNotEmpty(address.getPersonal())) {
            String displayName = decodeMultiEncodedValue(address.getPersonal());
            retval.setDisplayName(displayName);
        } else {
            retval.setDisplayName(addr);
        }
        return retval;
    }

    private static final Pattern ENC_PATTERN = Pattern.compile("(=\\?\\S+?\\?\\S+?\\?)(.+?)(\\?=)");

    /**
     * Decodes a multi-mime-encoded value using the algorithm specified in RFC 2047, Section 6.1.
     * <p>
     * If the charset-conversion fails for any sequence, an {@link UnsupportedEncodingException} is thrown.
     * <p>
     * If the String is not a RFC 2047 style encoded value, it is returned as-is
     *
     * @param value The possibly encoded value
     * @return The possibly decoded value
     * @throws UnsupportedEncodingException If an unsupported charset encoding occurs
     * @throws ParseException If encoded value cannot be decoded
     */
    private static String decodeMultiEncodedValue(final String value) throws ParseException, UnsupportedEncodingException {
        if (value == null) {
            return null;
        }
        final String val = MimeUtility.unfold(value);
        final Matcher m = ENC_PATTERN.matcher(val);
        if (m.find()) {
            final StringBuilder sa = new StringBuilder(val.length());
            int lastMatch = 0;
            do {
                sa.append(val.substring(lastMatch, m.start()));
                sa.append(com.openexchange.java.Strings.quoteReplacement(MimeUtility.decodeWord(m.group())));
                lastMatch = m.end();
            } while (m.find());
            sa.append(val.substring(lastMatch));
            return sa.toString();
        }
        return val;
    }

    // ----------------------------------------------- IncrementArguments Collector --------------------------------------------------------

    /** Collects increment arguments. */
    private static interface IncrementArgumentsCollector {

        /**
         * Adds increment arguments for given numeric object and folder identifier.
         *
         * @param objectId The object identifier
         * @param folderId The folder identifier
         */
        void add(int objectId, int folderId);

        /**
         * Adds increment arguments for given account, folder and folder object.
         *
         * @param accountId The account identifier
         * @param folderId The folder identifier
         * @param objectId The object identifier
         */
        void add(int accountId, String folderId, String objectId);

        /**
         * Checks if this collector does collect at all.
         *
         * @return <code>true</code> if collecting; otherwise <code>false</code>
         */
        boolean collect();

        /**
         * Checks if this collector does <b>not</b> collect at all.
         *
         * @return <code>true</code> if not collecting; otherwise <code>false</code>
         */
        default boolean notCollect() {
            return collect() == false;
        }

        /**
         * Gets the collected increment arguments.
         *
         * @return The collected increment arguments or empty list if this collector does not collect
         */
        List<IncrementArguments> getCollectedArguments();
    }

    private static class BatchIncrementArgumentsCollector implements IncrementArgumentsCollector {

        private static final BatchIncrementArgumentsCollector NOOP = new BatchIncrementArgumentsCollector(false);

        static BatchIncrementArgumentsCollector instanceFor(boolean collect) {
            return collect ? new BatchIncrementArgumentsCollector(true) : NOOP;
        }

        private final boolean collect;
        private final Builder incrementArgsBuilder;

        private BatchIncrementArgumentsCollector(boolean collect) {
            super();
            this.collect = collect;
            this.incrementArgsBuilder = collect ? new BatchIncrementArguments.Builder() : null;
        }

        @Override
        public void add(int objectId, int folderId) {
            if (collect) {
                incrementArgsBuilder.add(objectId, folderId);
            }
        }

        @Override
        public void add(int accountId, String folderId, String objectId) {
            if (collect) {
                incrementArgsBuilder.add(accountId, ObjectUseCountService.CONTACT_MODULE, folderId, objectId);
            }
        }

        @Override
        public boolean collect() {
            return collect;
        }

        @Override
        public List<IncrementArguments> getCollectedArguments() {
            return collect ? Collections.singletonList(incrementArgsBuilder.build()) : Collections.emptyList();
        }
    }

    private static class ListIncrementArgumentsCollector implements IncrementArgumentsCollector {

        private static final ListIncrementArgumentsCollector NOOP = new ListIncrementArgumentsCollector(0);

        static ListIncrementArgumentsCollector instanceFor(int size) {
            return size > 0 ? new ListIncrementArgumentsCollector(size) : NOOP;
        }

        private final boolean collect;
        private final List<IncrementArguments> argumentsList;

        private ListIncrementArgumentsCollector(int size) {
            super();
            this.collect = size > 0;
            this.argumentsList = collect ? new ArrayList<IncrementArguments>(size) : null;
        }

        @Override
        public void add(int objectId, int folderId) {
            if (collect) {
                argumentsList.add(IncrementArguments.builderWithInternalObject(objectId, folderId).setThrowException(true).build());
            }
        }

        @Override
        public void add(int accountId, String folderId, String objectId) {
            if (collect) {
                argumentsList.add(IncrementArguments.builderWithObject(ObjectUseCountService.CONTACT_MODULE, accountId, folderId, objectId).setThrowException(true).build());
            }
        }

        @Override
        public boolean collect() {
            return collect;
        }

        @Override
        public List<IncrementArguments> getCollectedArguments() {
            return collect ? argumentsList : Collections.emptyList();
        }
    }

}
