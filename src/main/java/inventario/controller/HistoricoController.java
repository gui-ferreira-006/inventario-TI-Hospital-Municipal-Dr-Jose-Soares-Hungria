package inventario.controller;

import inventario.model.RegistroHistorico;
import inventario.repository.HistoricoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/historico")
public class HistoricoController {

    @Autowired
    private HistoricoRepository historicoRepository;

    @GetMapping
    public String listarHistorico(
            @RequestParam(value = "termo", required = false) String termo,
            @RequestParam(value = "ordenarPor", required = false, defaultValue = "dataRegistro") String ordenarPor,
            @RequestParam(value = "direcao", required = false, defaultValue = "DESC") String direcao,
            Model model) {

        Sort.Direction dir = "ASC".equalsIgnoreCase(direcao) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String propriedade = "dataRegistro";

        if ("modulo".equalsIgnoreCase(ordenarPor)) {
            propriedade = "modulo";
        } else if ("identificador".equalsIgnoreCase(ordenarPor)) {
            propriedade = "identificador";
        } else if ("acao".equalsIgnoreCase(ordenarPor)) {
            propriedade = "acao";
        }

        Sort sort = Sort.by(dir, propriedade);
        List<RegistroHistorico> registros;

        if (termo != null && !termo.trim().isEmpty()) {
            registros = historicoRepository.pesquisarGlobal(termo.trim(), sort);
            model.addAttribute("termoBusca", termo.trim());
        } else {
            registros = historicoRepository.findAll(sort);
        }

        model.addAttribute("registros", registros);
        model.addAttribute("ordenarPor", ordenarPor);
        model.addAttribute("direcao", direcao);

        return "historico/lista";
    }
}
