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

package com.openexchange.smtp;

import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.mail.Protocol;
import com.openexchange.mail.api.AbstractProtocolProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.dataobjects.compose.DataMailPart;
import com.openexchange.mail.dataobjects.compose.InfostoreDocumentMailPart;
import com.openexchange.mail.dataobjects.compose.ReferencedMailPart;
import com.openexchange.mail.dataobjects.compose.TextBodyMailPart;
import com.openexchange.mail.dataobjects.compose.UploadFileMailPart;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.transport.TransportProvider;
import com.openexchange.session.Session;
import com.openexchange.smtp.config.SMTPProperties;
import com.openexchange.smtp.config.SMTPSessionProperties;
import com.openexchange.smtp.dataobjects.SMTPBodyPart;
import com.openexchange.smtp.dataobjects.SMTPDataPart;
import com.openexchange.smtp.dataobjects.SMTPDocumentPart;
import com.openexchange.smtp.dataobjects.SMTPFilePart;
import com.openexchange.smtp.dataobjects.SMTPMailMessage;
import com.openexchange.smtp.dataobjects.SMTPReferencedPart;

/**
 * {@link SMTPProvider}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SMTPProvider extends TransportProvider {

    /**
     * SMTP protocol
     */
    public static final Protocol PROTOCOL_SMTP = new Protocol("smtp", "smtps");

    private static final SMTPProvider instance = new SMTPProvider();

    /**
     * Gets the singleton instance of SMTP provider
     *
     * @return The singleton instance of SMTP provider
     */
    public static SMTPProvider getInstance() {
        return instance;
    }

    /**
     * Initializes a new {@link SMTPProvider}
     */
    private SMTPProvider() {
        super();
    }

    @Override
    protected void startUp() throws OXException {
        super.startUp();
        SMTPCapabilityCache.init();
    }

    @Override
    protected void shutDown() throws OXException {
        SMTPSessionProperties.resetDefaultSessionProperties();
        SMTPCapabilityCache.tearDown();
        super.shutDown();
    }

    @Override
    public MailTransport createNewMailTransport(final Session session) throws OXException {
        return new DefaultSMTPTransport(session);
    }

    @Override
    public MailTransport createNewMailTransport(final Session session, final int accountId) throws OXException {
        return new DefaultSMTPTransport(session, accountId);
    }

    @Override
    public MailTransport createNewNoReplyTransport(int contextId) throws OXException {
        return new NoReplySMTPTransport(contextId);
    }

    @Override
    public MailTransport createNewNoReplyTransport(int contextId, boolean useNoReplyAddress) throws OXException {
        return new NoReplySMTPTransport(contextId, useNoReplyAddress);
    }

    @Override
    public ComposedMailMessage getNewComposedMailMessage(final Session session, final Context ctx) throws OXException {
        return new SMTPMailMessage(session, ctx);
    }

    @Override
    public InfostoreDocumentMailPart getNewDocumentPart(final String documentId, final Session session) throws OXException {
        return new SMTPDocumentPart(documentId, session);
    }

    @Override
    public UploadFileMailPart getNewFilePart(final UploadFile uploadFile) throws OXException {
        return new SMTPFilePart(uploadFile);
    }

    @Override
    public ReferencedMailPart getNewReferencedPart(final MailPart referencedPart, final Session session) throws OXException {
        return new SMTPReferencedPart(referencedPart, session);
    }

    @Override
    public ReferencedMailPart getNewReferencedMail(final MailMessage referencedMail, final Session session) throws OXException {
        return new SMTPReferencedPart(referencedMail, session);
    }

    @Override
    public TextBodyMailPart getNewTextBodyPart(final String textBody) throws OXException {
        return new SMTPBodyPart(textBody);
    }

    @Override
    public Protocol getProtocol() {
        return PROTOCOL_SMTP;
    }

    @Override
    protected AbstractProtocolProperties getProtocolProperties() {
        return SMTPProperties.getInstance();
    }

    @Override
    public DataMailPart getNewDataPart(final Object data, final Map<String, String> dataProperties, final Session session) throws OXException {
        return new SMTPDataPart(data, dataProperties, session);
    }

}
