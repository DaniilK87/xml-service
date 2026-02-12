package com.xmlservice.handler;

import com.xmlservice.Main;

import java.util.List;

public class CommandHandler {

    public static void handleCommand(String[] args, String xmlUrl, String defaultJdbc,
                                     String defaultUser, String defaultPassword) {
        String command = args[0].toLowerCase();

        try {
            Main app = new Main(
                    xmlUrl,
                    getEnvOrDefault("JDBC_URL", defaultJdbc),
                    getEnvOrDefault("DB_USER", defaultUser),
                    getEnvOrDefault("DB_PASSWORD", defaultPassword)
            );

            switch (command) {
                case "tables" -> showTables(app);
                case "ddl" -> showDDL(app, args);
                case "create" -> createTables(app);
                case "update" -> updateTables(app, args);
                case "columns" -> showColumns(app, args);
                case "validate" -> validateStructure(app);
//                case "help" -> HelpPrinter.printHelp();
                default -> handleUnknownCommand(command);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void showTables(Main app) {
        System.out.println("Tables in XML: " + app.getParser().getTableNames());
    }

    private static void showDDL(Main app, String[] args) {
        if (args.length > 1) {
            System.out.println(app.getDbService().getTableDDL(args[1]));
        } else {
            System.out.println("Usage: java -jar app.jar ddl <table_name>");
        }
    }

    private static void createTables(Main app) {
        List<String> tables = app.getParser().getTableNames();
        app.getDbService().createTables(tables);
        System.out.println("Tables created/verified: " + tables);
    }

    private static void updateTables(Main app, String[] args) {
        app.getDbService().validateDatabaseStructure(app.getParser().getTableNames());

        if (args.length > 1) {
            updateSpecificTable(app, args[1]);
        } else {
            updateAllTables(app);
        }
    }

    private static void updateSpecificTable(Main app, String table) {
        switch (table) {
            case "currency" -> {
                app.getDbService().upsertCurrencies(app.getParser().parseCurrencies());
                System.out.println("Table currency updated successfully");
            }
            case "categories" -> {
                app.getDbService().upsertCategories(app.getParser().parseCategories());
                System.out.println("Table categories updated successfully");
            }
            case "offers" -> {
                app.getDbService().upsertOffers(app.getParser().parseOffers());
                System.out.println("Table offers updated successfully");
            }
            default -> System.out.println("Unknown table: " + table);
        }
    }

    private static void updateAllTables(Main app) {
        app.getDbService().upsertCurrencies(app.getParser().parseCurrencies());
        app.getDbService().upsertCategories(app.getParser().parseCategories());
        app.getDbService().upsertOffers(app.getParser().parseOffers());
        System.out.println("All tables updated successfully");
    }

    private static void showColumns(Main app, String[] args) {
        if (args.length > 1) {
            String table = args[1];
            System.out.println("Columns: " + app.getDbService().getColumnNames(table));
            for (String col : app.getDbService().getColumnNames(table)) {
                System.out.println("  - " + col +
                        (app.getDbService().isColumnId(table, col) ? " (PRIMARY KEY)" : ""));
            }
        }
    }

    private static void validateStructure(Main app) {
        app.getDbService().validateDatabaseStructure(app.getParser().getTableNames());
        System.out.println("Database structure is valid");
    }

    private static void handleUnknownCommand(String command) {
        System.out.println("Unknown command: " + command);
//        HelpPrinter.printHelp();
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
