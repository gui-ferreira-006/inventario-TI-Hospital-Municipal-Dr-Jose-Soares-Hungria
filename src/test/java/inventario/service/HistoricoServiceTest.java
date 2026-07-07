package inventario.service;

import inventario.model.RegistroHistorico;
import inventario.repository.HistoricoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HistoricoServiceTest {

    @Mock
    private HistoricoRepository historicoRepository;

    @InjectMocks
    private HistoricoService historicoService;

    @Test
    public void testRegistrarCriacao() {
        historicoService.registrarEvento("COMPUTADOR", "PC-Recepcao", "CRIACAO", "Registro criado no sistema");

        ArgumentCaptor<RegistroHistorico> captor = ArgumentCaptor.forClass(RegistroHistorico.class);
        verify(historicoRepository, times(1)).save(captor.capture());

        RegistroHistorico registro = captor.getValue();
        assertNotNull(registro);
        assertEquals("COMPUTADOR", registro.getModulo());
        assertEquals("PC-Recepcao", registro.getIdentificador());
        assertEquals("CRIACAO", registro.getAcao());
        assertEquals("Registro criado no sistema", registro.getDetalhes());
    }

    @Test
    public void testRegistrarAtualizacaoSetor() {
        historicoService.registrarEvento("COMPUTADOR", "PC-Recepcao", "ATUALIZACAO", "Setor: TI ➔ Recepcao");

        ArgumentCaptor<RegistroHistorico> captor = ArgumentCaptor.forClass(RegistroHistorico.class);
        verify(historicoRepository, times(1)).save(captor.capture());

        RegistroHistorico registro = captor.getValue();
        assertNotNull(registro);
        assertEquals("COMPUTADOR", registro.getModulo());
        assertEquals("PC-Recepcao", registro.getIdentificador());
        assertEquals("ATUALIZACAO", registro.getAcao());
        assertEquals("Setor: TI ➔ Recepcao", registro.getDetalhes());
    }

    @Test
    public void testRegistrarAtualizacaoStatusIp() {
        historicoService.registrarEvento("REDE_IP", "192.168.1.10", "ATUALIZACAO", "Status: LIVRE ➔ OCUPADO");

        ArgumentCaptor<RegistroHistorico> captor = ArgumentCaptor.forClass(RegistroHistorico.class);
        verify(historicoRepository, times(1)).save(captor.capture());

        RegistroHistorico registro = captor.getValue();
        assertNotNull(registro);
        assertEquals("REDE_IP", registro.getModulo());
        assertEquals("192.168.1.10", registro.getIdentificador());
        assertEquals("ATUALIZACAO", registro.getAcao());
        assertEquals("Status: LIVRE ➔ OCUPADO", registro.getDetalhes());
    }

    @Test
    public void testRegistrarExclusao() {
        historicoService.registrarEvento("COMPUTADOR", "PC-Antigo", "EXCLUSAO", "Registro excluído do sistema");

        ArgumentCaptor<RegistroHistorico> captor = ArgumentCaptor.forClass(RegistroHistorico.class);
        verify(historicoRepository, times(1)).save(captor.capture());

        RegistroHistorico registro = captor.getValue();
        assertNotNull(registro);
        assertEquals("COMPUTADOR", registro.getModulo());
        assertEquals("PC-Antigo", registro.getIdentificador());
        assertEquals("EXCLUSAO", registro.getAcao());
        assertEquals("Registro excluído do sistema", registro.getDetalhes());
    }

    @Test
    public void testRegistrarEventosSimultaneos() {
        historicoService.registrarEvento("COMPUTADOR", "PC-Recepcao", "ATUALIZACAO", "IP: 192.168.1.10 ➔ 192.168.1.20 | Setor: TI ➔ Faturamento");

        ArgumentCaptor<RegistroHistorico> captor = ArgumentCaptor.forClass(RegistroHistorico.class);
        verify(historicoRepository, times(1)).save(captor.capture());

        RegistroHistorico registro = captor.getValue();
        assertNotNull(registro);
        assertEquals("COMPUTADOR", registro.getModulo());
        assertEquals("PC-Recepcao", registro.getIdentificador());
        assertEquals("ATUALIZACAO", registro.getAcao());
        assertEquals("IP: 192.168.1.10 ➔ 192.168.1.20 | Setor: TI ➔ Faturamento", registro.getDetalhes());
    }
}
