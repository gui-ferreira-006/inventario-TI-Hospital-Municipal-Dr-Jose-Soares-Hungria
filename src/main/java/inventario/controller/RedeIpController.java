package inventario.controller;

import inventario.model.RedeIp;
import inventario.model.StatusIp;
import inventario.repository.RedeIpRepository;
import inventario.service.RedeIpService;
import inventario.service.PadronizacaoObservacaoService;
import inventario.service.HistoricoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

@Controller
@RequestMapping("/redes")
public class RedeIpController {

    @Autowired
    private RedeIpRepository redeIpRepository;

    @Autowired
    private RedeIpService redeIpService;

    @Autowired
    private PadronizacaoObservacaoService padronizacaoObservacaoService;

    @Autowired
    private HistoricoService historicoService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false) String termo,
            @RequestParam(value = "termoBusca", required = false) String termoBusca,
            @RequestParam(value = "ordenarPor", required = false, defaultValue = "id") String ordenarPor,
            @RequestParam(value = "direcao", required = false, defaultValue = "ASC") String direcao,
            Model model) {

        String busca = (termo != null) ? termo : termoBusca;
        Sort.Direction dir = "DESC".equalsIgnoreCase(direcao) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String propriedade = "id";

        if ("enderecoIp".equalsIgnoreCase(ordenarPor)) {
            propriedade = "enderecoIp";
        } else if ("status".equalsIgnoreCase(ordenarPor)) {
            propriedade = "status";
        } else if ("observacao".equalsIgnoreCase(ordenarPor)) {
            propriedade = "observacao";
        }

        List<RedeIp> ips;
        boolean temBusca = (busca != null && !busca.trim().isEmpty());

        if ("setor".equalsIgnoreCase(ordenarPor)) {
            if (temBusca) {
                if (dir == Sort.Direction.ASC) {
                    ips = redeIpRepository.pesquisarGlobalOrdenadoPorSetorAsc(busca.trim());
                } else {
                    ips = redeIpRepository.pesquisarGlobalOrdenadoPorSetorDesc(busca.trim());
                }
            } else {
                if (dir == Sort.Direction.ASC) {
                    ips = redeIpRepository.findAllOrdenadoPorSetorAsc();
                } else {
                    ips = redeIpRepository.findAllOrdenadoPorSetorDesc();
                }
            }
        } else {
            Sort sort = Sort.by(dir, propriedade);
            if (temBusca) {
                ips = redeIpRepository.pesquisarGlobal(busca.trim(), sort);
            } else {
                ips = redeIpRepository.findAll(sort);
            }
        }

        model.addAttribute("ips", ips);
        model.addAttribute("termoBusca", temBusca ? busca.trim() : "");
        model.addAttribute("ordenarPor", ordenarPor);
        model.addAttribute("direcao", direcao);

        return "redes/lista";
    }

    @PostMapping("/gerar")
    public String gerar(@RequestParam String ipInicial, @RequestParam String ipFinal, RedirectAttributes redirectAttributes) {
        try {
            String resultado = redeIpService.gerarFaixaIp(ipInicial, ipFinal);
            redirectAttributes.addFlashAttribute("mensagemSucesso", resultado);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro inesperado ao gerar faixa de IPs: " + e.getMessage());
        }
        return "redirect:/redes";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model) {
        RedeIp ip = redeIpRepository.findById(id).orElse(null);
        if (ip == null) {
            return "redirect:/redes";
        }
        model.addAttribute("ip", ip);
        model.addAttribute("listaStatus", StatusIp.values());
        return "redes/cadastro";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("ip") RedeIp ip,
                         BindingResult result,
                         Model model) {
        
        if (ip.getId() != null) {
            RedeIp original = redeIpRepository.findById(ip.getId()).orElseThrow();
            
            // Regra A (Proteção de IP Ocupado): Se original for OCUPADO, força a ignorar alterações de IP e Status.
            if (original.getStatus() == StatusIp.OCUPADO) {
                ip.setEnderecoIp(original.getEnderecoIp());
                ip.setStatus(StatusIp.OCUPADO);
            } 
            // Regra B (Bloqueio de Falsa Ocupação): Se original não for OCUPADO, impede definir manualmente como OCUPADO.
            else if (ip.getStatus() == StatusIp.OCUPADO) {
                result.rejectValue("status", "error.redeIp", "Não é possível definir manualmente como OCUPADO. Para guardar este IP para uma máquina futura, utilize o status RESERVADO.");
            }
        } else {
            // Caso seja uma nova criação (getId() nulo) e o usuário tente criar como OCUPADO, também bloqueamos.
            if (ip.getStatus() == StatusIp.OCUPADO) {
                result.rejectValue("status", "error.redeIp", "Não é possível definir manualmente como OCUPADO. Para guardar este IP para uma máquina futura, utilize o status RESERVADO.");
            }
        }

        // Validação de duplicidade para o endereço IP
        if (ip.getEnderecoIp() != null && !ip.getEnderecoIp().trim().isEmpty()) {
            boolean ipDuplicado = (ip.getId() == null)
                ? redeIpRepository.existsByEnderecoIp(ip.getEnderecoIp())
                : redeIpRepository.existsByEnderecoIpAndIdNot(ip.getEnderecoIp(), ip.getId());
            if (ipDuplicado) {
                result.rejectValue("enderecoIp", "error.redeIp", "Este Endereço IP já está cadastrado.");
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("listaStatus", StatusIp.values());
            return "redes/cadastro";
        }

        // Instrumentação de Histórico/Auditoria
        if (ip.getId() != null) {
            RedeIp antigo = redeIpRepository.findById(ip.getId()).orElse(null);
            if (antigo != null) {
                List<String> alteracoes = new ArrayList<>();
                if (antigo.getStatus() != ip.getStatus()) {
                    alteracoes.add("Status: " + antigo.getStatus().name() + " ➔ " + ip.getStatus().name());
                }
                if (!Objects.equals(antigo.getObservacao(), ip.getObservacao())) {
                    alteracoes.add("Observação: " + antigo.getObservacao() + " ➔ " + ip.getObservacao());
                }
                if (!alteracoes.isEmpty()) {
                    historicoService.registrarEvento("REDE", ip.getEnderecoIp(), "ATUALIZACAO", String.join(" | ", alteracoes));
                }
            }
        }

        redeIpRepository.save(ip);
        return "redirect:/redes";
    }

    @PostMapping("/excluir-faixa")
    public String excluirFaixa(@RequestParam String ipInicialExclusao,
                               @RequestParam String ipFinalExclusao,
                               RedirectAttributes redirectAttributes) {
        try {
            String resultado = redeIpService.excluirFaixaIp(ipInicialExclusao, ipFinalExclusao);
            redirectAttributes.addFlashAttribute("mensagemSucesso", resultado);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro inesperado ao excluir faixa de IPs: " + e.getMessage());
        }
        return "redirect:/redes";
    }

    @GetMapping("/padronizar-base")
    public String padronizarBase(RedirectAttributes redirectAttributes) {
        String resultado = padronizacaoObservacaoService.padronizarTudo();
        redirectAttributes.addFlashAttribute("mensagemSucesso", resultado);
        return "redirect:/redes";
    }
}
