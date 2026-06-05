package inventario.controller;

import inventario.service.RelatorioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

    @Autowired
    private RelatorioService relatorioService;

    @GetMapping("/configurar")
    public String abrirTelaConfiguracao(
            @RequestParam(name = "modulo", required = false, defaultValue = "geral") String modulo,
            Model model) {

        // Passa o módulo que veio da URL para o HTML preencher automaticamente o campo
        // formulário.
        model.addAttribute("moduloSelecionado", modulo);

        return "relatorios/configurar"; // Aponta para a pasta relatórios, arquivo configurar.html
    }

    @PostMapping("/gerar")
    public ResponseEntity<byte[]> gerarRelatorio(
            @RequestParam("modulo") String modulo,
            @RequestParam("titulo") String titulo,
            @RequestParam("formato") String formato,
            @RequestParam("orientacao") String orientacao,
            @RequestParam(value = "incluirCabecalho", required = false, defaultValue = "false") boolean incluirCabecalho,
            @RequestParam(value = "incluirDataHora", required = false, defaultValue = "false") boolean incluirDataHora,
            @RequestParam(value = "incluirNumeracao", required = false, defaultValue = "false") boolean incluirNumeracao,
            @RequestParam(value = "incluirRodape", required = false, defaultValue = "false") boolean incluirRodape,
            @RequestParam(value = "cols_computadores", required = false) List<String> colsComputadores,
            @RequestParam(value = "filtro_comp_status", required = false, defaultValue = "todos") String filtroCompStatus,
            @RequestParam(value = "cols_impressoras", required = false) List<String> colsImpressoras,
            @RequestParam(value = "cols_monitores", required = false) List<String> colsMonitores,
            @RequestParam(value = "cols_setores", required = false) List<String> colsSetores
    ) {
        try {
            byte[] bytes = relatorioService.gerarRelatorio(
                    modulo, titulo, formato, orientacao, incluirCabecalho, incluirDataHora,
                    incluirNumeracao, incluirRodape, colsComputadores, filtroCompStatus,
                    colsImpressoras, colsMonitores, colsSetores
            );

            String extensao = "PDF".equalsIgnoreCase(formato) ? "pdf" : "xlsx";
            String filename = "relatorio_" + modulo + "." + extensao;

            MediaType mediaType = "PDF".equalsIgnoreCase(formato) 
                    ? MediaType.APPLICATION_PDF 
                    : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(mediaType)
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
