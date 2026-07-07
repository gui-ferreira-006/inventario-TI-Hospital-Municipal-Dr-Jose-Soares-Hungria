package inventario.controller;

import inventario.model.Setor;
import inventario.repository.SetorRepository;
import inventario.service.HistoricoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/setores")
public class SetorController {

    @Autowired
    private SetorRepository setorRepository;

    @Autowired
    private HistoricoService historicoService;


    @GetMapping
    public String listarSetores(Model model) {
        var listaDeSetores = setorRepository.findAll();
        model.addAttribute("setores", listaDeSetores);
        return "setores/lista";
    }

    @GetMapping("/novo")
    public String abrirFormularioCadastro(Model model) {
        model.addAttribute("setor", new Setor());
        return "setores/cadastro";
    }

    // Aqui vamos receber os dados do formulário, validar e salvar no banco
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("setor") Setor setor,
                         BindingResult result,
                         Model model) {
        // 1. Verificação de Duplicidade (Só fazemos se o nome não estiver em branco)
        if (setor.getNome() != null && !setor.getNome().trim().isEmpty()) {

            boolean nomeDuplicado = false;

            if (setor.getId() == null) {
                // Cenário A: É um CADASTRO NOVO (O ID ainda não existe)
                nomeDuplicado = setorRepository.existsByNome(setor.getNome());
            } else {
                // Cenário B: É uma EDIÇÃO (Ignora o próprio ID na hora da busca)
                nomeDuplicado = setorRepository.existsByNomeAndIdNot(setor.getNome(), setor.getId());
            }

            // Se o Java descobriu que o nome já existe no banco...
            if (nomeDuplicado) {
                // rejectValue: "Rejeita" o campo 'nome' e injeta a nossa mensagem personalizada nele!
                result.rejectValue("nome", "error.setor", "Este nome de setor já está cadastrado no sistema.");
            }
        }

        // 2. Se houver QUALQUER erro (seja de campo em branco ou de duplicidade)
        if (result.hasErrors()) {
            return "setores/cadastro";
        }

        // 3. Se passou por tudo limpo, salva no banco!
        if (setor.getId() == null) {
            historicoService.registrarEvento("DEPARTAMENTO", setor.getNome(), "CRIACAO", "Registro criado no sistema");
        } else {
            Setor antigo = setorRepository.findById(setor.getId()).orElse(null);
            if (antigo != null) {
                List<String> alteracoes = new ArrayList<>();
                if (!antigo.getNome().equals(setor.getNome())) {
                    alteracoes.add("Nome: " + antigo.getNome() + " ➔ " + setor.getNome());
                }
                if (!antigo.getBloco().equals(setor.getBloco())) {
                    alteracoes.add("Bloco: " + antigo.getBloco() + " ➔ " + setor.getBloco());
                }
                if (!alteracoes.isEmpty()) {
                    historicoService.registrarEvento("DEPARTAMENTO", setor.getNome(), "ATUALIZACAO", String.join(" | ", alteracoes));
                }
            }
        }

        setorRepository.save(setor);
        return "redirect:/setores";
    }

    @GetMapping("/editar/{id}")
    public String editarSetor(@PathVariable Long id, Model model) {
        Setor setorExistente = setorRepository.findById(id).orElse(null);
        model.addAttribute("setor", setorExistente);
        return "setores/cadastro";
    }

    @GetMapping("/excluir/{id}")
    public String excluirSetor(@PathVariable Long id) {
        Setor setor = setorRepository.findById(id).orElse(null);
        if (setor != null) {
            historicoService.registrarEvento("DEPARTAMENTO", setor.getNome(), "EXCLUSAO", "Registro excluído do sistema");
        }
        setorRepository.deleteById(id);
        return "redirect:/setores";
    }
}