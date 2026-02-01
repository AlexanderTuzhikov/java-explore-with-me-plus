package ru.practicum.handler;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.practicum.handler.exception.ConflictException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String getStackTraceAsString(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    @ExceptionHandler(ru.practicum.handler.exception.BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleCustomBadRequestException(final ru.practicum.handler.exception.BadRequestException exception) {
        log.error("Custom BadRequestError: {}", exception.getMessage());

        return new ApiError(getStackTrace(exception), exception.getMessage(),
                "Incorrectly made request.", HttpStatus.BAD_REQUEST.toString(), LocalDateTime.now().format(FORMATTER));
    }

    @ExceptionHandler(org.apache.coyote.BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleCoyoteBadRequestException(final org.apache.coyote.BadRequestException exception) {
        log.error("Coyote BadRequestError: {}", exception.getMessage());

        return new ApiError(getStackTrace(exception), exception.getMessage(),
                "BadRequestError.", HttpStatus.BAD_REQUEST.toString(), LocalDateTime.now().format(FORMATTER));
    }

    @ExceptionHandler(ru.practicum.handler.exception.ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(final ru.practicum.handler.exception.ValidationException exception) {
        log.error("ValidationException: {}", exception.getMessage());

        return new ApiError(getStackTrace(exception), exception.getMessage(),
                "Validation error.", HttpStatus.BAD_REQUEST.toString(), LocalDateTime.now().format(FORMATTER));
    }

    @ExceptionHandler(ru.practicum.handler.exception.NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final ru.practicum.handler.exception.NotFoundException exception) {
        log.error("NotFoundError: {}", exception.getMessage());

        return new ApiError(getStackTrace(exception), exception.getMessage(),
                "The required object was not found.", HttpStatus.NOT_FOUND.toString(), LocalDateTime.now().format(FORMATTER));
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(final ConflictException exception) {
        log.error("ConflictException: {}", exception.getMessage());

        return new ApiError(
                getStackTrace(exception),
                exception.getMessage(),
                "For the requested operation the conditions are not met.",
                HttpStatus.CONFLICT.toString(),
                LocalDateTime.now().format(FORMATTER)
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleRequestParameterException(final MissingServletRequestParameterException exception) {
        log.error("RequestParameterError: {}", exception.getMessage());

        return new ApiError(getStackTrace(exception), exception.getMessage(),
                "Required request parameter is not present.", HttpStatus.BAD_REQUEST.toString(), LocalDateTime.now().format(FORMATTER));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleHandlerMethodValidationException(final HandlerMethodValidationException exception) {
        List<String> errors = exception.getAllValidationResults().stream()
                .map(result -> "Parameter: " + result.getMethodParameter().getParameterName() +
                        " Error: " + result.getResolvableErrors().get(0).getDefaultMessage())
                .collect(Collectors.toList());

        String errorMessage = String.join("; ", errors);

        log.error("HandlerMethodValidationError: {}", errorMessage);

        return new ApiError(getStackTrace(exception), errorMessage,
                "Incorrectly made request.", HttpStatus.BAD_REQUEST.toString(),
                LocalDateTime.now().format(FORMATTER));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleUniqueConstraint(final DataIntegrityViolationException exception) {
        String message = "Entity already exists. Unique constraint violation.";

        if (exception.getCause() instanceof ConstraintViolationException cve) {
            if (cve.getMessage().contains("UQ_CATEGORY_NAME")) {
                message = "Category with this name already exists";
            }

            if (cve.getMessage().contains("UQ_USER_EMAIL")) {
                message = "User with this email already exists";
            }
        }

        log.error("Conflict: {}", message);

        return new ApiError(
                getStackTrace(exception),
                message,
                "Integrity constraint has been violated.",
                HttpStatus.CONFLICT.toString(),
                LocalDateTime.now().format(FORMATTER)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationExceptions(final MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> "Field: " + error.getField() + " Error: " + error.getDefaultMessage()
                        + " Value: " + error.getRejectedValue())
                .toList();

        String errorMessage = String.join("; ", errors);

        log.error("ValidationError: {}", errorMessage);

        return new ApiError(getStackTrace(exception), errorMessage,
                "Incorrectly made request.", HttpStatus.BAD_REQUEST.toString(), LocalDateTime.now().format(FORMATTER));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception exception) {
        log.error("Server error: {}", exception.getMessage());

        return new ApiError(getStackTrace(exception), exception.getMessage(), "Server error",
                HttpStatus.INTERNAL_SERVER_ERROR.toString(), LocalDateTime.now().format(FORMATTER));

    }

    private List<String> getStackTrace(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        return List.of(stringWriter.toString());
    }
}
