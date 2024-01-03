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

package com.sun.mail.imap.protocol;

import com.sun.mail.iap.ParsingException;

/**
 * The PREVIEW fetch response item.
 *
 * @author  Thorben Betten
 */

public class PREVIEW implements Item {

    static final char[] name = {'P','R','E','V','I','E','W'};

    private final String text;
    private final String algorithm;

    /**
     * Constructor
     *
     * @param	r	the FetchResponse
     * @exception	ParsingException	for parsing failures
     */
    public PREVIEW(FetchResponse r, boolean rfc8970Preview) throws ParsingException {
	r.skipSpaces();
	
	if (rfc8970Preview) {
        // Expect: ``PREVIEW "Some text"''
        algorithm = null;
        String text = r.readUtf8AtomString();
        if (text.startsWith("NIL")) {
            // Preview is empty
            int unread = text.length() - 3;
            if (unread > 0) {
                r.unreadAtom(unread);
            }
            text = null;
        }
        this.text = text;
    } else {        
        byte b = r.peekByte();
        if (b == '(') {
            // Expect: ``PREVIEW (FUZZY "Some text")''
            r.readByte();
            algorithm = r.readString(' ');
            if (r.readByte() != ' ') {
                throw new ParsingException("PREVIEW parse error: missing space at algorithm end");
            }
            
            String text = r.readUtf8AtomString();
            if ("NIL".equalsIgnoreCase(text)) {
                // Preview is empty
                text = "";
            }
            this.text = text;
            
            if (!r.isNextNonSpace(')')) // eat the end ')'
                throw new ParsingException(
                    "PREVIEW parse error: missing ``)'' at end");
        } else {
            // Expect: ``PREVIEW FUZZY "Some text"''
            algorithm = r.readString(' ');
            if (r.readByte() != ' ') {
                throw new ParsingException("PREVIEW parse error: missing space at algorithm end");
            }
            
            String text = r.readUtf8AtomString();
            if ("NIL".equalsIgnoreCase(text)) {
                // Preview is empty
                text = "";
            }
            this.text = text;
        }
    }
    }

    /**
     * Gets the preview's text; either
     * <ul>
     * <li>The preview text,</li>
     * <li>An empty string if message's body is empty or</li>
     * <li><code>null</code> if preview text is not available</li>
     * </ul>
     *
     * @return The text
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the algorithm; e.g. <code>"FUZZY"</code>
     *
     * @return The algorithm or <code>null</code>
     */
    public String getAlgorithm() {
        return algorithm;
    }
}