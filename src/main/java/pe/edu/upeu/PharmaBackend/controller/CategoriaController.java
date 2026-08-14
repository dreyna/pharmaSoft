package pe.edu.upeu.PharmaBackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.PharmaBackend.entity.Categoria;
import pe.edu.upeu.PharmaBackend.service.service.CategoriaService;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }
    //Listado de categorías
    @GetMapping
    public Iterable<Categoria> getCategorias() {
        return categoriaService.readAll();
    }
    @GetMapping("/{id}")
    public Categoria getIdCategorias(@PathVariable Long id) {
        return categoriaService.read(id).get();
    }

}
