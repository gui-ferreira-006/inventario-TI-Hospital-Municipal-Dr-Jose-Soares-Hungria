package inventario.controller;

import inventario.model.Impressora;
import inventario.repository.ImpressoraRepository;
import inventario.repository.SetorRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/impressoras")
public class ImpressoraController {
    
    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Autowired
    private SetorRepository setorRepository;

    @GetMapping
    public String listarImpressoras(Model model) {
        model.addAttribute("impressoras", impressoraRepository.findAll());
        return "impressoras/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("impressora", new Impressora());
        model.addAttribute("setores", setorRepository.findAll());
        return "impressoras/cadastro";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("impressora") Impressora impressora,
                         BindingResult result,
                         Model model) {
        
        // =========================================================
        // VERIFICAÇÕES DE DUPLICIDADE
        // =========================================================

        // 1. Duplicidade do Serial
        if (impressora.getSerialImpressora() != null && !impressora.getSerialImpressora().trim().isEmpty()) {
            boolean serialDuplicado = (impressora.getId() == null)
                ? impressoraRepository.existsBySerialImpressora(impressora.getSerialImpressora())
                : impressoraRepository.existsBySerialImpressoraAndIdNot(impressora.getSerialImpressora(), impressora.getId());
            if (serialDuplicado) result.rejectValue("serialImpressora", "error.impressora", "Este Número de Série já está cadastrado.");
        }

        // 2. Duplicidade do IP
        if (impressora.getEnderecoIp() != null && !impressora.getEnderecoIp().trim().isEmpty()) {
            boolean ipDuplicado = (impressora.getId() == null)
                ? impressoraRepository.existsByEnderecoIp(impressora.getEnderecoIp())
                : impressoraRepository.existsByEnderecoIpAndIdNot(impressora.getEnderecoIp(), impressora.getId());
            if (ipDuplicado) result.rejectValue("enderecoIp", "error.impressora", "ATENÇÃO: Este Endereço IP já está em uso!");
        }

        // =========================================================
        // RETORNO DE ERROS
        // =========================================================
        if (result.hasErrors()) {
            model.addAttribute("setores", setorRepository.findAll());
            return "impressoras/cadastro";
        }

        impressoraRepository.save(impressora);
        return "redirect:/impressoras";
    }

    @GetMapping("/editar/{id}")
    public String editor(@PathVariable Long id, Model model) {
        Impressora impressora = impressoraRepository.findById(id).orElse(null);
        model.addAttribute("impressora", impressora);
        model.addAttribute("setores", setorRepository.findAll());
        return "impressoras/cadastro";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        impressoraRepository.deleteById(id);
        return "redirect:/impressoras";
    }
}
