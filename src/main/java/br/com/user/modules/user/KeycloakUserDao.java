package br.com.user.modules.user;

import br.com.user.modules.user.dto.KeycloakUserDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class KeycloakUserDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public KeycloakUserDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<KeycloakUserDto> findByRealmAndUsername(String realmName, String username) {
        String sql = """
                 SELECT
                     u.ID,
                     u.FIRST_NAME,
                     u.LAST_NAME,
                     u.EMAIL,
                     u.EMAIL_VERIFIED,
                     u.USERNAME,
                     m.FAMILY_ID,
                     f.NAME,
                     f.HOLDER_ID
                 FROM USER_ENTITY u
                 INNER JOIN REALM r ON u.REALM_ID = r.ID
                 LEFT  JOIN FAMILY_MEMBER m ON u.ID = m.USER_ID
                 LEFT  JOIN FAMILY_ENTITY f ON m.FAMILY_ID = f.ID
                 WHERE r.NAME = :realmName AND u.USERNAME = :username
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("realmName", realmName)
                .addValue("username", username);

        List<KeycloakUserDto> results = jdbcTemplate.query(sql, params, new KeycloakUserRowMapper());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    public boolean existsByEmail(String email) {
        String sql = """
                 SELECT COUNT(*)
                 FROM USER_ENTITY u
                 WHERE u.EMAIL = :email
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("email", email);

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public List<KeycloakUserDto> findByFamilyId(long familyId) {
        String sql = """
                 SELECT
                     u.ID,
                     u.FIRST_NAME,
                     u.LAST_NAME,
                     u.EMAIL,
                     u.EMAIL_VERIFIED,
                     u.USERNAME,
                     m.FAMILY_ID,
                     f.NAME,
                     f.HOLDER_ID
                 FROM USER_ENTITY u
                 INNER JOIN FAMILY_MEMBER m ON u.ID = m.USER_ID
                 INNER JOIN FAMILY_ENTITY f ON m.FAMILY_ID = f.ID
                 WHERE m.FAMILY_ID = :familyId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("familyId", familyId);

        return jdbcTemplate.query(sql, params, new KeycloakUserRowMapper());
    }

    public List<KeycloakUserDto> findUsersWithoutFamily(String realmName) {
        String sql = """
                 SELECT
                     u.ID,
                     u.FIRST_NAME,
                     u.LAST_NAME,
                     u.EMAIL,
                     u.EMAIL_VERIFIED,
                     u.USERNAME,
                     NULL as FAMILY_ID,
                     NULL as NAME,
                     NULL as HOLDER_ID
                 FROM USER_ENTITY u
                 INNER JOIN REALM r ON u.REALM_ID = r.ID
                 LEFT JOIN FAMILY_MEMBER m ON u.ID = m.USER_ID
                 WHERE r.NAME = :realmName
                   AND m.USER_ID IS NULL
                   AND u.SERVICE_ACCOUNT_CLIENT_LINK IS NULL
                 ORDER BY u.USERNAME
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("realmName", realmName);

        return jdbcTemplate.query(sql, params, new KeycloakUserRowMapper());
    }

    private static class KeycloakUserRowMapper implements RowMapper<KeycloakUserDto> {
        @Override
        public KeycloakUserDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new KeycloakUserDto(
                    rs.getString("ID"),
                    rs.getString("USERNAME"),
                    rs.getString("FIRST_NAME"),
                    rs.getString("LAST_NAME"),
                    rs.getString("EMAIL"),
                    rs.getBoolean("EMAIL_VERIFIED"),
                    rs.getObject("FAMILY_ID", Long.class),
                    rs.getString("NAME"),
                    rs.getString("ID").equals(rs.getString("HOLDER_ID"))
            );
        }
    }
}

