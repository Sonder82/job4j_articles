package ru.job4j.articles.service.generator;

import ru.job4j.articles.model.Article;
import ru.job4j.articles.model.Word;

import java.util.List;
import java.util.stream.Collectors;

public class RandomArticleGenerator implements ArticleGenerator {
    @Override
    public Article generate(List<Word> words) {
        return new Article(words.stream()
                .map(Word::getValue)
                .collect(Collectors.joining(" ")));
    }
}
