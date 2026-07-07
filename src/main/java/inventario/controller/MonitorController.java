package inventario.controller;

import inventario.model.Monitor;
import inventario.model.Computador;
import inventario.repository.MonitorRepository;
import inventario.repository.ComputadorRepository;
import inventario.service.HistoricoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/monitores")
public class MonitorController {

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private ComputadorRepository computadorRepository;

    @Autowired
    private HistoricoService historicoService;

    @GetMapping
    public String listarMonitores(Model model) {
        model.addAttribute("monitores", monitorRepository.findAll());
        return "monitores/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("monitor", new Monitor());
        model.addAttribute("computadores", computadorRepository.findAll());
        return "monitores/cadastro";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("monitor") Monitor monitor,
                         BindingResult result,
                         Model model) {

        // Validação de Duplicidade para o Serial do Monitor
        if (monitor.getSerialMonitor() != null && !monitor.getSerialMonitor().trim().isEmpty()) {
            boolean serialDuplicado = (monitor.getId() == null)
                ? monitorRepository.existsBySerialMonitor(monitor.getSerialMonitor())
                : monitorRepository.existsBySerialMonitorAndIdNot(monitor.getSerialMonitor(), monitor.getId());
            
            if (serialDuplicado) {
                result.rejectValue("serialMonitor", "error.monitor", "Este Número de Série já está cadastrado.");
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("computadores", computadorRepository.findAll());
            return "monitores/cadastro";
        }

        // Instrumentação de Histórico/Auditoria
        String ident = (monitor.getSerialMonitor() != null && !monitor.getSerialMonitor().isEmpty()) 
                       ? monitor.getSerialMonitor() 
                       : monitor.getMarcaModelo();

        if (monitor.getId() == null) {
            historicoService.registrarEvento("MONITOR", ident, "CRIACAO", "Registro criado no sistema");
        } else {
            Monitor antigo = monitorRepository.findById(monitor.getId()).orElse(null);
            if (antigo != null) {
                List<String> alteracoes = new ArrayList<>();
                if (!Objects.equals(antigo.getMarcaModelo(), monitor.getMarcaModelo())) {
                    alteracoes.add("Marca/Modelo: " + antigo.getMarcaModelo() + " ➔ " + monitor.getMarcaModelo());
                }

                Computador compNovo = null;
                if (monitor.getComputador() != null && monitor.getComputador().getId() != null) {
                    compNovo = computadorRepository.findById(monitor.getComputador().getId()).orElse(null);
                }

                Long idSetorAntigo = (antigo.getComputador() != null && antigo.getComputador().getSetor() != null) 
                                      ? antigo.getComputador().getSetor().getId() : null;
                Long idSetorNovo = (compNovo != null && compNovo.getSetor() != null) ? compNovo.getSetor().getId() : null;

                if (!Objects.equals(idSetorAntigo, idSetorNovo)) {
                    String nomeSetorAntigo = (antigo.getComputador() != null && antigo.getComputador().getSetor() != null) 
                                             ? antigo.getComputador().getSetor().getNome() : "Reserva TI";
                    String nomeSetorNovo = (compNovo != null && compNovo.getSetor() != null) ? compNovo.getSetor().getNome() : "Reserva TI";
                    alteracoes.add("Setor: " + nomeSetorAntigo + " ➔ " + nomeSetorNovo);
                }
                if (!alteracoes.isEmpty()) {
                    historicoService.registrarEvento("MONITOR", ident, "ATUALIZACAO", String.join(" | ", alteracoes));
                }
            }
        }

        monitorRepository.save(monitor);
        return "redirect:/monitores";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Monitor monitor = monitorRepository.findById(id).orElse(null);
        model.addAttribute("monitor", monitor);
        model.addAttribute("computadores", computadorRepository.findAll());
        return "monitores/cadastro";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        Monitor monitor = monitorRepository.findById(id).orElse(null);
        if (monitor != null) {
            String ident = (monitor.getSerialMonitor() != null && !monitor.getSerialMonitor().isEmpty()) 
                           ? monitor.getSerialMonitor() 
                           : monitor.getMarcaModelo();
            historicoService.registrarEvento("MONITOR", ident, "EXCLUSAO", "Registro excluído do sistema");
        }
        monitorRepository.deleteById(id);
        return "redirect:/monitores";
    }
}
