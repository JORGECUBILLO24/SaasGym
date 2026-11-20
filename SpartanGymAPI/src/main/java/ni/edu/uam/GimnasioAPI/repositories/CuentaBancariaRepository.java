package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.CuentaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, UUID> {
    List<CuentaBancaria> findByActivaTrueOrderByBancoAscMonedaAsc();

    List<CuentaBancaria> findAllByOrderByActivaDescBancoAsc();
}
