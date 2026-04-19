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

    @NotBlank(message = "A marca e modelo são obrigatórios.")
    private String marcaModelo; // Ex: HP LaserJet Pro M404, Brother HL - L6202DW

    @NotBlank(message = "O número de série é obrigatório.")
    @Column(nullable = false, unique = true, name = "serial_impressora")
    private String serialImpressora;

    // O endereço IP é OBRIGATÓRIO, ÚNICO e com validação de formato
    @NotBlank(message = "O Endereço IP é obrigatório.")
    @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$", message = "Formato de IP inválido. Ex: 192.168.160.1")
    @Column(nullable = false, unique = true)
    private String enderecoIp;

    @ManyToOne
    @JoinColumn(name = "setor_id")
    private Setor setor;


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
        this.serialImpressora = serialImpressora;
    }

    public String getEnderecoIp() {
        return enderecoIp;
    }

    public void setEnderecoIp(String enderecoIp) {
        this.enderecoIp = enderecoIp;
    }

    public Setor getSetor() {
        return setor;
    }

    public void setSetor(Setor setor) {
        this.setor = setor;
    }

}
