package pe.edu.upeu.PharmaBackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.PharmaBackend.entity.Categoria;
import pe.edu.upeu.PharmaBackend.entity.Producto;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.mapper.ProductoMapper;
import pe.edu.upeu.PharmaBackend.repository.CategoriaRepository;
import pe.edu.upeu.PharmaBackend.repository.ProductoRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    private ProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductoServiceImpl(
                productoRepository, categoriaRepository, new ProductoMapper());
    }

    @Test
    @DisplayName("eliminar es baja lógica: marca estado=false y nunca borra la fila")
    void eliminarEsBajaLogica() {

        Producto producto = producto(10L, true);
        when(productoRepository.findById(10L))
                .thenReturn(Optional.of(producto));

        service.eliminar(10L);

        assertThat(producto.getEstado()).isFalse();
        verify(productoRepository).save(producto);
        verify(productoRepository, never()).delete(any());
        verify(productoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminar un producto ya inactivo se rechaza con regla de negocio")
    void eliminarProductoYaInactivo() {

        when(productoRepository.findById(10L))
                .thenReturn(Optional.of(producto(10L, false)));

        assertThatThrownBy(() -> service.eliminar(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya se encuentra inactivo");

        verify(productoRepository, never()).save(any());
        verify(productoRepository, never()).delete(any());
    }

    @Test
    void eliminarProductoInexistenteLanza404() {

        when(productoRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Producto no encontrado con id: 99");
    }

    private static Producto producto(Long id, boolean activo) {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Analgesicos");

        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Paracetamol");
        p.setPrecio(new BigDecimal("2.50"));
        p.setStock(100);
        p.setEstado(activo);
        p.setCategoria(categoria);
        return p;
    }
}
