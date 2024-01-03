package liquibase.structure.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import liquibase.CatalogAndSchema;
import liquibase.structure.AbstractDatabaseObject;
import liquibase.structure.DatabaseObject;
import liquibase.util.StringUtils;

public class Schema extends AbstractDatabaseObject {

    @Override
    public DatabaseObject[] getContainingObjects() {
        return null;
    }

    public Schema() {
        setAttribute("objects",  new HashMap<Class<? extends DatabaseObject>, Set<DatabaseObject>>());
    }

    public Schema(String catalog, String schemaName) {
        catalog = StringUtils.trimToNull(catalog);
        schemaName = StringUtils.trimToNull(schemaName);

        setAttribute("name", schemaName);
        setAttribute("catalog", new Catalog(catalog));
        setAttribute("objects",  new HashMap<Class<? extends DatabaseObject>, Set<DatabaseObject>>());
    }
    
    public Schema(Catalog catalog, String name) {
        this(catalog.getName(), name);
    }

    @Override
    public String getName() {
        return getAttribute("name", String.class);
    }


    @Override
    public Schema setName(String name) {
        setAttribute("name", name);
        return this;
    }

    @Override
    public Schema getSchema() {
        return this;
    }

    public Catalog getCatalog() {
        return getAttribute("catalog", Catalog.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Schema schema = (Schema) o;

        if (getCatalog() != null ? !getCatalog().equals(schema.getCatalog()) : schema.getCatalog() != null) return false;
        if (getName() != null ? !getName().equals(schema.getName()) : schema.getName() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getCatalog() != null ? getCatalog().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }

    public String getCatalogName() {
        if (getCatalog() == null) {
            return null;
        }
        return getCatalog().getName();
    }
    
    @Override
    public String toString() {
        String catalogName = getCatalogName();
        if (catalogName == null) {
            return getName();
        } else {
            return catalogName +"."+getName();
        }
    }

    public CatalogAndSchema toCatalogAndSchema() {
        return new CatalogAndSchema(getCatalogName(), getName());
    }

    protected Map<Class<? extends DatabaseObject>, Set<DatabaseObject>> getObjects() {
        return getAttribute("objects", Map.class);
    }

    public <DatabaseObjectType extends DatabaseObject> List<DatabaseObjectType> getDatabaseObjects(Class<DatabaseObjectType> type) {
        Set<DatabaseObjectType> databaseObjects = (Set<DatabaseObjectType>) getObjects().get(type);
        if (databaseObjects == null) {
            return new ArrayList<DatabaseObjectType>();
        }
        return new ArrayList<DatabaseObjectType>(databaseObjects);
    }

    public void addDatabaseObject(DatabaseObject databaseObject) {
        if (databaseObject == null) {
            return;
        }
        Set<DatabaseObject> objects = this.getObjects().get(databaseObject.getClass());
        if (objects == null) {
            objects = new HashSet<DatabaseObject>();
            this.getObjects().put(databaseObject.getClass(), objects);
        }
        objects.add(databaseObject);

    }
}
