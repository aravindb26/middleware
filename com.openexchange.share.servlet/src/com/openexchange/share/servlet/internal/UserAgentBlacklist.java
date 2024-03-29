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

package com.openexchange.share.servlet.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.openexchange.java.Strings;

/**
 * {@link UserAgentBlacklist}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.4
 */
public class UserAgentBlacklist {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(UserAgentBlacklist.class);

    private static interface Tokens {

        /**
         * Gets the number of tokens.
         *
         * @return The number of tokens
         */
        int size();

        /**
         * Checks if all tokens from this instance are contained in given string.
         *
         * @param toCheck The string that possibly contains all tokens
         * @return <code>true</code> if all tokens are contained; otherwise <code>false</code>
         */
        boolean allContainedIn(String toCheck);
    }

    private static final class DefaultTokens implements Tokens {

        private final List<String> tokens;

        /**
         * Initializes a new {@link DefaultTokens}.
         *
         * @param tokens The contained token collection
         */
        DefaultTokens(List<String> tokens) {
            super();
            this.tokens = tokens;
        }

        @Override
        public int size() {
            return tokens.size();
        }

        @Override
        public boolean allContainedIn(String toCheck) {
            boolean allContained = true;
            for (int i = tokens.size(); allContained && i-- > 0;) {
                if (toCheck.indexOf(tokens.get(i)) < 0) {
                    allContained = false;
                }
            }
            return allContained;
        }

        @Override
        public String toString() {
            return tokens.toString();
        }
    }

    private static final class SingletonTokens implements Tokens {

        private final String token;

        /**
         * Initializes a new {@link SingletonTokens}.
         *
         * @param token The singleton token
         */
        SingletonTokens(String token) {
            super();
            this.token = token;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean allContainedIn(String toCheck) {
            return toCheck.indexOf(token) >= 0;
        }

        @Override
        public String toString() {
            return token;
        }
    }

    /**
     * The default User-Agent black-list based on <a href="https://perishablepress.com/list-all-user-agents-top-search-engines/">this article</a>.
     */
    public static UserAgentBlacklist DEFAULT_BLACKLIST = new UserAgentBlacklist(
        "*aolbuild*, *baiduspider*, *baidu*search*, *bingbot*, *bingpreview*, *msnbot*, *duckduckgo*, *adsbot-google*, *googlebot*, *mediapartners-google*, *teoma*, *slurp*, *yandex*bot*", true);

    // ----------------------------------------------------------------------------------------------------------------

    private final ImmutableMap<Matcher, Matcher> map;
    private final Cache<String, Boolean> deniedCache;

    /**
     * Initializes a new {@link UserAgentBlacklist}.
     *
     * @param wildcardPatterns The comma-separated list of User-Agent expressions
     * @param ignoreCase Whether to ignore-case match against possible User-Agent identifiers
     */
    public UserAgentBlacklist(String wildcardPatterns, boolean ignoreCase) {
        super();

        String[] wps = Strings.splitByComma(wildcardPatterns);
        List<Matcher> blacklistMatchers = new ArrayList<>(wps.length);
        List<Tokens> containees = new ArrayList<>(wps.length);
        for (String wildcardPattern : wps) {
            if (Strings.isNotEmpty(wildcardPattern)) {
                String expr = wildcardPattern.trim();
                try {
                    String unquoted = removeQuotes(expr);
                    if (Strings.isEmpty(unquoted)) {
                        LOG.warn("Ignoring empty pattern expression: {}", expr);
                    } else {
                        Tokens tokens = isContainsMatcher(unquoted, ignoreCase);
                        if (null == tokens) {
                            Pattern pattern = Pattern.compile(Strings.wildcardToRegex(unquoted), ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
                            blacklistMatchers.add(new PatternMatcher(pattern));
                        } else {
                            containees.add(tokens);
                        }
                    }
                } catch (PatternSyntaxException e) {
                    LOG.warn("Ignoring invalid pattern expression: {}", expr, e);
                }
            }
        }

        if (false == containees.isEmpty()) {
            blacklistMatchers.add(0, new ContainsMatcher(ignoreCase, containees));
        }

        ImmutableMap.Builder<Matcher, Matcher> map = ImmutableMap.builder();
        for (Matcher blacklistMatcher : blacklistMatchers) {
            map.put(blacklistMatcher, blacklistMatcher);
        }
        this.map = map.build();

        deniedCache = CacheBuilder.newBuilder().maximumSize(65536).expireAfterAccess(30, TimeUnit.MINUTES).build();
    }

    private static Tokens isContainsMatcher(String expr, boolean ignoreCase) {
        int len = expr.length();
        if (len <= 2) {
            return null;
        }

        if (expr.charAt(0) != '*' || expr.charAt(len - 1) != '*') {
            return null;
        }

        String[] tokens = Strings.splitBy(expr, '*', true);

        int numTokens = tokens.length;
        if (numTokens == 0) {
            return null;
        }

        List<String> list = new ArrayList<>(numTokens);
        for (String token : tokens) {
            if (Strings.isNotEmpty(token)) {
                for (int i = token.length() - 1; i-- > 1;) {
                    char ch = token.charAt(i);
                    if (false == Strings.isAsciiLetter(ch) && ch != ' ' && ch != '-' && ch != '_') {
                        return null;
                    }
                }
                list.add(ignoreCase ? Strings.asciiLowerCase(token) : token);
            }
        }

        int size = list.size();
        if (0 == size) {
            return null;
        }

        return 1 == size ? new SingletonTokens(list.get(0)) : new DefaultTokens(list);
    }

    /**
     * Checks if this black-list is empty.
     *
     * @return <code>true</code> if this black-list is empty; otherwise <code>false</code>
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Checks if specified User-Agent identifier is matched by one of contained black-list patterns.
     *
     * @param userAgent The User-Agent identifier
     * @return <code>true</code> if specified User-Agent identifier is black-listed; otherwise <code>false</code>
     */
    public boolean isBlacklisted(final String userAgent) {
        if (null == userAgent) {
            return false;
        }

        Boolean cached = deniedCache.getIfPresent(userAgent);
        if (null != cached) {
            return cached.booleanValue();
        }

        Callable<Boolean> loader = new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                return Boolean.valueOf(doCheckBlacklisted(userAgent));
            }
        };

        try {
            return deniedCache.get(userAgent, loader).booleanValue();
        } catch (ExecutionException e) {
            // Cannot occur
            LOG.error("Failed to check User-Agent: {}", userAgent, e.getCause());
            return false;
        }
    }

    boolean doCheckBlacklisted(String userAgent) {
        for (Matcher matcher : map.keySet()) {
            if (matcher.matches(userAgent)) {
                return true;
            }
        }
        return false;
    }

    /*-
     * ------------------------------------- HELPERS -----------------------------------------------
     */

    private static interface Matcher {

        boolean matches(String userAgent);
    }

    private static final class PatternMatcher implements Matcher {

        private final Pattern pattern;

        PatternMatcher(Pattern pattern) {
            super();
            this.pattern = pattern;
        }

        @Override
        public boolean matches(String userAgent) {
            return pattern.matcher(userAgent).matches();
        }

        @Override
        public String toString() {
            return pattern.toString();
        }
    }

    private static final class ContainsMatcher implements Matcher {

        private final List<Tokens> containees;
        private final boolean ignoreCase;

        ContainsMatcher(boolean ignoreCase, List<Tokens> containees) {
            super();
            ImmutableList.Builder<Tokens> builder = ImmutableList.builder();
            for (Tokens contained : containees) {
                builder.add(contained);
            }
            this.containees = builder.build();
            this.ignoreCase = ignoreCase;
        }

        @Override
        public boolean matches(String userAgent) {
            String toCheck = ignoreCase ? Strings.asciiLowerCase(userAgent) : userAgent;
            for (Tokens contained : containees) {
                if (contained.allContainedIn(toCheck)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return containees.toString();
        }
    }

    /**
     * Removes possible surrounding quotes.
     *
     * @param quoted The possibly quoted string
     * @return The unquoted string
     */
    private static String removeQuotes(final String quoted) {
        if (quoted.length() < 2 || quoted.charAt(0) != '"') {
            return quoted;
        }
        String retval = quoted.substring(1);
        final int end = retval.length() - 1;
        if (retval.charAt(end) == '"') {
            retval = retval.substring(0, end);
        }
        return retval;
    }

}
