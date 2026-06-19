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

    @GetMapping("/{idOrigem}")
    public String selecionarTipo(@PathVariable Long idOrigem, Model model) {
        Impressora origem = impressoraRepository.findById(idOrigem).orElse(null);
        if (origem == null) {
            return "redirect:/impressoras";
        }
        model.addAttribute("impressoraOrigem", origem);
        return "impressoras/troca/selecionar-tipo";
    }

    @GetMapping("/{idOrigem}/defeito")
    public String formDefeito(@PathVariable Long idOrigem, Model model) {
        Impressora origem = impressoraRepository.findById(idOrigem).orElse(null);
        if (origem == null) {
            return "redirect:/impressoras";
        }

        List<Impressora> disponiveis = impressoraRepository.findAll().stream()
                .filter(imp -> !imp.getId().equals(idOrigem))
                .filter(imp -> (imp.getStatus() != null && (imp.getStatus().equalsIgnoreCase("Em Estoque") || imp.getStatus().equalsIgnoreCase("Em Estoque/Bancada")))
                        || (imp.getSetor() != null && imp.getSetor().getNome() != null && imp.getSetor().getNome().equalsIgnoreCase("TI")))
                .collect(Collectors.toList());

        model.addAttribute("impressoraOrigem", origem);
        model.addAttribute("impressorasDisponiveis", disponiveis);
        return "impressoras/troca/form-defeito";
    }

    @GetMapping("/{idOrigem}/remanejamento")
    public String formRemanejamento(@PathVariable Long idOrigem, Model model) {
        Impressora origem = impressoraRepository.findById(idOrigem).orElse(null);
        if (origem == null) {
            return "redirect:/impressoras";
        }

        List<Impressora> ativas = impressoraRepository.findAll().stream()
                .filter(imp -> !imp.getId().equals(idOrigem))
                .filter(imp -> imp.getStatus() != null 
                        && !imp.getStatus().equalsIgnoreCase("Em Manutenção") 
                        && !imp.getStatus().equalsIgnoreCase("Em Estoque")
                        && !imp.getStatus().equalsIgnoreCase("Em Estoque/Bancada"))
                .collect(Collectors.toList());

        model.addAttribute("impressoraOrigem", origem);
        model.addAttribute("impressorasAtivas", ativas);
        return "impressoras/troca/form-remanejamento";
    }

    @PostMapping("/confirmar")
    public String confirmarTroca(
            @RequestParam("idAvariada") Long idOrigem,
            @RequestParam("idSubstituta") Long idDestino,
            @RequestParam("tipoTroca") String tipoTroca) {
        try {
            impressoraTrocaService.processarTroca(idOrigem, idDestino, tipoTroca);
            return "redirect:/impressoras?trocaSucesso=true";
        } catch (IllegalArgumentException e) {
            return "redirect:/impressoras?erro=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
