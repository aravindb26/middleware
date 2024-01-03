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


package com.openexchange.file.storage.mail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViews;
import com.openexchange.file.storage.mail.filter.MailDriveFile;
import com.openexchange.file.storage.mail.filter.MailDriveFileFilter;
import com.openexchange.file.storage.mail.osgi.Services;
import com.openexchange.java.Strings;
import com.openexchange.session.Session;
import com.openexchange.session.UserAndContext;

/**
 * {@link MailDriveFileFilters} - Utility class for mail drive file filter.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class MailDriveFileFilters {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MailDriveFileFilters.class);

    /**
     * Initializes a new {@link MailDriveFileFilters}.
     */
    private MailDriveFileFilters() {
        super();
    }

    /** The filter accepting all mail drive files */
    public static final MailDriveFileFilter ACCEPT_ALL = new MailDriveFileFilter() {

        @Override
        public boolean accepts(MailDriveFile mailDriveFile, Session session) {
            return true;
        }
    };

    private static final Cache<UserAndContext, MailDriveFileFilter> CACHED_FILTERS = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

    /**
     * Gets the mail drive file filter for given session.
     *
     * @param session The session providing user information
     * @return The mail drive file filter
     */
    public static MailDriveFileFilter getFilterFor(Session session) {
        UserAndContext key = UserAndContext.newInstance(session);
        MailDriveFileFilter filter = CACHED_FILTERS.getIfPresent(key);
        if (filter != null) {
            return filter;
        }

        try {
            return CACHED_FILTERS.get(key, () -> {
                List<MailDriveFileFilter> filters = null;
                {
                    Collection<MailDriveFileFilter> registeredFilters = REGISTERED_FILTERS.values();
                    if (!registeredFilters.isEmpty()) {
                        filters = new ArrayList<>(registeredFilters);
                    }
                }
                {
                    Optional<List<MailDriveFileFilter>> configuredFilters = getConfiguredFilters(session);
                    if (configuredFilters.isPresent()) {
                        if (filters == null) {
                            filters = new ArrayList<>(configuredFilters.get());
                        } else {
                            filters.addAll(configuredFilters.get());
                        }
                    }
                }
                return filters == null ? ACCEPT_ALL : new CompositeMailDriveFileFilter(filters);
            });
        } catch (ExecutionException e) {
            LOG.warn("Failed to obtain mail drive file filter for user {} in context {}", Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()), e.getCause() == null ? e : e.getCause());
            return ACCEPT_ALL;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private static Optional<List<MailDriveFileFilter>> getConfiguredFilters(Session session) {
        ConfigViewFactory factory = Services.getOptionalService(ConfigViewFactory.class);
        if (factory == null) {
            return Optional.empty();
        }

        try {
            ConfigView view = factory.getView(session.getUserId(), session.getContextId());
            String filterExpressions = ConfigViews.getNonEmptyPropertyFrom("com.openexchange.file.storage.mail.fileFilter", view);
            if (filterExpressions == null) {
                return Optional.empty();
            }

            List<MailDriveFileFilter> filters = null;
            String[] patterns = Strings.splitByCommaNotInQuotes(filterExpressions);
            for (String filterExpression : patterns) {
                if (Strings.isNotEmpty(filterExpression)) {
                    String regex = Strings.wildcardToRegex(filterExpression);
                    Optional<Pattern> optPattern = compileToPattern(regex, filterExpression, session);
                    if (optPattern.isPresent()) {
                        Pattern pattern = optPattern.get();
                        if (filters == null) {
                            filters = new ArrayList<>(patterns.length);
                        }
                        filters.add(new FileNamePatternMailDriveFileFilter(pattern));
                    }
                }
            }
            return filters == null ? Optional.empty() : Optional.of(filters);
        } catch (Exception e) {
            LOG.warn("Failed to build mail drive file filter from configuration for user {} in context {}", Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()), e);
            return Optional.empty();
        }
    }

    private static Optional<Pattern> compileToPattern(String regex, String filterExpression, Session session) {
        try {
            return Optional.of(Pattern.compile(regex));
        } catch (Exception e) {
            LOG.warn("Discarding mail drive file filter with configured pattern \"{}\" (\"{}\") for user {} in context {}", filterExpression, regex, Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()), e);
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private static final ConcurrentMap<Class<? extends MailDriveFileFilter>, MailDriveFileFilter> REGISTERED_FILTERS = new ConcurrentHashMap<>();

    /**
     * Registers given mail drive file filter.
     *
     * @param filter The filter to register
     * @return <code>true</code> if successfully registered; otherwise <code>false</code>
     */
    public static boolean registerMailDriveFileFilter(MailDriveFileFilter filter) {
        return filter != null && REGISTERED_FILTERS.putIfAbsent(filter.getClass(), filter) == null;
    }

    /**
     * Unregisters given mail drive file filter.
     *
     * @param filter The filter to unregister
     * @return <code>true</code> if successfully unregistered; otherwise <code>false</code>
     */
    public static boolean unregisterMailDriveFileFilter(MailDriveFileFilter filter) {
        return filter != null && REGISTERED_FILTERS.remove(filter.getClass()) != null;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private static class FileNamePatternMailDriveFileFilter implements MailDriveFileFilter {

        private final Pattern pattern;

        FileNamePatternMailDriveFileFilter(Pattern pattern) {
            super();
            this.pattern = pattern;
        }

        @Override
        public boolean accepts(MailDriveFile mailDriveFile, Session session) {
            String fileName = mailDriveFile.getFileName();
            return fileName == null || pattern.matcher(fileName).matches();
        }
    }

    private static class CompositeMailDriveFileFilter implements MailDriveFileFilter {

        private final List<MailDriveFileFilter> filters;

        /**
         * Initializes a new {@link CompositeMailDriveFileFilter}.
         *
         * @param filters The collection of filters
         */
        CompositeMailDriveFileFilter(Collection<MailDriveFileFilter> filters) {
            super();
            this.filters = List.copyOf(filters);
        }

        @Override
        public boolean accepts(MailDriveFile mailDriveFile, Session session) {
            for (MailDriveFileFilter filter : filters) {
                if (!filter.accepts(mailDriveFile, session)) {
                    return false;
                }
            }
            return true;
        }
    } // End of class CompositeMailDriveFileFilter

}
