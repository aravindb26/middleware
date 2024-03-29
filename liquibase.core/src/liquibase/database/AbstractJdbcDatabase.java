package liquibase.database;

import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import liquibase.CatalogAndSchema;
import liquibase.change.Change;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RanChangeSet;
import liquibase.changelog.filter.ContextChangeSetFilter;
import liquibase.changelog.filter.DbmsChangeSetFilter;
import liquibase.database.core.OracleDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.core.SQLiteDatabase;
import liquibase.database.core.SybaseASADatabase;
import liquibase.database.core.SybaseDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.compare.DatabaseObjectComparatorFactory;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.DatabaseException;
import liquibase.exception.DatabaseHistoryException;
import liquibase.exception.DateParseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.RollbackImpossibleException;
import liquibase.exception.StatementNotSupportedOnDatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.logging.LogFactory;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.EmptyDatabaseSnapshot;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.sql.Sql;
import liquibase.sql.visitor.SqlVisitor;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.DatabaseFunction;
import liquibase.statement.SequenceCurrentValueFunction;
import liquibase.statement.SequenceNextValueFunction;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.AddColumnStatement;
import liquibase.statement.core.CreateDatabaseChangeLogLockTableStatement;
import liquibase.statement.core.CreateDatabaseChangeLogTableStatement;
import liquibase.statement.core.DropTableStatement;
import liquibase.statement.core.GetNextChangeSetSequenceValueStatement;
import liquibase.statement.core.GetViewDefinitionStatement;
import liquibase.statement.core.InitializeDatabaseChangeLogLockTableStatement;
import liquibase.statement.core.MarkChangeSetRanStatement;
import liquibase.statement.core.ModifyDataTypeStatement;
import liquibase.statement.core.RawSqlStatement;
import liquibase.statement.core.RemoveChangeSetRanStatusStatement;
import liquibase.statement.core.SelectFromDatabaseChangeLogStatement;
import liquibase.statement.core.SetNullableStatement;
import liquibase.statement.core.TagDatabaseStatement;
import liquibase.statement.core.UpdateChangeSetChecksumStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Catalog;
import liquibase.structure.core.Column;
import liquibase.structure.core.ForeignKey;
import liquibase.structure.core.Index;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Sequence;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;
import liquibase.structure.core.View;
import liquibase.util.ISODateFormat;
import liquibase.util.StreamUtil;
import liquibase.util.StringUtils;


/**
 * AbstractJdbcDatabase is extended by all supported databases as a facade to the underlying database.
 * The physical connection can be retrieved from the AbstractJdbcDatabase implementation, as well as any
 * database-specific characteristics such as the datatype for "boolean" fields.
 */
public abstract class AbstractJdbcDatabase implements Database {

    private static final Pattern startsWithNumberPattern = Pattern.compile("^[0-9].*");

    private DatabaseConnection connection;
    protected String defaultCatalogName;
    protected String defaultSchemaName;

    protected String currentDateTimeFunction;

    /**
     * The sequence name will be substituted into the string e.g. NEXTVAL('%s')
     */
    protected String sequenceNextValueFunction;
    protected String sequenceCurrentValueFunction;
    protected String quotingStartCharacter = "\"";
    protected String quotingEndCharacter = "\"";

    // List of Database native functions.
    protected List<DatabaseFunction> dateFunctions = new ArrayList<DatabaseFunction>();

    protected List<String> unmodifiableDataTypes = new ArrayList<String>();

    private List<RanChangeSet> ranChangeSetList;

    protected void resetRanChangeSetList() {
        ranChangeSetList = null;
    }

    private static Pattern CREATE_VIEW_AS_PATTERN = Pattern.compile("^CREATE\\s+.*?VIEW\\s+.*?AS\\s+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private String databaseChangeLogTableName = System.getProperty("liquibase.databaseChangeLogTableName") == null ? "DatabaseChangeLog".toUpperCase() : System.getProperty("liquibase.databaseChangeLogTableName");
    private String databaseChangeLogLockTableName = System.getProperty("liquibase.databaseChangeLogLockTableName") == null ? "DatabaseChangeLogLock".toUpperCase() : System.getProperty("liquibase.databaseChangeLogLockTableName");
    private String liquibaseTablespaceName = System.getProperty("liquibase.tablespaceName");
    private String liquibaseSchemaName = System.getProperty("liquibase.schemaName");
    private String liquibaseCatalogName = System.getProperty("liquibase.catalogName");

    private Integer lastChangeSetSequenceValue;
    private Boolean previousAutoCommit;

    private boolean canCacheLiquibaseTableInfo = false;
    private boolean hasDatabaseChangeLogTable = false;
    private boolean hasDatabaseChangeLogLockTable = false;
    private boolean isDatabaseChangeLogLockTableInitialized = false;

    protected BigInteger defaultAutoIncrementStartWith = BigInteger.ONE;
    protected BigInteger defaultAutoIncrementBy = BigInteger.ONE;
    // most databases either lowercase or uppercase unuqoted objects such as table and column names.
    protected Boolean unquotedObjectsAreUppercased = null;
    // whether object names should be quoted
    protected ObjectQuotingStrategy quotingStrategy = ObjectQuotingStrategy.LEGACY;

    private Boolean caseSensitive;
    private boolean outputDefaultSchema = true;
    private boolean outputDefaultCatalog = true;

    public String getName() {
        return toString();
    }

    @Override
    public boolean requiresPassword() {
        return true;
    }

    @Override
    public boolean requiresUsername() {
        return true;
    }

    public DatabaseObject[] getContainingObjects() {
        return null;
    }

    // ------- DATABASE INFORMATION METHODS ---- //

    @Override
    public DatabaseConnection getConnection() {
        return connection;
    }

    @Override
    public void setConnection(DatabaseConnection conn) {
        LogFactory.getLogger().debug("Connected to " + conn.getConnectionUserName() + "@" + conn.getURL());
        this.connection = conn;
        try {
            boolean autoCommit = conn.getAutoCommit();
            if (autoCommit == getAutoCommitMode()) {
                // Don't adjust the auto-commit mode if it's already what the database wants it to be.
                LogFactory.getLogger().debug("Not adjusting the auto commit mode; it is already " + autoCommit);
            } else {
                // Store the previous auto-commit mode, because the connection needs to be restored to it when this
                // AbstractDatabase type is closed. This is important for systems which use connection pools.
                previousAutoCommit = autoCommit ? Boolean.TRUE : Boolean.FALSE;

                LogFactory.getLogger().debug("Setting auto commit to " + getAutoCommitMode() + " from " + autoCommit);
                connection.setAutoCommit(getAutoCommitMode());
            }
        } catch (DatabaseException e) {
            LogFactory.getLogger().warning("Cannot set auto commit to " + getAutoCommitMode() + " on connection");
        }
    }

    /**
     * Auto-commit mode to run in
     */
    @Override
    public boolean getAutoCommitMode() {
        return !supportsDDLInTransaction();
    }

    /**
     * By default databases should support DDL within a transaction.
     */
    @Override
    public boolean supportsDDLInTransaction() {
        return true;
    }

    /**
     * Returns the name of the database product according to the underlying database.
     */
    @Override
    public String getDatabaseProductName() {
        if (connection == null) {
            return getDefaultDatabaseProductName();
        }

        try {
            return connection.getDatabaseProductName();
        } catch (DatabaseException e) {
            throw new RuntimeException("Cannot get database name");
        }
    }

    protected abstract String getDefaultDatabaseProductName();


    @Override
    public String getDatabaseProductVersion() throws DatabaseException {
        if (connection == null) {
            return null;
        }

        try {
            return connection.getDatabaseProductVersion();
        } catch (DatabaseException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int getDatabaseMajorVersion() throws DatabaseException {
        if (connection == null) {
            return -1;
        }
        try {
            return connection.getDatabaseMajorVersion();
        } catch (DatabaseException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int getDatabaseMinorVersion() throws DatabaseException {
        if (connection == null) {
            return -1;
        }
        try {
            return connection.getDatabaseMinorVersion();
        } catch (DatabaseException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public String getDefaultCatalogName() {
        if (defaultCatalogName == null) {
            if (defaultSchemaName != null && !this.supportsSchemas()) {
                return defaultSchemaName;
            }

            if (connection != null) {
                try {
                    defaultCatalogName = getConnectionCatalogName();
                } catch (DatabaseException e) {
                    LogFactory.getLogger().info("Error getting default catalog", e);
                }
            }
        }
        return defaultCatalogName;
    }

    protected String getConnectionCatalogName() throws DatabaseException {
        return connection.getCatalog();
    }

    public CatalogAndSchema correctSchema(String catalog, String schema) {
        return correctSchema(new CatalogAndSchema(catalog, schema));
    }

    @Override
    public CatalogAndSchema correctSchema(CatalogAndSchema schema) {
        if (schema == null) {
            return new CatalogAndSchema(getDefaultCatalogName(), getDefaultSchemaName());
        }
        String catalogName = StringUtils.trimToNull(schema.getCatalogName());
        String schemaName = StringUtils.trimToNull(schema.getSchemaName());

        if (supportsCatalogs() && supportsSchemas()) {
            if (catalogName == null) {
                catalogName = getDefaultCatalogName();
            } else {
                catalogName = correctObjectName(catalogName, Catalog.class);
            }

            if (schemaName == null) {
                schemaName = getDefaultSchemaName();
            } else {
                schemaName = correctObjectName(schemaName, Schema.class);
            }
        } else if (!supportsCatalogs() && !supportsSchemas()) {
            return new CatalogAndSchema(null, null);
        } else if (supportsCatalogs()) { //schema is null
            if (catalogName == null) {
                if (schemaName == null) {
                    catalogName = getDefaultCatalogName();
                } else {
                    catalogName = schemaName;
                }
            }
            schemaName = catalogName;
        } else if (supportsSchemas()) {
            if (schemaName == null) {
                if (catalogName == null) {
                    schemaName = getDefaultSchemaName();
                } else {
                    schemaName = catalogName;
                }
            }
            catalogName = schemaName;
        }
        return new CatalogAndSchema(catalogName, schemaName);

    }

    @Override
    public String correctObjectName(String objectName, Class<? extends DatabaseObject> objectType) {
        if (quotingStrategy == ObjectQuotingStrategy.QUOTE_ALL_OBJECTS || unquotedObjectsAreUppercased == null
                || objectName == null || (objectName.startsWith(quotingStartCharacter) && objectName.endsWith(
                quotingEndCharacter))) {
            return objectName;
        } else if (unquotedObjectsAreUppercased == Boolean.TRUE) {
            return objectName.toUpperCase();
        } else {
            return objectName.toLowerCase();
        }
    }

    @Override
    public CatalogAndSchema getDefaultSchema() {
        return new CatalogAndSchema(getDefaultCatalogName(), getDefaultSchemaName());

    }

    @Override
    public String getDefaultSchemaName() {

        if (!supportsSchemas()) {
            return getDefaultCatalogName();
        }

        if (defaultSchemaName == null && connection != null) {
            defaultSchemaName = getConnectionSchemaName();
        }


        return defaultSchemaName;
    }

    /**
     * Overwrite this method to get the default schema name for the connection.
     *
     * @return
     */
    protected String getConnectionSchemaName() {
        if (connection == null) {
            return null;
        }

        java.sql.CallableStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = ((JdbcConnection) connection).prepareCall("call current_schema");
            resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getString(1);
        } catch (Exception e) {
            LogFactory.getLogger().info("Error getting default schema", e);
        } finally {
            if (null != resultSet) {
                try { resultSet.close(); } catch(Exception e) {/*ignore*/}
            }
            if (null != statement) {
                try { statement.close(); } catch(Exception e) {/*ignore*/}
            }
        }
        return null;
    }

    @Override
    public void setDefaultCatalogName(String defaultCatalogName) {
        this.defaultCatalogName = correctObjectName(defaultCatalogName, Catalog.class);
    }

    @Override
    public void setDefaultSchemaName(String schemaName) {
        this.defaultSchemaName = correctObjectName(schemaName, Schema.class);
    }

    /**
     * Returns system (undroppable) views.
     */
    protected Set<String> getSystemTables() {
        return new HashSet<String>();
    }


    /**
     * Returns system (undroppable) views.
     */
    protected Set<String> getSystemViews() {
        return new HashSet<String>();
    }

    // ------- DATABASE FEATURE INFORMATION METHODS ---- //

    /**
     * Does the database type support sequence.
     */
    @Override
    public boolean supportsSequences() {
        return true;
    }

    @Override
    public boolean supportsAutoIncrement() {
        return true;
    }

    // ------- DATABASE-SPECIFIC SQL METHODS ---- //

    @Override
    public void setCurrentDateTimeFunction(String function) {
        if (function != null) {
            this.currentDateTimeFunction = function;
            this.dateFunctions.add(new DatabaseFunction(function));
        }
    }

    /**
     * Return a date literal with the same value as a string formatted using ISO 8601.
     * <p/>
     * Note: many databases accept date literals in ISO8601 format with the 'T' replaced with
     * a space. Only databases which do not accept these strings should need to override this
     * method.
     * <p/>
     * Implementation restriction:
     * Currently, only the following subsets of ISO8601 are supported:
     * yyyy-MM-dd
     * hh:mm:ss
     * yyyy-MM-ddThh:mm:ss
     */
    @Override
    public String getDateLiteral(String isoDate) {
        if (isDateOnly(isoDate) || isTimeOnly(isoDate)) {
            return "'" + isoDate + "'";
        } else if (isDateTime(isoDate)) {
//            StringBuffer val = new StringBuffer();
//            val.append("'");
//            val.append(isoDate.substring(0, 10));
//            val.append(" ");
////noinspection MagicNumber
//            val.append(isoDate.substring(11));
//            val.append("'");
//            return val.toString();
            return "'" + isoDate.replace('T', ' ') + "'";
        } else {
            return "BAD_DATE_FORMAT:" + isoDate;
        }
    }


    @Override
    public String getDateTimeLiteral(java.sql.Timestamp date) {
        return getDateLiteral(new ISODateFormat().format(date).replaceFirst("^'", "").replaceFirst("'$", ""));
    }

    @Override
    public String getDateLiteral(java.sql.Date date) {
        return getDateLiteral(new ISODateFormat().format(date).replaceFirst("^'", "").replaceFirst("'$", ""));
    }

    @Override
    public String getTimeLiteral(java.sql.Time date) {
        return getDateLiteral(new ISODateFormat().format(date).replaceFirst("^'", "").replaceFirst("'$", ""));
    }

    @Override
    public String getDateLiteral(Date date) {
        if (date instanceof java.sql.Date) {
            return getDateLiteral(((java.sql.Date) date));
        } else if (date instanceof java.sql.Time) {
            return getTimeLiteral(((java.sql.Time) date));
        } else if (date instanceof java.sql.Timestamp) {
            return getDateTimeLiteral(((java.sql.Timestamp) date));
        } else {
            throw new RuntimeException("Unexpected type: " + date.getClass().getName());
        }
    }

    @Override
    public Date parseDate(String dateAsString) throws DateParseException {
        try {
            if (dateAsString.indexOf(' ') > 0) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateAsString);
            } else if (dateAsString.indexOf('T') > 0) {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(dateAsString);
            } else {
                if (dateAsString.indexOf(':') > 0) {
                    return new SimpleDateFormat("HH:mm:ss").parse(dateAsString);
                } else {
                    return new SimpleDateFormat("yyyy-MM-dd").parse(dateAsString);
                }
            }
        } catch (ParseException e) {
            throw new DateParseException(dateAsString);
        }
    }

    protected boolean isDateOnly(String isoDate) {
        return isoDate.length() == "yyyy-MM-dd".length();
    }

    protected boolean isDateTime(String isoDate) {
        return isoDate.length() >= "yyyy-MM-ddThh:mm:ss".length();
    }

    protected boolean isTimeOnly(String isoDate) {
        return isoDate.length() == "hh:mm:ss".length();
    }


    /**
     * Returns database-specific line comment string.
     */
    @Override
    public String getLineComment() {
        return "--";
    }

    /**
     * Returns database-specific auto-increment DDL clause.
     */
    @Override
    public String getAutoIncrementClause(BigInteger startWith, BigInteger incrementBy) {
        if (!supportsAutoIncrement()) {
            return "";
        }

        // generate an SQL:2003 standard compliant auto increment clause by default

        String autoIncrementClause = getAutoIncrementClause();

        boolean generateStartWith = generateAutoIncrementStartWith(startWith);
        boolean generateIncrementBy = generateAutoIncrementBy(incrementBy);

        if (generateStartWith || generateIncrementBy) {
            autoIncrementClause += getAutoIncrementOpening();

            if (generateStartWith) {
                autoIncrementClause += String.format(getAutoIncrementStartWithClause(), (startWith == null) ? defaultAutoIncrementStartWith : startWith);
            }

            if (generateIncrementBy) {
                if (generateStartWith) {
                    autoIncrementClause += ", ";
                }

                autoIncrementClause += String.format(getAutoIncrementByClause(), (incrementBy == null) ? defaultAutoIncrementBy : incrementBy);
            }

            autoIncrementClause += getAutoIncrementClosing();
        }

        return autoIncrementClause;
    }

    protected String getAutoIncrementClause() {
        return "GENERATED BY DEFAULT AS IDENTITY";
    }

    protected boolean generateAutoIncrementStartWith(BigInteger startWith) {
        return startWith != null
                && !startWith.equals(defaultAutoIncrementStartWith);
    }

    protected boolean generateAutoIncrementBy(BigInteger incrementBy) {
        return incrementBy != null
                && !incrementBy.equals(defaultAutoIncrementBy);
    }

    protected String getAutoIncrementOpening() {
        return " (";
    }

    protected String getAutoIncrementClosing() {
        return ")";
    }

    protected String getAutoIncrementStartWithClause() {
        return "START WITH %d";
    }

    protected String getAutoIncrementByClause() {
        return "INCREMENT BY %d";
    }

    @Override
    public String getConcatSql(String... values) {
        StringBuffer returnString = new StringBuffer();
        for (String value : values) {
            returnString.append(value).append(" || ");
        }

        return returnString.toString().replaceFirst(" \\|\\| $", "");
    }

// ------- DATABASECHANGELOG / DATABASECHANGELOGLOCK METHODS ---- //

    /**
     * @see liquibase.database.Database#getDatabaseChangeLogTableName()
     */
    @Override
    public String getDatabaseChangeLogTableName() {
        return databaseChangeLogTableName;
    }

    /**
     * @see liquibase.database.Database#getDatabaseChangeLogLockTableName()
     */
    @Override
    public String getDatabaseChangeLogLockTableName() {
        return databaseChangeLogLockTableName;
    }

    /**
     * @see liquibase.database.Database#getLiquibaseTablespaceName()
     */
    @Override
    public String getLiquibaseTablespaceName() {
        return liquibaseTablespaceName;
    }

    /**
     * @see liquibase.database.Database#setDatabaseChangeLogTableName(java.lang.String)
     */
    @Override
    public void setDatabaseChangeLogTableName(String tableName) {
        this.databaseChangeLogTableName = tableName;
    }

    /**
     * @see liquibase.database.Database#setDatabaseChangeLogLockTableName(java.lang.String)
     */
    @Override
    public void setDatabaseChangeLogLockTableName(String tableName) {
        this.databaseChangeLogLockTableName = tableName;
    }

    /**
     * @see liquibase.database.Database#setLiquibaseTablespaceName(java.lang.String)
     */
    @Override
    public void setLiquibaseTablespaceName(String tablespace) {
        this.liquibaseTablespaceName = tablespace;
    }

    /**
     * This method will check the database ChangeLog table used to keep track of
     * the changes in the file. If the table does not exist it will create one
     * otherwise it will not do anything besides outputting a log message.
     *
     * @param updateExistingNullChecksums
     * @param contexts
     */
    @Override
    public void checkDatabaseChangeLogTable(boolean updateExistingNullChecksums, DatabaseChangeLog databaseChangeLog, String... contexts) throws DatabaseException {
        Executor executor = ExecutorService.getInstance().getExecutor(this);

        Table changeLogTable = null;
        try {
            changeLogTable = SnapshotGeneratorFactory.getInstance().getDatabaseChangeLogTable(new SnapshotControl(this, Table.class, Column.class), this);
        } catch (LiquibaseException e) {
            throw new UnexpectedLiquibaseException(e);
        }

        List<SqlStatement> statementsToExecute = new ArrayList<SqlStatement>();

        boolean changeLogCreateAttempted = false;
        if (changeLogTable != null) {
            boolean hasDescription = changeLogTable.getColumn("DESCRIPTION") != null;
            boolean hasComments = changeLogTable.getColumn("COMMENTS") != null;
            boolean hasTag = changeLogTable.getColumn("TAG") != null;
            boolean hasLiquibase = changeLogTable.getColumn("LIQUIBASE") != null;
            boolean liquibaseColumnNotRightSize = false;
            if (!connection.getDatabaseProductName().equals("SQLite")) {
                Integer columnSize = changeLogTable.getColumn("LIQUIBASE").getType().getColumnSize();
                liquibaseColumnNotRightSize = columnSize != null && columnSize.intValue() != 20;
            }
            boolean hasOrderExecuted = changeLogTable.getColumn("ORDEREXECUTED") != null;
            boolean checksumNotRightSize = false;
            if (!connection.getDatabaseProductName().equals("SQLite")) {
                Integer columnSize = changeLogTable.getColumn("MD5SUM").getType().getColumnSize();
                checksumNotRightSize = columnSize != null && columnSize.intValue() != 35;
            }
            boolean hasExecTypeColumn = changeLogTable.getColumn("EXECTYPE") != null;

            if (!hasDescription) {
                executor.comment("Adding missing databasechangelog.description column");
                statementsToExecute.add(new AddColumnStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(), "DESCRIPTION", "VARCHAR(255)", null));
            }
            if (!hasTag) {
                executor.comment("Adding missing databasechangelog.tag column");
                statementsToExecute.add(new AddColumnStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(), "TAG", "VARCHAR(255)", null));
            }
            if (!hasComments) {
                executor.comment("Adding missing databasechangelog.comments column");
                statementsToExecute.add(new AddColumnStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(), "COMMENTS", "VARCHAR(255)", null));
            }
            if (!hasLiquibase) {
                executor.comment("Adding missing databasechangelog.liquibase column");
                statementsToExecute.add(new AddColumnStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(), "LIQUIBASE", "VARCHAR(255)", null));
            }
            if (!hasOrderExecuted) {
                executor.comment("Adding missing databasechangelog.orderexecuted column");
                statementsToExecute.add(new AddColumnStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(), "ORDEREXECUTED", "INT", null));
                statementsToExecute.add(new UpdateStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName()).addNewColumnValue("ORDEREXECUTED", Integer.valueOf(-1)));
                statementsToExecute.add(new SetNullableStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(), "ORDEREXECUTED", "INT", false));
            }
            if (checksumNotRightSize) {
                executor.comment("Modifying size of databasechangelog.md5sum column");

                statementsToExecute.add(new ModifyDataTypeStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(), "MD5SUM", "VARCHAR(35)"));
            }
            if (liquibaseColumnNotRightSize) {
                executor.comment("Modifying size of databasechangelog.liquibase column");

                statementsToExecute.add(new ModifyDataTypeStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(), "LIQUIBASE", "VARCHAR(20)"));
            }
            if (!hasExecTypeColumn) {
                executor.comment("Adding missing databasechangelog.exectype column");
                statementsToExecute.add(new AddColumnStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(), "EXECTYPE", "VARCHAR(10)", null));
                statementsToExecute.add(new UpdateStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName()).addNewColumnValue("EXECTYPE", "EXECUTED"));
                statementsToExecute.add(new SetNullableStatement(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName(), "EXECTYPE", "VARCHAR(10)", false));
            }

            List<Map> md5sumRS = ExecutorService.getInstance().getExecutor(this).queryForList(new SelectFromDatabaseChangeLogStatement(new SelectFromDatabaseChangeLogStatement.ByNotNullCheckSum(), "MD5SUM"));
            if (md5sumRS.size() > 0) {
                String md5sum = md5sumRS.get(0).get("MD5SUM").toString();
                if (!md5sum.startsWith(CheckSum.getCurrentVersion() + ":")) {
                    executor.comment("DatabaseChangeLog checksums are an incompatible version.  Setting them to null so they will be updated on next database update");
                    statementsToExecute.add(new RawSqlStatement("UPDATE " + escapeTableName(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName()) + " SET MD5SUM=null"));
                }
            }


        } else if (!changeLogCreateAttempted) {
            executor.comment("Create Database Change Log Table");
            SqlStatement createTableStatement = new CreateDatabaseChangeLogTableStatement();
            if (!canCreateChangeLogTable()) {
                throw new DatabaseException("Cannot create " + escapeTableName(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName()) + " table for your database.\n\n" +
                        "Please construct it manually using the following SQL as a base and re-run Liquibase:\n\n" +
                        createTableStatement);
            }
            // If there is no table in the database for recording change history create one.
            statementsToExecute.add(createTableStatement);
            LogFactory.getLogger().info("Creating database history table with name: " + escapeTableName(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName()));
//                }
        }

        for (SqlStatement sql : statementsToExecute) {
            if (SqlGeneratorFactory.getInstance().supports(sql, this)) {
                executor.execute(sql);
                this.commit();
            } else {
                LogFactory.getLogger().info("Cannot run "+sql.getClass().getSimpleName()+" on "+this.getShortName()+" when checking databasechangelog table");
            }
        }

        if (updateExistingNullChecksums) {
            for (RanChangeSet ranChangeSet : this.getRanChangeSetList()) {
                if (ranChangeSet.getLastCheckSum() == null) {
                    ChangeSet changeSet = databaseChangeLog.getChangeSet(ranChangeSet);
                    if (changeSet != null && new ContextChangeSetFilter(contexts).accepts(changeSet) && new DbmsChangeSetFilter(this).accepts(changeSet)) {
                        LogFactory.getLogger().debug("Updating null or out of date checksum on changeSet " + changeSet + " to correct value");
                        executor.execute(new UpdateChangeSetChecksumStatement(changeSet));
                    }
                }
            }
            commit();
            this.ranChangeSetList = null;
        }
    }


    protected boolean canCreateChangeLogTable() throws DatabaseException {
        return true;
    }

    @Override
    public void setCanCacheLiquibaseTableInfo(boolean canCacheLiquibaseTableInfo) {
        this.canCacheLiquibaseTableInfo = canCacheLiquibaseTableInfo;
        hasDatabaseChangeLogTable = false;
        hasDatabaseChangeLogLockTable = false;
    }

    @Override
    public boolean hasDatabaseChangeLogTable() throws DatabaseException {
        if (hasDatabaseChangeLogTable) {
            return true;
        }
        boolean hasTable = false;
        try {
            hasTable = SnapshotGeneratorFactory.getInstance().hasDatabaseChangeLogTable(this);
        } catch (LiquibaseException e) {
            throw new UnexpectedLiquibaseException(e);
        }
        if (canCacheLiquibaseTableInfo) {
            hasDatabaseChangeLogTable = hasTable;
        }
        return hasTable;
    }

    @Override
    public boolean hasDatabaseChangeLogLockTable() throws DatabaseException {
        if (canCacheLiquibaseTableInfo && hasDatabaseChangeLogLockTable) {
            return true;
        }
        boolean hasTable = false;
        try {
            hasTable = SnapshotGeneratorFactory.getInstance().hasDatabaseChangeLogLockTable(this);
        } catch (LiquibaseException e) {
            throw new UnexpectedLiquibaseException(e);
        }
        if (canCacheLiquibaseTableInfo) {
            hasDatabaseChangeLogLockTable = hasTable;
        }
        return hasTable;
    }

    public boolean isDatabaseChangeLogLockTableInitialized(boolean tableJustCreated) throws DatabaseException {
        if (canCacheLiquibaseTableInfo && isDatabaseChangeLogLockTableInitialized) {
            return true;
        }
        boolean initialized;
        Executor executor = ExecutorService.getInstance().getExecutor(this);
        try {
            initialized = executor.queryForInt(new RawSqlStatement("select count(*) from " + this.escapeTableName(this.getLiquibaseCatalogName(), this.getLiquibaseSchemaName(), this.getDatabaseChangeLogLockTableName()))) > 0;
        } catch (LiquibaseException e) {
            if (executor.updatesDatabase()) {
                throw new UnexpectedLiquibaseException(e);
            } else {
                //probably didn't actually create the table yet.

                initialized = !tableJustCreated;
            }
        }
        if (canCacheLiquibaseTableInfo) {
            isDatabaseChangeLogLockTableInitialized = initialized;
        }
        return initialized;
    }

    @Override
    public String getLiquibaseCatalogName() {
        return liquibaseCatalogName == null ? getDefaultCatalogName() : liquibaseCatalogName;
    }

    @Override
    public void setLiquibaseCatalogName(String catalogName) {
        this.liquibaseCatalogName = catalogName;
    }

    @Override
    public String getLiquibaseSchemaName() {
        return liquibaseSchemaName == null ? getDefaultSchemaName() : liquibaseSchemaName;
    }

    @Override
    public void setLiquibaseSchemaName(String schemaName) {
        this.liquibaseSchemaName = schemaName;
    }

    /**
     * This method will check the database ChangeLogLock table used to keep track of
     * if a machine is updating the database. If the table does not exist it will create one
     * otherwise it will not do anything besides outputting a log message.
     */
    @Override
    public void checkDatabaseChangeLogLockTable() throws DatabaseException {

        boolean createdTable = false;
        Executor executor = ExecutorService.getInstance().getExecutor(this);
        if (!hasDatabaseChangeLogLockTable()) {

            executor.comment("Create Database Lock Table");
            executor.execute(new CreateDatabaseChangeLogLockTableStatement());
            this.commit();
            LogFactory.getLogger().debug("Created database lock table with name: " + escapeTableName(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogLockTableName()));
            this.hasDatabaseChangeLogLockTable = true;
            createdTable = true;
        }

        if (!isDatabaseChangeLogLockTableInitialized(createdTable)) {
            executor.comment("Initialize Database Lock Table");
            executor.execute(new InitializeDatabaseChangeLogLockTableStatement());
            this.commit();
        }
    }

    @Override
    public boolean isCaseSensitive() {
    	if (caseSensitive == null) {
            if (connection != null) {
                try {
                	caseSensitive = Boolean.valueOf(((JdbcConnection) connection).getUnderlyingConnection().getMetaData().supportsMixedCaseIdentifiers());
                } catch (SQLException e) {
                    LogFactory.getLogger().warning("Cannot determine case sensitivity from JDBC driver", e);
                }
            }
        }

    	if (caseSensitive == null) {
            return false;
    	} else {
    		return caseSensitive.booleanValue();
    	}
    }

    @Override
    public boolean isReservedWord(String string) {
        return false;
    }

    /*
    * Check if given string starts with numeric values that may cause problems and should be escaped.
    */
    protected boolean startsWithNumeric(String objectName) {
        return startsWithNumberPattern.matcher(objectName).matches();
    }

// ------- DATABASE OBJECT DROPPING METHODS ---- //

    /**
     * Drops all objects owned by the connected user.
     */
    @Override
    public void dropDatabaseObjects(CatalogAndSchema schemaToDrop) throws LiquibaseException {
        ObjectQuotingStrategy currentStrategy = this.getObjectQuotingStrategy();
        this.setObjectQuotingStrategy(ObjectQuotingStrategy.QUOTE_ALL_OBJECTS);
        try {
            DatabaseSnapshot snapshot;
            try {
	            final SnapshotControl snapshotControl = new SnapshotControl(this);
	            final Set<Class<? extends DatabaseObject>> typesToInclude = snapshotControl.getTypesToInclude();

	            //We do not need to remove indexes and primary/unique keys explicitly. They should be removed
	            //as part of tables.
	            typesToInclude.remove(Index.class);
	            typesToInclude.remove(PrimaryKey.class);
	            typesToInclude.remove(UniqueConstraint.class);

	            if (supportsForeignKeyDisable()) {
		            //We do not remove ForeignKey because they will be disabled and removed as parts of tables.
		            typesToInclude.remove(ForeignKey.class);
	            }

	            final long createSnapshotStarted = System.currentTimeMillis();
	            snapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(schemaToDrop, this, snapshotControl);
	            LogFactory.getLogger().debug(String.format("Database snapshot generated in %d ms. Snapshot includes: %s", Long.valueOf(System.currentTimeMillis() - createSnapshotStarted), typesToInclude));
            } catch (LiquibaseException e) {
                throw new UnexpectedLiquibaseException(e);
            }

	        final long changeSetStarted = System.currentTimeMillis();
	        DiffResult diffResult = DiffGeneratorFactory.getInstance().compare(new EmptyDatabaseSnapshot(this), snapshot, new CompareControl(snapshot.getSnapshotControl().getTypesToInclude()));
            List<ChangeSet> changeSets = new DiffToChangeLog(diffResult, new DiffOutputControl(true, true, false)).generateChangeSets();
	        LogFactory.getLogger().debug(String.format("ChangeSet to Remove Database Objects generated in %d ms.", Long.valueOf(System.currentTimeMillis() - changeSetStarted)));

            final boolean reEnableFK = supportsForeignKeyDisable() && disableForeignKeyChecks();
            try {
                for (ChangeSet changeSet : changeSets) {
                    for (Change change : changeSet.getChanges()) {
                        SqlStatement[] sqlStatements = change.generateStatements(this);
                        for (SqlStatement statement : sqlStatements) {
                            ExecutorService.getInstance().getExecutor(this).execute(statement);
                        }

                    }
                }
            } finally {
                if (reEnableFK) {
                    enableForeignKeyChecks();
                }
            }

            if (SnapshotGeneratorFactory.getInstance().has(new Table().setName(this.getDatabaseChangeLogTableName()).setSchema(this.getLiquibaseCatalogName(), this.getLiquibaseSchemaName()), this)) {
                ExecutorService.getInstance().getExecutor(this).execute(new DropTableStatement(this.getLiquibaseCatalogName(), this.getLiquibaseSchemaName(), this.getDatabaseChangeLogTableName(), false));
            }
            if (SnapshotGeneratorFactory.getInstance().has(new Table().setName(this.getDatabaseChangeLogLockTableName()).setSchema(this.getLiquibaseCatalogName(), this.getLiquibaseSchemaName()), this)) {
                ExecutorService.getInstance().getExecutor(this).execute(new DropTableStatement(this.getLiquibaseCatalogName(), this.getLiquibaseSchemaName(), this.getDatabaseChangeLogLockTableName(), false));
            }

        } finally {
            this.setObjectQuotingStrategy(currentStrategy);
            this.commit();
        }
    }

    @Override
    public boolean supportsDropTableCascadeConstraints() {
        return (this instanceof SQLiteDatabase
                || this instanceof SybaseDatabase
                || this instanceof SybaseASADatabase
                || this instanceof PostgresDatabase
                || this instanceof OracleDatabase
        );
    }

    @Override
    public boolean isSystemObject(DatabaseObject example) {
        if (example == null) {
            return false;
        }
        if (example.getSchema() != null && example.getSchema().getName() != null && example.getSchema().getName().equalsIgnoreCase("information_schema")) {
            return true;
        }
        if (example instanceof Table && getSystemTables().contains(example.getName())) {
            return true;
        }

        if (example instanceof View && getSystemViews().contains(example.getName())) {
            return true;
        }

        return false;
    }

    public boolean isSystemView(CatalogAndSchema schema, String viewName) {
        schema = correctSchema(schema);
        if ("information_schema".equalsIgnoreCase(schema.getSchemaName())) {
            return true;
        } else if (getSystemViews().contains(viewName)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isLiquibaseObject(DatabaseObject object) {
        if (object instanceof Table) {
            Schema liquibaseSchema = new Schema(getLiquibaseCatalogName(), getLiquibaseSchemaName());
            if (DatabaseObjectComparatorFactory.getInstance().isSameObject(object, new Table().setName(getDatabaseChangeLogTableName()).setSchema(liquibaseSchema), this)) {
                return true;
            }
            if (DatabaseObjectComparatorFactory.getInstance().isSameObject(object, new Table().setName(getDatabaseChangeLogLockTableName()).setSchema(liquibaseSchema), this)) {
                return true;
            }
            return false;
        } else if (object instanceof Column) {
            return isLiquibaseObject(((Column) object).getRelation());
        } else if (object instanceof Index) {
            return isLiquibaseObject(((Index) object).getTable());
        } else if (object instanceof PrimaryKey) {
            return isLiquibaseObject(((PrimaryKey) object).getTable());
        }
        return false;
    }

    // ------- DATABASE TAGGING METHODS ---- //

    /**
     * Tags the database changelog with the given string.
     */
    @Override
    public void tag(String tagString) throws DatabaseException {
        Executor executor = ExecutorService.getInstance().getExecutor(this);
        try {
            int totalRows = ExecutorService.getInstance().getExecutor(this).queryForInt(new SelectFromDatabaseChangeLogStatement("COUNT(*)"));
            if (totalRows == 0) {
                ChangeSet emptyChangeSet = new ChangeSet(String.valueOf(new Date().getTime()), "liquibase", false, false, "liquibase-internal", null, null, quotingStrategy, null);
                this.markChangeSetExecStatus(emptyChangeSet, ChangeSet.ExecType.EXECUTED);
            }

//            Timestamp lastExecutedDate = (Timestamp) this.getExecutor().queryForObject(createChangeToTagSQL(), Timestamp.class);
            executor.execute(new TagDatabaseStatement(tagString));
            this.commit();

            getRanChangeSetList().get(getRanChangeSetList().size() - 1).setTag(tagString);
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean doesTagExist(String tag) throws DatabaseException {
        int count = ExecutorService.getInstance().getExecutor(this).queryForInt(new SelectFromDatabaseChangeLogStatement(new SelectFromDatabaseChangeLogStatement.ByTag(tag), "COUNT(*)"));
        return count > 0;
    }

    @Override
    public String toString() {
        if (getConnection() == null) {
            return getShortName() + " Database";
        }

        return getConnection().getConnectionUserName() + " @ " + getConnection().getURL() + (getDefaultSchemaName() == null ? "" : " (Default Schema: " + getDefaultSchemaName() + ")");
    }


    @Override
    public String getViewDefinition(CatalogAndSchema schema, String viewName) throws DatabaseException {
        schema = correctSchema(schema);
        String definition = (String) ExecutorService.getInstance().getExecutor(this).queryForObject(new GetViewDefinitionStatement(schema.getCatalogName(), schema.getSchemaName(), viewName), String.class);
        if (definition == null) {
            return null;
        }
        return CREATE_VIEW_AS_PATTERN.matcher(definition).replaceFirst("");
    }

    @Override
    public String escapeTableName(String catalogName, String schemaName, String tableName) {
        return escapeObjectName(catalogName, schemaName, tableName, Table.class);
    }

    @Override
    public String escapeObjectName(String catalogName, String schemaName, String objectName, Class<? extends DatabaseObject> objectType) {
//        CatalogAndSchema catalogAndSchema = this.correctSchema(catalogName, schemaName);
//        catalogName = catalogAndSchema.getCatalogName();
//        schemaName = catalogAndSchema.getSchemaName();

        if (supportsSchemas()) {
            catalogName = StringUtils.trimToNull(catalogName);
            schemaName = StringUtils.trimToNull(schemaName);

            if (catalogName == null) {
                catalogName = this.getDefaultCatalogName();
            }
            if (schemaName == null) {
                schemaName = this.getDefaultSchemaName();
            }

            if (!supportsCatalogInObjectName(objectType)) {
                catalogName = null;
            }
            if (catalogName == null && schemaName == null) {
                return escapeObjectName(objectName, objectType);
            } else if (catalogName == null || !this.supportsCatalogInObjectName(objectType)) {
                if (isDefaultSchema(catalogName, schemaName) && !getOutputDefaultSchema()) {
                    return escapeObjectName(objectName, objectType);
                } else {
                    return escapeObjectName(schemaName, Schema.class) + "." + escapeObjectName(objectName, objectType);
                }
            } else {
                if (isDefaultSchema(catalogName, schemaName) && !getOutputDefaultSchema() && !getOutputDefaultCatalog()) {
                    return escapeObjectName(objectName, objectType);
                } else if (isDefaultSchema(catalogName, schemaName) && !getOutputDefaultCatalog()) {
                    return escapeObjectName(schemaName, Schema.class) + "." + escapeObjectName(objectName, objectType);
                } else {
                    return escapeObjectName(catalogName, Catalog.class) + "." + escapeObjectName(schemaName, Schema.class) + "." + escapeObjectName(objectName, objectType);
                }
            }
        } else if (supportsCatalogs()) {
            catalogName = StringUtils.trimToNull(catalogName);
            schemaName = StringUtils.trimToNull(schemaName);

            if (catalogName != null) {
                if (getOutputDefaultCatalog()) {
                    return escapeObjectName(catalogName, Catalog.class) + "." + escapeObjectName(objectName, objectType);
                } else {
                    if (isDefaultCatalog(catalogName)) {
                        return escapeObjectName(objectName, objectType);
                    } else {
                        return escapeObjectName(catalogName, Catalog.class) + "." + escapeObjectName(objectName, objectType);
                    }
                }
            } else {
                if (schemaName != null) { //they actually mean catalog name
                    if (getOutputDefaultCatalog()) {
                        return escapeObjectName(schemaName, Catalog.class) + "." + escapeObjectName(objectName, objectType);
                    } else {
                        if (isDefaultCatalog(schemaName)) {
                            return escapeObjectName(objectName, objectType);
                        } else {
                            return escapeObjectName(schemaName, Catalog.class) + "." + escapeObjectName(objectName, objectType);
                        }
                    }
                } else {
                    catalogName = this.getDefaultCatalogName();

                    if (catalogName == null) {
                        return escapeObjectName(objectName, objectType);
                    } else {
                        if (isDefaultCatalog(catalogName) && getOutputDefaultCatalog()) {
                            return escapeObjectName(catalogName, Catalog.class) + "." + escapeObjectName(objectName, objectType);
                        } else {
                            return escapeObjectName(objectName, objectType);
                        }
                    }
                }
            }

        } else {
            return escapeObjectName(objectName, objectType);
        }
    }

    @Override
    public String escapeObjectName(String objectName, Class<? extends DatabaseObject> objectType) {
        if (objectName != null) {
            if (objectName.contains("-") || startsWithNumeric(objectName) || isReservedWord(objectName)) {
                return quoteObject(objectName, objectType);
            } else if (quotingStrategy == ObjectQuotingStrategy.QUOTE_ALL_OBJECTS) {
                return quoteObject(objectName, objectType);
            }
            objectName = objectName.trim();
        }
        return objectName;
    }

    public String quoteObject(String objectName, Class<? extends DatabaseObject> objectType) {
        return quotingStartCharacter + objectName + quotingEndCharacter;
    }

    @Override
    public String escapeIndexName(String catalogName, String schemaName, String indexName) {
        return escapeObjectName(catalogName, schemaName, indexName, Index.class);
    }

    @Override
    public String escapeSequenceName(String catalogName, String schemaName, String sequenceName) {
        return escapeObjectName(catalogName, schemaName, sequenceName, Sequence.class);
    }

    @Override
    public String escapeConstraintName(String constraintName) {
        return escapeObjectName(constraintName, Index.class);
    }

    @Override
    public String escapeColumnName(String catalogName, String schemaName, String tableName, String columnName) {
        return escapeObjectName(columnName, Column.class);
    }

    @Override
    public String escapeColumnNameList(String columnNames) {
        StringBuffer sb = new StringBuffer();
        for (String columnName : columnNames.split(",")) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(escapeObjectName(columnName.trim(), Column.class));
        }
        return sb.toString();

    }

    @Override
    public boolean supportsSchemas() {
        return true;
    }

    @Override
    public boolean supportsCatalogs() {
        return true;
    }

    public boolean jdbcCallsCatalogsSchemas() {
        return false;
    }

    @Override
    public boolean supportsCatalogInObjectName(Class<? extends DatabaseObject> type) {
        return false;
    }

    @Override
    public String generatePrimaryKeyName(String tableName) {
        return "PK_" + tableName.toUpperCase();
    }

    @Override
    public String escapeViewName(String catalogName, String schemaName, String viewName) {
        return escapeObjectName(catalogName, schemaName, viewName, View.class);
    }

    /**
     * Returns the run status for the given ChangeSet
     */
    @Override
    public ChangeSet.RunStatus getRunStatus(ChangeSet changeSet) throws DatabaseException, DatabaseHistoryException {
        if (!hasDatabaseChangeLogTable()) {
            return ChangeSet.RunStatus.NOT_RAN;
        }

        RanChangeSet foundRan = getRanChangeSet(changeSet);

        if (foundRan == null) {
            return ChangeSet.RunStatus.NOT_RAN;
        } else {
            if (foundRan.getLastCheckSum() == null) {
                try {
                    LogFactory.getLogger().info("Updating NULL md5sum for " + changeSet.toString());
                    ExecutorService.getInstance().getExecutor(this).execute(new RawSqlStatement("UPDATE " + escapeTableName(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName()) + " SET MD5SUM='" + changeSet.generateCheckSum().toString() + "' WHERE ID='" + changeSet.getId() + "' AND AUTHOR='" + changeSet.getAuthor() + "' AND FILENAME='" + changeSet.getFilePath() + "'"));

                    this.commit();
                } catch (DatabaseException e) {
                    throw new DatabaseException(e);
                }

                return ChangeSet.RunStatus.ALREADY_RAN;
            } else {
                if (foundRan.getLastCheckSum().equals(changeSet.generateCheckSum())) {
                    return ChangeSet.RunStatus.ALREADY_RAN;
                } else {
                    if (changeSet.shouldRunOnChange()) {
                        return ChangeSet.RunStatus.RUN_AGAIN;
                    } else {
                        return ChangeSet.RunStatus.INVALID_MD5SUM;
//                        throw new DatabaseHistoryException("MD5 Check for " + changeSet.toString() + " failed");
                    }
                }
            }
        }
    }

    @Override
    public RanChangeSet getRanChangeSet(ChangeSet changeSet) throws DatabaseException, DatabaseHistoryException {
        if (!hasDatabaseChangeLogTable()) {
            return null;
        }

        RanChangeSet foundRan = null;
        for (RanChangeSet ranChange : getRanChangeSetList()) {
            if (ranChange.isSameAs(changeSet)) {
                foundRan = ranChange;
                break;
            }
        }
        return foundRan;
    }

    /**
     * Returns the ChangeSets that have been run against the current database.
     */
    @Override
    public List<RanChangeSet> getRanChangeSetList() throws DatabaseException {
        if (this.ranChangeSetList != null) {
            return this.ranChangeSetList;
        }

        String databaseChangeLogTableName = escapeTableName(getLiquibaseCatalogName(), getLiquibaseSchemaName(), getDatabaseChangeLogTableName());
        ranChangeSetList = new ArrayList<RanChangeSet>();
        if (hasDatabaseChangeLogTable()) {
            LogFactory.getLogger().info("Reading from " + databaseChangeLogTableName);
            SqlStatement select = new SelectFromDatabaseChangeLogStatement("FILENAME", "AUTHOR", "ID", "MD5SUM", "DATEEXECUTED", "ORDEREXECUTED", "TAG", "EXECTYPE", "DESCRIPTION", "COMMENTS").setOrderBy("DATEEXECUTED ASC", "ORDEREXECUTED ASC");
            List<Map> results = ExecutorService.getInstance().getExecutor(this).queryForList(select);
            for (Map rs : results) {
                String fileName = rs.get("FILENAME").toString();
                String author = rs.get("AUTHOR").toString();
                String id = rs.get("ID").toString();
                String md5sum = rs.get("MD5SUM") == null ? null : rs.get("MD5SUM").toString();
                String description = rs.get("DESCRIPTION") == null ? null : rs.get("DESCRIPTION").toString();
                String comments = rs.get("COMMENTS") == null ? null : rs.get("COMMENTS").toString();
                Object tmpDateExecuted = rs.get("DATEEXECUTED");
                Date dateExecuted = null;
                if (tmpDateExecuted instanceof LocalDateTime) {
                    dateExecuted = Date.from(java.sql.Timestamp.valueOf(((LocalDateTime) tmpDateExecuted)).toInstant());
                } else if (tmpDateExecuted instanceof Date) {
                    dateExecuted = (Date) tmpDateExecuted;
                } else {
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        dateExecuted = df.parse((String) tmpDateExecuted);
                    } catch (ParseException e) {
                    }
                }
                String tag = rs.get("TAG") == null ? null : rs.get("TAG").toString();
                String execType = rs.get("EXECTYPE") == null ? null : rs.get("EXECTYPE").toString();
                try {
                    RanChangeSet ranChangeSet = new RanChangeSet(fileName, id, author, CheckSum.parse(md5sum), dateExecuted, tag, ChangeSet.ExecType.valueOf(execType), description, comments);
                    ranChangeSetList.add(ranChangeSet);
                } catch (IllegalArgumentException e) {
                    LogFactory.getLogger().severe("Unknown EXECTYPE from database: " + execType);
                    throw e;
                }
            }
        }
        return ranChangeSetList;
    }

    @Override
    public Date getRanDate(ChangeSet changeSet) throws DatabaseException, DatabaseHistoryException {
        RanChangeSet ranChange = getRanChangeSet(changeSet);
        if (ranChange == null) {
            return null;
        } else {
            return ranChange.getDateExecuted();
        }
    }

    /**
     * After the change set has been ran against the database this method will update the change log table
     * with the information.
     */
    @Override
    public void markChangeSetExecStatus(ChangeSet changeSet, ChangeSet.ExecType execType) throws DatabaseException {


        ExecutorService.getInstance().getExecutor(this).execute(new MarkChangeSetRanStatement(changeSet, execType));
        commit();
        getRanChangeSetList().add(new RanChangeSet(changeSet, execType));
    }

    @Override
    public void removeRanStatus(ChangeSet changeSet) throws DatabaseException {

        ExecutorService.getInstance().getExecutor(this).execute(new RemoveChangeSetRanStatusStatement(changeSet));
        commit();

        getRanChangeSetList().remove(new RanChangeSet(changeSet));
    }

    @Override
    public String escapeStringForDatabase(String string) {
        if (string == null) {
            return null;
        }
        return string.replaceAll("'", "''");
    }

    @Override
    public void commit() throws DatabaseException {
        try {
            getConnection().commit();
        } catch (DatabaseException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void rollback() throws DatabaseException {
        try {
            getConnection().rollback();
        } catch (DatabaseException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractJdbcDatabase that = (AbstractJdbcDatabase) o;

        if (connection == null) {
            if (that.connection == null) {
                return this == that;
            } else {
                return false;
            }
        } else {
            return connection.equals(that.connection);
        }
    }

    @Override
    public int hashCode() {
        return (connection != null ? connection.hashCode() : super.hashCode());
    }

    @Override
    public void close() throws DatabaseException {
        DatabaseConnection connection = getConnection();
        if (connection != null) {
            if (previousAutoCommit != null) {
                try {
                    connection.setAutoCommit(previousAutoCommit.booleanValue());
                } catch (DatabaseException e) {
                    LogFactory.getLogger().warning("Failed to restore the auto commit to " + previousAutoCommit);

                    throw e;
                }
            }
            connection.close();
        }
    }

    @Override
    public boolean supportsRestrictForeignKeys() {
        return true;
    }

    @Override
    public boolean isAutoCommit() throws DatabaseException {
        try {
            return getConnection().getAutoCommit();
        } catch (DatabaseException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void setAutoCommit(boolean b) throws DatabaseException {
        try {
            getConnection().setAutoCommit(b);
        } catch (DatabaseException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Default implementation, just look for "local" IPs. If the database returns a null URL we return false since we don't know it's safe to run the update.
     *
     * @throws liquibase.exception.DatabaseException
     *
     */
    @Override
    public boolean isSafeToRunUpdate() throws DatabaseException {
        DatabaseConnection connection = getConnection();
        if (connection == null) {
            return true;
        }
        String url = connection.getURL();
        if (url == null) {
            return false;
        }
        return (url.contains("localhost")) || (url.contains("127.0.0.1"));
    }

    @Override
    public void executeStatements(Change change, DatabaseChangeLog changeLog, List<SqlVisitor> sqlVisitors) throws LiquibaseException {
        SqlStatement[] statements = change.generateStatements(this);

        execute(statements, sqlVisitors);
    }

    /*
     * Executes the statements passed as argument to a target {@link Database}
     *
     * @param statements an array containing the SQL statements to be issued
     * @param database the target {@link Database}
     * @throws DatabaseException if there were problems issuing the statements
     */
    @Override
    public void execute(SqlStatement[] statements, List<SqlVisitor> sqlVisitors) throws LiquibaseException {
        for (SqlStatement statement : statements) {
            if (statement.skipOnUnsupported() && !SqlGeneratorFactory.getInstance().supports(statement, this)) {
                continue;
            }
            LogFactory.getLogger().debug("Executing Statement: " + statement.getClass().getName());
            ExecutorService.getInstance().getExecutor(this).execute(statement, sqlVisitors);
        }
    }


    @Override
    public void saveStatements(Change change, List<SqlVisitor> sqlVisitors, Writer writer) throws IOException, StatementNotSupportedOnDatabaseException, LiquibaseException {
        SqlStatement[] statements = change.generateStatements(this);
        for (SqlStatement statement : statements) {
            for (Sql sql : SqlGeneratorFactory.getInstance().generateSql(statement, this)) {
                writer.append(sql.toSql()).append(sql.getEndDelimiter()).append(StreamUtil.getLineSeparator()).append(StreamUtil.getLineSeparator());
            }
        }
    }

    @Override
    public void executeRollbackStatements(Change change, List<SqlVisitor> sqlVisitors) throws LiquibaseException, RollbackImpossibleException {
        SqlStatement[] statements = change.generateRollbackStatements(this);
        List<SqlVisitor> rollbackVisitors = new ArrayList<SqlVisitor>();
        if (sqlVisitors != null) {
            for (SqlVisitor visitor : sqlVisitors) {
                if (visitor.isApplyToRollback()) {
                    rollbackVisitors.add(visitor);
                }
            }
        }
        execute(statements, rollbackVisitors);
    }

    @Override
    public void saveRollbackStatement(Change change, List<SqlVisitor> sqlVisitors, Writer writer) throws IOException, RollbackImpossibleException, StatementNotSupportedOnDatabaseException, LiquibaseException {
        SqlStatement[] statements = change.generateRollbackStatements(this);
        for (SqlStatement statement : statements) {
            for (Sql sql : SqlGeneratorFactory.getInstance().generateSql(statement, this)) {
                writer.append(sql.toSql()).append(sql.getEndDelimiter()).append("\n\n");
            }
        }
    }

    @Override
    public int getNextChangeSetSequenceValue() throws LiquibaseException {
        if (lastChangeSetSequenceValue == null) {
            if (getConnection() == null) {
                lastChangeSetSequenceValue = Integer.valueOf(0);
            } else {
                lastChangeSetSequenceValue = Integer.valueOf(ExecutorService.getInstance().getExecutor(this).queryForInt(new GetNextChangeSetSequenceValueStatement()));
            }
        }

        int next = lastChangeSetSequenceValue.intValue() + 1;
        lastChangeSetSequenceValue = Integer.valueOf(next);
        return next;
    }

    @Override
    public List<DatabaseFunction> getDateFunctions() {
        return dateFunctions;
    }

    @Override
    public boolean isFunction(String string) {
        if (string.endsWith("()")) {
            return true;
        }
        for (DatabaseFunction function : getDateFunctions()) {
            if (function.toString().equalsIgnoreCase(string)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void resetInternalState() {
        this.ranChangeSetList = null;
        this.hasDatabaseChangeLogLockTable = false;
    }

    @Override
    public boolean supportsForeignKeyDisable() {
        return false;
    }

    @Override
    public boolean disableForeignKeyChecks() throws DatabaseException {
        throw new DatabaseException("ForeignKeyChecks Management not supported");
    }

    @Override
    public void enableForeignKeyChecks() throws DatabaseException {
        throw new DatabaseException("ForeignKeyChecks Management not supported");
    }

    @Override
    public boolean createsIndexesForForeignKeys() {
        return false;
    }

    @Override
    public int getDataTypeMaxParameters(String dataTypeName) {
        return 2;
    }

    public CatalogAndSchema getSchemaFromJdbcInfo(String rawCatalogName, String rawSchemaName) {
        return this.correctSchema(new CatalogAndSchema(rawCatalogName, rawSchemaName));
    }

    public String getJdbcCatalogName(CatalogAndSchema schema) {
        return schema.getCatalogName();
    }

    public String getJdbcSchemaName(CatalogAndSchema schema) {
        return schema.getSchemaName();
    }

    public final String getJdbcCatalogName(Schema schema) {
        if (schema == null) {
            return getJdbcCatalogName(getDefaultSchema());
        } else {
            return getJdbcCatalogName(new CatalogAndSchema(schema.getCatalogName(), schema.getName()));
        }
    }

    public final String getJdbcSchemaName(Schema schema) {
        if (schema == null) {
            return getJdbcSchemaName(getDefaultSchema());
        } else {
            return getJdbcSchemaName(new CatalogAndSchema(schema.getCatalogName(), schema.getName()));
        }
    }

    @Override
    public boolean dataTypeIsNotModifiable(final String typeName) {
        return unmodifiableDataTypes.contains(typeName.toLowerCase());
    }

    @Override
    public void setObjectQuotingStrategy(ObjectQuotingStrategy quotingStrategy) {
        this.quotingStrategy = quotingStrategy;
    }

    @Override
    public ObjectQuotingStrategy getObjectQuotingStrategy() {
        return this.quotingStrategy;
    }

    @Override
    public String generateDatabaseFunctionValue(final DatabaseFunction databaseFunction) {
        if (databaseFunction.getValue() == null) {
            return null;
        }
        if (isCurrentTimeFunction(databaseFunction.getValue().toLowerCase())) {
            return getCurrentDateTimeFunction();
        } else if (databaseFunction instanceof SequenceNextValueFunction) {
            if (sequenceNextValueFunction == null) {
                throw new RuntimeException(String.format("next value function for a sequence is not configured for database %s",
                        getDefaultDatabaseProductName()));
            }
            return String.format(sequenceNextValueFunction, escapeObjectName(databaseFunction.getValue(), Sequence.class));
        } else if (databaseFunction instanceof SequenceCurrentValueFunction) {
            if (sequenceCurrentValueFunction == null) {
                throw new RuntimeException(String.format("current value function for a sequence is not configured for database %s",
                        getDefaultDatabaseProductName()));
            }
            return String.format(sequenceCurrentValueFunction, escapeObjectName(databaseFunction.getValue(), Sequence.class));
        } else {
            return databaseFunction.getValue();
        }
    }

    private boolean isCurrentTimeFunction(String functionValue) {
        return functionValue.startsWith("current_timestamp")
                || functionValue.startsWith("current_datetime")
                || getCurrentDateTimeFunction().equalsIgnoreCase(functionValue);
    }

    @Override
    public String getCurrentDateTimeFunction() {
        return currentDateTimeFunction;
    }

 	@Override
    public void setOutputDefaultSchema(boolean outputDefaultSchema) {
		this.outputDefaultSchema = outputDefaultSchema;

 	}

    @Override
    public boolean isDefaultSchema(String catalog, String schema) {
        if (!supportsSchemas()) {
            return true;
        }

        if (!isDefaultCatalog(catalog)) {
            return false;
        }
        return schema == null || schema.equalsIgnoreCase(getDefaultSchemaName());
    }

    @Override
    public boolean isDefaultCatalog(String catalog) {
        if (!supportsCatalogs()) {
            return true;
        }

        return catalog == null || catalog.equalsIgnoreCase(getDefaultCatalogName());

    }

 	@Override
    public boolean getOutputDefaultSchema() {
 		return outputDefaultSchema;
 	}

    @Override
    public boolean getOutputDefaultCatalog() {
        return outputDefaultCatalog;
    }

    @Override
    public void setOutputDefaultCatalog(boolean outputDefaultCatalog) {
        this.outputDefaultCatalog = outputDefaultCatalog;
    }

    @Override
    public boolean supportsPrimaryKeyNames() {
        return true;
    }
}
