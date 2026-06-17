package com.budgettracker.tag;

import com.budgettracker.domain.tag.Tag;
import com.budgettracker.domain.tag.TagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listTags() {
        return tagRepository.findAllByOrderByNameAsc().stream()
            .map(TagResponse::from)
            .toList();
    }

    @Transactional
    public TagResponse createTag(TagRequest request) {
        Tag tag = new Tag(request.name(), request.color());
        return TagResponse.from(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse updateTag(Integer id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new TagNotFoundException(id));

        tag.update(request.name(), request.color());
        return TagResponse.from(tag);
    }

    @Transactional
    public void deleteTag(Integer id) {
        if (!tagRepository.existsById(id)) {
            throw new TagNotFoundException(id);
        }

        tagRepository.deleteById(id);
    }
}
