package inventario.service;

import inventario.model.Impressora;
import inventario.model.Setor;
import inventario.repository.ImpressoraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImpressoraTrocaService {

    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Transactional
    public void realizarTroca(Long idAvariada, Long idSubstituta) {
        Impressora avariada = impressoraRepository.findById(idAvariada)
                .orElseThrow(() -> new IllegalArgumentException("Impressora avariada não encontrada: ID " + idAvariada));
        Impressora substituta = impressoraRepository.findById(idSubstituta)
                .orElseThrow(() -> new IllegalArgumentException("Impressora substituta não encontrada: ID " + idSubstituta));

        // 2) Guarda o IP e o Setor da impressora avariada em variáveis temporárias.
        String ipTemporario = avariada.getEnderecoIp();
        Setor setorTemporario = avariada.getSetor();

        // 3) Define o IP e o Setor da impressora avariada como null (movendo-a para a Reserva TI).
        avariada.setEnderecoIp(null);
        avariada.setSetor(null);
        avariada.setStatus("Em Manutenção"); // Atualiza status da avariada

        // 4) Aplica o IP temporário e o Setor temporário na impressora substituta.
        substituta.setEnderecoIp(ipTemporario);
        substituta.setSetor(setorTemporario);
        substituta.setStatus("Ativo no Setor"); // Atualiza status da substituta

        // 5) Guarda ambas no repositório.
        // Usamos saveAndFlush para garantir que o IP seja liberado no banco antes de salvar a substituta,
        // evitando conflito de unique constraint.
        impressoraRepository.saveAndFlush(avariada);
        impressoraRepository.save(substituta);
    }
}
