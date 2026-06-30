package inventario.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import inventario.model.*;

import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class PdfReportGenerator {

    private String titulo;
    private String orientacao;
    private boolean incluirCabecalho;
    private boolean incluirDataHora;
    private boolean incluirNumeracao;
    private boolean incluirRodape;

    // Cores padrão estilizadas
    private static final Color PRIMARY_COLOR = new Color(30, 27, 75); // #1E1B4B (Navy Escuro)
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color ROW_ALT_COLOR = new Color(248, 250, 252); // #F8FAFC
    private static final Color BORDER_COLOR = new Color(226, 232, 240); // #E2E8F0

    public PdfReportGenerator(String titulo, String orientacao, boolean incluirCabecalho, 
                              boolean incluirDataHora, boolean incluirNumeracao, boolean incluirRodape) {
        this.titulo = titulo;
        this.orientacao = orientacao;
        this.incluirCabecalho = incluirCabecalho;
        this.incluirDataHora = incluirDataHora;
        this.incluirNumeracao = incluirNumeracao;
        this.incluirRodape = incluirRodape;
    }

    private Font getFont(float size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED, size, style, color);
    }

    private Document initDocument(OutputStream os) throws DocumentException {
        Document document;
        if ("landscape".equalsIgnoreCase(orientacao)) {
            document = new Document(PageSize.A4.rotate(), 36, 36, 54, 54);
        } else {
            document = new Document(PageSize.A4, 36, 36, 54, 54);
        }
        
        PdfWriter writer = PdfWriter.getInstance(document, os);
        if (incluirNumeracao || incluirRodape) {
            writer.setPageEvent(new HeaderFooterPageEvent(incluirNumeracao, incluirRodape, incluirDataHora));
        }
        document.open();
        
        // Adiciona cabeçalho se solicitado
        if (incluirCabecalho) {
            adicionarBannerInstitucional(document);
        }
        
        // Título do documento
        Font fontTitle = getFont(18, Font.BOLD, PRIMARY_COLOR);
        Paragraph titlePara = new Paragraph(titulo, fontTitle);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(20);
        document.add(titlePara);
        
        return document;
    }

    private void adicionarBannerInstitucional(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(15);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setPadding(15);
        cell.setBorder(Rectangle.NO_BORDER);

        Font fontH1 = getFont(14, Font.BOLD, TEXT_WHITE);
        Font fontH2 = getFont(10, Font.NORMAL, new Color(199, 210, 254)); // Indigo claro

        Paragraph p1 = new Paragraph("HOSPITAL MUNICIPAL DR. JOSÉ SOARES HUNGRIA", fontH1);
        p1.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p1);

        Paragraph p2 = new Paragraph("Sistema de Inventário de TI - Relatório do Inventário", fontH2);
        p2.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p2);

        headerTable.addCell(cell);
        document.add(headerTable);
    }

    public void gerarComputadores(OutputStream os, List<Computador> computadores, List<String> colunas) throws Exception {
        Document document = initDocument(os);
        if (colunas == null || colunas.isEmpty()) {
            colunas = List.of("id", "hostname", "serial", "ip", "status", "setor");
        }

        PdfPTable table = new PdfPTable(colunas.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        Font headFont = getFont(10, Font.BOLD, TEXT_WHITE);
        Font bodyFont = getFont(9, Font.NORMAL, Color.DARK_GRAY);

        // Cabeçalhos
        for (String col : colunas) {
            String label = switch (col) {
                case "id" -> "ID";
                case "hostname" -> "Hostname";
                case "serial" -> "Nº Série";
                case "ip" -> "IP";
                case "status" -> "Status";
                case "setor" -> "Setor";
                default -> col.toUpperCase();
            };
            PdfPCell cell = new PdfPCell(new Phrase(label, headFont));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }

        // Caso a lista esteja vazia
        if (computadores.isEmpty()) {
            PdfPCell cell = new PdfPCell(new Phrase("Nenhum registro encontrado para os filtros selecionados.", bodyFont));
            cell.setColspan(colunas.size());
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }

        // Corpo
        int rowIdx = 0;
        for (Computador c : computadores) {
            Color rowBg = (rowIdx++ % 2 == 0) ? Color.WHITE : ROW_ALT_COLOR;

            for (String col : colunas) {
                String val = switch (col) {
                    case "id" -> String.valueOf(c.getId());
                    case "hostname" -> c.getHostname() != null ? c.getHostname() : "-";
                    case "serial" -> c.getSerialComputador();
                    case "ip" -> c.getRedeIp() != null ? c.getRedeIp().getEnderecoIp() : "-";
                    case "status" -> c.getStatus() != null ? c.getStatus() : "-";
                    case "setor" -> c.getSetor() != null ? c.getSetor().getNome() : "-";
                    default -> "";
                };
                PdfPCell cell = new PdfPCell(new Phrase(val, bodyFont));
                cell.setBackgroundColor(rowBg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                cell.setBorderColor(BORDER_COLOR);
                table.addCell(cell);
            }
        }

        document.add(table);
        document.close();
    }

    public void gerarImpressoras(OutputStream os, List<Impressora> impressoras, List<String> colunas) throws Exception {
        Document document = initDocument(os);
        if (colunas == null || colunas.isEmpty()) {
            colunas = List.of("id", "marcaModelo", "serial", "ip", "setor");
        }

        PdfPTable table = new PdfPTable(colunas.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        Font headFont = getFont(10, Font.BOLD, TEXT_WHITE);
        Font bodyFont = getFont(9, Font.NORMAL, Color.DARK_GRAY);

        for (String col : colunas) {
            String label = switch (col) {
                case "id" -> "ID";
                case "marcaModelo" -> "Marca/Modelo";
                case "serial" -> "Nº Série";
                case "ip" -> "IP";
                case "setor" -> "Setor";
                default -> col.toUpperCase();
            };
            PdfPCell cell = new PdfPCell(new Phrase(label, headFont));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }

        // Caso a lista esteja vazia
        if (impressoras.isEmpty()) {
            PdfPCell cell = new PdfPCell(new Phrase("Nenhum registro encontrado para os filtros selecionados.", bodyFont));
            cell.setColspan(colunas.size());
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }

        int rowIdx = 0;
        for (Impressora imp : impressoras) {
            Color rowBg = (rowIdx++ % 2 == 0) ? Color.WHITE : ROW_ALT_COLOR;

            for (String col : colunas) {
                String val = switch (col) {
                    case "id" -> String.valueOf(imp.getId());
                    case "marcaModelo" -> imp.getMarcaModelo();
                    case "serial" -> imp.getSerialImpressora();
                    case "ip" -> imp.getRedeIp() != null ? imp.getRedeIp().getEnderecoIp() : "-";
                    case "setor" -> imp.getSetor() != null ? imp.getSetor().getNome() : "-";
                    default -> "";
                };
                PdfPCell cell = new PdfPCell(new Phrase(val, bodyFont));
                cell.setBackgroundColor(rowBg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                cell.setBorderColor(BORDER_COLOR);
                table.addCell(cell);
            }
        }

        document.add(table);
        document.close();
    }

    public void gerarMonitores(OutputStream os, List<Monitor> monitores, List<String> colunas) throws Exception {
        Document document = initDocument(os);
        if (colunas == null || colunas.isEmpty()) {
            colunas = List.of("id", "marca", "modelo", "serial", "computador");
        }

        PdfPTable table = new PdfPTable(colunas.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        Font headFont = getFont(10, Font.BOLD, TEXT_WHITE);
        Font bodyFont = getFont(9, Font.NORMAL, Color.DARK_GRAY);

        for (String col : colunas) {
            String label = switch (col) {
                case "id" -> "ID";
                case "marca" -> "Marca";
                case "modelo" -> "Modelo";
                case "serial" -> "Nº Série";
                case "computador" -> "Comp. Alocado";
                default -> col.toUpperCase();
            };
            PdfPCell cell = new PdfPCell(new Phrase(label, headFont));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }

        // Caso a lista esteja vazia
        if (monitores.isEmpty()) {
            PdfPCell cell = new PdfPCell(new Phrase("Nenhum registro encontrado para os filtros selecionados.", bodyFont));
            cell.setColspan(colunas.size());
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }

        int rowIdx = 0;
        for (Monitor m : monitores) {
            Color rowBg = (rowIdx++ % 2 == 0) ? Color.WHITE : ROW_ALT_COLOR;

            for (String col : colunas) {
                String val = switch (col) {
                    case "id" -> String.valueOf(m.getId());
                    case "marca" -> m.getMarca();
                    case "modelo" -> m.getModelo();
                    case "serial" -> m.getSerialMonitor();
                    case "computador" -> m.getComputador() != null ? m.getComputador().getHostname() : "Não Alocado";
                    default -> "";
                };
                PdfPCell cell = new PdfPCell(new Phrase(val, bodyFont));
                cell.setBackgroundColor(rowBg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                cell.setBorderColor(BORDER_COLOR);
                table.addCell(cell);
            }
        }

        document.add(table);
        document.close();
    }

    public void gerarSetores(OutputStream os, List<Setor> setores, List<String> colunas) throws Exception {
        Document document = initDocument(os);
        if (colunas == null || colunas.isEmpty()) {
            colunas = List.of("id", "nome", "bloco");
        }

        PdfPTable table = new PdfPTable(colunas.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        Font headFont = getFont(10, Font.BOLD, TEXT_WHITE);
        Font bodyFont = getFont(9, Font.NORMAL, Color.DARK_GRAY);

        for (String col : colunas) {
            String label = switch (col) {
                case "id" -> "ID";
                case "nome" -> "Nome Setor";
                case "bloco" -> "Bloco / Localização";
                default -> col.toUpperCase();
            };
            PdfPCell cell = new PdfPCell(new Phrase(label, headFont));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }

        // Caso a lista esteja vazia
        if (setores.isEmpty()) {
            PdfPCell cell = new PdfPCell(new Phrase("Nenhum registro encontrado para os filtros selecionados.", bodyFont));
            cell.setColspan(colunas.size());
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }

        int rowIdx = 0;
        for (Setor s : setores) {
            Color rowBg = (rowIdx++ % 2 == 0) ? Color.WHITE : ROW_ALT_COLOR;

            for (String col : colunas) {
                String val = switch (col) {
                    case "id" -> String.valueOf(s.getId());
                    case "nome" -> s.getNome();
                    case "bloco" -> s.getBloco();
                    default -> "";
                };
                PdfPCell cell = new PdfPCell(new Phrase(val, bodyFont));
                cell.setBackgroundColor(rowBg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                cell.setBorderColor(BORDER_COLOR);
                table.addCell(cell);
            }
        }

        document.add(table);
        document.close();
    }

    @SuppressWarnings("unchecked")
    public void gerarGeral(OutputStream os, Map<String, Object> stats) throws Exception {
        Document document = initDocument(os);

        Font fontH = getFont(12, Font.BOLD, PRIMARY_COLOR);
        Font bodyFont = getFont(10, Font.NORMAL, Color.DARK_GRAY);
        Font headTableFont = getFont(10, Font.BOLD, TEXT_WHITE);

        // Seção 1: Totais Consolidados
        Paragraph pTitle1 = new Paragraph("Totais por Módulo", fontH);
        pTitle1.setSpacingBefore(10);
        document.add(pTitle1);
        
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(100);
        totalTable.setSpacingBefore(10);
        totalTable.setSpacingAfter(20);

        // Cabeçalhos
        PdfPCell h1 = new PdfPCell(new Phrase("Módulo", headTableFont));
        h1.setBackgroundColor(PRIMARY_COLOR);
        h1.setPadding(6);
        h1.setBorderColor(BORDER_COLOR);
        totalTable.addCell(h1);

        PdfPCell h2 = new PdfPCell(new Phrase("Total Registrado", headTableFont));
        h2.setBackgroundColor(PRIMARY_COLOR);
        h2.setPadding(6);
        h2.setBorderColor(BORDER_COLOR);
        totalTable.addCell(h2);

        String[][] modTotals = {
            {"Computadores", String.valueOf(stats.get("totalComputadores"))},
            {"Impressoras", String.valueOf(stats.get("totalImpressoras"))},
            {"Monitores", String.valueOf(stats.get("totalMonitores"))},
            {"Setores (Departamentos)", String.valueOf(stats.get("totalSetores"))}
        };

        for (int i = 0; i < modTotals.length; i++) {
            Color bg = (i % 2 == 0) ? Color.WHITE : ROW_ALT_COLOR;
            
            PdfPCell cMod = new PdfPCell(new Phrase(modTotals[i][0], bodyFont));
            cMod.setBackgroundColor(bg);
            cMod.setPadding(6);
            cMod.setBorderColor(BORDER_COLOR);
            totalTable.addCell(cMod);

            PdfPCell cVal = new PdfPCell(new Phrase(modTotals[i][1], bodyFont));
            cVal.setBackgroundColor(bg);
            cVal.setPadding(6);
            cVal.setBorderColor(BORDER_COLOR);
            totalTable.addCell(cVal);
        }
        document.add(totalTable);

        // Seção 2: Computadores por Status
        Paragraph pTitle2 = new Paragraph("Computadores por Status", fontH);
        pTitle2.setSpacingBefore(10);
        document.add(pTitle2);
        
        PdfPTable statusTable = new PdfPTable(2);
        statusTable.setWidthPercentage(100);
        statusTable.setSpacingBefore(10);
        statusTable.setSpacingAfter(20);

        PdfPCell sh1 = new PdfPCell(new Phrase("Status", headTableFont));
        sh1.setBackgroundColor(PRIMARY_COLOR);
        sh1.setPadding(6);
        sh1.setBorderColor(BORDER_COLOR);
        statusTable.addCell(sh1);

        PdfPCell sh2 = new PdfPCell(new Phrase("Quantidade", headTableFont));
        sh2.setBackgroundColor(PRIMARY_COLOR);
        sh2.setPadding(6);
        sh2.setBorderColor(BORDER_COLOR);
        statusTable.addCell(sh2);

        Map<String, Long> statusMap = (Map<String, Long>) stats.get("computadoresPorStatus");
        int idx = 0;
        for (Map.Entry<String, Long> entry : statusMap.entrySet()) {
            Color bg = (idx++ % 2 == 0) ? Color.WHITE : ROW_ALT_COLOR;
            
            PdfPCell cellName = new PdfPCell(new Phrase(entry.getKey(), bodyFont));
            cellName.setBackgroundColor(bg);
            cellName.setPadding(6);
            cellName.setBorderColor(BORDER_COLOR);
            statusTable.addCell(cellName);

            PdfPCell cellVal = new PdfPCell(new Phrase(String.valueOf(entry.getValue()), bodyFont));
            cellVal.setBackgroundColor(bg);
            cellVal.setPadding(6);
            cellVal.setBorderColor(BORDER_COLOR);
            statusTable.addCell(cellVal);
        }
        document.add(statusTable);

        // Seção 3: Distribuição de Monitores
        Paragraph pTitle3 = new Paragraph("Distribuição de Monitores", fontH);
        pTitle3.setSpacingBefore(10);
        document.add(pTitle3);
        
        PdfPTable monTable = new PdfPTable(2);
        monTable.setWidthPercentage(100);
        monTable.setSpacingBefore(10);

        PdfPCell mh1 = new PdfPCell(new Phrase("Situação", headTableFont));
        mh1.setBackgroundColor(PRIMARY_COLOR);
        mh1.setPadding(6);
        mh1.setBorderColor(BORDER_COLOR);
        monTable.addCell(mh1);

        PdfPCell mh2 = new PdfPCell(new Phrase("Quantidade", headTableFont));
        mh2.setBackgroundColor(PRIMARY_COLOR);
        mh2.setPadding(6);
        mh2.setBorderColor(BORDER_COLOR);
        monTable.addCell(mh2);

        Object[][] monData = {
            {"Alocados em Computadores", String.valueOf(stats.get("monitoresAlocados"))},
            {"Livres em Estoque / Backup", String.valueOf(stats.get("monitoresLivres"))}
        };

        for (int i = 0; i < monData.length; i++) {
            Color bg = (i % 2 == 0) ? Color.WHITE : ROW_ALT_COLOR;

            PdfPCell cellName = new PdfPCell(new Phrase((String) monData[i][0], bodyFont));
            cellName.setBackgroundColor(bg);
            cellName.setPadding(6);
            cellName.setBorderColor(BORDER_COLOR);
            monTable.addCell(cellName);

            PdfPCell cellVal = new PdfPCell(new Phrase((String) monData[i][1], bodyFont));
            cellVal.setBackgroundColor(bg);
            cellVal.setPadding(6);
            cellVal.setBorderColor(BORDER_COLOR);
            monTable.addCell(cellVal);
        }
        document.add(monTable);

        document.close();
    }

    // Classe auxiliar para cabeçalho, rodapé e numeração
    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private boolean showPageNum;
        private boolean showFooter;
        private boolean showTime;
        private Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

        public HeaderFooterPageEvent(boolean showPageNum, boolean showFooter, boolean showTime) {
            this.showPageNum = showPageNum;
            this.showFooter = showFooter;
            this.showTime = showTime;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float pageHeight = document.getPageSize().getHeight();
            float pageWidth = document.getPageSize().getWidth();

            // Texto de rodapé
            StringBuilder footerText = new StringBuilder();
            if (showFooter) {
                footerText.append("Hospital Municipal Dr. José Soares Hungria");
            }
            if (showTime) {
                if (footerText.length() > 0) footerText.append("  |  ");
                footerText.append("Gerado em: ")
                          .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }

            // Desenhar linha separadora
            cb.setLineWidth(0.5f);
            cb.setColorStroke(Color.LIGHT_GRAY);
            cb.moveTo(36, 40);
            cb.lineTo(pageWidth - 36, 40);
            cb.stroke();

            // Rodapé à esquerda
            if (footerText.length() > 0) {
                ColumnText.showTextAligned(
                    cb, Element.ALIGN_LEFT, 
                    new Phrase(footerText.toString(), footerFont), 
                    36, 26, 0
                );
            }

            // Numeração de páginas à direita
            if (showPageNum) {
                String pageNum = "Pág. " + writer.getPageNumber();
                ColumnText.showTextAligned(
                    cb, Element.ALIGN_RIGHT, 
                    new Phrase(pageNum, footerFont), 
                    pageWidth - 36, 26, 0
                );
            }
        }
    }
}
