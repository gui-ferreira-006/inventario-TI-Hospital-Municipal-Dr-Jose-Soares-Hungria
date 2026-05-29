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

    // IP: OPCIONAL e sem validação rígida de formato no modelo.
    // Impressoras via cabo ou de reserva podem não ter IP configurado.
    // O Controller valida o formato quando o campo é preenchido manualmente.
    @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$", message = "Formato de IP inválido. Ex: 192.168.160.1",
             groups = inventario.model.Impressora.ManualValidation.class)
    @Column(unique = true)
    private String enderecoIp;

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

    public String getEnderecoIp() {
        return enderecoIp;
    }

    public void setEnderecoIp(String enderecoIp) {
        // Converte string vazia para null — evita violação de UNIQUE com strings vazias
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
