package com.budgettracker.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.budgettracker.domain.tag.Tag;
import com.budgettracker.domain.tag.TagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    @Test
    void listsTags() {
        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(List.of(new Tag("Tax", "#336699")));

        assertThat(tagService.listTags())
            .extracting(TagResponse::name)
            .containsExactly("Tax");
    }

    @Test
    void createsTag() {
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tagService.createTag(new TagRequest("Work", "#112233"));

        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(captor.capture());
        assertThat(captor.getValue().getColor()).isEqualTo("#112233");
    }

    @Test
    void updatesTag() {
        Tag tag = new Tag("Old", "#000000");
        when(tagRepository.findById(1)).thenReturn(Optional.of(tag));

        TagResponse response = tagService.updateTag(1, new TagRequest("New", "#FFFFFF"));

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.color()).isEqualTo("#FFFFFF");
    }

    @Test
    void throwsWhenUpdatingMissingTag() {
        when(tagRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.updateTag(99, new TagRequest("New", "#FFFFFF")))
            .isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void deletesTag() {
        when(tagRepository.existsById(1)).thenReturn(true);

        tagService.deleteTag(1);

        verify(tagRepository).deleteById(1);
    }
}
