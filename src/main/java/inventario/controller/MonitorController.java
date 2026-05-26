package inventario.controller;

import inventario.model.Monitor;
import inventario.repository.MonitorRepository;
import inventario.repository.ComputadorRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/monitores")
public class MonitorController {

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private ComputadorRepository computadorRepository;

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
        monitorRepository.deleteById(id);
        return "redirect:/monitores";
    }
}
