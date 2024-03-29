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

package com.openexchange.jslob.config;

import static com.openexchange.ajax.tools.JSONCoercion.coerceToJSON;
import static com.openexchange.java.Strings.isEmpty;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import org.json.JSONTokener;
import org.json.JSONValue;
import com.openexchange.ajax.tools.JSONUtil;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViewScope;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.IValueHandlerExtended;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.settings.SettingExceptionCodes;
import com.openexchange.groupware.settings.impl.ConfigTree;
import com.openexchange.groupware.settings.impl.SettingStorage;
import com.openexchange.java.Charsets;
import com.openexchange.java.SequentialCompletionService;
import com.openexchange.java.Streams;
import com.openexchange.jslob.DefaultJSlob;
import com.openexchange.jslob.JSONPathElement;
import com.openexchange.jslob.JSONUpdate;
import com.openexchange.jslob.JSlob;
import com.openexchange.jslob.JSlobEntry;
import com.openexchange.jslob.JSlobExceptionCodes;
import com.openexchange.jslob.JSlobId;
import com.openexchange.jslob.JSlobKey;
import com.openexchange.jslob.JSlobService;
import com.openexchange.jslob.shared.SharedJSlobService;
import com.openexchange.jslob.storage.JSlobStorage;
import com.openexchange.jslob.storage.registry.JSlobStorageRegistry;
import com.openexchange.preferences.ServerUserSettingLoader;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.threadpool.ThreadPools;

/**
 * {@link ConfigJSlobService}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ConfigJSlobService implements JSlobService {

    /** The logger */
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ConfigJSlobService.class);

    private static final List<String> ALIASES = Arrays.asList("config");

    /**
     * <code>"preferencePath"</code>
     */
    private static final String METADATA_PREFERENCE_PATH = "preferencePath";

    /**
     * <code>"protected"</code>
     */
    private static final String METADATA_PROTECTED = "protected";

    /**
     * <code>"final"</code>
     */
    private static final String METADATA_FINAL = "final";

    private static final String SERVICE_ID = "com.openexchange.jslob.config";

    /**
     * <code>"io.ox/core"</code>
     */
    private static final String CORE = "io.ox/core";

    /**
     * Protected subtrees of {@value #CORE}
     */
    private static final List<String> PROTECTED_SUBTREES = List.of("tracking", "registry");

    /*-
     * ------------------------- Member stuff -----------------------------
     */

    private final ServiceLookup services;
    private final AtomicReference<ConcurrentMap<String, Map<String, AttributedProperty>>> preferenceItemsReference;
    private final AtomicReference<ConcurrentMap<String, ConfigTreeEquivalent>> configTreeEquivalentsReference;
    private final ConcurrentMap<String, ConfigTreeEquivalent> registeredConfigTreeEquivalents;
    private final JSlobEntryRegistry jSlobEntryRegistry;
    private final Map<String, SharedJSlobService> sharedJSlobs;

    /**
     * Initializes a new {@link ConfigJSlobService}.
     *
     * @throws OXException If initialization fails
     */
    public ConfigJSlobService(JSlobEntryRegistry jSlobEntryRegistry, ServiceLookup services) throws OXException {
        super();
        this.services = services;
        this.jSlobEntryRegistry = jSlobEntryRegistry;
        preferenceItemsReference = new AtomicReference<ConcurrentMap<String,Map<String,AttributedProperty>>>();
        configTreeEquivalentsReference = new AtomicReference<ConcurrentMap<String,ConfigTreeEquivalent>>();
        registeredConfigTreeEquivalents = new ConcurrentHashMap<String, ConfigTreeEquivalent>(32, 0.9f, 1);
        initPreferenceItems();
        // Initialize core name mapping
        final ConfigurationService service = services.getService(ConfigurationService.class);
        initConfigTree(service);
        // Initialize shared JSlobs
        sharedJSlobs = new ConcurrentHashMap<String, SharedJSlobService>();
    }

    /**
     * Initializes the configuration tree equivalents.
     *
     * @param service The configuration service to use
     * @throws OXException If initialization fails
     */
    protected synchronized void initConfigTree(final ConfigurationService service) throws OXException {
        ConcurrentMap<String, ConfigTreeEquivalent> configTreeEquivalents = new ConcurrentHashMap<String, ConfigTreeEquivalent>(48, 0.9f, 1);

        // Add configured config-tree equivalents (if any)
        File file = service.getFileByName("paths.perfMap");
        if (null != file) {
            readPerfMap(file, configTreeEquivalents);
        }

        // Add registered config-tree equivalents
        for (Map.Entry<String, ConfigTreeEquivalent> equivEntry : registeredConfigTreeEquivalents.entrySet()) {
            String jslobName = equivEntry.getKey();
            ConfigTreeEquivalent equiv = configTreeEquivalents.get(jslobName);
            if (null == equiv) {
                configTreeEquivalents.put(jslobName, equivEntry.getValue());
            } else {
                equiv.mergeWith(equivEntry.getValue());
            }
        }

        configTreeEquivalentsReference.set(configTreeEquivalents);
    }

    /**
     * Gets the service look-up.
     *
     * @return The service look-up
     */
    public ServiceLookup getServices() {
        return services;
    }

    private static void readPerfMap(final File file, final ConcurrentMap<String, ConfigTreeEquivalent> lConfigTreeEquivalents) throws OXException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charsets.ISO_8859_1));
            for (String line = reader.readLine(); null != line; line = reader.readLine()) {
                line = line.trim();
                if ((false == isEmpty(line)) && ('#' != line.charAt(0))) {
                    int pos = line.indexOf('>');
                    if (pos > 0) {
                        String configTreePath = line.substring(0, pos).trim();
                        String jslobPath = line.substring(pos + 1).trim();

                        String jslobName;
                        {
                            final int pathSep = jslobPath.indexOf("//");
                            if (pathSep < 0) {
                                jslobName = CORE;
                            } else {
                                jslobName = jslobPath.substring(0, pathSep);
                                jslobPath = jslobPath.substring(pathSep + 2);
                            }
                        }

                        ConfigTreeEquivalent equiv = lConfigTreeEquivalents.get(jslobName);
                        if (equiv == null) {
                            equiv = new ConfigTreeEquivalent();
                            lConfigTreeEquivalents.putIfAbsent(jslobName, equiv);
                        }

                        equiv.config2lob.put(configTreePath, jslobPath);
                        equiv.lob2config.put(jslobPath, configTreePath);
                    }
                }
            }
        } catch (IOException e) {
            throw JSlobExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (RuntimeException rte) {
            throw JSlobExceptionCodes.UNEXPECTED_ERROR.create(rte, rte.getMessage());
        } finally {
            Streams.close(reader);
        }
    }

    private Map<String, JSlobEntryWrapper> getJSlobEntriesFor(String id) {
        JSlobKey jslobKey = JSlobKey.jslobKeyFor(id);
        return jslobKey == null ? null : jSlobEntryRegistry.getAvailableJSlobEntries().get(jslobKey);
    }

    /**
     * Adds specified config tree to jslob path mapping (if not already contained).
     *
     * @param configTreePath The config tree path; e.g. <code>"modules/mymodule/mysetting"</code>
     * @param jslobPath The associated jslob path; e.g. <code>"io.ox/mymodule//mysetting"</code>
     */
    public void addConfigTreeEquivalent(final String configTreePath, final String jslobPath) {
        if (isEmpty(configTreePath) || isEmpty(jslobPath)) {
            return;
        }

        String path = jslobPath.trim();
        String jslobName;
        {
            int pathSep = path.indexOf("//");
            if (pathSep < 0) {
                jslobName = CORE;
            } else {
                jslobName = path.substring(0, pathSep);
                path = path.substring(pathSep + 2);
            }
        }
        String confTreePath = configTreePath.trim();

        // Add to currently active config-tree equivalents
        addConfigTreeEquivalentToMap(jslobName, path, confTreePath, configTreeEquivalentsReference.get());

        // Add to registered config-tree equivalents
        addConfigTreeEquivalentToMap(jslobName, path, confTreePath, registeredConfigTreeEquivalents);

        LOG.debug("Added config-tree equivalent to JSlob: {} > {}", confTreePath, jslobPath);
    }

    private static void addConfigTreeEquivalentToMap(String jslobName, String path, String configTreePath, ConcurrentMap<String, ConfigTreeEquivalent> configTreeEquivalents) {
        ConfigTreeEquivalent equiv = configTreeEquivalents.get(jslobName);
        if (equiv == null) {
            ConfigTreeEquivalent newEquiv = new ConfigTreeEquivalent();
            equiv = configTreeEquivalents.putIfAbsent(jslobName, newEquiv);
            if (null == equiv) {
                equiv = newEquiv;
            }
        }

        equiv.config2lob.put(configTreePath, path);
        equiv.lob2config.put(path, configTreePath);
    }

    /**
     * Removes specified config tree from jslob path mapping
     *
     * @param configTreePath The config tree path
     * @param jslobPath The associated jslob path
     */
    public void removeConfigTreeEquivalent(final String configTreePath, final String jslobPath) {
        if (isEmpty(configTreePath) || isEmpty(jslobPath)) {
            return;
        }
        String path = jslobPath.trim();
        String jslobName;
        {
            final int pathSep = path.indexOf("//");
            if (pathSep < 0) {
                jslobName = CORE;
            } else {
                jslobName = path.substring(0, pathSep);
                path = path.substring(pathSep + 2);
            }
        }
        String confTreePath = configTreePath.trim();

        // Remove from registered config-tree equivalents
        removeConfigTreeEquivalentFromMap(jslobName, path, confTreePath, registeredConfigTreeEquivalents);

        // Remove from currently active config-tree equivalents
        removeConfigTreeEquivalentFromMap(jslobName, path, confTreePath, configTreeEquivalentsReference.get());

        LOG.debug("Removed config-tree equivalent from JSlob: {} > {}", confTreePath, jslobPath);
    }

    private static void removeConfigTreeEquivalentFromMap(String jslobName, String path, String configTreePath, ConcurrentMap<String, ConfigTreeEquivalent> configTreeEquivalents) {
        ConfigTreeEquivalent equiv = configTreeEquivalents.get(jslobName);
        if (equiv != null) {
            equiv.config2lob.remove(configTreePath);
            equiv.lob2config.remove(path);
        }
    }

    /**
     * Initializes preference items obtained from config-cascade.
     *
     * @throws OXException If initialization fails
     */
    protected synchronized void initPreferenceItems() throws OXException {
        // Read from config-cascade
        ConfigView view = getConfigViewFactory().getView();
        Map<String, ComposedConfigProperty<String>> all = view.all();

        // Initialize resulting map
        int initialCapacity = all.size() >> 1;
        ConcurrentMap<String, Map<String, AttributedProperty>> preferenceItems = new ConcurrentHashMap<String, Map<String, AttributedProperty>>(initialCapacity);
        for (Map.Entry<String, ComposedConfigProperty<String>> entry : all.entrySet()) {
            // Check for existence of "preferencePath"
            ComposedConfigProperty<String> property = entry.getValue();
            String preferencePath = property.get(METADATA_PREFERENCE_PATH);
            if (null != preferencePath) {
                // e.g. ui//halo/configuration/property01
                int separatorPos = preferencePath.indexOf("//");
                if (separatorPos >= 0) {
                    final String key = preferencePath.substring(0, separatorPos);
                    preferencePath = preferencePath.substring(separatorPos + 2);
                    Map<String, AttributedProperty> attributes = preferenceItems.get(key);
                    if (null == attributes) {
                        attributes = new HashMap<String, AttributedProperty>(initialCapacity);
                        preferenceItems.putIfAbsent(key, attributes);
                    }
                    try {
                        boolean isProtected = Boolean.parseBoolean(property.get(METADATA_PROTECTED));
                        attributes.put(preferencePath, new AttributedProperty(preferencePath, entry.getKey(), property, isProtected));
                    } catch (Exception e) {
                        LOG.warn("Couldn't initialize preference path: {}", preferencePath, e);
                    }
                }
            }
        }
        preferenceItemsReference.set(preferenceItems);
    }

    private static void mergeWithSharedJSlobs(String id, JSlob jsonJSlob, Map<String, SharedJSlobService> sharedJSlobs, Session session) throws OXException {
        for (Entry<String, SharedJSlobService> entry : sharedJSlobs.entrySet()) {
            String sharedId = entry.getKey();
            if (sharedId.startsWith(id)) {
                try {
                    JSlob sharedJSlob = entry.getValue().getJSlob(session);
                    JSONObject jsonObject = jsonJSlob.getJsonObject();
                    if (sharedId.equals(id)) {
                        JSONObject sharedJsonObject = sharedJSlob.getJsonObject();
                        for (Entry<String, Object> sharedEntry : sharedJsonObject.entrySet()) {
                            jsonObject.put(sharedEntry.getKey(), sharedEntry.getValue());
                        }
                    } else {
                        String newId = sharedId.substring(id.length() + 1, sharedId.length());
                        jsonObject.put(newId, sharedJSlob.getJsonObject());
                    }
                } catch (JSONException e) {
                    // Should not happen
                    throw JSlobExceptionCodes.JSON_ERROR.create(e, e.getMessage());
                }
            }
        }
    }

    @Override
    public Collection<JSlob> get(Session session) throws OXException {
        return get(true, session);
    }

    @Override
    public Collection<JSlob> get(boolean allowInjectingConfigTreeSettings, Session session) throws OXException {
        final int userId = session.getUserId();
        final int contextId = session.getContextId();

        List<JSlob> ret;
        boolean coreIncluded = false;

        // Load from storage
        {
            Collection<JSlob> list = getStorage().list(new JSlobId(SERVICE_ID, null, userId, contextId));
            ret = new ArrayList<JSlob>(list.size() << 1);
            for (final JSlob jSlob : list) {
                addConfigTreeToJslob(DefaultJSlob.copyOf(jSlob), allowInjectingConfigTreeSettings, session);
                ret.add(get(jSlob.getId().getId(), session));
                if (jSlob.getId().getId().equals(CORE)) {
                    coreIncluded = true;
                }
            }
        }

        // Append config tree & config cascade settings
        {
            ConfigView view = getConfigViewFactory().getView(userId, contextId);
            ConcurrentMap<String, Map<String, AttributedProperty>> preferenceItems = preferenceItemsReference.get();
            for (final Map.Entry<String, Map<String, AttributedProperty>> entry : preferenceItems.entrySet()) {
                final DefaultJSlob jSlob = new DefaultJSlob(new JSONObject());
                jSlob.setId(new JSlobId(SERVICE_ID, entry.getKey(), userId, contextId));

                addConfigTreeToJslob(jSlob, allowInjectingConfigTreeSettings, session);

                for (final Map.Entry<String, AttributedProperty> entry2 : entry.getValue().entrySet()) {
                    add2JSlob(entry2.getValue(), jSlob, view);
                }

                if (jSlob.getId().getId().equals(CORE)) {
                    coreIncluded = true;
                }

                ret.add(jSlob);
            }
        }

        // Append registered JSlob entries
        {
            for (Map.Entry<JSlobKey, ConcurrentMap<String, JSlobEntryWrapper>> entry : jSlobEntryRegistry.getAvailableJSlobEntries().entrySet()) {
                DefaultJSlob jSlob = new DefaultJSlob(new JSONObject());
                jSlob.setId(new JSlobId(SERVICE_ID, entry.getKey().getIdentifier(), userId, contextId));

                for (final Map.Entry<String, JSlobEntryWrapper> entry2 : entry.getValue().entrySet()) {
                    add2JSlob(entry2.getValue(), jSlob, session);
                }

                if (jSlob.getId().getId().equals(CORE)) {
                    coreIncluded = true;
                }

                ret.add(jSlob);
            }
        }

        if (!coreIncluded) {
            final DefaultJSlob jSlob = new DefaultJSlob(new JSONObject());
            jSlob.setId(new JSlobId(SERVICE_ID, CORE, userId, contextId));
            addConfigTreeToJslob(jSlob, allowInjectingConfigTreeSettings, session);
        }

        // Search for shared jslobs and merge them if necessary
        Map<String, SharedJSlobService> sharedJSlobs = this.sharedJSlobs;
        if (!sharedJSlobs.isEmpty()) {
            for (JSlob jSlob : ret) {
                String id = jSlob.getId().getId();
                mergeWithSharedJSlobs(id, jSlob, sharedJSlobs, session);
            }
        }
        return ret;
    }

    @Override
    public Collection<JSlob> getShared(Session session) throws OXException {
        List<JSlob> retval = new LinkedList<JSlob>();
        for (SharedJSlobService service : sharedJSlobs.values()) {
            retval.add(service.getJSlob(session));
        }
        return retval;
    }

    @Override
    public JSlob get(String id, Session session) throws OXException {
        return get(id, true, session);
    }

    @Override
    public JSlob get(String id, boolean allowInjectingConfigTreeSettings, Session session) throws OXException {
        final int userId = session.getUserId();
        final int contextId = session.getContextId();

        // Get from storage
        final DefaultJSlob jsonJSlob;
        {
            JSlobId jslobId = new JSlobId(SERVICE_ID, id, userId, contextId);
            final JSlob opt = getStorage().opt(jslobId);
            if (null == opt) {
                jsonJSlob = new DefaultJSlob(new JSONObject());
                jsonJSlob.setId(jslobId);
            } else {
                jsonJSlob = DefaultJSlob.copyOf(opt);
            }
        }

        // Append config tree settings
        addConfigTreeToJslob(jsonJSlob, allowInjectingConfigTreeSettings, session);

        // Append config cascade settings
        ConcurrentMap<String, Map<String, AttributedProperty>> preferenceItems = preferenceItemsReference.get();
        final Map<String, AttributedProperty> attributes = preferenceItems.get(id);
        if (null != attributes) {
            final ConfigView view = getConfigViewFactory().getView(userId, contextId);
            for (final AttributedProperty attributedProperty : attributes.values()) {
                add2JSlob(attributedProperty, jsonJSlob, view);
            }
        }

        // Append registered JSlob entries
        Map<String, JSlobEntryWrapper> entries = getJSlobEntriesFor(id);
        if (null != entries) {
            for (JSlobEntryWrapper entry : entries.values()) {
                add2JSlob(entry, jsonJSlob, session);
            }
        }

        // Search for shared jslobs and merge them if necessary
        mergeWithSharedJSlobs(id, jsonJSlob, this.sharedJSlobs, session);

        return jsonJSlob;
    }

    @Override
    public List<JSlob> get(List<String> ids, Session session) throws OXException {
        return get(ids, true, session);
    }

    @Override
    public List<JSlob> get(List<String> ids, boolean allowInjectingConfigTreeSetting, Session session) throws OXException {
        final int userId = session.getUserId();
        final int contextId = session.getContextId();
        final int size = ids.size();

        final List<JSlob> jSlobs;
        {
            final List<JSlobId> jSlobIds = new ArrayList<JSlobId>(size);
            for (final String sId : ids) {
                jSlobIds.add(new JSlobId(SERVICE_ID, sId, userId, contextId));
            }
            jSlobs = getStorage().list(jSlobIds);
        }

        final List<JSlob> ret = new ArrayList<JSlob>(size);
        for (int i = 0; i < size; i++) {
            final JSlob opt = jSlobs.get(i);
            final String id = ids.get(i);
            final DefaultJSlob jsonJSlob;
            {
                if (null == opt) {
                    jsonJSlob = new DefaultJSlob(new JSONObject());
                    jsonJSlob.setId(new JSlobId(SERVICE_ID, id, userId, contextId));
                } else {
                    jsonJSlob = DefaultJSlob.copyOf(opt);
                }
            }

            // Append config tree settings
            addConfigTreeToJslob(jsonJSlob, allowInjectingConfigTreeSetting, session);

            // Append config cascade settings
            ConcurrentMap<String, Map<String, AttributedProperty>> preferenceItems = preferenceItemsReference.get();
            final Map<String, AttributedProperty> attributes = preferenceItems.get(id);
            if (null != attributes) {
                final ConfigView view = getConfigViewFactory().getView(userId, contextId);
                for (final AttributedProperty attributedProperty : attributes.values()) {
                    add2JSlob(attributedProperty, jsonJSlob, view);
                }
            }

            // Append registered JSlob entries
            Map<String, JSlobEntryWrapper> entries = getJSlobEntriesFor(id);
            if (null != entries) {
                for (JSlobEntryWrapper entry : entries.values()) {
                    add2JSlob(entry, jsonJSlob, session);
                }
            }

            ret.add(jsonJSlob);
        }

        // Search for shared jslobs and merge them if necessary
        Map<String, SharedJSlobService> sharedJSlobs = this.sharedJSlobs;
        if (!sharedJSlobs.isEmpty()) {
            for (JSlob jslob : ret) {
                String id = jslob.getId().getId();
                mergeWithSharedJSlobs(id, jslob, sharedJSlobs, session);
            }
        }

        return ret;
    }

    @Override
    public JSlob getShared(final String id, Session session) throws OXException {
        SharedJSlobService service = sharedJSlobs.get(id);
        if (null != service) {
            return sharedJSlobs.get(id).getJSlob(session);
        }
        return null;
    }

    /**
     * Adds settings from config-tree to jslob mappings.
     *
     * @param jsLob The JSlob to enhance by possible settings from config-tree
     * @param allowInjectingConfigTreeSettings <code>true</code> to allow injecting settings from config-tree; otherwise <code>false</code>
     * @param session The session providing user data
     * @throws OXException If operation fails
     */
    private void addConfigTreeToJslob(final DefaultJSlob jsLob, final boolean allowInjectingConfigTreeSettings, final Session session) throws OXException {
        if (false == allowInjectingConfigTreeSettings) {
            return;
        }

        try {
            ConcurrentMap<String, ConfigTreeEquivalent> configTreeEquivalents = configTreeEquivalentsReference.get();
            final ConfigTreeEquivalent equiv = configTreeEquivalents.get(jsLob.getId().getId());
            if (equiv == null) {
                return;
            }
            final SettingStorage stor = SettingStorage.getInstance(session);
            final ConfigTree configTree = ConfigTree.getInstance();

            final JSONObject jObject = new JSONObject(jsLob.getJsonObject());
            for (final Map.Entry<String, String> mapping : equiv.config2lob.entrySet()) {
                final String configTreePath = mapping.getKey();
                final String lobPath = mapping.getValue();

                // Get setting by config-tree path
                Setting setting = null;
                try {
                    setting = configTree.getSettingByPath(configTreePath);
                } catch (OXException e) {
                    LOG.warn("Illegal config-tree path: {}. Please check paths.perfMap file (JSlob ID: {}) OR if path-associated bundle has been started.", configTreePath, lobPath, e);
                } catch (Exception e) {
                    LOG.warn("Failed to get setting by config-tree path: {} (JSlob ID: {}).", configTreePath, lobPath, e);
                }

                // Get its value & put it into JSlob
                if (null != setting) {
                    try {
                        stor.readValues(setting);

                        Object value = convert2JS(setting);
                        if (value != JSONObject.NULL) {
                            putToJsonObject(lobPath, value, jObject);
                        }
                    } catch (Exception e) {
                        LOG.warn("Failed to read value for config-tree path: {} (JSlob ID: {})", configTreePath, lobPath, e);
                    }
                } else {
                    LOG.debug("Failed to obtain setting for config-tree path: {} (JSlob ID: {})", configTreePath, lobPath);
                }
            }
            jsLob.setJsonObject(jObject);
        } catch (RuntimeException rte) {
            throw JSlobExceptionCodes.UNEXPECTED_ERROR.create(rte, rte.getMessage());
        }
    }

    private static void putToJsonObject(final String lobPath, final Object setting, final JSONObject jObject) throws JSONException {
        final int pos = lobPath.indexOf('/');
        if (pos <= 0) {
            jObject.put(lobPath, setting);
        } else {
            final String fieldName = lobPath.substring(0, pos);
            final String subLobPath = lobPath.substring(pos + 1);

            JSONObject jChild = jObject.optJSONObject(fieldName);
            if (null == jChild) {
                jChild = new JSONObject(2);
                jObject.put(fieldName, jChild);
            }

            putToJsonObject(subLobPath, setting, jChild);
        }
    }

    /**
     * Converts a tree of settings into the according java script objects.
     *
     * @param setting Tree of settings.
     * @return java script object representing the setting tree.
     * @throws JSONException if the conversion to java script objects fails.
     */
    private static Object convert2JS(final Setting setting) throws JSONException {
        if (setting.isLeaf()) {
            Object[] multiValue = setting.getMultiValue();
            if (null == multiValue) {
                // Not a multi-value
                Object singleValue = setting.getSingleValue();
                if (null == singleValue) {
                    return JSONObject.NULL;
                }
                if (singleValue instanceof JSONObject) {
                    return singleValue;
                }
                try {
                    return JSONServices.parseObject(singleValue.toString());
                } catch (JSONException e) {
                    return singleValue;
                }
            }

            // Handle multi-value
            JSONArray array = new JSONArray(multiValue.length);
            for (Object value : multiValue) {
                array.put(coerceToJSON(value));
            }
            return array;
        }

        // Not a leaf element; recursive invocation needed
        Setting[] elements = setting.getElements();
        JSONObject json = new JSONObject(elements.length);
        for (Setting subSetting : elements) {
            json.put(subSetting.getName(), convert2JS(subSetting));
        }
        return json;
    }

    @Override
    public String getIdentifier() {
        return SERVICE_ID;
    }

    @Override
    public List<String> getAliases() {
        return ALIASES;
    }

    @Override
    public void set(final String id, final JSlob jSlob, final Session session) throws OXException {
        final int userId = session.getUserId();
        final int contextId = session.getContextId();

        if ((DefaultJSlob.EMPTY_JSLOB == jSlob) || (null == jSlob)) {
            getStorage().remove(new JSlobId(SERVICE_ID, id, userId, contextId));
        } else {
            if (null == jSlob.getJsonObject()) {
                getStorage().remove(new JSlobId(SERVICE_ID, id, userId, contextId));
                return;
            }

            // Clone it
            final DefaultJSlob jsonJSlob = DefaultJSlob.copyOf(jSlob);
            final JSONObject jObject = jsonJSlob.getJsonObject();

            // Remember the paths to purge
            final List<List<JSONPathElement>> pathsToPurge = new LinkedList<List<JSONPathElement>>();
            if (JSlobKey.CORE.getIdentifier().equals(id)) {
                for (String subtree : PROTECTED_SUBTREES) {
                    if (jObject.hasAndNotNull(subtree)) {
                        pathsToPurge.add(Collections.singletonList(new JSONPathElement(subtree)));
                    }
                }
            }

            // Config Tree Values first
            {
                final CompletionServiceReference cr = new CompletionServiceReference();
                try {
                    ConcurrentMap<String, ConfigTreeEquivalent> configTreeEquivalents = configTreeEquivalentsReference.get();
                    final ConfigTreeEquivalent equiv = configTreeEquivalents.get(id);
                    if (equiv != null) {
                        session.setParameter("__serverUserSetting", ServerUserSettingLoader.getInstance().loadFor(session.getUserId(), session.getContextId()));
                        try {
                            final SettingStorage stor = SettingStorage.getInstance(session);
                            final ConfigTree configTree = ConfigTree.getInstance();
                            final Map<String, String> attribute2ConfigTreeMap = equiv.lob2config;
                            // Update setting
                            updateConfigTreeSetting("", jObject, configTree, attribute2ConfigTreeMap, stor, pathsToPurge, cr);
                        } finally {
                            session.setParameter("__serverUserSetting", null);
                        }
                    }

                    // Check completion service
                    if (cr.num > 0) {
                        ThreadPools.<Void, OXException> awaitCompletionService(cr.completionService, cr.num, ThreadPools.DEFAULT_EXCEPTION_FACTORY);
                    }
                } finally {
                    SequentialCompletionService<Void> completionService = cr.completionService;
                    if (null != completionService) {
                        completionService.close();
                    }
                }
            }

            // Set (or replace) JSlob
            ConcurrentMap<String, Map<String, AttributedProperty>> preferenceItems = preferenceItemsReference.get();
            Map<String, AttributedProperty> attributes = preferenceItems.get(id);
            if (null != attributes) {
                // A config cascade change because identifier refers to a preference item
                ConfigView view = getConfigViewFactory().getView(userId, contextId);
                for (AttributedProperty attributedProperty : attributes.values()) {
                    Object value = JSONPathElement.getPathFrom(attributedProperty.path, jObject);
                    if (null != value) {
                        if (view.property(attributedProperty.propertyName, String.class).get() != null) {
                            pathsToPurge.add(attributedProperty.path);
                            // Update if not protected
                            if (false == attributedProperty.isProtected) {
                                Object oldValue = asJSObject(view.get(attributedProperty.propertyName, String.class));
                                // Clients have a habit of dumping the config back at us, so we only save differing values.
                                if (!value.equals(oldValue)) {
                                    view.set(ConfigViewScope.USER.getScopeName(), attributedProperty.propertyName, value);
                                }
                            }
                        }
                    }
                }
            }

            Map<String, JSlobEntryWrapper> entries = getJSlobEntriesFor(id);
            if (null != entries) {
                // A change for a JSlob entry
                for (JSlobEntryWrapper wrapper : entries.values()) {
                    Object value = JSONPathElement.getPathFrom(wrapper.getParsedPath(), jObject);
                    if (null != value) {
                        pathsToPurge.add(wrapper.getParsedPath());
                        // Update if not read-only
                        JSlobEntry jSlobEntry = wrapper.getJSlobEntry();
                        if (jSlobEntry.isWritable(session) && jSlobEntry.needsUpdate(value, session)) {
                            jSlobEntry.setValue(value, session);
                        }
                    }
                }
            }

            for (List<JSONPathElement> path : pathsToPurge) {
                JSONPathElement.remove(path, jObject);
            }
            jsonJSlob.setJsonObject(jObject);

            // Finally store JSlob
            getStorage().store(new JSlobId(SERVICE_ID, id, userId, contextId), jsonJSlob);
        }
    }

    private static void updateConfigTreeSetting(String prefix, JSONObject jObject, final ConfigTree configTree, Map<String, String> attribute2ConfigTreeMap, final SettingStorage stor, List<List<JSONPathElement>> pathsToPurge, CompletionServiceReference cr) throws OXException {
        for (final Entry<String, Object> entry : jObject.entrySet()) {
            String key = prefix + entry.getKey();
            final Object value = entry.getValue();
            String path = attribute2ConfigTreeMap.get(key);
            if (path != null) {
                pathsToPurge.add(JSONPathElement.parsePath(key));
                if (path.length() > 0 && path.charAt(0) == '/') {
                    path = path.substring(1);
                }
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                final String _path = path;
                Callable<Void> task = new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        try {
                            Setting setting = configTree.optSettingByPath(_path);
                            if (null != setting) {
                                setting.setSingleValue(value);
                                saveSettingWithSubs(stor, setting);
                            }
                        } catch (OXException x) {
                            if (!SettingExceptionCodes.UNKNOWN_PATH.equals(x)) {
                                throw x;
                            }
                            LOG.debug("Ignoring update to unmappable path", x);
                        }
                        return null;
                    }
                };

                if (null == cr.completionService) {
                    cr.completionService = new SequentialCompletionService<Void>(ThreadPools.getThreadPool().getExecutor());
                }
                cr.completionService.submit(task);
                cr.num++;
            } else if (value instanceof JSONObject) {
                // Recursive
                updateConfigTreeSetting(key + "/", (JSONObject) value, configTree, attribute2ConfigTreeMap, stor, pathsToPurge, cr);
            }
        }
    }

    @Override
    public void setShared(final String id, final SharedJSlobService service) {
        if (null == service) {
            sharedJSlobs.remove(id);
        } else {
            sharedJSlobs.put(id, service);
        }
    }

    /**
     * Splits a value for a not leaf setting into its sub-settings and stores them.
     *
     * @param storage setting storage.
     * @param setting actual setting.
     * @throws OXException If an error occurs.
     */
    protected static void saveSettingWithSubs(SettingStorage storage, Setting setting) throws OXException {
        try {
            if (setting.isLeaf()) {
                String value = setting.getSingleValue().toString();
                if (null != value && value.length() > 0 && '[' == value.charAt(0)) {
                    JSONArray array = asJsonArray(value);
                    if (array != null) {
                        if (array.length() == 0) {
                            setting.setEmptyMultiValue();
                        } else {
                            for (int i = 0; i < array.length(); i++) {
                                setting.addMultiValue(array.getString(i));
                            }
                        }
                        setting.setSingleValue(null);
                    }
                }
                storage.save(setting);
            } else {
                // Construct JSON object
                JSONObject json;
                {
                    Object singleValue = setting.getSingleValue();
                    json = singleValue instanceof JSONObject ? new JSONObject((JSONObject) singleValue) : JSONServices.parseObject(singleValue.toString());
                }

                // Save it
                OXException exc = null;
                for (Iterator<String> iter = json.keys(); iter.hasNext();) {
                    String key = iter.next();
                    Setting subSetting = ConfigTree.optSettingByPath(setting, new String[] { key });
                    if (null != subSetting) {
                        subSetting.setSingleValue(json.getString(key));
                        try {
                            // Catch single exceptions if GUI writes not writable fields.
                            saveSettingWithSubs(storage, subSetting);
                        } catch (OXException e) {
                            exc = e;
                        }
                    }
                }

                // Check for exception
                if (null != exc) {
                    throw exc;
                }
            }
        } catch (JSONException e) {
            throw JSlobExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException rte) {
            throw JSlobExceptionCodes.UNEXPECTED_ERROR.create(rte, rte.getMessage());
        }
    }

    private static JSONArray asJsonArray(final String value) {
        try {
            return JSONServices.parseArray(value);
        } catch (JSONException e) {
            // Apparently no JSON array, treat as string
            return null;
        }
    }

    @Override
    public void update(final String id, final JSONUpdate jsonUpdate, final Session session) throws OXException {
        try {
            /*
             * Look-up appropriate storage
             */
            int userId = session.getUserId();
            int contextId = session.getContextId();
            JSlobStorage storage = getStorage();
            JSlobId jslobId = new JSlobId(SERVICE_ID, id, userId, contextId);
            /*
             * Get JSlob
             */
            JSONObject storageObject;
            DefaultJSlob jsonJSlob;
            {
                JSlob opt = storage.opt(jslobId);
                if (null == opt) {
                    storageObject = new JSONObject();
                    jsonJSlob = new DefaultJSlob(storageObject);
                } else {
                    jsonJSlob = DefaultJSlob.copyOf(opt);
                    storageObject = jsonJSlob.getJsonObject();
                }
            }
            /*
             * Examine path
             */
            List<JSONPathElement> path = jsonUpdate.getPath();
            if (path.isEmpty()) {
                /*
                 * Merge whole object
                 */
                JSONObject merged = JSONUtil.merge(storageObject, (JSONObject) jsonUpdate.getValue());
                set(id, jsonJSlob.setJsonObject(merged), session);
                return;
            }

            /*
             * Deny setting any protected subtrees
             */
            if (JSlobKey.CORE.getIdentifier().equals(id)) {
                for (String subtree : PROTECTED_SUBTREES) {
                    if (subtree.equals(path.get(0).getName())) {
                        throw JSlobExceptionCodes.PROTECTED.create(path.get(0).getName());
                    }
                }
            }

            /*
             * Let's try the config mappings
             */
            if (path.size() == 1) {
                // Only first level elements can be config tree equivalents
                ConcurrentMap<String, ConfigTreeEquivalent> configTreeEquivalents = configTreeEquivalentsReference.get();
                ConfigTreeEquivalent equiv = configTreeEquivalents.get(id);
                if (equiv != null) {
                    String configTreePath = equiv.lob2config.get(path.get(0).getName());
                    Object value = jsonUpdate.getValue();
                    if (null != value && configTreePath != null) {
                        SettingStorage stor = SettingStorage.getInstance(session);
                        ConfigTree configTree = ConfigTree.getInstance();
                        Setting setting = configTree.getSettingByPath(configTreePath);
                        IValueHandler valueHandler = setting.getShared();
                        if (null != valueHandler && false == (valueHandler instanceof IValueHandlerExtended ? ((IValueHandlerExtended) valueHandler).isWritable(session) : valueHandler.isWritable())) {
                            // Not writable
                            throw JSlobExceptionCodes.PROTECTED.create(path.get(0).getName());
                        }
                        setting.setSingleValue(value);
                        saveSettingWithSubs(stor, setting);
                    }
                }
            }

            /*
             * Update in config cascade or basic storage
             */
            int size = path.size();
            ConcurrentMap<String, Map<String, AttributedProperty>> preferenceItems = preferenceItemsReference.get();
            Map<String, AttributedProperty> attributes = preferenceItems.get(id);
            if (null != attributes) {
                /*
                 * A config cascade change because identifier refers to a preference item
                 */
                String sPath;
                {
                    StringBuilder pathBuilder = new StringBuilder(16);
                    pathBuilder.append(path.get(0).toString());
                    for (int i = 1; i < size; i++) {
                        pathBuilder.append('/').append(path.get(i).toString());
                    }
                    sPath = pathBuilder.toString();
                }
                AttributedProperty attributedProperty = attributes.get(sPath);
                if (null == attributedProperty) {
                    /*
                     * No such property
                     */
                    throw JSlobExceptionCodes.PATH_NOT_FOUND.create(sPath);
                }
                Object newValue = jsonUpdate.getValue();
                if (null != newValue) {
                    ConfigView view = getConfigViewFactory().getView(userId, contextId);
                    String oldValue = view.get(attributedProperty.propertyName, String.class);
                    // Clients have a habit of dumping the config back at us, so we only save differing values.
                    if (!newValue.equals(oldValue)) {
                        if (attributedProperty.isProtected) {
                            /*
                             * Protected property
                             */
                            throw JSlobExceptionCodes.PROTECTED.create(sPath);
                        }
                        view.set(ConfigViewScope.USER.getScopeName(), attributedProperty.propertyName, newValue);
                    }
                }
                return;
            }

            /*
             * Update in JSlob entry
             */
            Map<String, JSlobEntryWrapper> entries = getJSlobEntriesFor(id);
            if (null != entries) {
                /*
                 * A JSlob entry change
                 */
                String sPath;
                {
                    StringBuilder pathBuilder = new StringBuilder(16);
                    pathBuilder.append(path.get(0).toString());
                    for (int i = 1; i < size; i++) {
                        pathBuilder.append('/').append(path.get(i).toString());
                    }
                    sPath = pathBuilder.toString();
                }
                JSlobEntryWrapper wrapper = entries.get(sPath);
                if (null == wrapper) {
                    /*
                     * No such JSlob entry
                     */
                    throw JSlobExceptionCodes.PATH_NOT_FOUND.create(sPath);
                }
                Object newValue = jsonUpdate.getValue();
                if (null != newValue) {
                    JSlobEntry jSlobEntry = wrapper.getJSlobEntry();
                    if (jSlobEntry.needsUpdate(newValue, session)) {
                        if (false == jSlobEntry.isWritable(session)) {
                            /*
                             * Protected property
                             */
                            throw JSlobExceptionCodes.PROTECTED.create(sPath);
                        }
                        jSlobEntry.setValue(newValue, session);
                    }
                }
                return;
            }

            /*-
             * Update in store
             *
             * Iterate path except last element
             */
            int msize = size - 1;
            JSONObject current = storageObject;
            for (int i = 0; i < msize; i++) {
                JSONPathElement jsonPathElem = path.get(i);
                int index = jsonPathElem.getIndex();
                String name = jsonPathElem.getName();
                if (index >= 0) {
                    /*
                     * Denotes an index within a JSON array
                     */
                    if (isInstance(name, JSONArray.class, current)) {
                        JSONArray jsonArray = current.getJSONArray(name);
                        if (index >= jsonArray.length()) {
                            current = putNewJSONObject(jsonArray);
                        } else {
                            if (isInstance(index, JSONObject.class, jsonArray)) {
                                current = jsonArray.getJSONObject(index);
                            } else {
                                current = putNewJSONObject(index, jsonArray);
                            }
                        }
                    } else {
                        JSONArray newArray = new JSONArray();
                        current.put(name, newArray);
                        current = putNewJSONObject(newArray);
                    }
                } else {
                    /*
                     * Denotes an element within a JSON object
                     */
                    if (isInstance(name, JSONObject.class, current)) {
                        current = current.getJSONObject(name);
                    } else {
                        JSONObject newObject = new JSONObject();
                        current.put(name, newObject);
                        current = newObject;
                    }
                }
            }
            /*
             * Handle last path element
             */
            JSONPathElement lastPathElem = path.get(msize);
            int index = lastPathElem.getIndex();
            String name = lastPathElem.getName();
            if (index >= 0) {
                if (isInstance(name, JSONArray.class, current)) {
                    current.getJSONArray(name).put(index, jsonUpdate.getValue());
                } else {
                    JSONArray newArray = new JSONArray();
                    current.put(name, newArray);
                    newArray.put(jsonUpdate.getValue());
                }
            } else {
                current.put(name, jsonUpdate.getValue());
            }
            /*
             * Write to store
             */
            storage.store(jslobId, jsonJSlob.setJsonObject(storageObject));
        } catch (JSONException e) {
            throw JSlobExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException rte) {
            throw JSlobExceptionCodes.UNEXPECTED_ERROR.create(rte, rte.getMessage());
        }
    }

    private static JSONObject putNewJSONObject(final JSONArray jsonArray) throws JSONException {
        return putNewJSONObject(-1, jsonArray);
    }

    private static JSONObject putNewJSONObject(final int index, final JSONArray jsonArray) throws JSONException {
        final JSONObject newObject = new JSONObject();
        if (index >= 0) {
            jsonArray.put(index, newObject);
        } else {
            jsonArray.put(newObject);
        }
        return newObject;
    }

    private static boolean isInstance(final String name, final Class<? extends JSONValue> clazz, final JSONObject jsonObject) {
        if (!jsonObject.hasAndNotNull(name)) {
            return false;
        }
        return clazz.isInstance(jsonObject.opt(name));
    }

    private static boolean isInstance(final int index, final Class<? extends JSONValue> clazz, final JSONArray jsonArray) {
        return clazz.isInstance(jsonArray.opt(index));
    }

    // TODO: Make configurable
    private static final String STORAGE_ID = "io.ox.wd.jslob.storage.db";

    private JSlobStorage getStorage() throws OXException {
        final JSlobStorageRegistry storageRegistry = services.getService(JSlobStorageRegistry.class);
        final JSlobStorage storage = storageRegistry.getJSlobStorage(STORAGE_ID);
        if (null == storage) {
            throw JSlobExceptionCodes.NOT_FOUND.create(STORAGE_ID);
        }
        return storage;
    }

    private ConfigViewFactory getConfigViewFactory() {
        return services.getService(ConfigViewFactory.class);
    }

    private static final Set<String> SKIP_META = Set.of(METADATA_FINAL, METADATA_PROTECTED, METADATA_PREFERENCE_PATH);

    private static void add2JSlob(final AttributedProperty attributedProperty, final JSlob jsonJSlob, final ConfigView view) throws OXException {
        if (null == attributedProperty) {
            return;
        }
        try {
            // Add property's value
            final List<JSONPathElement> path = attributedProperty.path;
            Object value = asJSObject(view.get(attributedProperty.propertyName, String.class));

            addValueByPath(path, value, jsonJSlob.getJsonObject());

            // Add the metadata as well as a separate JSON object
            final JSONObject jMetaData = new JSONObject();
            final ComposedConfigProperty<String> preferenceItem = attributedProperty.property;
            final List<String> metadataNames = preferenceItem.getMetadataNames();
            if (null != metadataNames && !metadataNames.isEmpty()) {
                for (final String metadataName : metadataNames) {
                    if (SKIP_META.contains(metadataName)) {
                        continue;
                    }
                    // Metadata value
                    final ComposedConfigProperty<String> prop = view.property(attributedProperty.propertyName, String.class);
                    value = asJSObject(prop.get(metadataName));
                    jMetaData.put(metadataName, value);
                }
            }
            // Lastly, let's add configurability.
            final String finalScope = preferenceItem.get(METADATA_FINAL);
            final String isProtected = preferenceItem.get(METADATA_PROTECTED);
            final boolean writable = (finalScope == null || finalScope.equals(ConfigViewScope.USER.getScopeName())) && (isProtected == null || !Boolean.parseBoolean(isProtected));
            if (!writable) {
                jMetaData.put("configurable", Boolean.valueOf(writable));
            }
            if (jMetaData.length() > 0) {
                addValueByPath(path, jMetaData, jsonJSlob.getMetaObject());
            }
        } catch (JSONException e) {
            throw JSlobExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException rte) {
            throw JSlobExceptionCodes.UNEXPECTED_ERROR.create(rte, rte.getMessage());
        }
    }

    private static void add2JSlob(final JSlobEntryWrapper wrapper, final JSlob jsonJSlob, Session session) throws OXException {
        if (null == wrapper) {
            return;
        }
        try {
            // Add property's value
            List<JSONPathElement> path = wrapper.getParsedPath();
            JSlobEntry jSlobEntry = wrapper.getJSlobEntry();
            Object value = jSlobEntry.getValue(session);

            addValueByPath(path, value, jsonJSlob.getJsonObject());

            // Add the metadata as well as a separate JSON object
            JSONObject jMetaData;
            {
                Map<String, Object> metadata = jSlobEntry.metadata(session);
                int size;
                if (null != metadata && (size = metadata.size()) > 0) {
                    jMetaData = new JSONObject(size + 2);
                    for (Map.Entry<String, Object> metadataEntry : metadata.entrySet()) {
                        String metadataName = metadataEntry.getKey();
                        if (SKIP_META.contains(metadataName)) {
                            continue;
                        }
                        // Metadata value
                        value = metadataEntry.getValue();
                        jMetaData.put(metadataName, value);
                    }
                } else {
                    jMetaData = new JSONObject(2);
                }
            }
            // Lastly, let's add configurability.
            boolean writable = jSlobEntry.isWritable(session);
            jMetaData.put("configurable", Boolean.valueOf(writable));
            if (jMetaData.length() > 0) {
                addValueByPath(path, jMetaData, jsonJSlob.getMetaObject());
            }
        } catch (JSONException e) {
            throw JSlobExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException rte) {
            throw JSlobExceptionCodes.UNEXPECTED_ERROR.create(rte, rte.getMessage());
        }
    }

    private static void addValueByPath(final List<JSONPathElement> path, final Object value, final JSONObject object) throws JSONException {
        if (null == object) {
            LOG.debug("JSONObject is null. Can't add value {}", value);
            return;
        }
        final int msize = path.size() - 1;
        JSONObject current = object;
        for (int i = 0; i < msize; i++) {
            final JSONPathElement jsonPathElem = path.get(i);
            final int index = jsonPathElem.getIndex();
            final String name = jsonPathElem.getName();
            if (index >= 0) {
                /*
                 * Denotes an index within a JSON array
                 */
                if (isInstance(name, JSONArray.class, current)) {
                    final JSONArray jsonArray = current.getJSONArray(name);
                    if (index >= jsonArray.length()) {
                        current = putNewJSONObject(jsonArray);
                    } else {
                        if (isInstance(index, JSONObject.class, jsonArray)) {
                            current = jsonArray.getJSONObject(index);
                        } else {
                            current = putNewJSONObject(index, jsonArray);
                        }
                    }
                } else {
                    final JSONArray newArray = new JSONArray();
                    current.put(name, newArray);
                    current = putNewJSONObject(newArray);
                }
            } else {
                /*
                 * Denotes an element within a JSON object
                 */
                if (isInstance(name, JSONObject.class, current)) {
                    current = current.getJSONObject(name);
                } else {
                    final JSONObject newObject = new JSONObject();
                    current.put(name, newObject);
                    current = newObject;
                }
            }
        }
        /*
         * Handle last path element
         */
        final JSONPathElement lastPathElem = path.get(msize);
        final int index = lastPathElem.getIndex();
        final String name = lastPathElem.getName();
        if (index >= 0) {
            if (isInstance(name, JSONArray.class, current)) {
                current.getJSONArray(name).put(index, value);
            } else {
                final JSONArray newArray = new JSONArray();
                current.put(name, newArray);
                newArray.put(value);
            }
        } else {
            current.put(name, value);
        }
    }

    /**
     * Converts given String to a regular JSON-supported value.
     * <p>
     * The value can be a Boolean, Double, Integer, JSONArray, JSONObject, Long, or String, or the JSONObject.NULL object.
     *
     * @param propertyValue The value to convert
     * @return The resulting value; either Boolean, Double, Integer, JSONArray, JSONObject, Long, or String, or the JSONObject.NULL object
     */
    private static Object asJSObject(final String propertyValue) {
        if (null == propertyValue) {
            return null;
        }
        try {
            Object value = new JSONTokener(propertyValue).nextValue();
            if (value instanceof String) {
                if (propertyValue.startsWith("\"")) {
                    return value;
                }
                return propertyValue;
            }
            return value;
        } catch (Exception e) {
            return propertyValue;
        }
    }

    /**
     * <ul>
     * <li><b><code>preferencePath</code></b>:<br>
     * &nbsp;&nbsp;The preference path</li>
     * <li><b><code>propertyName</code></b>:<br>
     * &nbsp;&nbsp;The property name of the preference item property.</li>
     * <li><b><code>property</code></b>:<br>
     * &nbsp;&nbsp;The property representing a preference item.</li>
     * <li><b><code>path</code></b>:<br>
     * &nbsp;&nbsp;The parsed preference path.</li>
     * </ul>
     */
    private static final class AttributedProperty {

        /** The property name of the preference item property. */
        protected final String propertyName;

        /** The property representing a preference item. */
        protected final ComposedConfigProperty<String> property;

        /** The parsed preference path. */
        protected final List<JSONPathElement> path;

        /** Whether the associated property is protected */
        protected final boolean isProtected;

        protected AttributedProperty(String preferencePath, String propertyName, ComposedConfigProperty<String> property, boolean isProtected) throws OXException {
            super();
            this.propertyName = propertyName;
            this.property = property;
            this.isProtected = isProtected;
            path = JSONPathElement.parsePath(preferencePath);
        }
    } // End of class AttributedProperty

    private static final class CompletionServiceReference {

        SequentialCompletionService<Void> completionService = null;
        int num = 0;

        /**
         * Initializes a new {@link ConfigJSlobService.CompletionServiceReference}.
         */
        public CompletionServiceReference() {
            super();
        }

    }
}
