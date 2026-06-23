package inventario.controller;

import inventario.model.Computador;
import inventario.repository.ComputadorRepository;
import inventario.repository.SetorRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/computadores")
public class ComputadorController {

    @Autowired
    private ComputadorRepository computadorRepository;

    @Autowired
    private SetorRepository setorRepository;

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
            propriedade = "enderecoIp";
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
            if (computador.getHostname() == null) {
                result.rejectValue("hostname", "error.computador", "O Hostname é obrigatório para computadores alocados em setores.");
            }
            if (computador.getEnderecoIp() == null) {
                result.rejectValue("enderecoIp", "error.computador", "O Endereço IP é obirgatório para computadores alocados em setores.");
            }
        } else {
            // Se NÃO tem setor (Informática Reserva), GARANTE que o IP e Hostname fiquem vazios
            computador.setHostname(null);
            computador.setEnderecoIp(null);
        }

        // ===========================================================
        // 2. VALIDAÇÕES DE DUPLICIDADE (Hostname, Serial e IP)
        // ===========================================================

        if (computador.getHostname() != null) {
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

        if (computador.getEnderecoIp() != null) {
            boolean ipDuplicado = (computador.getId() == null)
                ? computadorRepository.existsByEnderecoIp(computador.getEnderecoIp())
                : computadorRepository.existsByEnderecoIpAndIdNot(computador.getEnderecoIp(), computador.getId());

            if (ipDuplicado) result.rejectValue("enderecoIp", "error.computador", "ATENÇÃO: Este Endereço IP já está em uso!");
        }

        // ===========================================================
        // 3. RETORNO DE ERROS
        // ===========================================================
        if (result.hasErrors()) {
            model.addAttribute("setores", setorRepository.findAll());
            return "computadores/cadastro";
        }

        computadorRepository.save(computador);
        return "redirect:/computadores";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Computador computador = computadorRepository.findById(id).orElse(null);
        model.addAttribute("computador", computador);
        model.addAttribute("setores", setorRepository.findAll());
        return "computadores/cadastro";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        computadorRepository.deleteById(id);
        return "redirect:/computadores";
    }
}