package liquibase.datatype.core;

import liquibase.database.Database;
import liquibase.database.core.CacheDatabase;
import liquibase.database.core.FirebirdDatabase;
import liquibase.database.core.H2Database;
import liquibase.database.core.HsqlDatabase;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.MaxDBDatabase;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.core.SybaseASADatabase;
import liquibase.database.core.SybaseDatabase;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;
import liquibase.servicelocator.PrioritizedService;

@DataTypeInfo(name="blob", aliases = {"longblob", "longvarbinary", "java.sql.Types.BLOB", "java.sql.Types.LONGBLOB", "java.sql.Types.LONGVARBINARY", "java.sql.Types.VARBINARY", "varbinary"}, minParameters = 0, maxParameters = 0, priority = PrioritizedService.PRIORITY_DEFAULT)
public class BlobType extends LiquibaseDataType {
    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        if (database instanceof CacheDatabase || database instanceof H2Database || database instanceof HsqlDatabase) {
            return new DatabaseDataType("LONGVARBINARY");
        }
        if (database instanceof MaxDBDatabase) {
            return new DatabaseDataType("LONG BYTE");
        }
        if (database instanceof MSSQLDatabase) {
            String param = "MAX";
            if (this.getParameters().length > 0) {
                param = this.getParameters()[0].toString();
            }
            if (param.equals("2147483647")) {
                param = "MAX";
            }
            return new DatabaseDataType("VARBINARY", param);
        }
        if (database instanceof MySQLDatabase) {
            return new DatabaseDataType("LONGBLOB");
        }
        if (database instanceof PostgresDatabase) {
            return new DatabaseDataType("BYTEA");
        }
        if (database instanceof SybaseASADatabase) {
            return new DatabaseDataType("LONG BINARY");
        }
        if (database instanceof SybaseDatabase) {
            return new DatabaseDataType("IMAGE");
        }
        if (database instanceof OracleDatabase) {
            return new DatabaseDataType("BLOB");
        }

        if (database instanceof FirebirdDatabase) {
            return new DatabaseDataType("BLOB");
        }
        return super.toDatabaseDataType(database);
    }

    //sqlite
    //        } else if (columnTypeString.toLowerCase(Locale.ENGLISH).contains("blob") ||
//                columnTypeString.toLowerCase(Locale.ENGLISH).contains("binary")) {
//            type = new BlobType("BLOB");

}
