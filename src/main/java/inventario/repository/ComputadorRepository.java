package inventario.repository;

import inventario.model.Computador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ComputadorRepository extends JpaRepository<Computador, Long> {

    @Query("SELECT c FROM Computador c " +
        "LEFT JOIN c.setor s " +
        "WHERE LOWER(c.serialComputador) LIKE LOWER(CONCAT('%', :termo, '%')) OR " + 
        "LOWER(c.hostname) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
        "LOWER(c.enderecoIp) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
        "(LOWER(s.nome) LIKE LOWER(CONCAT('%', :termo, '%')) AND c.status = 'Ativo no Setor')"
    )
    List<Computador> pesquisarGlobal(@Param("termo") String termo);

    // --- Validações de Duplicidade para o Hostname ---
    boolean existsByHostname(String hostname);

    boolean existsByHostnameAndIdNot(String hostname, Long id);

    java.util.Optional<Computador> findByHostname(String hostname);


    // --- Validações de Duplicidade para o Serial ---
    boolean existsBySerialComputador(String serialComputador);

    boolean existsBySerialComputadorAndIdNot(String serialComputador, Long id);

    // --- Validações de Duplicidade para o Endereço IP ---
    boolean existsByEnderecoIp(String enderecoIp);

    boolean existsByEnderecoIpAndIdNot(String enderecoIp, Long id);
}

// Essa Classe permite realizar operações de CRUD (Create, Read, Update, Delete) na entidade Computador no Banco de Dados.