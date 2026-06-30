package inventario.service;

import inventario.model.Computador;
import inventario.model.Impressora;
import inventario.model.RedeIp;
import inventario.repository.ComputadorRepository;
import inventario.repository.ImpressoraRepository;
import inventario.repository.RedeIpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PadronizacaoObservacaoService {

    @Autowired
    private ComputadorRepository computadorRepository;

    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Autowired
    private RedeIpRepository redeIpRepository;

    public String padronizarTudo() {
        int pcsAtualizados = 0;
        int impsAtualizadas = 0;

        // Loop de Computadores
        for (Computador comp : computadorRepository.findAll()) {
            if (comp.getRedeIp() != null) {
                String nomeSetor = (comp.getSetor() != null) ? comp.getSetor().getNome() : "TI Reserva";
                String identificador = (comp.getHostname() != null && !comp.getHostname().trim().isEmpty())
                        ? comp.getHostname()
                        : "S/N: " + comp.getSerialComputador();

                comp.getRedeIp().setObservacao("Computador " + identificador + " - " + nomeSetor);
                redeIpRepository.save(comp.getRedeIp());
                pcsAtualizados++;
            }
        }

        // Loop de Impressoras
        for (Impressora imp : impressoraRepository.findAll()) {
            if (imp.getRedeIp() != null) {
                String nomeSetor = (imp.getSetor() != null) ? imp.getSetor().getNome() : "TI Reserva";

                imp.getRedeIp().setObservacao(imp.getMarcaModelo() + " - " + nomeSetor);
                redeIpRepository.save(imp.getRedeIp());
                impsAtualizadas++;
            }
        }

        return "Padronização concluída! Computadores atualizados: " + pcsAtualizados + " | Impressoras atualizadas: " + impsAtualizadas;
    }
}
