
package com.openexchange.admin.soap.reseller.context.reseller.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import com.openexchange.admin.soap.reseller.context.reseller.soap.dataobjects.ResellerContext;
import com.openexchange.admin.soap.reseller.context.rmi.dataobjects.Credentials;
import com.openexchange.admin.soap.reseller.context.soap.dataobjects.SchemaSelectStrategy;
import com.openexchange.admin.soap.reseller.context.soap.dataobjects.User;
import com.openexchange.admin.soap.reseller.context.soap.dataobjects.UserModuleAccess;

/**
 * <p>Java-Klasse f\u00fcr anonymous complex type.
 *
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 *
 * <pre>
 * &lt;complexType>
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="ctx" type="{http://dataobjects.soap.reseller.admin.openexchange.com/xsd}ResellerContext" minOccurs="0"/>
 * &lt;element name="admin_user" type="{http://dataobjects.soap.admin.openexchange.com/xsd}User" minOccurs="0"/>
 * &lt;element name="access" type="{http://dataobjects.soap.admin.openexchange.com/xsd}UserModuleAccess" minOccurs="0"/>
 * &lt;element name="auth" type="{http://dataobjects.rmi.admin.openexchange.com/xsd}Credentials" minOccurs="0"/>
 * &lt;element name="auth" type="{http://dataobjects.rmi.admin.openexchange.com/xsd}SchemaSelectStrategy" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "ctx",
    "adminUser",
    "access",
    "auth",
    "schemaSelectStrategy"
})
@XmlRootElement(name = "createModuleAccess")
public class CreateModuleAccess {

    @XmlElement(nillable = true)
    protected ResellerContext ctx;
    @XmlElement(name = "admin_user", nillable = true)
    protected User adminUser;
    @XmlElement(nillable = true)
    protected UserModuleAccess access;
    @XmlElement(nillable = true)
    protected Credentials auth;
    @XmlElement(name = "schema_select_strategy", nillable = true)
    protected SchemaSelectStrategy schemaSelectStrategy;

    /**
     * Ruft den Wert der ctx-Eigenschaft ab.
     *
     * @return
     *         possible object is {@link ResellerContext }
     *
     */
    public ResellerContext getCtx() {
        return ctx;
    }

    /**
     * Legt den Wert der ctx-Eigenschaft fest.
     *
     * @param value
     *            allowed object is {@link ResellerContext }
     *
     */
    public void setCtx(ResellerContext value) {
        this.ctx = value;
    }

    /**
     * Ruft den Wert der adminUser-Eigenschaft ab.
     *
     * @return
     *         possible object is {@link User }
     *
     */
    public User getAdminUser() {
        return adminUser;
    }

    /**
     * Legt den Wert der adminUser-Eigenschaft fest.
     *
     * @param value
     *            allowed object is {@link User }
     *
     */
    public void setAdminUser(User value) {
        this.adminUser = value;
    }

    /**
     * Ruft den Wert der access-Eigenschaft ab.
     *
     * @return
     *         possible object is {@link UserModuleAccess }
     *
     */
    public UserModuleAccess getAccess() {
        return access;
    }

    /**
     * Legt den Wert der access-Eigenschaft fest.
     *
     * @param value
     *            allowed object is {@link UserModuleAccess }
     *
     */
    public void setAccess(UserModuleAccess value) {
        this.access = value;
    }

    /**
     * Ruft den Wert der auth-Eigenschaft ab.
     *
     * @return
     *         possible object is {@link Credentials }
     *
     */
    public Credentials getAuth() {
        return auth;
    }

    /**
     * Legt den Wert der auth-Eigenschaft fest.
     *
     * @param value
     *            allowed object is {@link Credentials }
     *
     */
    public void setAuth(Credentials value) {
        this.auth = value;
    }

    /**
     * Returns the schema select strategy.
     *
     * @return The {@link SchemaSelectStrategy}
     */
    public SchemaSelectStrategy getSchemaSelectStrategy() {
        return schemaSelectStrategy;
    }

    /**
     * Sets the schema select strategy.
     *
     * @param schemaSelectStrategy The {@link SchemaSelectStrategy}
     */
    public void setSchemaSelectStrategy(SchemaSelectStrategy schemaSelectStrategy) {
        this.schemaSelectStrategy = schemaSelectStrategy;
    }

}