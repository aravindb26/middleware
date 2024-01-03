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

package com.openexchange.chronos.alarm.message.impl;

/**
 * {@link Key} is a identifying key for a {@link SingleMessageDeliveryTask}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.1
 */
class Key implements Comparable<Key> {

    private final int cid;
    private final int account;
    private final int id;
    private final String eventId;
    private final int hash;

    /**
     * Initializes a new {@link Key}.
     *
     * @param cid The context identifier
     * @param account The account identifier
     * @param eventId The event identifier
     * @param id The alarm identifier
     */
    public Key(int cid, int account, String eventId, int id) {
        super();
        this.cid = cid;
        this.account = account;
        this.id = id;
        this.eventId = eventId;

        int prime = 31;
        int result = 1;
        result = prime * result + cid;
        result = prime * result + account;
        result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
        result = prime * result + id;
        this.hash = result;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Key other = (Key) obj;
        if (cid != other.cid) {
            return false;
        }
        if (account != other.account) {
            return false;
        }
        if (eventId == null) {
            if (other.eventId != null) {
                return false;
            }
        } else if (!eventId.equals(other.eventId)) {
            return false;
        }
        if (id != other.id) {
            return false;
        }
        return true;
    }

    /**
     * Gets the event identifier
     *
     * @return The event identifier
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the context identifier
     *
     * @return The context identifier
     */
    public int getCid() {
        return cid;
    }

    /**
     * Gets the account identifier
     *
     * @return The account identifier
     */
    public int getAccount() {
        return account;
    }

    @Override
    public String toString() {
        return "Key [cid=" + cid + "|account=" + account + "|eventId=" + eventId + "|alarmId=" + id + "]";
    }

    /**
     * Gets the alarm identifier
     *
     * @return The alarm identifier
     */
    public int getId() {
        return id;
    }

    @Override
    public int compareTo(Key o) {
        int c = Integer.compare(cid, o.cid);
        if (c == 0) {
            c = Integer.compare(account, o.account);
        }
        if (c == 0) {
            if (eventId == o.eventId) { // NOSONARLINT this intentionally uses == to allow for both null
                c = 0;
            } else if (eventId == null) {
                c = -1;
            } else if (o.eventId == null) {
                c = 1;
            } else {
                c = eventId.compareTo(o.eventId);
            }
        }
        if (c == 0) {
            c = Integer.compare(id, o.id);
        }
        return c;
    }
}