package inventario.controller;

import inventario.model.Impressora;
import inventario.model.RedeIp;
import inventario.model.StatusIp;
import inventario.repository.ImpressoraRepository;
import inventario.repository.RedeIpRepository;
import inventario.repository.SetorRepository;
import inventario.service.RedeIpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/impressoras")
public class ImpressoraController {
    
    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Autowired
    private SetorRepository setorRepository;

    @Autowired
    private RedeIpRepository redeIpRepository;

    @Autowired
    private RedeIpService redeIpService;

    @GetMapping
    public String listarImpressoras(
            @RequestParam(value = "termo", required = false) String termo,
            @RequestParam(value = "ordenarPor", required = false, defaultValue = "id") String ordenarPor,
            @RequestParam(value = "direcao", required = false, defaultValue = "ASC") String direcao,
            Model model) {

        Sort.Direction dir = "DESC".equalsIgnoreCase(direcao) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String propriedade = "id";

        if ("marcaModelo".equalsIgnoreCase(ordenarPor)) {
            propriedade = "marcaModelo";
        } else if ("enderecoIp".equalsIgnoreCase(ordenarPor)) {
            propriedade = "redeIp.enderecoIp";
        } else if ("status".equalsIgnoreCase(ordenarPor)) {
            propriedade = "status";
        } else if ("setor".equalsIgnoreCase(ordenarPor)) {
            propriedade = "setor.nome";
        }

        Sort sort = Sort.by(dir, propriedade);
        List<Impressora> impressoras;

        if (termo != null && !termo.trim().isEmpty()) {
            impressoras = impressoraRepository.pesquisarGlobal(termo.trim(), sort);
            model.addAttribute("termoBusca", termo.trim());
        } else {
            impressoras = impressoraRepository.findAll(sort);
        }

        model.addAttribute("impressoras", impressoras);
        model.addAttribute("ordenarPor", ordenarPor);
        model.addAttribute("direcao", direcao);

        return "impressoras/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("impressora", new Impressora());
        model.addAttribute("setores", setorRepository.findAll());
        model.addAttribute("ipsLivres", redeIpRepository.findByStatus(StatusIp.LIVRE));
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

        // =========================================================
        // RETORNO DE ERROS
        // =========================================================
        if (result.hasErrors()) {
            model.addAttribute("setores", setorRepository.findAll());
            List<RedeIp> ipsLivres = new ArrayList<>(redeIpRepository.findByStatus(StatusIp.LIVRE));
            if (impressora.getRedeIp() != null) {
                ipsLivres.add(0, impressora.getRedeIp());
            }
            model.addAttribute("ipsLivres", ipsLivres);
            return "impressoras/cadastro";
        }

        // ===========================================================
        // CICLO DE VIDA DO IP
        // ===========================================================
        // Liberar IP antigo se mudou ou foi removido
        if (impressora.getId() != null) {
            Impressora antiga = impressoraRepository.findById(impressora.getId()).orElse(null);
            if (antiga != null && antiga.getRedeIp() != null) {
                if (impressora.getRedeIp() == null || !antiga.getRedeIp().getId().equals(impressora.getRedeIp().getId())) {
                    redeIpService.liberarIp(antiga.getRedeIp());
                }
            }
        }

        // Ocupar o novo IP
        if (impressora.getRedeIp() != null) {
            String nomeSetor = "TI Reserva";
            if (impressora.getSetor() != null) {
                if (impressora.getSetor().getNome() != null) {
                    nomeSetor = impressora.getSetor().getNome();
                } else if (impressora.getSetor().getId() != null) {
                    inventario.model.Setor setor = setorRepository.findById(impressora.getSetor().getId()).orElse(null);
                    if (setor != null) {
                        nomeSetor = setor.getNome();
                    }
                }
            }
            String observacaoDinamica = impressora.getMarcaModelo() + " - " + nomeSetor;
            redeIpService.ocuparIp(impressora.getRedeIp(), observacaoDinamica);
        }

        impressoraRepository.save(impressora);
        return "redirect:/impressoras";
    }

    @GetMapping("/editar/{id}")
    public String editor(@PathVariable Long id, Model model) {
        Impressora impressora = impressoraRepository.findById(id).orElse(null);
        model.addAttribute("impressora", impressora);
        model.addAttribute("setores", setorRepository.findAll());

        List<RedeIp> ipsLivres = new ArrayList<>(redeIpRepository.findByStatus(StatusIp.LIVRE));
        if (impressora != null && impressora.getRedeIp() != null) {
            ipsLivres.add(0, impressora.getRedeIp());
        }
        model.addAttribute("ipsLivres", ipsLivres);
        return "impressoras/cadastro";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        Impressora impressora = impressoraRepository.findById(id).orElse(null);
        if (impressora != null && impressora.getRedeIp() != null) {
            redeIpService.liberarIp(impressora.getRedeIp());
        }
        impressoraRepository.deleteById(id);
        return "redirect:/impressoras";
    }
}
