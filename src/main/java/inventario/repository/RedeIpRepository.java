package inventario.repository;

import inventario.model.RedeIp;
import inventario.model.StatusIp;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RedeIpRepository extends JpaRepository<RedeIp, Long> {

    boolean existsByEnderecoIp(String enderecoIp);

    boolean existsByEnderecoIpAndIdNot(String enderecoIp, Long id);

    List<RedeIp> findByStatus(StatusIp status);

    List<RedeIp> findByEnderecoIpContaining(String fragmento);

    Optional<RedeIp> findByEnderecoIp(String enderecoIp);

    @Query("SELECT r FROM RedeIp r " +
           "LEFT JOIN Computador c ON c.redeIp = r " +
           "LEFT JOIN Impressora i ON i.redeIp = r " +
           "LEFT JOIN c.setor cs " +
           "LEFT JOIN i.setor iset " +
           "WHERE LOWER(r.enderecoIp) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(r.observacao) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.hostname) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(i.marcaModelo) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(cs.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(iset.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(r.status) LIKE LOWER(CONCAT('%', :termo, '%'))"
    )
    List<RedeIp> pesquisarGlobal(@Param("termo") String termo, Sort sort);

    @Query("SELECT r FROM RedeIp r " +
           "LEFT JOIN Computador c ON c.redeIp = r " +
           "LEFT JOIN Impressora i ON i.redeIp = r " +
           "LEFT JOIN c.setor cs " +
           "LEFT JOIN i.setor iset " +
           "WHERE LOWER(r.enderecoIp) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(r.observacao) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.hostname) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(i.marcaModelo) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(cs.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(iset.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(r.status) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "ORDER BY COALESCE(cs.nome, iset.nome, 'TI Reserva') ASC"
    )
    List<RedeIp> pesquisarGlobalOrdenadoPorSetorAsc(@Param("termo") String termo);

    @Query("SELECT r FROM RedeIp r " +
           "LEFT JOIN Computador c ON c.redeIp = r " +
           "LEFT JOIN Impressora i ON i.redeIp = r " +
           "LEFT JOIN c.setor cs " +
           "LEFT JOIN i.setor iset " +
           "WHERE LOWER(r.enderecoIp) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(r.observacao) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.hostname) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(i.marcaModelo) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(cs.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(iset.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(r.status) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "ORDER BY COALESCE(cs.nome, iset.nome, 'TI Reserva') DESC"
    )
    List<RedeIp> pesquisarGlobalOrdenadoPorSetorDesc(@Param("termo") String termo);

    @Query("SELECT r FROM RedeIp r " +
           "LEFT JOIN Computador c ON c.redeIp = r " +
           "LEFT JOIN Impressora i ON i.redeIp = r " +
           "LEFT JOIN c.setor cs " +
           "LEFT JOIN i.setor iset " +
           "ORDER BY COALESCE(cs.nome, iset.nome, 'TI Reserva') ASC"
    )
    List<RedeIp> findAllOrdenadoPorSetorAsc();

    @Query("SELECT r FROM RedeIp r " +
           "LEFT JOIN Computador c ON c.redeIp = r " +
           "LEFT JOIN Impressora i ON i.redeIp = r " +
           "LEFT JOIN c.setor cs " +
           "LEFT JOIN i.setor iset " +
           "ORDER BY COALESCE(cs.nome, iset.nome, 'TI Reserva') DESC"
    )
    List<RedeIp> findAllOrdenadoPorSetorDesc();
}
