package inventario.service;

import inventario.model.RedeIp;
import inventario.model.StatusIp;
import inventario.repository.RedeIpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RedeIpService {

    @Autowired
    private RedeIpRepository redeIpRepository;

    public void liberarIp(RedeIp ip) {
        if (ip != null) {
            ip.setStatus(StatusIp.LIVRE);
            ip.setObservacao("");
            redeIpRepository.save(ip);
        }
    }

    public void ocuparIp(RedeIp ip, String observacao) {
        if (ip != null) {
            ip.setStatus(StatusIp.OCUPADO);
            ip.setObservacao(observacao);
            redeIpRepository.save(ip);
        }
    }

    public String gerarFaixaIp(String ipInicial, String ipFinal) {
        long ipInicialLong = ipToLong(ipInicial);
        long ipFinalLong = ipToLong(ipFinal);

        if (ipInicialLong > ipFinalLong) {
            throw new IllegalArgumentException("O IP inicial deve ser menor ou igual ao final.");
        }

        long quantidade = (ipFinalLong - ipInicialLong) + 1;
        if (quantidade > 1000) {
            throw new IllegalArgumentException("Limite máximo de 1000 IPs por geração excedido para evitar lentidão.");
        }

        int criados = 0;
        int ignorados = 0;

        for (long current = ipInicialLong; current <= ipFinalLong; current++) {
            String ipStr = longToIp(current);
            if (ipStr.endsWith(".0") || ipStr.endsWith(".255")) {
                continue;
            }

            if (redeIpRepository.existsByEnderecoIp(ipStr)) {
                ignorados++;
                continue;
            }

            RedeIp novoIp = new RedeIp();
            novoIp.setEnderecoIp(ipStr);
            novoIp.setStatus(StatusIp.LIVRE);
            redeIpRepository.save(novoIp);
            criados++;
        }

        return "Geração concluída: " + criados + " IPs criados, " + ignorados + " ignorados (já existentes ou reservados).";
    }

    @Transactional
    public String excluirFaixaIp(String ipInicial, String ipFinal) {
        long ipInicialLong = ipToLong(ipInicial);
        long ipFinalLong = ipToLong(ipFinal);

        if (ipInicialLong > ipFinalLong) {
            throw new IllegalArgumentException("O IP inicial deve ser menor ou igual ao final.");
        }

        int excluidos = 0;
        int ignorados = 0;

        for (long current = ipInicialLong; current <= ipFinalLong; current++) {
            String ipStr = longToIp(current);
            java.util.Optional<RedeIp> ipOpt = redeIpRepository.findByEnderecoIp(ipStr);
            if (ipOpt.isPresent()) {
                RedeIp redeIp = ipOpt.get();
                if (redeIp.getStatus() == StatusIp.LIVRE || redeIp.getStatus() == StatusIp.INATIVO) {
                    redeIpRepository.delete(redeIp);
                    excluidos++;
                } else if (redeIp.getStatus() == StatusIp.OCUPADO || redeIp.getStatus() == StatusIp.RESERVADO) {
                    ignorados++;
                }
            } else {
                ignorados++;
            }
        }

        return "Limpeza concluída: " + excluidos + " IPs excluídos com sucesso. " + ignorados + " IPs ignorados (em uso, reservados ou não encontrados na base).";
    }

    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long octet0 = Long.parseLong(octets[0]);
        long octet1 = Long.parseLong(octets[1]);
        long octet2 = Long.parseLong(octets[2]);
        long octet3 = Long.parseLong(octets[3]);
        return octet0 * 256 * 256 * 256 + octet1 * 256 * 256 + octet2 * 256 + octet3;
    }

    private String longToIp(long ip) {
        long octet0 = (ip >> 24) & 0xFF;
        long octet1 = (ip >> 16) & 0xFF;
        long octet2 = (ip >> 8) & 0xFF;
        long octet3 = ip & 0xFF;
        return octet0 + "." + octet1 + "." + octet2 + "." + octet3;
    }
}
