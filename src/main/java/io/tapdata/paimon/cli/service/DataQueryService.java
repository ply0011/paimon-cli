package io.tapdata.paimon.cli.service;

import io.tapdata.paimon.cli.catalog.CatalogManager;
import org.apache.paimon.data.InternalRow;
import org.apache.paimon.predicate.Predicate;
import org.apache.paimon.predicate.PredicateBuilder;
import org.apache.paimon.reader.RecordReader;
import org.apache.paimon.table.Table;
import org.apache.paimon.table.source.ReadBuilder;
import org.apache.paimon.table.source.Split;
import org.apache.paimon.table.source.TableRead;
import org.apache.paimon.types.DataField;
import org.apache.paimon.types.RowType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data Query Service
 */
public class DataQueryService {

    private final CatalogManager catalogManager;

    public DataQueryService(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
    }

    /**
     * Count total rows in a table
     */
    public void countTable(String database, String tableName) {
        try {
            if (!catalogManager.tableExists(database, tableName)) {
                System.err.println("Table does not exist: " + database + "." + tableName);
                return;
            }

            Table table = catalogManager.getTable(database, tableName);
            long count = countRows(table);

            System.out.println("\nTotal rows in table " + database + "." + tableName + ": " + count + "\n");
        } catch (Exception e) {
            System.err.println("Failed to count rows: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Query table data with limit support
     */
    public void selectTable(String database, String tableName, int limit) {
        selectTable(database, tableName, limit, null);
    }

    /**
     * Query table data with limit and filter support
     */
    public void selectTable(String database, String tableName, int limit, String filterExpression) {
        try {
            if (!catalogManager.tableExists(database, tableName)) {
                System.err.println("Table does not exist: " + database + "." + tableName);
                return;
            }

            Table table = catalogManager.getTable(database, tableName);
            RowType rowType = table.rowType();

            // Build read builder
            ReadBuilder readBuilder = table.newReadBuilder();

            // Parse and apply filter if provided
            if (filterExpression != null && !filterExpression.trim().isEmpty()) {
                try {
                    List<Predicate> predicates = parseFilter(filterExpression, rowType);
                    if (!predicates.isEmpty()) {
                        readBuilder = readBuilder.withFilter(predicates);
                        System.out.println("\nApplied filter: " + filterExpression);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse filter: " + e.getMessage());
                    System.err.println("Filter will be ignored. Continuing without filter...");
                }
            }

            // Calculate column widths for better formatting
            Map<Integer, Integer> columnWidths = calculateColumnWidths(table, rowType, readBuilder, limit);

            // Print table header
            System.out.println("\nTable: " + database + "." + tableName);
            System.out.println("====================");
            printHeader(rowType, columnWidths);

            // Read data
            List<Split> splits = readBuilder.newScan().plan().splits();
            TableRead tableRead = readBuilder.newRead();

            int rowCount = 0;
            boolean limitReached = false;

            for (Split split : splits) {
                if (limitReached) {
                    break;
                }

                try (RecordReader<InternalRow> reader = tableRead.createReader(split)) {
                    RecordReader.RecordIterator<InternalRow> iterator;
                    while ((iterator = reader.readBatch()) != null) {
                        InternalRow row;
                        while ((row = iterator.next()) != null) {
                            printRow(row, rowType, columnWidths);
                            rowCount++;

                            if (limit > 0 && rowCount >= limit) {
                                limitReached = true;
                                break;
                            }
                        }
                        iterator.releaseBatch();
                        if (limitReached) {
                            break;
                        }
                    }
                }
            }

            System.out.println("\nDisplayed " + rowCount + " row(s)\n");
        } catch (Exception e) {
            System.err.println("Failed to query data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Count total rows in a table
     */
    private long countRows(Table table) throws Exception {
        ReadBuilder readBuilder = table.newReadBuilder();
        List<Split> splits = readBuilder.newScan().plan().splits();
        TableRead tableRead = readBuilder.newRead();

        long count = 0;
        for (Split split : splits) {
            try (RecordReader<InternalRow> reader = tableRead.createReader(split)) {
                RecordReader.RecordIterator<InternalRow> iterator;
                while ((iterator = reader.readBatch()) != null) {
                    InternalRow row;
                    while ((row = iterator.next()) != null) {
                        count++;
                    }
                    iterator.releaseBatch();
                }
            }
        }

        return count;
    }

    /**
     * Calculate optimal column widths based on actual data
     */
    private Map<Integer, Integer> calculateColumnWidths(Table table, RowType rowType, ReadBuilder readBuilder, int limit) {
        Map<Integer, Integer> widths = new HashMap<>();
        List<DataField> fields = rowType.getFields();

        // Initialize with header widths
        for (int i = 0; i < fields.size(); i++) {
            widths.put(i, Math.max(fields.get(i).name().length(), 10));
        }

        try {
            // Sample data to calculate widths (limit to first 100 rows for performance)
            List<Split> splits = readBuilder.newScan().plan().splits();
            TableRead tableRead = readBuilder.newRead();

            int sampleCount = 0;
            int maxSample = Math.min(limit > 0 ? limit : 100, 100);
            boolean done = false;

            for (Split split : splits) {
                if (done) break;

                try (RecordReader<InternalRow> reader = tableRead.createReader(split)) {
                    RecordReader.RecordIterator<InternalRow> iterator;
                    while ((iterator = reader.readBatch()) != null) {
                        InternalRow row;
                        while ((row = iterator.next()) != null) {
                            for (int i = 0; i < fields.size(); i++) {
                                Object value = extractValue(row, i, fields.get(i));
                                String valueStr = value == null ? "NULL" : value.toString();
                                int currentWidth = widths.get(i);
                                widths.put(i, Math.max(currentWidth, Math.min(valueStr.length(), 50)));
                            }
                            sampleCount++;
                            if (sampleCount >= maxSample) {
                                done = true;
                                break;
                            }
                        }
                        iterator.releaseBatch();
                        if (done) break;
                    }
                }
            }
        } catch (Exception e) {
            // If sampling fails, use default widths
        }

        return widths;
    }

    /**
     * Print table header with dynamic column widths
     */
    private void printHeader(RowType rowType, Map<Integer, Integer> columnWidths) {
        List<DataField> fields = rowType.getFields();

        StringBuilder line = new StringBuilder();
        StringBuilder separator = new StringBuilder();

        for (int i = 0; i < fields.size(); i++) {
            int width = columnWidths.getOrDefault(i, 20);
            String header = fields.get(i).name();

            line.append(String.format("%-" + width + "s", header));
            if (i < fields.size() - 1) {
                line.append(" | ");
            }

            separator.append("-".repeat(width));
            if (i < fields.size() - 1) {
                separator.append("-+-");
            }
        }

        System.out.println(line);
        System.out.println(separator);
    }

    /**
     * Print data row with dynamic column widths
     */
    private void printRow(InternalRow row, RowType rowType, Map<Integer, Integer> columnWidths) {
        StringBuilder line = new StringBuilder();
        List<DataField> fields = rowType.getFields();

        for (int i = 0; i < fields.size(); i++) {
            int width = columnWidths.getOrDefault(i, 20);
            Object value = extractValue(row, i, fields.get(i));
            String valueStr = value == null ? "NULL" : value.toString();

            // Truncate if too long
            if (valueStr.length() > width) {
                valueStr = valueStr.substring(0, width - 3) + "...";
            }

            line.append(String.format("%-" + width + "s", valueStr));
            if (i < fields.size() - 1) {
                line.append(" | ");
            }
        }

        System.out.println(line);
    }

    /**
     * Parse filter expression and build predicates
     * Supports simple filters like: field=value, field>value, field<value, field>=value, field<=value, field!=value
     * Multiple conditions can be combined with AND
     */
    private List<Predicate> parseFilter(String filterExpression, RowType rowType) {
        List<Predicate> predicates = new ArrayList<>();
        PredicateBuilder builder = new PredicateBuilder(rowType);

        // Split by AND (case insensitive)
        String[] conditions = filterExpression.split("(?i)\\s+AND\\s+");

        for (String condition : conditions) {
            condition = condition.trim();
            if (condition.isEmpty()) {
                continue;
            }

            Predicate predicate = parseCondition(condition, rowType, builder);
            if (predicate != null) {
                predicates.add(predicate);
            }
        }

        return predicates;
    }

    /**
     * Parse a single condition
     */
    private Predicate parseCondition(String condition, RowType rowType, PredicateBuilder builder) {
        // Pattern for: field operator value
        Pattern pattern = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*(>=|<=|!=|=|>|<)\\s*(.+)\\s*$");
        Matcher matcher = pattern.matcher(condition);

        if (!matcher.matches()) {
            System.err.println("Invalid filter condition: " + condition);
            return null;
        }

        String fieldName = matcher.group(1);
        String operator = matcher.group(2);
        String valueStr = matcher.group(3).trim();

        // Remove quotes if present
        if ((valueStr.startsWith("'") && valueStr.endsWith("'")) ||
                (valueStr.startsWith("\"") && valueStr.endsWith("\""))) {
            valueStr = valueStr.substring(1, valueStr.length() - 1);
        }

        // Find field index
        List<DataField> fields = rowType.getFields();
        int fieldIndex = -1;
        DataField field = null;

        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).name().equalsIgnoreCase(fieldName)) {
                fieldIndex = i;
                field = fields.get(i);
                break;
            }
        }

        if (fieldIndex == -1) {
            System.err.println("Field not found: " + fieldName);
            return null;
        }

        try {
            // Build predicate based on operator and type
            Object value = parseValue(valueStr, field);

            switch (operator) {
                case "=":
                    return builder.equal(fieldIndex, value);
                case "!=":
                    return builder.notEqual(fieldIndex, value);
                case ">":
                    return builder.greaterThan(fieldIndex, value);
                case ">=":
                    return builder.greaterOrEqual(fieldIndex, value);
                case "<":
                    return builder.lessThan(fieldIndex, value);
                case "<=":
                    return builder.lessOrEqual(fieldIndex, value);
                default:
                    System.err.println("Unsupported operator: " + operator);
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Failed to parse value for field " + fieldName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse value string to appropriate type
     */
    private Object parseValue(String valueStr, DataField field) {
        switch (field.type().getTypeRoot()) {
            case BOOLEAN:
                return Boolean.parseBoolean(valueStr);
            case TINYINT:
                return Byte.parseByte(valueStr);
            case SMALLINT:
                return Short.parseShort(valueStr);
            case INTEGER:
                return Integer.parseInt(valueStr);
            case BIGINT:
                return Long.parseLong(valueStr);
            case FLOAT:
                return Float.parseFloat(valueStr);
            case DOUBLE:
                return Double.parseDouble(valueStr);
            case CHAR:
            case VARCHAR:
                return valueStr;
            default:
                return valueStr;
        }
    }

    /**
     * Extract value from InternalRow
     */
    private Object extractValue(InternalRow row, int pos, DataField field) {
        if (row.isNullAt(pos)) {
            return null;
        }

        switch (field.type().getTypeRoot()) {
            case BOOLEAN:
                return row.getBoolean(pos);
            case TINYINT:
                return row.getByte(pos);
            case SMALLINT:
                return row.getShort(pos);
            case INTEGER:
                return row.getInt(pos);
            case BIGINT:
                return row.getLong(pos);
            case FLOAT:
                return row.getFloat(pos);
            case DOUBLE:
                return row.getDouble(pos);
            case CHAR:
            case VARCHAR:
                return row.getString(pos).toString();
            case DECIMAL:
                // For DECIMAL type, use default precision
                return row.getDecimal(pos, 10, 2);
            case DATE:
                return row.getInt(pos); // Date as int
            case TIMESTAMP_WITHOUT_TIME_ZONE:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                return row.getTimestamp(pos, 3);
            default:
                // For other types, try to convert to string
                try {
                    return row.getString(pos).toString();
                } catch (Exception e) {
                    return "N/A";
                }
        }
    }
}

