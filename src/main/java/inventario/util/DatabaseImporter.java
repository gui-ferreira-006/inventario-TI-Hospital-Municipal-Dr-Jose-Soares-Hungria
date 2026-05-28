package inventario.util;

import inventario.model.Computador;
import inventario.model.Setor;
import inventario.repository.ComputadorRepository;
import inventario.repository.SetorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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

    @Override
    public void run(String... args) throws Exception {
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
                if (!ip.isEmpty() && computadorRepository.existsByEnderecoIp(ip)) {
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
                    computador.setEnderecoIp(ip.isEmpty() ? null : ip);
                    computador.setStatus("Ativo no Setor");
                    computador.setSetor(setor);

                    computadorRepository.save(computador);
                    System.out.println("[OK] Importado: Serial " + serial + " -> " + hostname + " (" + setorNome + ")");
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
        System.out.println("   FIM DA IMPORTAÇÃO");
        System.out.println("==================================================");
        System.out.println("Total lidos:                  " + totalLidos);
        System.out.println("Total importados com sucesso: " + totalImportados);
        System.out.println("Total pulados (Serial Duplic.):" + totalDuplicadosSerial);
        System.out.println("Total pulados (Outros Duplic.):" + totalDuplicadosOutros);
        System.out.println("Total erros/falhas:           " + totalErros);
        System.out.println("==================================================\n");
    }
}
