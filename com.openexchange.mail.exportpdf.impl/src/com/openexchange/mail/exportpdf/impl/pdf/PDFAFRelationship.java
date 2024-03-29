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
package com.openexchange.mail.exportpdf.impl.pdf;

/**
 * {@link PDFAFRelationship} - Enumerates the type of relationship between embedded documents and the main PDF content, as required by PDF/A compliance
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public enum PDFAFRelationship {


    /**
     * The embedded file represents data which is used for visual presentation within the PDF (a table, a graph, etc)
     */
    Data,

    /**
     * The embedded file contains the source for creating the visual presentation within the PDF
     */
    Source,

    /**
     * The embedded file is an alternative representation of the PDF content (audio for example)
     */
    Alternative,

    /**
     * The embedded file contains additional information for easier processing
     */
    Supplement,

    /**
     * Relationship is unknown, or cannot be described by on of the other values
     */
    Unspecified

    ;

    public static final String AF_RELATIONSHIP_KEY_NAME = "AFRelationship";
}
