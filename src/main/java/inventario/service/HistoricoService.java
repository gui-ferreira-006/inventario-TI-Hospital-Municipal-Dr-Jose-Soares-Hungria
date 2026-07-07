package inventario.service;

import inventario.model.RegistroHistorico;
import inventario.repository.HistoricoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoricoService {

    @Autowired
    private HistoricoRepository historicoRepository;

    @Transactional
    public void registrarEvento(String modulo, String identificador, String acao, String detalhes) {
        RegistroHistorico registro = new RegistroHistorico(modulo, identificador, acao, detalhes);
        historicoRepository.save(registro);
    }
}
