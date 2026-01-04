package com.example.demo.services;

import com.example.demo.dto.request.ExcursionRequestDTO;
import com.example.demo.dto.response.ExcursionResponseDTO;
import com.example.demo.dto.response.ExcursionRouteDTO;
import com.example.demo.models.*;
import com.example.demo.repositories.ExcursionRepository;
import com.example.demo.repositories.ExcursionRouteRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WorkshopRepository;
import com.example.demo.utils.DateTimeUtils;
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
            List<ExcursionRoute> routes = generateAutomaticRoute(excursion, dto.getMinRequiredWorkshops());
            
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
            List<ExcursionRoute> routes = generateAutomaticRoute(excursion, dto.getMinRequiredWorkshops());
            
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
    // УЛУЧШЕННЫЙ АЛГОРИТМ: сначала ищем доступный цех в текущее время, только потом сдвигаем время
    private List<ExcursionRoute> generateAutomaticRoute(Excursion excursion, Integer requestedMinWorkshops) {
        List<Workshop> allWorkshops = workshopRepository.findAll();
        
        // Фильтруем цеха, которые подходят по вместимости
        List<Workshop> suitableWorkshops = allWorkshops.stream()
            .filter(w -> w.getCapacity() == null || w.getCapacity() >= excursion.getParticipantsCount())
            .collect(Collectors.toList());
        
        // Проверяем, есть ли хотя бы один подходящий цех
        if (suitableWorkshops.isEmpty()) {
            throw new RuntimeException("Нет цехов, подходящих для группы из " + 
                excursion.getParticipantsCount() + " человек. Все цеха имеют меньшую вместимость.");
        }
        
        // Определяем минимальное требуемое количество цехов
        int minRequiredWorkshops;
        if (requestedMinWorkshops == null || requestedMinWorkshops <= 0) {
            // Если не указано или указано "максимально возможно" (0 или null) - пытаемся посетить все
            minRequiredWorkshops = suitableWorkshops.size();
        } else {
            // Проверяем, не запрашивает ли пользователь больше, чем доступно
            if (requestedMinWorkshops > suitableWorkshops.size()) {
                throw new RuntimeException("Запрошено посещение минимум " + requestedMinWorkshops + 
                    " цехов, но для группы из " + excursion.getParticipantsCount() + 
                    " человек доступно только " + suitableWorkshops.size() + " цехов с подходящей вместимостью. " +
                    "Уменьшите количество цехов или размер группы.");
            }
            minRequiredWorkshops = requestedMinWorkshops;
        }
        
        Set<Long> visitedWorkshops = new HashSet<>(); // Отслеживаем посещенные цеха
        List<ExcursionRoute> routes = new ArrayList<>();
        
        LocalDateTime currentTime = excursion.getStartTime();
        LocalDateTime maxTime = currentTime.plusHours(8); // Максимум 8 часов на экскурсию
        int orderNumber = 1;
        int noProgressCounter = 0; // Счетчик попыток без прогресса
        
        // Продолжаем, пока не посетим все ПОДХОДЯЩИЕ цеха или не достигнем максимального времени
        while (visitedWorkshops.size() < suitableWorkshops.size() && currentTime.isBefore(maxTime)) {
            Workshop selectedWorkshop = null;
            int selectedDuration = 15;
            
            // Ищем ЛЮБОЙ доступный цех в текущее время
            for (Workshop workshop : suitableWorkshops) {
                // Пропускаем уже посещенные цеха
                if (visitedWorkshops.contains(workshop.getId())) {
                    continue;
                }
                
                int duration = workshop.getVisitDurationMinutes() != null ? 
                              workshop.getVisitDurationMinutes() : 15;
                
                // Проверяем доступность цеха в текущее время
                if (isWorkshopAvailableAtTime(workshop, currentTime, duration, 
                                             excursion.getId(), excursion.getParticipantsCount())) {
                    selectedWorkshop = workshop;
                    selectedDuration = duration;
                    break; // Нашли доступный цех - берем его
                }
            }
            
            if (selectedWorkshop != null) {
                // Нашли доступный цех - добавляем в маршрут
                ExcursionRoute route = new ExcursionRoute();
                route.setExcursion(excursion);
                route.setWorkshop(selectedWorkshop);
                route.setOrderNumber(orderNumber++);
                route.setStartTime(currentTime);
                route.setDurationMinutes(selectedDuration);
                
                route = routeRepository.save(route);
                routes.add(route);
                
                visitedWorkshops.add(selectedWorkshop.getId());
                currentTime = currentTime.plusMinutes(selectedDuration);
                noProgressCounter = 0; // Сбрасываем счетчик
            } else {
                // Все подходящие цеха заняты в текущее время - сдвигаем время на 15 минут
                currentTime = currentTime.plusMinutes(15);
                noProgressCounter++;
                
                // Если слишком долго не можем найти цех - прерываем
                if (noProgressCounter > 20) { // 20 * 15 минут = 5 часов без прогресса
                    break;
                }
            }
        }
        
        // Проверяем, удалось ли посетить требуемое количество цехов
        if (routes.size() < minRequiredWorkshops) {
            // Не удалось построить маршрут с требуемым количеством цехов
            // Возвращаем пустой список (обработка ошибки будет в вызывающем коде)
            return new ArrayList<>();
        }

        return routes;
    }
    
    // Проверка доступности цеха в конкретное время
    private boolean isWorkshopAvailableAtTime(Workshop workshop, LocalDateTime startTime, 
                                              int duration, Long excursionId, Integer participantsCount) {
        LocalDateTime endTime = startTime.plusMinutes(duration);
        
        // Проверяем вместимость цеха
        if (workshop.getCapacity() != null) {
            Integer currentOccupancy = routeRepository.getTotalParticipantsInWorkshop(
                workshop.getId(), 
                startTime, 
                endTime
            );
            
            // Если редактируем экскурсию, вычитаем её участников
            if (excursionId != null) {
                List<ExcursionRoute> currentExcursionRoutes = routeRepository.findConflictingRoutes(
                    workshop.getId(), startTime, endTime
                ).stream()
                    .filter(r -> r.getExcursion().getId().equals(excursionId))
                    .collect(Collectors.toList());
                
                if (!currentExcursionRoutes.isEmpty()) {
                    currentOccupancy -= currentExcursionRoutes.get(0).getExcursion().getParticipantsCount();
                }
            }
            
            int totalWithNewGroup = currentOccupancy + participantsCount;
            
            // Проверяем, помещается ли группа
            if (totalWithNewGroup > workshop.getCapacity()) {
                return false;
            }
        }
        
        return true; // Цех доступен
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
                    String message = "Цех '" + workshop.getName() + "' перегружен в период с " + currentTime + " по " + endTime + ": ";
                    if (currentOccupancy == 0) {
                        message += "ваша группа из " + excursion.getParticipantsCount() + " человек не помещается (вместимость цеха: " + workshop.getCapacity() + " чел.)";
                    } else {
                        message += "уже занято " + currentOccupancy + " мест, ваша группа " + excursion.getParticipantsCount() + " чел., вместимость " + workshop.getCapacity() + " чел. (не хватает " + (totalWithNewGroup - workshop.getCapacity()) + " мест)";
                    }
                    throw new RuntimeException(message);
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
                    String conflictMessage;
                    if (currentOccupancy == 0) {
                        conflictMessage = "Цех '" + workshop.getName() + "': группа из " + dto.getParticipantsCount() + 
                            " человек не помещается (вместимость: " + workshop.getCapacity() + " чел.)";
                    } else {
                        conflictMessage = "Цех '" + workshop.getName() + "': уже занято " + currentOccupancy + 
                            " мест, ваша группа " + dto.getParticipantsCount() + " чел., вместимость " + 
                            workshop.getCapacity() + " чел. (не хватает " + (totalWithNewGroup - workshop.getCapacity()) + " мест)";
                    }
                    conflicts.add(conflictMessage);
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
    
    /**
     * Автоматическое обновление статусов экскурсий (для планировщика)
     * Изменяет статусы на IN_PROGRESS или COMPLETED в зависимости от времени
     */
    @Transactional
    public Map<String, Integer> updateExcursionStatuses() {
        LocalDateTime now = DateTimeUtils.nowUTC();
        
        int startedCount = startExcursions(now);
        int completedCount = completeExcursions(now);
        
        Map<String, Integer> result = new HashMap<>();
        result.put("started", startedCount);
        result.put("completed", completedCount);
        
        return result;
    }
    
    /**
     * Изменяет статус экскурсий на IN_PROGRESS, если наступило время начала
     */
    private int startExcursions(LocalDateTime now) {
        List<Excursion> excursionsToStart = excursionRepository.findAll().stream()
            .filter(e -> e.getStatus() == ExcursionStatus.CONFIRMED) // Только подтвержденные
            .filter(e -> e.getStartTime() != null)
            .filter(e -> !e.getStartTime().isAfter(now)) // Время начала <= текущего времени (UTC)
            .collect(Collectors.toList());
        
        for (Excursion excursion : excursionsToStart) {
            excursion.setStatus(ExcursionStatus.IN_PROGRESS);
            excursionRepository.save(excursion);
            System.out.println("[Excursion UTC] Начата: #" + excursion.getId() + " '" + 
                             excursion.getName() + "' (время начала UTC: " + excursion.getStartTime() + ")");
        }
        
        return excursionsToStart.size();
    }
    
    /**
     * Изменяет статус экскурсий на COMPLETED, если наступило время окончания
     */
    private int completeExcursions(LocalDateTime now) {
        List<Excursion> excursionsToComplete = excursionRepository.findAll().stream()
            .filter(e -> e.getStatus() == ExcursionStatus.IN_PROGRESS) // Только в процессе
            .filter(e -> {
                // Рассчитываем время окончания экскурсии
                LocalDateTime endTime = calculateExcursionEndTime(e);
                return endTime != null && !endTime.isAfter(now); // Время окончания <= текущего времени
            })
            .collect(Collectors.toList());
        
        for (Excursion excursion : excursionsToComplete) {
            excursion.setStatus(ExcursionStatus.COMPLETED);
            excursionRepository.save(excursion);
            System.out.println("[Excursion UTC] Завершена: #" + excursion.getId() + " '" + 
                             excursion.getName() + "'");
        }
        
        return excursionsToComplete.size();
    }
    
    /**
     * Вычисляет время окончания экскурсии на основе маршрута
     */
    private LocalDateTime calculateExcursionEndTime(Excursion excursion) {
        if (excursion.getRoutes() == null || excursion.getRoutes().isEmpty()) {
            // Если нет маршрута, считаем что экскурсия длится 2 часа
            return excursion.getStartTime().plusHours(2);
        }
        
        // Находим максимальное время окончания последней точки маршрута
        return excursion.getRoutes().stream()
            .map(route -> route.getStartTime().plusMinutes(route.getDurationMinutes()))
            .max(LocalDateTime::compareTo)
            .orElse(excursion.getStartTime().plusHours(2));
    }
}

