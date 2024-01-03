
package com.openexchange.admin.soap.reseller.user.reseller.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import com.openexchange.admin.soap.reseller.user.reseller.soap.dataobjects.ResellerContext;
import com.openexchange.admin.soap.reseller.user.rmi.dataobjects.Credentials;


/**
 * <p>Java-Klasse f\u00fcr anonymous complex type.
 *
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ctx" type="{http://dataobjects.soap.reseller.admin.openexchange.com/xsd}ResellerContext" minOccurs="0"/>
 *         &lt;element name="search_pattern" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="auth" type="{http://dataobjects.rmi.admin.openexchange.com/xsd}Credentials" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "ctx",
    "searchPattern",
    "auth",
    "length", 
    "offset"
})
@XmlRootElement(name = "listCaseInsensitive")
public class ListCaseInsensitive {

    @XmlElement(name = "offset", required = false)
    protected Integer offset;
    @XmlElement(name = "length", required = false)
    protected Integer length;
    @XmlElement(nillable = true)
    protected ResellerContext ctx;
    @XmlElement(name = "search_pattern", nillable = true)
    protected String searchPattern;
    @XmlElement(nillable = true)
    protected Credentials auth;

    /**
     * Ruft den Wert der ctx-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link ResellerContext }
     *
     */
    public ResellerContext getCtx() {
        return ctx;
    }

    /**
     * Legt den Wert der ctx-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link ResellerContext }
     *
     */
    public void setCtx(ResellerContext value) {
        this.ctx = value;
    }

    /**
     * Ruft den Wert der searchPattern-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSearchPattern() {
        return searchPattern;
    }

    /**
     * Legt den Wert der searchPattern-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSearchPattern(String value) {
        this.searchPattern = value;
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

    /**
     * Sets the length
     *
     * @param length The length to set
     */
    public void setLength(Integer length) {
        this.length = length;
    }

    /**
     * Gets the length
     *
     * @return The length
     */
    public Integer getLength() {
        return length;
    }

    /**
     * Gets the offset
     *
     * @return The offset
     */
    public Integer getOffset() {
        return offset;
    }

    /**
     * Sets the offset
     *
     * @param offset The offset to set
     */
    public void setOffset(Integer offset) {
        this.offset = offset;
    }

}