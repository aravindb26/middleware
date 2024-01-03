package liquibase.datatype.core;

import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;
import liquibase.servicelocator.PrioritizedService;

@DataTypeInfo(name="decimal", aliases = "java.sql.Types.DECIMAL" , minParameters = 0, maxParameters = 2, priority = PrioritizedService.PRIORITY_DEFAULT)
public class DecimalType  extends LiquibaseDataType {

    private boolean autoIncrement;

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }



}
