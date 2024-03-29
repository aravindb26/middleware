/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

/**
 * This class implements comparisons for the a part's file name.
 * The comparison is case-insensitive.  The pattern is a simple string
 * that must appear as a substring in a part's file name.
 *
 * @author Thorben Betten
 */
public final class FileNameTerm extends StringTerm {

    private static final long serialVersionUID = 1337568618055573432L;

    private boolean fileNameSearchSupported;

    /**
     * Constructor.
     *
     * @param pattern  the pattern to search for
     */
    public FileNameTerm(String pattern) {
	// Note: comparison is case-insensitive
	super(pattern);
    }

    /**
     * Sets whether it is known if file name search is supported by target IMAP server.
     *
     * @param fileNameSearchSupported <code>true</code> if supported; otherwise <code>false</code>
     */
    public void setFileNameSearchSupported(boolean fileNameSearchSupported) {
        this.fileNameSearchSupported = fileNameSearchSupported;
    }

    /**
     * Checks if it is known that target IMAP server supports file name search.
     *
     * @return <code>true</code> if known to be supported; otherwise <code>false</code>
     */
    public boolean isFileNameSearchSupported() {
        return fileNameSearchSupported;
    }

    /**
     * The match method.
     *
     * @param msg	the pattern match is applied to this Message's parts
     * @return		<code>true</code> if the pattern match succeeds, otherwise <code>false</code>
     */
    @Override
    public boolean match(Message msg) {
        try {
            return checkPart(msg);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkPart(Part p) throws MessagingException, IOException {
        String contentType = p.getContentType();
        if (null != contentType) {
            contentType = com.openexchange.java.Strings.asciiLowerCase(contentType).trim();
            if (contentType.startsWith("multipart/")) {
                Multipart m = (Multipart) p.getContent();
                int count = m.getCount();
                for (int i = 0, k = count; k-- > 0; i++) {
                    BodyPart bodyPart = m.getBodyPart(i);
                    if (checkPart(bodyPart)) {
                        return true;
                    }
                }
            } else {
                String fileName = p.getFileName();

                if (null != fileName && super.match(fileName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Equality comparison.
     */
    @Override
    public boolean equals(Object obj) {
	if (!(obj instanceof FileNameTerm)) {
        return false;
    }
	return super.equals(obj);
    }

}
