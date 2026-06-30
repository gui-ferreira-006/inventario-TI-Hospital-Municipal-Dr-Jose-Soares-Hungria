package inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "tb_impressoras")
public class Impressora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Marca e modelo: obrigatório ao cadastrar manualmente via API
    @NotBlank(message = "A marca e modelo são obrigatórios.")
    private String marcaModelo; // Ex: HP LaserJet Flow MFP E52645, Samsung SL-M4020ND

    // Serial: OPCIONAL — muitas impressoras de rede (ex: Zebras) não possuem serial cadastrado
    @Column(unique = true, name = "serial_impressora")
    private String serialImpressora;

    @OneToOne
    @JoinColumn(name = "rede_ip_id")
    private RedeIp redeIp;

    // Status: "Ativo no Setor" ou "Em Estoque/Bancada"
    private String status;

    @ManyToOne
    @JoinColumn(name = "setor_id")
    private Setor setor;

    // ===============================================================
    // GRUPO DE VALIDAÇÃO — usado apenas no fluxo manual (Controller)
    // ===============================================================
    public interface ManualValidation {}

    // ===============================================================
    // CONSTRUTORES
    // ===============================================================
    public Impressora() {}

    // ===============================================================
    // GETTERS E SETTERS
    // ===============================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMarcaModelo() {
        return marcaModelo;
    }

    public void setMarcaModelo(String marcaModelo) {
        this.marcaModelo = marcaModelo;
    }

    public String getSerialImpressora() {
        return serialImpressora;
    }

    public void setSerialImpressora(String serialImpressora) {
        // Converte string vazia para null — evita violação de UNIQUE com strings vazias
        if (serialImpressora != null && serialImpressora.trim().isEmpty()) {
            this.serialImpressora = null;
        } else {
            this.serialImpressora = serialImpressora;
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

    public RedeIp getRedeIp() {
        return redeIp;
    }

    public void setRedeIp(RedeIp redeIp) {
        this.redeIp = redeIp;
    }
}
