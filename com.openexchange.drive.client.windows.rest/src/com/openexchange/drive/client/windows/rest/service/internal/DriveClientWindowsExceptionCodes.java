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

package com.openexchange.drive.client.windows.rest.service.internal;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link DriveClientWindowsExceptionCodes}
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 */
public enum DriveClientWindowsExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * Branding file is missing the necessary entries
     */
    MISSING_PROPERTIES("Branding file is missing the necessary entries.", Category.CATEGORY_CONFIGURATION, 1, null),

    /**
     * Could not find kubernetes service for branding \"%1$s\. Please check if a service is up and running
     */
    KUBERNETES_MISSING_SERVICE("Could not find kubernetes service for branding \"%1$s\". Please check if a service is up and running.",
        Category.CATEGORY_SERVICE_DOWN, 2, null),

    /**
     * Could not fetch any kubernetes service running in namespace \"%1$s\ labeled with \"%2$s\ from kubernetes API. Please check configuration.
     */
    KUBERNETES_MISSING_SERVICES("Could not fetch any kubernetes service running in namespace \"%1$s\" labeled with \"%2$s\" from kubernetes api.",
        Category.CATEGORY_SERVICE_DOWN, 3, null),

    /**
     * Could not connect to kubernetes API. \"%1$s\
     */
    KUBERNETES_ERROR("Could not connect to kubernetes API. \"%1$s\"", Category.CATEGORY_ERROR, 4, null),

    /**
     * Could not execute request \"%1$s\. Host not reachable
     */
    SERVICE_REQUEST_FAILED("Could not execute request \"%1$s\"! Drive Client Service not reachable.", Category.CATEGORY_SERVICE_DOWN, 504, null),

    /**
     * Receive malformed download url \"%1$s\ from underlying service
     */
    INVALID_DOWNLOAD_URL("Receive malformed download url \"%1$s\" from underlying service.", Category.CATEGORY_WARNING, 6, null),

    /**
     * Could not retrieve a namespace. Please check configuration
     */
    MISSING_NAMESPACE("Could not retrieve a namespace. Please check configuration.", Category.CATEGORY_CONFIGURATION, 7, null),

    /**
     * Could not find cluster type \"%1$s\". Use one of \"%2$s\". Please check configuration
     */
    UNKNOWN_CLUSTER_TYPE("Could not find cluster type \"%1$s\". Use one of \"%2$s\". Please check configuration.", Category.CATEGORY_CONFIGURATION, 8, null),

    /**
     * The remote service responded with a client error: %1$s.
     */
    REMOTE_CLIENT_ERROR("The remote service responded with client error %1$s.", Category.CATEGORY_WARNING, 400, null),

    /**
     * The remote service responded with a server error: %1$s.
     */
    REMOTE_SERVER_ERROR("The remote service responded with a server error %1$s.", Category.CATEGORY_SERVICE_DOWN, 500, OXExceptionStrings.MESSAGE_RETRY),

    /**
     * The remote service is missing the requested manifest
     */
    MISSING_RESOURCE("The remote service could not deliver the requested manifest", Category.CATEGORY_WARNING, 11, null),

    /**
     * The remote service responded with unexpected status: %1$s.
     */
    UNEXPECTED_STATUS("The remote service responded with an unexpected status %1$s.", Category.CATEGORY_WARNING, 12, null),

    /**
     * Received malformed or invalid request data.
     */
    INVALID_REQUEST_DATA("Received malformed or invalid request data.", Category.CATEGORY_WARNING, 13, null),
    ;

    /**
     * The error code prefix.
     */
    public static final String PREFIX = "DRIVECLIENT";

    private final String message;

    private final int detailNumber;

    private final Category category;

    private final String displayMessage;

    private DriveClientWindowsExceptionCodes(String message, Category category, int number, String displayMessage) {
        this.message = message;
        detailNumber = number;
        this.category = category;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @return The newly created {@link OXException} instance
     */
    public OXException create() {
        return OXExceptionFactory.getInstance().create(this);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param args The message arguments in case of printf-style message
     *
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Object... args) {
        return OXExceptionFactory.getInstance().create(this, (Throwable) null, args);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param cause The optional initial cause
     * @param args The message arguments in case of printf-style message
     *
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Throwable cause, final Object... args) {
        return OXExceptionFactory.getInstance().create(this, cause, args);
    }

    @Override
    public boolean equals(final OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    @Override
    public String getDisplayMessage() {
        return displayMessage;
    }

    @Override
    public int getNumber() {
        return detailNumber;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
