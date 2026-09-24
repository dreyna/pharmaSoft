package pe.edu.upeu.PharmaBackend.service.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.PharmaBackend.dto.CategoriaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.CategoriaResponseDTO;
import pe.edu.upeu.PharmaBackend.entity.Categoria;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.mapper.CategoriaMapper;
import pe.edu.upeu.PharmaBackend.repository.CategoriaRepository;
import pe.edu.upeu.PharmaBackend.repository.ProductoRepository;
import pe.edu.upeu.PharmaBackend.service.service.CategoriaService;


@Service
public class CategoriaServiceImpl implements CategoriaService {

    private static final Logger log =
            LoggerFactory.getLogger(CategoriaServiceImpl.class);

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaMapper categoriaMapper;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository,
                                ProductoRepository productoRepository,
                                CategoriaMapper categoriaMapper) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.categoriaMapper = categoriaMapper;
    }

    @Override
    @Transactional
    public CategoriaResponseDTO crear(CategoriaRequestDTO request) {
        String nombre = request.getNombre().trim();//"Carnes " != "Carnes"
        if(categoriaRepository.existsByNombreIgnoreCase(nombre)){
            throw new ReglaNegocioException(
                    "Ya existe una categoria con el nomre "+ nombre
            );
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.getDescripcion());
        categoria.setEstado(request.getEstado());

        Categoria catCreada = categoriaRepository.save(categoria);

        return categoriaMapper.toResponse(catCreada);
    }

    @Override
    @Transactional
    public CategoriaResponseDTO actualizar(Long id, CategoriaRequestDTO request) {
        Categoria categoria = categoriaRepository.findById(id).orElseThrow(()->
                new RecursoNoEncontradoException(
                        "Categoria no encontrada con id: "+ id
                )
        );

        String nombre = request.getNombre().trim();
        if(categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)){
            throw new ReglaNegocioException(
                    "Ya existe una categoria con el nomre "+ nombre
            );
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(request.getDescripcion());
        categoria.setEstado(request.getEstado());

        Categoria catActualizada = categoriaRepository.saveAndFlush(categoria);

        return categoriaMapper.toResponse(catActualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscar(Long id) {

        log.info("Buscando categoria | id={}", id);

        Categoria categoria =  categoriaRepository.findById(id)
                .orElseThrow(()->
                        new RecursoNoEncontradoException(
                                "Categoria no encontrada con id: "+ id
                        )
                );
        return categoriaMapper.toResponse(categoria);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = categoriaRepository.findById(id).orElseThrow(()->
                new RecursoNoEncontradoException(
                        "Categoria no encontrada con id: "+ id
                )
        );

        if(productoRepository.existsByCategoriaId(id)){
            throw new ReglaNegocioException(
                    "No se puede eliminar la categoria "+ categoria.getNombre()
                            + " porque tiene productos asociados"
            );
        }

        categoriaRepository.delete(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listar() {

        long inicio = System.currentTimeMillis();
        log.info("Inicio listar categorias");

        List<CategoriaResponseDTO> resultado =
                categoriaRepository.findAll()
                        .stream()
                        .map(categoriaMapper::toResponse)
                        .toList();

        log.info("Fin listar categorias | filas={} | duracionMs={}",
                resultado.size(),
                System.currentTimeMillis() - inicio);

        return resultado;
    }
}
