package com.example.demo.data_init;

import com.example.demo.models.*;
import com.example.demo.repositories.EquipmentRepository;
import com.example.demo.repositories.TaskRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WorkshopRepository;

import jakarta.transaction.Transactional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Transactional
@Component
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

        User worker = new User();
        worker.setUsername("worker1");
        worker.setPassword(encodedPassword);
        worker.setRole(Role.WORKER);
        worker.setWorkshops(Collections.emptySet());

        User foreman = new User();
        foreman.setUsername("foreman1");
        foreman.setPassword(encodedPassword);
        foreman.setRole(Role.FOREMAN);
        foreman.setWorkshops(Collections.emptySet());

        User unknown = new User();
        unknown.setUsername("unknown1");
        unknown.setPassword(encodedPassword);
        unknown.setRole(Role.UNKNOWN);
        unknown.setWorkshops(Collections.emptySet());

        worker = userRepository.save(worker);
        foreman = userRepository.save(foreman);
        unknown = userRepository.save(unknown);

        worker = userRepository.findById(worker.getId()).orElseThrow();
        foreman = userRepository.findById(foreman.getId()).orElseThrow();
        unknown = userRepository.findById(unknown.getId()).orElseThrow();

        Task task1 = new Task();
        task1.setName("Подготовить упаковочную линию");
        task1.setDescription("Проверить работу оборудования перед запуском смены");
        task1.setStatus(TaskStatus.IN_PROGRESS);
        task1.setUser(worker);

        Task task2 = new Task();
        task2.setName("Составить график дежурств");
        task2.setDescription("Распределить смены между рабочими");
        task2.setStatus(TaskStatus.NOT_ASSIGNED);
        task2.setUser(foreman);

        Task task3 = new Task();
        task3.setName("Проанализировать неисправности");
        task3.setDescription("Собрать отчёт по авариям за прошлую неделю");
        task3.setStatus(TaskStatus.COMPLETED);
        task3.setUser(worker);

        taskRepository.saveAll(List.of(task1, task2, task3));

        System.out.println("Created users");
        System.out.println("worker1 / password");
        System.out.println("foreman1 / password");
        System.out.println("unknown1 / password");

        Workshop w1 = new Workshop();
        w1.setName("Цех упаковки");
        w1.setDescription("Отвечает за упаковку готовой продукции");
        w1.setForemans(Set.of(foreman));
        w1.setEquipments(Collections.emptySet());

        Workshop w2 = new Workshop();
        w2.setName("Цех глазирования");
        w2.setDescription("Нанесение шоколадной глазури на изделия");
        w2.setForemans(Set.of(foreman));
        w2.setEquipments(Collections.emptySet());

        Workshop w3 = new Workshop();
        w3.setName("Цех экспериментов");
        w3.setDescription("Тестирование новых рецептов");
        w3.setForemans(Set.of(unknown));
        w3.setEquipments(Collections.emptySet());

        w1 = workshopRepository.save(w1);
        w2 = workshopRepository.save(w2);
        w3 = workshopRepository.save(w3);

        w1 = workshopRepository.findById(w1.getId()).orElseThrow();
        w2 = workshopRepository.findById(w2.getId()).orElseThrow();
        w3 = workshopRepository.findById(w3.getId()).orElseThrow();

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

        equipmentRepository.saveAll(List.of(eq1, eq2, eq3));
    }
}