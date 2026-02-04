package ru.practicum.comment.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.dto.CommentShortDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;

import java.util.List;

public interface CommentService {
    CommentFullDto postComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    void deleteComment(Long userId, Long commentId);

    CommentFullDto patchComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    CommentShortDto getComment(Long userId, Long commentId);

    List<CommentShortDto> getComments(Long userId, Long eventId, Pageable pageable);
}