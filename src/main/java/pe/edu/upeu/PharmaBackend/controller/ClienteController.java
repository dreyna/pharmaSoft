package pe.edu.upeu.PharmaBackend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.PharmaBackend.dto.ClienteRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.service.service.ClienteService;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(
            ClienteService clienteService) {

        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crear(
            @Valid
            @RequestBody ClienteRequestDTO request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(clienteService.crear(request));
    }

    /*
     * Listado paginado de clientes.
     * Campos de ordenamiento permitidos: id, dni, nombres, apellidos, email.
     */
    @GetMapping
    public ResponseEntity<PaginaResponseDTO<ClienteResponseDTO>> listar(

            @RequestParam(defaultValue = "0")
            int pagina,

            @RequestParam(defaultValue = "20")
            int tamanio,

            @RequestParam(required = false, defaultValue = "id")
            String ordenarPor,

            @RequestParam(required = false, defaultValue = "asc")
            String direccion) {

        return ResponseEntity.ok(
                clienteService.listar(pagina, tamanio, ordenarPor, direccion)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                clienteService.buscar(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid
            @RequestBody ClienteRequestDTO request) {

        return ResponseEntity.ok(
                clienteService.actualizar(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {

        clienteService.eliminar(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}
