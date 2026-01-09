package com.example.demo.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты структуры базы данных
 * Проверяет наличие всех таблиц, колонок и их свойства
 */
@DisplayName("Тесты структуры БД")
public class DatabaseStructureTest extends BaseDatabaseTest {

    @Test
    @DisplayName("Проверка существования всех основных таблиц")
    public void testAllTablesExist() {
        assertTrue(tableExists("users"), "Таблица users должна существовать");
        assertTrue(tableExists("tasks"), "Таблица tasks должна существовать");
        assertTrue(tableExists("workshops"), "Таблица workshops должна существовать");
        assertTrue(tableExists("equipment"), "Таблица equipment должна существовать");
        assertTrue(tableExists("excursions"), "Таблица excursions должна существовать");
        assertTrue(tableExists("excursion_routes"), "Таблица excursion_routes должна существовать");
        assertTrue(tableExists("golden_tickets"), "Таблица golden_tickets должна существовать");
        assertTrue(tableExists("notifications"), "Таблица notifications должна существовать");
        assertTrue(tableExists("workshop_user"), "Таблица workshop_user должна существовать");
    }

    @Test
    @DisplayName("Проверка обязательных полей таблицы users")
    public void testUsersTableStructure() {
        assertTrue(columnExists("users", "id"), "Колонка id должна существовать");
        assertTrue(columnExists("users", "username"), "Колонка username должна существовать");
        assertTrue(columnExists("users", "password"), "Колонка password должна существовать");
        assertTrue(columnExists("users", "role"), "Колонка role должна существовать");
        assertTrue(columnExists("users", "created_at"), "Колонка created_at должна существовать");

        // Проверка NOT NULL
        assertFalse(isColumnNullable("users", "username"), "username должен быть NOT NULL");
        assertFalse(isColumnNullable("users", "password"), "password должен быть NOT NULL");
        assertFalse(isColumnNullable("users", "role"), "role должен быть NOT NULL");
    }

    @Test
    @DisplayName("Проверка структуры таблицы equipment")
    public void testEquipmentTableStructure() {
        assertTrue(columnExists("equipment", "id"));
        assertTrue(columnExists("equipment", "name"));
        assertTrue(columnExists("equipment", "status"));
        assertTrue(columnExists("equipment", "health"));
        assertTrue(columnExists("equipment", "workshop_id"));

        // NOT NULL проверки
        assertFalse(isColumnNullable("equipment", "name"), "name должен быть NOT NULL");
        assertFalse(isColumnNullable("equipment", "status"), "status должен быть NOT NULL");
        assertFalse(isColumnNullable("equipment", "health"), "health должен быть NOT NULL");
        assertFalse(isColumnNullable("equipment", "workshop_id"), "workshop_id должен быть NOT NULL (FK)");

        // Nullable поля
        assertTrue(isColumnNullable("equipment", "description"), "description должен быть nullable");
        assertTrue(isColumnNullable("equipment", "temperature"), "temperature должен быть nullable");
    }

    @Test
    @DisplayName("Проверка структуры таблицы golden_tickets")
    public void testGoldenTicketsTableStructure() {
        assertTrue(columnExists("golden_tickets", "ticket_number"));
        assertTrue(columnExists("golden_tickets", "status"));
        assertTrue(columnExists("golden_tickets", "excursion_id"));
        assertTrue(columnExists("golden_tickets", "generated_at"));

        assertFalse(isColumnNullable("golden_tickets", "ticket_number"), "ticket_number NOT NULL");
        assertFalse(isColumnNullable("golden_tickets", "status"), "status NOT NULL");
        assertTrue(isColumnNullable("golden_tickets", "excursion_id"), "excursion_id nullable");
    }

    @Test
    @DisplayName("Проверка структуры таблицы excursions")
    public void testExcursionsTableStructure() {
        assertTrue(columnExists("excursions", "name"));
        assertTrue(columnExists("excursions", "start_time"));
        assertTrue(columnExists("excursions", "participants_count"));
        assertTrue(columnExists("excursions", "guide_id"));
        assertTrue(columnExists("excursions", "status"));

        assertFalse(isColumnNullable("excursions", "name"));
        assertFalse(isColumnNullable("excursions", "start_time"));
        assertFalse(isColumnNullable("excursions", "guide_id"));
    }

    @Test
    @DisplayName("Проверка первичных ключей")
    public void testPrimaryKeys() {
        String sql = """
            SELECT COUNT(*) FROM information_schema.table_constraints 
            WHERE constraint_type = 'PRIMARY KEY' 
            AND table_name = ?
            """;

        assertTrue(countRows(sql, "users") > 0, "users должна иметь PK");
        assertTrue(countRows(sql, "tasks") > 0, "tasks должна иметь PK");
        assertTrue(countRows(sql, "equipment") > 0, "equipment должна иметь PK");
        assertTrue(countRows(sql, "workshops") > 0, "workshops должна иметь PK");
    }

    @Test
    @DisplayName("Проверка внешних ключей")
    public void testForeignKeys() {
        String sql = """
            SELECT COUNT(*) FROM information_schema.table_constraints 
            WHERE constraint_type = 'FOREIGN KEY' 
            AND table_name = ?
            """;

        assertTrue(countRows(sql, "equipment") > 0, "equipment должна иметь FK");
        assertTrue(countRows(sql, "tasks") > 0, "tasks должна иметь FK");
        assertTrue(countRows(sql, "excursions") > 0, "excursions должна иметь FK");
        assertTrue(countRows(sql, "golden_tickets") > 0, "golden_tickets должна иметь FK");
    }
}

