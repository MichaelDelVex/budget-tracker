package com.budgettracker.domain.tag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Test
    void savesAndFindsTagByName() {
        String name = "Shared " + UUID.randomUUID();
        tagRepository.save(new Tag(name, "#2F855A"));

        assertThat(tagRepository.findByName(name))
            .isPresent()
            .get()
            .extracting(Tag::getColor)
            .isEqualTo("#2F855A");
    }
}
