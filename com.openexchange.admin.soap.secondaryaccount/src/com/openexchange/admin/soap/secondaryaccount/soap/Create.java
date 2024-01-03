
package com.openexchange.admin.soap.secondaryaccount.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import com.openexchange.admin.soap.secondaryaccount.dataobjects.AccountDataOnCreate;
import com.openexchange.admin.soap.secondaryaccount.dataobjects.Context;
import com.openexchange.admin.soap.secondaryaccount.dataobjects.Credentials;
import com.openexchange.admin.soap.secondaryaccount.dataobjects.Group;
import com.openexchange.admin.soap.secondaryaccount.dataobjects.User;


/**
 * <p>Java-Klasse f\u00fcr anonymous complex type.
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "accountDataOnCreate",
    "context",
    "users",
    "groups",
    "auth"
})
@XmlRootElement(name = "create")
public class Create {

    @XmlElement(nillable = true)
    protected AccountDataOnCreate accountDataOnCreate;
    @XmlElement(nillable = true)
    protected Context context;
    @XmlElement(nillable = true)
    protected java.util.List<User> users;
    @XmlElement(nillable = true)
    protected java.util.List<Group> groups;
    @XmlElement(nillable = true)
    protected Credentials auth;

    /**
     * Ruft den Wert der accountData-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link AccountData }
     *
     */
    public AccountDataOnCreate getAccountDataOnCreate() {
        return accountDataOnCreate;
    }

    /**
     * Legt den Wert der accountData-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link AccountData }
     *
     */
    public void setAccountDataOnCreate(AccountDataOnCreate value) {
        this.accountDataOnCreate = value;
    }

    /**
     * Gets the context
     *
     * @return The context
     */
    public Context getContext() {
        return context;
    }

    /**
     * Sets the context
     *
     * @param context The context to set
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Gets the users
     *
     * @return The users
     */
    public java.util.List<User> getUsers() {
        return users;
    }

    /**
     * Sets the users
     *
     * @param users The users to set
     */
    public void setUsers(java.util.List<User> users) {
        this.users = users;
    }

    /**
     * Gets the groups
     *
     * @return The groups
     */
    public java.util.List<Group> getGroups() {
        return groups;
    }

    /**
     * Sets the groups
     *
     * @param groups The groups to set
     */
    public void setGroups(java.util.List<Group> groups) {
        this.groups = groups;
    }

    /**
     * Ruft den Wert der auth-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Credentials }
     *
     */
    public Credentials getAuth() {
        return auth;
    }

    /**
     * Legt den Wert der auth-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Credentials }
     *
     */
    public void setAuth(Credentials value) {
        this.auth = value;
    }

}
