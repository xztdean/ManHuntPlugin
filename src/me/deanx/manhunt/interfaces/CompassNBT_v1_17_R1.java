package me.deanx.manhunt.interfaces;

import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CompassNBT_v1_17_R1 implements CompassNBT {
    private static CompassNBT_v1_17_R1 instance;
    private final ItemStack compass;

    public static CompassNBT getInstance() {
        if (instance == null) {
            instance = new CompassNBT_v1_17_R1();
        }
        return instance;
    }

    public CompassNBT_v1_17_R1() {
        compass = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.COMPASS));
    }

    @Override
    public void updateCompass(Player runner) {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagCompound pos = new NBTTagCompound();
        Location location = runner.getLocation();
        pos.setInt("X", location.getBlockX());
        pos.setInt("Y", location.getBlockY());
        pos.setInt("Z", location.getBlockZ());
        compound.set("LodestoneTracked", NBTTagByte.a(false));
        compound.set("LodestoneDimension", NBTTagString.a("minecraft:overworld"));
        compound.set("LodestonePos", pos);
        compass.setTag(compound);
    }

    @Override
    public void updateInventory(Player player) {
        Inventory inventory = player.getInventory();
        int stackNum = inventory.first(Material.COMPASS);

        if (stackNum >= 0) {
            IInventory nmsInventory = ((CraftInventory) inventory).getInventory();
            nmsInventory.setItem(stackNum, compass);
        }
    }
}
