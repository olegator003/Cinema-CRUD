package org.example;

import lombok.Getter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class DatabaseManager implements AutoCloseable {

    private final Connection connection;

    // Constructor to initialize connection
    public DatabaseManager(String url, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
    }

    // Method to create tables
    public void createTables() throws SQLException {
        String[] createTableQueries = {
                "CREATE TABLE IF NOT EXISTS cinema (" +
                        "    cinema_name VARCHAR(255) NOT NULL," +
                        "    address VARCHAR(255) NOT NULL," +
                        "    PRIMARY KEY (cinema_name, address)" +
                        ");",
                "CREATE TABLE IF NOT EXISTS hall (" +
                        "    cinema_name VARCHAR(255) NOT NULL," +
                        "    address VARCHAR(255) NOT NULL," +
                        "    hall_number INT NOT NULL," +
                        "    hall_name VARCHAR(255)," +
                        "    PRIMARY KEY (cinema_name, address, hall_number)," +
                        "    FOREIGN KEY (cinema_name, address) " +
                        "    REFERENCES cinema (cinema_name, address) " +
                        "    ON DELETE CASCADE " +
                        "    ON UPDATE CASCADE" +
                        ");",
                "CREATE TABLE IF NOT EXISTS seat (" +
                        "    seat_number INT NOT NULL," +
                        "    cinema_name VARCHAR(255) NOT NULL," +
                        "    address VARCHAR(255) NOT NULL," +
                        "    hall_number INT NOT NULL," +
                        "    PRIMARY KEY (seat_number, cinema_name, address, hall_number)," +
                        "    FOREIGN KEY (cinema_name, address, hall_number) " +
                        "    REFERENCES hall (cinema_name, address, hall_number) " +
                        "    ON UPDATE CASCADE" +
                        "    ON DELETE CASCADE " +
                        ");",
                "CREATE TABLE IF NOT EXISTS movie (" +
                        "    movie_name VARCHAR(255) NOT NULL," +
                        "    genre VARCHAR(255)," +
                        "    PRIMARY KEY (movie_name)" +
                        ");",
                "CREATE TABLE IF NOT EXISTS session (" +
                        "    session_number INT AUTO_INCREMENT NOT NULL," +
                        "    movie_name VARCHAR(255) NOT NULL," +
                        "    hall_number INT NOT NULL," +
                        "    cinema_name VARCHAR(255) NOT NULL," +
                        "    address VARCHAR(255) NOT NULL," +
                        "    dateTime TIMESTAMP NOT NULL," +
                        "    PRIMARY KEY (session_number)," +
                        "    UNIQUE (movie_name, hall_number, cinema_name, address, dateTime)," +
                        "    FOREIGN KEY (movie_name) REFERENCES movie (movie_name)," +
                        "    FOREIGN KEY (cinema_name, address, hall_number) " +
                        "    REFERENCES hall (cinema_name, address, hall_number) " +
                        "    ON UPDATE CASCADE" +
                        "    ON DELETE CASCADE " +
                        ");",
                "CREATE TABLE IF NOT EXISTS ticket (" +
                        "    ticket_number INT AUTO_INCREMENT NOT NULL," +
                        "    seat_number INT NOT NULL," +
                        "    hall_number INT NOT NULL," +
                        "    cinema_name VARCHAR(255) NOT NULL," +
                        "    address VARCHAR(255) NOT NULL," +
                        "    session_number INT NOT NULL," +
                        "    price DECIMAL(10, 2)," +
                        "    PRIMARY KEY (ticket_number)," +
                        "    FOREIGN KEY (seat_number, cinema_name, address, hall_number) " +
                        "    REFERENCES seat (seat_number, cinema_name, address, hall_number)," +
                        "    FOREIGN KEY (session_number) REFERENCES session (session_number)" +
                        "    ON UPDATE CASCADE" +
                        "    ON DELETE CASCADE " +
        ");"
        };


        // Execute each table creation query
        try (Statement stmt = connection.createStatement()) {
            for (String query : createTableQueries) {
                stmt.execute(query);
            }
        }
    }



    // CRUD for 'cinema'
    public void insertCinema(String cinemaName, String address) throws SQLException {
        String query = "INSERT INTO cinema (cinema_name, address) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, cinemaName);
            preparedStatement.setString(2, address);
            preparedStatement.executeUpdate();
        }
    }

    public List<String> getAllCinemas() throws SQLException {
        String query = "SELECT * FROM cinema";
        List<String> cinemas = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                String cinema = "Название: " + resultSet.getString("cinema_name") + ", по адресу: " + resultSet.getString("address");
                cinemas.add(cinema);
            }
        }
        return cinemas;
    }

    public void updateCinema(String oldName, String oldAddress, String newName, String newAddress) throws SQLException {
        // Временно отключим проверки внешних ключей
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
        }

        // Выполним обновление в таблице cinema
        String updateCinemaQuery = "UPDATE cinema SET cinema_name = ?, address = ? WHERE cinema_name = ? AND address = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateCinemaQuery)) {
            ps.setString(1, newName);
            ps.setString(2, newAddress);
            ps.setString(3, oldName);
            ps.setString(4, oldAddress);
            ps.executeUpdate();
        }

        // Обновим все записи в таблице session, которые ссылаются на этот кинотеатр
        String updateSessionQuery = "UPDATE session SET cinema_name = ?, address = ? WHERE cinema_name = ? AND address = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateSessionQuery)) {
            ps.setString(1, newName);
            ps.setString(2, newAddress);
            ps.setString(3, oldName);
            ps.setString(4, oldAddress);
            ps.executeUpdate();
        }

        // Обновим все записи в таблице seat, которые ссылаются на этот кинотеатр
        String updateSeatQuery = "UPDATE seat SET cinema_name = ?, address = ? WHERE cinema_name = ? AND address = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateSeatQuery)) {
            ps.setString(1, newName);
            ps.setString(2, newAddress);
            ps.setString(3, oldName);
            ps.setString(4, oldAddress);
            ps.executeUpdate();
        }

        // Обновим все записи в таблице ticket, которые ссылаются на этот кинотеатр
        String updateTicketQuery = "UPDATE ticket SET cinema_name = ?, address = ? WHERE cinema_name = ? AND address = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateTicketQuery)) {
            ps.setString(1, newName);
            ps.setString(2, newAddress);
            ps.setString(3, oldName);
            ps.setString(4, oldAddress);
            ps.executeUpdate();
        }

        // Включим проверки внешних ключей обратно
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }


    public void deleteCinema(String cinemaName, String address) throws SQLException {
        // Сначала удаляем все билеты, которые ссылаются на сессии
        String deleteTicketQuery = "DELETE FROM ticket WHERE session_number IN " +
                "(SELECT session_number FROM session WHERE cinema_name = ? AND address = ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteTicketQuery)) {
            preparedStatement.setString(1, cinemaName);
            preparedStatement.setString(2, address);
            preparedStatement.executeUpdate();
        }

        // Затем удаляем все сессии, которые ссылаются на этот кинотеатр
        String deleteSessionQuery = "DELETE FROM session WHERE cinema_name = ? AND address = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteSessionQuery)) {
            preparedStatement.setString(1, cinemaName);
            preparedStatement.setString(2, address);
            preparedStatement.executeUpdate();
        }

        // Удаляем записи из таблицы hall, которые ссылаются на этот кинотеатр
        String deleteHallQuery = "DELETE FROM hall WHERE cinema_name = ? AND address = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteHallQuery)) {
            preparedStatement.setString(1, cinemaName);
            preparedStatement.setString(2, address);
            preparedStatement.executeUpdate();
        }

        // Удаляем записи из таблицы seat, которые ссылаются на этот кинотеатр
        String deleteSeatQuery = "DELETE FROM seat WHERE cinema_name = ? AND address = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteSeatQuery)) {
            preparedStatement.setString(1, cinemaName);
            preparedStatement.setString(2, address);
            preparedStatement.executeUpdate();
        }

        // Наконец, удаляем сам кинотеатр из таблицы cinema
        String deleteCinemaQuery = "DELETE FROM cinema WHERE cinema_name = ? AND address = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteCinemaQuery)) {
            preparedStatement.setString(1, cinemaName);
            preparedStatement.setString(2, address);
            preparedStatement.executeUpdate();
        }
    }

    public void insertHall(String cinemaName, String address, int hallNumber, String hallName) throws SQLException {
        String query = "INSERT INTO hall (cinema_name, address, hall_number, hall_name) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, cinemaName);
            preparedStatement.setString(2, address);
            preparedStatement.setInt(3, hallNumber);
            preparedStatement.setString(4, hallName);
            preparedStatement.executeUpdate();
        }
    }

    public List<String> getAllHalls() throws SQLException {
        String query = "SELECT * FROM hall";
        List<String> halls = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                String hall = "Название кинотеатра: " + resultSet.getString("cinema_name") + ", " +
                        "по адресу " + resultSet.getString("address") + ", " +
                       "Номер зала: " + resultSet.getInt("hall_number") + ", " +
                        "Название зала: " + resultSet.getString("hall_name");
                halls.add(hall);
            }
        }
        return halls;
    }

    public void updateHall(String cinemaName, String address, int hallNumber, String newHallName) throws SQLException {
        String query = "UPDATE hall SET hall_name = ? WHERE cinema_name = ? AND address = ? AND hall_number = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, newHallName);
            preparedStatement.setString(2, cinemaName);
            preparedStatement.setString(3, address);
            preparedStatement.setInt(4, hallNumber);
            preparedStatement.executeUpdate();
        }
    }

    public void deleteHall(String cinemaName, String address, int hallNumber) throws SQLException {
        String query = "DELETE FROM hall WHERE cinema_name = ? AND address = ? AND hall_number = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, cinemaName);
            preparedStatement.setString(2, address);
            preparedStatement.setInt(3, hallNumber);
            preparedStatement.executeUpdate();
        }
    }

    public void insertMovie(String movieName, String genre) throws SQLException {
        String query = "INSERT INTO movie (movie_name, genre) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, movieName);
            preparedStatement.setString(2, genre);
            preparedStatement.executeUpdate();
        }
    }

    public List<String> getAllMovies() throws SQLException {
        String query = "SELECT * FROM movie";
        List<String> movies = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                String movie = "Название фильма: " + resultSet.getString("movie_name") + ", жанр: " + resultSet.getString("genre");
                movies.add(movie);
            }
        }
        return movies;
    }

    public void updateMovie(String oldName, String newName, String genre) throws SQLException {
        String query = "UPDATE movie SET movie_name = ?, genre = ? WHERE movie_name = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, newName);
            preparedStatement.setString(2, genre);
            preparedStatement.setString(3, oldName);
            preparedStatement.executeUpdate();
        }
    }

    public void deleteMovie(String movieName) throws SQLException {
        String query = "DELETE FROM movie WHERE movie_name = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, movieName);
            preparedStatement.executeUpdate();
        }
    }

    public void insertSession(String movieName, int hallNumber, String cinemaName, String address, Timestamp dateTime) throws SQLException {
        String query = "INSERT INTO session (movie_name, hall_number, cinema_name, address, dateTime) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, movieName);
            preparedStatement.setInt(2, hallNumber);
            preparedStatement.setString(3, cinemaName);
            preparedStatement.setString(4, address);
            preparedStatement.setTimestamp(5, dateTime);
            preparedStatement.executeUpdate();
        }
    }

    public void updateSession(int sessionNumber, String movieName, Integer hallNumber, Timestamp dateTime) throws SQLException {
        // Строим строку SQL, добавляя только те поля, которые не null
        StringBuilder queryBuilder = new StringBuilder("UPDATE session SET ");
        boolean first = true;

        if (movieName != null && !movieName.isEmpty()) {
            if (!first) queryBuilder.append(", ");
            queryBuilder.append("movie_name = ?");
            first = false;
        }
        if (hallNumber != null) {
            if (!first) queryBuilder.append(", ");
            queryBuilder.append("hall_number = ?");
            first = false;
        }
        if (dateTime != null) {
            if (!first) queryBuilder.append(", ");
            queryBuilder.append("dateTime = ?");
        }

        queryBuilder.append(" WHERE session_number = ?");

        String query = queryBuilder.toString();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            int parameterIndex = 1;

            // Устанавливаем параметры в preparedStatement
            if (movieName != null && !movieName.isEmpty()) {
                preparedStatement.setString(parameterIndex++, movieName);
            }
            if (hallNumber != null) {
                preparedStatement.setInt(parameterIndex++, hallNumber);
            }
            if (dateTime != null) {
                preparedStatement.setTimestamp(parameterIndex++, dateTime);
            }

            // Устанавливаем номер сеанса
            preparedStatement.setInt(parameterIndex, sessionNumber);

            preparedStatement.executeUpdate();
        }
    }



    public List<String> getAllSessions() throws SQLException {
        String query = "SELECT * FROM session";
        List<String> sessions = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                String session ="Номер сеанса: " + resultSet.getInt("session_number") + ", " +
                        "название фильма: " + resultSet.getString("movie_name") + ", " +
                        "номер зала  " + resultSet.getInt("hall_number") + ", " +
                        "название кинотеатра  " + resultSet.getString("cinema_name") + ", " +
                        "по адресу  " + resultSet.getString("address") + ", " +
                        "дата и время " + resultSet.getTimestamp("dateTime") + ", ";
                sessions.add(session);
            }
        }
        return sessions;
    }

    public void deleteSession(int sessionNumber) throws SQLException {
        String query = "DELETE FROM session WHERE session_number = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, sessionNumber);
            preparedStatement.executeUpdate();
        }
    }

    public void insertSeat(int seatNumber, String cinemaName, String address, int hallNumber) throws SQLException {
        String query = "INSERT INTO seat (seat_number, cinema_name, address, hall_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, seatNumber);
            stmt.setString(2, cinemaName);
            stmt.setString(3, address);
            stmt.setInt(4, hallNumber);
            stmt.executeUpdate();
        }
    }

    public List<String> getAllSeats() throws SQLException {
        List<String> seats = new ArrayList<>();
        String query = "SELECT * FROM seat";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                seats.add("Место: " + rs.getString( "seat_number") + " - Кинотеатр: " + rs.getString("cinema_name") + ", по адресу: " + rs.getString("address"));
            }
        }
        return seats;
    }

    public void insertTicket(int seatNumber, int hallNumber, String cinemaName, String address, int sessionNumber, double price) throws SQLException {
        String query = "INSERT INTO ticket (seat_number, hall_number, cinema_name, address, session_number, price) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, seatNumber);
            stmt.setInt(2, hallNumber);
            stmt.setString(3, cinemaName);
            stmt.setString(4, address);
            stmt.setInt(5, sessionNumber);
            stmt.setDouble(6, price);
            stmt.executeUpdate();
        }
    }


    public List<String> getAllTickets() throws SQLException {
        List<String> tickets = new ArrayList<>();
        String query = "SELECT * FROM ticket";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                tickets.add("Номер билета: " + rs.getInt("ticket_number") +
                        ", место: " + rs.getInt("seat_number") +
                        ", цена: " + rs.getDouble("price") +
                        ", название кинотеатра " + rs.getString("cinema_name"));
            }
        }
        return tickets;
    }

    public void updateTicketForeignKeys(String oldCinemaName, String oldAddress, String newCinemaName, String newAddress) throws SQLException {
        String updateTicketQuery = "UPDATE ticket " +
                "SET cinema_name = ?, address = ? " +
                "WHERE cinema_name = ? AND address = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateTicketQuery)) {
            preparedStatement.setString(1, newCinemaName);
            preparedStatement.setString(2, newAddress);
            preparedStatement.setString(3, oldCinemaName);
            preparedStatement.setString(4, oldAddress);
            preparedStatement.executeUpdate();
        }
    }


    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}