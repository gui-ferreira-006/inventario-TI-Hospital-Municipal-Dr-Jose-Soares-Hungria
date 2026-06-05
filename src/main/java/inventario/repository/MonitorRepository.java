package inventario.repository;

import inventario.model.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {
    
    // Verificações de duplicidade para o número de série do monitor
    boolean existsBySerialMonitor(String serialMonitor);
    boolean existsBySerialMonitorAndIdNot(String serialMonitor, Long id);

    long countByComputadorIsNotNull();
}
