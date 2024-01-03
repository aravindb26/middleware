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

package com.openexchange.mail.exportpdf.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.openexchange.java.Streams;

/**
 * {@link ReplacingInputStream} an input stream which wraps a string an replaces it's content
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class ReplacingInputStream extends InputStream {

    private final String stringContent;
    private final Pattern pattern;
    private final Reader characterReader;
    private final Charset charset;

    private List<MatchResult> matches;
    private int characterToByteBufferIndex;
    private byte[] characterToByteBuffer;
    private Replacement replacement;

    /* The index of the current match to replace, or to replace next */
    private int matchIndex;

    /* The start position of the current match in the content */
    private int matchPosition;

    /* Counter for the bytes served */
    private int index;

    /* The current replacement content to server */
    private InputStream currentReplacement;

    //-------------------------------------------------------------------------------------------------------------

    /**
     * {@link Replacement} Defines a replacement
     */
    public interface Replacement {

        /**
         * Returns an {@link InputStream} to the data which will replace the content matching the given {@link MatchResult}
         *
         * @param matchCount The count of the match
         * @param match The match to replace
         * @return The data as {@link InputStream} replacing the match
         */
        InputStream getReplacementData(int matchCount, MatchResult match);
    }

    //-------------------------------------------------------------------------------------------------------------

    /* End of the replacement stream */
    private static final int EOF = -1;

    /**
     * A replacement which will remove a match
     */
    public static final Replacement REMOVE = (matchCount, match) -> null;

    /**
     * A NO-OP replacement mode which actually does not change anything
     */
    public static final Replacement NO_REPLACE = null;

    //-------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link ReplacingInputStream}.
     *
     * @param content The content to replace
     * @param charset The {@link Charset} to use
     * @param pattern A regular expression pattern which defines the data to replace
     * @param replacement The replacement as String
     */
    public ReplacingInputStream(String content, Charset charset, String pattern, String replacement) {
        this(content, charset, pattern != null ? Pattern.compile(pattern) : null, replacement);
    }

    /**
     * Initializes a new {@link ReplacingInputStream}.
     *
     * @param content The content to replace
     * @param charset The {@link Charset} to use
     * @param pattern A regular expression pattern which defines the data to replace
     * @param replacement The replacement
     */
    public ReplacingInputStream(String content, Charset charset, String pattern, Replacement replacement) {
        this(content, charset, pattern != null ? Pattern.compile(pattern) : null, replacement);
    }

    /**
     * Initializes a new {@link ReplacingInputStream}.
     *
     * @param content The content to replace
     * @param charset The {@link Charset} to use
     * @param pattern A regular expression pattern which defines the data to replace
     * @param patternFlags The flags for the regular expression pattern as bitmask (See {@link Pattern#compile(String, int)})
     * @param replacement The replacement
     */
    public ReplacingInputStream(String content, Charset charset, String pattern, int patternFlags, Replacement replacement) {
        this(content, charset, pattern != null ? Pattern.compile(pattern, patternFlags) : null, replacement);
    }

    /**
     * Initializes a new {@link ReplacingInputStream}.
     *
     * @param content The content to replace
     * @param charset The {@link Charset} to use
     * @param pattern The {@link Pattern} to use
     * @param replacement The replacement as String
     */
    public ReplacingInputStream(String content, Charset charset, Pattern pattern, String replacement) {
        this(content, charset, pattern, (matchCount, match) -> replacement != null ? new ByteArrayInputStream(replacement.getBytes(charset)) : null);
    }

    /**
     * Initializes a new {@link ReplacingInputStream}.
     *
     * @param content The content to replace
     * @param charset The {@link Charset} to use
     * @param pattern The {@link Pattern} to use
     * @param replacement The replacement
     */
    public ReplacingInputStream(String content, Charset charset, Pattern pattern, Replacement replacement) {
        this.stringContent = content;
        this.charset = charset;
        this.pattern = pattern;
        this.replacement = replacement;
        this.characterReader = new InputStreamReader(new ByteArrayInputStream(content.getBytes(charset)), charset);
    }

    //-------------------------------------------------------------------------------------------------------------

    private int getMatchPositionIndex() {
        return matches.get(matchIndex).start();
    }

    private InputStream getReplacementFor(MatchResult match) {
        if (match == null || replacement == null) {
            return null;
        }
        return replacement.getReplacementData(matchIndex, match);
    }

    private MatchResult getCurrentMatch() {
        if (matches == null || matchIndex >= matches.size()) {
            return null;
        }
        return matches.get(matchIndex);
    }

    private void updateMatchPosition() {
        if (matches != null && !matches.isEmpty() && matchIndex < matches.size()) {
            matchPosition = getMatchPositionIndex();
            return;
        }
        matchPosition = stringContent.length();
    }
    //-------------------------------------------------------------------------------------------------------------

    /**
     * Sets the {@link Replacement} to use for replacing the matched content within the stream
     *
     * @param replacement The {@link Replacement} to use
     */
    public void setReplacement(Replacement replacement) {
        this.replacement = replacement;
    }

    //-------------------------------------------------------------------------------------------------------------

    @Override
    public void close() throws IOException {
        this.characterReader.close();
        Streams.close(currentReplacement);
    }

    @Override
    public int read() throws IOException {
        if (pattern != null && matches == null) {
            /* find all matches for the the given pattern */
            Matcher matcher = pattern.matcher(stringContent);
            matches = matcher.results().toList();
        }
        updateMatchPosition();

        if (characterToByteBuffer != null && characterToByteBufferIndex < characterToByteBuffer.length) {
            /* serve from the internal byte buffer */
            byte b = characterToByteBuffer[characterToByteBufferIndex++];
            if (characterToByteBufferIndex == characterToByteBuffer.length) {
                /* buffer read to the end */
                characterToByteBuffer = null;
                characterToByteBufferIndex = 0;
            }
            return b;
        }

        if (index < matchPosition) {
            /* serve original content data. */
            /* read the next single character, which can be more than one byte */
            char[] cbuf = new char[1];
            characterReader.read(cbuf, 0, 1);
            /* get the bytes and buffer them in order to be served */
            characterToByteBuffer = new String(cbuf).getBytes(charset);
            characterToByteBufferIndex = 1;
            index++;
            return characterToByteBuffer[0];
        } else if (index == matchPosition /* reached the next match */ && index != stringContent.length() /* and not yet done with the whole content */) {
            boolean close = false;
            try {
                /* serve replacement data if present */
                if (currentReplacement == null) {
                    /* determine the next replacement */
                    this.currentReplacement = getReplacementFor(getCurrentMatch());
                }
                if (currentReplacement != null) {
                    /* serve from the replacement */
                    int replacedByte = currentReplacement.read();
                    if (replacedByte == EOF) {
                        /* replacement is done */
                        /* check how many characters were replaced and skip these amount in the original content */
                        int replacedAmount = getCurrentMatch().end() - getCurrentMatch().start();
                        index = index + replacedAmount;
                        close = true;
                        matchIndex++;
                        characterReader.skip(replacedAmount);
                        updateMatchPosition();
                    }
                    return replacedByte;
                }
            } catch (IOException e) {
                close = true;
                throw e;
            } finally {
                if (close) {
                    Streams.close(currentReplacement);
                    currentReplacement = null;
                }
            }

            /* The replacement is present but empty. I.E the matching parts will be removed from the original content */
            /* check how many characters can be skipped */
            int skipAmount = getCurrentMatch().end() - getCurrentMatch().start();
            index = index + skipAmount;
            matchIndex++;
            characterReader.skip(skipAmount);
            updateMatchPosition();
            return read();
        }
        return -1;
    }
}
