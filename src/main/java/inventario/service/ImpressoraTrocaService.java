package inventario.service;

import inventario.model.Impressora;
import inventario.model.RedeIp;
import inventario.model.Setor;
import inventario.repository.ImpressoraRepository;
import inventario.repository.SetorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImpressoraTrocaService {

    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Autowired
    private SetorRepository setorRepository;

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
        origem.setRedeIp(null);
        origem.setSetor(setorTI);
        origem.setStatus("Em Manutenção");
        impressoraRepository.saveAndFlush(origem);

        destino.setRedeIp(ipTemporarioOrigem);
        destino.setSetor(setorOrigem);
        destino.setStatus("Ativo no Setor");
        impressoraRepository.save(destino);
    }

    private void aplicarRegraRemanejamento(Impressora origem, Impressora destino, Setor setorOrigem) {
        RedeIp ipTemporarioOrigem = origem.getRedeIp();
        RedeIp ipTemporarioDestino = destino.getRedeIp();
        Setor setorDestino = destino.getSetor();

        origem.setRedeIp(null);
        impressoraRepository.saveAndFlush(origem);

        destino.setRedeIp(ipTemporarioOrigem);
        destino.setSetor(setorOrigem);
        impressoraRepository.saveAndFlush(destino);

        origem.setRedeIp(ipTemporarioDestino);
        origem.setSetor(setorDestino);
        impressoraRepository.save(origem);
    }
}
