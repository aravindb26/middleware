package liquibase.executor.jvm;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import liquibase.database.DatabaseConnection;
import liquibase.database.PreparedStatementFactory;
import liquibase.database.core.OracleDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.executor.AbstractExecutor;
import liquibase.executor.Executor;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import liquibase.sql.visitor.SqlVisitor;
import liquibase.statement.CallableSqlStatement;
import liquibase.statement.ExecutablePreparedStatement;
import liquibase.statement.SqlStatement;
import liquibase.util.JdbcUtils;
import liquibase.util.StringUtils;

/**
 * Class to simplify execution of SqlStatements.  Based heavily on <a href="http://static.springframework.org/spring/docs/2.0.x/reference/jdbc.html">Spring's JdbcTemplate</a>.
 * <br><br>
 * <b>Note: This class is currently intended for Liquibase-internal use only and may change without notice in the future</b>
 */
@SuppressWarnings({"unchecked"})
public class JdbcExecutor extends AbstractExecutor implements Executor {

    private final Logger log = LogFactory.getLogger();

    @Override
    public boolean updatesDatabase() {
        return true;
    }

    @Override
    public String[] applyVisitors(SqlStatement statement, List<SqlVisitor> sqlVisitors) throws DatabaseException {
        return super.applyVisitors(statement, sqlVisitors);
    }

    //-------------------------------------------------------------------------
    // Methods dealing with static SQL (java.sql.Statement)
    //-------------------------------------------------------------------------

    public Object execute(StatementCallback action, List<SqlVisitor> sqlVisitors) throws DatabaseException {
        DatabaseConnection con = database.getConnection();
        Statement stmt = null;
        try {
            stmt = ((JdbcConnection) con).getUnderlyingConnection().createStatement();
            Statement stmtToUse = stmt;

            return action.doInStatement(stmtToUse);
        }
        catch (SQLException ex) {
            // Release Connection early, to avoid potential connection pool deadlock
            // in the case when the exception translator hasn't been initialized yet.
            JdbcUtils.closeStatement(stmt);
            stmt = null;
            throw new DatabaseException("Error executing SQL " + StringUtils.join(applyVisitors(action.getStatement(), sqlVisitors), "; on "+ con.getURL())+": "+ex.getMessage(), ex);
        }
        finally {
            JdbcUtils.closeStatement(stmt);
        }
    }

    @Override
    public void execute(final SqlStatement sql) throws DatabaseException {
        execute(sql, new ArrayList<SqlVisitor>());
    }

    @Override
    public void execute(final SqlStatement sql, final List<SqlVisitor> sqlVisitors) throws DatabaseException {
        if (sql instanceof ExecutablePreparedStatement) {
            ((ExecutablePreparedStatement) sql).execute(new PreparedStatementFactory((JdbcConnection)database.getConnection()));
            return;
        }

        boolean isOracleDatabase = database instanceof OracleDatabase;
        class ExecuteStatementCallback implements StatementCallback {
            @Override
            public Object doInStatement(Statement stmt) throws SQLException, DatabaseException {
                for (String statement : applyVisitors(sql, sqlVisitors)) {
                    if (isOracleDatabase) {
                        statement = statement.replaceFirst("/\\s*/\\s*$", ""); //remove duplicated /'s
                    }

                    log.debug("Executing EXECUTE database command: "+statement);
                    if (statement.contains("?")) {
                        stmt.setEscapeProcessing(false);
                    }
                    stmt.execute(statement);
                }
                return null;
            }

            @Override
            public SqlStatement getStatement() {
                return sql;
            }
        }
        execute(new ExecuteStatementCallback(), sqlVisitors);
    }


    public Object query(final SqlStatement sql, final ResultSetExtractor rse) throws DatabaseException {
        return query(sql, rse, new ArrayList<SqlVisitor>());
    }

    public Object query(final SqlStatement sql, final ResultSetExtractor rse, final List<SqlVisitor> sqlVisitors) throws DatabaseException {
        if (sql instanceof CallableSqlStatement) {
            throw new DatabaseException("Direct query using CallableSqlStatement not currently implemented");
        }

        class QueryStatementCallback implements StatementCallback {
            @Override
            public Object doInStatement(Statement stmt) throws SQLException, DatabaseException {
                ResultSet rs = null;
                try {
                    String[] sqlToExecute = applyVisitors(sql, sqlVisitors);

                    if (sqlToExecute.length != 1) {
                        throw new DatabaseException("Can only query with statements that return one sql statement");
                    }
                    log.debug("Executing QUERY database command: "+sqlToExecute[0]);

                    rs = stmt.executeQuery(sqlToExecute[0]);
                    ResultSet rsToUse = rs;
                    return rse.extractData(rsToUse);
                }
                finally {
                    JdbcUtils.closeResultSet(rs);
                }
            }


            @Override
            public SqlStatement getStatement() {
                return sql;
            }
        }
        return execute(new QueryStatementCallback(), sqlVisitors);
    }

    public List query(SqlStatement sql, RowMapper rowMapper) throws DatabaseException {
        return query(sql, rowMapper, new ArrayList());
    }

    public List query(SqlStatement sql, RowMapper rowMapper, List<SqlVisitor> sqlVisitors) throws DatabaseException {
        return (List) query(sql, new RowMapperResultSetExtractor(rowMapper), sqlVisitors);
    }

    public Object queryForObject(SqlStatement sql, RowMapper rowMapper) throws DatabaseException {
        return queryForObject(sql, rowMapper, new ArrayList());
    }

    public Object queryForObject(SqlStatement sql, RowMapper rowMapper, List<SqlVisitor> sqlVisitors) throws DatabaseException {
        List results = query(sql, rowMapper, sqlVisitors);
        return JdbcUtils.requiredSingleResult(results);
    }

    @Override
    public Object queryForObject(SqlStatement sql, Class requiredType) throws DatabaseException {
        return queryForObject(sql, requiredType, new ArrayList());
    }

    @Override
    public Object queryForObject(SqlStatement sql, Class requiredType, List<SqlVisitor> sqlVisitors) throws DatabaseException {
        return queryForObject(sql, getSingleColumnRowMapper(requiredType), sqlVisitors);
    }

    @Override
    public long queryForLong(SqlStatement sql) throws DatabaseException {
        return queryForLong(sql, new ArrayList());
    }

    @Override
    public long queryForLong(SqlStatement sql, List<SqlVisitor> sqlVisitors) throws DatabaseException {
        Number number = (Number) queryForObject(sql, Long.class, sqlVisitors);
        return (number != null ? number.longValue() : 0);
    }

    @Override
    public int queryForInt(SqlStatement sql) throws DatabaseException {
        return queryForInt(sql, new ArrayList());
    }

    @Override
    public int queryForInt(SqlStatement sql, List<SqlVisitor> sqlVisitors) throws DatabaseException {
        Number number = (Number) queryForObject(sql, Integer.class, sqlVisitors);
        return (number != null ? number.intValue() : 0);
    }

    @Override
    public List queryForList(SqlStatement sql, Class elementType) throws DatabaseException {
        return queryForList(sql, elementType, new ArrayList());
    }

    @Override
    public List queryForList(SqlStatement sql, Class elementType, List<SqlVisitor> sqlVisitors) throws DatabaseException {
        return query(sql, getSingleColumnRowMapper(elementType), sqlVisitors);
    }

    @Override
    public List<Map> queryForList(SqlStatement sql) throws DatabaseException {
        return queryForList(sql, new ArrayList());
    }

    @Override
    public List<Map> queryForList(SqlStatement sql, List<SqlVisitor> sqlVisitors) throws DatabaseException {
        //noinspection unchecked
        return query(sql, getColumnMapRowMapper(), sqlVisitors);
    }

    @Override
    public int update(final SqlStatement sql) throws DatabaseException {
        return update(sql, new ArrayList());
    }

    @Override
    public int update(final SqlStatement sql, final List<SqlVisitor> sqlVisitors) throws DatabaseException {
        if (sql instanceof CallableSqlStatement) {
            throw new DatabaseException("Direct update using CallableSqlStatement not currently implemented");
        }

        class UpdateStatementCallback implements StatementCallback {
            @Override
            public Object doInStatement(Statement stmt) throws SQLException, DatabaseException {
                String[] sqlToExecute = applyVisitors(sql, sqlVisitors);
                if (sqlToExecute.length != 1) {
                    throw new DatabaseException("Cannot call update on Statement that returns back multiple Sql objects");
                }
                log.debug("Executing UPDATE database command: "+sqlToExecute[0]);
                return stmt.executeUpdate(sqlToExecute[0]);
            }


            @Override
            public SqlStatement getStatement() {
                return sql;
            }
        }
        return (Integer) execute(new UpdateStatementCallback(), sqlVisitors);
    }

    /**
     * Create a new RowMapper for reading columns as key-value pairs.
     *
     * @return the RowMapper to use
     * @see ColumnMapRowMapper
     */
    protected RowMapper getColumnMapRowMapper() {
        return new ColumnMapRowMapper();
    }

    /**
     * Create a new RowMapper for reading result objects from a single column.
     *
     * @param requiredType the type that each result object is expected to match
     * @return the RowMapper to use
     * @see SingleColumnRowMapper
     */
    protected RowMapper getSingleColumnRowMapper(Class requiredType) {
        return new SingleColumnRowMapper(requiredType);
    }

    @Override
    public void comment(String message) throws DatabaseException {
        LogFactory.getLogger().debug(message);
    }

    /**
     * Adapter to enable use of a RowCallbackHandler inside a ResultSetExtractor.
     * <p>Uses a regular ResultSet, so we have to be careful when using it:
     * We don't use it for navigating since this could lead to unpredictable consequences.
     */
    private static class RowCallbackHandlerResultSetExtractor implements ResultSetExtractor {

        private final RowCallbackHandler rch;

        public RowCallbackHandlerResultSetExtractor(RowCallbackHandler rch) {
            this.rch = rch;
        }

        @Override
        public Object extractData(ResultSet rs) throws SQLException {
            while (rs.next()) {
                this.rch.processRow(rs);
            }
            return null;
        }
    }
}
