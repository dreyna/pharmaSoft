package pe.edu.upeu.PharmaBackend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductoRequestDTO {

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(
            min = 3,
            max = 150,
            message = "El nombre debe tener entre 3 y 150 caracteres"
    )
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(
            value = "0.01",
            message = "El precio debe ser mayor que cero"
    )
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio")
    @Min(
            value = 0,
            message = "El stock no puede ser negativo"
    )
    private Integer stock;

    @NotNull(message = "El estado es obligatorio")
    private Boolean estado;

    @NotNull(message = "La categoría es obligatoria")
    @Positive(message = "El identificador de categoría debe ser válido")
    private Long categoriaId;


}