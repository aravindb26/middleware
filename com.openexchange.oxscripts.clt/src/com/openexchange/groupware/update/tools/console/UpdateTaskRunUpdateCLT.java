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

package com.openexchange.groupware.update.tools.console;

import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import com.openexchange.auth.Credentials;
import com.openexchange.groupware.update.UpdateTaskRMIService;
import com.openexchange.tools.console.TableWriter.ColumnFormat;
import com.openexchange.tools.console.TableWriter.ColumnFormat.Align;

/**
 * {@link UpdateTaskRunUpdateCLT} - Command-Line access to run update process for a certain schema via update task toolkit.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class UpdateTaskRunUpdateCLT extends AbstractUpdateTasksCLT<Void> {

    /**
     * Entry point
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new UpdateTaskRunUpdateCLT().execute(args);
    }

    private static final ColumnFormat[] FORMATS = { new ColumnFormat(Align.LEFT), new ColumnFormat(Align.LEFT), new ColumnFormat(Align.LEFT) };

    private static final String[] COLUMNS = { "taskName", "className", "schema" };

    private String schemaName;
    private int contextId;

    /**
     * Initializes a new {@link UpdateTaskRunUpdateCLT}.
     */
    private UpdateTaskRunUpdateCLT() {
        super("runupdate [-c <contextId> | -n <schemaName>] " + BASIC_MASTER_ADMIN_USAGE, "Runs the schema's update.");
    }

    @Override
    protected void addOptions(Options options) {
        options.addOption("c", "context", true, "A valid context identifier contained in target schema");
        options.addOption("n", "name", true, "A valid schema name. This option is a substitute for '-c/--context' option. If both are present '-c/--context' is preferred.");
    }

    @Override
    protected Void invoke(Options options, CommandLine cmd, String optRmiHostName) throws Exception {
        UpdateTaskRMIService updateTaskService = getRmiStub(UpdateTaskRMIService.RMI_NAME);
        String adminUser = cmd.getOptionValue('A');
        String adminPassword = cmd.getOptionValue('P');

        List<Map<String, Object>> failures = schemaName == null ? updateTaskService.runUpdate(contextId, new Credentials(adminUser, adminPassword)) : updateTaskService.runUpdate(schemaName, new Credentials(adminUser, adminPassword));
        if (failures == null || failures.isEmpty()) {
            return null;
        }
        System.out.println("The following update task(s) failed:");
        writeCompositeList(failures, COLUMNS, FORMATS);
        return null;
    }

    @Override
    protected void checkOptions(CommandLine cmd) {
        if (!cmd.hasOption('c')) {
            if (!cmd.hasOption('n')) {
                System.err.println("Missing context/schema identifier.");
                printHelp();
                System.exit(1);
            }
            schemaName = cmd.getOptionValue('n');
        } else {
            String optionValue = cmd.getOptionValue('c');
            try {
                contextId = Integer.parseInt(optionValue.trim());
            } catch (NumberFormatException e) {
                System.err.println("Context parameter is not a number: " + optionValue);
                printHelp();
                System.exit(1);
            }
        }
    }
}
