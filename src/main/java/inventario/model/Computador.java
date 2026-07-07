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

    @OneToOne
    @JoinColumn(name = "rede_ip_id")
    private RedeIp redeIp;

    private String status;

    @ManyToOne
    @JoinColumn(name = "setor_id")
    private Setor setor;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

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

    public RedeIp getRedeIp() {
        return redeIp;
    }

    public void setRedeIp(RedeIp redeIp) {
        this.redeIp = redeIp;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}