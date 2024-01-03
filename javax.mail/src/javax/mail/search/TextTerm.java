/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package javax.mail.search;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import com.openexchange.java.Strings;

/**
 * This class implements comparisons for the message TEXT.
 * The comparison is case-insensitive.  The pattern is a simple string
 * that must appear as a substring in the TEXT.
 *
 * @author Thorben Betten
 */
public final class TextTerm extends StringTerm {

    private static final long serialVersionUID = 8581568618055573432L;

    /**
     * Used to read content from a part.
     */
    public static interface PartReader {

        /**
         * Reads the content from given part.
         *
         * @param p The part to read
         * @param charset The character encoding
         * @return The part's content
         * @throws MessagingException If content cannot be read
         */
        String readPart(Part p, String charset) throws MessagingException;

        /**
         * Attempts to get the multipart content from given part.
         *
         * @param part The part
         * @return The multipart content or <code>null</code>
         * @throws MessagingException If a messaging error occurs
         * @throws IOException If an I/O error occurs
         */
        Multipart multipartFrom(Part part) throws MessagingException, IOException;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final PartReader partReader;

    /**
     * Constructor.
     *
     * @param pattern  the pattern to search for
     */
    public TextTerm(String pattern, PartReader partReader) {
	// Note: comparison is case-insensitive
	super(pattern);
    this.partReader = partReader;
    }

    /**
     * The match method.
     *
     * @param msg	the pattern match is applied to this Message's
     *			subject header
     * @return		true if the pattern match succeeds, otherwise false
     */
    @Override
    public boolean match(Message msg) {
	try {
	    return lookUp(msg);
	} catch (Exception e) {
	    return false;
	}
    }

    /**
     * Equality comparison.
     */
    @Override
    public boolean equals(Object obj) {
	if (!(obj instanceof TextTerm)) {
        return false;
    }
	return super.equals(obj);
    }

    private boolean lookUp(Part part) throws IOException, MessagingException {
        for (Enumeration<Header> e = part.getAllHeaders(); e.hasMoreElements();) {
            Header header = e.nextElement();
            if (super.match(header.getName())) {
                return true;
            }
            if (super.match(header.getValue())) {
                return true;
            }
        }

        String contentType = part.getContentType();
        contentType = Strings.isEmpty(contentType) ? "text/plain; charset=US-ASCII" : Strings.asciiLowerCase(contentType.trim());

        ContentType ct = new ContentType(contentType);
        if (ct.match("multipart/*")) {
            Multipart mp = null;

            Object content = part.getContent();
            if (content instanceof Multipart) {
                mp = (Multipart) content;
            } else {
                if (content instanceof InputStream) {
                    ((InputStream) content).close();
                }
                mp = partReader.multipartFrom(part);
            }

            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = mp.getBodyPart(i);
                if (lookUp(bodyPart)) {
                    return true;
                }
            }
        } else if (ct.match("message/rfc822")) {
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
        } else if (ct.match("text/*") || ct.match("message/*") || ct.match("application/json") || ct.match("application/xml") || ct.match("application/xhtml+xml") || ct.match("application/pgp-signature")) {
            if (textMatch(part, ct)) {
                return true;
            }
        }
        return false;
    }

    private boolean textMatch(Part part, ContentType ct) throws MessagingException {
        String charset = ct.getParameter("charset");
        charset = Strings.isEmpty(charset) ? "ISO-8859-1" : charset;
        return super.match(partReader.readPart(part, charset));
    }

}
