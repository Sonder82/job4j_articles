package ru.job4j.articles.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.job4j.articles.model.Article;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ArticleStore implements Store<Article>, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleStore.class.getSimpleName());

    private final Properties properties;

    private Connection connection;

    public ArticleStore(Properties properties) {
        this.properties = properties;
        initConnection();
        initScheme();
    }

    private void initConnection() {
        LOGGER.info("Создание подключения к БД статей");
        try {
            connection = DriverManager.getConnection(
                    properties.getProperty("url"),
                    properties.getProperty("username"),
                    properties.getProperty("password")
            );
        } catch (SQLException throwables) {
            LOGGER.error("Не удалось выполнить операцию: { }", throwables.getCause());
            throw new IllegalStateException();
        }
    }

    private void initScheme() {
        LOGGER.info("Инициализация таблицы статей");
        try (var statement = connection.createStatement()) {
            statement.execute(Files.readString(Path.of("db/scripts", "articles.sql")));
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
    }

    @Override
    public Article save(Article model) {
        LOGGER.info("Сохранение статьи");
        try (var statement = connection.prepareStatement("insert into articles(text) values(?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, model.getText());
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
    public List<Article> findAll() {
        LOGGER.info("Загрузка всех статей");
        WeakReference<List<Article>> articles = new WeakReference<>(new ArrayList<>());
        List<Article> list = articles.get();
        if (list == null) {
            try (var statement = connection.prepareStatement("select * from articles")) {
                try (ResultSet selection = statement.executeQuery()) {
                    while (selection.next()) {
                        list.add(new Article(
                                selection.getInt("id"),
                                selection.getString("text")
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
