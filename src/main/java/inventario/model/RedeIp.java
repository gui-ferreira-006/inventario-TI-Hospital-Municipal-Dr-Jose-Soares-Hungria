package inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "tb_rede_ip")
public class RedeIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O Endereço IP é obrigatório.")
    @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$", message = "Formato de IP inválido.")
    @Column(nullable = false, unique = true)
    private String enderecoIp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusIp status;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Version
    private Long version;

    // Construtor vazio exigido pelo JPA
    public RedeIp() {
    }

    // Construtor parametrizado
    public RedeIp(String enderecoIp, StatusIp status, String observacao) {
        this.enderecoIp = enderecoIp;
        this.status = status;
        this.observacao = observacao;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEnderecoIp() {
        return enderecoIp;
    }

    public void setEnderecoIp(String enderecoIp) {
        this.enderecoIp = enderecoIp;
    }

    public StatusIp getStatus() {
        return status;
    }

    public void setStatus(StatusIp status) {
        this.status = status;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
