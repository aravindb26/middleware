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

package com.openexchange.admin.contextrestore.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.output.NullWriter;
import com.openexchange.admin.contextrestore.dataobjects.PoolIdSchemaAndVersionInfo;
import com.openexchange.admin.contextrestore.dataobjects.UpdateTaskEntry;
import com.openexchange.admin.contextrestore.dataobjects.UpdateTaskInformation;
import com.openexchange.admin.contextrestore.rmi.exceptions.OXContextRestoreException;
import com.openexchange.admin.contextrestore.rmi.exceptions.OXContextRestoreException.Code;
import com.openexchange.admin.contextrestore.rmi.impl.OXContextRestore;

/**
 * Parser for MySQL dump files.
 */
public class Parser {

    private final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Parser.class);

    private final static Pattern PATTERN_COMMENT_DATABASE = Pattern.compile("^.*?(?:Current )?Database:\\s+`?([^` ]*)`?.*$");

    private final static Pattern PATTERN_COMMENT_TABLE = Pattern.compile("^Table\\s+structure\\s+for\\s+table\\s+`([^`]*)`.*$");

    private final static Pattern PATTERN_COMMENT_DATADUMP = Pattern.compile("^Dumping\\s+data\\s+for\\s+table\\s+`([^`]*)`.*$");

    private static enum State {

        /** Undefined state; e.g. premature end of input */
        UNDEFINED,

        /** Start of the parsing machine. */
        START,

        /** Read a starting dash: <code>"-"</code>. Possibly from a starting comment line */
        STARTED_COMMENT_LINE,

        /** Read comment prefix: <code>"--"</code> */
        READ_COMMENT_PREFIX,

        /** Found a comment announcing a table <code>CREATE</code> statement;<br>
         * e.g. <code>"Table structure for table `xyz`"</code> */
        TABLE_FOUND,

        /** Found the respective <code>CREATE</code> statement for previous comment announcing that table creation */
        CREATE_FOUND,

        /** Found a comment announcing one or more <code>INSERT</code> statements to dump data into a table;<br>
         * e.g. <code>"Dumping data for table `xyz`"</code> */
        TABLE_CONTENT_FOUND,

        /** Found the respective <code>INSERT</code> statement(s) for previous comment announcing that data dump */
        TABLE_INSERT_FOUND;

    }

    /**
     * Initializes a new {@link Parser}.
     */
    private Parser() {
        super();
    }

    /**
     * Starts parsing named MySQL dump file
     *
     * @param cid The identifier of the context that shall be restored
     * @param fileName The name of the MySQL dump file
     * @param optConfigDbName The optional name of the ConfigDB schema
     * @param schema The name of the database schema in which to-restore context resides
     * @return The information object for parsed MySQL dump file
     * @throws IOException If an I/O error occurs
     * @throws OXContextRestoreException If a context restore error occurs
     */
    public static PoolIdSchemaAndVersionInfo start(final int cid, final String fileName, final String optConfigDbName, String schema, final Map<String, File> tempfilemap) throws IOException, OXContextRestoreException {
        String scheema = schema;
        int c;
        State state = State.START;
        State oldstate = State.START;
        int cidpos = -1;
        String tableName = null;
        // Set if a database is found in which the search for cid should be done
        boolean furthersearch = true;
        // Defines if we have found a contextserver2pool table
        boolean searchcontext = false;
        // boolean searchdbpool = false;
        int poolId = -1;
        UpdateTaskInformation updateTaskInformation = null;

        BufferedReader in = null;
        BufferedWriter bufferedWriter = null;
        try {
            in = new BufferedReader(new FileReader(fileName));
            while ((c = in.read()) != -1) {
                if (State.START == state && c == '-') {
                    state = State.STARTED_COMMENT_LINE; // Started comment line
                    continue;
                } else if (State.STARTED_COMMENT_LINE == state) {
                    if (c == '-') {
                        state = State.READ_COMMENT_PREFIX; // Read comment prefix "--"
                        continue;
                    }
                    // Not a comment prefix; an interpretable line
                    state = oldstate;
                    continue;
                } else if (State.READ_COMMENT_PREFIX == state) {
                    if (c == ' ') { // Comment line: "-- " + <rest-of-line>
                        searchcontext = false;
                        final String readLine = in.readLine();
                        if (null == readLine) {
                            continue;
                        }

                        Matcher dbmatcher = PATTERN_COMMENT_DATABASE.matcher(readLine);
                        Matcher tablematcher = PATTERN_COMMENT_TABLE.matcher(readLine);
                        Matcher datadumpmatcher = PATTERN_COMMENT_DATADUMP.matcher(readLine);

                        if (dbmatcher.matches()) {
                            // Database found
                            final String databasename = dbmatcher.group(1);
                            if (OXContextRestore.getConfigDbName(optConfigDbName).equals(databasename) || (null != scheema && scheema.equals(databasename))) {
                                furthersearch = true;
                                LOG.info("Database: {}", databasename);
                                if (null != bufferedWriter) {
                                    bufferedWriter.append("/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;\n");
                                    bufferedWriter.flush();
                                    bufferedWriter.close();
                                }

                                if (!tempfilemap.containsKey(databasename)) {
                                    final File createTempFile = File.createTempFile(databasename, null);
                                    tempfilemap.put(databasename, createTempFile);
                                    bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(createTempFile), StandardCharsets.UTF_8));
                                    bufferedWriter.append("/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;\n");
                                } else {
                                    // We are in the seconds pass so we don't need to write the configdb entries again
                                    bufferedWriter = new BufferedWriter(new NullWriter());
                                }
                                // Reset values
                                cidpos = -1;
                                state = State.START;
                                oldstate = State.START;
                            } else {
                                furthersearch = false;
                            }
                        } else if (furthersearch && tablematcher.matches()) {
                            // Table found
                            tableName = tablematcher.group(1);
                            LOG.info("Table: {}", tableName);
                            cidpos = -1;
                            oldstate = State.START;
                            state = State.TABLE_FOUND;
                        } else if (furthersearch && datadumpmatcher.matches()) {
                            // Content found
                            LOG.info("Dump found");
                            if ("updateTask".equals(tableName)) {
                                // One or more entries for 'updateTask' table
                                updateTaskInformation = searchAndCheckUpdateTask(in, cid);
                            }
                            if ("context_server2db_pool".equals(tableName)) {
                                searchcontext = true;
                            }
                            // if ("db_pool".equals(table_name)) {
                            // // As the table in the dump are sorted alphabetically it's safe to
                            // // assume that we have the pool id here
                            // searchdbpool = true;
                            // }
                            state = State.TABLE_CONTENT_FOUND;
                            oldstate = State.START;
                        } else {
                            state = State.START;
                            oldstate = State.START;
                        }
                        continue;
                    }
                    // Reset to old state
                    state = oldstate;
                } else if (State.TABLE_FOUND == state && c == 'C') {
                    final String creatematchpart = "REATE";
                    state = returnRightStateToString(in, creatematchpart, State.CREATE_FOUND, State.TABLE_FOUND);
                    continue;
                } else if (State.TABLE_FOUND == state && c == '-') {
                    oldstate = State.TABLE_FOUND;
                    state = State.STARTED_COMMENT_LINE;
                    continue;
                } else if (State.CREATE_FOUND == state && c == '(') {
                    cidpos = cidsearch(in);
                    LOG.info("Cid pos: {}", Integer.valueOf(cidpos));
                    state = State.START;
                    continue;
                } else if (State.TABLE_CONTENT_FOUND == state && c == 'I') {
                    state = returnRightStateToString(in, "NSERT", State.TABLE_INSERT_FOUND, State.TABLE_CONTENT_FOUND);
                    continue;
                } else if (State.TABLE_CONTENT_FOUND == state && c == '-') {
                    oldstate = State.TABLE_CONTENT_FOUND;
                    state = State.STARTED_COMMENT_LINE;
                } else if (State.TABLE_INSERT_FOUND == state && c == '(') {
                    LOG.info("Insert found and cid={}", Integer.valueOf(cidpos));
                    // Now we search for matching cids and write them to the tmp file
                    if (searchcontext && null != bufferedWriter) {
                        final String value[] =
                            searchAndWriteMatchingCidValues(in, bufferedWriter, cidpos, Integer.toString(cid), tableName, true, true);
                        if (value.length >= 2) {
                            try {
                                poolId = Integer.parseInt(value[1]);
                            } catch (NumberFormatException e) {
                                throw new OXContextRestoreException(Code.COULD_NOT_CONVERT_POOL_VALUE, e);
                            }
                            scheema = value[2];
                            // } else if (searchdbpool) {
                            // final String value[] = searchAndWriteMatchingCidValues(in, bufferedWriter, 1, Integer.toString(pool_id),
                            // table_name, true, false);
                            // searchdbpool = false;
                            // System.out.println(Arrays.toString(value));
                        } else {
                            state = State.TABLE_CONTENT_FOUND;
                            continue;
                        }
                    } else if (null != bufferedWriter) {
                        // Here we should only search if a fitting db was found and thus the writer was set
                        searchAndWriteMatchingCidValues(in, bufferedWriter, cidpos, Integer.toString(cid), tableName, false, true);
                    }
                    searchcontext = false;
                    oldstate = State.START;
                    state = State.TABLE_CONTENT_FOUND;
                }
                // Reset state machine at the end of the line if we are in the first two states
                if ((state == State.STARTED_COMMENT_LINE || state == State.READ_COMMENT_PREFIX) && c == '\n') {
                    state = State.START;
                    continue;
                }
            }
        } finally {
            flush(bufferedWriter);
            close(bufferedWriter, in);
        }
        //if (null == updateTaskInformation) {
        //    throw new OXContextRestoreException(Code.NO_UPDATE_TASK_INFORMATION_FOUND);
        // }
        return new PoolIdSchemaAndVersionInfo(fileName, cid, poolId, scheema, updateTaskInformation);
    }

    private final static String REGEX_VALUE = "([^\\),]*)";
    private final static Pattern insertIntoUpdateTaskValues =
        Pattern.compile("\\((?:" + REGEX_VALUE + ",)(?:" + REGEX_VALUE + ",)(?:" + REGEX_VALUE + ",)(?:" + REGEX_VALUE + ",)" + ".*?\\)");

    private static UpdateTaskInformation searchAndCheckUpdateTask(final BufferedReader in, final int contextId) throws IOException {
        final UpdateTaskInformation retval = new UpdateTaskInformation();
        String line = in.readLine();
        while ((line = in.readLine()) != null && !line.startsWith("--")) {
            if (line.startsWith("INSERT INTO `updateTask` VALUES ")) {
                final Matcher matcher = insertIntoUpdateTaskValues.matcher(line.substring(32));
                while (matcher.find()) {
                    final UpdateTaskEntry updateTaskEntry = new UpdateTaskEntry();
                    final int contextId2 = Integer.parseInt(matcher.group(1));
                    if (contextId2 <= 0 || contextId2 == contextId) {
                        updateTaskEntry.setContextId(contextId2);
                        updateTaskEntry.setTaskName(matcher.group(2).replaceAll("'", ""));
                        updateTaskEntry.setSuccessful((Integer.parseInt(matcher.group(3)) > 0));
                        updateTaskEntry.setLastModified(Long.parseLong(matcher.group(4)));
                        retval.add(updateTaskEntry);
                    }
                }
            }
        }
        return retval;
    }

    /**
     * @param in
     * @param bufferedWriter
     * @param valuepos The position of the value inside the value row
     * @param value The value itself
     * @param table_name
     * @param readall If the rest of the row should be returned as string array after a match or not
     * @param contextsearch
     * @throws IOException
     */
    private static String[] searchAndWriteMatchingCidValues(final BufferedReader in, final Writer bufferedWriter, final int valuepos, final String value, final String table_name, boolean readall, boolean contextsearch) throws IOException {
        final StringBuilder currentValues = new StringBuilder();
        currentValues.append('(');
        final StringBuilder lastpart = new StringBuilder();
        int c = 0;
        int counter = 1;
        // If we are inside a string '' or not
        boolean instring = false;
        // If we are inside a dataset () or not
        boolean indatarow = true;
        // Have we found the value we searched for?
        boolean found = false;
        // Is this the first time we found the value
        boolean firstfound = true;
        // Are we in escapted mode
        boolean escapted = false;
        // Used for only escaping one char
        boolean firstescaperun = false;
        // Used to leave the loop
        boolean continuation = true;
        final ArrayList<String> retval = new ArrayList<String>();
        while ((c = in.read()) != -1 && continuation) {
            if (firstescaperun && escapted) {
                escapted = false;
                firstescaperun = false;
            }
            if (escapted) {
                firstescaperun = true;
            }
            switch (c) {
            case '(':
                if (!indatarow) {
                    indatarow = true;
                    currentValues.setLength(0);
                    currentValues.append('(');
                } else {
                    currentValues.append((char) c);
                }
                break;
            case ')':
                if (indatarow) {
                    if (!instring) {
                        if (counter == valuepos) {
                            if (lastpart.toString().equals(value)) {
                                found = true;
                            }
                        } else if (readall && found) {
                            retval.add(lastpart.toString());
                        }
                        lastpart.setLength(0);
                        indatarow = false;
                        if (found && contextsearch) {
                            if (firstfound) {
                                bufferedWriter.write("INSERT INTO `");
                                bufferedWriter.write(table_name);
                                bufferedWriter.write("` VALUES ");
                                firstfound = false;
                            } else {
                                bufferedWriter.write(",");
                            }

                            bufferedWriter.write(currentValues.toString());
                            bufferedWriter.write(")");
                            bufferedWriter.flush();
                            found = false;
                        }
                    }
                    currentValues.append((char) c);
                }
                break;
            case ',':
                if (indatarow) {
                    if (!instring) {
                        if (counter == valuepos) {
                            if (lastpart.toString().equals(value)) {
                                found = true;
                            }
                        } else if (readall && found) {
                            retval.add(lastpart.toString());
                        }
                        counter++;
                        lastpart.setLength(0);
                    }
                    currentValues.append((char) c);
                } else {
                    // New datarow comes
                    counter = 1;
                }
                break;
            case '\'':
                if (indatarow) {
                    if (!instring) {
                        instring = true;
                    } else {
                        if (!escapted) {
                            instring = false;
                        }
                    }
                    currentValues.append((char) c);
                }
                break;
            case '\\':
                if (indatarow) {
                    if (instring && !escapted) {
                        escapted = true;
                    }
                    currentValues.append((char) c);
                }
                break;
            case ';':
                if (!indatarow) {
                    if (!firstfound && contextsearch) {
                        // End of VALUES part
                        bufferedWriter.write(";");
                        bufferedWriter.write("\n");
                    }
                    continuation = false;
                } else {
                    currentValues.append((char) c);
                }
                break;
            default:
                if (indatarow) {
                    lastpart.append((char) c);
                    currentValues.append((char) c);
                }
                break;
            }
        }
        return retval.toArray(new String[retval.size()]);
    }

    private static State returnRightStateToString(final BufferedReader in, final String string, State successstate, State failstate) throws IOException {
        final int length = string.length();
        char[] arr = new char[length];
        int i;
        if ((i = in.read(arr)) != -1 && length == i) {
            if (string.equals(new String(arr))) {
                return successstate;
            }
            return failstate;
        }
        // File at the end or no more chars
        return State.UNDEFINED;
    }

    private final static Pattern cidpattern = Pattern.compile(".*`cid`.*");
    private final static Pattern engine = Pattern.compile("^\\).*ENGINE=.*.*$");

    /**
     * Searches for the cid and returns the line number in which is was found, after this method the reader's position is behind the
     * create structure
     *
     * @param in
     * @return
     * @throws IOException
     */
    private static int cidsearch(final BufferedReader in) throws IOException {
        String readLine;
        readLine = in.readLine();
        int columnpos = 0;
        boolean found = false;
        while (null != readLine) {
            final Matcher cidmatcher = cidpattern.matcher(readLine);
            final Matcher enginematcher = engine.matcher(readLine);
            // Now searching for cid text...
            if (cidmatcher.matches()) {
                final List<String> searchingForeignKey = searchingForeignKey(in);
                LOG.info("Foreign Keys: {}", searchingForeignKey);
                found = true;
                break;
            } else if (enginematcher.matches()) {
                break;
            }
            columnpos++;
            readLine = in.readLine();
        }
        if (!found) {
            return -1;
        }
        return columnpos;
    }

    private final static Pattern foreignkey =
        Pattern.compile("^\\s+CONSTRAINT.*FOREIGN KEY\\s+\\(`([^`]*)`(?:,\\s+`([^`]*)`)*\\)\\s+REFERENCES `([^`]*)`.*$");

    private static List<String> searchingForeignKey(final BufferedReader in) throws IOException {
        String readLine;
        readLine = in.readLine();
        List<String> foreign_keys = null;
        while (null != readLine) {
            final Matcher matcher = foreignkey.matcher(readLine);
            final Matcher enginematcher = engine.matcher(readLine);
            if (matcher.matches()) {
                foreign_keys = get_foreign_keys(matcher);
            } else if (enginematcher.matches()) {
                return foreign_keys;
            }
            readLine = in.readLine();
        }
        return null;
    }

    private static List<String> get_foreign_keys(Matcher matcher) {
        final ArrayList<String> retval = new ArrayList<String>();
        final int groupCount = matcher.groupCount();
        for (int i = 1; i < groupCount; i++) {
            final String group = matcher.group(i);
            if (null != group) {
                retval.add(group);
            }
        }
        return retval;
    }

    /**
     * Safely closes specified {@link Closeable} instance.
     *
     * @param toClose The {@link Closeable} instance
     */
    private static void close(final Closeable... toClose) {
        if (null != toClose && toClose.length > 0) {
            for (Closeable closeable : toClose) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }
    }

    /**
     * Safely flushes specified {@link Flushable} instance.
     *
     * @param toFlush The {@link Flushable} instance
     */
    private static void flush(final Flushable... toFlush) {
        if (null != toFlush && toFlush.length > 0) {
            for (Flushable flushable : toFlush) {
                if (flushable != null) {
                    try {
                        flushable.flush();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }
    }

} // End of class "Parser"