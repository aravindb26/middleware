package liquibase.datatype.core;

import liquibase.database.Database;
import liquibase.database.core.DB2Database;
import liquibase.database.core.DerbyDatabase;
import liquibase.database.core.FirebirdDatabase;
import liquibase.database.core.HsqlDatabase;
import liquibase.database.core.InformixDatabase;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;
import liquibase.servicelocator.PrioritizedService;

@DataTypeInfo(name="bigint", aliases = {"java.sql.Types.BIGINT", "java.math.BigInteger", "java.lang.Long", "integer8", "bigserial"}, minParameters = 0, maxParameters = 1, priority = PrioritizedService.PRIORITY_DEFAULT)
public class BigIntType extends LiquibaseDataType {

    private boolean autoIncrement;

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        if (database instanceof InformixDatabase) {
            if (isAutoIncrement()) {
                return new DatabaseDataType("SERIAL8");
            } else {
                return new DatabaseDataType("INT8");
            }
        }
        if (database instanceof OracleDatabase) {
            return new DatabaseDataType("NUMBER", 38,0);
        }
        if (database instanceof DB2Database || database instanceof DerbyDatabase
                || database instanceof MSSQLDatabase || database instanceof HsqlDatabase || database instanceof FirebirdDatabase) {
            return new DatabaseDataType("BIGINT");
        }
        if (database instanceof PostgresDatabase) {
            if (isAutoIncrement()) {
                return new DatabaseDataType("BIGSERIAL");
            }
        }
        return super.toDatabaseDataType(database);
    }
}
