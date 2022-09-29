package net.combatspells.network;

import com.google.gson.Gson;
import net.combatspells.CombatSpells;
import net.combatspells.api.Spell;
import net.combatspells.config.ServerConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class Packets {
    public record ParticleBatches(Vec3d origin, Spell.ParticleBatch[] batches) {
        public static Identifier ID = new Identifier(CombatSpells.MOD_ID, "particle_effects");
        public PacketByteBuf write() {
            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeDouble(origin.x);
            buffer.writeDouble(origin.y);
            buffer.writeDouble(origin.z);
            buffer.writeInt(batches.length);
            for(var batch: batches) {
                buffer.writeString(batch.particle_id);
                buffer.writeInt(batch.shape.ordinal());
                buffer.writeInt(batch.count);
                buffer.writeFloat(batch.min_speed);
                buffer.writeFloat(batch.max_speed);
            }
            return buffer;
        }

        public static ParticleBatches read(PacketByteBuf buffer) {
            var x = buffer.readDouble();
            var y = buffer.readDouble();
            var z = buffer.readDouble();
            var batchCount = buffer.readInt();
            var batches = new ArrayList<Spell.ParticleBatch>();
            for (int i = 0; i < batchCount; ++i) {
                var batch = new Spell.ParticleBatch(
                        buffer.readString(),
                        Spell.ParticleBatch.Shape.values()[buffer.readInt()],
                        buffer.readInt(),
                        buffer.readFloat(),
                        buffer.readFloat()
                );
                batches.add(batch);
            }
            Spell.ParticleBatch[] array = new Spell.ParticleBatch[batches.size()];
            array = batches.toArray(array);
            return new ParticleBatches(new Vec3d(x, y, z), array);
        }
    }

    public static class ConfigSync {
        public static Identifier ID = new Identifier(CombatSpells.MOD_ID, "config_sync");

        public static PacketByteBuf write(ServerConfig config) {
            var gson = new Gson();
            var json = gson.toJson(config);
            var buffer = PacketByteBufs.create();
            buffer.writeString(json);
            return buffer;
        }

        public static ServerConfig read(PacketByteBuf buffer) {
            var gson = new Gson();
            var json = buffer.readString();
            var config = gson.fromJson(json, ServerConfig.class);
            return config;
        }
    }
}
