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

package com.openexchange.mail.dataobjects;

import static com.openexchange.mail.mime.utils.MimeMessageUtility.decodeMultiEncodedHeader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import com.google.common.collect.ImmutableSet;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.mime.HeaderName;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.PlainTextAddress;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.tools.TimeZoneUtils;

/**
 * {@link MailMessage} - Abstract super class for all {@link MailMessage} subclasses.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class MailMessage extends MailPart {

    private static final long serialVersionUID = 8585899349289256569L;

    private static final transient org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(MailMessage.class);

    private static final String HDR_REFERENCES = MessageHeaders.HDR_REFERENCES;
    private static final String HDR_MESSAGE_ID = MessageHeaders.HDR_MESSAGE_ID;
    private static final String HDR_IN_REPLY_TO = MessageHeaders.HDR_IN_REPLY_TO;

    /*-
     * ------------------- Flags ------------------------------
     */
    /**
     * This message has been answered. This flag is set by clients to indicate that this message has been answered to.
     *
     * @value 1
     */
    public static final int FLAG_ANSWERED = 1;

    /**
     * This message is marked deleted. Clients set this flag to mark a message as deleted. The expunge operation on a folder removes all
     * messages in that folder that are marked for deletion.
     *
     * @value 2
     */
    public static final int FLAG_DELETED = 1 << 1;

    /**
     * This message is a draft. This flag is set by clients to indicate that the message is a draft message.
     *
     * @value 4
     */
    public static final int FLAG_DRAFT = 1 << 2;

    /**
     * This message is flagged. No semantic is defined for this flag. Clients alter this flag.
     *
     * @value 8
     */
    public static final int FLAG_FLAGGED = 1 << 3;

    /**
     * This message is recent. Folder implementations set this flag to indicate that this message is new to this folder, that is, it has
     * arrived since the last time this folder was opened.
     * <p>
     * Clients cannot alter this flag.
     *
     * @value 16
     */
    public static final int FLAG_RECENT = 1 << 4;

    /**
     * This message is seen. This flag is implicitly set by the implementation when the this Message's content is returned to the client in
     * some form.
     *
     * @value 32
     */
    public static final int FLAG_SEEN = 1 << 5;

    /**
     * A special flag that indicates that this folder supports user defined flags.
     * <p>
     * Clients cannot alter this flag.
     *
     * @value 64
     */
    public static final int FLAG_USER = 1 << 6;

    /**
     * Virtual Spam flag
     *
     * @value 128
     */
    public static final int FLAG_SPAM = 1 << 7;

    /**
     * Virtual forwarded flag that marks this message as being forwarded.
     *
     * @value 256
     */
    public static final int FLAG_FORWARDED = 1 << 8;

    /**
     * Virtual read acknowledgment flag that marks this message as being notified for delivery.
     *
     * @value 512
     */
    public static final int FLAG_READ_ACK = 1 << 9;

    /*-
     * ------------------- User Flags ------------------------------
     */

    /**
     * The value of virtual forwarded flag.
     *
     * @value $Forwarded
     */
    public static final String USER_FORWARDED = "$Forwarded";

    /**
     * The value of virtual read acknowledgment flag.
     *
     * @value $MDNSent
     */
    public static final String USER_READ_ACK = "$MDNSent";

    /**
     * The value of virtual spam flag.
     *
     * @value $Junk
     */
    public static final String USER_SPAM = "$Junk";

    /**
     * Marks if the mail has an attachment
     *
     * @value $HasAttachment
     */
    public static final String USER_HAS_ATTACHMENT = "$HasAttachment";

    /**
     * Marks if the mail has no attachment
     *
     * @value $HasNoAttachment
     */
    public static final String USER_HAS_NO_ATTACHMENT = "$HasNoAttachment";

    /**
     * The flag bit 0 set by Apple Mail to encode mail color flags.
     *
     * @value $MailFlagBit0
     * @see #getAppleMailFlag(boolean, boolean, boolean)
     */
    public static final String USER_APPLE_MAIL_FLAG_BIT0 = "$MailFlagBit0";

    /**
     * The flag bit 1 set by Apple Mail to encode mail color flags.
     *
     * @value $MailFlagBit1
     * @see #getAppleMailFlag(boolean, boolean, boolean)
     */
    public static final String USER_APPLE_MAIL_FLAG_BIT1 = "$MailFlagBit1";

    /**
     * The flag bit 2 set by Apple Mail to encode mail color flags.
     *
     * @value $MailFlagBit2
     * @see #getAppleMailFlag(boolean, boolean, boolean)
     */
    public static final String USER_APPLE_MAIL_FLAG_BIT2 = "$MailFlagBit2";

    /**
     * Gets the Apple Mail color flag for given user flags.
     * <p>
     * Checks for presence of <code>"$MailFlagBit0"</code>, <code>"$MailFlagBit1"</code> and/or <code>"$MailFlagBit2"</code> to determine
     * the Apple Mail flag bitmask:
     * <p>
     * <table>
     * <tr><th>Color<th>Number<th>Bitmask
     * <tr><td style="color:red;">Red<td>0<td>000
     * <tr><td style="color:orange;">Orange<td>1<td>001
     * <tr><td style="color:yellow;">Yellow<td>2<td>010
     * <tr><td style="color:green;">Green<td>3<td>011
     * <tr><td style="color:blue;">Blue<td>4<td>100
     * <tr><td style="color:purple;">Purple<td>5<td>101
     * <tr><td style="color:gray;">Grey<td>6<td>110
     * </table>
     *
     * @param userFlags A set containing the user flags
     * @return The Apple Mail color flag or <code>0</code> (zero, which might be red n case mail has \Flagged system flag set)
     */
    public static <C extends Collection<String>> int getAppleMailFlag(C userFlags) {
        boolean hasMailFlagBit0 = userFlags.contains(MailMessage.USER_APPLE_MAIL_FLAG_BIT0);
        boolean hasMailFlagBit1 = userFlags.contains(MailMessage.USER_APPLE_MAIL_FLAG_BIT1);
        boolean hasMailFlagBit2 = userFlags.contains(MailMessage.USER_APPLE_MAIL_FLAG_BIT2);
        return getAppleMailFlag(hasMailFlagBit0, hasMailFlagBit1, hasMailFlagBit2);
    }

    /**
     * Gets the Apple Mail color flag for given flag bitmask.
     * <p>
     * <table>
     * <tr><th>Color<th>Number<th>Bitmask
     * <tr><td style="color:red;">Red<td>0<td>000
     * <tr><td style="color:orange;">Orange<td>1<td>001
     * <tr><td style="color:yellow;">Yellow<td>2<td>010
     * <tr><td style="color:green;">Green<td>3<td>011
     * <tr><td style="color:blue;">Blue<td>4<td>100
     * <tr><td style="color:purple;">Purple<td>5<td>101
     * <tr><td style="color:gray;">Grey<td>6<td>110
     * </table>
     *
     * @param hasMailFlagBit0 Whether mail flag bit 0 is set
     * @param hasMailFlagBit1 Whether mail flag bit 1 is set
     * @param hasMailFlagBit2 Whether mail flag bit 2 is set
     * @return The Apple Mail color flag or <code>0</code> (zero, which might be red n case mail has \Flagged system flag set)
     */
    public static int getAppleMailFlag(boolean hasMailFlagBit0, boolean hasMailFlagBit1, boolean hasMailFlagBit2) {
        if (hasMailFlagBit0 && hasMailFlagBit1 == false && hasMailFlagBit2 == false) {
            // ORANGE
            return COLOR_LABEL_ORANGE;
        } else if (hasMailFlagBit0 == false && hasMailFlagBit1 && hasMailFlagBit2 == false) {
            // YELLOW
            return COLOR_LABEL_YELLOW;
        } else if (hasMailFlagBit0 && hasMailFlagBit1 && hasMailFlagBit2 == false) {
            // GREEN
            return COLOR_LABEL_GREEN;
        } else if (hasMailFlagBit0 == false && hasMailFlagBit1 == false && hasMailFlagBit2) {
            // BLUE
            return COLOR_LABEL_BLUE;
        } else if (hasMailFlagBit0 && hasMailFlagBit1 == false && hasMailFlagBit2) {
            // PURPLE
            return COLOR_LABEL_PURPLE;
        } else if (hasMailFlagBit0 == false && hasMailFlagBit1 && hasMailFlagBit2) {
            // GRAY
            return COLOR_LABEL_GRAY;
        }
        // None set. Might be RED in case mail has \Flagged system flag set
        return 0;
    }

    /*-
     * ------------------- Priority ------------------------------
     */
    /**
     * Highest priority
     */
    public static final int PRIORITY_HIGHEST = 1;

    /**
     * High priority
     */
    public static final int PRIORITY_HIGH = 2;

    /**
     * Normal priority
     */
    public static final int PRIORITY_NORMAL = 3;

    /**
     * Low priority
     */
    public static final int PRIORITY_LOW = 4;

    /**
     * Lowest priority
     */
    public static final int PRIORITY_LOWEST = 5;

    /*-
     * ------------------- Color Label ------------------------------
     */

    /**
     * The prefix for a mail message's color labels stored as a user flag
     */
    public static final String COLOR_LABEL_PREFIX = "$cl_";

    /**
     * The deprecated prefix for a mail message's color labels stored as a user flag
     */
    public static final String COLOR_LABEL_PREFIX_OLD = "cl_";

    /**
     * The <code>int</code> value for no color label
     */
    public static final int COLOR_LABEL_NONE = 0;

    /**
     * The <code>string</code> with all valid color flags whitespace separated
     */
    private static final Set<String> ALL_COLOR_LABELS = Set.of("$cl_0", "$cl_1", "$cl_2", "$cl_3", "$cl_4", "$cl_5", "$cl_6", "$cl_7", "$cl_8", "$cl_9", "$cl_10", "cl_0", "cl_1", "cl_2", "cl_3", "cl_4", "cl_5", "cl_6", "cl_7", "cl_8", "cl_9", "cl_10");

    /**
     * Determines the corresponding <code>int</code> value of a given color label's string representation.
     * <p>
     * A color label's string representation matches the pattern:<br>
     * &lt;value-of-{@link #COLOR_LABEL_PREFIX}&gt;&lt;color-label-int-value&gt;
     * <p>
     * &lt;value-of-{@link #COLOR_LABEL_PREFIX_OLD} &gt;&lt;color-label-int-value&gt; is also accepted.
     *
     * @param cl The color label's string representation
     * @return The color label's <code>int</code> value
     * @throws OXException
     */
    public static int getColorLabelIntValue(String cl) throws OXException {
        if (!isColorLabel(cl)) {
            throw MailExceptionCode.UNKNOWN_COLOR_LABEL.create(cl);
        } else if (!isValidColorLabel(cl)) {
            return COLOR_LABEL_NONE;
        }
        try {
            return Integer.parseInt(cl.substring(cl.charAt(0) == '$' ? COLOR_LABEL_PREFIX.length() : COLOR_LABEL_PREFIX_OLD.length()));
        } catch (NumberFormatException e) {
            throw MailExceptionCode.UNKNOWN_COLOR_LABEL.create(e, cl);
        }
    }

    /**
     * Tests if specified string matches a color label pattern.
     *
     * @param cl The string to check
     * @return <code>true</code> if specified string matches a color label pattern; otherwise <code>false</code>
     */
    public static boolean isColorLabel(String cl) {
        return (cl != null && (cl.startsWith(MailMessage.COLOR_LABEL_PREFIX) || cl.startsWith(MailMessage.COLOR_LABEL_PREFIX_OLD)));
    }

    /**
     * Tests if specified string contains a valid color label
     *
     * @param cl The string to check
     * @return <code>true</code> if specified string is a valid color label; otherwise <code>false</code>
     */
    public static boolean isValidColorLabel(String cl) {
        return ALL_COLOR_LABELS.contains(cl);
    }

    /**
     * Parses specified color label's string.
     * <p>
     * <b>Note</b> that this method assumes {@link #isColorLabel(String)} would return <code>true</code> for specified string.
     *
     * @param cl The color label's string
     * @param defaultValue The default value to return if parsing color label's <code>int</code> value fails
     * @return The color label's <code>int</code> value or <code>defaultValue</code> on failure.
     */
    public static int parseColorLabel(String cl, int defaultValue) {
        try {
            return Integer.parseInt(cl.substring('$' == cl.charAt(0) ? COLOR_LABEL_PREFIX.length() : COLOR_LABEL_PREFIX_OLD.length()));
        } catch (NumberFormatException e) {
            LOG.debug("Invalid color label: {}", cl, e);
            return defaultValue;
        }
    }

    /**
     * Generates the color label's string representation from given <code>int</code> value.
     * <p>
     * A color label's string representation matches the pattern:<br>
     * &lt;value-of-{@link #COLOR_LABEL_PREFIX}&gt;&lt;color-label-int-value&gt;
     *
     * @param cl The color label's <code>int</code> value
     * @return The color abel's string representation
     */
    public static String getColorLabelStringValue(int cl) {
        return new StringBuilder(COLOR_LABEL_PREFIX).append(cl).toString();
    }

    /**
     * Checks if the provided user flag is <code>"$HasAttachment"</code>.
     *
     * @param userFlag The flag to check
     * @return <code>true</code> if the flag is <code>"$HasAttachment"</code>; otherwise <code>false</code>
     */
    public static boolean isHasAttachment(String userFlag) {
        return MailMessage.USER_HAS_ATTACHMENT.equalsIgnoreCase(userFlag);
    }

    /**
     * Checks if the provided user flag is <code>"$HasNoAttachment"</code>.
     *
     * @param userFlag The flag to check
     * @return <code>true</code> if the flag is <code>"$HasNoAttachment"</code>; otherwise <code>false</code>
     */
    public static boolean isHasNoAttachment(String userFlag) {
        return MailMessage.USER_HAS_NO_ATTACHMENT.equalsIgnoreCase(userFlag);
    }

    /**
     * <b style="color:red;">&block;</b> App Suite color label for red.
     */
    public static final int COLOR_LABEL_RED = 1;

    /**
     * <b style="color:blue;">&block;</b> App Suite color label for blue.
     */
    public static final int COLOR_LABEL_BLUE = 2;

    /**
     * <b style="color:green;">&block;</b> App Suite color label for green.
     */
    public static final int COLOR_LABEL_GREEN = 3;

    /**
     * <b style="color:gray;">&block;</b> App Suite color label for gray.
     */
    public static final int COLOR_LABEL_GRAY = 4;

    /**
     * <b style="color:purple;">&block;</b> App Suite color label for purple.
     */
    public static final int COLOR_LABEL_PURPLE = 5;

    /**
     * <b style="color:lightgreen;">&block;</b> App Suite color label for light-green.
     */
    public static final int COLOR_LABEL_LIGHTGREEN = 6;

    /**
     * <b style="color:orange;">&block;</b> App Suite color label for orange.
     */
    public static final int COLOR_LABEL_ORANGE = 7;

    /**
     * <b style="color:pink;">&block;</b> App Suite color label for pink.
     */
    public static final int COLOR_LABEL_PINK = 8;

    /**
     * <b style="color:cyan;">&block;</b> App Suite color label for cyan.
     */
    public static final int COLOR_LABEL_CYAN = 9;

    /**
     * <b style="color:yellow;">&block;</b> App Suite color label for yellow.
     */
    public static final int COLOR_LABEL_YELLOW = 10;

    private static final InternetAddress[] EMPTY_ADDRS = new InternetAddress[0];

    /**
     * The flags.
     */
    private int flags;

    private boolean b_flags;

    /**
     * The previous \Seen state.
     */
    private boolean prevSeen;

    private boolean b_prevSeen;

    /**
     * References to other messages.
     */
    private String[] references;

    private boolean b_references;

    /**
     * From addresses.
     */
    private Set<InternetAddress> from;

    private boolean b_from;

    /**
     * Sender addresses.
     */
    private Set<InternetAddress> sender;

    private boolean b_sender;

    /**
     * To addresses.
     */
    private Set<InternetAddress> to;

    private boolean b_to;

    /**
     * Cc addresses.
     */
    private Set<InternetAddress> cc;

    private boolean b_cc;

    /**
     * Bcc addresses.
     */
    private Set<InternetAddress> bcc;

    private boolean b_bcc;

    /**
     * Reply-To addresses.
     */
    private Set<InternetAddress> replyTo;

    private boolean b_replyTo;

    /**
     * The level in a communication thread.
     */
    private int threadLevel;

    private boolean b_threadLevel;

    /**
     * The subject.
     */
    private String subject;

    private boolean b_subject;

    /**
     * The subject.
     */
    private boolean subjectDecoded;

    /**
     * The sent date (the <code>Date</code> header).
     */
    private Date sentDate;

    private boolean b_sentDate;

    /**
     * The (internal) received date.
     */
    private Date receivedDate;

    private boolean b_receivedDate;

    /**
     * User flags.
     */
    private Set<HeaderName> userFlags;

    private boolean b_userFlags;

    /**
     * The color label (set through an user flag).
     */
    private int colorLabel;

    private boolean b_colorLabel;

    /**
     * The priority (the <code>X-Priority</code> header).
     */
    private int priority;

    private boolean b_priority;

    /**
     * The <code>Disposition-Notification-To</code> header.
     */
    private InternetAddress dispositionNotification;

    private boolean b_dispositionNotification;

    /**
     * The message folder fullname/ID.
     */
    private String folder;

    private boolean b_folder;

    /**
     * The message's account ID.
     */
    private int accountId;

    private boolean b_accountId;

    /**
     * The message account's name.
     */
    private String accountName;

    private boolean b_accountName;

    /**
     * Whether an attachment is present or not.
     */
    private boolean hasAttachment;

    private boolean b_hasAttachment;

    /**
     * The alternative flag whether an attachment is present or not.
     */
    private boolean alternativeHasAttachment;

    private boolean b_alternativeHasAttachment;

    /**
     * Whether a VCard should be appended or not.
     */
    private boolean appendVCard;

    private boolean b_appendVCard;

    /**
     * The number of recent messages in associated folder.
     */
    private int recentCount;

    private boolean b_recentCount;

    /**
     * The Message-Id header value.
     */
    private String messageId;

    private boolean b_messageId;

    /**
     * The original folder identifier
     */
    private FullnameArgument originalFolder;
    private boolean b_originalFolder;

    /**
     * The original identifier
     */
    private String originalId;
    private boolean b_originalId;

    /**
     * Email Security Info, encrypted or signed
     */
    private SecurityInfo securityInfo;
    private boolean b_securityInfo;

    /**
     * Email security results from decryption/signature verification
     */
    private SecurityResult securityResult;
    private boolean b_securityResult;

    /**
     * Email authenticity results
     */
    private MailAuthenticityResult authenticityResult;
    private boolean b_authenticityResult;

    /**
     * The text preview
     */
    private String textPreview;
    private boolean b_textPreview;

    /**
     * The mail structure
     */
    private MailStructure mailStructure;
    private boolean b_mailStructure;

    /**
     * Default constructor
     */
    protected MailMessage() {
        super();
        priority = PRIORITY_NORMAL;
        colorLabel = COLOR_LABEL_NONE;
        accountId = Account.DEFAULT_ID;
    }

    /**
     * Removes the personal parts from given addresses
     *
     * @param addrs The addresses to remove the personals from
     */
    protected void removePersonalsFrom(Set<InternetAddress> addrs) {
        if (null != addrs) {
            for (InternetAddress addr : addrs) {
                try {
                    addr.setPersonal(null);
                } catch (UnsupportedEncodingException e) {
                    // Cannot occur
                }
            }
        }
    }

    /**
     * Adds an email address to <i>From</i>.
     *
     * @param addr The address
     */
    public void addFrom(InternetAddress addr) {
        if (null == addr) {
            b_from = true;
            return;
        } else if (null == from) {
            from = new LinkedHashSet<>();
            b_from = true;
        }
        from.add(addr);
    }

    /**
     * Adds email addresses to <i>From</i>.
     *
     * @param addrs The addresses
     */
    public void addFrom(InternetAddress[] addrs) {
        if (null == addrs) {
            b_from = true;
            return;
        } else if (null == from) {
            from = new LinkedHashSet<>();
            b_from = true;
        }
        from.addAll(Arrays.asList(addrs));
    }

    /**
     * Adds email addresses to <i>From</i>.
     *
     * @param addrs The addresses
     */
    public void addFrom(Collection<InternetAddress> addrs) {
        if (null == addrs) {
            b_from = true;
            return;
        } else if (null == from) {
            from = new LinkedHashSet<>();
            b_from = true;
        }
        from.addAll(addrs);
    }

    /**
     * @return <code>true</code> if <i>From</i> is set; otherwise <code>false</code>
     */
    public boolean containsFrom() {
        return b_from || containsHeader(MessageHeaders.HDR_FROM);
    }

    /**
     * Removes the <i>From</i> addresses.
     */
    public void removeFrom() {
        from = null;
        removeHeader(MessageHeaders.HDR_FROM);
        b_from = false;
    }

    /**
     * @return The <i>From</i> addresses.
     */
    public InternetAddress[] getFrom() {
        if (!b_from) {
            addFrom(MimeMessageUtility.getAddressHeader(MessageHeaders.HDR_FROM, this));
        }
        return from == null ? EMPTY_ADDRS : from.toArray(new InternetAddress[from.size()]);
    }

    /**
     * Adds an email address to <i>Sender</i>.
     *
     * @param addr The address
     */
    public void addSender(InternetAddress addr) {
        if (null == addr) {
            b_sender = true;
            return;
        } else if (null == sender) {
            sender = new LinkedHashSet<>();
            b_sender = true;
        }
        sender.add(addr);
    }

    /**
     * Adds email addresses to <i>Sender</i>.
     *
     * @param addrs The addresses
     */
    public void addSender(InternetAddress[] addrs) {
        if (null == addrs) {
            b_sender = true;
            return;
        } else if (null == sender) {
            sender = new LinkedHashSet<>();
            b_sender = true;
        }
        sender.addAll(Arrays.asList(addrs));
    }

    /**
     * Adds email addresses to <i>Sender</i>.
     *
     * @param addrs The addresses
     */
    public void addSender(Collection<InternetAddress> addrs) {
        if (null == addrs) {
            b_sender = true;
            return;
        } else if (null == sender) {
            sender = new LinkedHashSet<>();
            b_sender = true;
        }
        sender.addAll(addrs);
    }

    /**
     * @return <code>true</code> if <i>Sender</i> is set; otherwise <code>false</code>
     */
    public boolean containsSender() {
        return b_sender || containsHeader(MessageHeaders.HDR_SENDER);
    }

    /**
     * Removes the <i>Sender</i> addresses.
     */
    public void removeSender() {
        sender = null;
        removeHeader(MessageHeaders.HDR_SENDER);
        b_sender = false;
    }

    /**
     * @return The <i>Sender</i> addresses.
     */
    public InternetAddress[] getSender() {
        if (!b_sender) {
            addSender(MimeMessageUtility.getAddressHeader(MessageHeaders.HDR_SENDER, this));
        }
        return sender == null ? EMPTY_ADDRS : sender.toArray(new InternetAddress[sender.size()]);
    }

    /**
     * Removes the personal parts from the <i>From</i> addresses.
     */
    public void removeFromPersonals() {
        removePersonalsFrom(this.from);
    }

    /**
     * Adds an email address to <i>To</i>.
     *
     * @param addr The address
     */
    public void addTo(InternetAddress addr) {
        if (null == addr) {
            b_to = true;
            return;
        } else if (null == to) {
            to = new LinkedHashSet<>();
            b_to = true;
        }
        to.add(addr);
    }

    /**
     * Adds email addresses to <i>To</i>
     *
     * @param addrs The addresses
     */
    public void addTo(InternetAddress[] addrs) {
        if (null == addrs) {
            b_to = true;
            return;
        } else if (null == to) {
            to = new LinkedHashSet<>();
            b_to = true;
        }
        to.addAll(Arrays.asList(addrs));
    }

    /**
     * Adds email addresses to <i>To</i>
     *
     * @param addrs The addresses
     */
    public void addTo(Collection<InternetAddress> addrs) {
        if (null == addrs) {
            b_to = true;
            return;
        } else if (null == to) {
            to = new LinkedHashSet<>();
            b_to = true;
        }
        to.addAll(addrs);
    }

    /**
     * @return <code>true</code> if <i>To</i> is set; otherwise <code>false</code>
     */
    public boolean containsTo() {
        return b_to || containsHeader(MessageHeaders.HDR_TO);
    }

    /**
     * Removes the <i>To</i> addresses
     */
    public void removeTo() {
        to = null;
        removeHeader(MessageHeaders.HDR_TO);
        b_to = false;
    }

    /**
     * @return The <i>To</i> addresses
     */
    public InternetAddress[] getTo() {
        if (!b_to) {
            addTo(MimeMessageUtility.getAddressHeader(MessageHeaders.HDR_TO, this));
        }
        return to == null ? EMPTY_ADDRS : to.toArray(new InternetAddress[to.size()]);
    }

    /**
     * Removes the personal parts from the <i>To</i> addresses.
     */
    public void removeToPersonals() {
        removePersonalsFrom(this.to);
    }

    /**
     * Adds an email address to <i>Cc</i>
     *
     * @param addr The address
     */
    public void addCc(InternetAddress addr) {
        if (null == addr) {
            b_cc = true;
            return;
        } else if (null == cc) {
            cc = new LinkedHashSet<>();
            b_cc = true;
        }
        cc.add(addr);
    }

    /**
     * Adds email addresses to <i>Cc</i>
     *
     * @param addrs The addresses
     */
    public void addCc(InternetAddress[] addrs) {
        if (null == addrs) {
            b_cc = true;
            return;
        } else if (null == cc) {
            cc = new LinkedHashSet<>();
            b_cc = true;
        }
        cc.addAll(Arrays.asList(addrs));
    }

    /**
     * Adds email addresses to <i>Cc</i>
     *
     * @param addrs The addresses
     */
    public void addCc(Collection<InternetAddress> addrs) {
        if (null == addrs) {
            b_cc = true;
            return;
        } else if (null == cc) {
            cc = new LinkedHashSet<>();
            b_cc = true;
        }
        cc.addAll(addrs);
    }

    /**
     * @return <code>true</code> if <i>Cc</i> is set; otherwise <code>false</code>
     */
    public boolean containsCc() {
        return b_cc || containsHeader(MessageHeaders.HDR_CC);
    }

    /**
     * Removes the <i>Cc</i> addresses
     */
    public void removeCc() {
        cc = null;
        removeHeader(MessageHeaders.HDR_CC);
        b_cc = false;
    }

    /**
     * @return The <i>Cc</i> addresses
     */
    public InternetAddress[] getCc() {
        if (!b_cc) {
            addCc(MimeMessageUtility.getAddressHeader(MessageHeaders.HDR_CC, this));
        }
        return cc == null ? EMPTY_ADDRS : cc.toArray(new InternetAddress[cc.size()]);
    }

    /**
     * Removes the personal parts from the <i>Cc</i> addresses.
     */
    public void removeCcPersonals() {
        removePersonalsFrom(this.cc);
    }

    /**
     * Adds an email address to <i>Bcc</i>
     *
     * @param addr The address
     */
    public void addBcc(InternetAddress addr) {
        if (null == addr) {
            b_bcc = true;
            return;
        } else if (null == bcc) {
            bcc = new LinkedHashSet<>();
            b_bcc = true;
        }
        bcc.add(addr);
    }

    /**
     * Adds email addresses to <i>Bcc</i>
     *
     * @param addrs The addresses
     */
    public void addBcc(InternetAddress[] addrs) {
        if (null == addrs) {
            b_bcc = true;
            return;
        } else if (null == bcc) {
            bcc = new LinkedHashSet<>();
            b_bcc = true;
        }
        bcc.addAll(Arrays.asList(addrs));
    }

    /**
     * Adds email addresses to <i>Bcc</i>
     *
     * @param addrs The addresses
     */
    public void addBcc(Collection<InternetAddress> addrs) {
        if (null == addrs) {
            b_bcc = true;
            return;
        } else if (null == bcc) {
            bcc = new LinkedHashSet<>();
            b_bcc = true;
        }
        bcc.addAll(addrs);
    }

    /**
     * @return <code>true</code> if <i>Bcc</i> is set; otherwise <code>false</code>
     */
    public boolean containsBcc() {
        return b_bcc || containsHeader(MessageHeaders.HDR_BCC);
    }

    /**
     * Removes the <i>Bcc</i> addresses
     */
    public void removeBcc() {
        bcc = null;
        removeHeader(MessageHeaders.HDR_BCC);
        b_bcc = false;
    }

    /**
     * @return The <i>Bcc</i> addresses
     */
    public InternetAddress[] getBcc() {
        if (!b_bcc) {
            addBcc(MimeMessageUtility.getAddressHeader(MessageHeaders.HDR_BCC, this));
        }
        return bcc == null ? EMPTY_ADDRS : bcc.toArray(new InternetAddress[bcc.size()]);
    }

    /**
     * Removes the personal parts from the <i>Bcc</i> addresses.
     */
    public void removeBccPersonals() {
        removePersonalsFrom(this.bcc);
    }

    /**
     * Gets all the recipient addresses for the message.<br>
     * Extracts the TO, CC, and BCC recipients.
     *
     * @return The recipients
     */
    public InternetAddress[] getAllRecipients() {
        Set<InternetAddress> set = new LinkedHashSet<>(6);
        set.addAll(Arrays.asList(getTo()));
        set.addAll(Arrays.asList(getCc()));
        set.addAll(Arrays.asList(getBcc()));
        return set.toArray(new InternetAddress[set.size()]);
    }

    /**
     * Adds an email address to <i>Reply-To</i>
     *
     * @param addr The address
     */
    public void addReplyTo(InternetAddress addr) {
        if (null == addr) {
            b_replyTo = true;
            return;
        } else if (null == replyTo) {
            replyTo = new LinkedHashSet<>();
            b_replyTo = true;
        }
        replyTo.add(addr);
    }

    /**
     * Adds email addresses to <i>Reply-To</i>
     *
     * @param addrs The addresses
     */
    public void addReplyTo(InternetAddress[] addrs) {
        if (null == addrs) {
            b_replyTo = true;
            return;
        } else if (null == replyTo) {
            replyTo = new LinkedHashSet<>();
            b_replyTo = true;
        }
        replyTo.addAll(Arrays.asList(addrs));
    }

    /**
     * Adds email addresses to <i>Reply-To</i>
     *
     * @param addrs The addresses
     */
    public void addReplyTo(Collection<InternetAddress> addrs) {
        if (null == addrs) {
            b_replyTo = true;
            return;
        } else if (null == replyTo) {
            replyTo = new LinkedHashSet<>();
            b_replyTo = true;
        }
        replyTo.addAll(addrs);
    }

    /**
     * @return <code>true</code> if <i>Reply-To</i> is set; otherwise <code>false</code>
     */
    public boolean containsReplyTo() {
        return b_replyTo || containsHeader(MessageHeaders.HDR_REPLY_TO);
    }

    /**
     * Removes the <i>Reply-To</i> addresses
     */
    public void removeReplyTo() {
        replyTo = null;
        removeHeader(MessageHeaders.HDR_REPLY_TO);
        b_replyTo = false;
    }

    /**
     * @return The <i>Reply-To</i> addresses
     */
    public InternetAddress[] getReplyTo() {
        if (!b_replyTo) {
            addReplyTo(MimeMessageUtility.getAddressHeader(MessageHeaders.HDR_REPLY_TO, this));
        }
        return replyTo == null ? EMPTY_ADDRS : replyTo.toArray(new InternetAddress[replyTo.size()]);
    }

    /**
     * Gets the flags
     *
     * @return the flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @return <code>true</code> if flag \ANSWERED is set; otherwise <code>false</code>
     */
    public boolean isAnswered() {
        return ((flags & FLAG_ANSWERED) == FLAG_ANSWERED);
    }

    /**
     * @return <code>true</code> if flag \DELETED is set; otherwise <code>false</code>
     */
    public boolean isDeleted() {
        return ((flags & FLAG_DELETED) == FLAG_DELETED);
    }

    /**
     * @return <code>true</code> if flag \DRAFT is set; otherwise <code>false</code>
     */
    public boolean isDraft() {
        return ((flags & FLAG_DRAFT) == FLAG_DRAFT);
    }

    /**
     * @return <code>true</code> if flag \FLAGGED is set; otherwise <code>false</code>
     */
    public boolean isFlagged() {
        return ((flags & FLAG_FLAGGED) == FLAG_FLAGGED);
    }

    /**
     * @return <code>true</code> if flag \RECENT is set; otherwise <code>false</code>
     */
    public boolean isRecent() {
        return ((flags & FLAG_RECENT) == FLAG_RECENT);
    }

    /**
     * @return <code>true</code> if flag \SEEN is set; otherwise <code>false</code>
     */
    public boolean isSeen() {
        return ((flags & FLAG_SEEN) == FLAG_SEEN);
    }

    /**
     * @return <code>true</code> if flag \SEEN is not set; otherwise <code>false</code>
     */
    public boolean isUnseen() {
        return !isSeen();
    }

    /**
     * @return <code>true</code> if virtual spam flag is set; otherwise <code>false</code>
     */
    public boolean isSpam() {
        return ((flags & FLAG_SPAM) == FLAG_SPAM);
    }

    /**
     * @return <code>true</code> if forwarded flag is set; otherwise <code>false</code>
     */
    public boolean isForwarded() {
        return ((flags & FLAG_FORWARDED) == FLAG_FORWARDED);
    }

    /**
     * @return <code>true</code> if read acknowledgment flag is set; otherwise <code>false</code>
     */
    public boolean isReadAcknowledgment() {
        return ((flags & FLAG_READ_ACK) == FLAG_READ_ACK);
    }

    /**
     * @return <code>true</code> if flag \USER is set; otherwise <code>false</code>
     */
    public boolean isUser() {
        return ((flags & FLAG_USER) == FLAG_USER);
    }

    /**
     * @return <code>true</code> if flags is set; otherwise <code>false</code>
     */
    public boolean containsFlags() {
        return b_flags;
    }

    /**
     * Removes the flags
     */
    public void removeFlags() {
        flags = 0;
        b_flags = false;
    }

    /**
     * Sets the flags
     *
     * @param flags the flags to set
     */
    public void setFlags(int flags) {
        this.flags = flags;
        b_flags = true;
    }

    /**
     * Sets a system flag
     *
     * @param flag The system flag to set
     * @param enable <code>true</code> to enable; otherwise <code>false</code>
     * @throws OXException If an illegal flag argument is specified
     */
    public void setFlag(int flag, boolean enable) throws OXException {
        if ((flag == 1) || ((flag % 2) != 0)) {
            throw MailExceptionCode.ILLEGAL_FLAG_ARGUMENT.create(Integer.valueOf(flag));
        }
        flags = enable ? (flags | flag) : (flags & ~flag);
        b_flags = true;
    }

    /**
     * Gets the previous \Seen state.
     * <p>
     * This flag is used when writing the message later on. There a check is performed whether header
     * <code>Disposition-Notification-To</code> is indicated or not.
     *
     * @return the previous \Seen state
     */
    public boolean isPrevSeen() {
        return prevSeen;
    }

    /**
     * @return <code>true</code> if previous \Seen state is set; otherwise <code>false</code>
     */
    public boolean containsPrevSeen() {
        return b_prevSeen;
    }

    /**
     * Removes the previous \Seen state
     */
    public void removePrevSeen() {
        prevSeen = false;
        b_prevSeen = false;
    }

    /**
     * Sets the previous \Seen state.
     * <p>
     * This flag is used when writing the message later on. There a check is performed whether header
     * <code>Disposition-Notification-To</code> is indicated or not.
     *
     * @param prevSeen the previous \Seen state to set
     */
    public void setPrevSeen(boolean prevSeen) {
        this.prevSeen = prevSeen;
        b_prevSeen = true;
    }

    /**
     * Gets the threadLevel
     *
     * @return the threadLevel
     */
    public int getThreadLevel() {
        return threadLevel;
    }

    /**
     * @return <code>true</code> if threadLevel is set; otherwise <code>false</code>
     */
    public boolean containsThreadLevel() {
        return b_threadLevel;
    }

    /**
     * Removes the threadLevel
     */
    public void removeThreadLevel() {
        threadLevel = 0;
        b_threadLevel = false;
    }

    /**
     * Sets the threadLevel
     *
     * @param threadLevel the threadLevel to set
     */
    public void setThreadLevel(int threadLevel) {
        this.threadLevel = threadLevel;
        b_threadLevel = true;
    }

    /**
     * Gets the subject
     *
     * @return the subject
     */
    public String getSubject() {
        if (!b_subject) {
            final String subjectStr = MimeMessageUtility.checkNonAscii(getFirstHeader(MessageHeaders.HDR_SUBJECT));
            if (subjectStr != null) {
                setSubject(decodeMultiEncodedHeader(subjectStr), true);
            }
        }
        return subject;
    }

    /**
     * @return <code>true</code> if subject is set; otherwise <code>false</code>
     */
    public boolean containsSubject() {
        return b_subject || containsHeader(MessageHeaders.HDR_SUBJECT);
    }

    /**
     * Removes the subject
     */
    public void removeSubject() {
        subject = null;
        removeHeader(MessageHeaders.HDR_SUBJECT);
        b_subject = false;
    }

    /**
     * Sets the subject
     *
     * @param subject the subject to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
        b_subject = true;
    }

    /**
     * Sets the subject
     *
     * @param subject The subject to set
     * @param decoded <code>true</code> if ensured to be decoded; otherwise <code>false</code>
     */
    public void setSubject(String subject, boolean decoded) {
        this.subject = subject;
        b_subject = true;
        this.subjectDecoded = decoded;
    }

    /**
     * Checks whether subject is ensured to be decoded.
     *
     * @return <code>true</code> if decoded; otherwise <code>false</code>
     */
    public boolean isSubjectDecoded() {
        return subjectDecoded;
    }

    private static final MailDateFormat MAIL_DATE_FORMAT;

    static {
        MAIL_DATE_FORMAT = new MailDateFormat();
        MAIL_DATE_FORMAT.setTimeZone(TimeZoneUtils.getTimeZone("GMT"));
    }

    /**
     * Gets the sent date which corresponds to <i>Date</i> header
     *
     * @return the sent date
     */
    public Date getSentDate() {
        if (!b_sentDate) {
            final String sentDateStr = getFirstHeader(MessageHeaders.HDR_DATE);
            if (sentDateStr != null) {
                synchronized (MAIL_DATE_FORMAT) {
                    try {
                        final Date parsedDate = MAIL_DATE_FORMAT.parse(sentDateStr);
                        if (null != parsedDate) {
                            setSentDate(parsedDate);
                        }
                    } catch (java.text.ParseException e) {
                        LOG.warn("Date string could not be parsed: {}", sentDateStr, e);
                    }
                }
            }
        }
        final Date sentDate = this.sentDate;
        return sentDate == null ? null : new Date(sentDate.getTime());
    }

    /**
     * @return <code>true</code> if sent date is set; otherwise <code>false</code>
     */
    public boolean containsSentDate() {
        return b_sentDate || containsHeader(MessageHeaders.HDR_DATE);
    }

    /**
     * Removes the sent date
     */
    public void removeSentDate() {
        sentDate = null;
        removeHeader(MessageHeaders.HDR_DATE);
        b_sentDate = false;
    }

    /**
     * Sets the sent date
     *
     * @param sentDate the sent date to set
     */
    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate == null ? null : new Date(sentDate.getTime());
        b_sentDate = true;
    }

    /**
     * Gets the received date which represents the internal timestamp set by mail server on arrival.
     *
     * @return The received date
     */
    public Date getReceivedDate() {
        final Date receivedDate = this.receivedDate;
        return receivedDate == null ? null : new Date(receivedDate.getTime());
    }

    /**
     * Gets the received date directly which represents the internal timestamp set by mail server on arrival.
     *
     * @return The received date
     */
    public Date getReceivedDateDirect() {
        return receivedDate;
    }

    /**
     * @return <code>true</code> if received date is set; otherwise <code>false</code>
     */
    public boolean containsReceivedDate() {
        return b_receivedDate;
    }

    /**
     * Removes the received date
     */
    public void removeReceivedDate() {
        receivedDate = null;
        b_receivedDate = false;
    }

    /**
     * Sets the received date
     *
     * @param receivedDate the received date to set
     */
    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate == null ? null : new Date(receivedDate.getTime());
        b_receivedDate = true;
    }

    /**
     * Adds given user flag
     *
     * @param userFlag The user flag to add
     */
    public void addUserFlag(String userFlag) {
        if (userFlag == null) {
            return;
        } else if (userFlags == null) {
            userFlags = new LinkedHashSet<>();
            b_userFlags = true;
        }
        userFlags.add(HeaderName.valueOf(userFlag));
    }

    /**
     * Adds given user flags
     *
     * @param userFlags The user flags to add
     */
    public void addUserFlags(String[] userFlags) {
        if (userFlags == null) {
            return;
        } else if (this.userFlags == null) {
            this.userFlags = new LinkedHashSet<>();
            b_userFlags = true;
        }
        for (String userFlag : userFlags) {
            this.userFlags.add(HeaderName.valueOf(userFlag));
        }
    }

    /**
     * Adds given user flags
     *
     * @param userFlags The user flags to add
     */
    public void addUserFlags(Collection<String> userFlags) {
        if (userFlags == null) {
            return;
        } else if (this.userFlags == null) {
            this.userFlags = new LinkedHashSet<>();
            b_userFlags = true;
        }
        for (String userFlag : userFlags) {
            this.userFlags.add(HeaderName.valueOf(userFlag));
        }
    }

    /**
     * @return <code>true</code> if userFlags is set; otherwise <code>false</code>
     */
    public boolean containsUserFlags() {
        return b_userFlags;
    }

    /**
     * Removes the userFlags
     */
    public void removeUserFlags() {
        userFlags = null;
        b_userFlags = false;
    }

    /**
     * Gets the user flags
     *
     * @return The user flags
     */
    public String[] getUserFlags() {
        if (containsUserFlags() && (null != userFlags)) {
            final int size = userFlags.size();
            if (size <= 0) {
                return Strings.getEmptyStrings();
            }
            final List<String> retval = new ArrayList<>(size);
            final Iterator<HeaderName> iter = userFlags.iterator();
            for (int i = size; i-- > 0;) {
                retval.add(iter.next().toString());
            }
            return retval.toArray(new String[size]);
        }
        return Strings.getEmptyStrings();
    }

    /**
     * Gets the user flags as set
     *
     * @return The user flags
     */
    public Set<String> getUserFlagsAsSet() {
        if (containsUserFlags() && (null != userFlags)) {
            final int size = userFlags.size();
            if (size <= 0) {
                return Collections.emptySet();
            }
            final Set<String> retval = new java.util.HashSet<>(size);
            final Iterator<HeaderName> iter = userFlags.iterator();
            for (int i = size; i-- > 0;) {
                retval.add(iter.next().toString());
            }
            return retval;
        }
        return Collections.emptySet();
    }

    /**
     * Gets the color label
     *
     * @return the color label
     */
    public int getColorLabel() {
        return colorLabel;
    }

    /**
     * @return <code>true</code> if color label is set; otherwise <code>false</code>
     */
    public boolean containsColorLabel() {
        return b_colorLabel;
    }

    /**
     * Removes the color label
     */
    public void removeColorLabel() {
        colorLabel = COLOR_LABEL_NONE;
        b_colorLabel = false;
    }

    /**
     * Sets the color label
     *
     * @param colorLabel the color label to set
     */
    public void setColorLabel(int colorLabel) {
        this.colorLabel = colorLabel;
        b_colorLabel = true;
    }

    /**
     * Gets the priority
     *
     * @return the priority
     */
    public int getPriority() {
        if (!b_priority) {
            final String imp = getFirstHeader(MessageHeaders.HDR_IMPORTANCE);
            if (imp != null) {
                setPriority(MimeMessageUtility.parseImportance(imp));
            } else {
                final String prioStr = getFirstHeader(MessageHeaders.HDR_X_PRIORITY);
                if (prioStr != null) {
                    setPriority(MimeMessageUtility.parsePriority(prioStr));
                }
            }
        }
        return priority;
    }

    /**
     * @return <code>true</code> if priority is set; otherwise <code>false</code>
     */
    public boolean containsPriority() {
        return b_priority || containsHeader(MessageHeaders.HDR_IMPORTANCE) || containsHeader(MessageHeaders.HDR_X_PRIORITY);
    }

    /**
     * Removes the priority
     */
    public void removePriority() {
        priority = PRIORITY_NORMAL;
        removeHeader(MessageHeaders.HDR_IMPORTANCE);
        removeHeader(MessageHeaders.HDR_X_PRIORITY);
        b_priority = false;
    }

    /**
     * Sets the priority
     *
     * @param priority the priority to set
     */
    public void setPriority(int priority) {
        this.priority = priority;
        b_priority = true;
    }

    /**
     * Gets the dispositionNotification
     *
     * @return the dispositionNotification
     */
    public InternetAddress getDispositionNotification() {
        if (!b_dispositionNotification) {
            final String dispNotTo = getFirstHeader(MessageHeaders.HDR_DISP_NOT_TO);
            if (dispNotTo != null) {
                try {
                    setDispositionNotification(new QuotedInternetAddress(dispNotTo, false));
                } catch (AddressException e) {
                    LOG.debug("", e);
                    setDispositionNotification(new PlainTextAddress(dispNotTo));
                }
            }
        }
        return dispositionNotification;
    }

    /**
     * @return <code>true</code> if dispositionNotification is set; otherwise <code>false</code>
     */
    public boolean containsDispositionNotification() {
        return b_dispositionNotification || containsHeader(MessageHeaders.HDR_DISP_NOT_TO);
    }

    /**
     * Removes the dispositionNotification
     */
    public void removeDispositionNotification() {
        dispositionNotification = null;
        removeHeader(MessageHeaders.HDR_DISP_NOT_TO);
        b_dispositionNotification = false;
    }

    /**
     * Sets the dispositionNotification
     *
     * @param dispositionNotification the dispositionNotification to set
     */
    public void setDispositionNotification(InternetAddress dispositionNotification) {
        this.dispositionNotification = dispositionNotification;
        b_dispositionNotification = true;
    }

    /**
     * Gets the original folder
     *
     * @return the original folder
     */
    public FullnameArgument getOriginalFolder() {
        return originalFolder;
    }

    /**
     * @return <code>true</code> if original folder is set; otherwise <code>false</code>
     */
    public boolean containsOriginalFolder() {
        return b_originalFolder;
    }

    /**
     * Removes the original folder
     */
    public void removeOriginalFolder() {
        originalFolder = null;
        b_originalFolder = false;
    }

    /**
     * Sets the original folder
     *
     * @param originalFolder the original folder to set
     */
    public void setOriginalFolder(FullnameArgument originalFolder) {
        this.originalFolder = originalFolder;
        b_originalFolder = true;
    }

    /**
     * Gets the text preview
     *
     * @return the text preview
     */
    public String getTextPreview() {
        return textPreview;
    }

    /**
     * @return <code>true</code> if text preview is set; otherwise <code>false</code>
     */
    public boolean containsTextPreview() {
        return b_textPreview;
    }

    /**
     * Removes the text preview
     */
    public void removeTextPreview() {
        textPreview = null;
        b_textPreview = false;
    }

    /**
     * Sets the text preview
     *
     * @param textPreview the text preview to set
     */
    public void setTextPreview(String textPreview) {
        this.textPreview = textPreview;
        b_textPreview = true;
    }

    /**
     * Gets the mail structure
     * <p>
     * The mail structure is optionally set when caller provides either of the fields:
     * <ul>
     * <li>{@link MailField#CONTENT_TYPE}</li>
     * <li>{@link MailField#ATTACHMENT}</li>
     * <li>{@link MailField#MIME_TYPE}</li>
     * </ul>
     *
     * @return The mail structure
     */
    public MailStructure getMailStructure() {
        return mailStructure;
    }

    /**
     * @return <code>true</code> if mail structure is set; otherwise <code>false</code>
     */
    public boolean containsMailStructure() {
        return b_mailStructure;
    }

    /**
     * Removes the mail structure
     */
    public void removeMailStructure() {
        mailStructure = null;
        b_mailStructure = false;
    }

    /**
     * Sets the mail structure
     *
     * @param mailStructure The mail structure to set
     */
    public void setMailStructure(MailStructure mailStructure) {
        this.mailStructure = mailStructure;
        b_mailStructure = true;
    }

    /**
     * Gets the original identifier
     *
     * @return the original identifier
     */
    public String getOriginalId() {
        return originalId;
    }

    /**
     * @return <code>true</code> if original identifier is set; otherwise <code>false</code>
     */
    public boolean containsOriginalId() {
        return b_originalId;
    }

    /**
     * Removes the original identifier
     */
    public void removeOriginalId() {
        originalId = null;
        b_originalId = false;
    }

    /**
     * Sets the original identifier
     *
     * @param originalId the original identifier to set
     */
    public void setOriginalId(String originalId) {
        this.originalId = originalId;
        b_originalId = true;
    }

    /**
     * Gets the folder
     *
     * @return the folder
     */
    public String getFolder() {
        return folder;
    }

    /**
     * @return <code>true</code> if folder is set; otherwise <code>false</code>
     */
    public boolean containsFolder() {
        return b_folder;
    }

    /**
     * Removes the folder
     */
    public void removeFolder() {
        folder = null;
        b_folder = false;
    }

    /**
     * Sets the folder
     *
     * @param folder the folder to set
     */
    public void setFolder(String folder) {
        this.folder = folder;
        b_folder = true;
    }

    /**
     * Gets the account ID.
     *
     * @return The account ID
     */
    public int getAccountId() {
        return accountId;
    }

    /**
     * @return <code>true</code> if account ID is set; otherwise <code>false</code>
     */
    public boolean containsAccountId() {
        return b_accountId;
    }

    /**
     * Removes the account ID.
     */
    public void removeAccountId() {
        accountId = 0;
        b_accountId = false;
    }

    /**
     * Sets the account ID.
     *
     * @param accountId The account ID
     */
    public void setAccountId(int accountId) {
        this.accountId = accountId;
        b_accountId = true;
    }

    /**
     * Gets the account Name
     *
     * @return The account name
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * @return <code>true</code> if account name is set; otherwise <code>false</code>
     */
    public boolean containsAccountName() {
        return b_accountName;
    }

    /**
     * Removes the account name.
     */
    public void removeAccountName() {
        accountName = null;
        b_accountName = false;
    }

    /**
     * Sets the account Name
     *
     * @param accountName The account name
     */
    public void setAccountName(String accountName) {
        this.accountName = accountName;
        b_accountName = true;
    }

    /**
     * Checks if this mail message is marked to contain (file) attachments
     *
     * @return <code>true</code> if this mail message is marked to contain (file) attachments; otherwise <code>false</code>
     */
    public boolean hasAttachment() {
        return b_hasAttachment ? hasAttachment : alternativeHasAttachment;
    }

    /**
     * Gets the has-attachment flag
     *
     * @return the has-attachment flag
     */
    public boolean isHasAttachment() {
        return hasAttachment;
    }

    /**
     * @return <code>true</code> if has-attachment flag is set; otherwise <code>false</code>
     */
    public boolean containsHasAttachment() {
        return b_hasAttachment;
    }

    /**
     * Removes the has-attachment flag
     */
    public void removeHasAttachment() {
        hasAttachment = false;
        b_hasAttachment = false;
    }

    /**
     * Sets the has-attachment flag
     *
     * @param hasAttachment the has-attachment flag to set
     */
    public void setHasAttachment(boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
        b_hasAttachment = true;
    }

    /**
     * Gets the alternative has-attachment flag
     *
     * @return the alternative has-attachment flag
     */
    public boolean isAlternativeHasAttachment() {
        return alternativeHasAttachment;
    }

    /**
     * @return <code>true</code> if alternative has-attachment flag is set; otherwise <code>false</code>
     */
    public boolean containsAlternativeHasAttachment() {
        return b_alternativeHasAttachment;
    }

    /**
     * Removes the alternative has-attachment flag
     */
    public void removeAlternativeHasAttachment() {
        alternativeHasAttachment = false;
        b_alternativeHasAttachment = false;
    }

    /**
     * Sets the alternative has-attachment flag
     *
     * @param hasAttachment the alternative has-attachment flag to set
     */
    public void setAlternativeHasAttachment(boolean hasAttachment) {
        this.alternativeHasAttachment = hasAttachment;
        b_alternativeHasAttachment = true;
    }

    @Override
    public Object clone() {
        final MailMessage clone = (MailMessage) super.clone();
        if (from != null) {
            clone.from = new LinkedHashSet<>(from);
        }
        if (to != null) {
            clone.to = new LinkedHashSet<>(to);
        }
        if (cc != null) {
            clone.cc = new LinkedHashSet<>(cc);
        }
        if (bcc != null) {
            clone.bcc = new LinkedHashSet<>(bcc);
        }
        if (sender != null) {
            clone.sender = new LinkedHashSet<>(sender);
        }
        if (receivedDate != null) {
            clone.receivedDate = new Date(receivedDate.getTime());
        }
        if (sentDate != null) {
            clone.sentDate = new Date(sentDate.getTime());
        }
        if (userFlags != null) {
            clone.userFlags = new LinkedHashSet<>(userFlags);
        }
        return clone;
    }

    /**
     * Gets the appendVCard
     *
     * @return the appendVCard
     */
    public boolean isAppendVCard() {
        return appendVCard;
    }

    /**
     * @return <code>true</code> if appendVCard is set; otherwise <code>false</code>
     */
    public boolean containsAppendVCard() {
        return b_appendVCard;
    }

    /**
     * Removes the appendVCard
     */
    public void removeAppendVCard() {
        appendVCard = false;
        b_appendVCard = false;
    }

    /**
     * Sets the appendVCard
     *
     * @param appendVCard the appendVCard to set
     */
    public void setAppendVCard(boolean appendVCard) {
        this.appendVCard = appendVCard;
        b_appendVCard = true;
    }

    /**
     * Gets the number of recent mails in associated folder.
     *
     * @return The recent count
     */
    public int getRecentCount() {
        return recentCount;
    }

    /**
     * @return <code>true</code> if number of recent mails is set; otherwise <code>false</code>
     */
    public boolean containsRecentCount() {
        return b_recentCount;
    }

    /**
     * Removes the recent count.
     */
    public void removeRecentCount() {
        recentCount = 0;
        b_recentCount = false;
    }

    /**
     * Sets the number of recent mails in associated folder.
     *
     * @param recentCount The recent count
     */
    public void setRecentCount(int recentCount) {
        this.recentCount = recentCount;
        b_recentCount = true;
    }

    /**
     * Gets the mail path.
     *
     * @param accountId The account ID
     * @return The mail path
     */
    public MailPath getMailPath() {
        return new MailPath(getAccountId(), getFolder(), getMailId());
    }

    /**
     * Gets the <i>Message-Id</i> value.
     *
     * @return The <i>Message-Id</i> value or <code>null</code>
     */
    public String getMessageId() {
        if (!b_messageId) {
            final String messageId = getFirstHeader(HDR_MESSAGE_ID);
            if (messageId == null) {
                return null;
            }
            setMessageId(messageId);
        }
        return this.messageId;
    }

    /**
     * @return <code>true</code> if <i>Message-Id</i> is set; otherwise <code>false</code>
     */
    public boolean containsMessageId() {
        return b_messageId;
    }

    /**
     * Removes the <i>Message-Id</i>.
     */
    public void removeMessageId() {
        messageId = null;
        b_messageId = false;
    }

    /**
     * Sets the <i>Message-Id</i>.
     *
     * @param sReferences The <i>Message-Id</i> header value
     */
    public void setMessageId(String messageId) {
        b_messageId = true;
        this.messageId = messageId;
    }

    /**
     * Gets the <i>In-Reply-To</i> value.
     *
     * @return The <i>In-Reply-To</i> value or <code>null</code>
     */
    public String getInReplyTo() {
        return getFirstHeader(HDR_IN_REPLY_TO);
    }

    private static final Pattern SPLIT = Pattern.compile(" +");

    /**
     * Gets the <i>References</i>.
     *
     * @return The <i>References</i> or <code>null</code>
     */
    public String[] getReferences() {
        if (!b_references) {
            final String references = getFirstHeader(HDR_REFERENCES);
            if (references == null) {
                return null;
            }
            setReferences(SPLIT.split(MimeMessageUtility.decodeMultiEncodedHeader(references)));
        }
        return this.references;
    }

    /**
     * Gets the <i>References</i> in first order, falls back to <i>In-Reply-To</i> value if absent
     *
     * @return Either the <i>References</i>/<i>In-Reply-To</i> value or <code>null</code> if none available
     */
    public String[] getReferencesOrInReplyTo() {
        String[] references = getReferences();
        if (null != references) {
            return references;
        }
        String inReplyTo = getInReplyTo();
        return null == inReplyTo ? null : new String[] { inReplyTo };
    }

    /**
     * @return <code>true</code> if <i>References</i> is set; otherwise <code>false</code>
     */
    public boolean containsReferences() {
        return b_references;
    }

    /**
     * Removes the <i>References</i>.
     */
    public void removeReferences() {
        references = null;
        b_references = false;
    }

    /**
     * Sets the <i>References</i>.
     *
     * @param sReferences The <i>References</i> header value
     */
    public void setReferences(String sReferences) {
        if (null == sReferences) {
            this.references = null;
            b_references = true;
        } else {
            setReferences(SPLIT.split(MimeMessageUtility.decodeMultiEncodedHeader(sReferences)));
        }
    }

    /**
     * Sets the <i>References</i>.
     *
     * @param references The <i>References</i>
     */
    public void setReferences(String[] references) {
        if (null == references) {
            this.references = null;
        } else {
            final int length = references.length;
            this.references = new String[length];
            System.arraycopy(references, 0, this.references, 0, length);
        }
        b_references = true;
    }


    /**
     * Sets the security info (encrypted, signed, etc)
     *
     * @param securityInfo The security info to set
     */
    public void setSecurityInfo(SecurityInfo securityInfo) {
        this.securityInfo = securityInfo;
        b_securityInfo = (securityInfo != null);
    }

    /**
     * Gets the security info (encypted, signed, etc)
     *
     * @return The security info or <code>null</code>
     */
    public SecurityInfo getSecurityInfo () {
        return this.securityInfo;
    }

    /**
     * Checks if security info is contained
     *
     * @return <code>true</code> if contained; otherwise <code>false</code>
     */
    public boolean containsSecurityInfo () {
        return b_securityInfo;
    }

    /**
     * Removes the security info
     */
    public void removeSecurityInfo () {
        this.securityInfo = null;
        b_securityInfo = false;
    }

    /**
     * Sets the given security result.
     *
     * @param result The security result to set
     */
    public void setSecurityResult(SecurityResult result) {
        this.securityResult = result;
        b_securityResult = true;
    }

    /**
     * Gets the security result
     *
     * @return The security result or <code>null</code> if not set
     */
    public SecurityResult getSecurityResult() {
        return this.securityResult;
    }

    /**
     * Checks if security result is available
     *
     * @return <code>true</code> if available; otherwise <code>false</code>
     */
    public boolean hasSecurityResult() {
        return securityResult != null;
    }

    /**
     * Checks if security result has been set
     *
     * @return <code>true</code> if set; otherwise <code>false</code>
     */
    public boolean containsSecurityResult() {
        return b_securityResult;
    }

    /**
     * Removes the security result.
     */
    public void removeSecurityResult() {
        this.securityResult = null;
        b_securityResult = false;
    }

    /**
     * Sets the given authentication result for this mail.
     *
     * @param result The authentication result to set
     */
    public void setAuthenticityResult(MailAuthenticityResult authenticationResult) {
        this.authenticityResult = authenticationResult;
        b_authenticityResult = true;
    }

    /**
     * Gets the authentication result for this mail.
     *
     * @return The authentication result or <code>null</code> if not set
     */
    public MailAuthenticityResult getAuthenticityResult() {
        return this.authenticityResult;
    }

    /**
     * Checks if authentication result is available.
     *
     * @return <code>true</code> if available; otherwise <code>false</code>
     */
    public boolean hasAuthenticityResult() {
        return authenticityResult != null;
    }

    /**
     * Checks if authentication result has been set for this mail.
     *
     * @return <code>true</code> if set; otherwise <code>false</code>
     */
    public boolean containsAuthenticityResult() {
        return b_authenticityResult;
    }

    /**
     * Removes the authentication result from this mail.
     */
    public void removeAuthenticityResult() {
        this.authenticityResult = null;
        b_authenticityResult = false;
    }

    /**
     * Gets the implementation-specific unique ID of this mail in its mail folder. The ID returned by this method is used in storages to
     * refer to a mail.
     *
     * @return The ID of this mail or <code>null</code> if not available.
     */
    public abstract String getMailId();

    /**
     * Sets the implementation-specific unique mail ID of this mail in its mail folder. The ID returned by this method is used in storages
     * to refer to a mail.
     *
     * @param id The mail ID or <code>null</code> to indicate its absence
     */
    public abstract void setMailId(String id);

    /**
     * Gets the number of unread messages
     *
     * @return The number of unread messages
     */
    public abstract int getUnreadMessages();

    /**
     * Sets the number of unread messages
     *
     * @param unreadMessages The number of unread messages
     */
    public abstract void setUnreadMessages(int unreadMessages);
}