package pe.edu.upeu.PharmaBackend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.ProductoRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.ProductoResponseDTO;
import pe.edu.upeu.PharmaBackend.service.service.ProductoService;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {
    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }


    /*
     * Listado paginado de productos.
     * Campos de ordenamiento permitidos: id, nombre, precio, stock.
     */
    @GetMapping
    public ResponseEntity<PaginaResponseDTO<ProductoResponseDTO>> listar(

            @RequestParam(defaultValue = "0")
            int pagina,

            @RequestParam(defaultValue = "20")
            int tamanio,

            @RequestParam(required = false, defaultValue = "id")
            String ordenarPor,

            @RequestParam(required = false, defaultValue = "asc")
            String direccion) {

        return ResponseEntity.ok(
                productoService.listar(pagina, tamanio, ordenarPor, direccion)
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscar(id)
        );
    }

    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crear(@Valid @RequestBody ProductoRequestDTO requestDTO) {
        ProductoResponseDTO response = productoService.crear(requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequestDTO requestDTO) {
        return ResponseEntity.ok(
                productoService.actualizar(id, requestDTO)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {
        productoService.eliminar(id);

        return ResponseEntity.noContent().build();

    }


}
