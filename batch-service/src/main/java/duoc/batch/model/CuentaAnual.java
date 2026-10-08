package duoc.batch.model;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cuenta_anual")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentaAnual {

    @Id
    private int cuentaId;
    private Date fecha;
    private String transaccion;
    private int monto;
    private String descripcion;

}
