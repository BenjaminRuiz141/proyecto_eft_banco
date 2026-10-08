package duoc.batch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import duoc.batch.model.Transaccion;

public interface TransaccionRepository extends JpaRepository<Transaccion, Integer> {
}
