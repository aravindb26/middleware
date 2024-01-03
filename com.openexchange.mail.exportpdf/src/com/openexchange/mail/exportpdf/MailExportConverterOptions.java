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

package com.openexchange.mail.exportpdf;

import com.openexchange.session.Session;

/**
 * {@link MailExportConverterOptions} - Defines internal mail conversion options
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface MailExportConverterOptions {

    /**
     * Returns the user's session
     * 
     * @return the session
     */
    Session getSession();

    /**
     * Returns the amount of lines to reserve for the mail headers
     * 
     * @return the amount of lines to reserve for the mail headers
     */
    int getMailHeaderLines();

    /**
     * Returns the header page count
     * 
     * @return The header page count
     */
    int getHeaderPageCount();

    /**
     * Returns the amount of space (in pixels) to reserve for the mail headers.
     * This already includes the pre-configured topMargin
     * 
     * @return the amount of space (in pixels) to reserve for the mail headers.
     */
    float getTopMarginOffset();

    /**
     * Returns the desired top margin for the page in pixels
     * 
     * @return the desired top margin for the page
     */
    float getTopMargin();

    /**
     * Returns the desired bottom margin for the page in pixels
     * 
     * @return the desired bottom margin for the page
     */
    float getBottomMargin();

    /**
     * Returns the desired left margin for the page in pixels
     * 
     * @return the desired left margin for the page
     */
    float getLeftMargin();

    /**
     * Returns the desired right margin for the page in pixels
     * 
     * @return the desired right margin for the page
     */
    float getRightMargin();

    /**
     * Returns the desired width of the document's page in pixels
     * 
     * @return the width of the document's page in pixels
     */
    float getWidth();

    /**
     * Returns the desired height of the document's page in pixels
     * 
     * @return the height of the document's page in pixels
     */
    float getHeight();
}
