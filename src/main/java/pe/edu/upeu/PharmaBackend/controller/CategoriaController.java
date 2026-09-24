package pe.edu.upeu.PharmaBackend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.PharmaBackend.dto.CategoriaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.CategoriaResponseDTO;
import pe.edu.upeu.PharmaBackend.service.service.CategoriaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {
    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }


    //Listado de categorías
    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> listar() {
        return ResponseEntity.ok(
                categoriaService.listar()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> buscar(@PathVariable Long id) {
        return  ResponseEntity.ok(categoriaService.buscar(id)
        );
    }

    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> crear(@Valid @RequestBody CategoriaRequestDTO requestDTO) {
        CategoriaResponseDTO response = categoriaService.crear(requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequestDTO requestDTO) {
        return ResponseEntity.ok(
                categoriaService.actualizar(id, requestDTO)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {
        categoriaService.eliminar(id);

        return ResponseEntity.noContent().build();

    }


}
