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

package com.openexchange.html.internal.jsoup;

import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import com.google.common.collect.ImmutableSet;
import com.openexchange.java.Strings;

/**
 * {@link HtmlToPlainText} - HTML to plain-text.
 * <p>
 * This program is derived from <a href="https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/HtmlToPlainText.java">Jonathan Hedley's example</a>
 * to demonstrate the use of jsoup to convert HTML input to lightly-formatted plain-text.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class HtmlToPlainText {

    /**
     * Initializes a new {@link HtmlToPlainText}.
     */
    private HtmlToPlainText() {
        super();
    }

    /**
     * Format an Element to plain-text
     *
     * @param element the root element to format
     * @param maxLineLength The max length of a line
     * @param appendHref <code>true</code> to append URLs contained in <i>href</i>s and <i>src</i>s; otherwise <code>false</code>
     * @return The formatted text
     * @throws InterruptedConversionException If conversion has been interrupted
     */
    public static String getPlainText(Element element, int maxLineLength, boolean appendHref) {
        FormattingVisitor formatter = new FormattingVisitor(maxLineLength, appendHref);
        NodeTraversor.traverse(formatter, element); // walk the DOM, and call .head() and .tail() for each node
        return formatter.toString();
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private static class FormattingVisitor implements NodeVisitor {

        private int width = 0;
        private final StringBuilder accum; // holds the accumulated text
        private final boolean appendHref;
        private final int maxLineLength;

        FormattingVisitor(int maxLineLength, boolean appendHref) {
            super();
            this.maxLineLength = maxLineLength;
            this.appendHref = appendHref;
            this.accum = new StringBuilder();
        }

        private static final Set<String> TAGS_HEADING_LF = ImmutableSet.<String> builderWithExpectedSize(8).add("p", "div", "h1", "h2", "h3", "h4", "h5", "tr").build();

        // hit when the node is first seen
        @Override
        public void head(Node node, int depth) {
            if (Thread.interrupted()) {
                throw new InterruptedConversionException();
            }

            String name = node.nodeName();
            if (node instanceof TextNode) {
                // TextNodes carry all user-readable text in the DOM.
                String text = ((TextNode) node).getWholeText();
                StringBuilder sb = StringUtil.borrowBuilder();
                StringUtil.appendNormalisedWhitespace(sb, text, true);
                text = StringUtil.releaseBuilder(sb);
                append(text);
            } else if (name.equals("li")) {
                append("\n * ");
            } else if (name.equals("dt")) {
                append("  ");
            } else if (TAGS_HEADING_LF.contains(name)) {
                if (accum.length() > 0) {
                    append("\n");
                }
            }
        }

        private static final Set<String> TAGS_TRAILING_LF = ImmutableSet.<String> builderWithExpectedSize(10).add("p", "br", "dd", "dt", "h1", "h2", "h3", "h4", "h5").build();

        // hit when all of the node's children (if any) have been visited
        @Override
        public void tail(Node node, int depth) {
            if (Thread.interrupted()) {
                throw new InterruptedConversionException();
            }

            String name = node.nodeName();
            if (TAGS_TRAILING_LF.contains(name)) {
                append("\n");
            } else if (name.equals("a")) {
                if (appendHref) {
                    String absUrl = node.absUrl("href");
                    if (Strings.isNotEmpty(absUrl) && absUrl.trim().startsWith("javascript:") == false) {
                        append(" <");
                        append(absUrl);
                        append(">");
                    }
                }
            } else if (name.equals("img")) {
                if (appendHref) {
                    String absUrl = node.absUrl("src");
                    if (Strings.isNotEmpty(absUrl) && absUrl.trim().startsWith("javascript:") == false && absUrl.trim().startsWith("cid:") == false) {
                        append(" <");
                        append(absUrl);
                        append(">");
                    }
                }
            }
        }

        private static final Pattern PATTERN_WORDS = Pattern.compile("\\s+");

        // appends text to the string builder with a simple word wrap method
        private void append(String text) {
            if (text.startsWith("\n")) {
                width = 0; // reset counter if starts with a newline. only from formats above, not in natural text
            }
            if (text.equals(" ") && (accum.length() == 0 || endsWithSpaceOrLF())) {
                return; // don't accumulate long runs of empty spaces
            }

            StringBuilder wordBuilder = null;
            if (text.length() + width > maxLineLength) { // won't fit, needs to wrap
                String[] words = PATTERN_WORDS.split(text, 0);
                for (int i = 0; i < words.length; i++) {
                    String word = words[i];
                    if (i != words.length - 1) { // not the last word -> append space character.
                        if (wordBuilder == null) {
                            wordBuilder = new StringBuilder(word);
                        } else {
                            wordBuilder.setLength(0);
                            wordBuilder.append(word);
                        }
                        wordBuilder.append(' ');
                        word = wordBuilder.toString();
                    }
                    if (word.length() + width > maxLineLength) { // wrap and reset counter
                        accum.append("\n").append(word);
                        width = word.length();
                    } else {
                        accum.append(word);
                        width += word.length();
                    }
                }
            } else { // fits as is, without need to wrap text
                accum.append(text);
                width += text.length();
            }
        }

        private boolean endsWithSpaceOrLF() {
            char ch = accum.charAt(accum.length() - 1);
            return ch == ' ' || ch == '\n';
        }

        @Override
        public String toString() {
            return accum.toString();
        }
    }

}
