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

package com.openexchange.database.tombstone.cleanup;

import static com.openexchange.java.Autoboxing.L;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableSet;
import com.openexchange.database.cleanup.CleanUpJob;
import com.openexchange.database.cleanup.CompositeCleanUpExecution;
import com.openexchange.database.cleanup.DefaultCleanUpJob;
import com.openexchange.database.tombstone.cleanup.cleaners.AbstractTombstoneTableCleaner;
import com.openexchange.database.tombstone.cleanup.cleaners.AttachmentTombstoneCleaner;
import com.openexchange.database.tombstone.cleanup.cleaners.CalendarTombstoneCleaner;
import com.openexchange.database.tombstone.cleanup.cleaners.ContactTombstoneCleaner;
import com.openexchange.database.tombstone.cleanup.cleaners.FolderTombstoneCleaner;
import com.openexchange.database.tombstone.cleanup.cleaners.GroupTombstoneCleaner;
import com.openexchange.database.tombstone.cleanup.cleaners.InfostoreTombstoneCleaner;
import com.openexchange.database.tombstone.cleanup.cleaners.ObjectPermissionTombstoneCleaner;
import com.openexchange.database.tombstone.cleanup.cleaners.ResourceTombstoneCleaner;
import com.openexchange.database.tombstone.cleanup.cleaners.TaskTombstoneCleaner;
import com.openexchange.java.Strings;

/**
 * {@link SchemaTombstoneCleaner}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.2
 */
public class SchemaTombstoneCleaner {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SchemaTombstoneCleaner.class);

    /**
     * Creates a set of tomb stone cleaners.
     *
     * @return The set of tomb stone cleaners
     */
    private static Set<AbstractTombstoneTableCleaner> createTombstoneTableCleaners() {
        return ImmutableSet.of( //@formatter:off
            new AttachmentTombstoneCleaner(),
            new CalendarTombstoneCleaner(),
            new ContactTombstoneCleaner(),
            new FolderTombstoneCleaner(),
            new GroupTombstoneCleaner(),
            new InfostoreTombstoneCleaner(),
            new ObjectPermissionTombstoneCleaner(),
            new ResourceTombstoneCleaner(),
            new TaskTombstoneCleaner());
        //@formatter:on
    }

    /**
     * Gets the available tomb stone cleaners and pre-sets the specified time span on them.
     *
     * @param timespan The tome span to set
     * @return The tomb stone cleaners pre-set with specified time span
     */
    private static Set<AbstractTombstoneTableCleaner> getTombstoneCleaner(long timespan) {
        Set<AbstractTombstoneTableCleaner> cleaners = createTombstoneTableCleaners();
        for (AbstractTombstoneTableCleaner cleaner : cleaners) {
            cleaner.setTimespan(timespan);
        }
        return cleaners;
    }

    /**
     * Creates the appropriate clean-up job applying given time span to tomb stone cleaners.
     *
     * @param timespan The time span to apply to tomb stone cleaners
     * @return The clean-up job
     */
    public static CleanUpJob getCleanUpJob(long timespan) {
        return DefaultCleanUpJob.builder(). //@formatter:off
            withId(SchemaTombstoneCleaner.class).
            withDelay(Duration.ofDays(1)).
            withInitialDelay(Duration.ofMinutes(60)).
            withRunsExclusive(true).
            withExecution(new CompositeCleanUpExecution(getTombstoneCleaner(timespan))).
            build();
        //@formatter:on
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Cleans up based on the given connection and time stamp. All entries older than the time stamp will be removed.
     *
     * @param writeConnection {@link Connection} to a schema.
     * @param timestamp long defining what will be removed.
     * @return {@link Map} containing the result of the cleanup in a 'table name' - 'number of removed rows' mapping
     * @see SchemaTombstoneCleaner#SchemaCleaner()
     */
    public Map<String, Integer> cleanup(Connection writeConnection, long timestamp) throws SQLException {
        Map<String, Integer> tableCleanupResults = new HashMap<>();

        for (AbstractTombstoneTableCleaner cleaner : getTombstoneCleaner()) {
            long before = System.currentTimeMillis();
            LOG.debug("Starting TombstoneCleaner '{}'", cleaner);
            Map<String, Integer> cleanedTables = cleaner.cleanup(writeConnection, timestamp);
            tableCleanupResults.putAll(cleanedTables);
            long after = System.currentTimeMillis();
            LOG.debug("Successfully finished TombstoneCleaner '{}' on schema '{}'. Processing took {}ms.", cleaner, writeConnection.getCatalog(), L(after - before));
        }
        return tableCleanupResults;
    }

    /**
     * Gets the tomb stone cleaners.
     *
     * @return The tomb stone cleaners
     */
    protected Set<AbstractTombstoneTableCleaner> getTombstoneCleaner() {
        return createTombstoneTableCleaners();
    }

    /**
     * Logs the result of the cleanup run
     *
     * @param lSchemaName The name of the schema that has been cleaned up.
     * @param cleanedTables The result of the cleanup that will be prepared to be logged.
     */
    public void logResults(String lSchemaName, Map<String, Integer> cleanedTables) {
        if (LOG.isDebugEnabled() == false) {
            return;
        }

        if (cleanedTables == null || cleanedTables.isEmpty()) {
            return;
        }
        Map<String, Integer> tablesHavingRowsRemoved = cleanedTables.entrySet().stream().filter(x -> Strings.isNotEmpty(x.getKey()) && x.getValue() != null && x.getValue().intValue() > 0).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (tablesHavingRowsRemoved == null || tablesHavingRowsRemoved.isEmpty()) {
            LOG.info("No rows have been identified to be removed for schema {}", lSchemaName);
            return;
        }

        if (LOG.isDebugEnabled()) {
            final StringBuilder logBuilder = new StringBuilder(1024);
            List<Object> args = new ArrayList<>(16);
            logBuilder.append("The following clean ups have been made on schema '{}'{}");
            String separator = Strings.getLineSeparator();
            args.add(lSchemaName);
            args.add(separator);

            for (Entry<String, Integer> tableResults : tablesHavingRowsRemoved.entrySet()) {
                if (tableResults != null && tableResults.getValue() != null) {
                    logBuilder.append("\tRemoved {} rows for table {}.{}");
                    args.add(tableResults.getValue());
                    args.add(tableResults.getKey());
                    args.add(separator);
                }
            }
            LOG.debug(logBuilder.toString(), args.toArray(new Object[args.size()]));
        }
    }

}
