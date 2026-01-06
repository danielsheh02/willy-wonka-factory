package com.example.demo.data_init;

import com.example.demo.models.*;
import com.example.demo.repositories.EquipmentRepository;
import com.example.demo.repositories.TaskRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WorkshopRepository;

import jakarta.transaction.Transactional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Transactional
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    private final TaskRepository taskRepository;

    private final PasswordEncoder passwordEncoder;

    private final WorkshopRepository workshopRepository;

    private final EquipmentRepository equipmentRepository;

    public DataInitializer(UserRepository userRepository, TaskRepository taskRepository,
            PasswordEncoder passwordEncoder, WorkshopRepository workshopRepository,
            EquipmentRepository equipmentRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
        this.workshopRepository = workshopRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0)
            return;

        String rawPassword = "password";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Создаем пользователей всех ролей
        User worker1 = new User();
        worker1.setUsername("worker1");
        worker1.setPassword(encodedPassword);
        worker1.setRole(Role.WORKER);

        User worker2 = new User();
        worker2.setUsername("worker2");
        worker2.setPassword(encodedPassword);
        worker2.setRole(Role.WORKER);

        User foreman = new User();
        foreman.setUsername("foreman1");
        foreman.setPassword(encodedPassword);
        foreman.setRole(Role.FOREMAN);

        User admin = new User();
        admin.setUsername("admin1");
        admin.setPassword(encodedPassword);
        admin.setRole(Role.ADMIN);

        User master = new User();
        master.setUsername("master1");
        master.setPassword(encodedPassword);
        master.setRole(Role.MASTER);

        User guide = new User();
        guide.setUsername("guide1");
        guide.setPassword(encodedPassword);
        guide.setRole(Role.GUIDE);

        User unknown = new User();
        unknown.setUsername("unknown1");
        unknown.setPassword(encodedPassword);
        unknown.setRole(Role.UNKNOWN);

        worker1 = userRepository.save(worker1);
        worker2 = userRepository.save(worker2);
        foreman = userRepository.save(foreman);
        admin = userRepository.save(admin);
        master = userRepository.save(master);
        guide = userRepository.save(guide);
        unknown = userRepository.save(unknown);

        worker1 = userRepository.findById(worker1.getId()).orElseThrow();
        worker2 = userRepository.findById(worker2.getId()).orElseThrow();
        foreman = userRepository.findById(foreman.getId()).orElseThrow();
        admin = userRepository.findById(admin.getId()).orElseThrow();
        master = userRepository.findById(master.getId()).orElseThrow();
        guide = userRepository.findById(guide.getId()).orElseThrow();
        unknown = userRepository.findById(unknown.getId()).orElseThrow();

        Task task1 = new Task();
        task1.setName("Подготовить упаковочную линию");
        task1.setDescription("Проверить работу оборудования перед запуском смены");
        task1.setStatus(TaskStatus.IN_PROGRESS);
        task1.setUser(worker1);

        Task task2 = new Task();
        task2.setName("Составить график дежурств");
        task2.setDescription("Распределить смены между рабочими");
        task2.setStatus(TaskStatus.NOT_ASSIGNED);
        task2.setUser(foreman);

        Task task3 = new Task();
        task3.setName("Проанализировать неисправности");
        task3.setDescription("Собрать отчёт по авариям за прошлую неделю");
        task3.setStatus(TaskStatus.COMPLETED);
        task3.setUser(worker1);

        Task task4 = new Task();
        task4.setName("Настроить шоколадный водопад");
        task4.setDescription("Проверить температуру и консистенцию шоколада");
        task4.setStatus(TaskStatus.IN_PROGRESS);
        task4.setUser(worker2);

        Task task5 = new Task();
        task5.setName("Провести инвентаризацию какао-бобов");
        task5.setDescription("Подсчитать запасы сырья на складе");
        task5.setStatus(TaskStatus.NOT_ASSIGNED);
        task5.setUser(worker2);

        Task task6 = new Task();
        task6.setName("Обслуживание глазировочного барабана");
        task6.setDescription("Произвести техническое обслуживание оборудования");
        task6.setStatus(TaskStatus.IN_PROGRESS);
        task6.setUser(master);

        Task task7 = new Task();
        task7.setName("Организовать экскурсию для школьников");
        task7.setDescription("Подготовить маршрут и презентацию");
        task7.setStatus(TaskStatus.NOT_ASSIGNED);
        task7.setUser(guide);

        Task task8 = new Task();
        task8.setName("Провести аудит безопасности");
        task8.setDescription("Проверить соблюдение норм охраны труда во всех цехах");
        task8.setStatus(TaskStatus.COMPLETED);
        task8.setUser(admin);

        taskRepository.saveAll(List.of(task1, task2, task3, task4, task5, task6, task7, task8));

        System.out.println("========================================");
        System.out.println("Created test users (login / password):");
        System.out.println("worker1 / password (WORKER)");
        System.out.println("worker2 / password (WORKER)");
        System.out.println("foreman1 / password (FOREMAN)");
        System.out.println("admin1 / password (ADMIN)");
        System.out.println("master1 / password (MASTER)");
        System.out.println("guide1 / password (GUIDE)");
        System.out.println("unknown1 / password (UNKNOWN)");
        System.out.println("========================================");

        // Цех упаковки
        Set<WorkshopToUser> foremanLinks1 = new HashSet<>();
        Workshop w1 = new Workshop();
        w1.setName("Цех упаковки");
        w1.setDescription("Отвечает за упаковку готовой продукции");
        w1.setCapacity(20);
        w1.setVisitDurationMinutes(15);
        w1.setEquipments(Collections.emptySet());
        WorkshopToUser link1 = new WorkshopToUser();
        link1.setWorkshop(w1);
        link1.setUser(foreman);
        foremanLinks1.add(link1);
        w1.setForemanLinks(foremanLinks1);

        // Цех глазирования
        Set<WorkshopToUser> foremanLinks2 = new HashSet<>();
        Workshop w2 = new Workshop();
        w2.setName("Цех глазирования");
        w2.setDescription("Нанесение шоколадной глазури на изделия");
        w2.setCapacity(15);
        w2.setVisitDurationMinutes(20);
        w2.setEquipments(Collections.emptySet());
        WorkshopToUser link2 = new WorkshopToUser();
        link2.setWorkshop(w2);
        link2.setUser(foreman);
        foremanLinks2.add(link2);
        w2.setForemanLinks(foremanLinks2);

        // Цех экспериментов
        Set<WorkshopToUser> foremanLinks3 = new HashSet<>();
        Workshop w3 = new Workshop();
        w3.setName("Цех экспериментов");
        w3.setDescription("Тестирование новых рецептов");
        w3.setCapacity(10);
        w3.setVisitDurationMinutes(25);
        w3.setEquipments(Collections.emptySet());
        WorkshopToUser link3 = new WorkshopToUser();
        link3.setWorkshop(w3);
        link3.setUser(master);
        foremanLinks3.add(link3);
        w3.setForemanLinks(foremanLinks3);

        // Шоколадный водопад
        Set<WorkshopToUser> foremanLinks4 = new HashSet<>();
        Workshop w4 = new Workshop();
        w4.setName("Шоколадный водопад");
        w4.setDescription("Главная достопримечательность фабрики - гигантский водопад из растопленного шоколада");
        w4.setCapacity(30);
        w4.setVisitDurationMinutes(10);
        w4.setEquipments(Collections.emptySet());
        WorkshopToUser link4 = new WorkshopToUser();
        link4.setWorkshop(w4);
        link4.setUser(worker1);
        foremanLinks4.add(link4);
        w4.setForemanLinks(foremanLinks4);

        // Комната вечных конфет
        Set<WorkshopToUser> foremanLinks5 = new HashSet<>();
        Workshop w5 = new Workshop();
        w5.setName("Комната вечных конфет");
        w5.setDescription("Волшебная комната с конфетами, которые никогда не кончаются");
        w5.setCapacity(25);
        w5.setVisitDurationMinutes(15);
        w5.setEquipments(Collections.emptySet());
        WorkshopToUser link5 = new WorkshopToUser();
        link5.setWorkshop(w5);
        link5.setUser(worker2);
        foremanLinks5.add(link5);
        w5.setForemanLinks(foremanLinks5);

        // Цех орехового дробления
        Set<WorkshopToUser> foremanLinks6 = new HashSet<>();
        Workshop w6 = new Workshop();
        w6.setName("Цех орехового дробления");
        w6.setDescription("Обработка и дробление орехов для шоколадных изделий");
        w6.setCapacity(12);
        w6.setVisitDurationMinutes(12);
        w6.setEquipments(Collections.emptySet());
        WorkshopToUser link6 = new WorkshopToUser();
        link6.setWorkshop(w6);
        link6.setUser(admin);
        foremanLinks6.add(link6);
        w6.setForemanLinks(foremanLinks6);

        w1 = workshopRepository.save(w1);
        w2 = workshopRepository.save(w2);
        w3 = workshopRepository.save(w3);
        w4 = workshopRepository.save(w4);
        w5 = workshopRepository.save(w5);
        w6 = workshopRepository.save(w6);

        w1 = workshopRepository.findById(w1.getId()).orElseThrow();
        w2 = workshopRepository.findById(w2.getId()).orElseThrow();
        w3 = workshopRepository.findById(w3.getId()).orElseThrow();
        w4 = workshopRepository.findById(w4.getId()).orElseThrow();
        w5 = workshopRepository.findById(w5.getId()).orElseThrow();
        w6 = workshopRepository.findById(w6.getId()).orElseThrow();

        Equipment eq1 = new Equipment();
        eq1.setName("Упаковочная машина X1");
        eq1.setDescription("Автоматическая упаковка шоколада в фольгу");
        eq1.setModel("X1-2020");
        eq1.setStatus(EquipmentStatus.WORKING);
        eq1.setHealth(95);
        eq1.setTemperature(40.5);
        eq1.setLastServicedAt(LocalDate.now().minusDays(7));
        eq1.setWorkshop(w1);

        Equipment eq2 = new Equipment();
        eq2.setName("Глазировочный барабан");
        eq2.setDescription("Наносит шоколадное покрытие");
        eq2.setModel("GL-3000");
        eq2.setStatus(EquipmentStatus.BROKEN);
        eq2.setHealth(60);
        eq2.setTemperature(52.3);
        eq2.setLastServicedAt(LocalDate.now().minusDays(20));
        eq2.setWorkshop(w2);

        Equipment eq3 = new Equipment();
        eq3.setName("Миксер-экспериментатор");
        eq3.setDescription("Смешивает ингредиенты для новых вкусов");
        eq3.setModel("LAB-MX-9000");
        eq3.setStatus(EquipmentStatus.UNDER_REPAIR);
        eq3.setHealth(30);
        eq3.setTemperature(75.0);
        eq3.setLastServicedAt(LocalDate.now().minusDays(40));
        eq3.setWorkshop(w3);

        Equipment eq4 = new Equipment();
        eq4.setName("Насос шоколадного водопада");
        eq4.setDescription("Поддерживает циркуляцию шоколада в водопаде");
        eq4.setModel("WF-PUMP-5000");
        eq4.setStatus(EquipmentStatus.WORKING);
        eq4.setHealth(88);
        eq4.setTemperature(45.0);
        eq4.setLastServicedAt(LocalDate.now().minusDays(5));
        eq4.setWorkshop(w4);

        Equipment eq5 = new Equipment();
        eq5.setName("Конфетный генератор");
        eq5.setDescription("Производит бесконечные конфеты");
        eq5.setModel("CANDY-GEN-2023");
        eq5.setStatus(EquipmentStatus.WORKING);
        eq5.setHealth(100);
        eq5.setTemperature(22.0);
        eq5.setLastServicedAt(LocalDate.now().minusDays(3));
        eq5.setWorkshop(w5);

        Equipment eq6 = new Equipment();
        eq6.setName("Ореходробилка 3000");
        eq6.setDescription("Измельчает орехи различных размеров");
        eq6.setModel("NUT-CRUSH-3000");
        eq6.setStatus(EquipmentStatus.WORKING);
        eq6.setHealth(78);
        eq6.setTemperature(35.5);
        eq6.setLastServicedAt(LocalDate.now().minusDays(14));
        eq6.setWorkshop(w6);

        equipmentRepository.saveAll(List.of(eq1, eq2, eq3, eq4, eq5, eq6));
    }
}