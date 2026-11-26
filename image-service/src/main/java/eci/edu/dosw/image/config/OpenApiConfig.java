package eci.edu.dosw.image.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración OpenAPI / Swagger para el Microservicio de Imágenes de OficioYa.
 * Define metadatos, servidores disponibles y el esquema de seguridad Bearer JWT.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("OficioYa - Microservicio de Imágenes")
                        .version("1.0.0")
                        .description("""
                                Microservicio para la gestión y almacenamiento de imágenes en MongoDB para OficioYa.
                                
                                Provee los contratos de API para:
                                - **Descarga y Visualización:** Entrega de binarios optimizada con cabeceras de caché HTTP.
                                - **Consulta de Metadatos:** Información ligera sin transferir bytes en listados.
                                - **Carga Individual y en Lote (Multipart):** Validación de tipos MIME, tamaño máximo (5MB) y cuotas por tipo (PROFILE, WORK, REVIEW, REPORT, etc.).
                                - **Borrado Lógico y Auditoría:** Eliminación controlada por propietario o Administrador.
                                """)
                        .contact(new Contact()
                                .name("Equipo OficioYa - Escuela Colombiana de Ingeniería")
                                .email("soporte@oficioya.com"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("https://oficioya-images-api-dzecc5e7dzb4d8a5.chilecentral-01.azurewebsites.net").description("Entorno de Producción en Azure"),
                        new Server().url("http://localhost:8083").description("Entorno de Desarrollo Local")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese su token JWT (sin el prefijo 'Bearer ')")));
    }
}
