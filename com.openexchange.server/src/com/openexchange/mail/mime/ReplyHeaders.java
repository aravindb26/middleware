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

package com.openexchange.mail.mime;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.mime.utils.MimeMessageUtility;

/**
 * {@link ReplyHeaders} - Utility class for setting reply headers.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class ReplyHeaders {

    /**
     * Initializes a new {@link ReplyHeaders}.
     */
    private ReplyHeaders() {
        super();
    }

    /**
     * Sets the appropriate headers <code>In-Reply-To</code> and <code>References</code> in specified MIME message.
     * <p>
     * Moreover the <code>Reply-To</code> header is set.
     *
     * @param referencedMail The referenced mail
     * @param mimeMessage The MIME message
     * @param maxReferencesLength The optional max. length for <code>"References"</code> header
     * @throws OXException If setting the reply headers fails
     */
    public static void setReplyHeaders(MailMessage referencedMail, MimeMessage mimeMessage, Integer... maxReferencesLength) throws OXException {
        ReplyHeadersResult result = determineReplyHeaders(referencedMail, maxReferencesLength);
        if (NO_REPLY_HEADERS_RESULT == result) {
            // Cancel setting reply headers Message-Id, In-Reply-To, and References.
            return;
        }

        if (result.inReplyTo != null) {
            try {
                mimeMessage.setHeader(HDR_IN_REPLY_TO, result.inReplyTo);
            } catch (MessagingException e) {
                throw MimeMailException.handleMessagingException(e);
            }
        }
        if (result.references != null) {
            try {
                mimeMessage.setHeader(HDR_REFERENCES, result.references);
            } catch (MessagingException e) {
                throw MimeMailException.handleMessagingException(e);
            }
        }
    }

    /**
     * Sets the appropriate headers <code>In-Reply-To</code> and <code>References</code> in specified mail message.
     * <p>
     * Moreover the <code>Reply-To</code> header is set.
     *
     * @param referencedMail The referenced mail
     * @param mailMessage The mail message
     * @param maxReferencesLength The optional max. length for <code>"References"</code> header
     */
    public static void setReplyHeaders(MailMessage referencedMail, MailMessage mailMessage, Integer... maxReferencesLength) {
        ReplyHeadersResult result = determineReplyHeaders(referencedMail, maxReferencesLength);
        if (NO_REPLY_HEADERS_RESULT == result) {
            // Cancel setting reply headers Message-Id, In-Reply-To, and References.
            return;
        }

        if (result.inReplyTo != null) {
            mailMessage.setHeader(HDR_IN_REPLY_TO, result.inReplyTo);
        }
        if (result.references != null) {
            mailMessage.setHeader(HDR_REFERENCES, result.references);
        }
    }

    /**
     * Sets a header name-value-pair.
     */
    @FunctionalInterface
    public static interface HeaderSetter {

        /**
         * Sets the denoted header.
         *
         * @param name The header name
         * @param value The header value
         * @throws OXException If header cannot be set
         */
        void setHeader(String name, String value) throws OXException;
    }

    /**
     * Sets the appropriate headers <code>In-Reply-To</code> and <code>References</code> in specified MIME message.
     * <p>
     * Moreover the <code>Reply-To</code> header is set.
     *
     * @param referencedMail The referenced mail
     * @param headerSetter The setter to use to set reply headers
     * @param maxReferencesLength The optional max. length for <code>"References"</code> header
     * @throws OXException If setting the reply headers fails
     */
    public static void setReplyHeaders(MailMessage referencedMail, HeaderSetter headerSetter, Integer... maxReferencesLength) throws OXException {
        ReplyHeadersResult result = determineReplyHeaders(referencedMail, maxReferencesLength);
        if (NO_REPLY_HEADERS_RESULT == result) {
            // Cancel setting reply headers Message-Id, In-Reply-To, and References.
            return;
        }

        if (result.inReplyTo != null) {
            headerSetter.setHeader(HDR_IN_REPLY_TO, result.inReplyTo);
        }
        if (result.references != null) {
            headerSetter.setHeader(HDR_REFERENCES, result.references);
        }
    }

    private static class ReplyHeadersResult {

        final String inReplyTo;
        final String references;

        ReplyHeadersResult(String inReplyTo, String references) {
            super();
            this.inReplyTo = inReplyTo;
            this.references = references;
        }
    }

    /** Don't set any reply headers */
    private static final ReplyHeadersResult NO_REPLY_HEADERS_RESULT = new ReplyHeadersResult(null, null);

    private static final String HDR_MESSAGE_ID = MessageHeaders.HDR_MESSAGE_ID;
    private static final String HDR_REFERENCES = MessageHeaders.HDR_REFERENCES;
    private static final String HDR_IN_REPLY_TO = MessageHeaders.HDR_IN_REPLY_TO;

    private static ReplyHeadersResult determineReplyHeaders(MailMessage referencedMail, Integer... maxReferencesLength) {
        if (null == referencedMail) {
            /*
             * Obviously referenced mail does no more exist; cancel setting reply headers Message-Id, In-Reply-To, and References.
             */
            return NO_REPLY_HEADERS_RESULT;
        }

        // Determine "In-Reply-To" header
        String inReplyTo = null;
        String pMsgId = referencedMail.getFirstHeader(HDR_MESSAGE_ID);
        if (pMsgId != null) {
            inReplyTo = pMsgId;
        }

        // Determine "References" header
        String references = null;
        {
            String pReferences = referencedMail.getFirstHeader(HDR_REFERENCES);
            String pInReplyTo;
            StringBuilder refBuilder = new StringBuilder();
            if (pReferences != null) {
                /*
                 * The "References:" field will contain the contents of the parent's "References:" field (if any) followed by the contents of
                 * the parent's "Message-ID:" field (if any).
                 */
                refBuilder.append(pReferences);
            } else if ((pInReplyTo = referencedMail.getFirstHeader(HDR_IN_REPLY_TO)) != null) {
                /*
                 * If the parent message does not contain a "References:" field but does have an "In-Reply-To:" field containing a single
                 * message identifier, then the "References:" field will contain the contents of the parent's "In-Reply-To:" field followed by
                 * the contents of the parent's "Message-ID:" field (if any).
                 */
                refBuilder.append(pInReplyTo);
            }
            if (pMsgId != null) {
                if (refBuilder.length() > 0) {
                    refBuilder.append(' ');
                }
                refBuilder.append(pMsgId);
            }
            if (refBuilder.length() > 0) {
                /*
                 * If the parent has none of the "References:", "In-Reply-To:", or "Message-ID:" fields, then the new message will have no
                 * "References:" field.
                 */
                int maxLen = getMaxReferencesLength(maxReferencesLength);
                references = MimeMessageUtility.fold(12, maxLen > 0 ? ensureMaxReferencesLength(refBuilder.toString(), maxLen) : refBuilder.toString());
            }
        }

        return inReplyTo == null && references == null ? NO_REPLY_HEADERS_RESULT : new ReplyHeadersResult(inReplyTo, references);
    }

    private static int getMaxReferencesLength(Integer... maxHeaderLength) {
        if (maxHeaderLength == null || maxHeaderLength.length <= 0 || maxHeaderLength[0] == null) {
            return 0;
        }

        int retval = maxHeaderLength[0].intValue();
        return retval <= 0 ? 0 : retval;
    }

    private static String ensureMaxReferencesLength(String value, int maxReferencesLength) {
        if (value == null || maxReferencesLength <= 0) {
            return value;
        }

        int nameLen = 12; // "References" length plus tow extra characters for delimiter ": "
        String newValue = Strings.dropCRLFFrom(value);
        if ((nameLen + newValue.length()) <= maxReferencesLength) {
            return value;
        }

        do {
            int pos = newValue.indexOf(' ');
            if (pos < 0) {
                // No further stripping possible
                return newValue;
            }
            newValue = newValue.substring(pos + 1);
        } while ((nameLen + newValue.length()) > maxReferencesLength);
        return newValue;
    }

}
