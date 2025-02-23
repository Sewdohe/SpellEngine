package net.spell_engine.api.spell;

import net.spell_engine.api.render.LightEmission;
import net.spell_power.api.MagicSchool;
import org.jetbrains.annotations.Nullable;

public class Spell {
    // Structure
    public MagicSchool school;
    public float range = 50;

    public Learn learn = new Learn();
    public static class Learn { public Learn() {}
        public int tier = 1;
        public int level_cost_per_tier = 3;
        public int level_requirement_per_tier = 10;
    }

    public enum Mode { CAST, BYPASS_TO_ITEM_USE }
    public Mode mode = Mode.CAST;

    public Cast cast = new Cast();
    public static class Cast { public Cast() { }
        public boolean haste_affected = true;
        public float duration = 0;
        public int channel_ticks = 0;
        public String animation;
        public Sound start_sound;
        public Sound sound;
        public ParticleBatch[] particles = new ParticleBatch[]{};
    }

    public Release release;
    public static class Release { public Release() { }
        public Target target;
        public boolean custom_impact = false;
        public static class Target { public Target() { }
            public Type type;
            public enum Type {
                AREA, BEAM, CURSOR, PROJECTILE, METEOR, SELF
            }

            public Area area;
            public static class Area { public Area() { }
                public enum DropoffCurve { NONE, SQUARED }
                public DropoffCurve distance_dropoff = DropoffCurve.NONE;
                public float horizontal_range_multiplier = 1F;
                public float vertical_range_multiplier = 1F;
                public float angle_degrees = 0F;
                public boolean include_caster = false;
            }

            public Beam beam;
            public static class Beam { public Beam() { }
                public String texture_id = "textures/entity/beacon_beam.png";
                public long color_rgba = 0xFFFFFFFF;
                public float width = 0.1F;
                public float flow = 1;
                public ParticleBatch[] block_hit_particles = new ParticleBatch[]{};
            }

            public Cursor cursor;
            public static class Cursor { public Cursor() { }
                public boolean use_caster_as_fallback = false;
            }

            public ProjectileData projectile;

            public Meteor meteor;
            public static class Meteor {
                public float launch_height = 10;
            }
        }
        public String animation;
        public ParticleBatch[] particles;
        public Sound sound;
    }

    public boolean allow_mixed_intents = false;
    public Impact[] impact;
    public static class Impact { public Impact() { }
        public Action action;
        public static class Action { public Action() { }
            public Type type;
            public boolean apply_to_caster = false;
            public float min_power = 1;
            public enum Type {
                DAMAGE, HEAL, STATUS_EFFECT, FIRE
            }
            public Damage damage;
            public static class Damage { public Damage() { }
                public float spell_power_coefficient = 1;
                public float knockback = 1;
            }
            public Heal heal;
            public static class Heal { public Heal() { }
                public float spell_power_coefficient = 1;
            }
            public StatusEffect status_effect;
            public static class StatusEffect { public StatusEffect() { }
                public String effect_id;
                public float duration = 10;
                public int amplifier = 0;
                public float amplifier_power_multiplier = 0;
                public ApplyMode apply_mode = ApplyMode.SET;
                public enum ApplyMode { SET, ADD }
                public ApplyLimit apply_limit;
                public static class ApplyLimit { public ApplyLimit() { }
                    public float health_base = 0;
                    public float spell_power_multiplier = 0;
                }
                public boolean show_particles = true;
            }
            public Fire fire;
            public static class Fire { public Fire() { }
                // Entity.java - Notice `% 20` - tick offset is used to avoid instant hits
                // if (this.fireTicks % 20 == 0 && !this.isInLava()) {
                //    this.damage(DamageSource.ON_FIRE, 1.0f);
                // }
                public int duration = 2;
                public int tick_offset = 10;
            }
        }

        public ParticleBatch[] particles = new ParticleBatch[]{};
        public Sound sound;
    }

    public Cost cost = new Cost();
    public static class Cost { public Cost() { }
        public float exhaust = 0.1F;
        public String item_id;
        public String effect_id;
        public int durability = 1;
        public float cooldown_duration = 0;
        public boolean cooldown_proportional = false;
        public boolean cooldown_haste_affected = true;
    }

    public static class ProjectileData { public ProjectileData() { }
        public float velocity = 1F;
        public float divergence = 0;
        public boolean inherit_shooter_velocity = false;
        public float homing_angle = 1F;

        @Nullable
        public AreaImpact area_impact;
        public static class AreaImpact { public AreaImpact() { }
            public float radius = 1F;
            public Release.Target.Area area = new Release.Target.Area();
            public ParticleBatch[] particles = new ParticleBatch[]{};
            @Nullable
            public Sound sound;
        }

        public Perks perks = new Perks();
        public static class Perks { Perks() { }
            /// How many entities projectile can ricochet to
            public int ricochet = 0;
            /// How far ricochet can look for a target
            public float ricochet_range = 5;
            /// How many times projectile can bounce off a wall
            public int bounce = 0;
            /// Whether ricochet and bounce should be decremented together
            public boolean bounce_ricochet_sync = true;
            /// How many entities projectile can go through
            public int pierce = 0;
            /// How many additional projectiles are spawned on impact
            public int chain_reaction_size = 0;
            /// How many generation of chain reaction projectiles are spawned
            public int chain_reaction_triggers = 1;
            /// How many more projectiles are spawned from chain reaction of a spawned projectile
            public int chain_reaction_increment = -1;
            /// How many additional projectiles are spawned after launch
            public int extra_launch_count = 0;
            /// How many ticks after launch additional projectiles are spawned
            public int extra_launch_delay = 2;

            public Perks copy() {
                Perks copy = new Perks();
                copy.ricochet = this.ricochet;
                copy.ricochet_range = this.ricochet_range;
                copy.bounce = this.bounce;
                copy.bounce_ricochet_sync = this.bounce_ricochet_sync;
                copy.pierce = this.pierce;
                copy.chain_reaction_size = this.chain_reaction_size;
                copy.chain_reaction_triggers = this.chain_reaction_triggers;
                copy.chain_reaction_increment = this.chain_reaction_increment;
                copy.extra_launch_count = this.extra_launch_count;
                copy.extra_launch_delay = this.extra_launch_delay;
                return copy;
            }
        }

        public Client client_data;
        public static class Client { public Client() { }
            public ParticleBatch[] travel_particles;
            public String model_id;
            public LightEmission light_emission = LightEmission.GLOW;
            public float scale = 1F;
            public float rotate_degrees_per_tick = 2F;
            public RenderMode render = RenderMode.FLAT;
            public enum RenderMode {
                FLAT, DEEP
            }
            public Client(ParticleBatch[] travel_particles, String model_id) {
                this.travel_particles = travel_particles;
                this.model_id = model_id;
            }
        }
    }

    public ItemUse item_use = new ItemUse();
    public static class ItemUse { public ItemUse() { }
        public boolean shows_item_as_icon = false;
        @Nullable public Arrow arrow_perks;
        public static class Arrow { public Arrow() { }
            public float velocity_multiplier = 1F;
        }
    }
}
