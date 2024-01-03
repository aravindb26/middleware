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

package com.openexchange.mail.exportpdf.converter;

import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.session.Session;

/**
 * {@link DefaultMailExportConverterOptions} - Default implementation of the mail export converter options
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class DefaultMailExportConverterOptions implements MailExportConverterOptions {

    private Session session;
    private int mailHeaderLines;
    private int headerPageCount = 1;
    private float width;
    private float height;
    private float topMarginOffset;
    private float topMargin;
    private float bottomMargin;
    private float leftMargin;
    private float rightMargin;

    /**
     * Initialises a new {@link DefaultMailExportConverterOptions}.
     */
    public DefaultMailExportConverterOptions() {
        super();
    }

    /**
     * Sets the session
     *
     * @param session the session
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Sets the mail header lines
     *
     * @param lines the lines to set
     */
    public void setMailHeaderLines(int lines) {
        this.mailHeaderLines = lines;
    }

    /**
     * Sets the header page count
     * 
     * @param headerPageCount the header page count to set
     */
    public void setHeaderPageCount(int headerPageCount) {
        this.headerPageCount = headerPageCount;
    }

    /**
     * Sets the top margin in pixels
     *
     * @param topMargin The top margin
     */
    public void setTopMargin(float topMargin) {
        this.topMargin = topMargin;
    }

    /**
     * Sets the width
     *
     * @param width The width to set
     */
    public void setWidth(float width) {
        this.width = width;
    }

    /**
     * Sets the height
     *
     * @param height The height to set
     */
    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * Sets the topMarginOffset
     *
     * @param topMarginOffset The topMarginOffset to set
     */
    public void setTopMarginOffset(float topMarginOffset) {
        this.topMarginOffset = topMarginOffset;
    }

    /**
     * Sets the bottomMargin
     *
     * @param bottomMargin The bottomMargin to set
     */
    public void setBottomMargin(float bottomMargin) {
        this.bottomMargin = bottomMargin;
    }

    /**
     * Sets the leftMargin
     *
     * @param leftMargin The leftMargin to set
     */
    public void setLeftMargin(float leftMargin) {
        this.leftMargin = leftMargin;
    }

    /**
     * Sets the rightMargin
     *
     * @param rightMargin The rightMargin to set
     */
    public void setRightMargin(float rightMargin) {
        this.rightMargin = rightMargin;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public int getMailHeaderLines() {
        return mailHeaderLines;
    }

    @Override
    public int getHeaderPageCount() {
        return headerPageCount;
    }

    @Override
    public float getTopMargin() {
        return topMargin;
    }

    @Override
    public float getTopMarginOffset() {
        return topMarginOffset;
    }

    @Override
    public float getBottomMargin() {
        return bottomMargin;
    }

    @Override
    public float getLeftMargin() {
        return leftMargin;
    }

    @Override
    public float getRightMargin() {
        return rightMargin;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }
}
