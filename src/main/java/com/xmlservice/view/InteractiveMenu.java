package com.xmlservice.view;

import com.xmlservice.Main;

import java.util.List;
import java.util.Scanner;

public class InteractiveMenu {

    private final Scanner scanner;
    private final Main app;

    public InteractiveMenu(Scanner scanner, String xmlUrl, String defaultJdbc,
                           String defaultUser, String defaultPassword) {
        this.scanner = scanner;
        this.app = createApp(xmlUrl, defaultJdbc, defaultUser, defaultPassword);
    }

    private Main createApp(String xmlUrl, String defaultJdbc, String defaultUser, String defaultPassword) {
        return new Main(xmlUrl, defaultJdbc, defaultUser, defaultPassword);
    }

    public void run() {
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                if (!processChoice(choice)) {
                    break;
                }
            } catch (Exception e) {
                System.err.println("❌ Ошибка: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("                 МЕНЮ");
        System.out.println("═══════════════════════════════════════");
        System.out.println("1. Показать таблицы из XML");
        System.out.println("2. Показать DDL таблицы");
        System.out.println("3. Создать таблицы в БД");
        System.out.println("4. Обновить все данные");
        System.out.println("5. Обновить конкретную таблицу");
        System.out.println("6. Информация о колонках");
        System.out.println("7. Валидация структуры БД");
        System.out.println("0. Выход");
        System.out.print("Выберите опцию: ");
    }

    private boolean processChoice(String choice) {
        switch (choice) {
            case "1" -> showTables();
            case "2" -> showDDL();
            case "3" -> createTables();
            case "4" -> updateAllTables();
            case "5" -> updateSpecificTable();
            case "6" -> showColumns();
            case "7" -> validateStructure();
            case "0" -> {
                System.out.println("👋 До свидания!");
                return false;
            }
            default -> System.out.println("❌ Неверная опция");
        }
        return true;
    }

    private void showTables() {
        System.out.println("\n📋 Таблицы в XML: " + app.getParser().getTableNames());
    }

    private void showDDL() {
        System.out.print("Имя таблицы (currency/categories/offers): ");
        String table = scanner.nextLine().trim();
        System.out.println("\n📝 DDL для таблицы " + table + ":");
        System.out.println(app.getDbService().getTableDDL(table));
    }

    private void createTables() {
        List<String> tables = app.getParser().getTableNames();
        app.getDbService().createTables(tables);
        System.out.println("✅ Таблицы созданы/проверены: " + tables);
    }

    private void updateAllTables() {
        System.out.println("🔄 Обновление всех таблиц...");
        app.getDbService().validateDatabaseStructure(app.getParser().getTableNames());

        int currencyCount = app.getParser().parseCurrencies().size();
        app.getDbService().upsertCurrencies(app.getParser().parseCurrencies());

        int categoryCount = app.getParser().parseCategories().size();
        app.getDbService().upsertCategories(app.getParser().parseCategories());

        int offerCount = app.getParser().parseOffers().size();
        app.getDbService().upsertOffers(app.getParser().parseOffers());

        System.out.printf("✅ Обновление завершено: %d валют, %d категорий, %d предложений%n",
                currencyCount, categoryCount, offerCount);
    }

    private void updateSpecificTable() {
        System.out.print("Имя таблицы для обновления: ");
        String table = scanner.nextLine().trim();

        app.getDbService().validateDatabaseStructure(app.getParser().getTableNames());

        switch (table) {
            case "currency" -> {
                int count = app.getParser().parseCurrencies().size();
                app.getDbService().upsertCurrencies(app.getParser().parseCurrencies());
                System.out.printf("✅ Обновлено %d валют%n", count);
            }
            case "categories" -> {
                int count = app.getParser().parseCategories().size();
                app.getDbService().upsertCategories(app.getParser().parseCategories());
                System.out.printf("✅ Обновлено %d категорий%n", count);
            }
            case "offers" -> {
                int count = app.getParser().parseOffers().size();
                app.getDbService().upsertOffers(app.getParser().parseOffers());
                System.out.printf("✅ Обновлено %d предложений%n", count);
            }
            default -> System.out.println("❌ Неизвестная таблица: " + table);
        }
    }

    private void showColumns() {
        System.out.print("Имя таблицы: ");
        String table = scanner.nextLine().trim();
        System.out.println("\n📊 Колонки таблицы " + table + ":");
        for (String col : app.getDbService().getColumnNames(table)) {
            System.out.printf("  • %s %s%n",
                    col,
                    app.getDbService().isColumnId(table, col) ? "🔑 PRIMARY KEY" : "");
        }
    }

    private void validateStructure() {
        app.getDbService().validateDatabaseStructure(app.getParser().getTableNames());
        System.out.println("✅ Структура базы данных валидна");
    }
}
