/**
 * Утилиты для работы с датами в UTC
 * 
 * Backend всегда отправляет даты в UTC (ISO-8601 формат): "2025-01-04T14:00:00"
 * Эти функции конвертируют UTC в локальное время пользователя для отображения.
 */

/**
 * Парсит UTC дату из строки и конвертирует в локальное время пользователя
 * 
 * @param {string} utcDateString - Дата в формате "2025-01-04T14:00:00" (UTC)
 * @returns {Date|null} - Date объект в локальном времени пользователя или null
 * 
 * @example
 * // Backend отправляет UTC: "2025-01-04T14:00:00"
 * const date = parseUTCDate("2025-01-04T14:00:00");
 * // В Москве (UTC+3) date будет представлять 17:00
 * // В Нью-Йорке (UTC-5) date будет представлять 09:00
 */
export function parseUTCDate(utcDateString) {
  if (!utcDateString) return null;
  
  // Добавляем 'Z' к строке, чтобы явно указать что это UTC
  // Spring Boot отправляет LocalDateTime без 'Z': "2025-01-04T14:00:00"
  // Добавляя 'Z', мы говорим JavaScript что это UTC: "2025-01-04T14:00:00Z"
  const dateString = utcDateString.endsWith('Z') ? utcDateString : utcDateString + 'Z';
  
  return new Date(dateString);
}

/**
 * Форматирует UTC дату в локальную дату (без времени)
 * 
 * @param {string} utcDateString - Дата в UTC
 * @param {string} locale - Локаль (по умолчанию 'ru-RU')
 * @returns {string} - Отформатированная дата в локальном времени
 * 
 * @example
 * formatDate("2025-01-04T14:00:00") // "04.01.2025"
 */
export function formatDate(utcDateString, locale = 'ru-RU') {
  const date = parseUTCDate(utcDateString);
  if (!date) return '-';
  
  return date.toLocaleDateString(locale);
}

/**
 * Форматирует UTC дату в локальную дату и время
 * 
 * @param {string} utcDateString - Дата в UTC
 * @param {string} locale - Локаль (по умолчанию 'ru-RU')
 * @returns {string} - Отформатированная дата и время в локальном времени
 * 
 * @example
 * // В Москве (UTC+3):
 * formatDateTime("2025-01-04T14:00:00") // "04.01.2025, 17:00:00"
 */
export function formatDateTime(utcDateString, locale = 'ru-RU') {
  const date = parseUTCDate(utcDateString);
  if (!date) return '-';
  
  return date.toLocaleString(locale);
}

/**
 * Форматирует UTC дату в локальную дату и время (короткий формат)
 * 
 * @param {string} utcDateString - Дата в UTC
 * @param {string} locale - Локаль (по умолчанию 'ru-RU')
 * @returns {string} - Отформатированная дата и время (без секунд)
 * 
 * @example
 * // В Москве (UTC+3):
 * formatDateTimeShort("2025-01-04T14:00:00") // "04.01.2025, 17:00"
 */
export function formatDateTimeShort(utcDateString, locale = 'ru-RU') {
  const date = parseUTCDate(utcDateString);
  if (!date) return '-';
  
  return date.toLocaleString(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

export function utcToLocalInputValue(utcDateString) {
  if (!utcDateString) return "";

  const date = new Date(
    utcDateString.endsWith("Z")
      ? utcDateString
      : utcDateString + "Z"
  );

  // ⚠️ ВАЖНО: toISOString() всегда UTC,
  // поэтому сначала компенсируем offset
  const localDate = new Date(
    date.getTime() - date.getTimezoneOffset() * 60000
  );

  return localDate.toISOString().slice(0, 16);
}

/**
 * Форматирует UTC дату в локальное время (только время)
 * 
 * @param {string} utcDateString - Дата в UTC
 * @param {string} locale - Локаль (по умолчанию 'ru-RU')
 * @returns {string} - Отформатированное время
 * 
 * @example
 * // В Москве (UTC+3):
 * formatTime("2025-01-04T14:00:00") // "17:00:00"
 */
export function formatTime(utcDateString, locale = 'ru-RU') {
  const date = parseUTCDate(utcDateString);
  if (!date) return '-';
  
  return date.toLocaleTimeString(locale);
}

/**
 * Конвертирует локальную дату пользователя в UTC для отправки на сервер
 * 
 * @param {Date|string} localDate - Локальная дата (Date объект или строка из input)
 * @returns {string} - Дата в UTC формате "2025-01-04T14:00:00" для backend
 * 
 * @example
 * // Пользователь в Москве (UTC+3) выбирает 17:00 локального времени
 * const input = "2025-01-04T17:00";
 * const utc = toUTCString(input); // "2025-01-04T14:00:00" (14:00 UTC)
 */
export function toUTCString(localDate) {
  if (!localDate) return null;
  
  const date = localDate instanceof Date ? localDate : new Date(localDate);
  
  // Конвертируем в UTC и форматируем без 'Z' (как ожидает Spring Boot LocalDateTime)
  return date.toISOString().slice(0, 19); // "2025-01-04T14:00:00"
}

/**
 * Получает текущее локальное время пользователя в UTC формате для сервера
 * 
 * @returns {string} - Текущая дата в UTC формате
 * 
 * @example
 * const now = nowUTC(); // "2025-01-04T14:00:00" (UTC)
 */
export function nowUTC() {
  return toUTCString(new Date());
}

/**
 * Проверяет, прошла ли дата (сравнивает с текущим временем)
 * 
 * @param {string} utcDateString - Дата в UTC
 * @returns {boolean} - true если дата в прошлом
 * 
 * @example
 * isPast("2020-01-01T12:00:00") // true
 * isPast("2030-01-01T12:00:00") // false
 */
export function isPast(utcDateString) {
  const date = parseUTCDate(utcDateString);
  if (!date) return false;
  
  return date < new Date();
}

/**
 * Проверяет, будущая ли дата
 * 
 * @param {string} utcDateString - Дата в UTC
 * @returns {boolean} - true если дата в будущем
 */
export function isFuture(utcDateString) {
  const date = parseUTCDate(utcDateString);
  if (!date) return false;
  
  return date > new Date();
}

/**
 * Получает часовой пояс пользователя
 * 
 * @returns {string} - Часовой пояс, например "Europe/Moscow"
 */
export function getUserTimezone() {
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

/**
 * Получает смещение часового пояса пользователя относительно UTC (в часах)
 * 
 * @returns {number} - Смещение в часах, например +3 для Москвы, -5 для Нью-Йорка
 */
export function getTimezoneOffset() {
  return -new Date().getTimezoneOffset() / 60;
}

