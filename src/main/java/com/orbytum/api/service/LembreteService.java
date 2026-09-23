package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CreateLembreteRequest;
import com.orbytum.api.models.dto.request.EditLembreteRequest;
import com.orbytum.api.models.dto.response.LembretePaginadoResponse;
import com.orbytum.api.models.dto.response.LembreteResponse;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Lembrete;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.LembreteNaoEncontradoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.GrupoRepository;
import com.orbytum.api.repository.LembreteRepository;
import com.orbytum.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LembreteService {

    private final LembreteRepository lembreteRepository;
    private final GrupoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final S3StorageService s3StorageService;

    @Transactional
    public LembreteResponse criarLembrete(CreateLembreteRequest request, String emailUsuarioLogado) {
        Usuario organizador = usuarioRepository.findByEmail(emailUsuarioLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário logado não encontrado"));

        Grupo grupo = grupoRepository.findById(request.getGrupoId())
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado"));

        List<Usuario> participantes = new ArrayList<>();
        if (request.getParticipantesIds() != null && !request.getParticipantesIds().isEmpty()) {
            participantes = usuarioRepository.findAllById(request.getParticipantesIds());
        }

        Lembrete lembrete = Lembrete.builder()
                .titulo(request.getTitulo())
                .descricao(request.getDescricao())
                .tipo(request.getTipo())
                .dataHora(request.getDataHora())
                .localizacao(request.getLocalizacao())
                .link(request.getLink())
                .recorrencia(request.getRecorrencia())
                .grupo(grupo)
                .organizador(organizador)
                .participantes(participantes)
                .dthCriacao(LocalDateTime.now())
                .isAtivo(true)
                .build();

        Lembrete salvo = lembreteRepository.save(lembrete);
        log.info("Lembrete '{}' (ID: {}) criado com sucesso para o grupo ID {}", salvo.getTitulo(), salvo.getId(), grupo.getId());
        return mapToResponse(salvo);
    }

    @Transactional(readOnly = true)
    public LembretePaginadoResponse listarLembretes(Long grupoId, TipoLembrete tipo, String busca, int page, int size) {
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, size);

        Page<Lembrete> pag = lembreteRepository.filtrarLembretes(grupoId, tipo, busca, pageable);
        List<LembreteResponse> items = pag.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new LembretePaginadoResponse(
                items,
                pag.getTotalElements(),
                pag.getTotalPages(),
                page,
                size
        );
    }

    @Transactional(readOnly = true)
    public LembreteResponse buscarPorId(Long id) {
        Lembrete lembrete = lembreteRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new LembreteNaoEncontradoErro("Lembrete não encontrado com ID: " + id));
        return mapToResponse(lembrete);
    }

    @Transactional
    public LembreteResponse atualizarLembrete(Long id, EditLembreteRequest request, String emailUsuarioLogado) {
        Lembrete lembrete = lembreteRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new LembreteNaoEncontradoErro("Lembrete não encontrado com ID: " + id));

        // CA 6: Se tipo for Reunião, somente o criador pode editar o lembrete
        if (lembrete.getTipo() == TipoLembrete.REUNIAO) {
            validarCriador(lembrete, emailUsuarioLogado, "Somente o criador da reunião pode editar este lembrete.");
        }

        List<Usuario> participantes = new ArrayList<>();
        if (request.getParticipantesIds() != null) {
            participantes = usuarioRepository.findAllById(request.getParticipantesIds());
        }

        lembrete.setTitulo(request.getTitulo());
        lembrete.setDescricao(request.getDescricao());
        lembrete.setTipo(request.getTipo());
        lembrete.setDataHora(request.getDataHora());
        lembrete.setLocalizacao(request.getLocalizacao());
        lembrete.setLink(request.getLink());
        lembrete.setRecorrencia(request.getRecorrencia());
        lembrete.setParticipantes(participantes);

        Lembrete atualizado = lembreteRepository.save(lembrete);
        log.info("Lembrete ID {} atualizado com sucesso.", id);
        return mapToResponse(atualizado);
    }

    @Transactional
    public LembreteResponse anexarAtaReuniao(Long id, MultipartFile file, String emailUsuarioLogado) {
        Lembrete lembrete = lembreteRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new LembreteNaoEncontradoErro("Lembrete não encontrado com ID: " + id));

        if (lembrete.getTipo() != TipoLembrete.REUNIAO) {
            throw new IllegalArgumentException("Somente lembretes do tipo Reunião permitem anexar ata.");
        }

        // CA 6: Somente o criador pode inserir a ata de reunião
        validarCriador(lembrete, emailUsuarioLogado, "Somente o criador da reunião pode anexar a ata.");

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de ata não pode ser nulo ou vazio.");
        }

        // Remover ata antiga do S3 se existir
        if (lembrete.getAtaKey() != null && !lembrete.getAtaKey().isBlank()) {
            try {
                s3StorageService.deleteFile(lembrete.getAtaKey());
            } catch (Exception e) {
                log.warn("Erro ao remover ata antiga do S3 (key: {}): {}", lembrete.getAtaKey(), e.getMessage());
            }
        }

        String key = "atas/lembrete_" + id + "_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        s3StorageService.uploadFile(key, file);

        lembrete.setAtaKey(key);
        lembrete.setAtaNomeOriginal(file.getOriginalFilename());

        Lembrete atualizado = lembreteRepository.save(lembrete);
        log.info("Ata anexada ao lembrete de reunião ID {} por {}", id, emailUsuarioLogado);
        return mapToResponse(atualizado);
    }

    @Transactional
    public LembreteResponse removerAtaReuniao(Long id, String emailUsuarioLogado) {
        Lembrete lembrete = lembreteRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new LembreteNaoEncontradoErro("Lembrete não encontrado com ID: " + id));

        validarCriador(lembrete, emailUsuarioLogado, "Somente o criador da reunião pode remover a ata.");

        if (lembrete.getAtaKey() != null && !lembrete.getAtaKey().isBlank()) {
            try {
                s3StorageService.deleteFile(lembrete.getAtaKey());
            } catch (Exception e) {
                log.warn("Erro ao remover ata do S3 (key: {}): {}", lembrete.getAtaKey(), e.getMessage());
            }
        }

        lembrete.setAtaKey(null);
        lembrete.setAtaNomeOriginal(null);

        Lembrete atualizado = lembreteRepository.save(lembrete);
        log.info("Ata removida do lembrete ID {} por {}", id, emailUsuarioLogado);
        return mapToResponse(atualizado);
    }

    @Transactional
    public void inativarLembrete(Long id, String emailUsuarioLogado) {
        Lembrete lembrete = lembreteRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new LembreteNaoEncontradoErro("Lembrete não encontrado com ID: " + id));

        if (lembrete.getTipo() == TipoLembrete.REUNIAO) {
            validarCriador(lembrete, emailUsuarioLogado, "Somente o criador da reunião pode excluir este lembrete.");
        }

        lembrete.setAtivo(false);
        lembreteRepository.save(lembrete);
        log.info("Lembrete ID {} inativado por {}", id, emailUsuarioLogado);
    }

    public byte[] baixarAta(Long id) {
        Lembrete lembrete = lembreteRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new LembreteNaoEncontradoErro("Lembrete não encontrado com ID: " + id));

        if (lembrete.getAtaKey() == null || lembrete.getAtaKey().isBlank()) {
            throw new IllegalArgumentException("Este lembrete não possui ata anexada.");
        }

        return s3StorageService.downloadFile(lembrete.getAtaKey());
    }

    private void validarCriador(Lembrete lembrete, String emailUsuarioLogado, String mensagemErro) {
        if (lembrete.getOrganizador() == null || !lembrete.getOrganizador().getEmail().equalsIgnoreCase(emailUsuarioLogado)) {
            throw new AccessDeniedException(mensagemErro);
        }
    }

    private LembreteResponse mapToResponse(Lembrete lembrete) {
        String ataUrl = null;
        if (lembrete.getAtaKey() != null && !lembrete.getAtaKey().isBlank()) {
            try {
                ataUrl = s3StorageService.getFileUrl(lembrete.getAtaKey());
            } catch (Exception e) {
                log.warn("Erro ao obter URL S3 para ata (key: {}): {}", lembrete.getAtaKey(), e.getMessage());
            }
        }

        List<LembreteResponse.ParticipanteLembreteResponse> participantes = lembrete.getParticipantes() == null ? List.of() :
                lembrete.getParticipantes().stream()
                        .map(p -> new LembreteResponse.ParticipanteLembreteResponse(p.getId(), p.getNome(), p.getEmail()))
                        .toList();

        return new LembreteResponse(
                lembrete.getId(),
                lembrete.getTitulo(),
                lembrete.getDescricao(),
                lembrete.getTipo(),
                lembrete.getDataHora(),
                lembrete.getLocalizacao(),
                lembrete.getLink(),
                lembrete.getRecorrencia(),
                lembrete.getGrupo().getId(),
                lembrete.getGrupo().getNome(),
                lembrete.getOrganizador().getId(),
                lembrete.getOrganizador().getNome(),
                lembrete.getOrganizador().getEmail(),
                participantes,
                lembrete.getAtaNomeOriginal(),
                ataUrl,
                lembrete.getAtaKey() != null && !lembrete.getAtaKey().isBlank(),
                lembrete.getDthCriacao()
        );
    }
}
