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

package com.openexchange.imap.command;

import static com.openexchange.imap.IMAPCommandsCollection.prepareStringArgument;
import java.util.Arrays;
import javax.mail.MessagingException;
import com.openexchange.java.Strings;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;

/**
 * {@link CopyIMAPCommand} - Copies messages from given folder to given destination folder just using their sequence numbers/UIDs
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CopyIMAPCommand extends AbstractIMAPCommand<long[]> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CopyIMAPCommand.class);

    private static final long[] DEFAULT_RETVAL = new long[0];

    private final boolean uid;

    private final boolean fast;

    private final int length;

    private final String[] args;

    private final long[] uids;

    private final long[] retval;

    private final String destFolderName;

    private boolean proceed = true;

    /**
     * Constructor using sequence numbers and performs a fast <code>COPY</code> command; meaning optional <i>COPYUID</i> response is
     * discarded.
     *
     * @param imapFolder - the IMAP folder
     * @param startSeqNum - the starting sequence number of the messages that shall be copied
     * @param endSeqNum - the ending sequence number of the messages that shall be copied
     * @param destFolderName - the destination folder fullname
     * @throws MessagingException If a messaging error occurs
     */
    public CopyIMAPCommand(IMAPFolder imapFolder, int startSeqNum, int endSeqNum, String destFolderName) throws MessagingException {
        this(imapFolder, startend2long(startSeqNum, endSeqNum), destFolderName, true, true, false);
    }

    /**
     * Constructor using sequence numbers and performs a fast <code>COPY</code> command; meaning optional <i>COPYUID</i> response is
     * discarded.
     *
     * @param imapFolder - the IMAP folder
     * @param seqNums - the sequence numbers of the messages that shall be copied
     * @param destFolderName - the destination folder fullname
     * @param isSequential - whether sequence numbers are sequential or not
     * @throws MessagingException If a messaging error occurs
     */
    public CopyIMAPCommand(IMAPFolder imapFolder, int[] seqNums, String destFolderName, boolean isSequential) throws MessagingException {
        this(imapFolder, int2long(seqNums), destFolderName, isSequential, true, false);
    }

    /**
     * Constructor using UIDs and consequently performs a <code>UID COPY</code> command
     *
     * @param imapFolder - the IMAP folder
     * @param uids - the UIDs of the messages that shall be copied
     * @param destFolderName - the destination folder fullname
     * @param isSequential - whether UIDs are sequential or not
     * @param fast - <code>true</code> to ignore corresponding UIDs of copied messages and return value is empty (array of length zero)
     * @throws MessagingException If a messaging error occurs
     */
    public CopyIMAPCommand(IMAPFolder imapFolder, long[] uids, String destFolderName, boolean isSequential, boolean fast) throws MessagingException {
        this(imapFolder, uids, destFolderName, isSequential, fast, true);
    }

    private static final int LENGTH = 6; // "COPY <nums> <destination-folder>"

    private static final int LENGTH_WITH_UID = 10; // "UID COPY <nums> <destination-folder>"

    private CopyIMAPCommand(IMAPFolder imapFolder, long[] nums, String destFolderName, boolean isSequential, boolean fast, boolean uid) throws MessagingException {
        super(imapFolder);
        uids = nums == null ? DEFAULT_RETVAL : nums;
        this.uid = uid;
        if (imapFolder.getMessageCount() <= 0) {
            returnDefaultValue = true;
        } else {
            returnDefaultValue = (uids.length == 0);
        }
        this.fast = fast;
        this.destFolderName = prepareStringArgument(destFolderName);
        length = uids.length;
        args = length == 0 ? ARGS_EMPTY : (isSequential ? new String[] { new StringBuilder(64).append(uids[0]).append(':').append(
            uids[length - 1]).toString() } : IMAPNumArgSplitter.splitUIDArg(
            uids,
            false,
            (uid ? LENGTH_WITH_UID : LENGTH) + destFolderName.length()));
        if (fast) {
            retval = DEFAULT_RETVAL;
        } else {
            retval = new long[length];
            Arrays.fill(retval, -1);
        }
    }

    /**
     * Constructor to copy all messages of given folder to given destination folder by performing a <code>COPY 1:*</code> command.
     * <p>
     * <b>Note</b>: Ensure that denoted folder is not empty.
     *
     * @param imapFolder - the IMAP folder
     * @param destFolderName - the destination folder
     * @throws MessagingException If a messaging error occurs
     */
    public CopyIMAPCommand(IMAPFolder imapFolder, String destFolderName) throws MessagingException {
        super(imapFolder);
        final int messageCount = imapFolder.getMessageCount();
        if (messageCount <= 0) {
            returnDefaultValue = true;
        }
        fast = true;
        uid = false;
        uids = DEFAULT_RETVAL;
        this.destFolderName = prepareStringArgument(destFolderName);
        retval = DEFAULT_RETVAL;
        args = 1 == messageCount ? ARGS_FIRST : ARGS_ALL;
        length = -1;
    }

    @Override
    protected boolean addLoopCondition() {
        return (fast ? false : proceed);
    }

    @Override
    protected String[] getArgs() {
        return args;
    }

    @Override
    protected String getCommand(int argsIndex) {
        final StringBuilder sb = new StringBuilder(args[argsIndex].length() + 64);
        if (uid) {
            sb.append("UID ");
        }
        sb.append("COPY ");
        sb.append(args[argsIndex]);
        sb.append(' ').append(destFolderName);
        return sb.toString();
    }

    @Override
    protected long[] getDefaultValue() {
        return DEFAULT_RETVAL;
    }

    @Override
    protected long[] getReturnVal() {
        return retval;
    }

    private static final String COPYUID = "copyuid";

    @Override
    protected boolean handleResponse(Response response) throws MessagingException {
        if (fast || !response.isOK()) {
            return false;
        }
        /*-
         * Parse response:
         *
         * OK [COPYUID 1184051486 10031:10523,10525:11020,11022:11027,11030:11047,11050:11051,11053:11558 1024:2544] Completed
         *
         * * 45 EXISTS
         * * 2 RECENT
         * A4 OK [COPYUID 1185853191 7,32 44:45] Completed
         */
        String resp = Strings.asciiLowerCase(response.toString());
        int pos = resp.indexOf(COPYUID);
        if (pos < 0) {
            return false;
        }
        /*
         * Found COPYUID...
         */
        final COPYUIDResponse copyuidResp = new COPYUIDResponse(LOG);
        /*
         * Find next starting ATOM in IMAP response
         */
        pos += COPYUID.length();
        while (Strings.isWhitespace(resp.charAt(pos))) {
            pos++;
        }
        /*
         * Split by ATOMs
         */
        final String[] sa = Strings.splitByWhitespaces(resp.substring(pos));
        if (sa.length >= 3) {
            /*-
             * Array contains atoms like:
             *
             * "1167880112", "11937", "11939]", "Completed"
             */
            copyuidResp.src = sa[1];
            {
                final String destStr = sa[2];
                final int mlen = destStr.length() - 1;
                if (']' == destStr.charAt(mlen)) {
                    copyuidResp.dest = destStr.substring(0, mlen);
                } else {
                    copyuidResp.dest = destStr;
                }
            }
            copyuidResp.fillResponse(uids, retval);
        } else {
            LOG.error("Invalid COPYUID response: {}", resp);
        }
        proceed = false;
        return true;
    }

    private static long[] startend2long(int start, int end) {
        final long[] longArr = new long[2];
        longArr[0] = start;
        longArr[1] = end;
        return longArr;
    }

    private static long[] int2long(int[] intArr) {
        final long[] longArr = new long[intArr.length];
        System.arraycopy(intArr, 0, longArr, 0, intArr.length);
        return longArr;
    }
}
