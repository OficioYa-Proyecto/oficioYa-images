package eci.edu.dosw.image.controller;

import eci.edu.dosw.image.dto.ImageResponseDTO;
import eci.edu.dosw.image.model.ImageDocument;
import eci.edu.dosw.image.model.ImageType;
import eci.edu.dosw.image.service.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/images")
@Tag(name = "Imágenes", description = "Microservicio para gestión, almacenamiento y consulta de imágenes binarias y metadatos en MongoDB")
public class ImageController {

    private final ImageStorageService storageService;

    public ImageController(ImageStorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(summary = "Descargar binario de imagen por ID", description = "Descarga directa del arreglo de bytes con cabeceras HTTP de caché optimizadas para navegador y servicios cliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Imagen binaria recuperada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Imagen no encontrada o eliminada",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadImage(
            @Parameter(description = "Identificador único de la imagen", required = true)
            @PathVariable UUID id) {
        ImageDocument image = storageService.getImageById(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getTipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getNombre() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(image.getDatos());
    }

    @Operation(summary = "Consultar metadatos de la imagen por ID", description = "Retorna los metadatos de la imagen (nombre, tamaño, tipo, fecha de carga) sin cargar los bytes binarios.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Metadatos obtenidos exitosamente"),
        @ApiResponse(responseCode = "404", description = "Imagen no encontrada",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/metadata")
    public ResponseEntity<ImageResponseDTO> getImageMetadata(
            @Parameter(description = "Identificador único de la imagen", required = true)
            @PathVariable UUID id) {
        ImageDocument image = storageService.getImageById(id);
        return ResponseEntity.ok(storageService.toDTO(image));
    }

    @Operation(summary = "Listar imágenes por rootId", description = "Retorna todas las imágenes asociadas a una entidad (usuario, solicitud, trabajo, etc.), con filtro opcional por tipo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de imágenes asociadas obtenida exitosamente")
    })
    @GetMapping("/by-root/{rootId}")
    public ResponseEntity<List<ImageResponseDTO>> getImagesByRoot(
            @Parameter(description = "UUID de la entidad raíz asociada", required = true)
            @PathVariable UUID rootId,
            @Parameter(description = "Filtro opcional por tipo de imagen (PROFILE, WORK, etc.)")
            @RequestParam(value = "tipo", required = false) ImageType tipo) {
        return ResponseEntity.ok(storageService.getImagesByRootId(rootId, tipo));
    }

    @Operation(summary = "Subir una imagen individual",
            description = "Sube una imagen validando formato (JPEG, PNG, WEBP), tamaño máximo (5MB) y cuotas por tipo: PROFILE (máx 1 activa), WORK (50), REVIEW (5), REPORT (5).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Imagen subida exitosamente"),
        @ApiResponse(responseCode = "400", description = "Archivo corrupto, formato no soportado o datos inválidos",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "No tiene permisos para modificar la imagen de este rootId",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Cuota máxima de imágenes excedida para este tipo",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponseDTO> uploadImage(
            @Parameter(description = "Archivo binario de imagen (multipart/form-data)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "UUID de la entidad dueña de la imagen", required = true)
            @RequestParam("rootId") UUID rootId,
            @Parameter(description = "Tipo funcional de imagen a registrar", required = true)
            @RequestParam("tipo") ImageType tipo,
            @Parameter(description = "Referencia externa o etiqueta opcional")
            @RequestParam(value = "referenciaExterna", required = false) String referenciaExterna,
            Authentication auth) throws IOException {

        ImageResponseDTO response = storageService.saveImage(rootId, tipo, file, referenciaExterna,
                usuario(auth), esAdmin(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Subir múltiples imágenes en lote",
            description = "Carga simultánea de varios archivos respetando los límites de cuota establecidos.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Imágenes en lote cargadas exitosamente"),
        @ApiResponse(responseCode = "400", description = "Archivos inválidos o supera el límite permitido",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permiso denegado sobre el rootId",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ImageResponseDTO>> uploadBatch(
            @Parameter(description = "Lista de archivos binarios a subir", required = true)
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "UUID de la entidad asociada", required = true)
            @RequestParam("rootId") UUID rootId,
            @Parameter(description = "Tipo de las imágenes a registrar", required = true)
            @RequestParam("tipo") ImageType tipo,
            @Parameter(description = "Referencia externa opcional")
            @RequestParam(value = "referenciaExterna", required = false) String referenciaExterna,
            Authentication auth) throws IOException {

        List<ImageResponseDTO> response = storageService.saveBatchImages(rootId, tipo, files, referenciaExterna,
                usuario(auth), esAdmin(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Borrado lógico de imagen", description = "Marca la imagen como inactiva. Solo permitido al propietario de la imagen o a un Administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Imagen eliminada exitosamente"),
        @ApiResponse(responseCode = "403", description = "No tiene permisos para eliminar esta imagen",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Imagen no encontrada",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "UUID de la imagen a eliminar", required = true)
            @PathVariable UUID id,
            Authentication auth) {
        storageService.deleteImage(id, usuario(auth), esAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    /** Id del usuario autenticado (sub del JWT), o null si no hay sesion */
    private static UUID usuario(Authentication auth) {
        return (auth != null && auth.getPrincipal() instanceof UUID userId) ? userId : null;
    }

    private static boolean esAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
