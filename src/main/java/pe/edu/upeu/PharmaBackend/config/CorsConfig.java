package pe.edu.upeu.PharmaBackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 * Configuración global de CORS para el frontend de la Unidad 2.
 *
 * Los orígenes se declaran de forma explícita: no se usa el comodín
 * "*", porque deshabilita el envío de credenciales y abre la API a
 * cualquier sitio. La lista se puede sobrescribir por perfil con la
 * propiedad app.cors.allowed-origins.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origenesPermitidos;

    public CorsConfig(
            @Value("${app.cors.allowed-origins:"
                    + "http://localhost:5173,"
                    + "http://127.0.0.1:5173,"
                    + "http://localhost:4200,"
                    + "http://localhost:3000}")
            String[] origenesPermitidos) {

        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
                .allowedHeaders(
                        "Content-Type",
                        "Accept",
                        "Authorization",
                        "Origin"
                )
                .exposedHeaders("Location")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
