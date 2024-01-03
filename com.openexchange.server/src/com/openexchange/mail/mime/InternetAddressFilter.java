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

package com.openexchange.mail.mime;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.mail.internet.InternetAddress;
import org.slf4j.Logger;
import com.openexchange.java.Strings;

/**
 * {@link InternetAddressFilter} - An address filter.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class InternetAddressFilter implements Iterable<InternetAddress> {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(InternetAddressFilter.class);
    }

    private final Set<InternetAddress> addresses;

    /**
     * Initializes a new {@link InternetAddressFilter} without insertion order and default initial capacity.
     */
    public InternetAddressFilter() {
        this(false, 16);
    }

    /**
     * Initializes a new {@link InternetAddressFilter}.
     *
     * @param keepOrder Whether to keep insertion order
     * @param initialCapacity The initial capacity of this set
     */
    public InternetAddressFilter(boolean keepOrder, int initialCapacity) {
        super();
        addresses = keepOrder ? new LinkedHashSet<InternetAddress>(initialCapacity) : new HashSet<InternetAddress>(initialCapacity);
    }

    /**
     * Gets the size of this filter.
     *
     * @return The size
     */
    public int size() {
        return addresses.size();
    }

    /**
     * Checks if this filter is empty.
     *
     * @return <code>true</code> if empty; otherwise <code>false</code>
     */
    public boolean isEmpty() {
        return addresses.isEmpty();
    }

    /**
     * Returns {@code true} if this filter contains the specified element.
     * More formally, returns {@code true} if and only if this set
     * contains an element {@code e} such that
     * {@code Objects.equals(o, e)}.
     *
     * @param o element whose presence in this set is to be tested
     * @return {@code true} if this set contains the specified element
     * @throws ClassCastException if the type of the specified element
     *             is incompatible with this set
     *             (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *             set does not permit null elements
     *             (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    public boolean contains(Object o) {
        return addresses.contains(o) || ((o instanceof InternetAddress) && containsAlternativeAddress((InternetAddress) o, addresses));
    }

    /**
     * Returns an iterator over the elements in this filter. The elements are
     * returned in no particular order (unless this set is an instance of some
     * class that provides a guarantee).
     *
     * @return an iterator over the elements in this set
     */
    @Override
    public Iterator<InternetAddress> iterator() {
        return addresses.iterator();
    }

    /**
     * Returns an array containing all of the elements in this filter; the
     * runtime type of the returned array is that of the specified array.
     * If the set fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of this set.
     *
     * <p>If this filter fits in the specified array with room to spare
     * (i.e., the array has more elements than this filter), the element in
     * the array immediately following the end of the filter is set to
     * {@code null}. (This is useful in determining the length of this
     * set <i>only</i> if the caller knows that this set does not contain
     * any null elements.)
     *
     * <p>If this filter makes any guarantees as to what order its elements
     * are returned by its iterator, this method must return the elements
     * in the same order.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs. Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a set known to contain only strings.
     * The following code can be used to dump the set into a newly allocated
     * array of {@code String}:
     *
     * <pre>
     * String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of this set are to be
     *            stored, if it is big enough; otherwise, a new array of the same
     *            runtime type is allocated for this purpose.
     * @return an array containing all the elements in this set
     * @throws ArrayStoreException if the runtime type of the specified array
     *             is not a supertype of the runtime type of every element in this
     *             set
     * @throws NullPointerException if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        return addresses.toArray(a);
    }

    /**
     * Adds the specified element to this filter if it is not already present
     * (optional operation). More formally, adds the specified element
     * {@code e} to this filter if the set contains no element {@code e2}
     * such that
     * {@code Objects.equals(e, e2)}.
     * If this filter already contains the element, the call leaves the filter
     * unchanged and returns {@code false}. In combination with the
     * restriction on constructors, this ensures that sets never contain
     * duplicate elements.
     *
     * <p>The stipulation above does not imply that sets must accept all
     * elements; sets may refuse to add any particular element, including
     * {@code null}, and throw an exception, as described in the
     * specification for {@link Collection#add Collection.add}.
     * Individual set implementations should clearly document any
     * restrictions on the elements that they may contain.
     *
     * @param e element to be added to this set
     * @return {@code true} if this set did not already contain the specified
     *         element
     * @throws UnsupportedOperationException if the {@code add} operation
     *             is not supported by this set
     * @throws ClassCastException if the class of the specified element
     *             prevents it from being added to this set
     * @throws NullPointerException if the specified element is null and this
     *             set does not permit null elements
     * @throws IllegalArgumentException if some property of the specified element
     *             prevents it from being added to this set
     */
    public boolean add(InternetAddress e) {
        return addresses.add(e) || addAlternativeAddressIfPossible(e, addresses);
    }

    /**
     * Clears this filter's elements.
     */
    public void clear() {
        addresses.clear();
    }

    /**
     * Returns a sequential {@code Stream} with this collection as its source.
     *
     * <p>This method should be overridden when the {@link #spliterator()}
     * method cannot return a spliterator that is {@code IMMUTABLE},
     * {@code CONCURRENT}, or <em>late-binding</em>. (See {@link #spliterator()}
     * for details.)
     *
     * @implSpec
     *           The default implementation creates a sequential {@code Stream} from the
     *           collection's {@code Spliterator}.
     *
     * @return a sequential {@code Stream} over the elements in this collection
     */
    public Stream<InternetAddress> stream() {
        return addresses.stream();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static InternetAddress optAlternativeAddress(InternetAddress a) {
        String addr = a.getAddress();
        String address = Strings.asciiLowerCase(addr);
        if (address.endsWith("@gmail.com")) {
            return newAlternativeAddress("googlemail.com", addr, a.getPersonal());
        }
        if (address.endsWith("@googlemail.com")) {
            return newAlternativeAddress("gmail.com", addr, a.getPersonal());
        }
        return null;
    }

    private static InternetAddress newAlternativeAddress(String domainPart, String addr, String personal) {
        QuotedInternetAddress alt = new QuotedInternetAddress();

        // Take over possible personal
        if (personal != null) {
            try {
                alt.setPersonal(personal, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Cannot occur
                LoggerHolder.LOG.debug("Unsupported character encoding. Ignoring personal: {}", personal, e);
            }
        }

        // Compose new address through keeping local part and using given domain part
        alt.setAddress(new StringBuilder(addr.substring(0, addr.indexOf('@'))).append('@').append(domainPart).toString());
        return alt;
    }

    private static boolean containsAlternativeAddress(InternetAddress a, Set<InternetAddress> addresses) {
        InternetAddress alt = optAlternativeAddress(a);
        return alt == null ? false : addresses.contains(alt);
    }

    private static boolean addAlternativeAddressIfPossible(InternetAddress a, Set<InternetAddress> addresses) {
        InternetAddress alt = optAlternativeAddress(a);
        return alt == null ? false : addresses.add(alt);
    }

    /**
     * Filters given address array against given filter set. All addresses currently contained in filter set are removed from specified
     * <code>addrs</code> and all addresses not contained in filter set are added to filter set for future invocations.
     *
     * @param filter The current address filter
     * @param addrs The address list to filter
     * @return The filtered set of addresses
     */
    public static Set<InternetAddress> filter(InternetAddressFilter filter, final InternetAddress[] addrs) {
        if (addrs == null) {
            return new HashSet<InternetAddress>(0);
        }
        final Set<InternetAddress> set = new LinkedHashSet<InternetAddress>(Arrays.asList(addrs));
        /*
         * Remove all addresses from set which are contained in filter
         */
        for (InternetAddress a : filter) {
            set.remove(a);
        }
        /*
         * Add new addresses to filter
         */
        for (InternetAddress a : set) {
            filter.add(a);
        }
        return set;
    }

}
