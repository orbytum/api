package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.EmailRequest;
import com.orbytum.api.models.dto.request.GerarConviteCadastroRequest;
import com.orbytum.api.models.dto.request.GerarConviteGrupoRequest;
import com.orbytum.api.models.dto.request.RegisterRequest;
import com.orbytum.api.models.dto.response.AuthResponse;
import com.orbytum.api.models.dto.response.ConviteCadastroDetalheResponse;
import com.orbytum.api.models.dto.response.ConviteCadastroPaginadoResponse;
import com.orbytum.api.models.dto.response.ConviteCadastroResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoEnviadoResponse;
import com.orbytum.api.models.dto.response.ConviteGrupoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.orbytum.api.models.entity.*;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.exceptions.*;
import com.orbytum.api.repository.ConviteCadastroRepository;
import com.orbytum.api.repository.ConviteGrupoRepository;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.RoleRepository;
import com.orbytum.api.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ConviteService {

    private final ConviteGrupoRepository conviteGrupoRepository;
    private final ConviteCadastroRepository conviteCadastroRepository;
    private final CredenciaisLoginService credenciaisLoginService;
    private final GrupoService grupoService;
    private final UsuarioService usuarioService;
    private final GrupoXUsuarioService grupoXUsuarioService;
    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final ProjetoService projetoService;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public ConviteGrupoEnviadoResponse enviarConviteGrupo(Usuario remetente, Long grupoId, String emailConvidado, List<Long> projetoIds) {
        Grupo grupo = grupoService.findById(grupoId)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo não encontrado com ID: " + grupoId));

        validarPermissaoConviteGrupo(remetente, grupoId);

        Usuario convidado = usuarioService.findByEmail(emailConvidado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário convidado não encontrado com e-mail: " + emailConvidado));

        if (grupoXUsuarioService.isUsuarioNoGrupo(grupoId, convidado.getId())) {
            throw new UsuarioJaNoGrupoErro("O usuário convidado já pertence a este grupo");
        }

        List<Projeto> projetos = validarProjetos(grupoId, projetoIds);

        LocalDateTime dthExpiracao = LocalDateTime.now().plusDays(7);
        ConviteGrupo conviteGrupo = new ConviteGrupo(grupo, convidado, remetente, projetos, dthExpiracao);
        conviteGrupo = conviteGrupoRepository.save(conviteGrupo);

        List<Long> idsProjetosSalvos = conviteGrupo.getProjetos() != null
                ? conviteGrupo.getProjetos().stream().map(Projeto::getId).collect(Collectors.toList())
                : List.of();

        return new ConviteGrupoEnviadoResponse(
                conviteGrupo.getId(),
                grupo.getId(),
                grupo.getNome(),
                convidado.getEmail(),
                idsProjetosSalvos,
                conviteGrupo.getDthRegistro(),
                conviteGrupo.getDthExpiracao(),
                conviteGrupo.isAtivo()
        );
    }

    @Transactional
    public ConviteGrupoResponse gerarConviteGrupo(Usuario remetente, GerarConviteGrupoRequest request) {
        Grupo grupo = grupoService.findById(request.idGrupo())
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo não encontrado com ID: " + request.idGrupo()));

        validarPermissaoConviteGrupo(remetente, request.idGrupo());

        List<Projeto> projetos = validarProjetos(request.idGrupo(), request.idsProjeto());

        Role role = null;
        if (request.idRole() != null) {
            role = roleRepository.findById(request.idRole())
                    .orElseThrow(() -> new IllegalArgumentException("Cargo não encontrado com ID: " + request.idRole()));
        }

        Integer limiteUso = request.limiteUso();
        if (role != null && isLiderRole(role)) {
            limiteUso = 1;
        }

        String token = UUID.randomUUID().toString();
        int diasValidade = (request.diasValidade() != null && request.diasValidade() > 0) ? request.diasValidade() : 7;
        LocalDateTime dthExpiracao = LocalDateTime.now().plusDays(diasValidade);

        ConviteGrupo conviteGrupo = new ConviteGrupo(grupo, remetente, token, projetos, dthExpiracao, role, limiteUso);
        conviteGrupo = conviteGrupoRepository.save(conviteGrupo);

        List<Long> idsProjetosSalvos = conviteGrupo.getProjetos() != null
                ? conviteGrupo.getProjetos().stream().map(Projeto::getId).collect(Collectors.toList())
                : List.of();

        String urlConvite = "/convites/aceitar/grupo/" + token;
        String nomeCargo = role != null ? role.getNome() : null;

        return new ConviteGrupoResponse(
                conviteGrupo.getId(),
                token,
                urlConvite,
                grupo.getId(),
                grupo.getNome(),
                idsProjetosSalvos,
                conviteGrupo.getDthRegistro(),
                conviteGrupo.getDthExpiracao(),
                conviteGrupo.isAtivo(),
                nomeCargo,
                conviteGrupo.getLimiteUso(),
                conviteGrupo.getUsos()
        );
    }

    @Transactional
    public ConviteCadastroResponse gerarConviteCadastro(Usuario remetente, GerarConviteCadastroRequest request) {

        AccessLevel accessLevel = credenciaisLoginService.findByEmail(remetente.getEmail())
                .map(CredenciaisLogin::getAccessLevel)
                .orElseGet(() -> remetente.getCredenciaisLogin() != null
                        ? remetente.getCredenciaisLogin().getAccessLevel()
                        : null);

        if (accessLevel != AccessLevel.ADMIN) {
            throw new SemPermissaoConvidarErro("Você não tem permissão para enviar convites de cadastro.");
        }

        var conviteExistente = conviteCadastroRepository.findByEmailAndIsAtivoTrue(request.email());
        if(conviteExistente.isPresent()) {
            throw new ConviteJaEnviadoErro("Esse email já possui um convite pendente ativo.");
        }

        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString();
        int dias = (request.diasValidade() != null && request.diasValidade() > 0) ? request.diasValidade() : 7;

        ConviteCadastro convite = new ConviteCadastro(
                null,
                token,
                request.email(),
                now.plusDays(dias),
                now,
                true
        );

        convite = conviteCadastroRepository.save(convite);

        String url = "/convites/aceitar/cadastro/" + token;

        String assunto = "Você foi convidado para se juntar ao Orbytum";
        String templateName = "convite-template";
        Map<String, Object> variaveis = Map.of(
                "nomeOrganizacao", "Orbytum",
                "loginUrl", "http://localhost:8080" + url
        );

        EmailRequest emailReq = EmailRequest.comTemplate(request.email(), assunto, templateName, variaveis);

        try {
            emailService.sendEmail(emailReq);
        } catch (Exception e) {
            log.warn("Não foi possível enviar o e-mail de convite para {}: {}", request.email(), e.getMessage());
        }

        return new ConviteCadastroResponse(
                convite.getId(),
                token,
                url,
                convite.getDthExpiracao()
        );
    }

    public ConviteCadastroPaginadoResponse listarConvitesCadastro(
            Usuario solicitante,
            int page,
            int size,
            String email,
            String status
    ) {
        AccessLevel accessLevel = credenciaisLoginService.findByEmail(solicitante.getEmail())
                .map(CredenciaisLogin::getAccessLevel)
                .orElseGet(() -> solicitante.getCredenciaisLogin() != null
                        ? solicitante.getCredenciaisLogin().getAccessLevel()
                        : null);

        if (accessLevel != AccessLevel.ADMIN) {
            throw new SemPermissaoConvidarErro("Você não tem permissão para visualizar convites de cadastro.");
        }

        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 5;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim().toLowerCase() : "ativos";
        String normalizedEmail = (email != null && !email.isBlank()) ? email.trim() : null;

        LocalDateTime now = LocalDateTime.now();
        Page<ConviteCadastro> pageResult = conviteCadastroRepository.filtrarConvites(
                normalizedEmail,
                normalizedStatus,
                now,
                pageable
        );

        List<ConviteCadastroDetalheResponse> items = pageResult.getContent().stream()
                .map(c -> new ConviteCadastroDetalheResponse(
                        c.getId(),
                        c.getEmail(),
                        c.getToken(),
                        "/convites/aceitar/cadastro/" + c.getToken(),
                        c.getDthRegistro(),
                        c.getDthExpiracao(),
                        c.isAtivo() && c.getDthExpiracao().isAfter(now)
                ))
                .collect(Collectors.toList());

        long totalAtivos = conviteCadastroRepository.countAtivos(now);
        long totalInativos = conviteCadastroRepository.countInativos(now);
        long totalGeral = conviteCadastroRepository.count();

        return new ConviteCadastroPaginadoResponse(
                items,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                page,
                pageSize,
                totalAtivos,
                totalInativos,
                totalGeral
        );
    }

    @Transactional
    public void revogarConviteCadastro(Long id, Usuario solicitante) {
        AccessLevel accessLevel = credenciaisLoginService.findByEmail(solicitante.getEmail())
                .map(CredenciaisLogin::getAccessLevel)
                .orElseGet(() -> solicitante.getCredenciaisLogin() != null
                        ? solicitante.getCredenciaisLogin().getAccessLevel()
                        : null);

        if (accessLevel != AccessLevel.ADMIN && accessLevel != AccessLevel.INITIAL_ADMIN) {
            throw new SemPermissaoConvidarErro("Você não tem permissão para revogar convites de cadastro.");
        }

        ConviteCadastro convite = conviteCadastroRepository.findById(id)
                .orElseThrow(() -> new ConviteInvalidoOuExpiradoErro("Convite não encontrado."));

        convite.setAtivo(false);
        conviteCadastroRepository.save(convite);
    }

    @Transactional
    public ConviteGrupoEnviadoResponse aceitarConviteGrupo(String token, Usuario usuarioLogado) {
        ConviteGrupo conviteGrupo = conviteGrupoRepository.findByTokenAndIsAtivoTrue(token)
                .orElseThrow(() -> new ConviteInvalidoOuExpiradoErro("Convite por link inválido ou inativo"));

        if (conviteGrupo.getDthExpiracao().isBefore(LocalDateTime.now())) {
            conviteGrupo.setAtivo(false);
            conviteGrupoRepository.save(conviteGrupo);
            throw new ConviteInvalidoOuExpiradoErro("Este convite por link já expirou");
        }

        if (conviteGrupo.getLimiteUso() != null && conviteGrupo.getUsos() != null && conviteGrupo.getUsos() >= conviteGrupo.getLimiteUso()) {
            conviteGrupo.setAtivo(false);
            conviteGrupoRepository.save(conviteGrupo);
            throw new ConviteInvalidoOuExpiradoErro("Este convite por link já atingiu o limite de usos");
        }

        Grupo grupo = conviteGrupo.getGrupo();
        if (grupoXUsuarioService.isUsuarioNoGrupo(grupo.getId(), usuarioLogado.getId())) {
            throw new UsuarioJaNoGrupoErro("Você já pertence a este grupo");
        }

        Role role = conviteGrupo.getRole();
        if (role == null) {
            role = roleRepository.findByNome("Membro")
                    .orElseGet(() -> roleRepository.findAll().stream().findFirst().orElse(null));
        }

        GrupoXUsuario gxu = new GrupoXUsuario(grupo, usuarioLogado, role, true);
        grupoXUsuarioRepository.save(gxu);

        int novosUsos = (conviteGrupo.getUsos() == null ? 0 : conviteGrupo.getUsos()) + 1;
        conviteGrupo.setUsos(novosUsos);
        if (conviteGrupo.getLimiteUso() != null && novosUsos >= conviteGrupo.getLimiteUso()) {
            conviteGrupo.setAtivo(false);
        }
        conviteGrupoRepository.save(conviteGrupo);

        List<Long> idsProjetos = conviteGrupo.getProjetos() != null
                ? conviteGrupo.getProjetos().stream().map(Projeto::getId).collect(Collectors.toList())
                : List.of();

        return new ConviteGrupoEnviadoResponse(
                conviteGrupo.getId(),
                grupo.getId(),
                grupo.getNome(),
                usuarioLogado.getEmail(),
                idsProjetos,
                conviteGrupo.getDthRegistro(),
                conviteGrupo.getDthExpiracao(),
                conviteGrupo.isAtivo()
        );
    }

    @Transactional
    public AuthResponse aceitarConviteCadastro(String token, RegisterRequest request) {

        ConviteCadastro c = conviteCadastroRepository.findByTokenAndIsAtivoTrue(token)
                .orElseThrow(() -> new ConviteInvalidoOuExpiradoErro("Convite não encontrado."));

        Usuario usuario = new Usuario(
                request.nome(),
                c.getEmail(),
                request.telefone(),
                request.titulo()
        );

        usuario = usuarioService.save(usuario);

        AccessLevel accessLevel = usuarioService.count() == 1
                ? AccessLevel.ADMIN
                : AccessLevel.USER;

        CredenciaisLogin credenciais = new CredenciaisLogin(
                c.getEmail(),
                passwordEncoder.encode(request.senha()),
                accessLevel,
                usuario,
                null
        );

        c.setAtivo(false);

        conviteCadastroRepository.save(c);
        credenciaisLoginService.save(credenciais);

        UserDetails userDetails = User.builder()
                .username(c.getEmail())
                .password(credenciais.getSenha())
                .authorities(Collections.emptyList())
                .build();

        String bearerToken = jwtUtil.generateToken(userDetails, AccessLevel.USER, Collections.emptyList());
        return new AuthResponse(bearerToken, "Bearer");
    }

    private List<Projeto> validarProjetos(Long grupoId, List<Long> idsProjeto) {
        if (idsProjeto == null || idsProjeto.isEmpty()) {
            return List.of();
        }
        List<Projeto> projetos = projetoService.findAllByIds(idsProjeto);
        if (projetos.size() != idsProjeto.size()) {
            throw new ProjetoNaoEncontradoErro("Um ou mais projetos informados não foram encontrados");
        }
        for (Projeto projeto : projetos) {
            if (!projeto.getGrupo().getId().equals(grupoId)) {
                throw new ProjetoNaoPertenceAoGrupoErro("O projeto '" + projeto.getTitulo() + "' não pertence ao grupo informado");
            }
        }
        return projetos;
    }

    private void validarPermissaoConviteGrupo(Usuario remetente, Long grupoId) {
        boolean isAdmin = remetente.getCredenciaisLogin() != null &&
                remetente.getCredenciaisLogin().getAccessLevel() == AccessLevel.ADMIN;

        if (isAdmin) {
            return;
        }

        Optional<GrupoXUsuario> gxuOpt = grupoXUsuarioService.findByGrupoIdAndUsuarioId(grupoId, remetente.getId());
        if (gxuOpt.isEmpty()) {
            throw new SemPermissaoConvidarErro("Você não possui permissão para enviar convites para este grupo");
        }

        GrupoXUsuario gxu = gxuOpt.get();
        if (!isLiderRole(gxu.getRole())) {
            throw new SemPermissaoConvidarErro("Você não possui permissão para enviar convites para este grupo. É necessário ser líder do grupo.");
        }
    }

    private boolean isLiderRole(Role role) {
        if (role == null) {
            return false;
        }
        return role.isLider();
    }
}
