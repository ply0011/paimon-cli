package io.tapdata.paimon.cli;

import io.tapdata.paimon.cli.catalog.CatalogManager;
import io.tapdata.paimon.cli.config.ConfigHistoryManager;
import io.tapdata.paimon.cli.config.StorageConfig;
import io.tapdata.paimon.cli.service.DataQueryService;
import io.tapdata.paimon.cli.service.MetadataService;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.History;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.List;

/**
 * Paimon CLI Main Program
 */
public class PaimonCLI {

    private CatalogManager catalogManager;
    private MetadataService metadataService;
    private DataQueryService dataQueryService;
    private LineReader lineReader;
    private Terminal terminal;
    private ConfigHistoryManager configHistoryManager;

    public PaimonCLI() {
        try {
            // Create terminal and line reader with history support
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Create history with max size of 5
            History history = new DefaultHistory();

            lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(history)
                    .variable(LineReader.HISTORY_SIZE, 5)
                    .build();

            // Initialize config history manager
            configHistoryManager = new ConfigHistoryManager();
        } catch (IOException e) {
            System.err.println("Failed to initialize terminal: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    public static void main(String[] args) {
        PaimonCLI cli = new PaimonCLI();
        cli.run();
    }
    
    public void run() {
        printWelcome();

        // Initialize storage configuration
        if (!initializeStorage()) {
            System.err.println("Storage initialization failed, exiting");
            return;
        }

        // Enter command loop
        commandLoop();

        // Cleanup resources
        cleanup();
    }

    /**
     * Print welcome message
     */
    private void printWelcome() {
        System.out.println("========================================");
        System.out.println("    Welcome to Paimon CLI");
        System.out.println("========================================");
        System.out.println();
    }

    /**
     * Initialize storage configuration
     */
    private boolean initializeStorage() {
        try {
            // 打印配置文件路径
            System.out.println("Configuration file: " + configHistoryManager.getConfigFilePath());
            System.out.println();

            // 加载历史配置
            List<StorageConfig> history = configHistoryManager.loadHistory();

            StorageConfig config = null;

            if (!history.isEmpty()) {
                // 显示历史配置选项
                System.out.println("Recent configurations:");
                for (int i = 0; i < history.size(); i++) {
                    System.out.println(configHistoryManager.formatConfigForDisplay(i + 1, history.get(i)));
                }
                System.out.println("  " + (history.size() + 1) + ". Enter new configuration");

                String choice;
                try {
                    choice = lineReader.readLine("Select option (1-" + (history.size() + 1) + "): ").trim();
                } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
                    // User pressed Ctrl+C or Ctrl+D
                    System.out.println("\nOperation cancelled");
                    return false;
                } catch (Exception e) {
                    System.err.println("Failed to read input: " + e.getMessage());
                    return false;
                }

                try {
                    int selectedIndex = Integer.parseInt(choice);
                    if (selectedIndex >= 1 && selectedIndex <= history.size()) {
                        // 使用历史配置
                        config = history.get(selectedIndex - 1);
                        System.out.println("Using configuration: " + config);
                    } else if (selectedIndex == history.size() + 1) {
                        // 输入新配置
                        config = promptNewConfiguration();
                    } else {
                        System.err.println("Invalid option");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid option");
                    return false;
                }
            } else {
                // 没有历史配置，直接输入新配置
                config = promptNewConfiguration();
            }

            if (config == null) {
                return false;
            }

            System.out.println("\nConnecting to storage...");
            catalogManager = new CatalogManager(config);
            metadataService = new MetadataService(catalogManager);
            dataQueryService = new DataQueryService(catalogManager);

            System.out.println("Storage connected successfully!");
            System.out.println("Configuration: " + config);
            System.out.println();

            // 保存配置到历史记录
            configHistoryManager.saveConfig(config);

            return true;
        } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
            // User pressed Ctrl+C or Ctrl+D
            System.out.println("\nOperation cancelled");
            return false;
        } catch (Exception e) {
            System.err.println("Storage initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 提示用户输入新配置
     */
    private StorageConfig promptNewConfiguration() {
        try {
            System.out.println("\nPlease select storage type:");
            System.out.println("  1. Local (Local File System)");
            System.out.println("  2. S3");

            String choice;
            try {
                choice = lineReader.readLine("Enter option (1 or 2): ").trim();
            } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
                // User pressed Ctrl+C or Ctrl+D
                System.out.println("\nOperation cancelled");
                return null;
            } catch (Exception e) {
                System.err.println("Failed to read input: " + e.getMessage());
                return null;
            }

            if ("1".equals(choice)) {
                return configureLocal();
            } else if ("2".equals(choice)) {
                return configureS3();
            } else {
                System.err.println("Invalid option");
                return null;
            }
        } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
            // User pressed Ctrl+C or Ctrl+D during configuration
            System.out.println("\nOperation cancelled");
            return null;
        } catch (Exception e) {
            System.err.println("Failed to configure storage: " + e.getMessage());
            return null;
        }
    }

    /**
     * Configure local storage
     */
    private StorageConfig configureLocal() {
        try {
            String path = lineReader.readLine("Enter local storage path (e.g., /tmp/paimon): ").trim();
            return StorageConfig.createLocal(path);
        } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
            // User pressed Ctrl+C or Ctrl+D
            throw new RuntimeException("Operation cancelled", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input: " + e.getMessage(), e);
        }
    }

    /**
     * Configure S3 storage
     */
    private StorageConfig configureS3() {
        try {
            String s3Path = lineReader.readLine("Enter S3 path (e.g., s3://bucket-name/path): ").trim();
            String accessKey = lineReader.readLine("Enter Access Key (optional, press Enter to skip): ").trim();
            String secretKey = lineReader.readLine("Enter Secret Key (optional, press Enter to skip): ").trim();
            String endpoint = lineReader.readLine("Enter Endpoint (optional, press Enter to skip): ").trim();
            String region = lineReader.readLine("Enter Region (optional, press Enter to skip): ").trim();

            return StorageConfig.createS3(s3Path, accessKey, secretKey, endpoint, region);
        } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
            // User pressed Ctrl+C or Ctrl+D
            throw new RuntimeException("Operation cancelled", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input: " + e.getMessage(), e);
        }
    }

    /**
     * Command loop for interactive CLI
     */
    private void commandLoop() {
        printHelp();

        while (true) {
            try {
                String input = lineReader.readLine("paimon> ");

                if (input == null || input.trim().isEmpty()) {
                    continue;
                }

                input = input.trim();
                String[] parts = input.split("\\s+");
                String command = parts[0].toLowerCase();

                if ("exit".equals(command) || "quit".equals(command)) {
                    System.out.println("Goodbye!");
                    break;
                } else if ("help".equals(command)) {
                    printHelp();
                } else if ("show".equals(command)) {
                    handleShowCommand(parts);
                } else if ("desc".equals(command) || "describe".equals(command)) {
                    handleDescribeCommand(parts);
                } else if ("count".equals(command)) {
                    handleCountCommand(parts);
                } else if ("select".equals(command)) {
                    handleSelectCommand(parts);
                } else {
                    System.err.println("Unknown command: " + command + ", type 'help' for available commands");
                }
            } catch (org.jline.reader.UserInterruptException e) {
                // User pressed Ctrl+C
                System.out.println("\nGoodbye!");
                break;
            } catch (org.jline.reader.EndOfFileException e) {
                // User pressed Ctrl+D
                System.out.println("\nGoodbye!");
                break;
            } catch (Exception e) {
                System.err.println("Command execution failed: " + e.getMessage());
            }
        }
    }

    /**
     * Print help information
     */
    private void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  show databases                              - Show all databases");
        System.out.println("  show tables <database>                      - Show all tables in a database");
        System.out.println("  desc <database>.<table>                     - Show table structure");
        System.out.println("  count <database>.<table>                    - Count total rows in a table");
        System.out.println("  select <database>.<table> [limit|all] [where <filter>]");
        System.out.println("                                              - Query table data with optional limit and filter");
        System.out.println("                                                Use 'all' for pagination mode (5 rows/page)");
        System.out.println("  help                                        - Show help information");
        System.out.println("  exit/quit                                   - Exit the program");
        System.out.println();
        System.out.println("Query examples:");
        System.out.println("  select default.users 10                     - Show first 10 rows");
        System.out.println("  select default.users all                    - Show all rows with pagination (type 'it' to continue)");
        System.out.println("  select default.users 10 where age>18        - Show 10 rows where age > 18");
        System.out.println("  select default.users all where age>18       - Show all rows where age > 18 with pagination");
        System.out.println("  select default.users where age>=18 AND name=Alice");
        System.out.println();
    }

    /**
     * Handle show command
     */
    private void handleShowCommand(String[] parts) {
        if (parts.length < 2) {
            System.err.println("Usage: show databases or show tables <database>");
            return;
        }

        String subCommand = parts[1].toLowerCase();

        if ("databases".equals(subCommand)) {
            metadataService.showDatabases();
        } else if ("tables".equals(subCommand)) {
            if (parts.length < 3) {
                System.err.println("Usage: show tables <database>");
                return;
            }
            metadataService.showTables(parts[2]);
        } else {
            System.err.println("Unknown show subcommand: " + subCommand);
        }
    }

    /**
     * Handle describe command
     */
    private void handleDescribeCommand(String[] parts) {
        if (parts.length < 2) {
            System.err.println("Usage: desc <database>.<table>");
            return;
        }

        String[] dbTable = parts[1].split("\\.");
        if (dbTable.length != 2) {
            System.err.println("Invalid table name format, should be: <database>.<table>");
            return;
        }

        metadataService.describeTable(dbTable[0], dbTable[1]);
    }

    /**
     * Handle count command
     */
    private void handleCountCommand(String[] parts) {
        if (parts.length < 2) {
            System.err.println("Usage: count <database>.<table>");
            return;
        }

        String[] dbTable = parts[1].split("\\.");
        if (dbTable.length != 2) {
            System.err.println("Invalid table name format, should be: <database>.<table>");
            return;
        }

        dataQueryService.countTable(dbTable[0], dbTable[1]);
    }

    /**
     * Handle select command
     * Supports: select <database>.<table> [limit|all] [where <filter>]
     */
    private void handleSelectCommand(String[] parts) {
        if (parts.length < 2) {
            System.err.println("Usage: select <database>.<table> [limit|all] [where <filter>]");
            System.err.println("Example: select default.users 10");
            System.err.println("Example: select default.users all");
            System.err.println("Example: select default.users all where age>18");
            System.err.println("Example: select default.users 10 where age>18");
            System.err.println("Example: select default.users where age>=18 AND name=Alice");
            System.err.println("\nNote: Using 'all' enables pagination mode (5 rows per page, type 'it' to continue)");
            return;
        }

        String[] dbTable = parts[1].split("\\.");
        if (dbTable.length != 2) {
            System.err.println("Invalid table name format, should be: <database>.<table>");
            return;
        }

        int limit = 10; // Default limit
        String filter = null;
        boolean usePagination = false;

        // Parse remaining arguments
        int currentIndex = 2;

        // Check if next argument is a number (limit) or "all" keyword
        if (currentIndex < parts.length) {
            if ("all".equalsIgnoreCase(parts[currentIndex])) {
                usePagination = true;
                currentIndex++;
            } else {
                try {
                    limit = Integer.parseInt(parts[currentIndex]);
                    currentIndex++;
                } catch (NumberFormatException e) {
                    // Not a number, might be "where" keyword
                }
            }
        }

        // Check for "where" keyword
        if (currentIndex < parts.length && parts[currentIndex].equalsIgnoreCase("where")) {
            currentIndex++;
            // Collect remaining parts as filter expression
            StringBuilder filterBuilder = new StringBuilder();
            for (int i = currentIndex; i < parts.length; i++) {
                if (i > currentIndex) {
                    filterBuilder.append(" ");
                }
                filterBuilder.append(parts[i]);
            }
            filter = filterBuilder.toString();
        }

        // Execute query with or without pagination
        if (usePagination) {
            dataQueryService.selectTableWithPagination(dbTable[0], dbTable[1], filter);
        } else {
            dataQueryService.selectTable(dbTable[0], dbTable[1], limit, filter);
        }
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        try {
            if (catalogManager != null) {
                catalogManager.close();
            }
            if (terminal != null) {
                terminal.close();
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup resources: " + e.getMessage());
        }
    }
}

