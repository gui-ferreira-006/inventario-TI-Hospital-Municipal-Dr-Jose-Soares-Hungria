package inventario.controller;

import inventario.model.Impressora;
import inventario.model.RedeIp;
import inventario.model.StatusIp;
import inventario.repository.ImpressoraRepository;
import inventario.repository.RedeIpRepository;
import inventario.repository.SetorRepository;
import inventario.service.RedeIpService;
import inventario.service.HistoricoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Autowired
    private HistoricoService historicoService;

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

        // Instrumentação de Histórico/Auditoria
        String ident = impressora.getSerialImpressora();

        if (impressora.getId() == null) {
            historicoService.registrarEvento("IMPRESSORA", ident, "CRIACAO", "Registro criado no sistema");
        } else {
            Impressora antiga = impressoraRepository.findById(impressora.getId()).orElse(null);
            if (antiga != null) {
                List<String> alteracoes = new ArrayList<>();
                if (!Objects.equals(antiga.getMarcaModelo(), impressora.getMarcaModelo())) {
                    alteracoes.add("Marca/Modelo: " + antiga.getMarcaModelo() + " ➔ " + impressora.getMarcaModelo());
                }

                String ipAntigo = (antiga.getRedeIp() != null) ? antiga.getRedeIp().getEnderecoIp() : null;
                String ipNovo = (impressora.getRedeIp() != null) ? impressora.getRedeIp().getEnderecoIp() : null;
                if (!Objects.equals(ipAntigo, ipNovo)) {
                    alteracoes.add("IP: " + (ipAntigo != null ? ipAntigo : "Nenhum") + " ➔ " + (ipNovo != null ? ipNovo : "Nenhum"));
                }

                Long idSetorAntigo = (antiga.getSetor() != null) ? antiga.getSetor().getId() : null;
                Long idSetorNovo = (impressora.getSetor() != null) ? impressora.getSetor().getId() : null;
                if (!Objects.equals(idSetorAntigo, idSetorNovo)) {
                    String nomeSetorAntigo = "Reserva TI";
                    if (antiga.getSetor() != null) {
                        nomeSetorAntigo = antiga.getSetor().getNome();
                    }
                    String nomeSetorNovo = "Reserva TI";
                    if (impressora.getSetor() != null && impressora.getSetor().getId() != null) {
                        inventario.model.Setor sNovo = setorRepository.findById(impressora.getSetor().getId()).orElse(null);
                        if (sNovo != null) {
                            nomeSetorNovo = sNovo.getNome();
                        }
                    }
                    alteracoes.add("Setor: " + nomeSetorAntigo + " ➔ " + nomeSetorNovo);
                }
                if (!alteracoes.isEmpty()) {
                    historicoService.registrarEvento("IMPRESSORA", ident, "ATUALIZACAO", String.join(" | ", alteracoes));
                }
            }
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
        Impressora imp = impressoraRepository.findById(id).orElse(null);
        if (imp != null) {
            if (imp.getRedeIp() != null) {
                redeIpService.liberarIp(imp.getRedeIp());
            }
            String ident = imp.getSerialImpressora();
            historicoService.registrarEvento("IMPRESSORA", ident, "EXCLUSAO", "Registro excluído do sistema");
        }
        impressoraRepository.deleteById(id);
        return "redirect:/impressoras";
    }
}
