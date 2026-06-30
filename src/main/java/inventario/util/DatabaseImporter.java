package inventario.util;

import inventario.model.Computador;
import inventario.model.Impressora;
import inventario.model.Monitor;
import inventario.model.Setor;
import inventario.repository.ComputadorRepository;
import inventario.repository.ImpressoraRepository;
import inventario.repository.MonitorRepository;
import inventario.repository.SetorRepository;
import inventario.repository.RedeIpRepository;
import inventario.model.RedeIp;
import inventario.model.StatusIp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.springframework.jdbc.core.JdbcTemplate;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "importar", havingValue = "true")
public class DatabaseImporter implements CommandLineRunner {

    @Autowired
    private SetorRepository setorRepository;

    @Autowired
    private ComputadorRepository computadorRepository;

    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private RedeIpRepository redeIpRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Garantir que a coluna endereco_ip aceita valores nulos no MySQL
        try {
            jdbcTemplate.execute("ALTER TABLE tb_impressoras MODIFY COLUMN endereco_ip VARCHAR(255) NULL");
            System.out.println("[DB-PATCH] Alterada coluna endereco_ip da tabela tb_impressoras para permitir valores NULL.");
        } catch (Exception e) {
            System.err.println("[DB-PATCH] Erro ao alterar coluna endereco_ip: " + e.getMessage());
        }

        System.out.println("\n==================================================");
        System.out.println("   INICIANDO IMPORTAÇÃO DE COMPUTADORES");
        System.out.println("==================================================");

        // Corrigir computadores que ainda estão com o status antigo "ATIVO"
        int totalCorrigidos = 0;
        try {
            List<Computador> todos = computadorRepository.findAll();
            for (Computador c : todos) {
                if ("ATIVO".equalsIgnoreCase(c.getStatus())) {
                    c.setStatus("Ativo no Setor");
                    computadorRepository.save(c);
                    totalCorrigidos++;
                }
            }
            if (totalCorrigidos > 0) {
                System.out.println("[MIGRAÇÃO] Sucesso: Corrigidos " + totalCorrigidos + " computadores com status 'ATIVO' para 'Ativo no Setor'.");
            }
        } catch (Exception e) {
            System.err.println("[MIGRAÇÃO] Erro ao corrigir computadores antigos: " + e.getMessage());
        }

        // O arquivo está em Imports/Listagem de CPUs.txt
        File file = new File("Imports/Listagem de CPUs.txt");
        if (!file.exists()) {
            System.err.println("ERRO: Arquivo não encontrado em: " + file.getAbsolutePath());
            System.out.println("==================================================\n");
            return;
        }

        System.out.println("Lendo arquivo: " + file.getAbsolutePath());

        int totalLidos = 0;
        int totalImportados = 0;
        int totalDuplicadosSerial = 0;
        int totalDuplicadosOutros = 0;
        int totalErros = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                
                // Pular linhas vazias ou de cabeçalho
                if (line.isEmpty() || line.startsWith("Listagem") || line.startsWith("ID\t") || line.startsWith("ID ")) {
                    continue;
                }

                totalLidos++;
                
                // Dividir a linha por tabulações
                String[] parts = line.split("\t");
                if (parts.length < 5) {
                    // Tenta dividir por múltiplos espaços caso o delimitador seja diferente
                    parts = line.split("\\s{2,}");
                }

                if (parts.length < 5) {
                    System.err.println("[ERRO] Linha inválida (menos de 5 colunas): " + line);
                    totalErros++;
                    continue;
                }

                String idStr = parts[0].trim();
                String setorNome = parts[1].trim();
                String hostname = parts[2].trim();
                String ip = parts[3].trim();
                String serial = parts[4].trim();

                // Normalização/limpeza de dados específicos para computadores de reserva e sem IP
                if (hostname.equalsIgnoreCase("SMSHMJSH") || hostname.equalsIgnoreCase("DESAPARECIDO")) {
                    hostname = ""; // Fica nulo no banco para evitar duplicidade de placeholder
                }

                if (ip.equalsIgnoreCase("Sem IP") || !ip.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
                    ip = ""; // Fica nulo no banco para evitar formato de IP inválido ou duplicidade de prefixo
                }

                // Validações básicas antes de salvar
                if (serial.length() < 10) {
                    System.err.println("[ERRO] Serial inválido (menos de 10 caracteres): " + serial + " na linha: " + line);
                    totalErros++;
                    continue;
                }

                // Verificar duplicidade de Serial
                if (computadorRepository.existsBySerialComputador(serial)) {
                    System.out.println("[INFO-SKIP] Serial já cadastrado: " + serial + " (Estação: " + hostname + ")");
                    totalDuplicadosSerial++;
                    continue;
                }

                // Verificar duplicidade de Hostname (se não vazio)
                if (!hostname.isEmpty() && computadorRepository.existsByHostname(hostname)) {
                    System.out.println("[INFO-SKIP] Hostname já cadastrado: " + hostname + " (Serial: " + serial + ")");
                    totalDuplicadosOutros++;
                    continue;
                }

                // Verificar duplicidade de IP (se não vazio)
                if (!ip.isEmpty() && redeIpRepository.existsByEnderecoIp(ip)) {
                    System.out.println("[INFO-SKIP] IP já cadastrado: " + ip + " (Serial: " + serial + ")");
                    totalDuplicadosOutros++;
                    continue;
                }

                try {
                    // Buscar ou criar o setor correspondente
                    Setor setor = setorRepository.findByNome(setorNome).orElseGet(() -> {
                        Setor novoSetor = new Setor(setorNome, "Importado");
                        System.out.println("[SETOR] Criando novo setor: " + setorNome);
                        return setorRepository.save(novoSetor);
                    });

                    // Instanciar o computador
                    Computador computador = new Computador();
                    computador.setSerialComputador(serial);
                    computador.setHostname(hostname.isEmpty() ? null : hostname);
                    final String finalIp = ip;
                    if (!finalIp.isEmpty()) {
                        RedeIp redeIp = redeIpRepository.findByEnderecoIp(finalIp).orElseGet(() -> {
                            RedeIp novo = new RedeIp();
                            novo.setEnderecoIp(finalIp);
                            novo.setStatus(StatusIp.OCUPADO);
                            novo.setObservacao("Importado via CSV/Excel");
                            return redeIpRepository.save(novo);
                        });
                        redeIp.setStatus(StatusIp.OCUPADO);
                        redeIpRepository.save(redeIp);
                        computador.setRedeIp(redeIp);
                    }
                    
                    // Definir o status com base no setor (reserva fica em estoque)
                    if (setorNome.equalsIgnoreCase("Informática - Reserva")) {
                        computador.setStatus("Em Estoque/Bancada");
                    } else {
                        computador.setStatus("Ativo no Setor");
                    }
                    
                    computador.setSetor(setor);

                    computadorRepository.save(computador);
                    System.out.println("[OK] Importado: Serial " + serial + " -> " + (hostname.isEmpty() ? "Sem Hostname" : hostname) + " (" + setorNome + ")");
                    totalImportados++;

                } catch (Exception e) {
                    System.err.println("[ERRO] Falha ao cadastrar computador: " + serial + ". Erro: " + e.getMessage());
                    totalErros++;
                }
            }
        } catch (IOException e) {
            System.err.println("ERRO de I/O ao ler o arquivo: " + e.getMessage());
            totalErros++;
        }

        System.out.println("==================================================");
        System.out.println("   FIM DA IMPORTAÇÃO DE COMPUTADORES");
        System.out.println("==================================================");
        System.out.println("Total lidos:                  " + totalLidos);
        System.out.println("Total importados com sucesso: " + totalImportados);
        System.out.println("Total pulados (Serial Duplic.):" + totalDuplicadosSerial);
        System.out.println("Total pulados (Outros Duplic.):" + totalDuplicadosOutros);
        System.out.println("Total erros/falhas:           " + totalErros);
        System.out.println("==================================================\n");

        // ================================================================
        // IMPORTAÇÃO DE IMPRESSORAS
        // ================================================================
        importarImpressoras();

        // ================================================================
        // IMPORTAÇÃO DE MONITORES
        // ================================================================
        importarMonitores();
    }

    private void importarImpressoras() {
        System.out.println("\n==================================================");
        System.out.println("   INICIANDO IMPORTAÇÃO DE IMPRESSORAS");
        System.out.println("==================================================");

        File fileImp = new File("Imports/Listagem de Impressoras.txt");
        if (!fileImp.exists()) {
            System.err.println("ERRO: Arquivo não encontrado em: " + fileImp.getAbsolutePath());
            System.out.println("==================================================\n");
            return;
        }

        System.out.println("Lendo arquivo: " + fileImp.getAbsolutePath());

        int totalLidos = 0;
        int totalImportados = 0;
        int totalDuplicadosSerial = 0;
        int totalDuplicadosIp = 0;
        int totalSemDados = 0;
        int totalErros = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(fileImp, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Pular linhas vazias ou de cabeçalho
                if (line.isEmpty()
                        || line.startsWith("Listagem")
                        || line.startsWith("Nº\t")
                        || line.startsWith("N\u00ba ")) {
                    continue;
                }

                String[] parts = line.split("\t");

                // Nº | Setor | Impressora | IP | Serial
                // Precisamos de pelo menos 3 colunas (Nº, Setor, Impressora)
                if (parts.length < 3) {
                    System.err.println("[ERRO] Linha inválida (menos de 3 colunas): " + line);
                    totalErros++;
                    continue;
                }

                String setorNome = parts.length > 1 ? parts[1].trim() : "";
                String marcaModelo = parts.length > 2 ? parts[2].trim() : "";
                String ip        = parts.length > 3 ? parts[3].trim() : "";
                String serial    = parts.length > 4 ? parts[4].trim() : "";

                // --- Pular linhas sem setor E sem modelo (números 1 e 2 do arquivo) ---
                if (setorNome.isEmpty() && marcaModelo.isEmpty()) {
                    System.out.println("[SKIP] Linha sem setor e modelo (vaga reservada): " + line);
                    totalSemDados++;
                    continue;
                }

                // --- Pular linhas sem modelo de impressora ---
                if (marcaModelo.isEmpty()) {
                    System.out.println("[SKIP] Linha sem modelo de impressora: " + line);
                    totalSemDados++;
                    continue;
                }

                // --- Filtro de marca: aceitar SOMENTE Samsung e HP ---
                // Impressoras ZEBRA seguem padrão próprio (etiquetas) e não pertencem a este módulo.
                // Outros modelos avulsos (GS, CPL, etc.) também são ignorados.
                String marcaUpper = marcaModelo.toUpperCase();
                if (!marcaUpper.startsWith("SAMSUNG") && !marcaUpper.startsWith("HP")) {
                    System.out.println("[SKIP] Marca não suportada (apenas Samsung e HP): " + marcaModelo + " (" + setorNome + ")");
                    totalSemDados++;
                    continue;
                }

                totalLidos++;

                // --- Normalização do IP ---
                // IPs com texto ("Via cabo") ou prefixos incompletos ("192.168.", "192.168") ficam nulos
                // no banco para evitar formato de IP inválido ou duplicidade de prefixo.
                if (ip.equalsIgnoreCase("Sem IP") || !ip.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
                    ip = ""; // Fica nulo no banco para evitar formato de IP inválido ou duplicidade de prefixo
                }

                // --- Normalização do Serial ---
                // Serial vazio ou muito curto: gravar como null
                if (serial.length() < 4) {
                    serial = "";
                }

                // --- Verificar duplicidade de Serial (somente se serial não é vazio) ---
                if (!serial.isEmpty() && impressoraRepository.existsBySerialImpressora(serial)) {
                    System.out.println("[INFO-SKIP] Serial já cadastrado: " + serial + " (" + marcaModelo + " / " + setorNome + ")");
                    totalDuplicadosSerial++;
                    continue;
                }

                // --- Verificar duplicidade de IP (somente se IP não é vazio) ---
                if (!ip.isEmpty() && redeIpRepository.existsByEnderecoIp(ip)) {
                    System.out.println("[INFO-SKIP] IP já cadastrado: " + ip + " (" + marcaModelo + " / " + setorNome + ")");
                    totalDuplicadosIp++;
                    continue;
                }

                try {
                    // Buscar ou criar o setor correspondente
                    Setor setor = setorRepository.findByNome(setorNome).orElseGet(() -> {
                        Setor novoSetor = new Setor(setorNome, "Importado");
                        System.out.println("[SETOR] Criando novo setor: " + setorNome);
                        return setorRepository.save(novoSetor);
                    });

                    // Instanciar a impressora
                    Impressora impressora = new Impressora();
                    impressora.setMarcaModelo(marcaModelo);
                    impressora.setSerialImpressora(serial.isEmpty() ? null : serial);
                    final String finalIp = ip;
                    if (!finalIp.isEmpty()) {
                        RedeIp redeIp = redeIpRepository.findByEnderecoIp(finalIp).orElseGet(() -> {
                            RedeIp novo = new RedeIp();
                            novo.setEnderecoIp(finalIp);
                            novo.setStatus(StatusIp.OCUPADO);
                            novo.setObservacao("Importado via CSV/Excel");
                            return redeIpRepository.save(novo);
                        });
                        redeIp.setStatus(StatusIp.OCUPADO);
                        redeIpRepository.save(redeIp);
                        impressora.setRedeIp(redeIp);
                    }

                    // Status: reserva fica em estoque, demais ficam ativas no setor
                    if (setorNome.equalsIgnoreCase("Informática - Reserva")) {
                        impressora.setStatus("Em Estoque/Bancada");
                    } else {
                        impressora.setStatus("Ativo no Setor");
                    }

                    impressora.setSetor(setor);

                    impressoraRepository.save(impressora);
                    System.out.println("[OK] Importada: " + marcaModelo
                            + " | IP: " + (ip.isEmpty() ? "Sem IP" : ip)
                            + " | Serial: " + (serial.isEmpty() ? "Sem serial" : serial)
                            + " (" + setorNome + ")");
                    totalImportados++;

                } catch (Exception e) {
                    System.err.println("[ERRO] Falha ao cadastrar impressora: " + marcaModelo
                            + " / " + setorNome + ". Erro: " + e.getMessage());
                    totalErros++;
                }
            }
        } catch (IOException e) {
            System.err.println("ERRO de I/O ao ler o arquivo de impressoras: " + e.getMessage());
            totalErros++;
        }

        System.out.println("==================================================");
        System.out.println("   FIM DA IMPORTAÇÃO DE IMPRESSORAS");
        System.out.println("==================================================");
        System.out.println("Total lidos:                  " + totalLidos);
        System.out.println("Total importados com sucesso: " + totalImportados);
        System.out.println("Total pulados (Serial Duplic.):" + totalDuplicadosSerial);
        System.out.println("Total pulados (IP Duplic.):   " + totalDuplicadosIp);
        System.out.println("Total pulados (sem dados):    " + totalSemDados);
        System.out.println("Total erros/falhas:           " + totalErros);
        System.out.println("==================================================\n");
    }

    private void importarMonitores() {
        System.out.println("\n==================================================");
        System.out.println("   INICIANDO IMPORTAÇÃO DE MONITORES");
        System.out.println("==================================================");

        File fileMon = new File("Imports/Listagem de Monitores.txt");
        if (!fileMon.exists()) {
            System.err.println("ERRO: Arquivo não encontrado em: " + fileMon.getAbsolutePath());
            System.out.println("==================================================\n");
            return;
        }

        System.out.println("Lendo arquivo: " + fileMon.getAbsolutePath());

        int totalLidos = 0;
        int totalImportados = 0;
        int totalDuplicadosSerial = 0;
        int totalErros = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(fileMon, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Pular linhas vazias ou de cabeçalho
                if (line.isEmpty()
                        || line.startsWith("Listagem")
                        || line.startsWith("Nº\t")
                        || line.startsWith("N\u00ba ")) {
                    continue;
                }

                String[] parts = line.split("\t");
                if (parts.length < 5) {
                    parts = line.split("\\s{2,}");
                }

                // Nº | Setor | Marca | Modelo | Serial | Estação
                if (parts.length < 5) {
                    System.err.println("[ERRO] Linha inválida (menos de 5 colunas): " + line);
                    totalErros++;
                    continue;
                }

                String setorNome = parts[1].trim();
                String marca     = parts[2].trim();
                String modelo    = parts[3].trim();
                String serial    = parts[4].trim();
                String estacao   = parts.length > 5 ? parts[5].trim() : "";

                totalLidos++;

                // Validar dados obrigatórios
                if (marca.isEmpty() || modelo.isEmpty() || serial.isEmpty()) {
                    System.err.println("[ERRO] Dados obrigatórios faltando na linha: " + line);
                    totalErros++;
                    continue;
                }

                // Verificar se o serial já existe no banco de dados
                if (monitorRepository.existsBySerialMonitor(serial)) {
                    System.out.println("[INFO-SKIP] Serial de Monitor já cadastrado: " + serial + " (" + marca + " / " + modelo + ")");
                    totalDuplicadosSerial++;
                    continue;
                }

                try {
                    // Instanciar o monitor
                    Monitor monitor = new Monitor();
                    monitor.setMarca(marca);
                    monitor.setModelo(modelo);
                    monitor.setSerialMonitor(serial);

                    // Buscar o Computador (Estação) correspondente
                    if (!estacao.isEmpty()) {
                        Optional<Computador> compOpt = computadorRepository.findByHostname(estacao);
                        if (compOpt.isPresent()) {
                            monitor.setComputador(compOpt.get());
                        } else {
                            System.out.println("[WARN] Computador (Estação) '" + estacao + "' não encontrado para o monitor serial " + serial);
                        }
                    }

                    monitorRepository.save(monitor);
                    System.out.println("[OK] Importado: " + marca + " " + modelo
                            + " | Serial: " + serial
                            + " | Estação: " + (estacao.isEmpty() ? "Reserva" : estacao));
                    totalImportados++;

                } catch (Exception e) {
                    System.err.println("[ERRO] Falha ao cadastrar monitor: " + serial + ". Erro: " + e.getMessage());
                    totalErros++;
                }
            }
        } catch (IOException e) {
            System.err.println("ERRO de I/O ao ler o arquivo de monitores: " + e.getMessage());
            totalErros++;
        }

        System.out.println("==================================================");
        System.out.println("   FIM DA IMPORTAÇÃO DE MONITORES");
        System.out.println("==================================================");
        System.out.println("Total lidos:                  " + totalLidos);
        System.out.println("Total importados com sucesso: " + totalImportados);
        System.out.println("Total pulados (Serial Duplic.):" + totalDuplicadosSerial);
        System.out.println("Total erros/falhas:           " + totalErros);
        System.out.println("==================================================\n");
    }
}
