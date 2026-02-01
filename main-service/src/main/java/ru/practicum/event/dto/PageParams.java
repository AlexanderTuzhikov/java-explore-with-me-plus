package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageParams {
    private Integer from = 0;
    private Integer size = 10;

    public Integer getFrom() {
        return (from == null || from < 0) ? 0 : from;
    }

    public Integer getSize() {
        return (size == null || size <= 0) ? 10 : size;
    }
}
