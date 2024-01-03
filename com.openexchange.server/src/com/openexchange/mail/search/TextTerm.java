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

package com.openexchange.mail.search;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailField;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mail.utils.MessageUtility;

/**
 * {@link TextTerm}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class TextTerm extends SearchTerm<String> {

    private static final long serialVersionUID = 1462060457742619720L;

    private final boolean ignoreCase;
    private final String text;
    private String lowerCaseAddr;

    /**
     * Initializes a new {@link TextTerm}
     */
    public TextTerm(String text) {
        super();
        this.text = text;
        this.ignoreCase = true;
    }

    private String getLowerCaseAddr() {
        String s = lowerCaseAddr;
        if (null == s) {
            s = Strings.asciiLowerCase(text);
            lowerCaseAddr = s;
        }
        return s;
    }

    @Override
    public void accept(SearchTermVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * @return The unicode representation of the subject
     */
    @Override
    public String getPattern() {
        return text;
    }

    @Override
    public void addMailField(Collection<MailField> col) {
        col.add(MailField.SUBJECT);
    }

    @Override
    public boolean matches(Message msg) throws OXException {
        try {
            return lookUp(msg);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(TextTerm.class).warn("Error during search.", e);
            return false;
        }
    }

    private boolean lookUp(Part part) throws IOException, MessagingException, OXException {
        for (Enumeration<Header> e = part.getAllHeaders(); e.hasMoreElements();) {
            Header header = e.nextElement();
            if (match(header.getName())) {
                return true;
            }
            if (match(header.getValue())) {
                return true;
            }
        }

        String contentType = part.getContentType();
        contentType = Strings.isEmpty(contentType) ? "text/plain; charset=US-ASCII" : Strings.asciiLowerCase(contentType.trim());

        ContentType ct = new ContentType(contentType);
        if (ct.startsWith("multipart/")) {
            Multipart mp = null;

            Object content = part.getContent();
            if (content instanceof Multipart) {
                mp = (Multipart) content;
            } else {
                if (content instanceof InputStream) {
                    ((InputStream) content).close();
                }
                mp = MimeMessageUtility.multipartFrom(part);
            }

            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = mp.getBodyPart(i);
                if (lookUp(bodyPart)) {
                    return true;
                }
            }
        } else if (ct.startsWith("message/rfc822")) {
            Object content = part.getContent();
            if (content instanceof Message) {
                if (lookUp((Message) content)) {
                    return true;
                }
            } else {
                if (textMatch(part, ct)) {
                    return true;
                }
            }
        } else if (ct.startsWith("text/") || ct.startsWith("message/") || ct.startsWith("application/json") || ct.startsWith("application/xml") || ct.startsWith("application/xhtml+xml") || ct.startsWith("application/pgp-signature")) {
            if (textMatch(part, ct)) {
                return true;
            }
        }
        return false;
    }

    private boolean textMatch(Part part, ContentType ct) throws MessagingException {
        return match(MessageUtility.readMimePart(part, ct, -1));
    }

    @Override
    public boolean matches(MailMessage mailMessage) {
        try {
            return lookUp(mailMessage);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(TextTerm.class).warn("Error during search.", e);
            return false;
        }
    }

    private boolean lookUp(MailPart part) throws IOException, OXException {
        for (Iterator<Map.Entry<String, String>> e = part.getHeaders().getAllHeaders(); e.hasNext();) {
            Map.Entry<String, String> header = e.next();
            if (match(header.getKey())) {
                return true;
            }
            if (match(header.getValue())) {
                return true;
            }
        }

        ContentType ct = part.getContentType();
        if (ct.startsWith("multipart/") && part.getEnclosedCount() != MailPart.NO_ENCLOSED_PARTS) {
            int count = part.getEnclosedCount();
            for (int i = 0; i < count; i++) {
                MailPart bodyPart = part.getEnclosedMailPart(i);
                if (lookUp(bodyPart)) {
                    return true;
                }
            }
        } else if (ct.startsWith("message/rfc822")) {
            Object content = part.getContent();
            if (content instanceof MailMessage) {
                if (lookUp((MailMessage) content)) {
                    return true;
                }
            } else {
                if (textMatch(part, ct)) {
                    return true;
                }
            }
        } else if (ct.startsWith("text/") || ct.startsWith("message/") || ct.startsWith("application/json") || ct.startsWith("application/xml") || ct.startsWith("application/xhtml+xml") || ct.startsWith("application/pgp-signature")) {
            if (textMatch(part, ct)) {
                return true;
            }
        }
        return false;
    }

    private boolean textMatch(MailPart part, ContentType ct) throws IOException, OXException {
        return match(MessageUtility.readMailPart(part, ct.getCharsetParameter()));
    }

    @Override
    public javax.mail.search.SearchTerm getJavaMailSearchTerm() {
        return new javax.mail.search.TextTerm(text, new PartReaderImpl());
    }

    @Override
    public javax.mail.search.SearchTerm getNonWildcardJavaMailSearchTerm() {
        return new javax.mail.search.TextTerm(getNonWildcardPart(text), new PartReaderImpl());
    }

    @Override
    public void contributeTo(FetchProfile fetchProfile) {
        // Cannot...
    }

    @Override
    public boolean isAscii() {
        return isAscii(text);
    }

    @Override
    public boolean containsWildcard() {
        return null == text ? false : text.indexOf('*') >= 0 || text.indexOf('?') >= 0;
    }

    private boolean match(String s) {
        int len = s.length() - text.length();
        for (int i = 0; i <= len; i++) {
            if (s.regionMatches(ignoreCase, i, text, 0, text.length())) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class PartReaderImpl implements javax.mail.search.TextTerm.PartReader {

        @Override
        public String readPart(Part p, String charset) throws MessagingException {
            return MessageUtility.readMimePart(p, charset, -1);
        }

        @Override
        public Multipart multipartFrom(Part part) throws MessagingException, IOException {
            return MimeMessageUtility.getMultipartContentFrom(part);
        }
    }

}
