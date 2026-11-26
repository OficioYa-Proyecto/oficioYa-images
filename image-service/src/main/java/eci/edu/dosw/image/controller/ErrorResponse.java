package eci.edu.dosw.image.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Respuesta estándar de error en el servicio de imágenes")
public record ErrorResponse(
        @Schema(description = "Código de estado HTTP", example = "400")
        int status,

        @Schema(description = "Tipo o nombre del error", example = "Bad Request")
        String error,

        @Schema(description = "Mensaje detallado para el cliente", example = "El archivo excede el tamaño máximo permitido de 5MB")
        String message,

        @Schema(description = "Marca de tiempo de la ocurrencia", example = "2026-11-26T16:00:00")
        LocalDateTime timestamp
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, LocalDateTime.now());
    }
}
