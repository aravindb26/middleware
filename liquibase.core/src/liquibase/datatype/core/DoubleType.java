package liquibase.datatype.core;

import liquibase.database.Database;
import liquibase.database.core.DB2Database;
import liquibase.database.core.DerbyDatabase;
import liquibase.database.core.HsqlDatabase;
import liquibase.database.core.InformixDatabase;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;
import liquibase.servicelocator.PrioritizedService;

@DataTypeInfo(name="double", aliases = {"java.sql.Types.DOUBLE", "java.lang.Double"}, minParameters = 0, maxParameters = 2, priority = PrioritizedService.PRIORITY_DEFAULT)
public class DoubleType  extends LiquibaseDataType {
    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        if (database instanceof MSSQLDatabase) {
            return new DatabaseDataType("FLOAT");
        }
        if (database instanceof DB2Database || database instanceof DerbyDatabase || database instanceof HsqlDatabase || database instanceof MySQLDatabase) {
            return new DatabaseDataType("DOUBLE");
        }
        if (database instanceof OracleDatabase) {
            return new DatabaseDataType("FLOAT", 24);
        }
        if (database instanceof PostgresDatabase) {
            return new DatabaseDataType("DOUBLE PRECISION");
        }
        if (database instanceof InformixDatabase) {
            return new DatabaseDataType("DOUBLE PRECISION");
        }
        return super.toDatabaseDataType(database);
    }
}