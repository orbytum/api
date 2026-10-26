package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CreateLembreteRequest;
import com.orbytum.api.models.dto.request.EditLembreteRequest;
import com.orbytum.api.models.dto.response.LembretePaginadoResponse;
import com.orbytum.api.models.dto.response.LembreteResponse;
import com.orbytum.api.models.dto.response.ParticipanteLembreteResponse;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Evento;
import com.orbytum.api.models.entity.EventoParticipante;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.StatusLembrete;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.enums.TipoRecorrencia;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.EventoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.EventoParticipanteRepository;
import com.orbytum.api.repository.EventoRepository;
import com.orbytum.api.util.IcsCalendarBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LembreteService {

    private static final Logger logger = LoggerFactory.getLogger(LembreteService.class);

    /**
     * Ordem cronológica pela próxima ocorrência: primeiro o que ainda vai acontecer (do mais
     * próximo ao mais distante), depois o que já passou (do mais recente ao mais antigo).
     */
    private static final Comparator<LembreteResponse> ORDEM_CRONOLOGICA = (a, b) -> {
        boolean aEncerrado = a.status() == StatusLembrete.ENCERRADO;
        boolean bEncerrado = b.status() == StatusLembrete.ENCERRADO;
        if (aEncerrado != bEncerrado) {
            return aEncerrado ? 1 : -1;
        }
        int porData = a.proximaOcorrencia().compareTo(b.proximaOcorrencia());
        if (porData != 0) {
            return aEncerrado ? -porData : porData;
        }
        return a.id().compareTo(b.id());
    };

    private final EventoRepository eventoRepository;
    private final EventoParticipanteRepository participanteRepository;
    private final GrupoRepository grupoRepository;
    private final UsuarioService usuarioService;
    private final GrupoAcessoService grupoAcessoService;
    private final S3StorageService s3StorageService;

    @Transactional
    public LembreteResponse criar(CreateLembreteRequest request, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);

        if (request.grupoId() == null) {
            throw new IllegalArgumentException("O ID do grupo vinculado é obrigatório.");
        }

        Grupo grupo = grupoRepository.findById(request.grupoId())
                .filter(Grupo::isAtivo)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo não encontrado com ID: " + request.grupoId()));

        validarAcessoAoGrupo(grupo.getId(), solicitante);

        String organizador = (request.organizador() != null && !request.organizador().isBlank())
                ? request.organizador().trim()
                : solicitante.getNome();

        TipoRecorrencia recorrencia = request.recorrencia() != null
                ? request.recorrencia()
                : TipoRecorrencia.NENHUMA;

        Evento evento = Evento.builder()
                .grupo(grupo)
                .criador(solicitante)
                .titulo(request.titulo().trim())
                .descricao(request.descricao() != null ? request.descricao().trim() : "")
                .tipo(request.tipo())
                .dthInicio(request.dataHora())
                .localizacao(request.getLocalFinal())
                .url(request.link() != null ? request.link().trim() : null)
                .isOnline(request.link() != null && !request.link().isBlank())
                .recorrencia(recorrencia)
                .organizador(organizador)
                .dthRegistro(LocalDateTime.now())
                .isAtivo(true)
                .participantes(new ArrayList<>())
                .build();

        adicionarParticipantes(evento, request.participanteIds(), request.participantes());

        Evento salvo = eventoRepository.save(evento);
        logger.info("Lembrete/Evento criado com sucesso: id={}, tipo={}, grupoId={}", salvo.getId(), salvo.getTipo(), grupo.getId());

        return toResponse(salvo, solicitante);
    }

    @Transactional(readOnly = true)
    public LembreteResponse buscar(Long id, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Evento evento = buscarEntidade(id);
        validarAcessoAoGrupo(evento.getGrupo().getId(), solicitante);
        return toResponse(evento, solicitante);
    }

    @Transactional
    public LembreteResponse atualizar(Long id, EditLembreteRequest request, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Evento evento = buscarEntidade(id);

        validarCriadorOuAdmin(evento, solicitante, "Somente o criador do lembrete pode realizar alterações.");

        if (request.tipo() != TipoLembrete.REUNIAO && evento.isTemAta()) {
            throw new IllegalArgumentException("Não é possível alterar o tipo do lembrete pois já existe uma ata anexada a esta reunião.");
        }

        evento.setTitulo(request.titulo().trim());
        evento.setDescricao(request.descricao() != null ? request.descricao().trim() : "");
        evento.setTipo(request.tipo());
        evento.setDthInicio(request.dataHora());
        evento.setLocalizacao(request.getLocalFinal());
        evento.setUrl(request.link() != null ? request.link().trim() : null);
        evento.setOnline(request.link() != null && !request.link().isBlank());
        evento.setRecorrencia(request.recorrencia() != null ? request.recorrencia() : TipoRecorrencia.NENHUMA);

        if (request.organizador() != null && !request.organizador().isBlank()) {
            evento.setOrganizador(request.organizador().trim());
        }

        if (request.participanteIds() != null || request.participantes() != null) {
            evento.getParticipantes().clear();
            adicionarParticipantes(evento, request.participanteIds(), request.participantes());
        }

        Evento salvo = eventoRepository.save(evento);
        logger.info("Lembrete/Evento atualizado com sucesso: id={}", salvo.getId());

        return toResponse(salvo, solicitante);
    }

    @Transactional
    public void deletar(Long id, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Evento evento = buscarEntidade(id);

        boolean isCriador = evento.getCriador().getId().equals(solicitante.getId());
        boolean isLider = grupoAcessoService.isLider(evento.getGrupo().getId(), solicitante.getId());

        if (!isAdmin(solicitante) && !isCriador && !isLider) {
            throw new SemPermissaoNoGrupoErro("Você não possui permissão para excluir este lembrete.");
        }

        evento.setAtivo(false);
        eventoRepository.save(evento);
        logger.info("Lembrete/Evento inativado com sucesso: id={}", id);
    }

    @Transactional(readOnly = true)
    public LembretePaginadoResponse listarPorGrupo(
            Long grupoId,
            TipoLembrete tipo,
            String busca,
            int page,
            int size,
            String emailLogado
    ) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarAcessoAoGrupo(grupoId, solicitante);

        int pageSize = size > 0 ? size : 10;
        String buscaTrim = (busca != null && !busca.isBlank()) ? busca.trim() : "";

        // Busca sem filtro de tipo para que os totais por tipo reflitam a pesquisa inteira
        List<LembreteResponse> pesquisados = eventoRepository
                .filtrarPorGrupo(grupoId, buscaTrim, Pageable.unpaged()).getContent().stream()
                .map(e -> toResponse(e, solicitante))
                .toList();

        Map<String, Long> totaisPorTipo = new LinkedHashMap<>();
        for (TipoLembrete t : TipoLembrete.values()) {
            totaisPorTipo.put(t.getDescricao(), pesquisados.stream().filter(l -> l.tipo() == t).count());
        }

        List<LembreteResponse> ordenados = pesquisados.stream()
                .filter(l -> tipo == null || l.tipo() == tipo)
                .sorted(ORDEM_CRONOLOGICA)
                .toList();

        long total = ordenados.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        int pageAtual = Math.max(1, page);
        int from = (int) Math.min((long) (pageAtual - 1) * pageSize, total);
        int to = (int) Math.min((long) from + pageSize, total);

        return new LembretePaginadoResponse(
                ordenados.subList(from, to),
                total,
                totalPages,
                pageAtual,
                pageSize,
                totaisPorTipo
        );
    }

    @Transactional(readOnly = true)
    public List<LembreteResponse> listarTodosPorGrupo(Long grupoId, TipoLembrete tipo, String busca, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarAcessoAoGrupo(grupoId, solicitante);

        String buscaTrim = (busca != null && !busca.isBlank()) ? busca.trim() : "";

        return eventoRepository.filtrarPorGrupo(grupoId, buscaTrim, Pageable.unpaged()).getContent().stream()
                .filter(e -> tipo == null || e.getTipo() == tipo)
                .map(e -> toResponse(e, solicitante))
                .sorted(ORDEM_CRONOLOGICA)
                .toList();
    }

    /**
     * Exporta os lembretes/eventos ativos do grupo em formato iCalendar (.ics).
     * O arquivo inclui recorrência (RRULE), local, link, organizador e participantes.
     */
    @Transactional(readOnly = true)
    public byte[] exportarIcs(Long grupoId, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);

        Grupo grupo = grupoRepository.findById(grupoId)
                .filter(Grupo::isAtivo)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo não encontrado com ID: " + grupoId));

        validarAcessoAoGrupo(grupoId, solicitante);

        List<Evento> eventos = eventoRepository.findAllByGrupoIdAndIsAtivoTrueOrderByDthInicioAsc(grupoId);
        return IcsCalendarBuilder.gerar(grupo.getNome(), eventos);
    }

    @Transactional
    public LembreteResponse inserirAta(Long id, MultipartFile arquivo, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Evento evento = buscarEntidade(id);

        if (evento.getTipo() != TipoLembrete.REUNIAO) {
            throw new IllegalArgumentException("Apenas lembretes do tipo Reunião permitem inserção de arquivo de ata.");
        }

        validarCriadorOuAdmin(evento, solicitante, "Somente o criador do lembrete pode inserir a ata.");
        validarArquivoAta(arquivo);

        String uuid = UUID.randomUUID().toString();
        String originalFilename = arquivo.getOriginalFilename() != null && !arquivo.getOriginalFilename().isBlank()
                ? arquivo.getOriginalFilename()
                : "ata.pdf";

        String key = "atas/" + evento.getId() + "/" + uuid + "_" + originalFilename;
        logger.info("Enviando arquivo de ata do evento/lembrete {} para o S3: key={}", id, key);
        String s3Url = s3StorageService.uploadFile(key, arquivo);

        evento.setAtaS3Key(key);
        evento.setAtaUrl(s3Url);
        evento.setAtaNomeArquivo(originalFilename);

        Evento salvo = eventoRepository.save(evento);
        logger.info("Ata inserida com sucesso no evento/lembrete id={}", salvo.getId());

        return toResponse(salvo, solicitante);
    }

    @Transactional(readOnly = true)
    public byte[] downloadAta(Long id, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Evento evento = buscarEntidade(id);
        validarAcessoAoGrupo(evento.getGrupo().getId(), solicitante);

        if (!evento.isTemAta() || evento.getAtaS3Key() == null) {
            throw new IllegalArgumentException("Este lembrete não possui arquivo de ata disponível.");
        }

        return s3StorageService.downloadFile(evento.getAtaS3Key());
    }

    @Transactional
    public LembreteResponse confirmarPresenca(Long id, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Evento evento = buscarEntidade(id);
        validarAcessoAoGrupo(evento.getGrupo().getId(), solicitante);

        EventoParticipante participante = evento.getParticipantes().stream()
                .filter(p -> p.getUsuario() != null && p.getUsuario().getId().equals(solicitante.getId()))
                .findFirst()
                .orElse(null);

        if (participante != null) {
            participante.setPresencaConfirmada(!participante.isPresencaConfirmada());
        } else {
            EventoParticipante novoParticipante = EventoParticipante.builder()
                    .evento(evento)
                    .usuario(solicitante)
                    .nome(solicitante.getNome())
                    .presencaConfirmada(true)
                    .build();
            evento.getParticipantes().add(novoParticipante);
        }

        Evento salvo = eventoRepository.save(evento);
        return toResponse(salvo, solicitante);
    }

    private void adicionarParticipantes(Evento evento, List<Long> participanteIds, List<String> participanteNomes) {
        if (participanteIds != null) {
            for (Long usuarioId : participanteIds) {
                if (usuarioId != null) {
                    usuarioService.findById(usuarioId).ifPresent(usuario -> {
                        boolean exists = evento.getParticipantes().stream()
                                .anyMatch(p -> p.getUsuario() != null && p.getUsuario().getId().equals(usuario.getId()));
                        if (!exists) {
                            evento.getParticipantes().add(EventoParticipante.builder()
                                    .evento(evento)
                                    .usuario(usuario)
                                    .nome(usuario.getNome())
                                    .presencaConfirmada(false)
                                    .build());
                        }
                    });
                }
            }
        }

        if (participanteNomes != null) {
            for (String nome : participanteNomes) {
                if (nome != null && !nome.isBlank()) {
                    String trimmed = nome.trim();
                    boolean exists = evento.getParticipantes().stream()
                            .anyMatch(p -> p.getNome().equalsIgnoreCase(trimmed));
                    if (!exists) {
                        evento.getParticipantes().add(EventoParticipante.builder()
                                .evento(evento)
                                .usuario(null)
                                .nome(trimmed)
                                .presencaConfirmada(false)
                                .build());
                    }
                }
            }
        }
    }

    private Evento buscarEntidade(Long id) {
        return eventoRepository.findByIdComRelacionamentos(id)
                .orElseThrow(() -> new EventoNaoEncontradoErro("Lembrete/Evento não encontrado com ID: " + id));
    }

    private Usuario usuarioAutenticado(String emailLogado) {
        return usuarioService.findByEmail(emailLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário autenticado não encontrado."));
    }

    private boolean isAdmin(Usuario usuario) {
        if (usuario.getCredenciaisLogin() == null) {
            return false;
        }
        AccessLevel nivel = usuario.getCredenciaisLogin().getAccessLevel();
        return nivel == AccessLevel.ADMIN || nivel == AccessLevel.INITIAL_ADMIN;
    }

    private void validarAcessoAoGrupo(Long grupoId, Usuario solicitante) {
        if (isAdmin(solicitante)) {
            return;
        }
        grupoAcessoService.validarMembro(grupoId, solicitante.getId());
    }

    private void validarCriadorOuAdmin(Evento evento, Usuario solicitante, String mensagemErro) {
        if (isAdmin(solicitante)) {
            return;
        }
        if (!Objects.equals(evento.getCriador().getId(), solicitante.getId())) {
            throw new SemPermissaoNoGrupoErro(mensagemErro);
        }
    }

    private void validarArquivoAta(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de ata não pode ser nulo ou vazio.");
        }
        String originalFilename = arquivo.getOriginalFilename();
        if (originalFilename != null && !originalFilename.isBlank()) {
            String lower = originalFilename.toLowerCase();
            if (!lower.endsWith(".pdf") && !lower.endsWith(".docx") && !lower.endsWith(".doc")) {
                throw new IllegalArgumentException("O arquivo de ata deve ser nos formatos PDF, DOC ou DOCX.");
            }
        }
    }

    private LembreteResponse toResponse(Evento evento, Usuario solicitante) {
        TipoRecorrencia recorrenciaEvento = evento.getRecorrencia() != null ? evento.getRecorrencia() : TipoRecorrencia.NENHUMA;
        LocalDateTime proxima = recorrenciaEvento.proximaOcorrencia(evento.getDthInicio(), LocalDateTime.now());
        StatusLembrete status = StatusLembrete.calcularStatus(proxima);

        boolean presencaConfirmada = evento.getParticipantes().stream()
                .anyMatch(p -> p.getUsuario() != null
                        && p.getUsuario().getId().equals(solicitante.getId())
                        && p.isPresencaConfirmada());

        List<String> participanteNomes = evento.getParticipantes().stream()
                .map(EventoParticipante::getNome)
                .toList();

        List<ParticipanteLembreteResponse> detalhes = evento.getParticipantes().stream()
                .map(p -> new ParticipanteLembreteResponse(
                        p.getId(),
                        p.getUsuario() != null ? p.getUsuario().getId() : null,
                        p.getNome(),
                        p.isPresencaConfirmada()))
                .toList();

        return new LembreteResponse(
                evento.getId(),
                evento.getGrupo().getId(),
                evento.getGrupo().getNome(),
                evento.getCriador().getId(),
                evento.getCriador().getNome(),
                evento.getTitulo(),
                evento.getDescricao(),
                evento.getTipo(),
                evento.getDthInicio(),
                proxima,
                evento.getLocalizacao(),
                evento.getLocalizacao(),
                evento.getUrl(),
                evento.getRecorrencia(),
                evento.getOrganizador(),
                participanteNomes,
                detalhes,
                status,
                presencaConfirmada,
                evento.isTemAta(),
                evento.getAtaUrl(),
                evento.getAtaNomeArquivo(),
                evento.getDthRegistro(),
                evento.isAtivo()
        );
    }
}
