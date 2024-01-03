
package com.openexchange.admin.soap.secondaryaccount.dataobjects;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.openexchange.admin.soap.util.dataobjects package.
 * <p>An ObjectFactory allows you to programmatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.openexchange.admin.soap.secondaryaccount.dataobjects
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Credentials }
     *
     */
    public Credentials createCredentials() {
        return new Credentials();
    }

    /**
     * Create an instance of {@link AccountData }
     *
     */
    public AccountData createAccountData() {
        return new AccountData();
    }

    /**
     * Create an instance of {@link AccountDataOnCreate }
     *
     */
    public AccountDataOnCreate createAccountDataOnCreate() {
        return new AccountDataOnCreate();
    }

    /**
     * Create an instance of {@link AccountDataUpdate }
     *
     */
    public AccountDataUpdate createAccountDataUpdate() {
        return new AccountDataUpdate();
    }

    /**
     * Create an instance of {@link Account }
     *
     */
    public Account createAccount() {
        return new Account();
    }

    /**
     * Create an instance of {@link Context }
     *
     */
    public Context createContext() {
        return new Context();
    }

    /**
     * Create an instance of {@link User }
     *
     */
    public User createUser() {
        return new User();
    }

    /**
     * Create an instance of {@link Group }
     *
     */
    public Group createGroup() {
        return new Group();
    }

}
