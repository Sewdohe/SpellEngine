package net.spell_engine.internals;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellContainer;
import net.spell_engine.api.spell.SpellPool;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpellContainerHelper {
    public static Identifier getPoolId(SpellContainer container) {
        if (container != null && container.pool != null) {
            return new Identifier(container.pool);
        }
        return null;
    }

    public static SpellPool getPool(SpellContainer container) {
        if (container != null && container.pool != null) {
            var id = new Identifier(container.pool);
            return SpellRegistry.spellPool(id);
        }
        return SpellPool.empty;
    }

    public static SpellContainer containerWithProxy(ItemStack itemStack, PlayerEntity player) {
        var weaponContainer = containerFromItemStack(itemStack);
        return containerWithProxy(weaponContainer, player);
    }

    public static SpellContainer containerWithProxy(SpellContainer proxyContainer, PlayerEntity player) {
        var component = TrinketsApi.getTrinketComponent(player);
        if (proxyContainer!= null && proxyContainer.is_proxy && component.isPresent()) {
            var trinketComponent = component.get();
            var spellBookSlot = trinketComponent.getInventory().get("charm").get("spell_book");
            // Up casting to `Inventory` so dependent mod workspace don't crash on mapping error (dev env only)
            var spellBookStack = ((Inventory)spellBookSlot).getStack(0);
            if (!spellBookStack.isEmpty()) {
                var spellBookContainer = containerFromItemStack(spellBookStack);
                if (spellBookContainer != null) {
                    var mergedSpellIds = Stream.of(proxyContainer.spell_ids, spellBookContainer.spell_ids)
                            .flatMap(Collection::stream)
                            .distinct()
                            .toList();

                    return new SpellContainer(false, null, 0, mergedSpellIds);
                }
            }
        }
        return proxyContainer;
    }

    public static SpellContainer containerFromItemStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }
        var object = (Object)itemStack;
        if (object instanceof SpellCasterItemStack stack) {
            var container = stack.getSpellContainer();
            if (container != null && container.isValid()) {
                return container;
            }
        }
        return null;
    }

    public static void addContainerToItemStack(SpellContainer container, ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }
        var nbt = itemStack.getOrCreateNbt();
        var nbtContainer = SpellContainerHelper.toNBT(container);
        nbt.put(SpellContainerHelper.NBT_KEY_CONTAINER, nbtContainer);
    }

    @Nullable
    public static Identifier spellId(SpellContainer container, int selectedIndex) {
        if (container == null || !container.isUsable()) {
            return null;
        }
        return new Identifier(container.spellId(selectedIndex));
    }

    public static SpellContainer addSpell(Identifier spellId, SpellContainer container) {
        var spellIds = new ArrayList<String>(container.spell_ids);
        spellIds.add(spellId.toString());

        // Creating a map just for the sake of sorting
        HashMap<Identifier, Spell> spells = new HashMap<>();
        for(var idString: spellIds) {
            var id = new Identifier(idString);
            var spell = SpellRegistry.getSpell(id);
            if (spell != null) {
                spells.put(id, spell);
            }
        }
        var sortedSpellIds = spells.entrySet().stream()
                .sorted(SpellContainerHelper.spellSorter)
                .map(entry -> entry.getKey().toString())
                .collect(Collectors.toList());

        var newContainer = container.copy();
        newContainer.spell_ids = sortedSpellIds;

        return newContainer;
    }

    public static void addSpell(Identifier spellId, ItemStack itemStack) {
        var container = containerFromItemStack(itemStack);
        if (container == null || !container.isValid()) {
            System.err.println("Trying to add spell: " + spellId  + " to an ItemStack without valid spell container");
            return;
        }
        var modifiedContainer = addSpell(spellId, container);
        var nbtContainer = SpellContainerHelper.toNBT(modifiedContainer);
        var nbt = itemStack.getOrCreateNbt();
        nbt.put(SpellContainerHelper.NBT_KEY_CONTAINER, nbtContainer);
    }

    public static final Comparator<Map.Entry<Identifier, Spell>> spellSorter = (spell1, spell2) -> {
        if (spell1.getValue().learn.tier > spell2.getValue().learn.tier) {
            return 1;
        }  else if (spell1.getValue().learn.tier < spell2.getValue().learn.tier) {
            return -1;
        } else {
            return spell1.getKey().toString().compareTo(spell2.getKey().toString());
        }
    };

    public static boolean hasValidContainer(ItemStack itemStack) {
        return containerFromItemStack(itemStack) != null;
    }

    public static boolean hasBindableContainer(ItemStack itemStack) {
        var container = containerFromItemStack(itemStack);
        return container != null && container.pool != null && !container.pool.isEmpty();
    }

    public static boolean hasUsableContainer(ItemStack itemStack) {
        var container = containerFromItemStack(itemStack);
        return container != null && (container.isUsable() || container.is_proxy);
    }

    // MARK: NBT Codec

    public static final String NBT_KEY_CONTAINER = "spell_container";
    private static final String NBT_KEY_PROXY = "is_proxy";
    private static final String NBT_KEY_POOL = "pool";
    private static final String NBT_KEY_MAX_SPELL_COUNT = "max_spell_count";
    private static final String NBT_KEY_SPELL_IDS = "spell_ids";
    public static NbtCompound toNBT(SpellContainer container) {
        var object = new NbtCompound();
        if (container.is_proxy) {
            object.putBoolean(NBT_KEY_PROXY, container.is_proxy);
        }
        if (container.pool != null) {
            object.putString(NBT_KEY_POOL, container.pool);
        }
        if (container.max_spell_count != 0) {
            object.putInt(NBT_KEY_MAX_SPELL_COUNT, container.max_spell_count);
        }
        if (container.spell_ids.size() > 0) {
            var spellList = new NbtList();
            for (var spellId: container.spell_ids) {
                var element = NbtString.of(spellId);
                spellList.add(element);
            }
            object.put(NBT_KEY_SPELL_IDS, spellList);
        }
        return object;
    }

    public static SpellContainer fromNBT(NbtCompound nbt) {
        var nbtContainer = nbt.getCompound(NBT_KEY_CONTAINER);
        if (nbtContainer == null || nbtContainer.isEmpty()) {
            return null;
        }
        try {
            boolean is_proxy = false;
            if (nbtContainer.contains(NBT_KEY_PROXY)) {
                is_proxy = nbtContainer.getBoolean(NBT_KEY_PROXY);
            }
            String pool = null;
            if (nbtContainer.contains(NBT_KEY_POOL)) {
                pool = nbtContainer.getString(NBT_KEY_POOL);
            }
            var max_spell_count = 0;
            if (nbtContainer.contains(NBT_KEY_MAX_SPELL_COUNT)) {
                max_spell_count = nbtContainer.getInt(NBT_KEY_MAX_SPELL_COUNT);
            }
            var spellIds = new ArrayList<String>();
            if (nbtContainer.contains(NBT_KEY_SPELL_IDS)) {
                var spellList = nbtContainer.getList(NBT_KEY_SPELL_IDS, NbtElement.STRING_TYPE);
                for (int i = 0; i < spellList.size(); i++) {
                    spellIds.add(spellList.getString(i));
                }
            }
            // System.out.println("Returning NBT parsed container" + new SpellContainer(is_proxy, pool, max_spell_count, spellIds));
            return new SpellContainer(is_proxy, pool, max_spell_count, spellIds);
        } catch (Exception e) {
            System.err.println("Failed to decode spell container from NBT: " + e.getMessage());
        }
        return null;
    }
}
