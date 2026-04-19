package inventario.repository;

import inventario.model.Impressora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImpressoraRepository extends JpaRepository<Impressora, Long> {
    
    // Verificações de Duplicidade para o Serial
    boolean existsBySerialImpressora(String serialImpressora);
    boolean existsBySerialImpressoraAndIdNot(String serialImpressora, Long id);

    // Verificações de Duplicidade para o IP
    boolean existsByEnderecoIp(String enderecoIp);
    boolean existsByEnderecoIpAndIdNot(String enderecoIp, Long id);
}
