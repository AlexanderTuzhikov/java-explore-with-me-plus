package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NewEndpointHitDto {
    @NotNull(message = "app is null")
    @NotBlank(message = "app is blank")
    private String app;
    @NotNull(message = "uri is null")
    @NotBlank(message = "uri is blank")
    private String uri;
    @NotNull(message = "ip is null")
    @NotBlank(message = "ip is blank")
    private String ip;
    @NotNull(message = "timestamp is null")
    @PastOrPresent(message = "timestamp is future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
