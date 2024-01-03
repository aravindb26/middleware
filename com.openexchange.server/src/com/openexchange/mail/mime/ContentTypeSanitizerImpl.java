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

import java.util.Iterator;
import javax.mail.internet.ContentTypeSanitizer;
import javax.mail.internet.ParseException;
import com.openexchange.exception.OXException;


/**
 * {@link ContentTypeSanitizerImpl} - The Content-Type sanitizer.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ContentTypeSanitizerImpl implements ContentTypeSanitizer {

    /**
     * Initializes a new {@link ContentTypeSanitizerImpl}.
     */
    public ContentTypeSanitizerImpl() {
        super();
    }

    @Override
    public String sanitizeContentType(String type, ParseException cause) throws ParseException {
        try {
            return new ContentType(type).toString(true);
        } catch (OXException e) {
            throw new ParseException(e.getMessage(), cause);
        }
    }
    
    @Override
    public javax.mail.internet.ContentType reparseContentType(String type, ParseException cause) throws ParseException {
        try {
            ContentType contentType = new ContentType(type);
            
            javax.mail.internet.ContentType javaMailContentType = new javax.mail.internet.ContentType(contentType.getPrimaryType(), contentType.getSubType(), null);
            for (Iterator<String> parameterNames = contentType.getParameterNames(); parameterNames.hasNext();) {
                String parameterName = parameterNames.next();
                String parameterValue = contentType.getParameter(parameterName, null);
                if (parameterValue != null) {
                    javaMailContentType.setParameter(parameterName, parameterValue);
                }
            }
            return javaMailContentType;
        } catch (OXException e) {
            throw new ParseException(e.getMessage(), cause);
        }
    }

}
