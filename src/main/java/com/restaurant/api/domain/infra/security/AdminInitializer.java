package com.restaurant.api.domain.infra.security;

import com.restaurant.api.domain.entity.User;
import com.restaurant.api.domain.enums.Role;
import com.restaurant.api.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador seguro do usuário Administrador.
 * Cria o administrador automaticamente na inicialização caso as variáveis de ambiente
 * estejam definidas e o usuário ainda não exista no banco de dados.
 */
@Component
public class AdminInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.security.admin.login:admin}")
  private String adminLogin;

  @Value("${app.security.admin.password:}")
  private String adminPassword;

  @Value("${app.security.admin.name:Administrador}")
  private String adminName;

  @Value("${app.security.admin.phone:5500000000000}")
  private String adminPhone;

  public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    if (adminPassword == null || adminPassword.isBlank()) {
      log.info("[SECURITY] Nenhuma senha de administrador configurada em ADMIN_DEFAULT_PASSWORD. Pulando inicialização automática.");
      return;
    }

    if (!userRepository.existsByLogin(adminLogin)) {
      User admin = new User();
      admin.setLogin(adminLogin);
      admin.setPassword(passwordEncoder.encode(adminPassword));
      admin.setName(adminName != null && !adminName.isBlank() ? adminName : "Administrador");
      admin.setNumber(adminPhone != null && !adminPhone.isBlank() ? adminPhone : "5500000000000");
      admin.setRole(Role.ADMIN);
      admin.setActive(true);

      userRepository.save(admin);
      log.info("[SECURITY] Usuário administrador inicial '{}' criado com sucesso com papel ROLE_ADMIN.", adminLogin);
    } else {
      log.debug("[SECURITY] Usuário administrador '{}' já existe no banco de dados.", adminLogin);
    }
  }
}
