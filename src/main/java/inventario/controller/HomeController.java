package inventario.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import inventario.repository.ComputadorRepository;
import inventario.repository.SetorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;


@Controller
public class HomeController {

    @Autowired
    private ComputadorRepository computadorRepository;

    @Autowired
    private SetorRepository setorRepository;

    @GetMapping("/")
    public String exibirDashboard(Model model) {

        // O método .count() já vem pronto no Spring! Ele conta as linhas da tabela.
        long totalComputadores = computadorRepository.count();
        long totalSetores = setorRepository.count();

        model.addAttribute("totalComputadores", totalComputadores);
        model.addAttribute("totalSetores", totalSetores);

        return "index";
    }
}
