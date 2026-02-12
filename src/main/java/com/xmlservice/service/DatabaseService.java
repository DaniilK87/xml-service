package com.xmlservice.service;

import com.xmlservice.data.CategoryData;
import com.xmlservice.data.CurrencyData;
import com.xmlservice.data.OfferData;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseService {

    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    private static final Map<String, List<String>> TABLE_COLUMNS = new HashMap<>();
    private static final Map<String, String> TABLE_ID_COLUMNS = new HashMap<>();

    static {
        TABLE_COLUMNS.put("currency", List.of("code", "rate"));
        TABLE_COLUMNS.put("categories", List.of("category_id", "name"));
        TABLE_COLUMNS.put("offers", List.of("vendorCode", "name", "category_id", "price", "currency_code"));

        TABLE_ID_COLUMNS.put("currency", "code");
        TABLE_ID_COLUMNS.put("categories", "category_id");
        TABLE_ID_COLUMNS.put("offers", "vendorCode");
    }

    public DatabaseService(String jdbcUrl, String dbUser, String dbPassword) {
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }

    /**
     * Создает sql для создания таблиц динамически из XML
     */
    public String getTableDDL(String tableName) {
        validateTableName(tableName);

        return switch (tableName) {
            case "currency" -> """
                CREATE TABLE IF NOT EXISTS currency (
                    code VARCHAR(10) PRIMARY KEY,
                    rate NUMERIC(18,6) NOT NULL
                );
                """;
            case "categories" -> """
                CREATE TABLE IF NOT EXISTS categories (
                    category_id VARCHAR(50) PRIMARY KEY,
                    name TEXT NOT NULL
                );
                """;
            case "offers" -> """
                CREATE TABLE IF NOT EXISTS offers (
                    vendorCode VARCHAR(100) PRIMARY KEY,
                    name TEXT NOT NULL,
                    category_id VARCHAR(50),
                    price NUMERIC(18,2),
                    currency_code VARCHAR(10),
                    FOREIGN KEY (currency_code) REFERENCES currency(code),
                    FOREIGN KEY (category_id) REFERENCES categories(category_id)
                );
                """;
            default -> throw new IllegalArgumentException("Unknown table: " + tableName);
        };
    }

    public void createTables(List<String> tableNames) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            for (String tableName : tableNames) {
                String ddl = getTableDDL(tableName);
                stmt.execute(ddl);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creating tables: " + e.getMessage(), e);
        }
    }

    public void validateDatabaseStructure(List<String> tableNames) {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            for (String tableName : tableNames) {
                try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                    List<String> existingColumns = new ArrayList<>();
                    while (rs.next()) {
                        existingColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                    }

                    List<String> expectedColumns = TABLE_COLUMNS.get(tableName);
                    if (!existingColumns.containsAll(expectedColumns)) {
                        throw new RuntimeException(
                                String.format("Структура таблицы %s изменилась. Ожидаемые колонки: %s, Фактические: %s",
                                        tableName, expectedColumns, existingColumns)
                        );
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка валидации структуры БД: " + e.getMessage(), e);
        }
    }

    public void upsertCurrencies(List<CurrencyData> currencies) {
        if (currencies.isEmpty()) return;

        String sql = """
            INSERT INTO currency (code, rate) VALUES (?, ?) 
            ON CONFLICT (code) DO UPDATE SET rate = EXCLUDED.rate
            """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (CurrencyData currency : currencies) {
                ps.setString(1, currency.getCode());
                ps.setBigDecimal(2, currency.getRate());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error upserting currencies: " + e.getMessage(), e);
        }
    }

    public void upsertCategories(List<CategoryData> categories) {
        if (categories.isEmpty()) return;

        String sql = """
            INSERT INTO categories (category_id, name) VALUES (?, ?) 
            ON CONFLICT (category_id) DO UPDATE SET name = EXCLUDED.name
            """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (CategoryData category : categories) {
                ps.setString(1, category.getId());
                ps.setString(2, category.getName());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error upserting categories: " + e.getMessage(), e);
        }
    }

    public void upsertOffers(List<OfferData> offers) {
        if (offers.isEmpty()) return;

        String sql = """
            INSERT INTO offers (vendorCode, name, category_id, price, currency_code) 
            VALUES (?, ?, ?, ?, ?) 
            ON CONFLICT (vendorCode) DO UPDATE SET 
                name = EXCLUDED.name, 
                category_id = EXCLUDED.category_id, 
                price = EXCLUDED.price, 
                currency_code = EXCLUDED.currency_code
            """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (OfferData offer : offers) {
                ps.setString(1, offer.getVendorCode());
                ps.setString(2, offer.getName());
                ps.setString(3, offer.getCategoryId());
                ps.setBigDecimal(4, offer.getPrice());
                ps.setString(5, offer.getCurrencyCode());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error upserting offers: " + e.getMessage(), e);
        }
    }

    // Дополнительные методы по желанию
    public List<String> getColumnNames(String tableName) {
        validateTableName(tableName);
        return TABLE_COLUMNS.get(tableName);
    }

    public boolean isColumnId(String tableName, String columnName) {
        validateTableName(tableName);
        return columnName.equals(TABLE_ID_COLUMNS.get(tableName));
    }

    private void validateTableName(String tableName) {
        if (!TABLE_COLUMNS.containsKey(tableName)) {
            throw new IllegalArgumentException("Unknown table: " + tableName);
        }
    }
}
