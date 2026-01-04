import { useAuth } from "../auth/AuthProvider";

export const usePermissions = () => {
  const { user } = useAuth();
  const role = user?.role;

  const permissions = {
    // Просмотр страниц
    canViewTasks: ['WORKER', 'FOREMAN', 'ADMIN', 'MASTER', 'GUIDE'].includes(role),
    canViewTaskDistribution: ['FOREMAN', 'ADMIN'].includes(role),
    canViewUsers: ['WORKER', 'FOREMAN', 'ADMIN', 'MASTER', 'GUIDE'].includes(role),
    canViewEquipment: ['WORKER', 'FOREMAN', 'ADMIN', 'MASTER', 'GUIDE'].includes(role),
    canViewWorkshops: ['WORKER', 'FOREMAN', 'ADMIN', 'MASTER', 'GUIDE'].includes(role),
    canViewExcursions: ['ADMIN', 'GUIDE'].includes(role),
    canViewTickets: ['ADMIN', 'GUIDE'].includes(role),

    // Задачи
    canCreateTask: ['FOREMAN', 'ADMIN'].includes(role),
    canEditTask: (taskUserId) => {
      if (['ADMIN', 'FOREMAN'].includes(role)) return true;
      if (['WORKER', 'MASTER', 'GUIDE'].includes(role)) {
        return taskUserId === user?.id;
      }
      return false;
    },
    canDeleteTask: ['FOREMAN', 'ADMIN'].includes(role),

    // Пользователи
    canCreateUser: ['FOREMAN', 'ADMIN'].includes(role),
    canEditUser: ['FOREMAN', 'ADMIN'].includes(role),
    canDeleteUser: ['FOREMAN', 'ADMIN'].includes(role),

    // Оборудование
    canCreateEquipment: ['FOREMAN', 'ADMIN', 'MASTER'].includes(role),
    canEditEquipment: ['FOREMAN', 'ADMIN', 'MASTER'].includes(role),
    canDeleteEquipment: ['FOREMAN', 'ADMIN', 'MASTER'].includes(role),

    // Цеха
    canCreateWorkshop: ['FOREMAN', 'ADMIN'].includes(role),
    canEditWorkshop: ['FOREMAN', 'ADMIN'].includes(role),
    canDeleteWorkshop: ['FOREMAN', 'ADMIN'].includes(role),

    // Экскурсии
    canCreateExcursion: ['ADMIN', 'GUIDE'].includes(role),
    canEditExcursion: ['ADMIN', 'GUIDE'].includes(role),
    canDeleteExcursion: ['ADMIN', 'GUIDE'].includes(role),

    // Золотые билеты
    canGenerateTickets: ['ADMIN'].includes(role),
    canViewTicketsDetails: ['ADMIN', 'GUIDE'].includes(role),
    
    // Распределение задач
    canDistributeTasks: ['FOREMAN', 'ADMIN'].includes(role),
    
    // Отчеты
    canViewReports: ['FOREMAN', 'ADMIN'].includes(role),
  };

  return permissions;
};

