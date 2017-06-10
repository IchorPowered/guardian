/*
 * MIT License
 *
 * Copyright (c) 2017 Connor Hartley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.connorhartley.guardian;

import com.google.common.base.Preconditions;
import io.github.connorhartley.guardian.detection.punishment.Punishment;
import io.github.connorhartley.guardian.sequence.SequenceResult;
import io.github.connorhartley.guardian.storage.StorageProvider;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import tech.ferus.util.sql.api.Database;
import tech.ferus.util.sql.core.BasicSql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GuardianDatabase implements StorageProvider<Database> {

    private static final String[] databaseTableNames = { "GUARDIAN_PUNISHMENT", "GUARDIAN_LOCATION" };

    private final Guardian plugin;
    private final Database database;

    public GuardianDatabase(Guardian plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public void create() {
        this.createTables();

        // Older versions are priority to list for migration.

        BasicSql.query(
                this.database,
                "SELECT ID FROM GUARDIAN WHERE GUARDIAN.DATABASE_VERSION = ?",
                s -> s.setInt(1, Integer.valueOf(this.plugin.getGlobalConfiguration().configDatabaseCredentials.getValue().get("version"))),
                h -> {
                    if (!h.next()) {
                        BasicSql.execute(
                                this.database,
                                StringUtils.join(
                                "INSERT INTO GUARDIAN (",
                                "DATABASE_VERSION, ",
                                "SYNCHRONIZE_TIME, ",
                                "PUNISHMENT_TABLE, ",
                                "LOCATION_TABLE) ",
                                "VALUES (?, ?, ?, ?)"
                                ),
                                s -> {
                                    s.setInt(1, Integer.valueOf(PluginInfo.DATABASE_VERSION));
                                    s.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                                    s.setString(3, databaseTableNames[0]);
                                    s.setString(4, databaseTableNames[1]);
                                }
                        );
                    }
                }
        );
    }

    @Override
    public void load() {
        this.createTables();

        BasicSql.query(this.database,
                "SELECT ID FROM GUARDIAN WHERE GUARDIAN.DATABASE_VERSION = ?",
                s -> s.setInt(1, Integer.valueOf(PluginInfo.DATABASE_VERSION)),
                h -> {
                    while (h.next()) {
                        BasicSql.execute(this.database,
                                "UPDATE GUARDIAN SET SYNCHRONIZE_TIME = ? WHERE ID = ?",
                                s -> {
                                    s.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                                    s.setInt(2, h.getInt("ID"));
                                }
                        );
                    }
                }
        );
    }

    @Override
    public void update() {
        this.createTables();

        BasicSql.query(this.database,
                "SELECT ID FROM GUARDIAN WHERE GUARDIAN.DATABASE_VERSION = ?",
                s -> s.setInt(1, Integer.valueOf(PluginInfo.DATABASE_VERSION)),
                h -> {
                    if (h.next()) {
                        BasicSql.execute(this.database,
                                "UPDATE GUARDIAN SET SYNCHRONIZE_TIME = ? WHERE ID = ?",
                                s -> {
                                    s.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                                    s.setInt(2, h.getInt("ID"));
                                }
                        );
                    } else {
                        BasicSql.execute(this.database,
                                StringUtils.join(
                                        "INSERT INTO GUARDIAN (",
                                        "DATABASE_VERSION, ",
                                        "SYNCHRONIZE_TIME, ",
                                        "PUNISHMENT_TABLE, ",
                                        "LOCATION_TABLE, ",
                                        "PLAYER_TABLE) ",
                                        "VALUES (?, ?, ?, ?, ?)"
                                ),
                                s -> {
                                    s.setInt(1, Integer.valueOf(PluginInfo.DATABASE_VERSION));
                                    s.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                                    s.setString(3, databaseTableNames[0]);
                                    s.setString(4, databaseTableNames[1]);
                                    s.setString(5, databaseTableNames[2]);
                                }
                        );
                    }
                }
        );
    }

    private void createTables() {
        BasicSql.execute(this.database,
                StringUtils.join("CREATE TABLE IF NOT EXISTS GUARDIAN (",
                        "ID integer AUTO_INCREMENT, ",
                        "DATABASE_VERSION integer NOT NULL, ",
                        "SYNCHRONIZE_TIME timestamp NOT NULL, ",
                        "PUNISHMENT_TABLE varchar(24) NOT NULL, ",
                        "LOCATION_TABLE varchar(24) NOT NULL, ",
                        "PRIMARY KEY(ID) )"
                )
        );

        BasicSql.execute(this.database,
                StringUtils.join("CREATE TABLE IF NOT EXISTS " + databaseTableNames[0] + " (",
                        "ID integer AUTO_INCREMENT, ",
                        "DATABASE_VERSION integer NOT NULL, ",
                        "PLAYER_UUID varchar(36) NOT NULL, ",
                        "PUNISHMENT_COUNT integer NOT NULL, ",
                        "PUNISHMENT_TYPE varchar(64) NOT NULL, ",
                        "PUNISHMENT_REASON varchar(1024) NOT NULL, ",
                        "PUNISHMENT_TIME timestamp NOT NULL, ",
                        "PUNISHMENT_PROBABILITY double precision NOT NULL, ",
                        "FOREIGN KEY(DATABASE_VERSION) REFERENCES GUARDIAN(DATABASE_VERSION), ",
                        "PRIMARY KEY(ID) )"
                )
        );

        BasicSql.execute(
                this.database,
                StringUtils.join("CREATE TABLE IF NOT EXISTS " + databaseTableNames[1] + " (",
                        "ID integer AUTO_INCREMENT, ",
                        "PUNISHMENT_ID integer NOT NULL, ",
                        "DATABASE_VERSION integer NOT NULL, ",
                        "WORLD_UUID varchar(36) NOT NULL, ",
                        "X double precision NOT NULL, ",
                        "Y double precision NOT NULL, ",
                        "Z double precision NOT NULL, ",
                        "FOREIGN KEY(DATABASE_VERSION) REFERENCES GUARDIAN(DATABASE_VERSION), ",
                        "FOREIGN KEY(PUNISHMENT_ID) REFERENCES " + databaseTableNames[0] + "(ID), ",
                        "PRIMARY KEY(ID) )"
                )
        );
    }

    public Set<Punishment> getPunishmentsByProperties(@Nullable Integer databaseVersion,
                                                      @Nullable User user,
                                                      @Nullable String type) {
        final Set<Punishment> punishments = new HashSet<>();

        String[] filters = {
                databaseVersion != null ? "DATABASE_VERSION = ? " : "",
                user != null ? "PLAYER_UUID = ? " : "",
                type != null ? "PUNISHMENT_TYPE = ? " : ""
        };

        BasicSql.query(this.database,
                "SELECT * FROM " + databaseTableNames[0] + " WHERE " + StringUtils.join(filters),
                s -> {
                    for (int i = 0; i < 3; i++) {
                        if (databaseVersion != null) {
                            s.setInt(i + 1, databaseVersion);
                            continue;
                        }

                        if (user != null) {
                            s.setString(i + 1, user.getUniqueId().toString());
                            continue;
                        }

                        if (type != null) {
                            s.setString(i + 1, type);
                            break;
                        }
                    }
                },
                h -> {
                    while (h.next()) {
                        punishments.add(
                                Punishment.builder()
                                        .reason(h.getString("PUNISHMENT_TYPE"))
                                        .report(SequenceResult.builder()
                                                .information(h.getString("PUNISHMENT_REASON"))
                                                .type(h.getString("PUNISHMENT_TYPE"))
                                                .build(true))
                                        .time(h.getTimestamp("PUNISHMENT_TIME").toLocalDateTime())
                                        .probability(h.getDouble("PUNISHMENT_PROBABILITY"))
                                        .build()
                        );
                    }
                });

        return punishments;
    }

    public Set<Integer> getPunishmentIdByProperties(@Nullable Integer databaseVersion,
                                                      @Nullable User user,
                                                      @Nullable String type) {
        final Set<Integer> punishments = new HashSet<>();

        String[] filters = {
                databaseVersion != null ? "DATABASE_VERSION = ? " : "",
                user != null ? "PLAYER_UUID = ? " : "",
                type != null ? "PUNISHMENT_TYPE = ? " : ""
        };

        BasicSql.query(this.database,
                "SELECT * FROM " + databaseTableNames[0] + " WHERE " + StringUtils.join(filters),
                s -> {
                    for (int i = 0; i < 3; i++) {
                        if (databaseVersion != null) {
                            s.setInt(i + 1, databaseVersion);
                            continue;
                        }

                        if (user != null) {
                            s.setString(i + 1, user.getUniqueId().toString());
                            continue;
                        }

                        if (type != null) {
                            s.setString(i + 1, type);
                            break;
                        }
                    }
                },
                h -> {
                    while (h.next()) {
                        punishments.add(h.getInt("ID"));
                    }
                });

        return punishments;
    }

    public Optional<Punishment> getPunishmentById(int id) {
        return BasicSql.returnQuery(this.database,
                StringUtils.join(
                        "SELECT * FROM ",
                        databaseTableNames[0],
                        " WHERE ID = ?"
                ),
                s -> s.setInt(1, id),
                r -> Punishment.builder()
                        .reason(r.getString("PUNISHMENT_TYPE"))
                        .report(SequenceResult.builder()
                                .information(r.getString("PUNISHMENT_REASON"))
                                .type(r.getString("PUNISHMENT_TYPE"))
                                .build(true))
                        .time(r.getTimestamp("PUNISHMENT_TIME").toLocalDateTime())
                        .probability(r.getDouble("PUNISHMENT_PROBABILITY"))
                        .build()
        );
    }

    public Optional<Integer> getPunishmentCountById(int id) {
        return BasicSql.returnQuery(this.database,
                StringUtils.join(
                        "SELECT PUNISHMENT_COUNT FROM ",
                        databaseTableNames[0],
                        " WHERE ID = ?"
                ),
                s -> s.setInt(1, id),
                r -> r.getInt("PUNISHMENT_COUNT")
        );
    }

    public Set<Location<World>> getPunishmentLocationsById(int id) {
        final Set<Location<World>> locations = new HashSet<>();

        BasicSql.query(this.database,
                StringUtils.join(
                        "SELECT WORLD_UUID, X, Y, Z FROM ",
                        databaseTableNames[1],
                        " WHERE ",
                        databaseTableNames[1],
                        ".PUNISHMENT_ID = ?"
                ),
                s -> s.setInt(1, id),
                h -> {
                    while (h.next()) {
                        final Optional<World> world = Sponge.getServer().getWorld(UUID.fromString(h.getString("WORLD_UUID")));

                        if (world.isPresent()) {
                            locations.add(world.get().getLocation(h.getDouble("X"), h.getDouble("Y"), h.getDouble("Z")));
                        }
                    }
                }
        );

        return locations;
    }

    public void createPunishment(@Nonnull User user, @Nonnull Punishment punishment) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(punishment);

        BasicSql.query(this.database,
                StringUtils.join(
                        "INSERT INTO ",
                        databaseTableNames[0],
                        " (",
                        "DATABASE_VERSION, ",
                        "PLAYER_UUID, ",
                        "PUNISHMENT_COUNT, ",
                        "PUNISHMENT_TYPE, ",
                        "PUNISHMENT_REASON, ",
                        "PUNISHMENT_TIME, ",
                        "PUNISHMENT_PROBABILITY) ",
                        "VALUES (?, ?, ?, ?, ?, ?, ?)"
                ),
                s -> {
                    s.setInt(1, Integer.valueOf(PluginInfo.DATABASE_VERSION));
                    s.setString(2, user.getUniqueId().toString());
                    s.setInt(3, 0);
                    s.setString(4, punishment.getDetectionReason());
                    s.setString(5, StringUtils.join(punishment.getSequenceResult().getInformation(), ", "));
                    s.setTimestamp(6, Timestamp.valueOf(punishment.getLocalDateTime()));
                    s.setDouble(7, punishment.getProbability());
                },
                h -> {
                    BasicSql.execute(this.database,
                            StringUtils.join(
                                    "INSERTS INTO ",
                                    databaseTableNames[1],
                                    " (",
                                    "PUNISHMENT_ID, ",
                                    "DATABASE_VERSION, ",
                                    "WORLD_UUID, ",
                                    "X, ",
                                    "Y, ",
                                    "Z) ",
                                    "VALUES (?, ?, ?, ?, ?, ?)"
                            ),
                            s -> {
                                s.setInt(1, h.getInt("ID"));
                                s.setInt(2, Integer.valueOf(PluginInfo.DATABASE_VERSION));
                                s.setString(3, punishment.getSequenceResult().getInitialLocation()
                                        .get().getExtent().getUniqueId().toString());
                                s.setDouble(4, punishment.getSequenceResult().getInitialLocation()
                                        .get().getX());
                                s.setDouble(5, punishment.getSequenceResult().getInitialLocation()
                                        .get().getY());
                                s.setDouble(6, punishment.getSequenceResult().getInitialLocation()
                                        .get().getZ());
                            }
                    );
                }
        );
    }

    public void updatePunishment(@Nonnull Integer id, @Nonnull User user, @Nonnull Punishment punishment) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(punishment);

        BasicSql.query(this.database,
                StringUtils.join(
                        "SELECT * FROM ",
                        databaseTableNames[0],
                        " WHERE ID = ?"
                ),
                s -> s.setInt(1, id),
                h -> {
                    if (h.next()) {
                        BasicSql.execute(this.database,
                                StringUtils.join(
                                        "UPDATE ",
                                        databaseTableNames[0],
                                        " SET ",
                                        "PLAYER_UUID = ?, ",
                                        "PUNISHMENT_COUNT = ?",
                                        "PUNISHMENT_TYPE = ?, ",
                                        "PUNISHMENT_REASON = ?, ",
                                        "PUNISHMENT_TIME = ?, ",
                                        "PUNISHMENT_PROBABILITY = ? ",
                                        "WHERE ID = ?"
                                ),
                                s -> {
                                    s.setString(1, user.getUniqueId().toString());
                                    s.setInt(2, h.getInt("PUNISHMENT_COUNT") + 1);
                                    s.setString(3, punishment.getDetectionReason());
                                    s.setString(4, StringUtils.join(punishment.getSequenceResult().getInformation(), ", "));
                                    s.setTimestamp(5, Timestamp.valueOf(punishment.getLocalDateTime()));
                                    s.setDouble(6, punishment.getProbability());
                                    s.setInt(7, h.getInt("ID"));
                                }
                        );
                    }
                }
        );
    }

    @Override
    public Optional<Database> getLocation() {
        return Optional.ofNullable(this.database);
    }
}
