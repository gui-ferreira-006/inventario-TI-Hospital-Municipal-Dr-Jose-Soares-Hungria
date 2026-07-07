package inventario.repository;

import inventario.model.RegistroHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistoricoRepository extends JpaRepository<RegistroHistorico, Long> {
    List<RegistroHistorico> findAllByOrderByDataRegistroDesc();

    @Query("SELECT h FROM RegistroHistorico h " +
           "WHERE LOWER(h.modulo) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(h.identificador) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(h.acao) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(h.detalhes) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<RegistroHistorico> pesquisarGlobal(@Param("termo") String termo, Sort sort);
}
