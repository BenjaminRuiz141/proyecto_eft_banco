package duoc.batch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import duoc.batch.model.CuentaAnual;

public interface CuentaAnualRepository extends JpaRepository<CuentaAnual, Integer> {
}