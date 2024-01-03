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

package com.openexchange.chronos.common;

import com.openexchange.annotation.NonNull;
import com.openexchange.annotation.Nullable;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.scheduling.IncomingIMip;
import com.openexchange.chronos.scheduling.IncomingSchedulingObject;
import com.openexchange.chronos.scheduling.MessageStatus;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;

/**
 * {@link IncomingSchedulingObjectBuilder}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class IncomingSchedulingObjectBuilder {

    protected CalendarUser originator;
    protected String mailAccountId;
    protected String mailFolderId;
    protected String mailId;
    protected String messageId;
    protected String sequenceId;
    protected MessageStatus state;

    /**
     * Initializes a new {@link IncomingSchedulingObjectBuilder}.
     */
    private IncomingSchedulingObjectBuilder() {}

    /**
     * Initializes a new {@link IncomingSchedulingMessageBuilder}.
     *
     * @return This instance for chaining
     */
    public static IncomingSchedulingObjectBuilder newBuilder() {
        return new IncomingSchedulingObjectBuilder();
    }

    /**
     * Set the originator
     *
     * @param originator The originator to set
     * @return This instance for chaining
     */
    public IncomingSchedulingObjectBuilder setOriginator(CalendarUser originator) {
        this.originator = originator;
        return this;
    }

    /**
     * Sets the identifier of the mail folder the iMIP mail was received in
     *
     * @param mailAccountId The mail account identifier
     * @return This instance for chaining
     */
    public IncomingSchedulingObjectBuilder setMailAccountId(String mailAccountId) {
        this.mailAccountId = mailAccountId;
        return this;
    }

    /**
     * Sets the identifier of the mail folder the iMIP mail was received in
     *
     * @param mailFolderId The mail folder identifier
     * @return This instance for chaining
     */
    public IncomingSchedulingObjectBuilder setMailFolderId(String mailFolderId) {
        this.mailFolderId = mailFolderId;
        return this;
    }

    /**
     * Sets the identifier of the iMIP mail
     *
     * @param mailId The mail identifier
     * @return This instance for chaining
     */
    public IncomingSchedulingObjectBuilder setMailId(String mailId) {
        this.mailId = mailId;
        return this;
    }

    /**
     * Sets the sequence identifier of the attachment part
     * containing the iCAL file to process
     *
     * @param sequenceId The sequence identifier
     * @return This instance for chaining
     */
    public IncomingSchedulingObjectBuilder setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
        return this;
    }

    /**
     * Sets the state of the mail based on the flags
     *
     * @param state The state of the mail
     * @return This instance for chaining
     */
    public IncomingSchedulingObjectBuilder setState(MessageStatus state) {
        this.state = state;
        return this;
    }

    /**
     * Builds the object, based on the set data either
     * {@link IncomingSchedulingObject} or {@link IncomingIMip}
     *
     * @return The {@link IncomingSchedulingObject}
     * @throws OXException In case data is missing
     */
    public IncomingSchedulingObject build() throws OXException {
        if (Strings.isEmpty(mailId)) {
            return new IncomingSchedulingObjectImpl(this);
        }
        return new IncomingIMipImpl(this);
    }

    /**
     * Set the unique message identifier of the mail
     *
     * @param messageId The unique message identifier of the mail
     * @return The {@link IncomingSchedulingObject}
     */
    public IncomingSchedulingObjectBuilder setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }
}

class IncomingSchedulingObjectImpl implements IncomingSchedulingObject {

    private final @NonNull CalendarUser originator;

    /**
     * Initializes a new {@link IncomingSchedulingObjectImpl}.
     * 
     * @param builder The builder
     */
    public IncomingSchedulingObjectImpl(IncomingSchedulingObjectBuilder builder) {
        super();
        CalendarUser originator = builder.originator;
        if (null == originator) {
            throw new IllegalStateException();
        }
        this.originator = originator;
    }

    @Override
    @NonNull
    public CalendarUser getOriginator() throws OXException {
        return originator;
    }

    @Override
    public String toString() {
        return "IncomingSchedulingMailMeta [originator=" + originator + "]";
    }

}

class IncomingIMipImpl implements IncomingIMip {

    private final @NonNull CalendarUser originator;
    private final @NonNull String mailAccountId;
    private final @NonNull String mailFolderId;
    private final @NonNull String mailId;
    private final @Nullable String messageId;
    private final @Nullable MessageStatus state;

    /**
     * Initializes a new {@link IncomingSchedulingObjectImpl}.
     * 
     * @param builder The builder
     * @throws OXException In case data is missing
     */
    public IncomingIMipImpl(IncomingSchedulingObjectBuilder builder) throws OXException {
        CalendarUser originator = builder.originator;
        if (null == originator) {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Missing mail attribute");
        }
        this.originator = originator;
        this.mailAccountId = notNull(builder.mailAccountId);
        this.mailFolderId = notNull(builder.mailFolderId);
        this.mailId = notNull(builder.mailId);
        this.messageId = builder.messageId;
        this.state = builder.state;
    }

    @Override
    @NonNull
    public CalendarUser getOriginator() throws OXException {
        return originator;
    }

    @Override
    public String getMailAccountId() {
        return mailAccountId;
    }

    @Override
    public String getMailFolderId() {
        return mailFolderId;
    }

    @Override
    public String getMailId() {
        return mailId;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    @Nullable
    public MessageStatus getState() {
        return state;
    }

    @Override
    public String toString() {
        return "IncomingIMipImpl [originator=" + originator + ", mailFolderId=" + mailFolderId + ", mailId=" + mailId + ", messageId=" + messageId + "]";
    }

    @SuppressWarnings("null")
    private static @NonNull String notNull(String s) throws OXException {
        if (Strings.isEmpty(s)) {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Missing mail attribute");
        }
        return s;
    }

}
