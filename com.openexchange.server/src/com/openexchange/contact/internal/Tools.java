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

package com.openexchange.contact.internal;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.SortOptions;
import com.openexchange.contact.SortOrder;
import com.openexchange.contact.internal.mapping.ContactMapper;
import com.openexchange.contact.storage.ContactStorage;
import com.openexchange.contact.storage.registry.ContactStorageRegistry;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.folder.FolderService;
import com.openexchange.group.GroupStorage;
import com.openexchange.groupware.contact.ContactConfig;
import com.openexchange.groupware.contact.ContactConfig.Property;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.search.ContactSearchObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.java.Collators;
import com.openexchange.java.Strings;
import com.openexchange.l10n.SuperCollator;
import com.openexchange.preferences.ServerUserSetting;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.oxfolder.OXSubscribeAwareFolderSQL;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.userconf.UserConfigurationService;
import com.openexchange.userconf.UserPermissionService;

/**
 * {@link Tools} - Static utility functions for the contact service.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public final class Tools {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Tools.class);

    private static final FolderObject GUEST_CONTACTS = new FolderObject();
    static {
        GUEST_CONTACTS.setObjectID(FolderObject.VIRTUAL_GUEST_CONTACT_FOLDER_ID);
        GUEST_CONTACTS.setParentFolderID(FolderObject.SYSTEM_FOLDER_ID);
        GUEST_CONTACTS.setFolderName("Guest contacts");
        GUEST_CONTACTS.setModule(FolderObject.CONTACT);
        GUEST_CONTACTS.setType(FolderObject.SYSTEM_TYPE);
        GUEST_CONTACTS.setCreationDate(new Date());
        GUEST_CONTACTS.setCreatedBy(2);
        GUEST_CONTACTS.setLastModified(new Date());
        GUEST_CONTACTS.setModifiedBy(2);
        GUEST_CONTACTS.setPermissionFlag(FolderObject.PUBLIC_PERMISSION);
        GUEST_CONTACTS.setSubfolderFlag(false);
        GUEST_CONTACTS.setSubfolderIds(new ArrayList<Integer>(0));
        GUEST_CONTACTS.setDefaultFolder(false);

        OCLPermission allUsersPermission = new OCLPermission(GroupStorage.GROUP_ZERO_IDENTIFIER, FolderObject.VIRTUAL_GUEST_CONTACT_FOLDER_ID);
        allUsersPermission.setAllPermission(OCLPermission.READ_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
        allUsersPermission.setGroupPermission(true);

        OCLPermission allGuestsPermission = new OCLPermission(GroupStorage.GUEST_GROUP_IDENTIFIER, FolderObject.VIRTUAL_GUEST_CONTACT_FOLDER_ID);
        allGuestsPermission.setAllPermission(OCLPermission.READ_FOLDER, OCLPermission.READ_OWN_OBJECTS, OCLPermission.WRITE_OWN_OBJECTS, OCLPermission.NO_PERMISSIONS);
        allGuestsPermission.setGroupPermission(true);

        List<OCLPermission> permissions = new ArrayList<>(2);
        permissions.add(allUsersPermission);
        permissions.add(allGuestsPermission);

        GUEST_CONTACTS.setPermissions(permissions);
    }

	/**
	 * Gets a comparator for contacts based on the supplied sort options.
	 *
	 * @param sortOptions the sort options
	 * @return the comparator
	 */
	public static Comparator<Contact> getComparator(final SortOptions sortOptions) {
	    if (null == sortOptions || SortOptions.EMPTY.equals(sortOptions) ||
	        null == sortOptions.getOrder() || 0 == sortOptions.getOrder().length) {
	        /*
	         * nothing to sort
	         */
	        return new Comparator<Contact>() {
	            @Override
	            public int compare(Contact o1, Contact o2) {
	                return 0;
	            }
            };
	    }
        /*
         * sort using the mapping's comparator with collation
         */
	    Comparator<Object> comp = null;
	    if (null != sortOptions.getCollation()) {
	        SuperCollator superCollator = SuperCollator.get(sortOptions.getCollation());
	        if (superCollator != null) {
	            comp = Collators.getDefaultInstance(superCollator.getJavaLocale());
	        } else {
	            LOG.warn("The collation {} is unknown. Falling back to default collation.", sortOptions.getCollation());
	        }
	    }
        final Comparator<Object> collationComparator = comp;
        return new Comparator<Contact>() {
            @Override
            public int compare(Contact o1, Contact o2) {
                for (SortOrder order : sortOptions.getOrder()) {
                    int comparison = 0;
                    try {
                        comparison = ContactMapper.getInstance().get(order.getBy()).compare(o1, o2, collationComparator);
                    } catch (OXException e) {
                        LOG.error("error comparing objects", e);
                    }
                    if (0 != comparison) {
                        return Order.DESCENDING.equals(order.getOrder()) ? -1 * comparison : comparison;
                    }
                }
                return 0;
            }
        };
	}

	/**
	 * Gets the contact storage responsible for a folder identified by the supplied ID, throwing an exception if there is none.
	 *
	 * @param session The session
	 * @param folderID The folder ID
	 * @return The contact storage
	 * @throws OXException
	 */
	public static ContactStorage getStorage(Session session, String folderID) throws OXException {
		ContactStorage storage = ContactServiceLookup.getService(ContactStorageRegistry.class, true).getStorage(session, folderID);
        if (null == storage) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create("'Contact storage for folder " + folderID + "'");
        }
        return storage;
	}

	public static List<ContactStorage> getStorages() throws OXException {
	    return ContactServiceLookup.getService(ContactStorageRegistry.class, true).getStorages(null);
	}

	/**
	 * Gets all contact storages.
	 *
     * @param session the session
	 * @return the contact storages
	 * @throws OXException
	 */
	public static List<ContactStorage> getStorages(Session session) throws OXException {
		return ContactServiceLookup.getService(ContactStorageRegistry.class, true).getStorages(session);
	}

	/**
	 * Gets the contact storages for the supplied folders, each storage mapped
	 * to a list of folder IDs the respective storage is responsible for.
	 *
     * @param session the session
	 * @param folderIDs the folder IDs to get the storages for
	 * @return the contact storages
	 * @throws OXException
	 */
	public static Map<ContactStorage, List<String>> getStorages(Session session, Collection<String> folderIDs) throws OXException {
		Map<ContactStorage, List<String>> storages = new HashMap<ContactStorage, List<String>>();
		for (String folderID : folderIDs) {
			ContactStorage storage = getStorage(session, folderID);
			List<String> fids = storages.get(storage);
			if (fids == null) {
                fids = new ArrayList<String>();
                storages.put(storage, fids);
            }
			fids.add(folderID);
		}
		return storages;
	}

    /**
     * Takes over and verifies the email addresses from referenced contacts into the supplied distribution list members if applicable.
     *
     * @param session The user's session
     * @param distributionList The distribution list members to fill
     */
    public static void fillDistributionListMembers(Session session, DistributionListEntryObject[] distributionList) throws OXException {
        if (null != distributionList) {
            Set<String> alreadyChecked = distributionList.length > 1 ? new HashSet<>() : null;
            for (DistributionListEntryObject member : distributionList) {
                fillDistributionListMembers(session, member, alreadyChecked);
            }
        }
    }

    /**
     * Takes over and verifies the email address from the referenced contact into the supplied distribution list member if applicable.
     *
     * @param session The user's session
     * @param member The distribution list member to fill
     * @param alreadyChecked The optional set to remember already checked folders
     */
    private static void fillDistributionListMembers(Session session, DistributionListEntryObject member, Set<String> alreadyChecked) throws OXException {
        if (DistributionListEntryObject.INDEPENDENT == member.getEmailfield()) {
            return;
        }
        String referencedContactId = member.getEntryID();
        if (Strings.isEmpty(referencedContactId)) {
            throw ContactExceptionCodes.OBJECT_ID_MANDATORY_FOR_REFERENCED_MEMBERS.create();
        }
        /*
         * get referenced contact
         */
        Contact referencedContact;
        SearchIterator<Contact> searchIterator = null;
        try {
            searchIterator = getStorage(session, member.getFolderID()).list(session, member.getFolderID(), new String[] { referencedContactId },
                new ContactField[] { ContactField.EMAIL1, ContactField.EMAIL2, ContactField.EMAIL3, ContactField.FOLDER_ID });
            if (false == searchIterator.hasNext()) {
                throw ContactExceptionCodes.CONTACT_NOT_FOUND.create(I(Strings.parsePositiveInt(referencedContactId)), I(session.getContextId()));
            }
            referencedContact = searchIterator.next();
        } finally {
            close(searchIterator);
        }
        /*
         * check accessibility of given distribution list member
         */
        String folderIdToCheck = referencedContact.getFolderId() == null ? member.getFolderID() : referencedContact.getFolderId();
        if (folderIdToCheck != null && (alreadyChecked == null || alreadyChecked.add(folderIdToCheck))) {
            FolderObject memberSourceFolder = getFolder(session.getContextId(), folderIdToCheck);
            EffectivePermission permission = Tools.getPermission(session, memberSourceFolder);
            /*
             * advertise "Contact not found" to not exploit existence of such a contact in case access if prohibited
             */
            if (!permission.isFolderVisible()) {
                throw ContactExceptionCodes.CONTACT_NOT_FOUND.create(I(Strings.parsePositiveInt(referencedContactId)), I(session.getContextId()));
            }
            if (permission.getReadPermission() <= OCLPermission.NO_PERMISSIONS) {
                throw ContactExceptionCodes.CONTACT_NOT_FOUND.create(I(Strings.parsePositiveInt(referencedContactId)), I(session.getContextId()));
            }
            if (!permission.canReadAllObjects()) {
                if (permission.canReadOwnObjects() && memberSourceFolder.getCreatedBy() != session.getUserId()) {
                    throw ContactExceptionCodes.CONTACT_NOT_FOUND.create(I(Strings.parsePositiveInt(referencedContactId)), I(session.getContextId()));
                }
            }
        }
        /*
         * take over targeted email address in distribution list member & verify the address implicitly
         */
        String emailAddress = DistributionListEntryObject.EMAILFIELD1 == member.getEmailfield() ? referencedContact.getEmail1() :
            DistributionListEntryObject.EMAILFIELD2 == member.getEmailfield() ? referencedContact.getEmail2() : referencedContact.getEmail3();
        member.setEmailaddress(emailAddress, true);
    }

    /**
     * Gets a context.
     *
     * @param contextID the context ID
     * @return the context
     * @throws OXException
     */
    public static Context getContext(int contextID) throws OXException {
        final Context context = ContactServiceLookup.getService(ContextService.class, true).getContext(contextID);
        if (null == context) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create("Unable to get context '" + contextID + "'.");
        }
        return context;
    }

    /**
     * Gets the user configuration.
     *
     * @param session
     * @return
     * @throws OXException
     */
    public static UserConfiguration getUserConfig(Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getUserConfiguration();
        }
        return ContactServiceLookup.getService(UserConfigurationService.class, true).getUserConfiguration(
            session.getUserId(), getContext(session));
    }

    /**
     * Gets the user permission bits.
     *
     * @param session
     * @return
     * @throws OXException
     */
    public static UserPermissionBits getUserPermissionBits(Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getUserPermissionBits();
        }
        return getUserPermissionBits(session.getUserId(), session.getContextId());
    }

    /**
     * Gets the user permission bits.
     *
     * @param userID The user ID
     * @param contextID The context ID
     * @return
     * @throws OXException
     */
    private static UserPermissionBits getUserPermissionBits(int userID, int contextID) throws OXException {
        return ContactServiceLookup.getService(UserPermissionService.class, true).getUserPermissionBits(
            userID, getContext(contextID));
    }

    /**
     * Gets a context.
     *
     * @param session the session
     * @return the context
     * @throws OXException
     */
    public static Context getContext(Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getContext();
        }
        return getContext(session.getContextId());
    }

	/**
	 * Gets a folder.
	 *
     * @param contextID the context ID
     * @param folderId the folder ID
	 * @return
	 * @throws OXException
	 */
    public static FolderObject getFolder(int contextID, String folderId) throws OXException {
        if (Integer.toString(FolderObject.VIRTUAL_GUEST_CONTACT_FOLDER_ID).equals(folderId)) {
            return GUEST_CONTACTS;
        }

        FolderService folderService = ContactServiceLookup.getService(FolderService.class, true);
        return folderService.getFolderObject(parse(folderId), contextID);
    }

    /**
     * Gets the permissions of a folder.
     *
     * @param session
     * @param folder
     * @return
     * @throws OXException
     */
    public static EffectivePermission getPermission(Session session, FolderObject folder) throws OXException {
        try {
            return folder.getEffectiveUserPermission(session.getUserId(), getUserPermissionBits(session));
        } catch (RuntimeException e) {
            throw ContactExceptionCodes.UNEXPECTED_ERROR.create(e);
        }
    }

    /**
     * Gets the permissions of a folder.
     *
     * @param contextID the context ID
     * @param folderId the folder ID
     * @param userID the user ID
     * @return
     * @throws OXException
     */
    public static EffectivePermission getPermission(int contextID, String folderId, int userID) throws OXException {
        if (Integer.toString(FolderObject.VIRTUAL_GUEST_CONTACT_FOLDER_ID).equals(folderId)) {
            return GUEST_CONTACTS.getEffectiveUserPermission(userID, getUserPermissionBits(userID, contextID));
        }
        return ContactServiceLookup.getService(FolderService.class, true).getFolderPermission(parse(folderId), userID, contextID);
    }

    /**
	 * Checks whether the supplied string is empty, that is it is either
	 * <code>null</code>, or consists of whitespace characters exclusively.
	 *
	 * @param string the string to check
	 * @return <code>true</code>, if the string is 'empty', <code>false</code>,
	 * otherwise
	 */
	public static boolean isEmpty(final String string) {
        if (null != string) {
		    final int length = string.length();
		    for (int i = 0; i < length; i++) {
		    	if (false == Strings.isWhitespace(string.charAt(i))) {
		    		return false;
		    	}
		    }
        }
        return true;
    }

	/**
	 * Extracts all folder IDs that are present in the supplied search term.
	 *
	 * @param term the search term to analyze
	 * @return the folder IDs, or an empty list if none were found
	 */
	public static List<String> extractFolderIDs(final SearchTerm<?> term) {
		final List<String> folders = new SearchTermAnalyzer(term).getFolderIDs();
		return null != folders ? folders : new ArrayList<String>();
	}

	/**
	 * Constructs a search to search for specific folder IDs.
	 *
	 * @param folderIDs the folder IDs
	 * @return the search term
	 */
	public static SearchTerm<?> getFoldersTerm(final List<String> folderIDs) {
		if (null == folderIDs || 0 == folderIDs.size()) {
			return null;
		} else if (1 == folderIDs.size()) {
			return createContactFieldTerm(ContactField.FOLDER_ID, SingleOperation.EQUALS, folderIDs.get(0));
		} else {
    		final CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
			for (final String folderID : folderIDs) {
				orTerm.addSearchTerm(createContactFieldTerm(ContactField.FOLDER_ID, SingleOperation.EQUALS, folderID));
			}
			return orTerm;
		}
	}

	/**
	 * Creates a new {@link SingleSearchTerm} using the supplied operation, the contact field as 'column'-, and the constant as
	 * 'constant'-type operators.
	 *
	 * @param field The contact field for the 'column' operator
	 * @param operation The operation
	 * @param constant The 'constant' operator
	 * @return The search term
	 */
    public static <T> SingleSearchTerm createContactFieldTerm(ContactField field, SingleOperation operation, T constant) {
        SingleSearchTerm term = new SingleSearchTerm(operation);
        term.addOperand(new ContactFieldOperand(field));
        term.addOperand(new ConstantOperand<T>(constant));
        return term;
    }

	/**
	 * Gets the contact folders that are used for search by default when no
	 * other folders are specified by the search term. This may either be a
	 * set of default folders for the user, or all contact folders visible to
	 * the user.
	 *
	 * @param contextID The context ID
	 * @param userID The user ID
	 * @param emailAutoComplete <code>true</code> if the search is an e-mail auto-complete-search, <code>false</code>, otherwise.
	 * @return The search folders
	 * @throws OXException
	 */
    public static List<String> getSearchFolders(int contextID, int userID, boolean emailAutoComplete) throws OXException {
        if (emailAutoComplete && false == ContactConfig.getInstance().getBoolean(Property.ALL_FOLDERS_FOR_AUTOCOMPLETE).booleanValue()) {
            /*
             * use default set of folders for search
             */
            return getBasicFolders(contextID, userID);
        }
        /*
         * use all visible folders for search
         */
        return getVisibleFolders(contextID, userID);
    }

	/**
	 * Gets all contact folders where the user at least has permissions to read
	 * own objects.
	 *
	 * @param contextID the context ID
	 * @param userID the user ID
	 * @return the folder IDs
	 * @throws OXException
	 */
    public static List<String> getVisibleFolders(final int contextID, final int userID) throws OXException {
        final List<String> folderIDs = new ArrayList<String>();
        final UserConfiguration userConfig = UserConfigurationStorage.getInstance().getUserConfiguration(userID, Tools.getContext(contextID));
        Context context = Tools.getContext(contextID);
        List<FolderObject> folders = OXSubscribeAwareFolderSQL.getAllVisibleFoldersOfModule(userID, userConfig.getGroups(), userConfig.getAccessibleModules(), FolderObject.CONTACT, context);
        for (FolderObject folder : folders) {
            if (FolderObject.CONTACT != folder.getModule()) {
                continue;
            }
            final EffectivePermission permission = Tools.getPermission(contextID, Integer.toString(folder.getObjectID()), userID);
            if (null == permission || false == permission.canReadOwnObjects()) {
                continue;
            }
            folderIDs.add(Integer.toString(folder.getObjectID()));
        }
        return folderIDs;
    }

	/**
	 * Gets a default set of folders used for searches of an user.
	 *
	 * @param contextID The context ID
	 * @param userID The user ID
	 * @return The folder IDs
	 * @throws OXException
	 */
	private static List<String> getBasicFolders(int contextID, int userID) throws OXException {
		List<String> folderIDs = new ArrayList<String>();
		folderIDs.add(String.valueOf(new OXFolderAccess(getContext(contextID)).getDefaultFolderID(userID, FolderObject.CONTACT)));
		if (Tools.getPermission(contextID, Integer.toString(FolderObject.SYSTEM_LDAP_FOLDER_ID), userID).canReadAllObjects()) {
			folderIDs.add(String.valueOf(FolderObject.SYSTEM_LDAP_FOLDER_ID));
		}
		Integer collectedContactFolderID = ServerUserSetting.getInstance().getContactCollectionFolder(contextID, userID);
		if (null != collectedContactFolderID) {
			folderIDs.add(String.valueOf(collectedContactFolderID));
		}
		return folderIDs;
	}

	/**
	 * Parses a numerical identifier from a string, wrapping a possible
	 * NumberFormatException into an OXException.
	 *
	 * @param id the id string
	 * @return the parsed identifier
	 * @throws OXException
	 */
	public static int parse(final String id) throws OXException {
		try {
			return Integer.parseInt(id);
		} catch (NumberFormatException e) {
			throw ContactExceptionCodes.ID_PARSING_FAILED.create(e, id);
		}
	}

	/**
	 * Parses a list of numerical identifiers from strings, wrapping possible
	 * NumberFormatExceptions into an OXException.
	 *
	 * @param ids the list of string IDs
	 * @return a list of numerical IDs
	 * @throws OXException
	 */
	public static List<Integer> parse(List<String> ids) throws OXException {
		List<Integer> intIDs = new ArrayList<Integer>();
		for (String id : ids) {
			intIDs.add(I(parse(id)));
		}
		return intIDs;
	}

	/**
	 * Converts an array of numeric IDs to a list of String identifiers.
	 *
	 * @param ids the numeric identifiers
	 * @return the string identifiers
	 */
	public static List<String> toStringList(int[] ids) {
		List<String> stringList = new ArrayList<String>();
		for (int id : ids) {
			stringList.add(Integer.toString(id));
		}
		return stringList;
	}

	/**
	 * Closes a search iterator silently.
	 *
	 * @param searchIterator The iterator to close
	 */
	public static void close(SearchIterator<?> searchIterator) {
	    SearchIterators.close(searchIterator);
	}

	/**
	 * Invalidates the address properties of the given contact if one of the
	 * corresponding parts of the address is set, e.g. sets the "Business
	 * Address" to <code>null</code>, when the "Postal Code Business" is set.
	 *
	 * Necessary for Outlook, see bug #19827 for details
	 *
	 * @param contact the contact to invalidate the addresses for
	 */
	public static void invalidateAddressesIfNeeded(final Contact contact) {
		if (false == contact.containsAddressBusiness()) {
			if (contact.containsStreetBusiness() || contact.containsPostalCodeBusiness() || contact.containsCityBusiness() ||
					contact.containsStateBusiness() || contact.containsCountryBusiness()) {
				contact.setAddressBusiness(null);
			}
		}
		if (false == contact.containsAddressHome()) {
			if (contact.containsStreetHome() || contact.containsPostalCodeHome() || contact.containsCityHome() ||
					contact.containsStateHome() || contact.containsCountryHome()) {
				contact.setAddressHome(null);
			}
		}
		if (false == contact.containsAddressOther()) {
			if (contact.containsStreetOther() || contact.containsPostalCodeOther() || contact.containsCityOther() ||
					contact.containsStateOther() || contact.containsCountryOther()) {
				contact.setAddressOther(null);
			}
		}
	}

    /**
     * Sets the 'file as' attribute to the contact's display name if needed, i.e. the 'display name'-property is set, while the
     * 'file as'-property isn't.
     *
     * @param contact The contact to set the 'file as' for
     */
    public static void setFileAsIfNeeded(final Contact contact) {
        if (contact.containsDisplayName() && (false == contact.containsFileAs() || Tools.isEmpty(contact.getFileAs()))) {
            contact.setFileAs(contact.getDisplayName());
        }
    }

	/**
     * Creates a new {@link ContactSearchObject} based on the supplied one, but using a specific set of folder IDs.
	 *
	 * @param contactSearch The contact search to prepare
	 * @param folderIDs The folder IDs to use
	 * @return A new contact search object instance with the supplied folder IDs.
	 * @throws OXException
	 */
	public static ContactSearchObject prepareContactSearch(ContactSearchObject contactSearch, List<String> folderIDs) throws OXException {
        ContactSearchObject preparedSearchObject = new ContactSearchObject();
        preparedSearchObject.setFolders(parse(folderIDs));
        preparedSearchObject.setStartLetter(contactSearch.isStartLetter());
        preparedSearchObject.setPattern(contactSearch.getPattern());
        preparedSearchObject.setOrSearch(contactSearch.isOrSearch());
        preparedSearchObject.setEmailAutoComplete(contactSearch.isEmailAutoComplete());
        preparedSearchObject.setExactMatch(contactSearch.isExactMatch());
        preparedSearchObject.setCatgories(contactSearch.getCatgories());
        preparedSearchObject.setCityBusiness(contactSearch.getCityBusiness());
        preparedSearchObject.setCompany(contactSearch.getCompany());
        preparedSearchObject.setDepartment(contactSearch.getDepartment());
        preparedSearchObject.setDisplayName(contactSearch.getDisplayName());
        preparedSearchObject.setEmail1(contactSearch.getEmail1());
        preparedSearchObject.setEmail2(contactSearch.getEmail2());
        preparedSearchObject.setEmail3(contactSearch.getEmail3());
        preparedSearchObject.setGivenName(contactSearch.getGivenName());
        preparedSearchObject.setStreetBusiness(contactSearch.getStreetBusiness());
        preparedSearchObject.setSurname(contactSearch.getSurname());
        preparedSearchObject.setYomiCompany(contactSearch.getYomiCompany());
        preparedSearchObject.setYomiFirstname(contactSearch.getYomiFirstName());
        preparedSearchObject.setYomiLastName(contactSearch.getYomiLastName());
        preparedSearchObject.setHasImage(contactSearch.hasImage());
        return preparedSearchObject;
	}

	/**
	 * Creates a new {@link ContactSearchObject} containing only the relevant
	 * parts of the supplied search object, extending the contained patterns
	 * with implicit wildcards as needed.
	 *
	 * @param contactSearch
	 * @return
	 */
    public static ContactSearchObject prepareContactSearch(ContactSearchObject contactSearch) {
        if (null != contactSearch.getPattern() && 0 < contactSearch.getPattern().length()) {
            /*
             * "Search contacts"
             */
            return prepareSearchContacts(contactSearch);
        }
        /*
         * "Search contacts alternative"
         */
        return prepareSearchContactsAlternative(contactSearch);
    }

    /**
     * Constructs a search object using the supplied parameters.
     *
     * @param session The session
     * @param pattern The search pattern
     * @param requireEmail <code>true</code> if the returned contacts should have at least one e-mail address, <code>false</code>,
     *                     otherwise
     * @param folderIDs A list of folder IDs to restrict the search for, or <code>null</code> to search in folders available for
     *                  auto-complete
     * @return The prepared search object
     * @throws OXException
     */
    public static ContactSearchObject prepareAutocomplete(Session session, String pattern, boolean requireEmail, List<String> folderIDs) throws OXException {
//        pattern = addWildcards(pattern, false, true);
        ContactSearchObject searchObject = new ContactSearchObject();
        searchObject.setOrSearch(true);
        searchObject.setEmailAutoComplete(requireEmail);
        searchObject.setDisplayName(pattern);
        searchObject.setSurname(pattern);
        searchObject.setGivenName(pattern);
        searchObject.setEmail1(pattern);
        searchObject.setEmail2(pattern);
        searchObject.setEmail3(pattern);
        if (null != folderIDs) {
            searchObject.setFolders(parse(folderIDs));
        } else {
            Tools.getSearchFolders(session.getContextId(), session.getUserId(), true);
        }
        return searchObject;
    }

    private static ContactSearchObject prepareSearchContacts(ContactSearchObject contactSearch) {
        ContactSearchObject preparedSearchObject = new ContactSearchObject();
        preparedSearchObject.setFolders(contactSearch.getFolders());
        preparedSearchObject.setExcludeFolders(contactSearch.getExcludeFolders());
        if (contactSearch.isStartLetter()) {
            preparedSearchObject.setStartLetter(true);
            preparedSearchObject.setPattern(addWildcards(contactSearch.getPattern(), false, true));
        } else {
            preparedSearchObject.setStartLetter(false);
            preparedSearchObject.setPattern(addWildcards(contactSearch.getPattern(), true, true));
        }
        return preparedSearchObject;
    }

    private static ContactSearchObject prepareSearchContactsAlternative(ContactSearchObject contactSearch) {
        ContactSearchObject preparedSearchObject = new ContactSearchObject();
        boolean prependWildcard = false == contactSearch.isOrSearch() && false == contactSearch.isEmailAutoComplete();
        boolean appendWildcard = false == contactSearch.isExactMatch();
        preparedSearchObject.setOrSearch(contactSearch.isOrSearch());
        preparedSearchObject.setExactMatch(contactSearch.isExactMatch());
        preparedSearchObject.setEmailAutoComplete(contactSearch.isEmailAutoComplete());
        preparedSearchObject.setFolders(contactSearch.getFolders());
        preparedSearchObject.setExcludeFolders(contactSearch.getExcludeFolders());
        preparedSearchObject.setCatgories(addWildcards(contactSearch.getCatgories(), true, true));
        preparedSearchObject.setCityBusiness(addWildcards(contactSearch.getCityBusiness(), prependWildcard, appendWildcard));
        preparedSearchObject.setCompany(addWildcards(contactSearch.getCompany(), prependWildcard, appendWildcard));
        preparedSearchObject.setDepartment(addWildcards(contactSearch.getDepartment(), prependWildcard, appendWildcard));
        preparedSearchObject.setDisplayName(addWildcards(contactSearch.getDisplayName(), prependWildcard, appendWildcard));
        preparedSearchObject.setEmail1(addWildcards(contactSearch.getEmail1(), prependWildcard, appendWildcard));
        preparedSearchObject.setEmail2(addWildcards(contactSearch.getEmail2(), prependWildcard, appendWildcard));
        preparedSearchObject.setEmail3(addWildcards(contactSearch.getEmail3(), prependWildcard, appendWildcard));
        preparedSearchObject.setGivenName(addWildcards(contactSearch.getGivenName(), prependWildcard, appendWildcard));
        preparedSearchObject.setStreetBusiness(addWildcards(contactSearch.getStreetBusiness(), prependWildcard, appendWildcard));
        preparedSearchObject.setSurname(addWildcards(contactSearch.getSurname(), prependWildcard, appendWildcard));
        preparedSearchObject.setYomiCompany(addWildcards(contactSearch.getYomiCompany(), prependWildcard, appendWildcard));
        preparedSearchObject.setYomiFirstname(addWildcards(contactSearch.getYomiFirstName(), prependWildcard, appendWildcard));
        preparedSearchObject.setYomiLastName(addWildcards(contactSearch.getYomiLastName(), prependWildcard, appendWildcard));
        preparedSearchObject.setHasImage(contactSearch.hasImage());
        return preparedSearchObject;
    }

	private static String addWildcards(String pattern, boolean prepend, boolean append) {
	    if (null != pattern && 0 < pattern.length() && false == "*".equals(pattern)) {
            if (prepend && '*' != pattern.charAt(0)) {
                pattern = "*" + pattern;
            }
            if (append && '*' != pattern.charAt(pattern.length() - 1)) {
                pattern = pattern + "*";
            }
            return pattern;
	    }
	    return null;
	}

	private Tools() {
		// prevent instantiation
	}

}
