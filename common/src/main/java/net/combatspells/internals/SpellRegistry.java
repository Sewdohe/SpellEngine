package net.combatspells.internals;

import net.combatspells.api.Spell;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class SpellRegistry {
    public static final Map<Identifier, Spell> spells = new HashMap();

    public static void initialize() {
        // spells.put(new Identifier("minecraft", "wooden_sword"), "fireball");

        var fireBall = new Spell();
        fireBall.cast_duration = 2;
        fireBall.range = 64;
        fireBall.on_release = new Spell.Release();
        fireBall.on_release.action = Spell.Release.Action.SHOOT_PROJECTILE;
        fireBall.on_release.projectile = new Spell.ProjectileData();
        fireBall.on_release.projectile.client_data = new Spell.ProjectileData.Client(
                new Spell.ParticleBatch[] {
                        new Spell.ParticleBatch("flame", Spell.ParticleBatch.Shape.CIRCLE, 3, 0, 0.1F)
                },
                new Spell.ParticleBatch[] {
                        new Spell.ParticleBatch("lava", Spell.ParticleBatch.Shape.CIRCLE, 30, 0.5F, 3F)
                },
                "fire_charge"
        );

        spells.put(new Identifier("minecraft", "wooden_sword"), fireBall);
    }
}
