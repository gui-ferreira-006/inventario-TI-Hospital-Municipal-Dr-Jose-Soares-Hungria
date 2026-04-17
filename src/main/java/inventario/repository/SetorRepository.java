package inventario.repository;

import inventario.model.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // Diz ao Spring que esta classe gerencia o banco.
public interface SetorRepository extends JpaRepository<Setor, Long>{
    // 1. O Spring Boot é inteligente: se criarmos um método começando com "existsBy"
    // seguido do nome da variável (Nome), ele faz a busca SQL sozinho!
    boolean existsByNome(String nome);

    // 2. Aqui ele busca pelo Nome, mas exclui o ID (IdNot) que passarmos.
    // Essencial para quando formos editar um setor e não alterarmos o nome dele.
    boolean existsByNomeAndIdNot(String nome, Long id);
}
