package com.orbytum.api.configuration;

import com.orbytum.api.model.entity.CredenciaisLogin;
import com.orbytum.api.model.entity.Usuario;
import com.orbytum.api.model.enums.AccessLevel;
import com.orbytum.api.service.CredenciaisLoginService;
import com.orbytum.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInicialInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInicialInitializer.class);

    private final CredenciaisLoginService credenciaisLoginService;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    @Value("${orbytum.initial-admin.email:admin.inicial@orbytum.com}")
    private String adminInicialEmail;

    @Value("${orbytum.initial-admin.password:AdminInit@123}")
    private String adminInicialPassword;

    @Override
    public void run(String... args) {
        boolean temAdminDefinitivo = credenciaisLoginService.existsByAccessLevel(AccessLevel.ADMIN);
        boolean temAdminInicial = credenciaisLoginService.existsByAccessLevel(AccessLevel.INITIAL_ADMIN);

        if (!temAdminDefinitivo && !temAdminInicial) {
            logger.info("Nenhum administrador detectado no banco de dados. Criando Administrador Inicial padrão...");

            Usuario usuarioInicial = new Usuario(
                    "Administrador Padrão",
                    adminInicialEmail,
                    "00000000000",
                    "Administrador"
            );
            usuarioInicial = usuarioService.save(usuarioInicial);

            CredenciaisLogin credenciaisInicial = new CredenciaisLogin(
                    adminInicialEmail,
                    passwordEncoder.encode(adminInicialPassword),
                    AccessLevel.INITIAL_ADMIN,
                    usuarioInicial,
                    null
            );
            credenciaisLoginService.save(credenciaisInicial);

            logger.info("==================================================================");
            logger.info("[SEGURANÇA] Administrador Inicial criado com sucesso!");
            logger.info("E-mail: {}", adminInicialEmail);
            logger.info("Senha: {}", adminInicialPassword);
            logger.info("Atenção: Esta conta possui permissão exclusiva para cadastrar o primeiro administrador definitivo.");
            logger.info("==================================================================");
        } else {
            logger.info("Verificação de administradores na inicialização concluída.");
        }
    }
}
