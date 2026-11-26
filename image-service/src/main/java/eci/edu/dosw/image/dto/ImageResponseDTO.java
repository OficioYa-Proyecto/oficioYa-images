package eci.edu.dosw.image.dto;

import eci.edu.dosw.image.model.ImageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta con metadatos de la imagen.
 * NO incluye los bytes binarios para no saturar la red en listados.
 * Para descargar el binario usar: GET /api/v1/images/{id}
 */
@Schema(description = "Metadatos informativos de una imagen almacenada")
public record ImageResponseDTO(
        @Schema(description = "Identificador único de la imagen", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        UUID id,

        @Schema(description = "UUID de la entidad asociada (usuario, solicitud, trabajo, reseña, etc.)", example = "717cfb45-903b-4825-8c97-fb54e98b5bd9")
        UUID rootId,

        @Schema(description = "Tipo funcional de imagen dentro del sistema", example = "PROFILE")
        ImageType tipo,

        @Schema(description = "Nombre original del archivo cargado", example = "avatar-foto.jpg")
        String nombre,

        @Schema(description = "Tipo MIME del contenido", example = "image/jpeg")
        String tipoContenido,

        @Schema(description = "Tamaño del archivo en bytes", example = "1048576")
        Long tamano,

        @Schema(description = "Fecha y hora en que se cargó la imagen", example = "2026-11-26T14:30:00")
        LocalDateTime fechaCarga,

        @Schema(description = "Referencia externa opcional asociada al registro", example = "perfil-usuario-samuel")
        String referenciaExterna,

        @Schema(description = "Ruta relativa para descargar directamente la imagen binaria", example = "/api/v1/images/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        String url,

        @Schema(description = "UUID del usuario que cargó la imagen", example = "717cfb45-903b-4825-8c97-fb54e98b5bd9")
        UUID subidoPor
) {}
