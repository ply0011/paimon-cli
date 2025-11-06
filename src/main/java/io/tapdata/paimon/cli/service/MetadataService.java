package io.tapdata.paimon.cli.service;

import io.tapdata.paimon.cli.catalog.CatalogManager;
import org.apache.paimon.table.Table;
import org.apache.paimon.types.DataField;
import org.apache.paimon.types.RowType;

import java.util.List;

/**
 * Metadata Query Service
 */
public class MetadataService {

    private final CatalogManager catalogManager;

    public MetadataService(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    /**
     * Show all databases
     */
    public void showDatabases() {
        try {
            List<String> databases = catalogManager.listDatabases();
            System.out.println("\nDatabase List:");
            System.out.println("====================");
            if (databases.isEmpty()) {
                System.out.println("(No databases)");
            } else {
                for (String db : databases) {
                    System.out.println("  - " + db);
                }
            }
            System.out.println("Total: " + databases.size() + " database(s)\n");
        } catch (Exception e) {
            System.err.println("Failed to get database list: " + e.getMessage());
        }
    }

    /**
     * Show all tables in a database
     */
    public void showTables(String database) {
        try {
            if (!catalogManager.databaseExists(database)) {
                System.err.println("Database does not exist: " + database);
                return;
            }

            List<String> tables = catalogManager.listTables(database);
            System.out.println("\nTable List in Database '" + database + "':");
            System.out.println("====================");
            if (tables.isEmpty()) {
                System.out.println("(No tables)");
            } else {
                for (String table : tables) {
                    System.out.println("  - " + table);
                }
            }
            System.out.println("Total: " + tables.size() + " table(s)\n");
        } catch (Exception e) {
            System.err.println("Failed to get table list: " + e.getMessage());
        }
    }

    /**
     * Show table structure
     */
    public void describeTable(String database, String tableName) {
        try {
            if (!catalogManager.tableExists(database, tableName)) {
                System.err.println("Table does not exist: " + database + "." + tableName);
                return;
            }

            Table table = catalogManager.getTable(database, tableName);
            RowType rowType = table.rowType();

            System.out.println("\nTable: " + database + "." + tableName);
            System.out.println("====================");
            System.out.println("Field Information:");
            System.out.println(String.format("%-30s %-30s %-10s", "Field Name", "Type", "Nullable"));
            System.out.println("--------------------------------------------------------------------------------");

            for (DataField field : rowType.getFields()) {
                String fieldName = field.name();
                String fieldType = field.type().toString();
                String nullable = field.type().isNullable() ? "YES" : "NO";
                System.out.println(String.format("%-30s %-30s %-10s", fieldName, fieldType, nullable));
            }

            // Show primary keys
            List<String> primaryKeys = table.primaryKeys();
            if (!primaryKeys.isEmpty()) {
                System.out.println("\nPrimary Keys: " + String.join(", ", primaryKeys));
            }

            // Show partition keys
            List<String> partitionKeys = table.partitionKeys();
            if (!partitionKeys.isEmpty()) {
                System.out.println("Partition Keys: " + String.join(", ", partitionKeys));
            }

            System.out.println();
        } catch (Exception e) {
            System.err.println("Failed to get table structure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

