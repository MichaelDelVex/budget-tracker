package com.budgettracker.domain.tag;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Integer> {

    List<Tag> findAllByOrderByNameAsc();

    Optional<Tag> findByName(String name);
}
