package com.atlante.bootstrap;

import com.atlante.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * All'avvio, in ogni ambiente: crea i ruoli di sistema e, su un database nato quando il ruolo era
 * una colonna di "users", sposta quel valore nella tabella user_roles e toglie la colonna.
 * Gira prima dei seeder, che hanno bisogno dei ruoli.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RoleBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleBootstrap.class);

    private final RoleService roleService;
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        roleService.ensureSystemRoles();

        Integer legacy = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where lower(table_name) = 'users' and lower(column_name) = 'role'
                """, Integer.class);
        if (legacy == null || legacy == 0) return;

        // ogni utente riceve USER, più il ruolo che aveva nella vecchia colonna
        int moved = jdbc.update("""
                insert into user_roles (user_id, role_id)
                select u.id, r.id from users u join roles r on r.name = u.role
                where not exists (select 1 from user_roles ur where ur.user_id = u.id and ur.role_id = r.id)
                """);
        jdbc.update("""
                insert into user_roles (user_id, role_id)
                select u.id, r.id from users u join roles r on r.name = 'USER'
                where not exists (select 1 from user_roles ur where ur.user_id = u.id and ur.role_id = r.id)
                """);
        jdbc.execute("alter table users drop column role");
        log.info("Ruoli migrati nella tabella user_roles ({} assegnazioni); colonna users.role rimossa", moved);
    }
}
