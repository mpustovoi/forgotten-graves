package me.mgin.graves.block.utility;

import me.mgin.graves.Graves;
import me.mgin.graves.api.InventoriesApi;
import me.mgin.graves.block.entity.GraveBlockEntity;
import me.mgin.graves.config.GravesConfig;
import me.mgin.graves.config.enums.GraveDropType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RetrieveGrave {
    static public boolean retrieve(PlayerEntity player, World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        // Edge case checking & variable initialization
        if (!(blockEntity instanceof GraveBlockEntity graveEntity)) return false;
        if (world.isClient) return false;
        if (graveEntity.getInventory("Items") == null) return false;
        if (graveEntity.getGraveOwner() == null) return false;

        // Ensure they have proper permission
        if (!Permission.playerCanAttemptRetrieve(player, graveEntity))
            if (!Permission.playerCanOverride(player))
                return false;

        // Resolve drop type
        GraveDropType dropType = GravesConfig.resolveConfig("dropType", player.getGameProfile()).main.dropType;

        if (dropType == GraveDropType.INVENTORY) {
            // Store old inventories as one big inventory
            DefaultedList<ItemStack> oldInventory = DefaultedList.of();

            for (InventoriesApi api : Graves.inventories) {
                DefaultedList<ItemStack> inventory = api.getInventory(player);

                // Skip empty inventories
                if (inventory == null) continue;

                oldInventory.addAll(inventory);
                api.clearInventory(player);
            }

            // Keeps track of
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            // Equip inventories
            for (InventoriesApi api : Graves.inventories) {
                DefaultedList<ItemStack> inventory = graveEntity.getInventory(api.getID());

                if (inventory == null)
                    continue;

                if (api.getInventorySize(player) == inventory.size()) {
                    DefaultedList<ItemStack> unequippedItems = api.setInventory(inventory, player);
                    extraItems.addAll(unequippedItems);
                } else {
                    extraItems.addAll(inventory);
                }
            }

            // Check for any potentially unloaded inventories; store them if found
            for (String modID : Graves.unloadedInventories) {
                DefaultedList<ItemStack> inventory = graveEntity.getInventory(modID);
                if (inventory != null)
                    extraItems.addAll(inventory);
            }

            // Preserve previous inventory
            extraItems.addAll(oldInventory);

            // Remove any empty or air slots from extraItems
            extraItems.removeIf(item -> item == ItemStack.EMPTY || item.getItem() == Items.AIR);

            // Move extra items to open slots
            DefaultedList<Integer> openSlots = Inventory.getInventoryOpenSlots(player.getInventory().main);

            for (int i = 0; i < openSlots.size(); i++) {
                if (extraItems.size() > 0) {
                    player.getInventory().setStack(openSlots.get(i), extraItems.get(0));
                    extraItems.remove(0);
                }
            }

            // Drop any excess items
            DefaultedList<ItemStack> dropItems = DefaultedList.of();
            dropItems.addAll(extraItems);
            ItemScatterer.spawn(world, pos, dropItems);
        } else if (dropType == GraveDropType.DROP) {
            DefaultedList<ItemStack> droppedItems = DefaultedList.of();

            // Add loaded inventories to droppedItems list
            for (InventoriesApi api : Graves.inventories) {
                DefaultedList<ItemStack> modInventory = graveEntity.getInventory(api.getID());

                if (modInventory != null)
                    droppedItems.addAll(modInventory);
            }

            // Add any unloaded inventories to droppedItems list
            for (String modID : Graves.unloadedInventories) {
                DefaultedList<ItemStack> modInventory = graveEntity.getInventory(modID);

                if (modInventory != null)
                    droppedItems.addAll(modInventory);
            }

            ItemScatterer.spawn(world, pos, droppedItems);
        }

        // Add player experience back
        player.addExperience(graveEntity.getXp());

        // Remove block
        world.removeBlock(pos, false);
        return true;
    }
}