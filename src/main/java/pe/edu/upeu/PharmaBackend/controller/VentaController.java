package pe.edu.upeu.PharmaBackend.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.VentaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.VentaResponseDTO;
import pe.edu.upeu.PharmaBackend.enums.EstadoVenta;
import pe.edu.upeu.PharmaBackend.service.service.VentaService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(
            VentaService ventaService) {

        this.ventaService = ventaService;
    }

    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrar(
            @Valid
            @RequestBody VentaRequestDTO request) {

        VentaResponseDTO response =
                ventaService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponseDTO> buscar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ventaService.buscar(id)
        );
    }

    /*
     * Listado paginado de ventas, sin filtros.
     * Campos de ordenamiento permitidos: id, fecha, total, estado.
     */
    @GetMapping
    public ResponseEntity<PaginaResponseDTO<VentaResponseDTO>> listar(

            @RequestParam(defaultValue = "0")
            int pagina,

            @RequestParam(defaultValue = "20")
            int tamanio,

            @RequestParam(required = false, defaultValue = "fecha")
            String ordenarPor,

            @RequestParam(required = false, defaultValue = "desc")
            String direccion) {

        return ResponseEntity.ok(
                ventaService.listar(pagina, tamanio, ordenarPor, direccion)
        );
    }

    /*
     * Búsqueda paginada de ventas con filtros combinados.
     *
     * Todos los filtros son opcionales; los que no se envían no
     * filtran. Sin coincidencias responde 200 con página vacía.
     */
    @GetMapping("/buscar")
    public ResponseEntity<PaginaResponseDTO<VentaResponseDTO>> buscar(

            @RequestParam(required = false)
            Long clienteId,

            @RequestParam(required = false)
            EstadoVenta estado,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam(defaultValue = "0")
            int pagina,

            @RequestParam(defaultValue = "20")
            int tamanio,

            @RequestParam(required = false, defaultValue = "fecha")
            String ordenarPor,

            @RequestParam(required = false, defaultValue = "desc")
            String direccion) {

        return ResponseEntity.ok(
                ventaService.buscar(
                        clienteId,
                        estado,
                        desde,
                        hasta,
                        pagina,
                        tamanio,
                        ordenarPor,
                        direccion
                )
        );
    }
}
