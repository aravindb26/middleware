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

package com.openexchange.chronos.alarm.mail.notification;

import java.util.Locale;
import java.util.TimeZone;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.user.User;

/**
 * {@link NotificationParticipant}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class NotificationParticipant implements Cloneable {

    private boolean external;
    private String email;
    private int identifier = -1;
    private String displayName;
    private ParticipationStatus confirmStatus = ParticipationStatus.NEEDS_ACTION;
    private String comment;
    private User user;
    private Context ctx;
    private Locale locale;
    private boolean resource;
    private TimeZone tz;
    private String folderId;

    public NotificationParticipant(boolean external, String email) {
        this(external, email, -1);
    }

    public NotificationParticipant(boolean external, String email, int identifier) {
        super();
        this.external = external;
        this.email = email;
        this.identifier = identifier;
    }

    public String getDisplayName() {
        if (displayName == null) {
            return email;
        }
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ParticipationStatus getConfirmStatus() {
        return confirmStatus;
    }

    public void setConfirmStatus(ParticipationStatus confirmStatus) {
        this.confirmStatus = confirmStatus;
    }

    public int getIdentifier() {
        return identifier;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setContext(Context ctx) {
        this.ctx = ctx;
    }

    public Context getContext() {
        return ctx;
    }

    public Locale getLocale() {
        if (locale == null) {
            return LocaleTools.DEFAULT_LOCALE;
        }
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setResource(boolean b) {
        this.resource = b;
    }

    public boolean isResource() {
        return resource;
    }

    public TimeZone getTimeZone() {
        if (tz == null) {
            return TimeZone.getDefault();
        }
        return tz;
    }

    public void setTimezone(TimeZone tz) {
        this.tz = tz;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    @Override
    public NotificationParticipant clone() {
        NotificationParticipant clone = new NotificationParticipant(external, comment);
        clone.external = this.external;
        clone.email = this.email;
        clone.identifier = this.identifier;
        clone.displayName = this.displayName;
        clone.confirmStatus = this.confirmStatus;
        clone.comment = this.comment;
        clone.user = this.user;
        clone.ctx = this.ctx;
        clone.locale = this.locale;
        clone.resource = this.resource;
        clone.tz = this.tz;
        clone.folderId = this.folderId;
        return clone;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        return result;
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
        NotificationParticipant other = (NotificationParticipant) obj;
        if (email == null) {
            if (other.email != null) {
                return false;
            }
        } else if (!email.equals(other.email)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NotificationParticipant [external=" + external + ", email=" + email + ", identifier=" + identifier + ", displayName=" + displayName + "]";
    }

}
