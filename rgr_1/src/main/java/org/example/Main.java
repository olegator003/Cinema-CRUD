package org.example;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/cinemas";
        String user = "root";
        String password = "1337";

        try (DatabaseManager dbManager = new DatabaseManager(url, user, password)) {
            dbManager.createTables();
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("=== Меню ===");
                System.out.println("1. Cinema");
                System.out.println("2. Hall");
                System.out.println("3. Movie");
                System.out.println("4. Session");
                System.out.println("5. Seat");
                System.out.println("6. Ticket");
                System.out.println("7. Exit");
                System.out.print("Выберите таблицу для работы: ");

                while (!scanner.hasNextInt()) {
                    System.out.println("Ошибка! Пожалуйста, введите число.");
                    scanner.next();
                }

                int tableChoice = scanner.nextInt();
                scanner.nextLine(); // Считываем остаток строки

                switch (tableChoice) {
                    case 1 -> handleCinemaMenu(dbManager, scanner);
                    case 2 -> handleHallMenu(dbManager, scanner);
                    case 3 -> handleMovieMenu(dbManager, scanner);
                    case 4 -> handleSessionMenu(dbManager, scanner);
                    case 5 -> handleSeatMenu(dbManager, scanner); // Добавим обработку мест
                    case 6 -> handleTicketMenu(dbManager, scanner); // Добавим обработку билетов
                    case 7 -> {
                        System.out.println("Выход...");
                        return;
                    }
                    default -> System.out.println("Неверный выбор. Попробуйте снова.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Обработчик меню для работы с местами
    private static void handleSeatMenu(DatabaseManager dbManager, Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("\n=== Меню мест ===");
            System.out.println("1. Добавить место");
            System.out.println("2. Показать все места");
            System.out.println("3. Назад");
            System.out.print("Ваш выбор: ");

            String input = scanner.nextLine();
            if (!input.matches("\\d+")) {  // Проверяем, что ввод только цифра
                System.out.println("Неверный ввод. Пожалуйста, введите число.");
                continue;
            }

            int choice = Integer.parseInt(input);
            switch (choice) {
                case 1 -> {
                    try {
                        System.out.print("Введите номер места: ");
                        int seatNumber = getValidInteger(scanner);

                        System.out.print("Введите название кинотеатра: ");
                        String cinemaName = getNonEmptyString(scanner, "Название кинотеатра не может быть пустым.");

                        System.out.print("Введите адрес кинотеатра: ");
                        String address = getNonEmptyString(scanner, "Адрес кинотеатра не может быть пустым.");

                        System.out.print("Введите номер зала: ");
                        int hallNumber = getValidInteger(scanner);

                        dbManager.insertSeat(seatNumber, cinemaName, address, hallNumber);
                        System.out.println("Место добавлено.");
                    } catch (SQLException e) {
                        System.err.println("Ошибка при добавлении места: " + e.getMessage());
                    }
                }
                case 2 -> {
                    try {
                        List<String> seats = dbManager.getAllSeats();
                        System.out.println("Список мест:");
                        seats.forEach(System.out::println);
                    } catch (SQLException e) {
                        System.err.println("Ошибка при получении списка мест: " + e.getMessage());
                    }
                }
                case 3 -> {
                    System.out.println("Возврат в главное меню...");
                    return;
                }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    // Обработчик меню для работы с билетами
    private static void handleTicketMenu(DatabaseManager dbManager, Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("\n=== Меню билетов ===");
            System.out.println("1. Добавить билет");
            System.out.println("2. Показать все билеты");
            System.out.println("3. Назад");
            System.out.print("Ваш выбор: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Считываем остаток строки

            switch (choice) {
                case 1 -> {
                    System.out.print("Введите номер места: ");
                    int seatNumber = scanner.nextInt();
                    System.out.print("Введите номер зала: ");
                    int hallNumber = scanner.nextInt();
                    System.out.print("Введите название кинотеатра: ");
                    scanner.nextLine();  // Очистка остаточной строки
                    String cinemaName = scanner.nextLine();
                    System.out.print("Введите адрес кинотеатра: ");
                    String address = scanner.nextLine();
                    System.out.print("Введите номер сеанса: ");
                    int sessionNumber = scanner.nextInt();
                    System.out.print("Введите цену билета: ");
                    double price = scanner.nextDouble();

                    // Проверка существования сессии
                    String checkSessionQuery = "SELECT COUNT(*) FROM session WHERE session_number = ?";
                    try (PreparedStatement checkStmt = dbManager.getConnection().prepareStatement(checkSessionQuery)) {
                        checkStmt.setInt(1, sessionNumber);
                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) == 0) {
                                System.out.println("Сессия с номером " + sessionNumber + " не существует.");
                                return;  // Возвращаемся в меню
                            }
                        }
                    }

                    dbManager.insertTicket(seatNumber, hallNumber, cinemaName, address, sessionNumber, price);

                    System.out.println("Билет добавлен.");
                }
                case 2 -> {
                    List<String> tickets = dbManager.getAllTickets();
                    System.out.println("Список билетов:");
                    tickets.forEach(System.out::println);
                }
                case 3 -> {
                    System.out.println("Возврат в главное меню...");
                    return;
                }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    // Обработчик меню для работы с кинотеатрами
    private static void handleCinemaMenu(DatabaseManager dbManager, Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("\n=== Меню кинотеатров ===");
            System.out.println("1. Добавить кинотеатр");
            System.out.println("2. Показать все кинотеатры");
            System.out.println("3. Обновить кинотеатр");
            System.out.println("4. Удалить кинотеатр");
            System.out.println("5. Назад");
            System.out.print("Ваш выбор: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Считываем остаток строки

            switch (choice) {
                case 1 -> {
                    System.out.print("Введите название кинотеатра: ");
                    String name = scanner.nextLine();
                    System.out.print("Введите адрес кинотеатра: ");
                    String address = scanner.nextLine();
                    dbManager.insertCinema(name, address);
                    System.out.println("Кинотеатр добавлен.");
                }
                case 2 -> {
                    List<String> cinemas = dbManager.getAllCinemas();
                    System.out.println("Список кинотеатров:");
                    cinemas.forEach(System.out::println);
                }
                case 3 -> {
                    System.out.print("Введите текущее название кинотеатра: ");
                    String oldName = scanner.nextLine();
                    System.out.print("Введите текущий адрес кинотеатра: ");
                    String oldAddress = scanner.nextLine();
                    System.out.print("Введите новое название кинотеатра: ");
                    String newName = scanner.nextLine();
                    System.out.print("Введите новый адрес кинотеатра: ");
                    String newAddress = scanner.nextLine();
                    dbManager.updateCinema(oldName, oldAddress, newName, newAddress);
                    System.out.println("Кинотеатр обновлен.");
                }
                case 4 -> {
                    System.out.print("Введите название кинотеатра: ");
                    String name = scanner.nextLine();
                    System.out.print("Введите адрес кинотеатра: ");
                    String address = scanner.nextLine();
                    dbManager.deleteCinema(name, address);
                    System.out.println("Кинотеатр удален.");
                }
                case 5 -> {
                    System.out.println("Возврат в главное меню...");
                    return;
                }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    // Обработчик меню для работы с залами
    private static void handleHallMenu(DatabaseManager dbManager, Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("\n=== Меню залов ===");
            System.out.println("1. Добавить зал");
            System.out.println("2. Показать все залы");
            System.out.println("3. Обновить зал");
            System.out.println("4. Удалить зал");
            System.out.println("5. Назад");
            System.out.print("Ваш выбор: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Считываем остаток строки

            switch (choice) {
                case 1 -> {
                    System.out.print("Введите название кинотеатра: ");
                    String cinemaName = scanner.nextLine();
                    System.out.print("Введите адрес кинотеатра: ");
                    String address = scanner.nextLine();
                    System.out.print("Введите номер зала: ");
                    int hallNumber = scanner.nextInt();
                    scanner.nextLine(); // Считываем остаток строки
                    System.out.print("Введите название зала: ");
                    String hallName = scanner.nextLine();
                    dbManager.insertHall(cinemaName, address, hallNumber, hallName);
                    System.out.println("Зал добавлен.");
                }
                case 2 -> {
                    List<String> halls = dbManager.getAllHalls();
                    System.out.println("Список залов:");
                    halls.forEach(System.out::println);
                }
                case 3 -> {
                    System.out.print("Введите название кинотеатра: ");
                    String cinemaName = scanner.nextLine();
                    System.out.print("Введите адрес кинотеатра: ");
                    String address = scanner.nextLine();
                    System.out.print("Введите номер зала: ");
                    int hallNumber = scanner.nextInt();
                    scanner.nextLine(); // Считываем остаток строки
                    System.out.print("Введите новое название зала: ");
                    String newHallName = scanner.nextLine();
                    dbManager.updateHall(cinemaName, address, hallNumber, newHallName);
                    System.out.println("Зал обновлен.");
                }
                case 4 -> {
                    System.out.print("Введите название кинотеатра: ");
                    String cinemaName = scanner.nextLine();
                    System.out.print("Введите адрес кинотеатра: ");
                    String address = scanner.nextLine();
                    System.out.print("Введите номер зала: ");
                    int hallNumber = scanner.nextInt();
                    scanner.nextLine(); // Считываем остаток строки
                    dbManager.deleteHall(cinemaName, address, hallNumber);
                    System.out.println("Зал удален.");
                }
                case 5 -> {
                    System.out.println("Возврат в главное меню...");
                    return;
                }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    // Обработчик меню для работы с фильмами
    private static void handleMovieMenu(DatabaseManager dbManager, Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("\n=== Меню фильмов ===");
            System.out.println("1. Добавить фильм");
            System.out.println("2. Показать все фильмы");
            System.out.println("3. Обновить фильм");
            System.out.println("4. Удалить фильм");
            System.out.println("5. Назад");
            System.out.print("Ваш выбор: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Считываем остаток строки

            switch (choice) {
                case 1 -> {
                    System.out.print("Введите название фильма: ");
                    String movieName = scanner.nextLine();
                    System.out.print("Введите жанр фильма: ");
                    String genre = scanner.nextLine();
                    dbManager.insertMovie(movieName, genre);
                    System.out.println("Фильм добавлен.");
                }
                case 2 -> {
                    List<String> movies = dbManager.getAllMovies();
                    System.out.println("Список фильмов:");
                    movies.forEach(System.out::println);
                }
                case 3 -> {
                    System.out.print("Введите текущее название фильма: ");
                    String oldName = scanner.nextLine();
                    System.out.print("Введите новое название фильма: ");
                    String newName = scanner.nextLine();
                    System.out.print("Введите жанр фильма: ");
                    String genre = scanner.nextLine();
                    dbManager.updateMovie(oldName, newName, genre);
                    System.out.println("Фильм обновлен.");
                }
                case 4 -> {
                    System.out.print("Введите название фильма: ");
                    String movieName = scanner.nextLine();
                    dbManager.deleteMovie(movieName);
                    System.out.println("Фильм удален.");
                }
                case 5 -> {
                    System.out.println("Возврат в главное меню...");
                    return;
                }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }


    private static void handleSessionMenu(DatabaseManager dbManager, Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("\n=== Session Menu ===");
            System.out.println("1. Добавить сеанс");
            System.out.println("2. Показать все сеансы");
            System.out.println("3. Удалить сеанс");
            System.out.println("4. Обновить сеанс");
            System.out.println("5. Назад");
            System.out.print("Ваш выбор: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Считываем остаток строки

            switch (choice) {
                case 1 -> {
                    System.out.print("Введите название фильма: ");
                    String movieName = scanner.nextLine();
                    System.out.print("Введите номер зала: ");
                    int hallNumber = scanner.nextInt();
                    scanner.nextLine(); // Считываем остаток строки
                    System.out.print("Введите название кинотеатра: ");
                    String cinemaName = scanner.nextLine();
                    System.out.print("Введите адрес кинотеатра: ");
                    String address = scanner.nextLine();
                    System.out.print("Введите дату и время (yyyy-MM-dd HH:mm:ss): ");
                    String dateTimeString = scanner.nextLine();
                    try {
                        Timestamp dateTime = Timestamp.valueOf(dateTimeString);
                        dbManager.insertSession(movieName, hallNumber, cinemaName, address, dateTime);
                        System.out.println("Сеанс добавлен.");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Некорректный формат даты и времени. Попробуйте снова.");
                    }
                }
                case 2 -> {
                    List<String> sessions = dbManager.getAllSessions();
                    System.out.println("Список сеансов:");
                    sessions.forEach(System.out::println);
                }
                case 3 -> {
                    System.out.print("Введите номер сеанса: ");
                    int sessionNumber = scanner.nextInt();
                    scanner.nextLine(); // Считываем остаток строки
                    dbManager.deleteSession(sessionNumber);
                    System.out.println("Сеанс удален.");
                }
                case 4 -> {
                    System.out.print("Введите номер сеанса для обновления: ");
                    int sessionNumber = scanner.nextInt();
                    scanner.nextLine(); // Считываем остаток строки

                    System.out.print("Введите новое название фильма (или оставьте пустым, чтобы не изменять): ");
                    String movieName = scanner.nextLine();

                    System.out.print("Введите новый номер зала (или оставьте пустым, чтобы не изменять): ");
                    String hallNumberInput = scanner.nextLine();
                    Integer hallNumber = hallNumberInput.isEmpty() ? null : Integer.valueOf(hallNumberInput);


                    System.out.print("Введите новую дату и время (yyyy-MM-dd HH:mm:ss) (или оставьте пустым, чтобы не изменять): ");
                    String dateTimeInput = scanner.nextLine();
                    Timestamp dateTime = dateTimeInput.isEmpty() ? null : Timestamp.valueOf(dateTimeInput);

                    dbManager.updateSession(sessionNumber, movieName, hallNumber, dateTime);
                    System.out.println("Сеанс обновлен.");
                }

                case 5 -> {
                    System.out.println("Возврат в главное меню...");
                    return;
                }
                default -> System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }
    private static int getValidInteger(Scanner scanner) {
        while (true) {
            String input = scanner.nextLine();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print("Неверный ввод. Пожалуйста, введите целое число: ");
            }
        }
    }

    private static String getNonEmptyString(Scanner scanner, String errorMessage) {
        while (true) {
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.print(errorMessage + " Повторите ввод: ");
        }
    }
}