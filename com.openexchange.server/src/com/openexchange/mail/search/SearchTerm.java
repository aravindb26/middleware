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

package com.openexchange.mail.search;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.mail.FetchProfile;
import javax.mail.Message;
import com.openexchange.exception.OXException;
import com.openexchange.mail.MailField;
import com.openexchange.mail.dataobjects.MailMessage;

/**
 * {@link SearchTerm}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class SearchTerm<T> implements Serializable {

    private static final long serialVersionUID = -6443057148350714347L;

    /**
     * Initializes a new {@link SearchTerm}
     */
    public SearchTerm() {
        super();
    }

    /**
     * Applies given capabilities to this term.
     *
     * @param capabilities The capabilities to apply
     */
    public final void applyCapabilities(CapabilitiesAndOptions capabilities) {
        applyCapabilities(capabilities, null);
    }

    /**
     * Applies given capabilities to this term.
     *
     * @param capabilities The capabilities to apply
     * @param parentTerm The parent term if any; otherwise <code>null</code>
     * @return <code>true</code> if mail capabilities could be successfully applied; otherwise <code>false</code> if this terms collides with capabilities
     */
    protected boolean applyCapabilities(CapabilitiesAndOptions capabilities, SearchTerm<?> parentTerm) {
        // Assume true
        return true;
    }

    /**
     * Gets the pattern to which the expression should match.
     *
     * @return The pattern
     */
    public abstract T getPattern();

    /**
     * Handles given visitor for this search term.
     *
     * @param visitor The visitor
     */
    public abstract void accept(SearchTermVisitor visitor);

    /**
     * Adds the addressed MailField to specified collection
     *
     * @param col The collection which gathers addressed fields
     */
    public abstract void addMailField(Collection<MailField> col);

    /**
     * Checks if given message matches this search term
     *
     * @param msg The message to check
     * @return <code>true</code> if message matches this search term; otherwise <code>false</code>
     * @throws OXException If check fails
     */
    public abstract boolean matches(Message msg) throws OXException;

    /**
     * Checks if specified mail message matches this search term
     *
     * @param mailMessage The mail message to check
     * @return <code>true</code> if specified mail message matches this search term; otherwise <code>false</code>
     * @throws OXException If checking mail message against search term fails
     */
    public abstract boolean matches(MailMessage mailMessage) throws OXException;

    /**
     * Generates the corresponding <i><a href="http://java.sun.com/products/javamail/">JavaMail</a></i> instance of
     * {@link javax.mail.search.SearchTerm} from this search term
     *
     * @return The corresponding instance of {@link javax.mail.search.SearchTerm}
     */
    public abstract javax.mail.search.SearchTerm getJavaMailSearchTerm();

    /**
     * Generates the corresponding instance of {@link javax.mail.search.SearchTerm} from this search term without any wildcard characters.
     * <p>
     * This is useful to preselect possible positives and to manually filtering out false positives.
     *
     * @return The corresponding instance of {@link javax.mail.search.SearchTerm} without any wildcard characters.
     * @see #containsWildcard()
     */
    public abstract javax.mail.search.SearchTerm getNonWildcardJavaMailSearchTerm();

    /**
     * Contributes this search term's target fetch item to given fetch profile
     *
     * @param fetchProfile The fetch profile
     */
    public abstract void contributeTo(FetchProfile fetchProfile);

    /**
     * Generates a search term with the unsupported search terms specified through <code>filter</code> removed.
     * <p>
     * For each search term contained in this search term the following rule is applied:
     * <ol>
     * <li>If search term is an instance of {@link ORTerm} or {@link ANDTerm} replace the unsupported with:
     * <ul>
     * <li>the neutral element if it is the first element that has to be replaced: {@link BooleanTerm#FALSE} for {@link ORTerm} and
     * {@link BooleanTerm#TRUE} for {@link ANDTerm}</li>
     * <li>the failing element if term's other element has already been replaced to let the whole search term fail:
     * {@link BooleanTerm#FALSE} for both {@link ORTerm} and {@link ANDTerm}</li>
     * </ul>
     * </li>
     * <li>If search term is supported, return the search term itself</li>
     * <li>Otherwise replace with {@link BooleanTerm#FALSE}</li>
     * </ol>
     * <p>
     * <b>Note</b>: Only a shallow copy is generated; meaning further working on this search term may influence return value's search term.
     *
     * @param filter An array containing unsupported classes of {@link SearchTerm} to filter against
     * @return A new search term with the unsupported search terms removed
     */
    public SearchTerm<?> filter(Class<? extends SearchTerm>[] filter) {
        return filter(new HashSet<Class<? extends SearchTerm>>(Arrays.asList(filter)));
    }

    /**
     * Generates a search term with the unsupported search terms specified through <code>filter</code> removed.
     * <p>
     * For each search term contained in this search term the following rule is applied:
     * <ol>
     * <li>If search term is an instance of {@link ORTerm} or {@link ANDTerm} replace the unsupported with:
     * <ul>
     * <li>the neutral element if it is the first element that has to be replaced: {@link BooleanTerm#FALSE} for {@link ORTerm} and
     * {@link BooleanTerm#TRUE} for {@link ANDTerm}</li>
     * <li>the failing element if term's other element has already been replaced to let the whole search term fail:
     * {@link BooleanTerm#FALSE} for both {@link ORTerm} and {@link ANDTerm}</li>
     * </ul>
     * </li>
     * <li>If search term is supported, return the search term itself</li>
     * <li>Otherwise replace with {@link BooleanTerm#FALSE}</li>
     * </ol>
     * <p>
     * <b>Note</b>: Only a shallow copy is generated; meaning further working on this search term may influence return value's search term.
     *
     * @param filterSet The filter set containing classes unsupported search terms
     * @return A new search term with the unsupported search terms removed
     */
    public SearchTerm<?> filter(Set<Class<? extends SearchTerm>> filterSet) {
        if (filterSet.contains(getClass())) {
            return BooleanTerm.FALSE;
        }
        return this;
    }

    /**
     * Checks if this search term's pattern only consists of ASCII 7 bit characters.
     * <p>
     * This method implies that this search is some kind of string search term. Returns <code>true</code> if not appropriate.
     *
     * @return <code>true</code> if search term's pattern only consists of ASCII 7 bit characters; otherwise <code>false</code>
     */
    public boolean isAscii() {
        return true;
    }

    /**
     * Checks if this search term's pattern contains wildcard characters <code>'*'</code> and <code>'?'</code>.
     * <p>
     * This method implies that this search is some kind of string search term. Returns <code>false</code> if not appropriate.
     *
     * @return <code>true</code> if this search term's pattern contains wildcard characters; otherwise <code>false</code>
     */
    public boolean containsWildcard() {
        return false;
    }

    /**
     * Checks whether the specified string only consists of ASCII 7 bit characters.
     *
     * @param s The string to check
     * @return <code>true</code> if string only consists of ASCII 7 bit characters; otherwise <code>false</code>
     */
    protected static final boolean isAscii(String s) {
        final int length = s.length();
        boolean isAscii = true;
        for (int i = 0; i < length && isAscii; i++) {
            isAscii = (s.charAt(i) < 128);
        }
        return isAscii;
    }

    private static final Pattern PAT_SPLIT = Pattern.compile("\\?|\\*");

    /**
     * Gets the largest non-wildcard part out of specified pattern;<br>
     * e.g. <code>&quot;foo*barit?it&quot;</code> would return <code>&quot;barit&quot;</code>.
     * <p>
     * If specified pattern contains no wildcard characters, it is returned unchanged.
     * <p>
     * If specified pattern only consists of wildcard characters, an empty string is returned.
     *
     * @param pattern The pattern possibly containing wildcard characters
     * @return The largest non-wildcard part
     */
    protected static final String getNonWildcardPart(String pattern) {
        final String[] parts = PAT_SPLIT.split(pattern);
        if (parts.length == 0) {
            // Only consists of wildcard characters
            return "";
        }
        if (parts.length == 1) {
            // No wildcard characters
            return parts[0];
        }
        int mlen = -1;
        int index = -1;
        for (int i = 0; i < parts.length; i++) {
            final int len = parts[i].length();
            if (len > mlen) {
                mlen = len;
                index = i;
            }
        }
        return parts[index];
    }

    /**
     * Converts specified pattern into a corresponding regular expression.
     * <p>
     * Any wildcard characters are replaced with appropriate regex characters.
     *
     * @param pattern The wildcard pattern
     * @return The corresponding regular expression
     */
    protected static Pattern toRegex(String pattern) {
        return Pattern.compile(wildcardToRegex(pattern), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Converts specified wildcard string to a regular expression.
     *
     * @param wildcard The wildcard string to convert
     * @return An appropriate regular expression ready for being used in a {@link Pattern pattern}
     */
    private static String wildcardToRegex(String wildcard) {
        final StringBuilder s = new StringBuilder(wildcard.length());
        s.append('^');
        final int len = wildcard.length();
        for (int i = 0; i < len; i++) {
            final char c = wildcard.charAt(i);
            if (c == '*') {
                s.append(".*");
            } else if (c == '?') {
                s.append('.');
            } else if (c == '(' || c == ')' || c == '[' || c == ']' || c == '$' || c == '^' || c == '.' || c == '{' || c == '}' || c == '|' || c == '\\') {
                /*
                 * Escape special regular expression characters
                 */
                s.append('\\');
                s.append(c);
            } else {
                s.append(c);
            }
        }
        s.append('$');
        return (s.toString());
    }

}
