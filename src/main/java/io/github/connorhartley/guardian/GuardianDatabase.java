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

import io.github.connorhartley.guardian.punishment.Punishment;
import io.github.connorhartley.guardian.sequence.SequenceReport;
import io.github.connorhartley.guardian.storage.StorageProvider;
import io.github.connorhartley.guardian.storage.container.DatabaseValue;
import io.github.connorhartley.guardian.storage.container.StorageKey;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import tech.ferus.util.sql.api.Database;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GuardianDatabase implements StorageProvider<Database> {

    private static final Integer databaseVersion = 1;
    private static final String[] databaseTableNames = { "GUARDIAN_PUNISHMENT", "GUARDIAN_LOCATION", "GUARDIAN_PLAYER" };

    private final Guardian plugin;
    private final Database database;

    public DatabaseValue databaseVersionTable;
    public DatabaseValue databasePunishmentTable;
    public DatabaseValue databaseLocationTable;
    public DatabaseValue databasePlayerTable;

    public GuardianDatabase(Guardian plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public void create() {
        this.databaseVersionTable = new DatabaseValue(new StorageKey<>(this.database),
                StringUtils.join("CREATE TABLE IF NOT EXISTS GUARDIAN (",
                        "ID integer AUTO_INCREMENT, ",
                        "DATABASE_VERSION integer NOT NULL, ",
                        "SYNCHRONIZE_TIME timestamp NOT NULL, ",
                        "PUNISHMENT_TABLE varchar(24) NOT NULL, ",
                        "LOCATION_TABLE varchar(24) NOT NULL, ",
                        "PLAYER_TABLE varchar(24) NOT NULL, ",
                        "PRIMARY KEY(ID) )"
                ));

        this.databasePunishmentTable = new DatabaseValue(new StorageKey<>(this.database),
                StringUtils.join("CREATE TABLE IF NOT EXISTS " + databaseTableNames[0] + " (",
                        "ID integer AUTO_INCREMENT, ",
                        "DATABASE_VERSION integer NOT NULL, ",
                        "PLAYER_UUID varchar(36) NOT NULL, ",
                        "PUNISHMENT_TYPE varchar(64) NOT NULL, ",
                        "PUNISHMENT_REASON varchar(1024) NOT NULL, ",
                        "PUNISHMENT_TIME timestamp NOT NULL, ",
                        "PUNISHMENT_PROBABILITY double precision NOT NULL, ",
                        "FOREIGN KEY(DATABASE_VERSION) REFERENCES GUARDIAN(DATABASE_VERSION), ",
                        "PRIMARY KEY(ID) )"
                ));

        this.databaseLocationTable = new DatabaseValue(new StorageKey<>(this.database),
                StringUtils.join("CREATE TABLE IF NOT EXISTS " + databaseTableNames[1] + " (",
                        "PUNISHMENT_ID integer NOT NULL, ",
                        "DATABASE_VERSION integer NOT NULL, ",
                        "PUNISHMENT_ORDINAL integer NOT NULL, ",
                        "WORLD_UUID varchar(36) NOT NULL, ",
                        "X double precision NOT NULL, ",
                        "Y double precision NOT NULL, ",
                        "Z double precision NOT NULL, ",
                        "FOREIGN KEY(DATABASE_VERSION) REFERENCES GUARDIAN(DATABASE_VERSION), ",
                        "PRIMARY KEY(PUNISHMENT_ID, PUNISHMENT_ORDINAL) )"
                ));

        this.databasePlayerTable = new DatabaseValue(new StorageKey<>(this.database),
                StringUtils.join("CREATE TABLE IF NOT EXISTS " + databaseTableNames[2] + " (",
                        "PUNISHMENT_ID integer NOT NULL, ",
                        "DATABASE_VERSION integer NOT NULL, ",
                        "PLAYER_UUID varchar(36) NOT NULL, ",
                        "FOREIGN KEY(DATABASE_VERSION) REFERENCES GUARDIAN(DATABASE_VERSION), ",
                        "FOREIGN KEY(PUNISHMENT_ID) REFERENCES GUARDIAN_PUNISHMENT(ID) )"
                ));

        this.databaseVersionTable.execute();
        this.databasePunishmentTable.execute();
        this.databaseLocationTable.execute();
        this.databasePlayerTable.execute();

        // Older versions are priority to list for migration.

        int currentId = new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                "SELECT * FROM GUARDIAN WHERE GUARDIAN.DATABASE_VERSION = ?"
        )).returnQuery(
                s -> s.setInt(1, Integer.valueOf(this.plugin.getGlobalConfiguration().configDatabaseCredentials.getValue().get("version"))),
                r -> r.getInt("ID")
        ).orElseGet(() -> {
            new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                    "INSERT INTO GUARDIAN (",
                    "DATABASE_VERSION, ",
                    "SYNCHRONIZE_TIME, ",
                    "PUNISHMENT_TABLE, ",
                    "LOCATION_TABLE, ",
                    "PLAYER_TABLE) ",
                    "VALUES (?, ?, ?, ?, ?)"
            )).execute(
                    s -> {
                        s.setInt(1, databaseVersion);
                        s.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                        s.setString(3, databaseTableNames[0]);
                        s.setString(4, databaseTableNames[1]);
                        s.setString(5, databaseTableNames[2]);
                    }
            );

            return databaseVersion;
        });
    }

    @Override
    public void load() {
        this.databaseVersionTable.execute();
        this.databasePunishmentTable.execute();
        this.databaseLocationTable.execute();
        this.databasePlayerTable.execute();

        // Load new version as priority. Fallback to older versions if that is not possible.

        int currentId = new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                "SELECT * FROM GUARDIAN WHERE GUARDIAN.DATABASE_VERSION = ?"
        )).returnQuery(
                s -> s.setInt(1, databaseVersion),
                r -> r.getInt("ID")
        ).orElse(Integer.valueOf(this.plugin.getGlobalConfiguration().configDatabaseCredentials.getValue().get("version")));

        new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
           "UPDATE GUARDIAN SET SYNCHRONIZE_TIME = ? WHERE ID = ?"
        )).execute(
                s -> {
                    s.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                    s.setInt(2, currentId);
                }
        );
    }

    @Override
    public void update() {
        this.databaseVersionTable.execute();
        this.databasePunishmentTable.execute();
        this.databaseLocationTable.execute();
        this.databasePlayerTable.execute();

        // Load new version as priority. Fallback to older versions if that is not possible.

        int currentId = new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                "SELECT * FROM GUARDIAN WHERE GUARDIAN.DATABASE_VERSION = ?"
        )).returnQuery(
                s -> s.setInt(1, databaseVersion),
                r -> r.getInt("ID")
        ).orElseGet(() -> {
            new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                    "INSERT INTO GUARDIAN (",
                    "DATABASE_VERSION, ",
                    "SYNCHRONIZE_TIME, ",
                    "PUNISHMENT_TABLE, ",
                    "LOCATION_TABLE, ",
                    "PLAYER_TABLE) ",
                    "VALUES (?, ?, ?, ?, ?)"
            )).execute(
                    s -> {
                        s.setInt(1, databaseVersion);
                        s.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                        s.setString(3, databaseTableNames[0]);
                        s.setString(4, databaseTableNames[1]);
                        s.setString(5, databaseTableNames[2]);
                    }
            );

            return databaseVersion;
        });

        new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                "UPDATE GUARDIAN SET SYNCHRONIZE_TIME = ? WHERE ID = ?"
        )).execute(
                s -> {
                    s.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                    s.setInt(2, currentId);
                }
        );
    }

    //////////////////////////////////
    //       Database Getters       //
    //////////////////////////////////

    //         Punishments          //

    public Optional<Punishment> getPunishmentById(int id) {
        return new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                "SELECT * FROM ",
                databaseTableNames[0],
                " WHERE ID = ?"
        )).returnQuery(
                s -> s.setInt(1, id),
                r -> Punishment.builder()
                        .reason(r.getString("PUNISHMENT_TYPE"))
                        .report(SequenceReport.builder()
                                .information(r.getString("PUNISHMENT_REASON"))
                                .type(r.getString("PUNISHMENT_TYPE"))
                                .build(true))
                        .time(r.getTimestamp("PUNISHMENT_TIME").toLocalDateTime())
                        .probability(r.getDouble("PUNISHMENT_PROBABILITY"))
                        .build()
        );
    }

    public Set<Location<World>> getPunishmentLocationsById(int id) {
        final Set<Location<World>> locations = new HashSet<>();

        new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                "SELECT WORLD_UUID, X, Y, Z FROM ",
                databaseTableNames[1],
                " WHERE ",
                databaseTableNames[1],
                ".PUNISHMENT_ID = ?"
        )).query(
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

    public Set<UUID> getPunishedPlayersById(int id) {
        final Set<UUID> players = new HashSet<>();

        new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
           "SELECT PLAYER_UUID FROM ",
                databaseTableNames[2],
                " WHERE ",
                databaseTableNames[2],
                ".PUNISHMENT_ID = ?"
        )).query(
                s -> s.setInt(1, id),
                h -> {
                    while (h.next()) {
                        players.add(UUID.fromString(h.getString("PLAYER_UUID")));
                    }
                }
        );

        return players;
    }

    //         Punishments          //

    public void addPunishment(Player player, Punishment punishment) {
        int punishmentId = new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                "INSERT INTO ",
                databaseTableNames[0],
                " (",
                "DATABASE_VERSION, ",
                "PLAYER_UUID, ",
                "PUNISHMENT_TYPE, ",
                "PUNISHMENT_REASON, ",
                "PUNISHMENT_TIME, ",
                "PUNISHMENT_PROBABILITY) ",
                "VALUES (?, ?, ?, ?, ?, ?)"
        )).returnQuery(
                s -> {
                    s.setInt(1, databaseVersion);
                    s.setString(2, player.getUniqueId().toString());
                    s.setString(3, punishment.getDetectionReason());
                    s.setString(4, StringUtils.join(punishment.getSequenceReport().getInformation(), ", "));
                    s.setTimestamp(5, Timestamp.valueOf(punishment.getLocalDateTime()));
                    s.setDouble(6, punishment.getProbability());
                },
                r -> r.getInt("ID")
        ).orElseThrow(Error::new);

        new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                "INSERTS INTO ",
                databaseTableNames[1],
                " (",
                "PUNISHMENT_ID, ",
                "DATABASE_VERSION, ",
                "PUNISHMENT_ORDINAL, ",
                "WORLD_UUID, ",
                "X, ",
                "Y, ",
                "Z) ",
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
        )).execute(
                s -> {
                    s.setInt(1, punishmentId);
                    s.setInt(2, databaseVersion);
                    s.setInt(3, 1); // Needs to be dynamic.
                    s.setString(4, punishment.getSequenceReport().getInitialLocation()
                            .get().getExtent().getUniqueId().toString());
                    s.setDouble(5, punishment.getSequenceReport().getInitialLocation()
                            .get().getX());
                    s.setDouble(6, punishment.getSequenceReport().getInitialLocation()
                            .get().getY());
                    s.setDouble(7, punishment.getSequenceReport().getInitialLocation()
                            .get().getZ());
                }
        );

        new DatabaseValue(new StorageKey<>(this.database), StringUtils.join(
                "INSERTS INTO ",
                databaseTableNames[2],
                " (",
                "PUNISHMENT_ID, ",
                "DATABASE_VERSION, ",
                "PLAYER_UUID, ",
                "VALUES (?, ?, ?)"
        )).execute(
                s -> {
                    s.setInt(1, punishmentId);
                    s.setInt(2, databaseVersion);
                    s.setString(3, player.getUniqueId().toString());
                }
        );
    }

    @Override
    public Optional<Database> getLocation() {
        return Optional.ofNullable(this.database);
    }
}
