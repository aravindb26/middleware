/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.mail.imap.protocol;

import static com.sun.mail.imap.Utility.toUpperCase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.sun.mail.iap.ParsingException;
import com.sun.mail.iap.Protocol;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.util.ASCIIUtility;

/**
 * This class represents a FETCH response obtained from the input stream
 * of an IMAP server.
 *
 * @author  John Mani
 * @author  Bill Shannon
 */

public class FetchResponse extends IMAPResponse {
    /*
     * Regular Items are saved in the items array.
     * Extension items (items handled by subclasses
     * that extend the IMAP provider) are saved in the
     * extensionItems map, indexed by the FETCH item name.
     * The map is only created when needed.
     *
     * XXX - Should consider unifying the handling of
     * regular items and extension items.
     */
    private Item[] items;
    private Map<String, Object> extensionItems;
    private final FetchItem[] fitems;
    private final boolean rfc8970Preview;
    private final boolean decodeUserFlagsWithUTF7;

    public FetchResponse(Protocol p, boolean rfc8970Preview, boolean decodeUserFlagsWithUTF7) 
		throws IOException, ProtocolException {
	super(p);
	this.rfc8970Preview = rfc8970Preview;
    this.decodeUserFlagsWithUTF7 = decodeUserFlagsWithUTF7;
	fitems = null;
	parse();
	buffer = null; // Not needed anymore as completely parsed by now
    }

    /**
     * Construct a FetchResponse without additional FetchItems.
     *
     * @param   r   the IMAPResponse
     * @param   rfc8970Preview  Whether RFC8970 preview capability is supported
     * @param   decodeUserFlagsWithUTF7  Whether to decode user flags using RFC2060's UTF-7 encoding
     * @exception   IOException for I/O errors
     * @exception   ProtocolException   for protocol failures
     * @since JavaMail 1.4.6
     */
    public FetchResponse(IMAPResponse r, boolean rfc8970Preview, boolean decodeUserFlagsWithUTF7)
		throws IOException, ProtocolException {
	this(r, null, rfc8970Preview, decodeUserFlagsWithUTF7);
    }

    /**
     * Construct a FetchResponse that handles the additional FetchItems.
     *
     * @param	r	the IMAPResponse
     * @param	fitems	the fetch items
     * @param   rfc8970Preview  Whether RFC8970 preview capability is supported
     * @param   decodeUserFlagsWithUTF7  Whether to decode user flags using RFC2060's UTF-7 encoding
     * @exception	IOException	for I/O errors
     * @exception	ProtocolException	for protocol failures
     * @since JavaMail 1.4.6
     */
    public FetchResponse(IMAPResponse r, FetchItem[] fitems, boolean rfc8970Preview, boolean decodeUserFlagsWithUTF7)
		throws IOException, ProtocolException {
	super(r);
    this.rfc8970Preview = rfc8970Preview;
    this.decodeUserFlagsWithUTF7 = decodeUserFlagsWithUTF7;
	this.fitems = fitems;
	parse();
	buffer = null; // Not needed anymore as completely parsed by now
    }

    public int getItemCount() {
	return items.length;
    }

    public Item getItem(int index) {
	return items[index];
    }

    public <T extends Item> T getItem(Class<T> c) {
	for (int i = 0; i < items.length; i++) {
	    if (c.isInstance(items[i]))
		return c.cast(items[i]);
	}

	return null;
    }

    /**
     * Return the first fetch response item of the given class
     * for the given message number.
     *
     * @param	r	the responses
     * @param	msgno	the message number
     * @param	c	the class
     * @param	<T>	the type of fetch item
     * @return		the fetch item
     */
    public static <T extends Item> T getItem(Response[] r, int msgno,
				Class<T> c) {
	if (r == null)
	    return null;

	for (int i = 0; i < r.length; i++) {

	    if (r[i] == null ||
		!(r[i] instanceof FetchResponse) ||
		((FetchResponse)r[i]).getNumber() != msgno)
		continue;

	    FetchResponse f = (FetchResponse)r[i];
	    for (int j = 0; j < f.items.length; j++) {
		if (c.isInstance(f.items[j]))
		    return c.cast(f.items[j]);
	    }
	}

	return null;
    }

    /**
     * Return the first fetch response item of the given class
     * for the given message number.
     */
    public static <T extends Item> T getItem(Response[] r, Class<T> c) {
    if (r == null)
        return null;

    for (int i = 0; i < r.length; i++) {

        if (r[i] == null || !(r[i] instanceof FetchResponse))
        continue;

        FetchResponse f = (FetchResponse)r[i];
        for (int j = 0; j < f.items.length; j++) {
        if (c.isInstance(f.items[j]))
            return c.cast(f.items[j]);
        }
    }

    return null;
    }

    /**
     * Return all fetch response items of the given class
     * for the given message number.
     *
     * @param	r	the responses
     * @param	msgno	the message number
     * @param	c	the class
     * @param	<T>	the type of fetch items
     * @return		the list of fetch items
     * @since JavaMail 1.5.2
     */
    public static <T extends Item> List<T> getItems(Response[] r, int msgno,
				Class<T> c) {
	List<T> items = new ArrayList<T>();

	if (r == null)
	    return items;

	for (int i = 0; i < r.length; i++) {

	    if (r[i] == null ||
		!(r[i] instanceof FetchResponse) ||
		((FetchResponse)r[i]).getNumber() != msgno)
		continue;

	    FetchResponse f = (FetchResponse)r[i];
	    for (int j = 0; j < f.items.length; j++) {
		if (c.isInstance(f.items[j]))
		    items.add(c.cast(f.items[j]));
	    }
	}

	return items;
    }

    /**
     * Return all fetch response items of the given class
     * for the given message number.
     *
     * @since JavaMail 1.5.2
     */
    public static <T extends Item> List<T> getItems(Response[] r, Class<T> c) {
    List<T> items = new ArrayList<T>();

    if (r == null)
        return items;

    for (int i = 0; i < r.length; i++) {

        if (r[i] == null || !(r[i] instanceof FetchResponse))
        continue;

        FetchResponse f = (FetchResponse)r[i];
        for (int j = 0; j < f.items.length; j++) {
        if (c.isInstance(f.items[j]))
            items.add(c.cast(f.items[j]));
        }
    }

    return items;
    }

    /**
     * Return a map of the extension items found in this fetch response.
     * The map is indexed by extension item name.  Callers should not
     * modify the map.
     *
     * @return	Map of extension items, or null if none
     * @since JavaMail 1.4.6
     */
    public Map<String, Object> getExtensionItems() {
	return extensionItems;
    }

    private static final char[] HEADER = {'.','H','E','A','D','E','R'};
    private static final char[] TEXT = {'.','T','E','X','T'};

    private void parse() throws ParsingException {
	if (!isNextNonSpace('('))
	    throw new ParsingException(
		"error in FETCH parsing, missing '(' at index " + index);

	List<Item> v = new ArrayList<>();
	Item i = null;
	skipSpaces();
	do {

	    if (index >= size)
		throw new ParsingException(
		"error in FETCH parsing, ran off end of buffer, size " + size);

	    i = parseItem();
	    if (i != null)
		v.add(i);
	    else if (!parseExtensionItem())
		throw new ParsingException(
		    "error in FETCH parsing, unrecognized item at index " +
		    index + ", starts with \"" + next20() + "\"");
	} while (!isNextNonSpace(')'));

	items = v.toArray(new Item[v.size()]);
    }

    /**
     * Return the next 20 characters in the buffer, for exception messages.
     */
    private String next20() {
	if (index + 20 > size)
	    return ASCIIUtility.toString(buffer, index, size);
	else
	    return ASCIIUtility.toString(buffer, index, index + 20) + "...";
    }

    /**
     * Parse the item at the current position in the buffer,
     * skipping over the item if successful.  Otherwise, return null
     * and leave the buffer position unmodified.
     */
    @SuppressWarnings("empty")
    private Item parseItem() throws ParsingException {
	switch (buffer[index]) {
	case 'E': case 'e':
	    if (match(ENVELOPE.name))
		return new ENVELOPE(this);
	    break;
	case 'F': case 'f':
	    if (match(FLAGS.name))
		return new FLAGS(this, decodeUserFlagsWithUTF7);
	    break;
	case 'I': case 'i':
	    if (match(INTERNALDATE.name))
		return new INTERNALDATE(this);
	    break;
	case 'B': case 'b':
	    if (match(BODYSTRUCTURE.name))
		return new BODYSTRUCTURE(this);
	    else if (match(BODY.name)) {
		if (buffer[index] == '[')
		    return new BODY(this);
		else
		    return new BODYSTRUCTURE(this);
	    }
	    break;
	case 'R': case 'r':
	    if (match(RFC822SIZE.name))
		return new RFC822SIZE(this);
	    else if (match(RFC822DATA.name)) {
		boolean isHeader = false;
		if (match(HEADER))
		    isHeader = true;	// skip ".HEADER"
		else if (match(TEXT))
		    isHeader = false;	// skip ".TEXT"
		return new RFC822DATA(this, isHeader);
	    }
	    break;
	case 'U': case 'u':
	    if (match(UID.name))
		return new UID(this);
	    break;
    case 'X': case 'x':
        {
            if (match(X_REAL_UID.name)) {
                return new X_REAL_UID(this);
            }
            if (match(X_MAILBOX.name)) {
                return new X_MAILBOX(this);
            }
        }
        break;
	case 'M': case 'm':
	    if (match(MODSEQ.name))
		return new MODSEQ(this);
	    break;
	case 'S': case 's':
	    if (match(SNIPPET.name))
	    return new SNIPPET(this);
        break;
	case 'P': case 'p':
        if (match(PREVIEW.name))
        return new PREVIEW(this, rfc8970Preview);
        break;
	default: 
	    break;
	}
	return null;
    }

    /**
     * If this item is a known extension item, parse it.
     */
    private boolean parseExtensionItem() throws ParsingException {
	if (fitems == null)
	    return false;
	for (int i = 0; i < fitems.length; i++) {
	    if (match(fitems[i].getName())) {
		if (extensionItems == null)
		    extensionItems = new HashMap<String, Object>();
		extensionItems.put(fitems[i].getName(),
				    fitems[i].parseItem(this));
		return true;
	    }
	}
	return false;
    }

    /**
     * Does the current buffer match the given item name?
     * itemName is the name of the IMAP item to compare against.
     * NOTE that itemName *must* be all uppercase.
     * If the match is successful, the buffer pointer (index)
     * is incremented past the matched item.
     */
    private boolean match(char[] itemName) {
	int len = itemName.length;
	for (int i = 0, j = index; i < len;)
	    // IMAP tokens are case-insensitive. We store itemNames in
	    // uppercase, so convert operand to uppercase before comparing.
	    if (Character.toUpperCase((char)buffer[j++]) != itemName[i++])
		return false;
	index += len;
	return true;
    }

    /**
     * Does the current buffer match the given item name?
     * itemName is the name of the IMAP item to compare against.
     * NOTE that itemName *must* be all uppercase.
     * If the match is successful, the buffer pointer (index)
     * is incremented past the matched item.
     */
    private boolean match(String itemName) {
	int len = itemName.length();
	for (int i = 0, j = index; i < len;)
	    // IMAP tokens are case-insensitive. We store itemNames in
	    // uppercase, so convert operand to uppercase before comparing.
	    if (toUpperCase((char)buffer[j++]) !=
		    itemName.charAt(i++))
		return false;
	index += len;
	return true;
    }
}
