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

package com.openexchange.pop3;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link POP3ExceptionMessage}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class POP3ExceptionMessage implements LocalizableStrings {

    /**
     * Initializes a new {@link POP3ExceptionMessage}.
     */
    private POP3ExceptionMessage() {
        super();
    }

    /**
     * No connection available to access mailbox
     */
    public static final String NOT_CONNECTED_MSG = "No connection available to access mailbox";
    /**
     * User %1$s has no mail module access due to user configuration
     */
    public static final String NO_MAIL_MODULE_ACCESS_MSG = "User %1$s has no mail module access due to user configuration";
    /**
     * No access to mail folder %1$s
     */
    public static final String NO_ACCESS_MSG = "You do not have the appropriate permissions to access the mail folder %1$s";
    /**
     * No read access on mail folder %1$s
     */
    public static final String NO_READ_ACCESS_MSG = "You do not have the appropriate permissions to read mail folder %1$s";
    /**
     * No delete access on mail folder %1$s
     */
    public static final String NO_DELETE_ACCESS_MSG = "You do not have the appropriate permissions to delete mail folder %1$s";
    /**
     * No insert access on mail folder %1$s
     */
    public static final String NO_INSERT_ACCESS_MSG = "You do not have the appropriate permissions to insert to mail folder %1$s";
    /**
     * No administer access on mail folder %1$s
     */
    public static final String NO_ADMINISTER_ACCESS_MSG = "You do not have the appropriate permissions to administer mail folder %1$s";
    /**
     * No write access to POP3 folder %1$s
     */
    public static final String NO_WRITE_ACCESS_MSG = "You do not have the appropriate permissions to write to POP3 folder %1$s";
    /**
     * No keep-seen access on mail folder %1$s
     */
    public static final String NO_KEEP_SEEN_ACCESS_MSG = "You do not have the appropriate permissions for a Read/Unread access in the mail folder %1$s";
    /**
     * Folder %1$s does not allow subfolders.
     */
    public static final String FOLDER_DOES_NOT_HOLD_FOLDERS_MSG = "You cannot create subfolders in folder %1$s.";
    /**
     * POP3 does not support mail folder creation
     */
    public static final String FOLDER_CREATION_FAILED_MSG = "POP3 does not support mail folder creation";
    /**
     * The permissions set could not be applied to the new folder %1$s.
     * Its initial permissions specified by the POP3 server do not include administer permissions. The folder has been created, though.
     */
    public static final String NO_ADMINISTER_ACCESS_ON_INITIAL_MSG = "The permissions set could not be applied to the new folder %1$s. Its initial permissions specified by the POP3 server do not include administer permissions. The folder has been created, though.";
    /**
     * No admin permission specified for folder %1$s
     */
    public static final String NO_ADMIN_ACL_MSG = "No administer permission specified for folder %1$s";
    /**
     * Default folder %1$s must not be updated
     */
    public static final String NO_DEFAULT_FOLDER_UPDATE_MSG = "Default folder %1$s cannot be updated";
    /**
     * Deletion of folder %1$s failed
     */
    public static final String DELETE_FAILED_MSG = "Deletion of folder %1$s failed";
    /**
     * POP3 default folder %1$s could not be created
     */
    public static final String NO_DEFAULT_FOLDER_CREATION_MSG = "POP3 default folder %1$s could not be created";
    /**
     * Missing default %1$s folder in user mail settings
     */
    public static final String MISSING_DEFAULT_FOLDER_NAME_MSG = "Missing default %1$s folder in user mail settings";
    /**
     * Update of folder %1$s failed
     */
    public static final String UPDATE_FAILED_MSG = "Update of folder %1$s failed";
    /**
     * Folder %1$s must not be deleted
     */
    public static final String NO_FOLDER_DELETE_MSG = "Folder %1$s cannot be deleted";
    /**
     * Default folder %1$s must not be deleted
     */
    public static final String NO_DEFAULT_FOLDER_DELETE_MSG = "Default folder %1$s cannot be deleted";
    /**
     * Flag %1$s could not be changed due to following reason: %2$s
     */
    public static final String FLAG_FAILED_MSG = "Flag %1$s could not be changed due to the following reason: %2$s";
    /**
     * Number of search fields (%d) do not match number of search patterns (%d)
     */
    public static final String INVALID_SEARCH_PARAMS_MSG = "Number of search fields (%d) do not match number of search patterns (%d)";
    /**
     * POP3 search failed due to following reason: %1$s. Switching to application-based search
     */
    public static final String POP3_SEARCH_FAILED_MSG = "POP3 search failed due to the following reason: %1$s. Switching to application-based search";
    /**
     * POP3 sort failed due to following reason: %1$s Switching to application-based sorting
     */
    public static final String POP3_SORT_FAILED_MSG = "POP3 sort failed due to the following reason: %1$s Switching to application-based sorting";
    /**
     * Unknown search field: %1$s
     */
    public static final String UNKNOWN_SEARCH_FIELD_MSG = "The supplied search field \"%1$s\" is unknown.";
    /**
     * Mail folder %1$s must not be moved to subsequent folder %2$s
     */
    public static final String NO_MOVE_TO_SUBFLD_MSG = "Moving mail folder %1$s to subsequent folder %2$s is not allowed.";
    /**
     * This message could not be moved to trash folder, possibly because your mailbox is nearly full.<br>
     * In that case, please try to empty your deleted items first, or delete smaller messages first.
     */
    public static final String MOVE_ON_DELETE_FAILED_MSG = "This message could not be moved to trash folder, possibly because your mailbox is nearly full. In that case, please try to empty your deleted items first, or delete smaller messages first.";
    /**
     * Missing %1$s folder in mail move operation
     */
    public static final String MISSING_SOURCE_TARGET_FOLDER_ON_MOVE_MSG = "Missing %1$s folder in mail move operation. Please provide one and try again.";
    /**
     * Message move aborted for user %1$s. Source and destination folder are equal: %2$s
     */
    public static final String NO_EQUAL_MOVE_MSG = "Message move aborted for user %1$s. Source and destination folders are the same: %2$s";
    /**
     * Folder read-only check failed
     */
    public static final String FAILED_READ_ONLY_CHECK_MSG = "POP3 folder read-only check failed";
    /**
     * Unknown folder open mode %d
     */
    public static final String UNKNOWN_FOLDER_MODE_MSG = "Unknown folder open mode %d";
    /**
     * Message(s) %1$s in folder %2$s could not be deleted due to following error: %3$s
     */
    public static final String UID_EXPUNGE_FAILED_MSG = "Message(s) %1$s in folder %2$s could not be deleted due to the following error: %3$s";
    /**
     * Not allowed to open folder %1$s due to missing read access
     */
    public static final String NO_FOLDER_OPEN_MSG = "You do not have the appropriate permissions to open folder %1$s due to missing read access";
    /**
     * The raw content's input stream of message %1$s in folder %2$s cannot be read
     */
    public static final String MESSAGE_CONTENT_ERROR_MSG = "The raw content's input stream of message %1$s in folder %2$s cannot be read";
    /**
     * No attachment was found with id %1$s in message
     */
    public static final String NO_ATTACHMENT_FOUND_MSG = "No attachment was found with id %1$s in message";
    /**
     * Versit object %1$s could not be saved
     */
    public static final String FAILED_VERSIT_SAVE_MSG = "Versit object (i.e. vCard or vCalendar) could not be saved";
    /**
     * POP3 server does not support capability "THREAD=REFERENCES"
     */
    public static final String THREAD_SORT_NOT_SUPPORTED_MSG = "Thread sorting is not supported by the server.";
    /**
     * POP3 does not support to move folders.
     */
    public static final String MOVE_DENIED_MSG = "Moving folders is not supported by the server.";
    /**
     * Sort field %1$s is not supported via POP3 SORT command
     */
    public static final String UNSUPPORTED_SORT_FIELD_MSG = "The sort field you supplied is unsupported.";
    /**
     * Missing personal namespace
     */
    public static final String MISSING_PERSONAL_NAMESPACE_MSG = "Missing personal namespace. Please provide one and try again.";
    /**
     * POP3 does not support to create folders.
     */
    public static final String CREATE_DENIED_MSG = "The creation of folders is not supported by the server.";
    /**
     * POP3 does not support to delete folders.
     */
    public static final String DELETE_DENIED_MSG = "The deletion of folders is not supported by the server.";
    /**
     * POP3 does not support to update folders.
     */
    public static final String UPDATE_DENIED_MSG = "Updating folders is not supported by the server.";
    /**
     * POP3 does not support to move messages.
     */
    public static final String MOVE_MSGS_DENIED_MSG = "Moving messages is not supported by the server.";
    /**
     * POP3 does not support to copy messages.
     */
    public static final String COPY_MSGS_DENIED_MSG = "Copying messages is not supported by the server.";
    /**
     * POP3 does not support draft messages.
     */
    public static final String DRAFTS_NOT_SUPPORTED_MSG = "Draft messages are not supported by the server.";
    /**
     * Missing POP3 storage name for user %1$s in context %2$s.
     */
    public static final String MISSING_POP3_STORAGE_NAME_MSG = "The POP3 storage name is missing for user %1$s. Please provide one and try again.";
    /**
     * Missing POP3 storage for user %1$s in context %2$s.
     */
    public static final String MISSING_POP3_STORAGE_MSG = "The POP3 storage is missing for user %1$s. Please provide one and try again";
    /**
     * POP3 default folder %1$s must not be moved.
     */
    public static final String NO_DEFAULT_FOLDER_MOVE_MSG = "You are not allowed to move the POP3 default folder %1$s.";
    /**
     * POP3 default folder %1$s must not be renamed.
     */
    public static final String NO_DEFAULT_FOLDER_RENAME_MSG = "You are not allowed to rename the POP3 default folder %1$s.";
    /**
     * Missing POP3 storage path for user %1$s in context %2$s.
     */
    public static final String MISSING_PATH_MSG = "The POP3 storage path is missing for user %1$s. Please provide one and try again.";
    /**
     * Illegal move operation.
     */
    public static final String MOVE_ILLEGAL_MSG = "The move operation you want to execute is not allowed.";
    /**
     * Login delay denies connecting to server %1$s with login %2$s (user=%3$s, context=%4$s).<br>
     * Error message from server: %5$s
     */
    public static final String LOGIN_DELAY_MSG = "Login delay denies connecting to server %1$s with login %2$s. Error message from server: %5$s";
    /**
     * Login delay denies connecting to server %1$s with login %2$s (user=%3$s, context=%4$s). Try again in %5$s seconds.<br>
     * Error message from server: %6$s
     */
    public static final String LOGIN_DELAY2_MSG = "Login delay denies connecting to server %1$s with login %2$s. Try again in %5$s seconds. Error message from server: %6$s";
    /**
     * POP3 storage path "%1$s" cannot be created for user %2$s in context %3$s.
     */
    public static final String ILLEGAL_PATH_MSG = "The POP3 storage path is invalid. POP3 storage path \"%1$s\" cannot be created for user %2$s";
    /**
     * Validation of POP3 credentials is disabled due to possible login restrictions by provider. Otherwise subsequent login attempt might not work. Please be advised that while it is safe to ignore this warning, the POP3 account might not work if the supplied credentials are invalid.
     */
    public static final String VALIDATE_DENIED_MSG = "Validation of POP3 credentials is disabled due to possible login restrictions by provider. Otherwise subsequent login attempt might not work. Please be advised that while it is safe to ignore this warning, the POP3 account might not work if the supplied credentials are invalid.";

    // POP3 messages cannot be imported because of existing quota constraints on primary mail account. Please free some space.
    public static final String QUOTA_CONSTRAINT_MSG = "POP3 messages cannot be imported because of existing quota constraints on primary mail account. Please free some space.";

}
