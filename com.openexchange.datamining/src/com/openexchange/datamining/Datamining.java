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

package com.openexchange.datamining;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import com.mysql.cj.jdbc.JdbcConnection;
import com.openexchange.database.DatabaseConstants;
import com.openexchange.java.Streams;

/**
 * This is a simple Tool to get an idea how a specific installation of Open-Xchange is used. Operating on the MySQL-database exclusively it
 * is quite fast and uses few resources. Off-hours are still recommended for its usage to limit any performance-impact, though. It will find its required parameters automatically in the file
 * /opt/open-xchange/etc/configdb.properties. Otherwise it is possible to specify all parameters explicitly. Output is a single
 * text-file. The filename starts with "open-xchange_datamining" and includes the current date in YYYY-MM-DD format. The content of the file
 * is camelCased-Parameters, unique and one per line. This should make using these files as input, for example for a visualization, pretty
 * easy.
 *
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
@SuppressWarnings("deprecation")
public class Datamining {

    /**
     *
     */
    private static final String AVERAGE_FILESTORE_SIZE = "averageFilestoreSize";

    public static final String AVERAGE_NUMBER_OF_INFOSTORE_OBJECTS_PER_SCHEMA = "averageNumberOfInfostoreObjectsPerSchema";

    public static final String AVERAGE_NUMBER_OF_INFOSTORE_OBJECTS_PER_CONTEXT = "averageNumberOfInfostoreObjectsPerContext";

    public static final String NUMBER_OF_CONTEXTS = "numberOfContexts";

    public static final String NUMBER_OF_SCHEMATA = "numberOfSchemata";

    private static String configDBURL = "";

    private static String configDBUser = "";

    private static String configDBPassword = "";

    private static Properties jdbcProperties = null;

    private static boolean verbose = false;

    private static boolean helpPrinted = false;

    private static String reportfilePath = "";

    private static String filename = "";

    private static final AtomicBoolean noTimeout = new AtomicBoolean(true);

    private static final StringBuilder reportStringBuilder = new StringBuilder();

    public static final List<String> allTheQuestions = new ArrayList<String>();

    private static final HashMap<String, String> allTheAnswers = new HashMap<String, String>();

    private static List<Schema> allSchemata;

    private static Options staticOptions;

    public static void main(String[] args) {
        Calendar rightNow = Calendar.getInstance();
        final long before = rightNow.getTime().getTime();

        setReportFilename();
        readProperties();
        readParameters(args);

        if (configDBURL.equals("") || configDBUser.equals("") || configDBPassword.equals("")) {
            if (!helpPrinted) {
                printHelp();
            }
        } else if (!helpPrinted) {
            Connection configdbConnection = getDBConnection(configDBURL, configDBUser, configDBPassword);
            try {
                allSchemata = getAllSchemata(configdbConnection);
                report(NUMBER_OF_SCHEMATA, Integer.toString(allSchemata.size()));

                reportAverageFilestoreSize(configdbConnection);
                reportNumberOfContexts(configdbConnection);

                closeSafely(configdbConnection);
                configdbConnection = null;

                Questions.reportNumberOfUsers();
                Questions.reportNumberOfAppointments();
                Questions.reportNumberOfUsersWhoCreatedAppointments();
                Questions.reportMaximumNumberOfCreatedAppointmentsForOneUser();
                Questions.reportAverageNumberOfAppointmentsPerUserWhoHasAppointmentsAtAll();
                Questions.reportNumberOfUsersWithEventsInPrivateCalendarThatAreInTheFutureAndAreNotYearlySeries();
                Questions.reportNumberOfUsersWhoChangedTheirCalendarInTheLast30Days();
                Questions.reportNumberOfDocuments();
                Questions.reportNumberOfUsersWhoCreatedDocuments();
                Questions.reportMaximumNumberOfCreatedDocumentsForOneUser();
                Questions.reportAverageNumberOfDocumentsPerUserWhoHasDocumentsAtAll();
                reportAverageNumberOfInfostoreObjectsPerContext();
                reportAverageNumberOfInfostoreObjectsPerSchema();
                Questions.reportNumberOfNewInfostoreObjectsInTheLast30Days();
                Questions.reportNumberOfChangedInfostoreObjectsInTheLast30Days();
                Questions.reportNumberOfUsersWithNewInfostoreObjectsInTheLast30Days();
                Questions.reportSliceAndDiceOnDocumentSize();
                Questions.reportAverageDocumentSize();
                Questions.reportNumberOfContacts();
                Questions.reportNumberOfUserCreatedContacts();
                Questions.reportNumberOfUsersWhoHaveContacts();
                Questions.reportNumberOfUsersWhoCreatedContacts();
                Questions.reportMaximumNumberOfContactsForOneUser();
                Questions.reportMaximumNumberOfCreatedContactsForOneUser();
                Questions.reportAverageNumberOfContactsPerUserWhoHasContactsAtAll();
                Questions.reportAverageNumberOfContactsPerUserWhoHasCreatedContacts();
                Questions.reportNumberOfUsersWhoChangedTheirContactsInTheLast30Days();
                Questions.reportNumberOfUsersWithLinkedSocialNetworkingAccounts();
                Questions.reportNumberOfUsersConnectedToLinkedIn();
                Questions.reportNumberOfUsersConnectedToGoogle();
                Questions.reportNumberOfUsersConnectedToMSN();
                Questions.reportNumberOfUsersConnectedToYahoo();
                Questions.reportNumberOfUsersConnectedToXing();
                Questions.reportNumberOfUsersConnectedToTOnline();
                Questions.reportNumberOfUsersConnectedToGMX();
                Questions.reportNumberOfUsersConnectedToWebDe();
                Questions.reportNumberOfTasks();
                Questions.reportNumberOfUsersWhoCreatedTasks();
                Questions.reportMaximumNumberOfCreatedTasksForOneUser();
                Questions.reportAverageNumberOfTasksPerUserWhoHasTasksAtAll();
                Questions.reportNumberOfUsersWhoChangedTheirTasksInTheLast30Days();
                Questions.reportNumberOfUsersWhoSelectedTeamViewAsCalendarDefault();
                Questions.reportNumberOfUsersWhoSelectedCalendarViewAsCalendarDefault();
                Questions.reportNumberOfUsersWhoSelectedListViewAsCalendarDefault();
                Questions.reportNumberOfUsersWhoSelectedCardsViewAsContactsDefault();
                Questions.reportNumberOfUsersWhoSelectedListViewAsContactsDefault();
                Questions.reportNumberOfUsersWhoSelectedListViewAsTasksDefault();
                Questions.reportNumberOfUsersWhoSelectedHSplitViewAsTasksDefault();
                Questions.reportNumberOfUsersWhoSelectedListViewAsInfostoreDefault();
                Questions.reportNumberOfUsersWhoSelectedHSplitViewAsInfostoreDefault();
                Questions.reportNumberOfUsersWhoActivatedMiniCalendar();
                Questions.reportNumberOfUsersWhoLoggedInWithClientOX6UIInTheLast30Days();
                Questions.reportNumberOfUsersWhoLoggedInWithClientAppSuiteUIInTheLast30Days();
                Questions.reportNumberOfUsersWhoLoggedInWithClientMobileUIInTheLast30Days();
                Questions.reportNumberOfUsersWhoLoggedInWithClientEASInTheLast30Days();
                Questions.reportNumberOfUsersWhoLoggedInWithClientCalDAVInTheLast30Days();
                Questions.reportNumberOfUsersWhoLoggedInWithClientCardDAVInTheLast30Days();
                Questions.reportSliceAndDiceOnDraftMailSize();
                Questions.reportSliceAndDiceOnExternalAccountUsage();

                rightNow = Calendar.getInstance();
                final long after = rightNow.getTime().getTime();
                report("durationInSeconds", Long.toString((after - before) / 1000));

                sanityCheck();

                printReport();
            } finally {
                closeSafely(configdbConnection);
            }
        }
    }

    private static void closeSafely(Connection configdbConnection) {
        if (configdbConnection != null) {
            try {
                configdbConnection.close();
            } catch (SQLException e) {
                System.out.println("Error : " + e.getMessage());
            }
        }
    }

    private static void cleanupSql(final Object... clobj) {
        try {
            if ( null != clobj ) {
                for(final Object obj : clobj) {
                    if ( null != obj ) {
                        if ( obj instanceof Statement ) {
                            ((Statement)obj).close();
                        }
                        if ( obj instanceof ResultSet) {
                            ((ResultSet)obj).close();
                        }
                        if (obj instanceof JdbcConnection) {
                            ((JdbcConnection) obj).close();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void sanityCheck() {
        ArrayList<String> openQuestions = new ArrayList<String>();
        for (String question : allTheQuestions) {
            if (!allTheAnswers.containsKey(question)) {
                openQuestions.add(question);
            }
        }
        if (openQuestions.size() == 0) {
            report("sanityCheck", "ok");
        } else {
            report("sanityCheck", "problems");
            report("questionsNotAnswered", openQuestions.toString());
        }
    }

    private static void printHelp() {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(120);
            formatter.printHelp("datamining", staticOptions);
            System.out.println("\nEither specify these parameters manually or use this tool on an Open-Xchange application server where all necessary info\n is automatically found in the file /opt/open-xchange/etc/configdb.properties");
            System.out.println("Even then it is possible to override parameters from configdb.properties by setting them manually.");
            System.out.println("Please note that if you want to override hostname / dbName / dbPort at least hostname must be given (no default) and defaults will apply for the other 2.");

            helpPrinted = true;
    }

    private static void reportNumberOfContexts(Connection configdbConnection) {
        allTheQuestions.add(NUMBER_OF_CONTEXTS);
        if (configdbConnection != null) {
            Statement query = null;
            ResultSet result = null;
            try {
                query = configdbConnection.createStatement();

                String sql = "SELECT count(*) FROM context";
                result = query.executeQuery(sql);

                while (result.next()) {
                    String count = result.getString(1);
                    report(NUMBER_OF_CONTEXTS, count);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cleanupSql(query, result);
            }
        }
    }

    public static String getOneAnswer(String parameter) {
        return allTheAnswers.get(parameter);
    }

    @SuppressWarnings("static-access")
    private static void readParameters(String[] args) {

    	boolean mandatoryOptionsSet = !configDBURL.equals("") && !configDBUser.equals("") && !configDBPassword.equals("");

    	String hostname, dbPort, dbName;

    	Options options = new Options();

    	options.addOption(OptionBuilder.withLongOpt("hostname").isRequired(!mandatoryOptionsSet).hasArg().withDescription("Host where the Open-Xchange MySQL-database is running").create("n"));
    	options.addOption(OptionBuilder.withLongOpt("dbUser").isRequired(!mandatoryOptionsSet).hasArg().withDescription("Name of the MySQL-User for configdb").create("u"));
    	options.addOption(OptionBuilder.withLongOpt("dbPassword").isRequired(!mandatoryOptionsSet).hasArg().withDescription("Password for the user specified with \"-dbUser\"").create("p"));
    	options.addOption(OptionBuilder.withLongOpt("dbName").hasArg().withDescription("Name of the MySQL-database that contains the Open-Xchange configDB (default: \"configdb\")").create("d"));
    	options.addOption(OptionBuilder.withLongOpt("reportfilePath").hasArg().withDescription("Path where the report-file is saved (default: \"./\")").create("r"));
    	options.addOption(OptionBuilder.withLongOpt("verbose").withDescription("Print the results to the console as well as into the report file").create("v"));
    	options.addOption(OptionBuilder.withLongOpt("dbPort").hasArg().withDescription("Port where MySQL is running on the host specified with \"-hostname\" (default: \"3306\")").create("o"));
    	options.addOption(OptionBuilder.withLongOpt("help").withDescription("Print this helpfile").create("h"));
    	options.addOption(OptionBuilder.hasArg(false).withLongOpt("timeout").withDescription("If this flag is set connections to database are supposed to be established with timeout settings. If flag is absent connections are established without timeout settings.").create("t"));

    	staticOptions = options;

    	CommandLineParser parser = new PosixParser();

    	try {
			CommandLine cl = parser.parse(options, args);

			if (cl.hasOption("hostname")){
				hostname = cl.getOptionValue("hostname");
				dbName = (cl.hasOption("dbName")) ? cl.getOptionValue("dbName") : "configdb";
				dbPort = (cl.hasOption("dbPort")) ? cl.getOptionValue("dbPort") : "3306";

				configDBURL = "jdbc:mysql://" + hostname + ":" + dbPort + "/" + dbName;
			}

			if (cl.hasOption("dbUser")){
				configDBUser = cl.getOptionValue("dbUser");
			}

			if (cl.hasOption("dbPassword")){
				configDBPassword = cl.getOptionValue("dbPassword");
			}

			if (cl.hasOption("reportfilePath")){
				reportfilePath = cl.getOptionValue("reportfilePath");
			}

			if (cl.hasOption("verbose")){
				verbose = true;
			}

			if (cl.hasOption("timeout")){
			    noTimeout.set(false);
			}

			if (cl.hasOption("help")){
				printHelp();
			}


		} catch (ParseException e) {
			System.out.println(e.getMessage());
		}

    }

    private static void readProperties() {
        // Try to read configdb.properties
        FileInputStream in = null;
        try {
            Properties configdbProperties = new Properties();
            in = new FileInputStream("/opt/open-xchange/etc/configdb.properties");
            configdbProperties.load(in);
            in.close();
            in = null;

            configDBURL = configdbProperties.getProperty("readUrl");
            configDBUser = configdbProperties.getProperty("readProperty.1").substring(5);
            configDBPassword = configdbProperties.getProperty("readProperty.2").substring(9);

            if (configDBURL != null && configDBUser != null && configDBPassword != null) {
                System.out.println("All necessary parameters found in '/opt/open-xchange/etc/configdb.properties' file");
            }
        } catch (FileNotFoundException e) {
            System.out.println("File '/opt/open-xchange/etc/configdb.properties' is not available");
            System.out.println("Aborting...");
            System.exit(1);
        } catch (IOException e) {
            System.out.println("File '/opt/open-xchange/etc/configdb.properties' is not readable: " + e.getMessage());
            e.printStackTrace(System.out);
            System.out.println("Aborting...");
            System.exit(1);
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        // Try to read dbconnector.yaml
        in = null;
        try {
            // Whether to consider timeout settings
            boolean applyTimeout = noTimeout.get() == false;

            // Set defaults:
            Properties jdbcProps = new Properties();
            jdbcProps.setProperty("useUnicode", "true");
            jdbcProps.setProperty("characterEncoding", "UTF-8");
            jdbcProps.setProperty("autoReconnect", "false");
            jdbcProps.setProperty("useServerPrepStmts", "false");
            jdbcProps.setProperty("useTimezone", "true");
            jdbcProps.setProperty("serverTimezone", "UTC");
            if (applyTimeout) {
                jdbcProps.setProperty("connectTimeout", "15000");
                jdbcProps.setProperty("socketTimeout", "15000");
            }
            jdbcProps.setProperty("useSSL", "false");
            jdbcProps.setProperty("requireSSL", "false");
            jdbcProps.setProperty("verifyServerCertificate", "false");
            jdbcProps.setProperty("enabledTLSProtocols", "TLSv1,TLSv1.1,TLSv1.2");
            jdbcProps.setProperty("nullCatalogMeansCurrent", "true");

            in = new FileInputStream("/opt/open-xchange/etc/dbconnector.yaml");
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object yamlObj = yaml.load(in);
            in.close();
            in = null;

            if ((yamlObj instanceof Map)) {
                Object obj = ((Map<String, Object>) yamlObj).get("com.mysql.jdbc");
                if ((obj instanceof Map)) {
                    @SuppressWarnings("unchecked")
                    Stream<Entry<String, Object>> jdbcPropsStream = ((Map<String, Object>) obj).entrySet().stream();
                    if (applyTimeout == false) {
                        // Drop timeout-related JDBC properties
                        jdbcPropsStream = jdbcPropsStream.filter(jdbcPropEntry -> isNoTimeoutSetting(jdbcPropEntry));
                    }
                    Map<String, String> args = jdbcPropsStream.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
                    jdbcProps.putAll(args);
                    System.out.println("JDBC properties successfully read from '/opt/open-xchange/etc/dbconnector.yaml' file");
                }
            }

            jdbcProperties = jdbcProps;
        } catch (FileNotFoundException e) {
            System.out.println("File '/opt/open-xchange/etc/dbconnector.yaml' is not available");
            System.out.println("Aborting...");
            System.exit(1);
        } catch (IOException e) {
            System.out.println("File '/opt/open-xchange/etc/dbconnector.yaml' is not readable: " + e.getMessage());
            e.printStackTrace(System.out);
            System.out.println("Aborting...");
            System.exit(1);
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Checks if given JDBC property entry is neither <code>"connectTimeout"</code> nor <code>"socketTimeout"</code>.
     *
     * @param jdbcPropEntry The JDBC property entry to check
     * @return <code>true</code> if neither <code>"connectTimeout"</code> nor <code>"socketTimeout"</code>; otherwise false
     */
    private static boolean isNoTimeoutSetting(Map.Entry<String, Object> jdbcPropEntry) {
        String propertyName = jdbcPropEntry.getKey();
        return ("connectTimeout".equals(propertyName) || "socketTimeout".equals(propertyName)) == false;
    }

    protected static void report(String parameter, String value) {
        String combined = new StringBuilder(parameter).append('=').append(value).toString();
        allTheAnswers.put(parameter, value);
        reportStringBuilder.append(combined + '\n');
        if (verbose) {
            System.out.println(combined);
        }
    }

    private static void printReport() {
        FileOutputStream fos = null;
        OutputStreamWriter out = null;
        try {
            fos = new FileOutputStream(reportfilePath + filename);
            out = new OutputStreamWriter(fos, "UTF-8");
            out.write(reportStringBuilder.toString());
            out.flush();
            System.out.println("report written to this file : " + reportfilePath + filename);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Streams.close(out, fos);
        }
    }

    protected static JdbcConnection getDBConnection(String url, String user, String password) {
        try {

            // load mysql driver
            Class.forName(DatabaseConstants.DRIVER);
            DriverManager.setLoginTimeout(5);

            // connect
            String urlToUse = url;
            Properties defaults = jdbcProperties;
            if (null == defaults) {
                defaults = new Properties();
                defaults.setProperty("useSSL", "false");
            } else {
                urlToUse = removeParametersFromJdbcUrl(urlToUse);
            }
            defaults.put("user", user);
            defaults.put("password", password);

            Connection conn = DriverManager.getConnection(urlToUse, defaults);
            return (JdbcConnection) conn;
        } catch (ClassNotFoundException e) {
            System.out.println("Error : JDBC driver not found");
        } catch (SQLException e) {
            System.out.println("Error : No SQL-connection possible to this URL: " + url + " with this user and password : (" + user + " / " + password + ")");
            System.out.println(e.getMessage());
            System.exit(1);
        }
        return null;
    }

    /**
     * Removes possible parameters appended to specified JDBC URL and returns it.
     *
     * @param url The URL to remove possible parameters from
     * @return The parameter-less JDBC URL
     */
    private static String removeParametersFromJdbcUrl(String url) {
        if (null == url) {
            return url;
        }

        int paramStart = url.indexOf('?');
        return paramStart >= 0 ? url.substring(0, paramStart) : url;
    }

    private static void setReportFilename() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            Calendar cal = Calendar.getInstance();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            filename = "open-xchange_datamining_" + addr.getHostAddress() + "_" + dateFormat.format(cal.getTime()) + ".txt";
            report("hostIPAddress", addr.getHostAddress());
            report("hostname", addr.getHostName());
            report("dateOfReport", dateFormat.format(cal.getTime()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static List<Schema> getAllSchemata(Connection configdbConnection) {
        allTheQuestions.add(NUMBER_OF_SCHEMATA);
        try {
            if (configdbConnection != null) {
                report("configdbUrl", configDBURL);
                Statement query = null;
                ResultSet result = null;
                try {
                    query = configdbConnection.createStatement();

                    String sql = "SELECT DISTINCT csp.db_schema, csp.read_db_pool_id, dp.url, dp.login, dp.password FROM context_server2db_pool csp, db_pool dp WHERE csp.read_db_pool_id = dp.db_pool_id;";
                    result = query.executeQuery(sql);

                    if (result.next() == false) {
                        return Collections.emptyList();
                    }

                    List<Schema> schemata = new ArrayList<Schema>();
                    do {
                        Schema schema = new Schema(
                            result.getString("db_schema"),
                            result.getString("read_db_pool_id"),
                            result.getString("url"),
                            result.getString("login"),
                            result.getString("password"));
                        schemata.add(schema);
                    } while (result.next());
                    return schemata;
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    cleanupSql(query, result);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private static List<String> reportAverageFilestoreSize(Connection configdbConnection) {
        allTheQuestions.add(AVERAGE_FILESTORE_SIZE);
        try {
            if (configdbConnection != null) {
                Statement query = null;
                ResultSet result = null;
                try {
                    query = configdbConnection.createStatement();

                    String sql = "SELECT ROUND(AVG(size)) FROM filestore";
                    result = query.executeQuery(sql);

                    if (result.next() == false) {
                        return Collections.emptyList();
                    }

                    List<String> schemata = new ArrayList<String>();
                    do {
                        String size = result.getString(1);
                        report(AVERAGE_FILESTORE_SIZE, result.wasNull() ? "0" : size);
                    } while (result.next());
                    return schemata;
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    cleanupSql(query, result);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private static void reportAverageNumberOfInfostoreObjectsPerContext() {
        allTheQuestions.add(AVERAGE_NUMBER_OF_INFOSTORE_OBJECTS_PER_CONTEXT);
        float numberInAllSchemata = 0.0F;
        try {
            for (Schema schema : allSchemata) {
                String url = generateDbUrlFrom(schema);
                JdbcConnection conn = getDBConnection(url, schema.getLogin(), schema.getPassword());
                if (conn != null) {
                    Statement query = null;
                    ResultSet result = null;
                    try {
                        query = conn.createStatement();
                        // GROUP BY CLAUSE: ensure ONLY_FULL_GROUP_BY compatibility
                        String sql = "SELECT AVG(number_per_context) FROM (SELECT COUNT(id) AS number_per_context FROM infostore GROUP BY cid) AS T";
                        result = query.executeQuery(sql);

                        while (result.next()) {
                            String numberInOneSchema = result.getString(1);
                            numberInAllSchemata += (result.wasNull() ? 0.0F : Float.parseFloat(numberInOneSchema));
                        }
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        cleanupSql(query, result, conn);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        report(AVERAGE_NUMBER_OF_INFOSTORE_OBJECTS_PER_CONTEXT, Float.toString(numberInAllSchemata / allSchemata.size()));
    }

    private static void reportAverageNumberOfInfostoreObjectsPerSchema() {
        allTheQuestions.add(AVERAGE_NUMBER_OF_INFOSTORE_OBJECTS_PER_SCHEMA);
        float numberInAllSchemata = 0.0F;
        try {
            for (Schema schema : allSchemata) {
                String url = generateDbUrlFrom(schema);
                JdbcConnection conn = getDBConnection(url, schema.getLogin(), schema.getPassword());
                if (conn != null) {
                    Statement query = null;
                    ResultSet result = null;
                    try {
                        query = conn.createStatement();

                        String sql = "SELECT COUNT(*) FROM infostore";
                        result = query.executeQuery(sql);

                        while (result.next()) {
                            String numberInOneSchema = result.getString(1);
                            numberInAllSchemata += (result.wasNull() ? 0.0F : Float.parseFloat(numberInOneSchema));
                        }
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        cleanupSql(query, result, conn);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        report(AVERAGE_NUMBER_OF_INFOSTORE_OBJECTS_PER_SCHEMA, Float.toString(numberInAllSchemata / allSchemata.size()));
    }

    protected static BigInteger countOverAllSchemata(String sql) {
        BigInteger numberOfObjects = BigInteger.valueOf(0);
        for (Schema schema : allSchemata) {
            String url = generateDbUrlFrom(schema);
            JdbcConnection conn = Datamining.getDBConnection(url, schema.getLogin(), schema.getPassword());
            if (conn != null) {
                Statement query = null;
                ResultSet result = null;
                try {
                    query = conn.createStatement();
                    result = query.executeQuery(sql);
                    while (result.next()) {
                        String numberInOneSchema = result.getString(1);
                        numberOfObjects = numberOfObjects.add(result.wasNull() ? BigInteger.valueOf(0) : new BigInteger(numberInOneSchema));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    cleanupSql(query, result, conn);
                }
            }
        }
        return numberOfObjects;
    }

    protected static LinkedHashMap<Integer, Integer> draftMailOverAllSchemata(final int ranges[]) {
        // we want to keep the order of ranges
        final LinkedHashMap<Integer, Integer> rm = new LinkedHashMap<>(ranges.length);
        for(int r : ranges) {
            rm.put(Integer.valueOf(r), Integer.valueOf(0));
        }
        rm.put(Integer.valueOf(Integer.MAX_VALUE), Integer.valueOf(0));
        for (Schema schema : allSchemata) {
            String url = generateDbUrlFrom(schema);
            JdbcConnection conn = Datamining.getDBConnection(url, schema.getLogin(), schema.getPassword());
            if (conn != null) {
                java.sql.PreparedStatement prep = null;
                ResultSet result = null;
                try {
                    prep = conn.prepareStatement("SELECT data FROM jsonStorage WHERE id=?");
                    prep.setString(1, "io.ox/core");
                    result = prep.executeQuery();
                    while (result.next()) {
                        // FIXME: maybe sufficient to just NOT check for savepoints and sum up just all data within jsonStorage with id=io.ox/core ?
                        InputStream is = result.getBlob(1).getBinaryStream();
                        JSONObject job = new JSONObject(is);
                        if ( job.has("savepoints") ) {
                            JSONArray sp = (JSONArray) job.get("savepoints");
                            if ( null != sp ) {
                                int spsize = sp.toString().getBytes().length;
                                int ll = 0;
                                for (Map.Entry<Integer, Integer> entry : rm.entrySet()) {
                                    Integer rtb = entry.getKey();
                                    if (spsize > ll && spsize <= rtb.intValue()) {
                                        rm.put(rtb, Integer.valueOf(entry.getValue().intValue() + 1));
                                    }
                                    ll = rtb.intValue();
                                }
                            }
                        }
                        is.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    cleanupSql(prep, result, conn);
                }
            }
        }
        return rm;
    }

    protected static HashMap<Integer, Integer> externalAccountsOverAllSchemata(int max) {
        final HashMap<Integer, Integer> ret = new HashMap<>();
        for (Schema schema : allSchemata) {
            String url = generateDbUrlFrom(schema);
            JdbcConnection conn = Datamining.getDBConnection(url, schema.getLogin(), schema.getPassword());
            if (conn != null) {
                java.sql.PreparedStatement prep = null;
                ResultSet result = null;
                try {
                    // GROUP BY CLAUSE: ensure ONLY_FULL_GROUP_BY compatibility
                    prep = conn.prepareStatement("SELECT COUNT(id) FROM user_mail_account WHERE id != 0 GROUP BY cid,user");
                    result = prep.executeQuery();
                    while (result.next()) {
                        int ecnt = result.getInt(1);
                        ecnt = ecnt > max ? max : ecnt;
                        Integer ecntI = Integer.valueOf(ecnt);
                        Integer count = ret.get(ecntI);
                        if (null != count) {
                            ret.put(ecntI, Integer.valueOf(ret.get(ecntI).intValue() + 1));
                        } else {
                            ret.put(ecntI, Integer.valueOf(1));
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    cleanupSql(prep, result, conn);
                }
            }
        }
        return ret;
    }

    protected static BigInteger maximumForAllSchemata(String sql) {
        BigInteger numberOfObjects = BigInteger.valueOf(0L);
        for (Schema schema : allSchemata) {
            String url = generateDbUrlFrom(schema);
            JdbcConnection conn = Datamining.getDBConnection(url, schema.getLogin(), schema.getPassword());
            if (conn != null) {
                Statement query = null;
                ResultSet result = null;
                try {
                    query = conn.createStatement();
                    result = query.executeQuery(sql);
                    while (result.next()) {
                        String numberInOneSchema = result.getString(1);
                        BigInteger numberInThisSchema = result.wasNull() ? BigInteger.valueOf(0L) : new BigInteger(numberInOneSchema);
                        if (numberInThisSchema.compareTo(numberOfObjects) == 1) {
                            numberOfObjects = numberInThisSchema;
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    cleanupSql(query, result, conn);
                }
            }
        }
        return numberOfObjects;
    }

    protected static Float averageForAllSchemata(String sql) {
        float numberOfObjects = 0.0F;
        for (Schema schema : allSchemata) {
            String url = generateDbUrlFrom(schema);
            JdbcConnection conn = Datamining.getDBConnection(url, schema.getLogin(), schema.getPassword());
            if (conn != null) {
                Statement query = null;
                ResultSet result = null;
                try {
                    query = conn.createStatement();
                    result = query.executeQuery(sql);
                    while (result.next()) {
                        String numberInOneSchema = result.getString(1);
                        float numberInThisSchema = result.wasNull() ? 0.0F : Float.parseFloat(numberInOneSchema);
                        numberOfObjects = numberOfObjects + numberInThisSchema;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    cleanupSql(query, result, conn);
                }
            }
        }
        return Float.valueOf(numberOfObjects / allSchemata.size());
    }

    private static String generateDbUrlFrom(Schema schema) {
        String url = schema.getUrl();

        int pos = url.indexOf("://");
        String host, scheme;
        if (pos < 0) {
            host = url;
            scheme =  "";
        } else {
            pos = pos + 3;
            host = url.substring(pos);
            scheme = url.substring(0, pos);
        }

        pos = host.lastIndexOf('/');
        host = (pos < 0 ? host : host.substring(0, pos)) + '/' + schema.getSchemaname();

        return scheme + host;
    }

    public static String getFilename(){
        return filename;
    }
}
