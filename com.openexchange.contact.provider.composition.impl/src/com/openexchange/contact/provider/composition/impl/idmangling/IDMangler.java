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

package com.openexchange.contact.provider.composition.impl.idmangling;

import static com.openexchange.contact.common.ContactsAccount.DEFAULT_ACCOUNT;
import static com.openexchange.groupware.contact.helpers.ContactField.getByAjaxName;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.common.AccountAwareContactsFolder;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.common.ContactsFolder;
import com.openexchange.contact.common.GroupwareContactsFolder;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.results.UpdatesResult;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.Operand;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.internal.operands.ConstantOperand;

/**
 * {@link IDMangler} - The account aware IDMangler for contact folder identifiers.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class IDMangler extends com.openexchange.contact.provider.composition.IDMangling {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDMangler.class);

    /** The pattern to lookup folder place holders in contacts exception messages */
    private static final Pattern FOLDER_ARGUMENT_PATTERN = Pattern.compile("(?:\\[|,|)(?:[fF]older(?: | id |\\: )%(\\d)\\$[sd])(?:\\]|,| )");

    /**
     * Gets the account-relative representation for the supplied contact with unique composite identifiers.
     *
     * @param contact The contact
     * @return The contact representation with relative identifiers
     */
    public static Contact withRelativeID(Contact contact) throws OXException {
        DistributionListEntryObject[] distributionList = contact.getDistributionList();
        if (null != distributionList && 0 < distributionList.length) {
            for (DistributionListEntryObject entry : distributionList) {
                if (null != entry.getFolderID()) {
                    entry.setFolderID(getRelativeFolderId(entry.getFolderID()));
                }
            }
        }
        contact.setFolderId(getRelativeFolderId(getEffectiveFolderId(contact)));
        return contact;
    }

    /**
     * Gets the account-relative representation for the supplied contacts folder with unique composite identifiers.
     *
     * @param folder The contacts folder
     * @return The contacts folder representation with relative identifiers
     */
    public static ContactsFolder withRelativeID(ContactsFolder folder) throws OXException {
        String newId = getRelativeFolderId(folder.getId());
        if ((folder instanceof GroupwareContactsFolder)) {
            GroupwareContactsFolder groupwareFolder = (GroupwareContactsFolder) folder;
            String newParentId = getRelativeFolderId(groupwareFolder.getParentId());
            return new IDManglingGroupwareContactsFolder(groupwareFolder, newId, newParentId);
        }
        return new IDManglingContactsFolder(folder, newId);
    }

    /**
     * Re-creates the specified {@link SearchTerm} with a relative folder id
     *
     * @param searchTerm The {@link SearchTerm} to re-create
     * @return The re-created {@link SearchTerm}
     */
    public static SearchTerm<?> withRelativeID(SearchTerm<?> searchTerm) throws OXException {
        return recreateTerm(searchTerm);
    }

    /**
     * Gets a contact equipped with unique composite identifiers representing a contact from a specific contacts account.
     *
     * @param contact The contact from the account, or <code>null</code> to pass through
     * @param accountId The identifier of the account
     * @return The contact representation with unique identifiers
     */
    public static Contact withUniqueID(Contact contact, int accountId) {
        if (null == contact) {
            return null;
        }
        DistributionListEntryObject[] distributionList = contact.getDistributionList();
        if (null != distributionList && 0 < distributionList.length) {
            for (DistributionListEntryObject entry : distributionList) {
                if (null != entry.getFolderID()) {
                    entry.setFolderID(getUniqueFolderId(accountId, entry.getFolderID()));
                }
            }
        }
        contact.setFolderId(getUniqueFolderId(accountId, contact.getFolderId(true)));
        return contact;
    }

    /**
     * Gets a list of contacts folders equipped with unique composite identifiers representing the supplied list of contacts folders from
     * a specific contacts account.
     *
     * @param folders The contacts folders from the account
     * @param account The contacts account
     * @return The contacts folder representations with unique identifiers
     */
    public static List<AccountAwareContactsFolder> withUniqueID(List<? extends ContactsFolder> folders, ContactsAccount account) {
        if (null == folders) {
            return null;
        }
        List<AccountAwareContactsFolder> foldersWithUniqueIDs = new ArrayList<>(folders.size());
        for (ContactsFolder folder : folders) {
            foldersWithUniqueIDs.add(withUniqueID(folder, account));
        }
        return foldersWithUniqueIDs;
    }

    /**
     * Gets a contacts folder equipped with unique composite identifiers representing a contacts folder from a specific contacts account.
     *
     * @param folders The contacts folder from the account
     * @param account The contacts account
     * @return The contacts folder representation with unique identifiers
     */
    public static AccountAwareContactsFolder withUniqueID(ContactsFolder folder, ContactsAccount account) {
        if ((folder instanceof GroupwareContactsFolder)) {
            GroupwareContactsFolder groupwareFolder = (GroupwareContactsFolder) folder;
            String newId = getUniqueFolderId(account.getAccountId(), folder.getId(), true);
            String newParentId = getUniqueFolderId(account.getAccountId(), groupwareFolder.getParentId(), true);
            return new IDManglingContactsAccountAwareGroupwareFolder(groupwareFolder, account, newId, newParentId);
        }
        return new IDManglingContactsAccountAwareFolder(folder, account, getUniqueFolderId(account.getAccountId(), folder.getId()));
    }

    /**
     * Gets a list of contact results equipped with unique composite identifiers representing results from a specific contacts account.
     *
     * @param relativeResults The contacts from the account
     * @param accountId The identifier of the account
     * @return The contact representations with unique identifiers
     */
    public static List<Contact> withUniqueIDs(List<Contact> relativeResults, int accountId) {
        if (null == relativeResults || relativeResults.isEmpty()) {
            return relativeResults;
        }
        List<Contact> contacts = new ArrayList<>(relativeResults.size());
        for (Contact contact : relativeResults) {
            contacts.add(withUniqueID(contact, accountId));
        }
        return contacts;
    }

    /**
     * Gets an updates result equipped with unique composite identifiers representing results from a specific contacts account.
     *
     * @param relativeResult The updates result from the account
     * @param accountId The identifier of the account
     * @return The passed updates result, with the contained lists being adjusted with unique identifiers
     */
    public static UpdatesResult<Contact> withUniqueIDs(UpdatesResult<Contact> relativeResult, int accountId) {
        if (null == relativeResult || relativeResult.isEmpty()) {
            return relativeResult;
        }
        withUniqueIDs(relativeResult.getDeletedObjects(), accountId);
        withUniqueIDs(relativeResult.getNewAndModifiedObjects(), accountId);
        return relativeResult;
    }

    /**
     * Adjusts an exception raised by a specific contacts account so that any referenced identifiers appear in their unique composite
     * representation.
     *
     * @param e The exception to adjust, or <code>null</code> to do nothing
     * @param accountId The identifier of the account
     * @return The possibly adjusted exception
     */
    public static OXException withUniqueIDs(OXException e, int accountId) {
        if (null == e || false == e.isPrefix("CON")) {
            return e;
        }
        return adjustFolderArguments(e, accountId);
    }

    /**
     * Gets the relative representation of a list of unique full contact identifier, mapped to their associated account identifier.
     *
     * @param uniqueFolderIds The unique composite folder identifiers, e.g. <code>con://11/38</code>
     * @return The relative folder identifiers, mapped to their associated contacts account identifier
     * @throws OXException {@link ContactsProviderExceptionCodes#UNSUPPORTED_FOLDER} if the account identifier can't be extracted from a passed composite identifier
     */
    public static Map<Integer, List<ContactID>> getRelativeIdsPerAccountId(List<ContactID> uniqueContactIDs) throws OXException {
        Map<Integer, List<ContactID>> idsPerAccountId = new HashMap<>();
        for (ContactID contactID : uniqueContactIDs) {
            Integer accountId = I(getAccountId(contactID.getFolderID()));
            ContactID relativeContactId = getRelativeId(contactID);
            com.openexchange.tools.arrays.Collections.put(idsPerAccountId, accountId, relativeContactId);
        }
        return idsPerAccountId;
    }

    /**
     * Gets the relative representation of a list of unique composite folder identifier, mapped to their associated account identifier.
     * <p/>
     * {@link IDMangler#ROOT_FOLDER_IDS} are passed as-is implicitly, mapped to the default account.
     *
     * @param uniqueFolderIds The unique composite folder identifiers, e.g. <code>con://11/38</code>
     * @param errorsPerFolderId A map to track possible errors that occurred when parsing the supplied identifiers
     * @return The relative folder identifiers, mapped to their associated contacts account identifier
     */
    public static Map<Integer, List<String>> getRelativeFolderIdsPerAccountId(List<String> uniqueFolderIds) {
        if (null == uniqueFolderIds || uniqueFolderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, List<String>> foldersPerAccountId = new HashMap<>(uniqueFolderIds.size());
        for (String uniqueFolderId : uniqueFolderIds) {
            try {
                List<String> unmangledId = unmangleFolderId(uniqueFolderId);
                Integer accountId = Integer.valueOf(unmangledId.get(1));
                String relativeFolderId = unmangledId.get(2);
                com.openexchange.tools.arrays.Collections.put(foldersPerAccountId, accountId, relativeFolderId);
            } catch (IllegalArgumentException e) {
                //Ignore; carry on with unmangling
                LOGGER.debug(ContactsProviderExceptionCodes.UNSUPPORTED_FOLDER.create(e, uniqueFolderId, null).getMessage());
                com.openexchange.tools.arrays.Collections.put(foldersPerAccountId, I(DEFAULT_ACCOUNT.getAccountId()), uniqueFolderId);
            }
        }
        return foldersPerAccountId;
    }

    ////////////////////////////////// INTERNAL HELPERS //////////////////////////////////

    /**
     * Adjusts the log arguments indicating a <code>folder</code> in an exception raised by a specific contacts account so that any
     * referenced folder identifiers appear in their unique composite representation.
     *
     * @param e The contacts exception to adjust
     * @param accountId The identifier of the account
     * @return The possibly adjusted exception
     */
    private static OXException adjustFolderArguments(OXException e, int accountId) {
        try {
            OXExceptionCode exceptionCode = e.getExceptionCode();
            Object[] logArgs = e.getLogArgs();
            if (null != logArgs && 0 < logArgs.length && null != exceptionCode && null != exceptionCode.getMessage()) {
                boolean adjusted = false;
                Matcher matcher = FOLDER_ARGUMENT_PATTERN.matcher(exceptionCode.getMessage());
                while (matcher.find()) {
                    int argumentIndex = Integer.parseInt(matcher.group(1));
                    if (0 < argumentIndex && argumentIndex <= logArgs.length && (logArgs[argumentIndex - 1] instanceof String)) {
                        logArgs[argumentIndex - 1] = getUniqueFolderId(accountId, (String) logArgs[argumentIndex - 1]);
                        adjusted = true;
                    }
                }
                if (adjusted) {
                    e.setLogMessage(exceptionCode.getMessage(), logArgs);
                }
            }
        } catch (Exception x) {
            LOGGER.warn("Unexpected error while attempting to replace exception log arguments for {}", e.getLogMessage(), x);
        }
        return e;
    }

    /**
     * Recreates the specified {@link SearchTerm} with the relative folder id
     *
     * @param term The {@link SearchTerm} to recreate
     * @return The recreated {@link SearchTerm}
     * @throws OXException if the search term is invalid
     */
    private static SearchTerm<?> recreateTerm(SearchTerm<?> term) throws OXException {
        if ((term instanceof SingleSearchTerm)) {
            return recreateTerm((SingleSearchTerm) term);
        } else if ((term instanceof CompositeSearchTerm)) {
            return recreateTerm((CompositeSearchTerm) term);
        } else {
            throw new IllegalArgumentException("Need either a 'SingleSearchTerm' or 'CompositeSearchTerm'.");
        }
    }

    /**
     * Recreates the specified {@link CompositeSearchTerm} with the relative folder id
     *
     * @param term The {@link SearchTerm} to recreate
     * @return The recreated {@link SearchTerm}
     * @throws OXException if the search term is invalid
     */
    private static CompositeSearchTerm recreateTerm(CompositeSearchTerm term) throws OXException {
        CompositeSearchTerm compositeTerm = new CompositeSearchTerm(term.getOperation());
        for (SearchTerm<?> operand : term.getOperands()) {
            compositeTerm.addSearchTerm(recreateTerm(operand));
        }
        return compositeTerm;
    }

    /**
     * Recreates the specified {@link SingleSearchTerm} with the relative folder id
     *
     * @param term The {@link SearchTerm} to recreate
     * @return The recreated {@link SearchTerm}
     * @throws OXException if the search term is invalid
     */
    @SuppressWarnings("deprecation")
    private static SingleSearchTerm recreateTerm(SingleSearchTerm term) throws OXException {
        SingleSearchTerm newTerm = new SingleSearchTerm(term.getOperation());
        Operand<?>[] operands = term.getOperands();
        for (int i = 0; i < operands.length; i++) {
            if (Operand.Type.COLUMN != operands[i].getType()) {
                newTerm.addOperand(operands[i]);
                continue;
            }
            ContactField field = null;
            Object value = operands[i].getValue();
            if (null == value) {
                throw new IllegalArgumentException("column operand without value: " + operands[i]);
            } else if ((value instanceof ContactField)) {
                field = (ContactField) value;
            } else {
                //TODO: This is basically for backwards compatibility until AJAX names are no longer used in search terms.
                field = getByAjaxName(value.toString());
            }
            if (false == containsFolderId(operands, i, field)) {
                newTerm.addOperand(operands[i]);
                continue;
            }
            newTerm.addOperand(new ContactFieldOperand(ContactField.FOLDER_ID));
            String folderId = (String) operands[i + 1].getValue();
            i++;
            try {
                Integer.parseInt(folderId);
                newTerm.addOperand(new ConstantOperand<>(folderId));
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                newTerm.addOperand(new ConstantOperand<>(getRelativeFolderId(folderId)));
            }
        }
        return newTerm;
    }

    /**
     * Checks whether the specified {@link Operand}s contain the {@link ContactField#FOLDER_ID}
     * column as well as a value for that.
     *
     * @param operands The {@link Operand}s to check
     * @param i The array position
     * @param field The field
     * @return <code>true</code> if the operands contain the {@link ContactField#FOLDER_ID} and its value is not <code>null</code>;
     *         <code>false</code> otherwise
     */
    private static boolean containsFolderId(Operand<?>[] operands, int i, ContactField field) {
        return null != field && ContactField.FOLDER_ID.equals(field) && i + 1 < operands.length && null != operands[i + 1] && null != operands[i + 1].getValue();
    }

    /**
     * Gets the parent folder identifier of the supplied contact, first probing {@link Contact#getFolderId()}, then falling back to
     * {@link Contact#getParentFolderID()}.
     *
     * @param contact The contact to get the parent folder identifier for
     * @return The parent folder identifier, or <code>null</code> if not set
     */
    private static String getEffectiveFolderId(Contact contact) {
        if (contact.containsFolderId()) {
            return contact.getFolderId();
        }
        if (contact.containsParentFolderID()) {
            return String.valueOf(contact.getParentFolderID());
        }
        return null;
    }

}
