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

package com.openexchange.java;

/**
 * {@link LimitedStringBuilder} - Designed for use as a drop-in replacement for <code>StringBuffer</code>/<code>StringBuilder</code>, but
 * with a limited capacity. Exceeding the limit will raise a {@link LimitExceededException} runtime exception.
 * <p>
 * Moreover this class only supports the <code>append()</code> methods.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class LimitedStringBuilder implements Appendable, CharSequence {

    private final int limit;
    private final StringBuilder sb;

    /**
     * Initializes a new {@link LimitedStringBuilder}.
     *
     * @param limit The max. number of characters that may be appended to this string builder instance
     */
    public LimitedStringBuilder(final int limit) {
        this(limit + 8, limit);
    }

    /**
     * Initializes a new {@link LimitedStringBuilder}.
     *
     * @param capacity The initial capacity
     * @param limit The max. number of characters that may be appended to this string builder instance
     */
    public LimitedStringBuilder(int capacity, int limit) {
        super();
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must not be less than 0 (zero)");
        }
        this.limit = limit;
        this.sb = new StringBuilder(capacity);
    }

    /**
     * Gets the max. number of characters that may be appended to this string builder instance.
     *
     * @return The max. number of characters
     */
    public int getLimit() {
        return limit;
    }

    @Override
    public int length() {
        return sb.length();
    }

    /**
     * Appends abbreviating dots character sequence.
     */
    public LimitedStringBuilder appendDots() {
        sb.append("...");
        return this;
    }

    private static LimitExceededException newLimitExceededException(int limit) {
        return new LimitExceededException("Exceeded limit of " + limit + " characters.");
    }

    private void checkLimit(final CharSequence s) {
        if (sb.length() + s.length() > limit) {
            throw newLimitExceededException(limit);
        }
    }

    /**
     * Appends the string representation of the <code>Object</code> argument.
     * <p>
     * The argument is converted to a string as if by the method <code>String.valueOf</code>, and the characters of that string are then
     * appended to this sequence.
     *
     * @param obj an <code>Object</code>.
     * @return a reference to this object.
     */
    public LimitedStringBuilder append(final Object obj) {
        final String s = String.valueOf(obj);
        checkLimit(s);
        sb.append(s);
        return this;
    }

    /**
     * Appends the specified string to this character sequence.
     * <p>
     * The characters of the <code>String</code> argument are appended, in order, increasing the length of this sequence by the length of
     * the argument. If <code>str</code> is <code>null</code>, then the four characters <code>"null"</code> are appended.
     * <p>
     * Let <i>n</i> be the length of this character sequence just prior to execution of the <code>append</code> method. Then the character
     * at index <i>k</i> in the new character sequence is equal to the character at index <i>k</i> in the old character sequence, if
     * <i>k</i> is less than <i>n</i>; otherwise, it is equal to the character at index <i>k-n</i> in the argument <code>str</code>.
     *
     * @param str a string.
     * @return a reference to this object.
     */
    public LimitedStringBuilder append(final String str) {
        final String s = null == str ? "null" : str;
        checkLimit(s);
        sb.append(s);
        return this;
    }

    @Override
    public char charAt(final int index) {
        return sb.charAt(index);
    }

    @Override
    public LimitedStringBuilder append(final CharSequence cs) {
        final CharSequence s = null == cs ? "null" : cs;
        checkLimit(s);
        sb.append(s);
        return this;
    }

    @Override
    public LimitedStringBuilder append(final CharSequence cs, final int start, final int end) {
        final CharSequence s = null == cs ? "null" : cs;
        if (sb.length() + (end - start) > limit) {
            throw newLimitExceededException(limit);
        }
        sb.append(s, start, end);
        return this;
    }

    /**
     * Appends the string representation of the <code>char</code> array argument to this sequence.
     * <p>
     * The characters of the array argument are appended, in order, to the contents of this sequence. The length of this sequence increases
     * by the length of the argument.
     * <p>
     * The overall effect is exactly as if the argument were converted to a string by the method {@link String#valueOf(char[])} and the
     * characters of that string were then {@link #append(String) appended} to this character sequence.
     *
     * @param str the characters to be appended.
     * @return a reference to this object.
     */
    public LimitedStringBuilder append(final char[] str) {
        if (null == str) {
            return append("null");
        }
        if (sb.length() + str.length > limit) {
            throw newLimitExceededException(limit);
        }
        sb.append(str);
        return this;
    }

    /**
     * Appends the string representation of a subarray of the <code>char</code> array argument to this sequence.
     * <p>
     * Characters of the <code>char</code> array <code>str</code>, starting at index <code>offset</code>, are appended, in order, to the
     * contents of this sequence. The length of this sequence increases by the value of <code>len</code>.
     * <p>
     * The overall effect is exactly as if the arguments were converted to a string by the method {@link String#valueOf(char[],int,int)} and
     * the characters of that string were then {@link #append(String) appended} to this character sequence.
     *
     * @param str the characters to be appended.
     * @param offset the index of the first <code>char</code> to append.
     * @param len the number of <code>char</code>s to append.
     * @return a reference to this object.
     */
    public LimitedStringBuilder append(final char[] str, final int offset, final int len) {
        if (null == str) {
            return append("null");
        }
        if (sb.length() + (len - offset) > limit) {
            throw newLimitExceededException(limit);
        }
        sb.append(str, offset, len);
        return this;
    }

    /**
     * Appends the string representation of the <code>boolean</code> argument to the sequence.
     * <p>
     * The argument is converted to a string as if by the method <code>String.valueOf</code>, and the characters of that string are then
     * appended to this sequence.
     *
     * @param b a <code>boolean</code>.
     * @return a reference to this object.
     */
    public LimitedStringBuilder append(final boolean b) {
        final String s = String.valueOf(b);
        checkLimit(s);
        sb.append(s);
        return this;
    }

    @Override
    public LimitedStringBuilder append(final char c) {
        final String s = String.valueOf(c);
        checkLimit(s);
        sb.append(s);
        return this;
    }

    /**
     * Appends the string representation of the <code>int</code> argument to this sequence.
     * <p>
     * The argument is converted to a string as if by the method <code>String.valueOf</code>, and the characters of that string are then
     * appended to this sequence.
     *
     * @param i an <code>int</code>.
     * @return a reference to this object.
     */
    public LimitedStringBuilder append(final int i) {
        final String s = String.valueOf(i);
        checkLimit(s);
        sb.append(s);
        return this;
    }

    public int codePointBefore(final int index) {
        return sb.codePointBefore(index);
    }

    /**
     * Appends the string representation of the <code>long</code> argument to this sequence.
     * <p>
     * The argument is converted to a string as if by the method <code>String.valueOf</code>, and the characters of that string are then
     * appended to this sequence.
     *
     * @param l a <code>long</code>.
     * @return a reference to this object.
     */
    public LimitedStringBuilder append(final long lng) {
        final String s = String.valueOf(lng);
        checkLimit(s);
        sb.append(s);
        return this;
    }

    /**
     * Appends the string representation of the <code>float</code> argument to this sequence.
     * <p>
     * The argument is converted to a string as if by the method <code>String.valueOf</code>, and the characters of that string are then
     * appended to this string sequence.
     *
     * @param f a <code>float</code>.
     * @return a reference to this object.
     */
    public LimitedStringBuilder append(final float f) {
        final String s = String.valueOf(f);
        checkLimit(s);
        sb.append(s);
        return this;
    }

    /**
     * Appends the string representation of the <code>double</code> argument to this sequence.
     * <p>
     * The argument is converted to a string as if by the method <code>String.valueOf</code>, and the characters of that string are then
     * appended to this sequence.
     *
     * @param d a <code>double</code>.
     * @return a reference to this object.
     */
    public LimitedStringBuilder append(final double d) {
        final String s = String.valueOf(d);
        checkLimit(s);
        sb.append(s);
        return this;
    }

    /**
     * Removes the characters in a substring of this sequence. The substring begins at the specified <code>start</code> and extends to the
     * character at index <code>end - 1</code> or to the end of the sequence if no such character exists. If <code>start</code> is equal to
     * <code>end</code>, no changes are made.
     *
     * @param start The beginning index, inclusive.
     * @param end The ending index, exclusive.
     * @return This object.
     * @throws StringIndexOutOfBoundsException if <code>start</code> is negative, greater than <code>length()</code>, or greater than
     *             <code>end</code>.
     */
    public LimitedStringBuilder delete(final int start, final int end) {
        sb.delete(start, end);
        return this;
    }

    /**
     * Removes the <code>char</code> at the specified position in this sequence. This sequence is shortened by one <code>char</code>.
     * <p>
     * Note: If the character at the given index is a supplementary character, this method does not remove the entire character. If correct
     * handling of supplementary characters is required, determine the number of <code>char</code>s to remove by calling
     * <code>Character.charCount(thisSequence.codePointAt(index))</code>, where <code>thisSequence</code> is this sequence.
     *
     * @param index Index of <code>char</code> to remove
     * @return This object.
     * @throws StringIndexOutOfBoundsException if the <code>index</code> is negative or greater than or equal to <code>length()</code>.
     */
    public LimitedStringBuilder deleteCharAt(final int index) {
        sb.deleteCharAt(index);
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return sb.subSequence(start, end);
    }

}
