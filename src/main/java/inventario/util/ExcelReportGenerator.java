package inventario.util;

import inventario.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class ExcelReportGenerator {

    private String titulo;

    public ExcelReportGenerator(String titulo) {
        this.titulo = titulo;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Cor de fundo #1E1B4B (Navy Escuro)
        byte[] rgb = new byte[]{(byte) 30, (byte) 27, (byte) 75};
        if (style instanceof org.apache.poi.xssf.usermodel.XSSFCellStyle) {
            org.apache.poi.xssf.usermodel.XSSFCellStyle xstyle = (org.apache.poi.xssf.usermodel.XSSFCellStyle) style;
            xstyle.setFillForegroundColor(new XSSFColor(rgb, null));
        } else {
            style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Fonte do cabeçalho
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        // Alinhamento
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Bordas
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        
        byte[] rgb = new byte[]{(byte) 30, (byte) 27, (byte) 75};
        if (font instanceof org.apache.poi.xssf.usermodel.XSSFFont) {
            org.apache.poi.xssf.usermodel.XSSFFont xfont = (org.apache.poi.xssf.usermodel.XSSFFont) font;
            xfont.setColor(new XSSFColor(rgb, null));
        } else {
            font.setColor(IndexedColors.DARK_BLUE.getIndex());
        }
        style.setFont(font);
        return style;
    }

    public void gerarComputadores(OutputStream os, List<Computador> computadores, List<String> colunas) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Computadores");

        if (colunas == null || colunas.isEmpty()) {
            colunas = List.of("id", "hostname", "serial", "ip", "status", "setor");
        }

        // Título
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(titulo);
        titleCell.setCellStyle(createTitleStyle(workbook));

        // Cabeçalhos
        Row headerRow = sheet.createRow(2);
        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < colunas.size(); i++) {
            Cell cell = headerRow.createCell(i);
            String label = switch (colunas.get(i)) {
                case "id" -> "ID";
                case "hostname" -> "Hostname";
                case "serial" -> "Nº Série";
                case "ip" -> "IP";
                case "status" -> "Status";
                case "setor" -> "Setor";
                default -> colunas.get(i).toUpperCase();
            };
            cell.setCellValue(label);
            cell.setCellStyle(headerStyle);
        }

        // Conteúdo
        CellStyle bodyStyle = createBodyStyle(workbook);
        int rIdx = 3;
        if (computadores.isEmpty()) {
            Row row = sheet.createRow(rIdx++);
            for (int i = 0; i < colunas.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(bodyStyle);
            }
            row.getCell(0).setCellValue("Nenhum registro encontrado para os filtros selecionados.");
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, colunas.size() - 1));
        } else {
            for (Computador c : computadores) {
                Row row = sheet.createRow(rIdx++);
                for (int i = 0; i < colunas.size(); i++) {
                    Cell cell = row.createCell(i);
                    String val = switch (colunas.get(i)) {
                        case "id" -> String.valueOf(c.getId());
                        case "hostname" -> c.getHostname() != null ? c.getHostname() : "-";
                        case "serial" -> c.getSerialComputador();
                        case "ip" -> c.getEnderecoIp() != null ? c.getEnderecoIp() : "-";
                        case "status" -> c.getStatus() != null ? c.getStatus() : "-";
                        case "setor" -> c.getSetor() != null ? c.getSetor().getNome() : "-";
                        default -> "";
                    };
                    cell.setCellValue(val);
                    cell.setCellStyle(bodyStyle);
                }
            }
        }

        // Ajustar largura das colunas
        for (int i = 0; i < colunas.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(os);
        workbook.close();
    }

    public void gerarImpressoras(OutputStream os, List<Impressora> impressoras, List<String> colunas) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Impressoras");

        if (colunas == null || colunas.isEmpty()) {
            colunas = List.of("id", "marcaModelo", "serial", "ip", "setor");
        }

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(titulo);
        titleCell.setCellStyle(createTitleStyle(workbook));

        Row headerRow = sheet.createRow(2);
        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < colunas.size(); i++) {
            Cell cell = headerRow.createCell(i);
            String label = switch (colunas.get(i)) {
                case "id" -> "ID";
                case "marcaModelo" -> "Marca/Modelo";
                case "serial" -> "Nº Série";
                case "ip" -> "IP";
                case "setor" -> "Setor";
                default -> colunas.get(i).toUpperCase();
            };
            cell.setCellValue(label);
            cell.setCellStyle(headerStyle);
        }

        CellStyle bodyStyle = createBodyStyle(workbook);
        int rIdx = 3;
        if (impressoras.isEmpty()) {
            Row row = sheet.createRow(rIdx++);
            for (int i = 0; i < colunas.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(bodyStyle);
            }
            row.getCell(0).setCellValue("Nenhum registro encontrado para os filtros selecionados.");
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, colunas.size() - 1));
        } else {
            for (Impressora imp : impressoras) {
                Row row = sheet.createRow(rIdx++);
                for (int i = 0; i < colunas.size(); i++) {
                    Cell cell = row.createCell(i);
                    String val = switch (colunas.get(i)) {
                        case "id" -> String.valueOf(imp.getId());
                        case "marcaModelo" -> imp.getMarcaModelo();
                        case "serial" -> imp.getSerialImpressora();
                        case "ip" -> imp.getEnderecoIp();
                        case "setor" -> imp.getSetor() != null ? imp.getSetor().getNome() : "-";
                        default -> "";
                    };
                    cell.setCellValue(val);
                    cell.setCellStyle(bodyStyle);
                }
            }
        }

        for (int i = 0; i < colunas.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(os);
        workbook.close();
    }

    public void gerarMonitores(OutputStream os, List<Monitor> monitores, List<String> colunas) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Monitores");

        if (colunas == null || colunas.isEmpty()) {
            colunas = List.of("id", "marca", "modelo", "serial", "computador");
        }

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(titulo);
        titleCell.setCellStyle(createTitleStyle(workbook));

        Row headerRow = sheet.createRow(2);
        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < colunas.size(); i++) {
            Cell cell = headerRow.createCell(i);
            String label = switch (colunas.get(i)) {
                case "id" -> "ID";
                case "marca" -> "Marca";
                case "modelo" -> "Modelo";
                case "serial" -> "Nº Série";
                case "computador" -> "Comp. Alocado";
                default -> colunas.get(i).toUpperCase();
            };
            cell.setCellValue(label);
            cell.setCellStyle(headerStyle);
        }

        CellStyle bodyStyle = createBodyStyle(workbook);
        int rIdx = 3;
        if (monitores.isEmpty()) {
            Row row = sheet.createRow(rIdx++);
            for (int i = 0; i < colunas.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(bodyStyle);
            }
            row.getCell(0).setCellValue("Nenhum registro encontrado para os filtros selecionados.");
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, colunas.size() - 1));
        } else {
            for (Monitor m : monitores) {
                Row row = sheet.createRow(rIdx++);
                for (int i = 0; i < colunas.size(); i++) {
                    Cell cell = row.createCell(i);
                    String val = switch (colunas.get(i)) {
                        case "id" -> String.valueOf(m.getId());
                        case "marca" -> m.getMarca();
                        case "modelo" -> m.getModelo();
                        case "serial" -> m.getSerialMonitor();
                        case "computador" -> m.getComputador() != null ? m.getComputador().getHostname() : "Não Alocado";
                        default -> "";
                    };
                    cell.setCellValue(val);
                    cell.setCellStyle(bodyStyle);
                }
            }
        }

        for (int i = 0; i < colunas.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(os);
        workbook.close();
    }

    public void gerarSetores(OutputStream os, List<Setor> setores, List<String> colunas) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Setores");

        if (colunas == null || colunas.isEmpty()) {
            colunas = List.of("id", "nome", "bloco");
        }

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(titulo);
        titleCell.setCellStyle(createTitleStyle(workbook));

        Row headerRow = sheet.createRow(2);
        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < colunas.size(); i++) {
            Cell cell = headerRow.createCell(i);
            String label = switch (colunas.get(i)) {
                case "id" -> "ID";
                case "nome" -> "Nome Setor";
                case "bloco" -> "Bloco / Localização";
                default -> colunas.get(i).toUpperCase();
            };
            cell.setCellValue(label);
            cell.setCellStyle(headerStyle);
        }

        CellStyle bodyStyle = createBodyStyle(workbook);
        int rIdx = 3;
        if (setores.isEmpty()) {
            Row row = sheet.createRow(rIdx++);
            for (int i = 0; i < colunas.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(bodyStyle);
            }
            row.getCell(0).setCellValue("Nenhum registro encontrado para os filtros selecionados.");
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, colunas.size() - 1));
        } else {
            for (Setor s : setores) {
                Row row = sheet.createRow(rIdx++);
                for (int i = 0; i < colunas.size(); i++) {
                    Cell cell = row.createCell(i);
                    String val = switch (colunas.get(i)) {
                        case "id" -> String.valueOf(s.getId());
                        case "nome" -> s.getNome();
                        case "bloco" -> s.getBloco();
                        default -> "";
                    };
                    cell.setCellValue(val);
                    cell.setCellStyle(bodyStyle);
                }
            }
        }

        for (int i = 0; i < colunas.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(os);
        workbook.close();
    }

    @SuppressWarnings("unchecked")
    public void gerarGeral(OutputStream os, Map<String, Object> stats) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Consolidado Geral");

        // Título
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(titulo);
        titleCell.setCellStyle(createTitleStyle(workbook));

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle bodyStyle = createBodyStyle(workbook);

        // Seção 1: Totais por Módulo
        Row section1Row = sheet.createRow(2);
        Cell s1c = section1Row.createCell(0);
        s1c.setCellValue("Totais por Módulo");
        Font sFont = workbook.createFont();
        sFont.setBold(true);
        sFont.setFontHeightInPoints((short) 12);
        CellStyle sStyle = workbook.createCellStyle();
        sStyle.setFont(sFont);
        s1c.setCellStyle(sStyle);

        Row hRow1 = sheet.createRow(3);
        Cell h11 = hRow1.createCell(0); h11.setCellValue("Módulo"); h11.setCellStyle(headerStyle);
        Cell h12 = hRow1.createCell(1); h12.setCellValue("Total Registrado"); h12.setCellStyle(headerStyle);

        String[][] modTotals = {
            {"Computadores", String.valueOf(stats.get("totalComputadores"))},
            {"Impressoras", String.valueOf(stats.get("totalImpressoras"))},
            {"Monitores", String.valueOf(stats.get("totalMonitores"))},
            {"Setores", String.valueOf(stats.get("totalSetores"))}
        };

        int rIdx = 4;
        for (String[] t : modTotals) {
            Row r = sheet.createRow(rIdx++);
            Cell c1 = r.createCell(0); c1.setCellValue(t[0]); c1.setCellStyle(bodyStyle);
            Cell c2 = r.createCell(1); c2.setCellValue(t[1]); c2.setCellStyle(bodyStyle);
        }

        // Seção 2: Computadores por Status
        rIdx++;
        Row section2Row = sheet.createRow(rIdx++);
        Cell s2c = section2Row.createCell(0);
        s2c.setCellValue("Computadores por Status");
        s2c.setCellStyle(sStyle);

        Row hRow2 = sheet.createRow(rIdx++);
        Cell h21 = hRow2.createCell(0); h21.setCellValue("Status"); h21.setCellStyle(headerStyle);
        Cell h22 = hRow2.createCell(1); h22.setCellValue("Quantidade"); h22.setCellStyle(headerStyle);

        Map<String, Long> statusMap = (Map<String, Long>) stats.get("computadoresPorStatus");
        for (Map.Entry<String, Long> entry : statusMap.entrySet()) {
            Row r = sheet.createRow(rIdx++);
            Cell c1 = r.createCell(0); c1.setCellValue(entry.getKey()); c1.setCellStyle(bodyStyle);
            Cell c2 = r.createCell(1); c2.setCellValue(entry.getValue()); c2.setCellStyle(bodyStyle);
        }

        // Seção 3: Distribuição de Monitores
        rIdx++;
        Row section3Row = sheet.createRow(rIdx++);
        Cell s3c = section3Row.createCell(0);
        s3c.setCellValue("Distribuição de Monitores");
        s3c.setCellStyle(sStyle);

        Row hRow3 = sheet.createRow(rIdx++);
        Cell h31 = hRow3.createCell(0); h31.setCellValue("Situação"); h31.setCellStyle(headerStyle);
        Cell h32 = hRow3.createCell(1); h32.setCellValue("Quantidade"); h32.setCellStyle(headerStyle);

        Object[][] monData = {
            {"Alocados em Computadores", String.valueOf(stats.get("monitoresAlocados"))},
            {"Livres em Estoque", String.valueOf(stats.get("monitoresLivres"))}
        };

        for (Object[] d : monData) {
            Row r = sheet.createRow(rIdx++);
            Cell c1 = r.createCell(0); c1.setCellValue((String) d[0]); c1.setCellStyle(bodyStyle);
            Cell c2 = r.createCell(1); c2.setCellValue((String) d[1]); c2.setCellStyle(bodyStyle);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

        workbook.write(os);
        workbook.close();
    }
}
