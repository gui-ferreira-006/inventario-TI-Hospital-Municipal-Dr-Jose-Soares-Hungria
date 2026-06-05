package inventario.service;

import inventario.model.*;
import inventario.repository.*;
import inventario.util.PdfReportGenerator;
import inventario.util.ExcelReportGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    @Autowired
    private ComputadorRepository computadorRepository;

    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private SetorRepository setorRepository;

    public byte[] gerarRelatorio(
            String modulo,
            String titulo,
            String formato,
            String orientacao,
            boolean incluirCabecalho,
            boolean incluirDataHora,
            boolean incluirNumeracao,
            boolean incluirRodape,
            List<String> colsComputadores,
            String filtroCompStatus,
            List<String> colsImpressoras,
            List<String> colsMonitores,
            List<String> colsSetores
    ) throws Exception {

        if ("PDF".equalsIgnoreCase(formato)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfReportGenerator pdfGen = new PdfReportGenerator(
                titulo, orientacao, incluirCabecalho, incluirDataHora, incluirNumeracao, incluirRodape
            );
            
            if ("computadores".equals(modulo)) {
                List<Computador> dados = obterComputadores(filtroCompStatus);
                pdfGen.gerarComputadores(baos, dados, colsComputadores);
            } else if ("impressoras".equals(modulo)) {
                List<Impressora> dados = obterImpressoras();
                pdfGen.gerarImpressoras(baos, dados, colsImpressoras);
            } else if ("monitores".equals(modulo)) {
                List<Monitor> dados = obterMonitores();
                pdfGen.gerarMonitores(baos, dados, colsMonitores);
            } else if ("setores".equals(modulo)) {
                List<Setor> dados = obterSetores();
                pdfGen.gerarSetores(baos, dados, colsSetores);
            } else { // geral
                Map<String, Object> stats = obterEstatisticasGerais();
                pdfGen.gerarGeral(baos, stats);
            }
            return baos.toByteArray();
        } else if ("EXCEL".equalsIgnoreCase(formato)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ExcelReportGenerator excelGen = new ExcelReportGenerator(titulo);

            if ("computadores".equals(modulo)) {
                List<Computador> dados = obterComputadores(filtroCompStatus);
                excelGen.gerarComputadores(baos, dados, colsComputadores);
            } else if ("impressoras".equals(modulo)) {
                List<Impressora> dados = obterImpressoras();
                excelGen.gerarImpressoras(baos, dados, colsImpressoras);
            } else if ("monitores".equals(modulo)) {
                List<Monitor> dados = obterMonitores();
                excelGen.gerarMonitores(baos, dados, colsMonitores);
            } else if ("setores".equals(modulo)) {
                List<Setor> dados = obterSetores();
                excelGen.gerarSetores(baos, dados, colsSetores);
            } else { // geral
                Map<String, Object> stats = obterEstatisticasGerais();
                excelGen.gerarGeral(baos, stats);
            }
            return baos.toByteArray();
        }
        
        throw new IllegalArgumentException("Formato não suportado: " + formato);
    }

    private List<Computador> obterComputadores(String status) {
        if (status == null || "todos".equalsIgnoreCase(status)) {
            return computadorRepository.findAll(PageRequest.of(0, 500)).getContent();
        }
        return computadorRepository.findByStatus(status, PageRequest.of(0, 500));
    }

    private List<Impressora> obterImpressoras() {
        return impressoraRepository.findAll(PageRequest.of(0, 500)).getContent();
    }

    private List<Monitor> obterMonitores() {
        return monitorRepository.findAll(PageRequest.of(0, 500)).getContent();
    }

    private List<Setor> obterSetores() {
        return setorRepository.findAll(PageRequest.of(0, 500)).getContent();
    }

    private Map<String, Object> obterEstatisticasGerais() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        long totalComp = computadorRepository.count();
        long totalImp = impressoraRepository.count();
        long totalMon = monitorRepository.count();
        long totalSet = setorRepository.count();

        stats.put("totalComputadores", totalComp);
        stats.put("totalImpressoras", totalImp);
        stats.put("totalMonitores", totalMon);
        stats.put("totalSetores", totalSet);

        // Computadores por status (via query agrupada)
        List<Object[]> rawGroup = computadorRepository.countComputadoresGroupByStatus();
        Map<String, Long> compPorStatus = new LinkedHashMap<>();
        for (Object[] row : rawGroup) {
            String status = row[0] != null ? (String) row[0] : "Sem Status";
            Long count = (Long) row[1];
            compPorStatus.put(status, count);
        }
        stats.put("computadoresPorStatus", compPorStatus);

        // Monitores alocados vs total (via contagem direta no banco)
        long monitoresAlocados = monitorRepository.countByComputadorIsNotNull();
        stats.put("monitoresAlocados", monitoresAlocados);
        stats.put("monitoresLivres", totalMon - monitoresAlocados);

        return stats;
    }
}
