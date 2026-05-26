package inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tb_monitores")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "A marca é obrigatória.")
    private String marca;

    @NotBlank(message = "O modelo é obrigatório.")
    private String modelo;

    @NotBlank(message = "O número de série é obrigatório.")
    @Column(nullable = false, unique = true, name = "serial_monitor")
    private String serialMonitor;

    @ManyToOne
    @JoinColumn(name = "computador_id")
    private Computador computador;

    // Construtores
    public Monitor() {}

    public Monitor(String marca, String modelo, String serialMonitor, Computador computador) {
        this.marca = marca;
        this.modelo = modelo;
        this.serialMonitor = serialMonitor;
        this.computador = computador;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getSerialMonitor() {
        return serialMonitor;
    }

    public void setSerialMonitor(String serialMonitor) {
        this.serialMonitor = serialMonitor;
    }

    public Computador getComputador() {
        return computador;
    }

    public void setComputador(Computador computador) {
        this.computador = computador;
    }
}
