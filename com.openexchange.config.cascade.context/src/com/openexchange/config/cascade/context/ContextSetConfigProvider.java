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

package com.openexchange.config.cascade.context;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.BasicProperty;
import com.openexchange.config.cascade.ConfigCascadeExceptionCodes;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViewScope;
import com.openexchange.config.cascade.ReinitializableConfigProviderService;
import com.openexchange.config.cascade.context.matching.ContextSetTerm;
import com.openexchange.config.cascade.context.matching.ContextSetTermParser;
import com.openexchange.config.cascade.context.matching.UserConfigurationAnalyzer;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.userconfiguration.UserConfigurationCodes;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.java.Strings;
import com.openexchange.reseller.ResellerService;
import com.openexchange.reseller.data.ResellerTaxonomy;
import com.openexchange.server.ServiceLookup;
import com.openexchange.userconf.UserPermissionService;

/**
 * {@link ContextSetConfigProvider}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ContextSetConfigProvider extends AbstractContextBasedConfigProvider implements ReinitializableConfigProviderService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ContextSetConfigProvider.class);

    private static final String TAXONOMY_TYPES = "taxonomy/types";

    private static final String SCOPE = ConfigViewScope.CONTEXT_SETS.getScopeName();

    private static final String TYPE_PROPERTY = "com.openexchange.config.cascade.types";

    // -----------------------------------------------------------------------------------------------------

    private final Queue<ContextSetConfig> contextSetConfigs;
    private final Queue<AdditionalPredicates> additionalPredicates;
    private final UserConfigurationAnalyzer userConfigAnalyzer;

    /**
     * Initializes a new {@link ContextSetConfigProvider}.
     *
     * @param services The service look-up
     */
    public ContextSetConfigProvider(ServiceLookup services) {
        super(services);
        userConfigAnalyzer = new UserConfigurationAnalyzer();
        contextSetConfigs = new ConcurrentLinkedQueue<>();
        additionalPredicates = new ConcurrentLinkedQueue<>();
        init();
    }

    private final void init() {
        ConfigurationService config = services.getService(ConfigurationService.class);
        Map<String, Object> yamlInFolder = config.getYamlInFolder("contextSets");
        if (yamlInFolder != null) {
            prepare(yamlInFolder);
        }
    }

    @Override
    public void reinit() throws OXException {
        contextSetConfigs.clear();
        additionalPredicates.clear();
        init();
    }

    protected Set<String> getSpecification(Context context, Optional<UserPermissionBits> optPerms) throws OXException {
        // Gather available tags
        final Set<String> tags = new HashSet<>(64);

        // Tags from the reseller stack
        ResellerService resellerService = services.getOptionalService(ResellerService.class);
        if (resellerService != null) {
            for (ResellerTaxonomy resellerTaxonomy : resellerService.getTaxonomiesByContext(context.getContextId())) {
                tags.add(resellerTaxonomy.getTaxonomy());
            }
        }

        // Special tag that applies to the context
        tags.add(context.getName());
        tags.add(Integer.toString(context.getContextId()));

        // The ones from context attributes
        {
            List<String> typeValues = context.getAttributes().get(TAXONOMY_TYPES);
            if (typeValues == null) {
                typeValues = Collections.emptyList();
            }
            for (String string : typeValues) {
                tags.addAll(Arrays.asList(Strings.splitByComma(string)));
            }
        }

        // The ones from user configuration
        if (optPerms.isPresent()) {
            tags.addAll(userConfigAnalyzer.getTags(optPerms.get()));
        }

        // Now let's try modifications by cascade, first those below the contextSet level
        ConfigViewFactory configViews = services.getService(ConfigViewFactory.class);
        final ConfigView view = configViews.getView(optPerms.isPresent() ? optPerms.get().getUserId() : NO_USER, context.getContextId());

        String[] searchPath = configViews.getSearchPath();
        for (String scope : searchPath) {
            if (!SCOPE.equals(scope)) {
                String types = view.property(TYPE_PROPERTY, String.class).precedence(scope).get();
                if (types != null) {
                    tags.addAll(Arrays.asList(Strings.splitByComma(types)));
                }
            }
        }

        // Add additional predicates. Do so until no modification has been done
        boolean goOn = true;
        while (goOn) {
            goOn = false;
            for (final AdditionalPredicates additional : additionalPredicates) {
                goOn = goOn || additional.apply(tags);
            }
        }

        return tags;
    }

    @Override
    protected BasicProperty get(String propertyName, Context context, int userId) throws OXException {
        List<Map<String, Object>> config = getConfigData(getSpecification(context, optUserPermissionBits(context, userId)));

        final String value = findFirst(config, propertyName);
        return new ContextSetBasicProperty(propertyName, value, SCOPE);
    }

    private Optional<UserPermissionBits> optUserPermissionBits(Context ctx, int userId) throws OXException {
        if (userId == NO_USER) {
            return Optional.empty();
        }
        try {
            UserPermissionService userPermissions = services.getService(UserPermissionService.class);
            return Optional.ofNullable(userPermissions.getUserPermissionBits(userId, ctx));
        } catch (OXException e) {
            if (false == UserConfigurationCodes.NOT_FOUND.equals(e)) {
                throw e;
            }

            // No such user
            return Optional.empty();
        }
    }

    @Override
    protected Collection<String> getAllPropertyNamesFor(Context context, int userId) throws OXException {
        Set<String> tags = getSpecification(context, optUserPermissionBits(context, userId));
        Set<String> allNames = new HashSet<>();
        boolean somethingAdded = false;
        for (ContextSetConfig c : contextSetConfigs) {
            if (c.matches(tags)) {
                somethingAdded = allNames.addAll(c.getConfiguration().keySet());
            }
        }
        if (false == somethingAdded) {
            return Collections.emptyList();
        }
        allNames.remove("withTags");
        allNames.remove("addTags");
        return allNames;
    }

    protected String findFirst(List<Map<String, Object>> configData, String propertyName) {
        for (Map<String, Object> map : configData) {
            Object object = map.get(propertyName);
            if (object != null) {
                return object.toString();
            }
        }
        return null;
    }

    protected List<Map<String, Object>> getConfigData(Set<String> tags) {
        List<Map<String, Object>> retval = new LinkedList<>();
        for (ContextSetConfig c : contextSetConfigs) {
            if (c.matches(tags)) {
                retval.add(c.getConfiguration());
            }
        }
        return retval;
    }

    @SuppressWarnings("cast")
    protected void prepare(Map<String, Object> yamlFiles) {
        ContextSetTermParser parser = new ContextSetTermParser();
        for (Map.Entry<String, Object> file : yamlFiles.entrySet()) {
            String filename = file.getKey();

            try {
                @SuppressWarnings("unchecked") Map<Object, Map<String, Object>> content = (Map<Object, Map<String, Object>>) file.getValue();
                if (content == null) {
                    LOG.info("File {} is empty and will be ignored for contextSet configuration.", filename);
                    continue;
                }
                for (Map.Entry<Object, Map<String, Object>> configData : content.entrySet()) {
                    Object configName = configData.getKey();

                    // Check value validity
                    {
                        Object value = configData.getValue();
                        if (!(configData.getValue() instanceof Map)) {
                            throw new IllegalArgumentException("Invalid value. Expected " + Map.class.getName() + ", but was " + (null == value ? "null" : value.getClass().getName()) + ". Please check syntax of file " + filename);
                        }
                    }

                    Map<String, Object> configuration = configData.getValue();

                    Object withTags = configuration.get("withTags");
                    if (withTags == null) {
                        throw new IllegalArgumentException("Missing withTags specification in configuration " + configName + " in file " + filename);
                    }

                    try {
                        ContextSetTerm term = parser.parse(withTags.toString());
                        contextSetConfigs.add(new ContextSetConfig(term, configuration));
                        Object addTags = configuration.get("addTags");
                        if (addTags != null) {
                            final String additional = addTags.toString();
                            final List<String> additionalList = Arrays.asList(Strings.splitByComma(additional));
                            additionalPredicates.add(new AdditionalPredicates(term, additionalList));
                        }
                    } catch (IllegalArgumentException x) {
                        throw new IllegalArgumentException("Could not parse withTags expression '" + withTags + "' in configuration " + configName + " in file " + filename, x);
                    }
                }
            } catch (IllegalArgumentException x) {
                throw x;
            } catch (RuntimeException x) {
                throw new IllegalArgumentException("Failed to process file " + filename + " due to error: " + x.getMessage(), x);
            }
        }
    }

    @Override
    public String getScope() {
        return ConfigViewScope.CONTEXT_SETS.getScopeName();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class ContextSetBasicProperty implements BasicProperty {

        private final String value;
        private final String scope;
        private final String propertyName;

        ContextSetBasicProperty(String propertyName, String value, String scope) {
            this.value = value;
            this.scope = scope;
            this.propertyName = propertyName;
        }

        @Override
        public String get() {
            return value;
        }

        @Override
        public String get(final String metadataName) {
            return null;
        }

        @Override
        public boolean isDefined() {
            return value != null;
        }

        @Override
        public void set(String value) throws OXException {
            throw ConfigCascadeExceptionCodes.CAN_NOT_SET_PROPERTY.create(propertyName, scope);
        }

        @Override
        public void set(String metadataName, String value) throws OXException {
            throw ConfigCascadeExceptionCodes.CAN_NOT_DEFINE_METADATA.create(metadataName, scope);
        }

        @Override
        public List<String> getMetadataNames() throws OXException {
            return Collections.emptyList();
        }
    }

    private static class ContextSetConfig {

        private final ContextSetTerm term;
        private final Map<String, Object> configuration;

        public ContextSetConfig(ContextSetTerm term, Map<String, Object> configuration) {
            super();
            this.term = term;
            this.configuration = configuration;
        }

        public boolean matches(Set<String> tags) {
            return term.matches(tags);
        }

        public Map<String, Object> getConfiguration() {
            return configuration;
        }

    }

    private static class AdditionalPredicates {

        private final ContextSetTerm term;
        private final List<String> additionalTags;

        public AdditionalPredicates(ContextSetTerm term, List<String> additionalTags) {
            super();
            this.term = term;
            this.additionalTags = additionalTags;
        }

        public boolean apply(Set<String> terms) {
            if (term.matches(terms)) {
                return terms.addAll(additionalTags);
            }
            return false;
        }

    }

}
