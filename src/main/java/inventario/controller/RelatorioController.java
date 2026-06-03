package inventario.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/relatorios")

public class RelatorioController {

    @GetMapping("/configurar")
    public String abrirTelaConfiguracao(
            @RequestParam(name = "modulo", required = false, defaultValue = "geral") String modulo,
            Model model) {

        // Passa o módulo que veio da URL para o HTML preencher automaticamente o campo
        // do formulário.
        model.addAttribute("moduloSelecionado", modulo);

        return "relatorios/configurar"; // Aponta para a pasta relatórios, arquivo configurar.html
    }
}
