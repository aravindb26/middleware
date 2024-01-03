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

package com.openexchange.share.recipient;

import java.util.Date;

/**
 * {@link AnonymousRecipient}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.0
 */
public class AnonymousRecipient extends ShareRecipient {

    private static final long serialVersionUID = -6939532786908091158L;

    private boolean containsPassword;
    private boolean containsExpiryDate;
    private boolean containsIncludeSubfolders;
    private String password;
    private Date expiryDate;
    private boolean includeSubfolders;

    /**
     * Initializes a new {@link AnonymousRecipient}.
     */
    public AnonymousRecipient() {
        super();
    }

    /**
     * Initializes a new {@link AnonymousRecipient}.
     *
     * @param bits The permission bits to set
     * @param password The password to set, or <code>null</code> for no password
     * @param expiryDate The expiration date, or <code>null</code> if not set
     */
    public AnonymousRecipient(int bits, String password, Date expiryDate, boolean includeSubfolders) {
        this();
        setBits(bits);
        setPassword(password);
        setExpiryDate(expiryDate);
        setIncludeSubfolders(includeSubfolders);
    }

    @Override
    public RecipientType getType() {
        return RecipientType.ANONYMOUS;
    }

    /**
     * Gets the password
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password
     *
     * @param password The password to set
     */
    public void setPassword(String password) {
        this.password = password;
        this.containsPassword = true;
    }

    /**
     * Gets the expiration date of the anonymous guest.
     *
     * @return The expiration date, or <code>null</code> if not set
     */
    public Date getExpiryDate() {
        return expiryDate;
    }

    /**
     * Sets the expiration date for the anonymous guest.
     *
     * @param expiryDate The expiration date, or <code>null</code> if not set
     */
    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
        this.containsExpiryDate = true;
    }

    /**
     * Gets the includeSubfolders flag of the anonymous guest.
     *
     * @return The includeSubfolders flag
     */
    public boolean getIncludeSubfolders() {
        return includeSubfolders;
    }

    /**
     * Sets the includeSubfolders flag for the anonymous guest.
     *
     * @param includeSubfolders Whether sub-folders should be included or not
     */
    public void setIncludeSubfolders(boolean includeSubfolders) {
        this.includeSubfolders = includeSubfolders;
        this.containsIncludeSubfolders = true;
    }

    /**
     * Gets a value whether the recipient contains a expiration date or not.
     *
     * @return <code>true</code> if the expiration date is set, <code>false</code>, otherwise
     */
    public boolean containsExpiryDate() {
        return containsExpiryDate;
    }

    /**
     * Gets a value whether the recipient contains a password or not.
     *
     * @return <code>true</code> if the password is set, <code>false</code>, otherwise
     */
    public boolean containsPassword() {
        return containsPassword;
    }

    /**
     * Gets a value whether sub-folders should be included or not.
     *
     * @return <code>true</code> if sub-folders should be included, <code>false</code>, otherwise
     */
    public boolean containsIncludeSubfolders() {
        return containsIncludeSubfolders;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof AnonymousRecipient)) {
            return false;
        }
        AnonymousRecipient other = (AnonymousRecipient) obj;
        if (containsPassword != other.containsPassword()) {
            return false;
        } else if (password == null) {
            if (other.password != null) {
                return false;
            }
        } else if (!password.equals(other.password)) {
            return false;
        }
        if (containsExpiryDate != other.containsExpiryDate()) {
            return false;
        } else if (expiryDate == null) {
            if (other.expiryDate != null) {
                return false;
            }
        } else if (!expiryDate.equals(other.expiryDate)) {
            return false;
        }
        if (includeSubfolders != other.includeSubfolders) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AnonymousRecipient [type=" + getType() + ", bits=" + getBits() + "]";
    }

}
