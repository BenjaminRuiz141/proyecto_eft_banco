package duoc.batch.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interes {

    @Id
    private int id;
    private String nombre;
    private int saldo;
    private int edad;
    private String tipo;

}
