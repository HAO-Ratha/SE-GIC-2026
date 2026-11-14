package edu.itc.se.shop_jpa.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.itc.se.shop_jpa.domain.Tag;
import edu.itc.se.shop_jpa.repository.TagRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
class FlushOrderTest {

    @Autowired
    TagRepository tags;

    @Test
    @Transactional
    void flushOrderCanCauseUniqueConstraintViolation() {
        Tag oldTag = tags.saveAndFlush(new Tag("sale"));

        assertThatThrownBy(() -> {
            tags.delete(oldTag);
            tags.save(new Tag("sale"));
            tags.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void flushBetweenOperationsWorks() {
        Tag oldTag = tags.saveAndFlush(new Tag("sale"));

        tags.delete(oldTag);
        tags.flush();

        tags.save(new Tag("sale"));
        tags.flush();
    }
}