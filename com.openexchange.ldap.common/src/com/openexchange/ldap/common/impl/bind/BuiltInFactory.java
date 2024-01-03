package com.openexchange.ldap.common.impl.bind;

import com.openexchange.ldap.common.BindRequestFactory;

/**
 * {@link BuiltInFactory} - An enumeration of built-in {@link BindRequestFactory}.
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public enum BuiltInFactory {
    
    /**
     * {@link UserDNResolvedBindRequestFactory}
     */
    USER_DN_RESOLVED("userDNResolved"),
    /**
     * {@link UserDNTemplateBindRequestFactory}
     */
    USER_DN_TEMPLATE("userDNTemplate"),
    /**
     * {@link OAuthBearerBindRequestFactory}}
     */
    OAUTHBEARER("oauthbearer"),
    ;
    
    private final String id;

    private BuiltInFactory(String id) {
        this.id = id;
    }

    /**
     * Gets the identifier
     *
     * @return The identifier
     */
    public String getId() {
        return id;
    }
}
