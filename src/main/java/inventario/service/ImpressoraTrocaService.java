package inventario.service;

import inventario.model.Impressora;
import inventario.model.RedeIp;
import inventario.model.Setor;
import inventario.repository.ImpressoraRepository;
import inventario.repository.SetorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import inventario.service.HistoricoService;

@Service
public class ImpressoraTrocaService {

    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Autowired
    private SetorRepository setorRepository;

    @Autowired
    private HistoricoService historicoService;

    @Transactional
    public void processarTroca(Long idOrigem, Long idDestino, String tipoTroca) {
        if (idOrigem.equals(idDestino)) {
            throw new IllegalArgumentException("Não é possível trocar uma impressora por ela mesma.");
        }

        Impressora origem = impressoraRepository.findById(idOrigem)
                .orElseThrow(() -> new IllegalArgumentException("Impressora de origem não encontrada: ID " + idOrigem));
        Impressora destino = impressoraRepository.findById(idDestino)
                .orElseThrow(() -> new IllegalArgumentException("Impressora de destino não encontrada: ID " + idDestino));

        Setor setorOrigem = origem.getSetor();

        if ("DEFEITO".equalsIgnoreCase(tipoTroca)) {
            aplicarRegraTrocaPorDefeito(origem, destino, setorOrigem);
        } else if ("REMANEJAMENTO".equalsIgnoreCase(tipoTroca)) {
            aplicarRegraRemanejamento(origem, destino, setorOrigem);
        } else {
            throw new IllegalArgumentException("Tipo de troca desconhecido: " + tipoTroca);
        }
    }

    private void aplicarRegraTrocaPorDefeito(Impressora origem, Impressora destino, Setor setorOrigem) {
        Setor setorTI = setorRepository.findByNome("TI").orElse(null);

        RedeIp ipTemporarioOrigem = origem.getRedeIp();
        String ipOrigem = ipTemporarioOrigem != null ? ipTemporarioOrigem.getEnderecoIp() : null;

        origem.setRedeIp(null);
        origem.setSetor(setorTI);
        origem.setStatus("Em Manutenção");
        impressoraRepository.saveAndFlush(origem);

        destino.setRedeIp(ipTemporarioOrigem);
        destino.setSetor(setorOrigem);
        destino.setStatus("Ativo no Setor");
        impressoraRepository.save(destino);

        String identificador = "Troca Defeito: " + origem.getSerialImpressora() + " ⇄ " + destino.getSerialImpressora();

        String detalhes = "Motivo: Substituição de equipamento avariado | [Origem - " + origem.getSerialImpressora() + "] Setor: " + (setorOrigem != null ? setorOrigem.getNome() : "Reserva") + " ➔ TI, Status: Ativo ➔ Em Manutenção, IP liberado | [Destino - " + destino.getSerialImpressora() + "] Setor: Reserva ➔ " + (setorOrigem != null ? setorOrigem.getNome() : "Reserva") + ", Status: " + destino.getStatus() + " ➔ Ativo, IP assumido: " + (ipOrigem != null ? ipOrigem : "Nenhum");

        historicoService.registrarEvento("IMPRESSORA", identificador, "ATUALIZACAO", detalhes);
    }

    private void aplicarRegraRemanejamento(Impressora origem, Impressora destino, Setor setorOrigem) {
        RedeIp ipTempOrigem = origem.getRedeIp();
        RedeIp ipTempDestino = destino.getRedeIp();
        Setor setorTemporarioDestino = destino.getSetor();

        String ipOrigem = ipTempOrigem != null ? ipTempOrigem.getEnderecoIp() : null;
        String ipTemporarioDestino = ipTempDestino != null ? ipTempDestino.getEnderecoIp() : null;

        origem.setRedeIp(null);
        impressoraRepository.saveAndFlush(origem);

        destino.setRedeIp(ipTempOrigem);
        destino.setSetor(setorOrigem);
        impressoraRepository.saveAndFlush(destino);

        origem.setRedeIp(ipTempDestino);
        origem.setSetor(setorTemporarioDestino);
        impressoraRepository.save(origem);

        String identificador = "Remanejamento: " + origem.getSerialImpressora() + " ⇄ " + destino.getSerialImpressora();

        String detalhes = "Motivo: Remanejamento interno entre setores | [Saiu - " + origem.getSerialImpressora() + "] Setor: " + (setorOrigem != null ? setorOrigem.getNome() : "Reserva") + " ➔ " + (setorTemporarioDestino != null ? setorTemporarioDestino.getNome() : "Reserva") + ", IP: " + (ipOrigem != null ? ipOrigem : "N/A") + " ➔ " + (ipTemporarioDestino != null ? ipTemporarioDestino : "N/A") + " | [Entrou - " + destino.getSerialImpressora() + "] Setor: " + (setorTemporarioDestino != null ? setorTemporarioDestino.getNome() : "Reserva") + " ➔ " + (setorOrigem != null ? setorOrigem.getNome() : "Reserva") + ", IP: " + (ipTemporarioDestino != null ? ipTemporarioDestino : "N/A") + " ➔ " + (ipOrigem != null ? ipOrigem : "N/A");

        historicoService.registrarEvento("IMPRESSORA", identificador, "ATUALIZACAO", detalhes);
    }
}
