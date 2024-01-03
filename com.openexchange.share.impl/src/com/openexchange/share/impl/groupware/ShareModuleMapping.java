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

package com.openexchange.share.impl.groupware;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import com.openexchange.config.ConfigurationService;
import com.openexchange.groupware.modules.Module;
import com.openexchange.java.Strings;

/**
 * {@link ShareModuleMapping}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.8.0
 */
public class ShareModuleMapping {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ShareModuleMapping.class);
    }

    private static final AtomicReference<Map<Integer, String>> MODULE_MAPPING2_STRING_REF = new AtomicReference<>();
    private static final AtomicReference<Map<String, Integer>> MODULE_MAPPING2_INT_REF = new AtomicReference<>();
    private static final Module[] GROUPWARE_MODULES = { Module.CALENDAR, Module.TASK, Module.CONTACTS, Module.INFOSTORE, Module.MAIL };

    /**
     * Initializes a new {@link ShareModuleMapping}.
     */
    private ShareModuleMapping() {
        super();
    }

    public static synchronized void init(ConfigurationService configService) {
        if (MODULE_MAPPING2_STRING_REF.get() == null) {
            /*
             * map custom modules
             */
            ImmutableMap.Builder<Integer, String> moduleMapping2String = ImmutableMap.builder();
            ImmutableMap.Builder<String, Integer> moduleMapping2Int = ImmutableMap.builder();
            String mapping = configService.getProperty("com.openexchange.share.modulemapping");
            try {
                if (null != mapping && !"".equals(mapping) && !mapping.isEmpty()) {
                    for (String module : Strings.splitByComma(mapping)) {
                        String[] mods = Strings.splitBy(module, '=', true);
                        String moduleName = mods[0];
                        String moduleId = mods[1];
                        mods = null;
                        moduleMapping2Int.put(moduleName, Integer.valueOf(moduleId));
                        moduleMapping2String.put(Integer.valueOf(moduleId), moduleName);
                    }
                }
            } catch (RuntimeException e) {
                LoggerHolder.LOG.error("Invalid value for property \"com.openexchange.share.modulemapping\": {}", e);
            }
            /*
             * map available groupware modules
             */
            for (Module module : GROUPWARE_MODULES) {
                moduleMapping2Int.put(module.getName(), I(module.getFolderConstant()));
                moduleMapping2String.put(I(module.getFolderConstant()), module.getName());
            }
            MODULE_MAPPING2_STRING_REF.set(moduleMapping2String.build());
            MODULE_MAPPING2_INT_REF.set(moduleMapping2Int.build());
        }
    }

    /**
     * Gets a collection of the available share module identifiers.
     *
     * @return The module identifiers
     */
    public static Set<Integer> getModuleIDs() {
        Map<Integer, String> moduleMapping2String = MODULE_MAPPING2_STRING_REF.get();
        return moduleMapping2String == null ? Collections.emptySet() : moduleMapping2String.keySet();
    }

    public static int moduleMapping2int(String moduleName) {
        Map<String, Integer> moduleMapping2Int = MODULE_MAPPING2_INT_REF.get();
        if (moduleMapping2Int == null) {
            LoggerHolder.LOG.warn("share module mapping has not been initialized!");
            return -1;
        }
        Integer ix = moduleMapping2Int.get(moduleName);
        return ix != null ? i(ix) : -1;
    }

    public static String moduleMapping2String(int module) {
        Map<Integer, String> moduleMapping2String = MODULE_MAPPING2_STRING_REF.get();
        if (moduleMapping2String == null) {
            LoggerHolder.LOG.warn("share module mapping has not been initialized!");
            return Module.UNBOUND.name();
        }
        String moduleId = moduleMapping2String.get(I(module));
        return moduleId != null ? moduleId : Module.UNBOUND.name();
    }
}
