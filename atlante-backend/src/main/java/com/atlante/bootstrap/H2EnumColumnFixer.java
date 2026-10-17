package com.atlante.bootstrap;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Sul database H2 di sviluppo Hibernate crea le colonne degli enum come ENUM('A','B') e,
 * con ddl-auto=update, non le aggiorna quando si aggiunge un valore (es. il ruolo USER):
 * gli inserimenti fallirebbero. All'avvio le converte in VARCHAR, senza toccare i dati.
 */
@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class H2EnumColumnFixer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(H2EnumColumnFixer.class);

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> columns = jdbc.queryForList("""
                select table_name, column_name from information_schema.columns
                where table_schema = 'PUBLIC' and data_type = 'ENUM'
                """);
        for (Map<String, Object> c : columns) {
            String table = (String) c.get("TABLE_NAME");
            String column = (String) c.get("COLUMN_NAME");
            jdbc.execute("alter table \"" + table + "\" alter column \"" + column + "\" varchar(30)");
            log.info("colonna {}.{} convertita da ENUM a VARCHAR", table, column);
        }
    }
}
