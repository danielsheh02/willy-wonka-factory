package com.example.demo.database;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.repositories.*;
import com.example.demo.services.UserService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseDatabaseTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TaskRepository taskRepository;

    @Autowired
    protected WorkshopRepository workshopRepository;

    @Autowired
    protected EquipmentRepository equipmentRepository;

    @Autowired
    protected ExcursionRepository excursionRepository;

    @Autowired
    protected GoldenTicketRepository goldenTicketRepository;

    @Autowired
    protected NotificationRepository notificationRepository;
    
    @Autowired
    protected UserService userService;

    @BeforeEach
    public void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM golden_tickets");
        jdbcTemplate.execute("DELETE FROM excursion_routes");
        jdbcTemplate.execute("DELETE FROM excursions");
        jdbcTemplate.execute("DELETE FROM notifications");
        jdbcTemplate.execute("DELETE FROM tasks");
        jdbcTemplate.execute("DELETE FROM equipment");
        jdbcTemplate.execute("DELETE FROM workshop_user");
        jdbcTemplate.execute("DELETE FROM workshops");
        jdbcTemplate.execute("DELETE FROM users");
    }

    /**
     * Выполняет SQL запрос и возвращает количество строк
     */
    protected int countRows(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null ? count : 0;
    }

    /**
     * Проверяет существование таблицы
     */
    protected boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        return count != null && count > 0;
    }

    /**
     * Проверяет существование колонки в таблице
     */
    protected boolean columnExists(String tableName, String columnName) {
        String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    /**
     * Проверяет nullable статус колонки
     */
    protected boolean isColumnNullable(String tableName, String columnName) {
        String sql = "SELECT is_nullable FROM information_schema.columns WHERE table_name = ? AND column_name = ?";
        String isNullable = jdbcTemplate.queryForObject(sql, String.class, tableName, columnName);
        return "YES".equals(isNullable);
    }
}

