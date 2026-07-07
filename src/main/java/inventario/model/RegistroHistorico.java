package inventario.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_historico")
public class RegistroHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String modulo;

    @Column(nullable = false)
    private String identificador;

    @Column(nullable = false)
    private String acao;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    // Construtor vazio (obrigatório para JPA)
    public RegistroHistorico() {
    }

    // Construtor com parâmetros essenciais
    public RegistroHistorico(String modulo, String identificador, String acao, String detalhes) {
        this.modulo = modulo;
        this.identificador = identificador;
        this.acao = acao;
        this.detalhes = detalhes;
    }

    @PrePersist
    protected void onCreate() {
        if (this.dataRegistro == null) {
            this.dataRegistro = LocalDateTime.now();
        }
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}
