
package com.openexchange.sessiond.soap.exceptions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import com.openexchange.sessiond.soap.soap.Exception;


/**
 * <p>Java-Klasse f\u00fcr NoSuchUserException complex type.
 *
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 *
 * <pre>
 * &lt;complexType name="NoSuchUserException">
 *   &lt;complexContent>
 *     &lt;extension base="{http://soap.sessiond.openexchange.com}Exception">
 *       &lt;sequence>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NoSuchUserException")
public class NoSuchUserException
    extends Exception
{


}
