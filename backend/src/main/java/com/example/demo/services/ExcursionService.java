package com.example.demo.services;

import com.example.demo.dto.request.ExcursionRequestDTO;
import com.example.demo.dto.response.ExcursionResponseDTO;
import com.example.demo.dto.response.ExcursionRouteDTO;
import com.example.demo.models.*;
import com.example.demo.repositories.ExcursionRepository;
import com.example.demo.repositories.ExcursionRouteRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WorkshopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcursionService {

    @Autowired
    private ExcursionRepository excursionRepository;

    @Autowired
    private ExcursionRouteRepository routeRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ExcursionResponseDTO> getAllExcursions() {
        return excursionRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ExcursionResponseDTO> getExcursionById(Long id) {
        return excursionRepository.findById(id)
                .map(this::toDTO);
    }

    public List<ExcursionResponseDTO> getExcursionsByGuideId(Long guideId) {
        return excursionRepository.findByGuideId(guideId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ExcursionStatus> getAllStatuses() {
        return Arrays.asList(ExcursionStatus.values());
    }

    @Transactional
    public ExcursionResponseDTO createExcursion(ExcursionRequestDTO dto) {
        User guide = userRepository.findById(dto.getGuideId())
                .orElseThrow(() -> new RuntimeException("Экскурсовод не найден"));

        // ЖЕСТКАЯ ПРОВЕРКА: Проверяем занятость гида ПЕРЕД созданием экскурсии
        if (dto.getAutoGenerateRoute() != null && dto.getAutoGenerateRoute()) {
            // Для автоматического построения проверяем весь рабочий день (8 часов)
            LocalDateTime excursionEnd = dto.getStartTime().plusHours(8);
            List<Excursion> guideConflicts = excursionRepository.findGuideConflicts(
                dto.getGuideId(), 
                dto.getStartTime(), 
                excursionEnd
            );
            
            if (!guideConflicts.isEmpty()) {
                throw new RuntimeException("Гид '" + guide.getUsername() + "' уже занят в это время другой экскурсией (ID: " + 
                    guideConflicts.get(0).getId() + ", начало: " + guideConflicts.get(0).getStartTime() + "). Выберите другого гида или другое время.");
            }
        } else if (dto.getRoutes() != null && !dto.getRoutes().isEmpty()) {
            // Для ручного построения рассчитываем точное время окончания
            LocalDateTime excursionEnd = calculateExcursionEndTime(dto);
            List<Excursion> guideConflicts = excursionRepository.findGuideConflicts(
                dto.getGuideId(), 
                dto.getStartTime(), 
                excursionEnd
            );
            
            if (!guideConflicts.isEmpty()) {
                throw new RuntimeException("Гид '" + guide.getUsername() + "' уже занят в это время другой экскурсией (ID: " + 
                    guideConflicts.get(0).getId() + ", начало: " + guideConflicts.get(0).getStartTime() + "). Выберите другого гида или другое время.");
            }
        }

        Excursion excursion = new Excursion();
        excursion.setName(dto.getName());
        excursion.setStartTime(dto.getStartTime());
        excursion.setParticipantsCount(dto.getParticipantsCount());
        excursion.setGuide(guide);
        excursion.setStatus(dto.getStatus() != null ? dto.getStatus() : ExcursionStatus.DRAFT);

        excursion = excursionRepository.save(excursion);

        // Создание маршрута
        if (dto.getAutoGenerateRoute() != null && dto.getAutoGenerateRoute()) {
            // Автоматическое построение маршрута
            List<ExcursionRoute> routes = generateAutomaticRoute(excursion);
            
            // ЖЕСТКАЯ ПРОВЕРКА: Если не удалось построить маршрут - ЗАПРЕЩАЕМ создание
            if (routes.isEmpty()) {
                // Откатываем транзакцию
                excursionRepository.delete(excursion);
                throw new RuntimeException("Не удалось построить маршрут экскурсии: все цеха заняты или не подходят по вместимости для группы из " + 
                    excursion.getParticipantsCount() + " человек. Попробуйте изменить время начала или уменьшить количество участников.");
            }
            
            excursion.getRoutes().clear();
            excursion.getRoutes().addAll(routes);
        } else if (dto.getRoutes() != null && !dto.getRoutes().isEmpty()) {
            // Ручное создание маршрута с проверкой
            List<ExcursionRoute> routes = createManualRoute(excursion, dto.getRoutes());
            excursion.getRoutes().clear();
            excursion.getRoutes().addAll(routes);
        }

        excursion = excursionRepository.save(excursion);
        return toDTO(excursion);
    }

    @Transactional
    public ExcursionResponseDTO updateExcursion(Long id, ExcursionRequestDTO dto) {
        Excursion excursion = excursionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Экскурсия не найдена"));
        
        final Long excursionId = excursion.getId(); // Для использования в лямбдах

        if (dto.getName() != null) {
            excursion.setName(dto.getName());
        }
        if (dto.getStartTime() != null) {
            excursion.setStartTime(dto.getStartTime());
        }
        if (dto.getParticipantsCount() != null) {
            excursion.setParticipantsCount(dto.getParticipantsCount());
        }
        if (dto.getGuideId() != null) {
            User guide = userRepository.findById(dto.getGuideId())
                    .orElseThrow(() -> new RuntimeException("Экскурсовод не найден"));
            excursion.setGuide(guide);
        }
        if (dto.getStatus() != null) {
            excursion.setStatus(dto.getStatus());
        }

        // ЖЕСТКАЯ ПРОВЕРКА: Проверяем занятость гида при обновлении
        if ((dto.getGuideId() != null || dto.getStartTime() != null || dto.getRoutes() != null || dto.getAutoGenerateRoute() != null)) {
            LocalDateTime checkStartTime = dto.getStartTime() != null ? dto.getStartTime() : excursion.getStartTime();
            LocalDateTime checkEndTime;
            
            if (dto.getAutoGenerateRoute() != null && dto.getAutoGenerateRoute()) {
                checkEndTime = checkStartTime.plusHours(8);
            } else if (dto.getRoutes() != null && !dto.getRoutes().isEmpty()) {
                // Рассчитываем время окончания на основе нового маршрута
                LocalDateTime tempTime = checkStartTime;
                for (ExcursionRequestDTO.RoutePointDTO point : dto.getRoutes()) {
                    Workshop workshop = workshopRepository.findById(point.getWorkshopId()).orElse(null);
                    int duration = point.getDurationMinutes() != null ? point.getDurationMinutes() : 
                                  (workshop != null && workshop.getVisitDurationMinutes() != null ? 
                                   workshop.getVisitDurationMinutes() : 15);
                    tempTime = tempTime.plusMinutes(duration);
                }
                checkEndTime = tempTime;
            } else {
                // Используем существующий маршрут
                checkEndTime = excursion.getRoutes().stream()
                    .map(r -> r.getStartTime().plusMinutes(r.getDurationMinutes()))
                    .max(LocalDateTime::compareTo)
                    .orElse(checkStartTime.plusHours(2));
            }
            
            List<Excursion> guideConflicts = excursionRepository.findGuideConflicts(
                excursion.getGuide().getId(), 
                checkStartTime, 
                checkEndTime
            );
            
            // Исключаем текущую экскурсию из конфликтов
            guideConflicts = guideConflicts.stream()
                    .filter(e -> !e.getId().equals(excursionId))
                    .collect(Collectors.toList());
            
            if (!guideConflicts.isEmpty()) {
                throw new RuntimeException("Гид '" + excursion.getGuide().getUsername() + "' уже занят в это время другой экскурсией (ID: " + 
                    guideConflicts.get(0).getId() + ", начало: " + guideConflicts.get(0).getStartTime() + "). Выберите другого гида или другое время.");
            }
        }

        // Обновление маршрута при необходимости
        if (dto.getAutoGenerateRoute() != null && dto.getAutoGenerateRoute()) {
            // Удаляем старый маршрут и создаем новый
            excursion.getRoutes().clear();
            List<ExcursionRoute> routes = generateAutomaticRoute(excursion);
            
            // ЖЕСТКАЯ ПРОВЕРКА: Если не удалось построить маршрут - ЗАПРЕЩАЕМ обновление
            if (routes.isEmpty()) {
                throw new RuntimeException("Не удалось построить маршрут экскурсии: все цеха заняты или не подходят по вместимости для группы из " + 
                    excursion.getParticipantsCount() + " человек. Попробуйте изменить время начала или уменьшить количество участников.");
            }
            
            excursion.getRoutes().addAll(routes);
        } else if (dto.getRoutes() != null) {
            excursion.getRoutes().clear();
            List<ExcursionRoute> routes = createManualRoute(excursion, dto.getRoutes());
            excursion.getRoutes().addAll(routes);
        }

        excursion = excursionRepository.save(excursion);
        return toDTO(excursion);
    }

    @Transactional
    public void deleteExcursion(Long id) {
        excursionRepository.deleteById(id);
    }

    // Автоматическое построение маршрута с учетом занятости цехов
    private List<ExcursionRoute> generateAutomaticRoute(Excursion excursion) {
        List<Workshop> allWorkshops = workshopRepository.findAll();
        List<ExcursionRoute> routes = new ArrayList<>();
        
        LocalDateTime currentTime = excursion.getStartTime();
        int orderNumber = 1;

        // Сортируем цеха по имени для детерминированности
        allWorkshops.sort(Comparator.comparing(Workshop::getName));

        for (Workshop workshop : allWorkshops) {
            // Получаем стандартную длительность или используем 15 минут по умолчанию
            int duration = workshop.getVisitDurationMinutes() != null ? 
                          workshop.getVisitDurationMinutes() : 15;

            // Пытаемся найти свободное время для посещения этого цеха с учетом вместимости
            LocalDateTime availableTime = findAvailableTimeSlot(
                workshop, 
                currentTime, 
                duration, 
                excursion.getId(),
                excursion.getParticipantsCount()
            );

            if (availableTime != null) {
                ExcursionRoute route = new ExcursionRoute();
                route.setExcursion(excursion);
                route.setWorkshop(workshop);
                route.setOrderNumber(orderNumber++);
                route.setStartTime(availableTime);
                route.setDurationMinutes(duration);
                
                route = routeRepository.save(route);
                routes.add(route);

                // Обновляем текущее время для следующего цеха
                currentTime = availableTime.plusMinutes(duration);
            }
        }

        return routes;
    }

    // Создание маршрута вручную с проверкой доступности
    private List<ExcursionRoute> createManualRoute(Excursion excursion, 
                                                   List<ExcursionRequestDTO.RoutePointDTO> routePoints) {
        List<ExcursionRoute> routes = new ArrayList<>();
        LocalDateTime currentTime = excursion.getStartTime();

        // Сортируем точки маршрута по порядковому номеру
        routePoints.sort(Comparator.comparing(ExcursionRequestDTO.RoutePointDTO::getOrderNumber));

        for (ExcursionRequestDTO.RoutePointDTO point : routePoints) {
            Workshop workshop = workshopRepository.findById(point.getWorkshopId())
                    .orElseThrow(() -> new RuntimeException("Цех не найден: " + point.getWorkshopId()));

            int duration = point.getDurationMinutes() != null ? point.getDurationMinutes() : 
                          (workshop.getVisitDurationMinutes() != null ? workshop.getVisitDurationMinutes() : 15);

            LocalDateTime endTime = currentTime.plusMinutes(duration);

            // Проверяем суммарную вместимость цеха
            if (workshop.getCapacity() != null) {  // null = бесконечная вместимость
                Integer currentOccupancy = routeRepository.getTotalParticipantsInWorkshop(
                    workshop.getId(), 
                    currentTime, 
                    endTime
                );
                
                // Если редактируем экскурсию, вычитаем её участников из текущей занятости
                if (excursion.getId() != null) {
                    List<ExcursionRoute> currentExcursionRoutes = routeRepository.findConflictingRoutes(
                        workshop.getId(), currentTime, endTime
                    ).stream()
                        .filter(r -> r.getExcursion().getId().equals(excursion.getId()))
                        .collect(Collectors.toList());
                    
                    if (!currentExcursionRoutes.isEmpty()) {
                        currentOccupancy -= currentExcursionRoutes.get(0).getExcursion().getParticipantsCount();
                    }
                }
                
                int totalWithNewGroup = currentOccupancy + excursion.getParticipantsCount();
                
                if (totalWithNewGroup > workshop.getCapacity()) {
                    throw new RuntimeException("Цех '" + workshop.getName() + 
                        "' перегружен в период с " + currentTime + " по " + endTime + 
                        ": текущая занятость " + currentOccupancy + ", новая группа " + 
                        excursion.getParticipantsCount() + ", вместимость " + workshop.getCapacity());
                }
            }

            ExcursionRoute route = new ExcursionRoute();
            route.setExcursion(excursion);
            route.setWorkshop(workshop);
            route.setOrderNumber(point.getOrderNumber());
            route.setStartTime(currentTime);
            route.setDurationMinutes(duration);
            
            route = routeRepository.save(route);
            routes.add(route);

            currentTime = endTime;
        }

        return routes;
    }

    // Найти ближайшее свободное время для посещения цеха с учетом вместимости
    private LocalDateTime findAvailableTimeSlot(Workshop workshop, LocalDateTime preferredStart, 
                                               int duration, Long excursionId, Integer participantsCount) {
        LocalDateTime currentAttempt = preferredStart;
        LocalDateTime maxTime = preferredStart.plusHours(8); // Ищем в пределах 8 часов

        while (currentAttempt.isBefore(maxTime)) {
            LocalDateTime endTime = currentAttempt.plusMinutes(duration);
            
            // Проверяем суммарную вместимость (если она задана)
            boolean capacityAvailable = true;
            if (workshop.getCapacity() != null) {  // null = бесконечная вместимость
                Integer currentOccupancy = routeRepository.getTotalParticipantsInWorkshop(
                    workshop.getId(), 
                    currentAttempt, 
                    endTime
                );
                
                // Если редактируем экскурсию, вычитаем её участников
                if (excursionId != null) {
                    List<ExcursionRoute> currentExcursionRoutes = routeRepository.findConflictingRoutes(
                        workshop.getId(), currentAttempt, endTime
                    ).stream()
                        .filter(r -> r.getExcursion().getId().equals(excursionId))
                        .collect(Collectors.toList());
                    
                    if (!currentExcursionRoutes.isEmpty()) {
                        currentOccupancy -= currentExcursionRoutes.get(0).getExcursion().getParticipantsCount();
                    }
                }
                
                int totalWithNewGroup = currentOccupancy + participantsCount;
                capacityAvailable = (totalWithNewGroup <= workshop.getCapacity());
            }
            
            if (capacityAvailable) {
                return currentAttempt;
            }

            // Если нет места, пробуем после окончания самого раннего конфликтующего маршрута
            List<ExcursionRoute> conflicts = routeRepository.findConflictingRoutes(
                workshop.getId(), 
                currentAttempt, 
                endTime
            );
            
            if (!conflicts.isEmpty()) {
                // Исключаем текущую экскурсию
                conflicts = conflicts.stream()
                        .filter(r -> !r.getExcursion().getId().equals(excursionId))
                        .collect(Collectors.toList());
                
                if (!conflicts.isEmpty()) {
                    LocalDateTime earliestEnd = conflicts.stream()
                            .map(r -> r.getStartTime().plusMinutes(r.getDurationMinutes()))
                            .min(LocalDateTime::compareTo)
                            .orElse(currentAttempt.plusMinutes(15));
                    
                    currentAttempt = earliestEnd;
                } else {
                    currentAttempt = currentAttempt.plusMinutes(15); // Сдвигаем на 15 минут
                }
            } else {
                currentAttempt = currentAttempt.plusMinutes(15); // Сдвигаем на 15 минут
            }
        }

        return null; // Не нашли свободное время
    }

    // Проверка доступности маршрута
    public Map<String, Object> checkRouteAvailability(ExcursionRequestDTO dto) {
        Map<String, Object> result = new HashMap<>();
        List<String> conflicts = new ArrayList<>();
        boolean isAvailable = true;

        if (dto.getRoutes() == null || dto.getRoutes().isEmpty()) {
            result.put("available", false);
            result.put("message", "Маршрут не указан");
            return result;
        }

        // Проверяем занятость гида на всё время экскурсии
        if (dto.getGuideId() != null) {
            LocalDateTime excursionStart = dto.getStartTime();
            LocalDateTime excursionEnd = calculateExcursionEndTime(dto);
            
            List<Excursion> guideConflicts = excursionRepository.findGuideConflicts(
                dto.getGuideId(), 
                excursionStart, 
                excursionEnd
            );
            
            // Исключаем текущую экскурсию при редактировании
            if (dto.getExcursionId() != null) {
                guideConflicts = guideConflicts.stream()
                    .filter(e -> !e.getId().equals(dto.getExcursionId()))
                    .collect(Collectors.toList());
            }
            
            if (!guideConflicts.isEmpty()) {
                conflicts.add("Гид уже занят в это время другой экскурсией (ID: " + 
                    guideConflicts.get(0).getId() + ")");
                isAvailable = false;
            }
        }

        LocalDateTime currentTime = dto.getStartTime();
        
        for (ExcursionRequestDTO.RoutePointDTO point : dto.getRoutes()) {
            Workshop workshop = workshopRepository.findById(point.getWorkshopId()).orElse(null);
            
            if (workshop == null) {
                conflicts.add("Цех с ID " + point.getWorkshopId() + " не найден");
                isAvailable = false;
                continue;
            }

            int duration = point.getDurationMinutes() != null ? point.getDurationMinutes() : 
                          (workshop.getVisitDurationMinutes() != null ? workshop.getVisitDurationMinutes() : 15);

            LocalDateTime endTime = currentTime.plusMinutes(duration);

            // Проверка суммарной вместимости цеха
            if (workshop.getCapacity() != null) {  // null = бесконечная вместимость
                Integer currentOccupancy = routeRepository.getTotalParticipantsInWorkshop(
                    workshop.getId(), 
                    currentTime, 
                    endTime
                );
                
                // Если редактируем экскурсию, вычитаем её участников из текущей занятости
                if (dto.getExcursionId() != null) {
                    List<ExcursionRoute> currentExcursionRoutes = routeRepository.findConflictingRoutes(
                        workshop.getId(), currentTime, endTime
                    ).stream()
                        .filter(r -> r.getExcursion().getId().equals(dto.getExcursionId()))
                        .collect(Collectors.toList());
                    
                    if (!currentExcursionRoutes.isEmpty()) {
                        currentOccupancy -= currentExcursionRoutes.get(0).getExcursion().getParticipantsCount();
                    }
                }
                
                int totalWithNewGroup = currentOccupancy + dto.getParticipantsCount();
                
                if (totalWithNewGroup > workshop.getCapacity()) {
                    conflicts.add("Цех '" + workshop.getName() + "' перегружен: текущая занятость " + 
                        currentOccupancy + ", новая группа " + dto.getParticipantsCount() + 
                        ", вместимость " + workshop.getCapacity());
                    isAvailable = false;
                }
            }

            currentTime = endTime;
        }

        result.put("available", isAvailable);
        result.put("conflicts", conflicts);
        result.put("message", isAvailable ? "Маршрут доступен" : "Маршрут недоступен");
        
        return result;
    }
    
    // Вспомогательный метод для расчета времени окончания экскурсии
    private LocalDateTime calculateExcursionEndTime(ExcursionRequestDTO dto) {
        LocalDateTime currentTime = dto.getStartTime();
        for (ExcursionRequestDTO.RoutePointDTO point : dto.getRoutes()) {
            Workshop workshop = workshopRepository.findById(point.getWorkshopId()).orElse(null);
            int duration = point.getDurationMinutes() != null ? point.getDurationMinutes() : 
                          (workshop != null && workshop.getVisitDurationMinutes() != null ? 
                           workshop.getVisitDurationMinutes() : 15);
            currentTime = currentTime.plusMinutes(duration);
        }
        return currentTime;
    }

    // Конвертация в DTO
    private ExcursionResponseDTO toDTO(Excursion excursion) {
        List<ExcursionRouteDTO> routeDTOs = excursion.getRoutes().stream()
                .sorted(Comparator.comparing(ExcursionRoute::getOrderNumber))
                .map(route -> new ExcursionRouteDTO(
                    route.getId(),
                    route.getWorkshop().getId(),
                    route.getWorkshop().getName(),
                    route.getOrderNumber(),
                    route.getStartTime(),
                    route.getDurationMinutes()
                ))
                .collect(Collectors.toList());

        // Рассчитываем время окончания экскурсии на основе маршрута
        LocalDateTime endTime = excursion.getRoutes().stream()
                .map(route -> route.getStartTime().plusMinutes(route.getDurationMinutes()))
                .max(LocalDateTime::compareTo)
                .orElse(excursion.getStartTime()); // Если маршрут пустой, используем время начала

        return new ExcursionResponseDTO(
            excursion.getId(),
            excursion.getName(),
            excursion.getStartTime(),
            endTime,
            excursion.getParticipantsCount(),
            excursion.getGuide().getId(),
            excursion.getGuide().getUsername(),
            excursion.getStatus(),
            excursion.getCreatedAt(),
            routeDTOs
        );
    }
}

