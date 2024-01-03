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


package com.openexchange.pns.subscription.storage.rdb;

import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import com.openexchange.mail.utils.MailPasswordUtil;
import com.openexchange.pns.Meta;

/**
 * {@link MetaConverter} - Utility for converting meta instances.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MetaConverter {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MetaConverter.class);

    /**
     * Initializes a new {@link MetaConverter}.
     */
    private MetaConverter() {
        super();
    }

    /**
     * Converts given meta instance to its obfuscated string representation.
     *
     * @param meta The meta instance to convert
     * @return The encoded string representation or empty
     */
    public static Optional<String> convertMetaToString(Meta meta) {
        if (meta == null) {
            return Optional.empty();
        }

        JSONObject optionalJson = convertMetaToJson(meta);
        if (optionalJson == null) {
            return Optional.empty();
        }
        return Optional.of(encode(optionalJson.toString()));
    }

    /**
     * Converts given meta instance to its JSON representation.
     *
     * @param meta The meta instance to convert
     * @return The JSON representation or empty
     */
    private static JSONObject convertMetaToJson(Meta meta) {
        if (meta == null) {
            return null;
        }

        try {
            JSONObject jMeta = new JSONObject(meta.size());
            for (Map.Entry<String, Object> e : meta.entrySet()) {
                Object value = e.getValue();
                if (value instanceof Meta) {
                    JSONObject opt = convertMetaToJson((Meta) value);
                    if (opt != null) {
                        jMeta.put(e.getKey(), opt);
                    }
                } else {
                    jMeta.put(e.getKey(), value);
                }
            }
            return jMeta;
        } catch (JSONException e) {
            LOG.error("Unable to generate JSONObject", e);
        }
        return null;
    }

    /**
     * Parses given obfuscated string representation to its meta instance.
     *
     * @param sEncodedMeta The encoded string representation to parse
     * @return The meta instance or <code>null</code>
     */
    public static Meta parseStringToMeta(String sEncodedMeta) {
        if (sEncodedMeta == null) {
            return null;
        }

        try {
            return parseJsonToMeta(JSONServices.parseObject(decode(sEncodedMeta)));
        } catch (JSONException e) {
            LOG.error("Unable to parse JSONObject", e);
        }
        return null;
    }

    /**
     * Parses given JSON representation to its meta instance.
     *
     * @param jMeta The JSON representation to parse
     * @return The meta instance or <code>null</code>
     */
    private static Meta parseJsonToMeta(JSONObject jMeta) {
        if (jMeta == null) {
            return null;
        }

        Meta.Builder meta = Meta.builder();
        for (Map.Entry<String, Object> e :  jMeta.entrySet()) {
            Object value = e.getValue();
            if (value instanceof JSONObject) {
                meta.withProperty(e.getKey(), parseJsonToMeta((JSONObject) value));
            } else {
                meta.withProperty(e.getKey(), value);
            }
        }
        return meta.build();
    }

    // ----------------------------------------------------------------------------------------------------------------------------------

    private static final java.security.Key META_PW = MailPasswordUtil.generateSecretKey("pns-meta");

    /**
     * Encodes given header value
     *
     * @param used The number of already consumed characters in header line
     * @param raw The raw header value
     * @return The encoded header value
     */
    private static String encode(String raw) {
        if (null == raw) {
            return null;
        }

        try {
            return MailPasswordUtil.encrypt(raw, META_PW);
        } catch (GeneralSecurityException x) {
            LOG.debug("Failed to encode", x);
            return raw;
        }
    }

    /**
     * Decodes given header value
     *
     * @param encoded The encoded header value
     * @return The decoded header value
     */
    private static String decode(String encoded) {
        if (null == encoded) {
            return null;
        }

        try {
            return MailPasswordUtil.decrypt(encoded, META_PW);
        } catch (GeneralSecurityException x) {
            LOG.debug("Failed to decode", x);
            return encoded;
        }
    }

}
