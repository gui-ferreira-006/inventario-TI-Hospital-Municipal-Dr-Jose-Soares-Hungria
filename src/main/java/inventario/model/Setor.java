package inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity // Isso faz com que o Spring crie uma tabela para esta classe.
@Table(name = "tb_setores") // Nome da tabela do banco de dados.
public class Setor {
    
    @Id // Chave primária.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incremento.
    private Long id;

    @NotBlank(message = "O nome do setor é obrigatório.") // Validação: não pode ser vazio.
    @Size(min = 2, message = "O nome do setor deve ter pelo menos 2 caracteres.")
    @Column(nullable = false, unique = true) // Não pode ser vazio e não pode repetir nome.
    private String nome;

    @NotBlank(message = "O bloco ou prédio é obrigatório.")
    private String bloco; // Ex: Bloco A, Andar 2

    // --- CONSTRUTORES (Obrigatório ter um vazio pro JPA) ---
    public Setor() {}

    public Setor(String nome, String bloco) {
        this.nome = nome;
        this.bloco = bloco;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getBloco() {
        return bloco;
    }

    public void setBloco(String bloco) {
        this.bloco = bloco;
    }

    // --- GETTERS E SETTERS (O Java usa eles para ler e gravar os dados) ---
}
