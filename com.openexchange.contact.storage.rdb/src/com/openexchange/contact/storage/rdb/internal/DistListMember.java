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

package com.openexchange.contact.storage.rdb.internal;

import static java.util.Arrays.stream;
import java.util.UUID;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.DistributionListEntryObject;

/**
 * {@link DistListMember} -
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class DistListMember extends DistributionListEntryObject {

    private static final long serialVersionUID = 3887003030363062903L;

    /**
     * Creates an array of distribution list members using the supplied data.
     *
     * @param distList an array of distribution list entry objects
     * @param contextID the context ID
     * @param parentContactID the ID of the corresponding contact
     * @return the distribution list members
     * @throws OXException If email address of an distribution list member is invalid and verification is enabled
     */
    public static DistListMember[] create(final DistributionListEntryObject[] distList, final int contextID, final int parentContactID) throws OXException {
        if (null != distList && distList.length >= 1) {
            final DistListMember[] members = new DistListMember[distList.length];
            for (int i = 0; i < members.length; i++) {
                members[i] = DistListMember.create(distList[i], contextID, parentContactID);
            }
            return deduplicate(members);
        }
        return null;
    }

    /**
     * De-duplicates distribution list members in case of equal data
     * 
     * @param members The members of the list
     * @return Unique members for a distribution list
     * @throws OXException In case of error
     */
    private static DistListMember[] deduplicate(DistListMember[] members) {
        if (null == members || members.length <= 0) {
            return null;
        }
        if (members.length == 1) {
            return members;
        }
        return stream(members).map(m -> new CompareableDistListMember(m)) //Wrap to compare
            .distinct() // Reduce elements
            .map(m -> m.delegatee).toArray(size -> new DistListMember[size]); // Unwrap an return result
    }

    /**
     * Creates a {@link DistListMember}
     *
     * @param dleo The {@link DistributionListEntryObject} to create the member from
     * @param contextID The context identifier
     * @param parentContactID The parent contact identifier the member belongs to
     * @return A {@link DistListMember}
     * @throws OXException If specified email address is invalid and verification is enabled
     */
    public static DistListMember create(final DistributionListEntryObject dleo, final int contextID, final int parentContactID) throws OXException {
        final DistListMember member = new DistListMember();
        member.setParentContactID(parentContactID);
        member.setContextID(contextID);
        if (dleo.containsDisplayname()) {
            member.setDisplayname(dleo.getDisplayname());
        }
        if (dleo.containsEmailaddress()) {
            member.setEmailaddress(dleo.getEmailaddress(), false);
        }
        if (dleo.containsEmailfield()) {
            member.setEmailfield(dleo.getEmailfield());
        }
        if (dleo.containsEntryID()) {
            member.setEntryID(dleo.getEntryID());
        }
        if (dleo.containsFistname()) {
            member.setFirstname(dleo.getFirstname());
        }
        if (dleo.containsFolderld()) {
            member.setFolderID(dleo.getFolderID());
        }
        if (dleo.containsLastname()) {
            member.setLastname(dleo.getLastname());
        }
        if (dleo.containsContactUid()) {
            member.setContactUid(dleo.getContactUid());
        }

        return member;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    private int parentContactID;
    private boolean b_parentContactID;

    private int contextID;
    private boolean b_contextID;

    private UUID uuid;
    private boolean b_uuid;

    /**
     * Initializes a new {@link DistListMember}.
     */
    public DistListMember() {
        super();
    }

    /**
     * @return the parentContactID
     */
    public int getParentContactID() {
        return parentContactID;
    }

    /**
     * @param parentContactID the parentContactID to set
     */
    public void setParentContactID(int parentContactID) {
        this.b_parentContactID = true;
        this.parentContactID = parentContactID;
    }

    /**
     * Removes the parent contact identifier
     *
     */
    public void removeParentContactID() {
        parentContactID = 0;
        b_parentContactID = false;
    }

    /**
     * @return the contextID
     */
    public int getContextID() {
        return contextID;
    }

    /**
     * @param contextID the contextID to set
     */
    public void setContextID(int contextID) {
        this.b_contextID = true;
        this.contextID = contextID;
    }

    /**
     * Removes the context identifier
     *
     */
    public void removeContextID() {
        contextID = 0;
        b_contextID = false;
    }

    /**
     * @return the b_contextID
     */
    public boolean containsContextID() {
        return b_contextID;
    }

    /**
     * @return the b_parentContactID
     */
    public boolean containsParentContactID() {
        return b_parentContactID;
    }

    /**
     * Set the UUID of the member
     *
     * @param uuid The UUID to set
     */
    public void setUuid(UUID uuid) {
        this.b_uuid = true;
        this.uuid = uuid;
    }

    /**
     * Removes the UUID of the member
     *
     */
    public void removeUuid() {
        this.uuid = null;
        this.b_uuid = false;
    }

    /**
     * Gets a value indicating whether the UUID is set or not
     *
     * @return <code>true</code> if the UUID is set
     */
    public boolean containsUuid() {
        return b_uuid;
    }

    /**
     * Get the UUID of the member
     *
     * @return THe UUID
     */
    public UUID getUuid() {
        return uuid;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    /**
     * Simple class to compare two distribution list members for deduplication without breaking equals method of super class.
     * Basically two different cases are considered:
     * <li> In case of {@link DistributionListEntryObject#INDEPENDENT} contacts, check if the mail address is unique
     * <li> In case for known contacts, check that the mail field, from which the mail address for the distribution list comes from, is unique
     */
    @SuppressWarnings("serial")
    private static class CompareableDistListMember extends DistListMember {

        private DistListMember delegatee;

        private CompareableDistListMember(DistListMember m) {
            super();
            this.delegatee = m;
        }

        @Override
        public boolean equals(Object o) {
            if (false == o instanceof CompareableDistListMember) {
                return false;
            }
            DistListMember other = ((CompareableDistListMember) o).delegatee;
            /*
             * Check if the member is an independent entry for this distribution list
             */
            if (DistributionListEntryObject.INDEPENDENT == delegatee.getEmailfield()) {
                if (DistributionListEntryObject.INDEPENDENT == other.getEmailfield()) {
                    // At this point mail addresses are set
                    return delegatee.getEmailaddress().equals(other.getEmailaddress());
                }
                return false;
            }
            /*
             * Not-independent entries, compare by ID and mail field
             */
            if (delegatee.getEntryID().equals(other.getEntryID())) {
                return delegatee.getEmailfield() == other.getEmailfield();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return delegatee.hashCode();
        }

    }

}
