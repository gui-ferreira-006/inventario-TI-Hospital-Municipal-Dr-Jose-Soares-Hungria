package inventario.repository;

import inventario.model.Impressora;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ImpressoraRepository extends JpaRepository<Impressora, Long> {

    @Query("SELECT i FROM Impressora i " +
        "LEFT JOIN i.setor s " +
        "WHERE LOWER(i.marcaModelo) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
        "LOWER(i.serialImpressora) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
        "LOWER(i.enderecoIp) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
        "LOWER(i.status) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
        "LOWER(s.nome) LIKE LOWER(CONCAT('%', :termo, '%'))"
    )
    List<Impressora> pesquisarGlobal(@Param("termo") String termo, Sort sort);
    
    // Verificações de Duplicidade para o Serial
    boolean existsBySerialImpressora(String serialImpressora);
    boolean existsBySerialImpressoraAndIdNot(String serialImpressora, Long id);

    // Verificações de Duplicidade para o IP
    boolean existsByEnderecoIp(String enderecoIp);
    boolean existsByEnderecoIpAndIdNot(String enderecoIp, Long id);
}
