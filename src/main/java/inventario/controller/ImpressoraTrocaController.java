package inventario.controller;

import inventario.model.Impressora;
import inventario.repository.ImpressoraRepository;
import inventario.service.ImpressoraTrocaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/impressoras/troca")
public class ImpressoraTrocaController {

    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Autowired
    private ImpressoraTrocaService impressoraTrocaService;

    @GetMapping("/{id}")
    public String exibirFormularioTroca(@PathVariable Long id, Model model) {
        Impressora avariada = impressoraRepository.findById(id).orElse(null);
        if (avariada == null) {
            return "redirect:/impressoras";
        }

        // Recupera todas as outras impressoras exceto a avariada
        List<Impressora> disponiveis = impressoraRepository.findAll().stream()
                .filter(imp -> !imp.getId().equals(id))
                .collect(Collectors.toList());

        model.addAttribute("impressoraAvariada", avariada);
        model.addAttribute("impressorasDisponiveis", disponiveis);

        return "impressoras/troca";
    }

    @PostMapping("/confirmar")
    public String confirmarTroca(@RequestParam Long idAvariada, @RequestParam Long idSubstituta) {
        impressoraTrocaService.realizarTroca(idAvariada, idSubstituta);
        return "redirect:/impressoras?trocaSucesso=true";
    }
}
