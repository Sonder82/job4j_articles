package ru.job4j.articles.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.job4j.articles.model.Word;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WordStore implements Store<Word>, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WordStore.class.getSimpleName());

    private final Properties properties;

    private Connection connection;

    public WordStore(Properties properties) {
        this.properties = properties;
        initConnection();
        initScheme();
        initWords();
    }

    private void initConnection() {
        LOGGER.info("Подключение к базе данных слов");
        try {
            connection = DriverManager.getConnection(
                    properties.getProperty("url"),
                    properties.getProperty("username"),
                    properties.getProperty("password")
            );
        } catch (SQLException e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
    }

    private void initScheme() {
        LOGGER.info("Создание схемы таблицы слов");
        try (var statement = connection.createStatement()) {
            statement.execute(Files.readString(Path.of("db/scripts", "dictionary.sql")));
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
    }

    private void initWords() {
        LOGGER.info("Заполнение таблицы слов");
        try (var statement = connection.createStatement()) {
            statement.executeLargeUpdate(Files.readString(Path.of("db/scripts", "words.sql")));
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
    }

    @Override
    public Word save(Word model) {
        LOGGER.info("Добавление слова в базу данных");
        try (var statement = connection.prepareStatement("insert into dictionary(word) values(?);",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, model.getValue());
            statement.executeUpdate();
            try (ResultSet genKey = statement.getGeneratedKeys()) {
                if (genKey.next()) {
                    model.setId(genKey.getInt(1));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
        return model;
    }

    @Override
    public List<Word> findAll() {
        LOGGER.info("Загрузка всех слов");
        WeakReference<List<Word>> words = new WeakReference<>(new ArrayList<>());
        List<Word> list = words.get();
        if (list == null) {
            try (var statement = connection.prepareStatement("select * from dictionary")) {
                try (ResultSet selection = statement.executeQuery()) {
                    while (selection.next()) {
                        list.add(new Word(
                                selection.getInt("id"),
                                selection.getString("word")
                        ));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
                throw new IllegalStateException();
            }
        }
        return list;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

}
