package ru.practicum.compilations.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.CompilationSearchParam;
import ru.practicum.compilations.service.CompilationService;

import java.util.List;

@RestController
@RequestMapping(path = "/compilations")
@RequiredArgsConstructor
public class CompilationController {
    private static final String PATH = "comp-id";
    private final CompilationService compilationService;

    @GetMapping("/{comp-id}")
    CompilationDto get(@PathVariable(PATH) @Positive long compId) {
        CompilationDto compilationDto = compilationService.get(compId);

        return compilationDto;
    }

    @GetMapping
    List<CompilationDto> getCompilations(@RequestParam(required = false, name = "pinned") Boolean pinned,
                                         @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
                                         @RequestParam(name = "size", defaultValue = "10") @PositiveOrZero int size) {
        // Защита от некорректных значений
        if (size == 0) size = 10;
        int effectiveFrom = Math.max(from, 0);
        int effectiveSize = Math.max(size, 1);

        CompilationSearchParam params = new CompilationSearchParam(pinned, effectiveFrom, effectiveSize);
        return compilationService.getCompilations(params);
    }
}
