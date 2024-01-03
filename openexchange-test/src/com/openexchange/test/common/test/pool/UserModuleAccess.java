/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the OX Software GmbH group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2016-2020 OX Software GmbH
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.test.common.test.pool;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.lang.reflect.Field;

/**
 * {@link UserModuleAccess} a provisioning independent UserModuleAccess definition
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class UserModuleAccess implements Cloneable {

    private Boolean calendar;

    private Boolean contacts;

    private Boolean delegateTask;

    private Boolean editPublicFolders;

    private Boolean ical;

    private Boolean infostore;

    private Boolean readCreateSharedFolders;

    private Boolean Syncml;

    private Boolean Tasks;

    private Boolean Vcard;

    private Boolean Webdav;

    /** @deprecated */
    @Deprecated
    private Boolean WebdavXml;

    private Boolean Webmail;

    private Boolean EditGroup;

    private Boolean EditResource;

    private Boolean EditPassword;

    private Boolean CollectEmailAddresses;

    private Boolean MultipleMailAccounts;

    private Boolean Subscription;

    /**
     * @deprecated Publication has been removed with v7.10.2
     */
    @Deprecated
    private Boolean Publication;

    private Boolean ActiveSync;

    private Boolean USM;

    @Deprecated
    private Boolean OLOX20;

    private Boolean GlobalAddressBookDisabled;

    private Boolean PublicFolderEditable;

    private Boolean deniedPortal;

    /**
     * Creates a new instance of UserModuleAccess
     */
    public UserModuleAccess() {
        super();
    }

    /**
     * Creates a new instance of UserModuleAccess
     */
    private UserModuleAccess(Builder builder) {
        super();
        this.calendar = builder.calendar;
        this.contacts = builder.contacts;
        this.delegateTask = builder.delegateTask;
        this.editPublicFolders = builder.editPublicFolders;
        this.ical = builder.ical;
        this.infostore = builder.infostore;
        this.readCreateSharedFolders = builder.readCreateSharedFolders;
        this.Syncml = builder.Syncml;
        this.Tasks = builder.Tasks;
        this.Vcard = builder.Vcard;
        this.Webdav = builder.Webdav;
        this.WebdavXml = builder.WebdavXml;
        this.Webmail = builder.Webmail;
        this.EditGroup = builder.EditGroup;
        this.EditResource = builder.EditResource;
        this.EditPassword = builder.EditPassword;
        this.CollectEmailAddresses = builder.CollectEmailAddresses;
        this.MultipleMailAccounts = builder.MultipleMailAccounts;
        this.Subscription = builder.Subscription;
        this.Publication = builder.Publication;
        this.ActiveSync = builder.ActiveSync;
        this.USM = builder.USM;
        this.OLOX20 = builder.OLOX20;
        this.GlobalAddressBookDisabled = builder.GlobalAddressBookDisabled;
        this.PublicFolderEditable = builder.PublicFolderEditable;
        this.deniedPortal = builder.deniedPortal;
    }

    @Override
    public UserModuleAccess clone() {
        try {
            return (UserModuleAccess) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError("CloneNotSupportedException although Colenable is implemented");
        }
    }

    /**
     * Enable all modules
     */
    public void enableAll() {
        this.calendar = TRUE;
        this.contacts = TRUE;
        this.delegateTask = TRUE;
        this.editPublicFolders = TRUE;
        this.ical = TRUE;
        this.infostore = TRUE;
        this.readCreateSharedFolders = TRUE;
        this.Syncml = TRUE;
        this.Tasks = TRUE;
        this.Vcard = TRUE;
        this.Webdav = TRUE;
        this.WebdavXml = TRUE;
        this.Webmail = TRUE;
        this.EditGroup = TRUE;
        this.EditResource = TRUE;
        this.EditPassword = TRUE;
        this.CollectEmailAddresses = TRUE;
        this.MultipleMailAccounts = TRUE;
        this.Subscription = TRUE;
        this.Publication = TRUE;
        this.ActiveSync = TRUE;
        this.USM = TRUE;
        this.GlobalAddressBookDisabled = FALSE;
        this.PublicFolderEditable = TRUE;
        this.OLOX20 = TRUE;
    }

    /**
     * Disable all modules
     */
    public void disableAll() {
        this.calendar = FALSE;
        this.contacts = FALSE;
        this.delegateTask = FALSE;
        this.editPublicFolders = FALSE;
        this.ical = FALSE;
        this.infostore = FALSE;
        this.readCreateSharedFolders = FALSE;
        this.Syncml = FALSE;
        this.Tasks = FALSE;
        this.Vcard = FALSE;
        this.Webdav = FALSE;
        this.WebdavXml = FALSE;
        this.Webmail = FALSE;
        this.EditGroup = FALSE;
        this.EditResource = FALSE;
        this.EditPassword = FALSE;
        this.CollectEmailAddresses = FALSE;
        this.MultipleMailAccounts = FALSE;
        this.Subscription = FALSE;
        this.Publication = FALSE;
        this.ActiveSync = FALSE;
        this.USM = FALSE;
        this.GlobalAddressBookDisabled = TRUE;
        this.PublicFolderEditable = FALSE;
        this.OLOX20 = FALSE;
    }

    public Boolean getEditGroup() {
        return EditGroup;
    }

    public void setEditGroup(Boolean editGroup) {
        EditGroup = editGroup;
    }

    public Boolean getEditResource() {
        return EditResource;
    }

    public void setEditResource(Boolean editResource) {
        EditResource = editResource;
    }

    public Boolean getEditPassword() {
        return EditPassword;
    }

    public void setEditPassword(Boolean editPassword) {
        EditPassword = editPassword;
    }

    /**
     * Gets the collect-email-addresses access.
     *
     * @return The collect-email-addresses access
     */
    public Boolean isCollectEmailAddresses() {
        return CollectEmailAddresses;
    }

    /**
     * Sets the collect-email-addresses access.
     *
     * @param collectEmailAddresses The collect-email-addresses access to set
     */
    public void setCollectEmailAddresses(Boolean collectEmailAddresses) {
        CollectEmailAddresses = collectEmailAddresses;
    }

    /**
     * Gets the multiple-mail-accounts access.
     *
     * @return The multiple-mail-accounts access
     */
    public Boolean isMultipleMailAccounts() {
        return MultipleMailAccounts;
    }

    /**
     * Sets the multiple-mail-accounts access.
     *
     * @param multipleMailAccounts The multiple-mail-accounts access to set
     */
    public void setMultipleMailAccounts(Boolean multipleMailAccounts) {
        MultipleMailAccounts = multipleMailAccounts;
    }

    /**
     * Gets the subscription access.
     *
     * @return The subscription access
     */
    public Boolean isSubscription() {
        return Subscription;
    }

    /**
     * Sets the subscription access.
     *
     * @param subscription The subscription access to set
     */
    public void setSubscription(Boolean subscription) {
        Subscription = subscription;
    }

    /**
     * Gets the publication access.
     *
     * @return The publication
     * @deprecated with v7.10.2 publication has been removed
     */
    @Deprecated
    public Boolean isPublication() {
        return Publication;
    }

    /**
     * Sets the publication access.
     *
     * @param publication The publication access to set
     * @deprecated with v7.10.2 publication has been removed
     */
    @Deprecated
    public void setPublication(Boolean publication) {
        Publication = publication;
    }

    /**
     * Shows if a user has access to the calendar module of ox.
     *
     * @return Returns <CODE>true</CODE> if user has access to calendar module
     *         or <CODE>false</CODE> if he has now access!
     */
    public Boolean getCalendar() {
        return calendar;
    }

    /**
     * Defines if a user has access to the calendar module of ox.
     *
     * @param val
     *            Set to <CODE>true</CODE> if user should be able to access
     *            the calendar module!
     */
    public void setCalendar(Boolean val) {
        this.calendar = val;
    }

    /**
     * Shows if a user has access to the contact module of ox.
     *
     * @return Returns <CODE>true</CODE> if user has access to contact module
     *         or <CODE>false</CODE> if he has now access!
     */
    public Boolean getContacts() {
        return contacts;
    }

    /**
     * Defines if a user has access to the contact module of ox.
     *
     * @param val
     *            Set to <CODE>true</CODE> if user should be able to access
     *            the contact module!
     */
    public void setContacts(Boolean val) {
        this.contacts = val;
    }

    /**
     * Shows if a user has the right to delegate tasks in the ox groupware.
     *
     * @return Returns <CODE>true</CODE> if user has the right to delegate
     *         tasks in the ox groupware. Or <CODE>false</CODE> if he has no
     *         right to delegate tasks!
     */
    public Boolean getDelegateTask() {
        return delegateTask;
    }

    /**
     * Defines if a user has the right to delegate tasks in the ox groupware.
     *
     * @param val
     *            Set to <CODE>true</CODE> if user should be able to delegate
     *            tasks in the ox groupware.
     */
    public void setDelegateTask(Boolean val) {
        this.delegateTask = val;
    }

    public Boolean getEditPublicFolders() {
        return editPublicFolders;
    }

    public void setEditPublicFolders(Boolean val) {
        this.editPublicFolders = val;
    }

    public Boolean getIcal() {
        return ical;
    }

    public void setIcal(Boolean val) {
        this.ical = val;
    }

    public Boolean getInfostore() {
        return infostore;
    }

    public void setInfostore(Boolean val) {
        this.infostore = val;
    }

    public Boolean getReadCreateSharedFolders() {
        return readCreateSharedFolders;
    }

    public void setReadCreateSharedFolders(Boolean val) {
        this.readCreateSharedFolders = val;
    }

    public Boolean getSyncml() {
        return Syncml;
    }

    public void setSyncml(Boolean val) {
        this.Syncml = val;
    }

    public Boolean getTasks() {
        return Tasks;
    }

    public void setTasks(Boolean val) {
        this.Tasks = val;
    }

    public Boolean getVcard() {
        return Vcard;
    }

    public void setVcard(Boolean val) {
        this.Vcard = val;
    }

    public Boolean getWebdav() {
        return Webdav;
    }

    public void setWebdav(Boolean val) {
        this.Webdav = val;
    }

    @Deprecated
    public Boolean getWebdavXml() {
        return WebdavXml;
    }

    @Deprecated
    public void setWebdavXml(Boolean val) {
        this.WebdavXml = val;
    }

    public Boolean getWebmail() {
        return Webmail;
    }

    public void setWebmail(Boolean val) {
        this.Webmail = val;
    }

    public Boolean isActiveSync() {
        return ActiveSync;
    }

    public void setActiveSync(Boolean activeSync) {
        this.ActiveSync = activeSync;
    }

    public Boolean isUSM() {
        return USM;
    }

    public void setUSM(Boolean val) {
        this.USM = val;
    }

    @Deprecated
    public Boolean isOLOX20() {
        return OLOX20;
    }

    @Deprecated
    public void setOLOX20(Boolean val) {
        this.OLOX20 = val;
    }

    public void setDeniedPortal(Boolean val) {
        this.deniedPortal = val;
    }

    public Boolean isDeniedPortal() {
        return deniedPortal;
    }

    public Boolean isGlobalAddressBookDisabled() {
        return GlobalAddressBookDisabled;
    }

    public void setGlobalAddressBookDisabled(Boolean val) {
        this.GlobalAddressBookDisabled = val;
    }

    public Boolean isPublicFolderEditable() {
        return PublicFolderEditable;
    }

    public void setPublicFolderEditable(Boolean publicFolderEditable) {
        this.PublicFolderEditable = publicFolderEditable;
    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder();
        ret.append("[ \n");
        for (final Field f : this.getClass().getDeclaredFields()) {
            try {
                final Object ob = f.get(this);
                final String tname = f.getName();
                if (ob != null && !tname.equals("serialVersionUID")) {
                    ret.append("  ");
                    ret.append(tname);
                    ret.append(": ");
                    ret.append(ob);
                    ret.append("\n");
                }
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                ret.append("IllegalArgument\n");
            } catch (@SuppressWarnings("unused") IllegalAccessException e) {
                ret.append("IllegalAccessException\n");
            }
        }
        ret.append(']');
        return ret.toString();
    }

    /*
     * ============================== Builder ==============================
     */

    /**
     * {@link Builder} - Builder pattern for user module access
     *
     * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
     * @since v8.0.0
     */
    public static class Builder {

        private Boolean calendar;
        private Boolean contacts;
        private Boolean delegateTask;
        private Boolean editPublicFolders;
        private Boolean ical;
        private Boolean infostore;
        private Boolean readCreateSharedFolders;
        private Boolean Syncml;
        private Boolean Tasks;
        private Boolean Vcard;
        private Boolean Webdav;
        private Boolean WebdavXml;
        private Boolean Webmail;
        private Boolean EditGroup;
        private Boolean EditResource;
        private Boolean EditPassword;
        private Boolean CollectEmailAddresses;
        private Boolean MultipleMailAccounts;
        private Boolean Subscription;
        private Boolean Publication;
        private Boolean ActiveSync;
        private Boolean USM;
        private Boolean OLOX20;
        private Boolean GlobalAddressBookDisabled;
        private Boolean PublicFolderEditable;
        private Boolean deniedPortal;

        private Builder() {}

        /**
         * Get a new builder instance
         *
         * @return The builder instance
         */
        public static Builder newInstance() {
            return new Builder();
        }

        Builder(Boolean calendar, Boolean contacts, Boolean delegateTask, Boolean editPublicFolders, Boolean ical, Boolean infostore, Boolean readCreateSharedFolders, Boolean Syncml, Boolean Tasks, Boolean Vcard, Boolean Webdav, Boolean WebdavXml, Boolean Webmail, Boolean EditGroup, Boolean EditResource, Boolean EditPassword, Boolean CollectEmailAddresses, Boolean MultipleMailAccounts, Boolean Subscription, Boolean Publication, Boolean ActiveSync, Boolean USM, Boolean OLOX20, Boolean GlobalAddressBookDisabled, Boolean PublicFolderEditable, Boolean deniedPortal) {
            this.calendar = calendar;
            this.contacts = contacts;
            this.delegateTask = delegateTask;
            this.editPublicFolders = editPublicFolders;
            this.ical = ical;
            this.infostore = infostore;
            this.readCreateSharedFolders = readCreateSharedFolders;
            this.Syncml = Syncml;
            this.Tasks = Tasks;
            this.Vcard = Vcard;
            this.Webdav = Webdav;
            this.WebdavXml = WebdavXml;
            this.Webmail = Webmail;
            this.EditGroup = EditGroup;
            this.EditResource = EditResource;
            this.EditPassword = EditPassword;
            this.CollectEmailAddresses = CollectEmailAddresses;
            this.MultipleMailAccounts = MultipleMailAccounts;
            this.Subscription = Subscription;
            this.Publication = Publication;
            this.ActiveSync = ActiveSync;
            this.USM = USM;
            this.OLOX20 = OLOX20;
            this.GlobalAddressBookDisabled = GlobalAddressBookDisabled;
            this.PublicFolderEditable = PublicFolderEditable;
            this.deniedPortal = deniedPortal;
        }

        public Builder calendar(Boolean calendar) {
            this.calendar = calendar;
            return this;
        }

        public Builder contacts(Boolean contacts) {
            this.contacts = contacts;
            return this;
        }

        public Builder delegateTask(Boolean delegateTask) {
            this.delegateTask = delegateTask;
            return this;
        }

        public Builder editPublicFolders(Boolean editPublicFolders) {
            this.editPublicFolders = editPublicFolders;
            return this;
        }

        public Builder ical(Boolean ical) {
            this.ical = ical;
            return this;
        }

        public Builder infostore(Boolean infostore) {
            this.infostore = infostore;
            return this;
        }

        public Builder readCreateSharedFolders(Boolean readCreateSharedFolders) {
            this.readCreateSharedFolders = readCreateSharedFolders;
            return this;
        }

        public Builder Syncml(Boolean Syncml) {
            this.Syncml = Syncml;
            return this;
        }

        public Builder Tasks(Boolean Tasks) {
            this.Tasks = Tasks;
            return this;
        }

        public Builder Vcard(Boolean Vcard) {
            this.Vcard = Vcard;
            return this;
        }

        public Builder Webdav(Boolean Webdav) {
            this.Webdav = Webdav;
            return this;
        }

        public Builder WebdavXml(Boolean WebdavXml) {
            this.WebdavXml = WebdavXml;
            return this;
        }

        public Builder Webmail(Boolean Webmail) {
            this.Webmail = Webmail;
            return this;
        }

        public Builder EditGroup(Boolean EditGroup) {
            this.EditGroup = EditGroup;
            return this;
        }

        public Builder EditResource(Boolean EditResource) {
            this.EditResource = EditResource;
            return this;
        }

        public Builder EditPassword(Boolean EditPassword) {
            this.EditPassword = EditPassword;
            return this;
        }

        public Builder CollectEmailAddresses(Boolean CollectEmailAddresses) {
            this.CollectEmailAddresses = CollectEmailAddresses;
            return this;
        }

        public Builder MultipleMailAccounts(Boolean MultipleMailAccounts) {
            this.MultipleMailAccounts = MultipleMailAccounts;
            return this;
        }

        public Builder Subscription(Boolean Subscription) {
            this.Subscription = Subscription;
            return this;
        }

        public Builder Publication(Boolean Publication) {
            this.Publication = Publication;
            return this;
        }

        public Builder ActiveSync(Boolean ActiveSync) {
            this.ActiveSync = ActiveSync;
            return this;
        }

        public Builder USM(Boolean USM) {
            this.USM = USM;
            return this;
        }

        public Builder OLOX20(Boolean OLOX20) {
            this.OLOX20 = OLOX20;
            return this;
        }

        public Builder GlobalAddressBookDisabled(Boolean GlobalAddressBookDisabled) {
            this.GlobalAddressBookDisabled = GlobalAddressBookDisabled;
            return this;
        }

        public Builder PublicFolderEditable(Boolean PublicFolderEditable) {
            this.PublicFolderEditable = PublicFolderEditable;
            return this;
        }

        public Builder deniedPortal(Boolean deniedPortal) {
            this.deniedPortal = deniedPortal;
            return this;
        }

        /**
         * Builds a new {@link UserModuleAccess} from this builder
         *
         * @return The {@link UserModuleAccess}
         */
        public UserModuleAccess build() {
            return new UserModuleAccess(this);
        }
    }

}
