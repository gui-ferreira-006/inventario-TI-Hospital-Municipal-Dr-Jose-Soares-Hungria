package inventario.controller;

import inventario.model.Computador;
import inventario.model.RedeIp;
import inventario.model.StatusIp;
import inventario.repository.ComputadorRepository;
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
@RequestMapping("/computadores")
public class ComputadorController {

    @Autowired
    private ComputadorRepository computadorRepository;

    @Autowired
    private SetorRepository setorRepository;

    @Autowired
    private RedeIpRepository redeIpRepository;

    @Autowired
    private RedeIpService redeIpService;

    @Autowired
    private HistoricoService historicoService;

    @GetMapping
    public String listarComputadores(
            @RequestParam(value = "termo", required = false) String termo,
            @RequestParam(value = "ordenarPor", required = false, defaultValue = "setor") String ordenarPor,
            @RequestParam(value = "direcao", required = false, defaultValue = "ASC") String direcao,
            Model model) {

        Sort.Direction dir = "DESC".equalsIgnoreCase(direcao) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String propriedade = "id";

        if ("hostname".equalsIgnoreCase(ordenarPor)) {
            propriedade = "hostname";
        } else if ("serialComputador".equalsIgnoreCase(ordenarPor)) {
            propriedade = "serialComputador";
        } else if ("enderecoIp".equalsIgnoreCase(ordenarPor)) {
            propriedade = "redeIp.enderecoIp";
        } else if ("status".equalsIgnoreCase(ordenarPor)) {
            propriedade = "status";
        } else if ("setor".equalsIgnoreCase(ordenarPor)) {
            propriedade = "setor.nome";
        }

        Sort sort = Sort.by(dir, propriedade);
        List<Computador> computadores;

        if (termo != null && !termo.trim().isEmpty()) {
            computadores = computadorRepository.pesquisarGlobal(termo.trim(), sort);
            model.addAttribute("termoBusca", termo.trim());
        } else {
            computadores = computadorRepository.findAll(sort);
        }

        model.addAttribute("computadores", computadores);
        model.addAttribute("ordenarPor", ordenarPor);
        model.addAttribute("direcao", direcao);

        return "computadores/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("computador", new Computador());
        model.addAttribute("setores", setorRepository.findAll());
        model.addAttribute("ipsLivres", redeIpRepository.findByStatus(StatusIp.LIVRE));
        return "computadores/cadastro";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("computador") Computador computador,
                         BindingResult result,
                         Model model) {
        // ===========================================================
        // 1. REGRA DE NEGÓCIO CONDICIONAL: Se o computador tiver um setor, o hostname é obrigatório.
        // ===========================================================
        boolean temSetorAlocado = computador.getSetor() != null && computador.getSetor().getId() != null;

        if (temSetorAlocado) {
            // Se for alocado para um Setor, EXIGE Hostname e IP
            if (computador.getHostname() == null || computador.getHostname().trim().isEmpty()) {
                result.rejectValue("hostname", "error.computador", "O Hostname é obrigatório para computadores alocados em setores.");
            }
            if (computador.getRedeIp() == null) {
                result.rejectValue("redeIp", "error.computador", "O Endereço IP é obrigatório para computadores alocados em setores.");
            }
        } else {
            // Se NÃO tem setor (Informática Reserva), GARANTE que o IP e Hostname fiquem vazios
            computador.setHostname(null);
            computador.setRedeIp(null);
        }

        // ===========================================================
        // 2. VALIDAÇÕES DE DUPLICIDADE (Hostname e Serial)
        // ===========================================================

        if (computador.getHostname() != null && !computador.getHostname().trim().isEmpty()) {
            boolean hostnameDuplicado = (computador.getId() == null)
                ? computadorRepository.existsByHostname(computador.getHostname())
                : computadorRepository.existsByHostnameAndIdNot(computador.getHostname(), computador.getId());

            if (hostnameDuplicado) result.rejectValue("hostname", "error.computador", "Este Hostname já está cadastrado.");
        }

        if (computador.getSerialComputador() != null && !computador.getSerialComputador().trim().isEmpty()) {
            boolean serialDuplicado = (computador.getId() == null)
                ? computadorRepository.existsBySerialComputador(computador.getSerialComputador())
                : computadorRepository.existsBySerialComputadorAndIdNot(computador.getSerialComputador(), computador.getId());

            if (serialDuplicado) result.rejectValue("serialComputador", "error.computador", "Este Número de Serial já está cadastrado.");
        }

        // ===========================================================
        // 3. RETORNO DE ERROS
        // ===========================================================
        if (result.hasErrors()) {
            model.addAttribute("setores", setorRepository.findAll());
            List<RedeIp> ipsLivres = new ArrayList<>(redeIpRepository.findByStatus(StatusIp.LIVRE));
            if (computador.getRedeIp() != null) {
                ipsLivres.add(0, computador.getRedeIp());
            }
            model.addAttribute("ipsLivres", ipsLivres);
            return "computadores/cadastro";
        }

        // ===========================================================
        // 4. CICLO DE VIDA DO IP
        // ===========================================================
        // Liberar IP antigo se mudou ou foi removido
        if (computador.getId() != null) {
            Computador antigo = computadorRepository.findById(computador.getId()).orElse(null);
            if (antigo != null && antigo.getRedeIp() != null) {
                if (computador.getRedeIp() == null || !antigo.getRedeIp().getId().equals(computador.getRedeIp().getId())) {
                    redeIpService.liberarIp(antigo.getRedeIp());
                }
            }
        }

        // Ocupar o novo IP
        if (computador.getRedeIp() != null) {
            String nomeSetor = "TI Reserva";
            if (computador.getSetor() != null) {
                if (computador.getSetor().getNome() != null) {
                    nomeSetor = computador.getSetor().getNome();
                } else if (computador.getSetor().getId() != null) {
                    inventario.model.Setor setor = setorRepository.findById(computador.getSetor().getId()).orElse(null);
                    if (setor != null) {
                        nomeSetor = setor.getNome();
                    }
                }
            }
            // Pega o hostname. Se for nulo/vazio, usa o Serial (para o caso de máquinas na reserva)
            String identificador = (computador.getHostname() != null && !computador.getHostname().trim().isEmpty()) 
                                   ? computador.getHostname() 
                                   : "S/N: " + computador.getSerialComputador();
            
            String observacaoDinamica = "Computador " + identificador + " - " + nomeSetor;
            redeIpService.ocuparIp(computador.getRedeIp(), observacaoDinamica);
        }

        // Instrumentação de Histórico/Auditoria
        String ident = (computador.getHostname() != null && !computador.getHostname().isEmpty()) 
                       ? computador.getHostname() 
                       : computador.getSerialComputador();

        if (computador.getId() == null) {
            historicoService.registrarEvento("COMPUTADOR", ident, "CRIACAO", "Registro criado no sistema");
        } else {
            Computador antigo = computadorRepository.findById(computador.getId()).orElse(null);
            if (antigo != null) {
                List<String> alteracoes = new ArrayList<>();
                if (!Objects.equals(antigo.getHostname(), computador.getHostname())) {
                    alteracoes.add("Hostname: " + (antigo.getHostname() != null ? antigo.getHostname() : "Nenhum") + " ➔ " + (computador.getHostname() != null ? computador.getHostname() : "Nenhum"));
                }

                String ipAntigo = (antigo.getRedeIp() != null) ? antigo.getRedeIp().getEnderecoIp() : null;
                String ipNovo = (computador.getRedeIp() != null) ? computador.getRedeIp().getEnderecoIp() : null;
                if (!Objects.equals(ipAntigo, ipNovo)) {
                    alteracoes.add("IP: " + (ipAntigo != null ? ipAntigo : "Nenhum") + " ➔ " + (ipNovo != null ? ipNovo : "Nenhum"));
                }

                Long idSetorAntigo = (antigo.getSetor() != null) ? antigo.getSetor().getId() : null;
                Long idSetorNovo = (computador.getSetor() != null) ? computador.getSetor().getId() : null;
                if (!Objects.equals(idSetorAntigo, idSetorNovo)) {
                    String nomeSetorAntigo = (antigo.getSetor() != null) ? antigo.getSetor().getNome() : "Nenhum";
                    String nomeSetorNovo = "Nenhum";
                    if (computador.getSetor() != null && computador.getSetor().getId() != null) {
                        inventario.model.Setor sNovo = setorRepository.findById(computador.getSetor().getId()).orElse(null);
                        if (sNovo != null) {
                            nomeSetorNovo = sNovo.getNome();
                        }
                    }
                    alteracoes.add("Setor: " + nomeSetorAntigo + " ➔ " + nomeSetorNovo);
                }

                String statusAntigo = (antigo.getStatus() != null && !antigo.getStatus().trim().isEmpty()) ? antigo.getStatus() : "Sem Status";
                String statusNovo = (computador.getStatus() != null && !computador.getStatus().trim().isEmpty()) ? computador.getStatus() : "Sem Status";

                if (!statusAntigo.equals(statusNovo)) {
                    alteracoes.add("Status: " + statusAntigo + " ➔ " + statusNovo);
                }

                if (!alteracoes.isEmpty()) {
                    historicoService.registrarEvento("COMPUTADOR", ident, "ATUALIZACAO", String.join(" | ", alteracoes));
                }
            }
        }

        computadorRepository.save(computador);
        return "redirect:/computadores";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Computador computador = computadorRepository.findById(id).orElse(null);
        model.addAttribute("computador", computador);
        model.addAttribute("setores", setorRepository.findAll());

        List<RedeIp> ipsLivres = new ArrayList<>(redeIpRepository.findByStatus(StatusIp.LIVRE));
        if (computador != null && computador.getRedeIp() != null) {
            ipsLivres.add(0, computador.getRedeIp());
        }
        model.addAttribute("ipsLivres", ipsLivres);
        return "computadores/cadastro";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        Computador pc = computadorRepository.findById(id).orElse(null);
        if (pc != null) {
            if (pc.getRedeIp() != null) {
                redeIpService.liberarIp(pc.getRedeIp());
            }
            String ident = (pc.getHostname() != null && !pc.getHostname().isEmpty()) ? pc.getHostname() : pc.getSerialComputador();
            historicoService.registrarEvento("COMPUTADOR", ident, "EXCLUSAO", "Registro excluído do sistema");
        }
        computadorRepository.deleteById(id);
        return "redirect:/computadores";
    }
}