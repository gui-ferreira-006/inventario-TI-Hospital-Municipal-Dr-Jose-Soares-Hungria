package inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tb_computadores")
public class Computador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- HOSTNAME É OPCIONAL NO BANCO (Mas exixigido no Controller se tiver setor) ---
    @Column(unique = true)
    private String hostname;

    // --- O SERIAL É A ÚNICA INFORMAÇÃO OBRIGATÓRIA E IMUTÁVEL ---
    @NotBlank(message = "O número de serial é obrigatório.")
    @Size(min = 10, message = "O número de serial deve conter pelo menos 10 caracteres.")
    @Column(nullable = false, unique = true, name = "serial_computador")
    private String serialComputador;

    // --- IP COM VALIDAÇÃO DE FORMATO (REGEX) ---
    // Só aceita o formato X.X.X.X (ex: 192.168.0.1)
    @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$", message = "Formato de IP inválido. Ex: 192.168.0.1")
    @Column(unique = true)
    private String enderecoIp;

    private String status;

    @ManyToOne
    @JoinColumn(name = "setor_id")
    private Setor setor;

    // ===================================================
    // GETTERS E SETTERS
    // ===================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        if (hostname != null && hostname.trim().isEmpty()) {
            this.hostname = null;
        } else {
            this.hostname = hostname;
        }
    }

    public String getSerialComputador() {
        return serialComputador;
    }

    public void setSerialComputador(String serialComputador) {
        this.serialComputador = serialComputador;
    }

    public String getEnderecoIp() {
        return enderecoIp;
    }

    public void setEnderecoIp(String enderecoIp) {
        if (enderecoIp != null && enderecoIp.trim().isEmpty()) {
            this.enderecoIp = null;
        } else {
            this.enderecoIp = enderecoIp;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Setor getSetor() {
        return setor;
    }

    public void setSetor(Setor setor) {
        this.setor = setor;
    }
}