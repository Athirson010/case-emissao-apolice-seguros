package io.github.athirson010.adapters.in.web.exception;

import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.exception.InvalidCancellationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler - Testes Unitários")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Deve retornar 400 ao tratar InvalidCancellationException para apólice cancelada")
    void shouldReturn400ForCancelledPolicy() {
        InvalidCancellationException exception =
                new InvalidCancellationException("test-id", PolicyStatus.CANCELED);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleInvalidCancellationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Cancelamento Inválido");
        assertThat(response.getBody().getMessage()).contains("já está cancelada");
    }

    @Test
    @DisplayName("Deve retornar 400 ao tratar InvalidCancellationException para apólice rejeitada")
    void shouldReturn400ForRejectedPolicy() {
        InvalidCancellationException exception =
                new InvalidCancellationException("test-id", PolicyStatus.REJECTED);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleInvalidCancellationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Cancelamento Inválido");
        assertThat(response.getBody().getMessage()).contains("foi rejeitada");
    }

    @Test
    @DisplayName("Deve retornar 400 ao tratar IllegalArgumentException")
    void shouldReturn400ForIllegalArgumentException() {
        IllegalArgumentException exception =
                new IllegalArgumentException("Proposta não encontrada");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleIllegalArgumentException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Proposta não encontrada");
    }

    @Test
    @DisplayName("Deve retornar 400 ao tratar MethodArgumentNotValidException")
    void shouldReturn400ForValidationException() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "testObject");

        bindingResult.addError(
                new FieldError("testObject", "fieldName", "Campo obrigatório")
        );

        Method method = DummyController.class.getDeclaredMethod("dummyMethod", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleValidationExceptions(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Validation Error");
        assertThat(response.getBody().getErrors())
                .containsEntry("fieldName", "Campo obrigatório");
    }

    @Test
    @DisplayName("Deve retornar 500 ao tratar Exception genérica")
    void shouldReturn500ForGenericException() {
        Exception exception = new Exception("Erro inesperado");

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleGenericException(exception);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError())
                .isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage())
                .contains("Ocorreu um erro inesperado");
    }

    @Test
    @DisplayName("Deve incluir timestamp em todas as respostas de erro")
    void shouldIncludeTimestampInAllErrorResponses() {
        InvalidCancellationException exception =
                new InvalidCancellationException("test-id", PolicyStatus.CANCELED);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleInvalidCancellationException(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    // Controller dummy só pra criar MethodParameter válido
    static class DummyController {
        @SuppressWarnings("unused")
        void dummyMethod(String param) {
        }
    }
}
