package net.spell_engine.client.gui;

import com.ibm.icu.text.DecimalFormat;
import net.fabricmc.fabric.mixin.client.keybinding.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.input.Keybindings;
import net.spell_engine.internals.SpellCasterItemStack;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.SpellRegistry;

import java.util.ArrayList;
import java.util.List;

public class SpellTooltip {
    private static final String damageToken = "{damage}";
    private static final String healToken = "{heal}";
    private static final String rangeToken = "{range}";
    private static final String durationToken = "{duration}";
    private static final String itemToken = "{item}";
    private static final String effectDurationToken = "{effect_duration}";
    private static final String effectAmplifierToken = "{effect_amplifier}";
    private static final String impactRangeToken = "{impact_range}";

    public static void addSpellInfo(ItemStack itemStack, List<Text> lines) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        var config = SpellEngineClient.config;
        if ((Object)itemStack instanceof SpellCasterItemStack stack) {
            var container = stack.getSpellContainer();
            if(container != null && container.isValid()) {
                if (container.is_proxy) {
                    lines.add(Text.translatable("spell.tooltip.host.proxy")
                            .formatted(Formatting.GRAY));
                }

                if (container.spell_ids.size() > 0 || container.pool != null) {
                    if (container.pool == null) {
                        lines.add(Text.translatable(container.is_proxy ? "spell.tooltip.host.additional" : "spell.tooltip.host.pre_loaded")
                                .formatted(Formatting.GRAY));
                    } else {
                        String limit = "";
                        if (container.max_spell_count > 0) {
                            limit = I18n.translate("spell.tooltip.host.limit")
                                    .replace("{current}", "" + container.spell_ids.size())
                                    .replace("{max}", "" + container.max_spell_count);
                        }
                        lines.add(Text.translatable("spell.tooltip.host.bindable")
                                .append(Text.literal(" " + limit))
                                .formatted(Formatting.GRAY));
                    }
                }
                var keybinding = Keybindings.bypass_spell_hotbar;
                var showDetails = config.alwaysShowFullTooltip
                        || (!keybinding.isUnbound() && InputUtil.isKeyPressed(
                                MinecraftClient.getInstance().getWindow().getHandle(),
                                ((KeyBindingAccessor) keybinding).fabric_getBoundKey().getCode())
                        );
                for (int i = 0; i < container.spell_ids.size(); i++) {
                    var spellId = new Identifier(container.spell_ids.get(i));
                    var info = spellInfo(spellId, player, itemStack, showDetails);
                    if (!info.isEmpty()) {
                        if (i > 0 && showDetails) {
                            lines.add(Text.literal(" ")); // Separator: empty line
                        }
                        lines.addAll(info);
                    }
                }
                if (!showDetails) {
                    if (!keybinding.isUnbound() && container.spell_ids.size() > 0) {
                        lines.add(Text.translatable("spell.tooltip.hold_for_details",
                                        keybinding.getBoundKeyLocalizedText())
                                .formatted(Formatting.GRAY));
                    }
                    if (config.showSpellBindingTooltip && container.pool != null) {
                        lines.add(Text.translatable("spell.tooltip.spell_binding_tip")
                                .formatted(Formatting.GRAY));
                    }
                }
            }
        }
    }

    public static List<Text> spellInfo(Identifier spellId, PlayerEntity player, ItemStack itemStack, boolean details) {
        var lines = new ArrayList<Text>();
        var spell = SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return lines;
        }

        lines.add(Text.translatable(spellTranslationKey(spellId))
                .formatted(Formatting.BOLD)
                .formatted(Formatting.GRAY));

        if(!details) {
            return lines;
        }

        var description = I18n.translate(spellKeyPrefix(spellId) + ".description");
        var estimatedOutput = SpellHelper.estimate(spell, player, itemStack);

        var projectile = spell.release.target.projectile;
        if (projectile != null) {
            var area_impact = projectile.area_impact;
            if (area_impact != null) {
                description = description.replace(impactRangeToken, formattedNumber(area_impact.radius));
            }
            var extra_launch_count = projectile.perks.extra_launch_count;
            if (extra_launch_count > 0) {
                description = description.replace("{extra_launch}", formattedNumber(extra_launch_count));
            }
            if (projectile.perks.ricochet > 0) {
                description = description.replace("{ricochet}", formattedNumber(projectile.perks.ricochet));
            }
            if (projectile.perks.bounce > 0) {
                description = description.replace("{bounce}", formattedNumber(projectile.perks.bounce));
            }
            if (projectile.perks.pierce > 0) {
                description = description.replace("{pierce}", formattedNumber(projectile.perks.pierce));
            }
            if (projectile.perks.chain_reaction_size > 0) {
                description = description.replace("{chain_reaction_size}", formattedNumber(projectile.perks.chain_reaction_size));
            }
        }

        for (var impact: spell.impact) {
            switch (impact.action.type) {
                case DAMAGE -> {
                    description = replaceTokens(description, damageToken, estimatedOutput.damage());
                }
                case HEAL -> {
                    description = replaceTokens(description, healToken, estimatedOutput.heal());
                }
                case STATUS_EFFECT -> {
                    var statusEffect = impact.action.status_effect;
                    description = description.replace(effectAmplifierToken, "" + (statusEffect.amplifier + 1));
                    description = description.replace(effectDurationToken, formattedNumber(statusEffect.duration));
                }
            }
        }
        lines.add(Text.literal(" ")
                .append(Text.translatable(description))
                .formatted(Formatting.GRAY));

        if (SpellHelper.isInstant(spell)) {
            lines.add(Text.literal(" ")
                    .append(Text.translatable("spell.tooltip.cast_instant"))
                    .formatted(Formatting.GOLD));
        } else {
            var castDuration = SpellHelper.getCastDuration(player, spell, itemStack);
            var castTimeKey = keyWithPlural("spell.tooltip.cast_time", castDuration);
            var castTime = I18n.translate(castTimeKey).replace(durationToken, formattedNumber(castDuration));
            lines.add(Text.literal(" ")
                    .append(Text.literal(castTime))
                    .formatted(Formatting.GOLD));
        }


        if (spell.range > 0) {
            var rangeKey = keyWithPlural("spell.tooltip.range", spell.range);
            var range = I18n.translate(rangeKey).replace(rangeToken, formattedNumber(spell.range));
            lines.add(Text.literal(" ")
                    .append(Text.literal(range))
                    .formatted(Formatting.GOLD));
        }

        var cooldownDuration = SpellHelper.getCooldownDuration(player, spell, itemStack);
        if (cooldownDuration > 0) {
            String cooldown;
            if (spell.cost.cooldown_proportional) {
                cooldown = I18n.translate("spell.tooltip.cooldown.proportional");
            } else {
                var cooldownKey = keyWithPlural("spell.tooltip.cooldown", cooldownDuration);
                cooldown = I18n.translate(cooldownKey).replace(durationToken, formattedNumber(cooldownDuration));
            }
            lines.add(Text.literal(" ")
                    .append(Text.literal(cooldown))
                    .formatted(Formatting.GOLD));
        }

        var showItemCost = true;
        var config = SpellEngineMod.config;
        if (config != null) {
            showItemCost = config.spell_cost_item_allowed;
        }
        if (showItemCost && spell.cost != null && spell.cost.item_id != null && !spell.cost.item_id.isEmpty()) {
            var item = Registries.ITEM.get(new Identifier(spell.cost.item_id));
            if (item != Items.AIR) {
                var ammoKey = keyWithPlural("spell.tooltip.ammo", 1); // Add variable ammo count later
                var itemName = I18n.translate(item.getTranslationKey());
                var ammo = I18n.translate(ammoKey).replace(itemToken, itemName);
                var hasItem = SpellHelper.ammoForSpell(player, spell, itemStack).satisfied();
                lines.add(Text.literal(" ")
                        .append(Text.literal(ammo).formatted(hasItem ? Formatting.GREEN : Formatting.RED)));
            }
        }

        return lines;
    }

    private static String replaceTokens(String text, String token, List<SpellHelper.EstimatedValue> values) {
        for (int i = 0; i < values.size(); ++i) {
            var range = values.get(i);
            boolean indexedTokens = values.size() > 1;
            token = indexedTokens ? (token + "_" + i) : token;
            text = text.replace(token, formattedRange(range.min(), range.max()));
        }
        return text;
    }

    private static String formattedRange(double min, double max) {
        if (min == max) {
            return formattedNumber((float) min);
        }
        return formattedNumber((float) min) + " - " + formattedNumber((float) max);
    }

    private static String formattedNumber(float number) {
        DecimalFormat formatter = new DecimalFormat();
        formatter.setMaximumFractionDigits(1);
        return formatter.format(number);
    }

    private static String keyWithPlural(String key, float value) {
        if (value != 1) {
            return key + ".plural";
        }
        return key;
    }

    public static String spellTranslationKey(Identifier spellId) {
        return spellKeyPrefix(spellId) + ".name";
    }

    public static String spellKeyPrefix(Identifier spellId) {
        // For example: `spell.spell_engine.fireball`
        return "spell." + spellId.getNamespace() + "." + spellId.getPath();
    }
}
