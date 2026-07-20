package com.gymplatform.config;

import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Carga datos demo desde SQL.
 * Fuente de verdad: {@code db/demo-seed.sql}, {@code demo-seed-sales.sql}, {@code demo-seed-member.sql}.
 */
@Component
@Order(5)
public class DemoSqlSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSqlSeeder.class);
    private static final String SCRIPT_CORE = "db/demo-seed.sql";
    private static final String SCRIPT_SALES = "db/demo-seed-sales.sql";
    private static final String SCRIPT_MEMBER = "db/demo-seed-member.sql";
    private static final String SCRIPT_MEMBER_STAFF = "db/demo-seed-member-staff.sql";
    /** BCrypt de 12345678 (mismas cuentas demo). */
    private static final String PRIVATE_AREA_HASH =
            "$2a$10$aZ8ODce.PMKUkYFMVLPIRecG4Dc2tbnHwYdRJuCre6PfScQzioAi2";

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final ActivityImageSeeder activityImageSeeder;

    public DemoSqlSeeder(
            DataSource dataSource,
            JdbcTemplate jdbc,
            ActivityImageSeeder activityImageSeeder) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.activityImageSeeder = activityImageSeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        activityImageSeeder.ensureDemoImages();

        Integer fitlife = jdbc.queryForObject(
                "SELECT COUNT(*) FROM organizations WHERE slug = 'fitlife'", Integer.class);
        if (fitlife == null || fitlife == 0) {
            runScript(SCRIPT_CORE);
        } else {
            log.info("Demo SQL core omitido: ya existe organización fitlife");
        }

        ensureFitLifeIndigo();
        ensurePrivateAreasPassword();
        ensureSalesDemo();
        ensureMemberDemo();
        ensureMemberStaffDemo();
        printHints();
    }

    private void ensureSalesDemo() {
        Long orgId = fitLifeOrgId();
        if (orgId == null) {
            return;
        }
        Integer sales = jdbc.queryForObject(
                "SELECT COUNT(*) FROM store_sales WHERE organization_id = ?", Integer.class, orgId);
        if (sales != null && sales > 0) {
            log.info("Demo ventas omitido: ya hay {} ventas en FitLife", sales);
            return;
        }
        runScript(SCRIPT_SALES);
    }

    private void ensureMemberDemo() {
        Long orgId = fitLifeOrgId();
        if (orgId == null) {
            return;
        }
        Integer routines = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM routines r
                JOIN users u ON u.id = r.member_id
                WHERE u.email = 'miembro@fitlife.com'
                """,
                Integer.class);
        if (routines != null && routines > 0) {
            log.info("Demo miembro omitido: ya hay rutinas para miembro@fitlife.com");
            return;
        }
        runScript(SCRIPT_MEMBER);
    }

    /** Datos para dueño/recepción al usar perfil Miembro (switch de roles). */
    private void ensureMemberStaffDemo() {
        Long orgId = fitLifeOrgId();
        if (orgId == null) {
            return;
        }
        Integer routines = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM routines r
                JOIN users u ON u.id = r.member_id
                WHERE u.email = 'dueno@fitlife.com'
                """,
                Integer.class);
        if (routines != null && routines > 0) {
            log.info("Demo miembro-staff omitido: ya hay rutinas para dueno@fitlife.com");
            return;
        }
        runScript(SCRIPT_MEMBER_STAFF);
    }

    private void ensurePrivateAreasPassword() {
        Long orgId = fitLifeOrgId();
        if (orgId == null) {
            return;
        }
        Integer configured = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM organization_statistics_access
                WHERE organization_id = ?
                  AND password_hash IS NOT NULL
                  AND TRIM(password_hash) <> ''
                """,
                Integer.class,
                orgId);
        if (configured != null && configured > 0) {
            return;
        }
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM organization_statistics_access WHERE organization_id = ?",
                Integer.class,
                orgId);
        if (existing != null && existing > 0) {
            jdbc.update(
                    "UPDATE organization_statistics_access SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE organization_id = ?",
                    PRIVATE_AREA_HASH,
                    orgId);
        } else {
            jdbc.update(
                    "INSERT INTO organization_statistics_access (organization_id, password_hash, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                    orgId,
                    PRIVATE_AREA_HASH);
        }
        log.info("Contraseña de áreas privadas demo configurada para FitLife (12345678)");
    }

    private void ensureFitLifeIndigo() {
        int updated = jdbc.update(
                "UPDATE organizations SET accent_id = 'indigo' WHERE slug = 'fitlife' AND (accent_id IS NULL OR LOWER(accent_id) <> 'indigo')");
        if (updated > 0) {
            log.info("Acento FitLife ajustado a indigo (default GymPlatform)");
        }
    }

    private Long fitLifeOrgId() {
        try {
            return jdbc.query(
                    "SELECT id FROM organizations WHERE slug = 'fitlife'",
                    rs -> rs.next() ? rs.getLong(1) : null);
        } catch (Exception ex) {
            return null;
        }
    }

    private void runScript(String classpath) {
        log.info("Cargando datos demo desde classpath:{}", classpath);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(false);
        populator.setSeparator(";");
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.addScript(new ClassPathResource(classpath));
        populator.execute(dataSource);
        log.info("Datos demo cargados desde {}", classpath);
    }

    private void printHints() {
        System.out.println("=== Datos de prueba (GymPlatform) — fuente: db/demo-seed*.sql ===");
        System.out.println("Platform:       admin@gymplatform.com / admin123");
        System.out.println("Gym Admin:      dueno@fitlife.com / 12345678");
        System.out.println("Recepcionista:  recepcion@fitlife.com / recepcion123");
        System.out.println("Instructor:     instructor@fitlife.com / instructor123");
        System.out.println("Miembro:        miembro@fitlife.com / miembro123");
        System.out.println("  → Rutinas, nutrición, medidas, reservas y citas (demo-seed-member.sql)");
        System.out.println("  → Dueño/recepción en perfil Miembro: demo-seed-member-staff.sql");
        System.out.println("Areas privadas: 12345678  |  Color demo: indigo");
        System.out.println("===============================================================");
    }
}
