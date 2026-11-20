package ni.edu.uam.GimnasioAPI.repositories;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import ni.edu.uam.GimnasioAPI.models.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHashAndUsadoFalse(String tokenHash);

    /**
     * Igual que la anterior pero bloqueando la fila hasta el final de la transacción: dos
     * peticiones simultáneas con el mismo enlace ya no pueden leer ambas "usado = false" y
     * cambiar las dos la contraseña. La segunda espera, y al leer ve el token ya usado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select t from PasswordResetToken t where t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<PasswordResetToken> findByUsuarioIdAndUsadoFalse(UUID usuarioId);
}
