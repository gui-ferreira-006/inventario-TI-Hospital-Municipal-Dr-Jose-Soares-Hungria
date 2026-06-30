package inventario.repository;

import inventario.model.Computador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Sort;

@Repository
public interface ComputadorRepository extends JpaRepository<Computador, Long> {

    @Query("SELECT c FROM Computador c " +
        "LEFT JOIN c.setor s " +
        "LEFT JOIN c.redeIp r " +
        "WHERE LOWER(c.serialComputador) LIKE LOWER(CONCAT('%', :termo, '%')) OR " + 
        "LOWER(c.hostname) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
        "LOWER(r.enderecoIp) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
        "LOWER(c.status) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
        "(LOWER(s.nome) LIKE LOWER(CONCAT('%', :termo, '%')) AND c.status = 'Ativo no Setor')"
    )
    List<Computador> pesquisarGlobal(@Param("termo") String termo, Sort sort);

    // --- Validações de Duplicidade para o Hostname ---
    boolean existsByHostname(String hostname);

    boolean existsByHostnameAndIdNot(String hostname, Long id);

    java.util.Optional<Computador> findByHostname(String hostname);


    // --- Validações de Duplicidade para o Serial ---
    boolean existsBySerialComputador(String serialComputador);

    boolean existsBySerialComputadorAndIdNot(String serialComputador, Long id);

    List<Computador> findByStatus(String status, Pageable pageable);

    @Query("SELECT c.status, COUNT(c) FROM Computador c GROUP BY c.status")
    List<Object[]> countComputadoresGroupByStatus();
}

// Essa Classe permite realizar operações de CRUD (Create, Read, Update, Delete) na entidade Computador no Banco de Dados.