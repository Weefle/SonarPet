/*
 * This file is part of EchoPet.
 *
 * EchoPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EchoPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EchoPet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dsh105.echopet.api.pet;

import java.util.ArrayList;
import java.util.UUID;

import com.dsh105.commodus.StringUtil;
import com.dsh105.echopet.compat.api.entity.EntityPetType;
import com.dsh105.echopet.compat.api.entity.IEntityNoClipPet;
import com.dsh105.echopet.compat.api.entity.IEntityPet;
import com.dsh105.echopet.compat.api.entity.IPet;
import com.dsh105.echopet.compat.api.entity.PetData;
import com.dsh105.echopet.compat.api.entity.PetType;
import com.dsh105.echopet.compat.api.event.PetPreSpawnEvent;
import com.dsh105.echopet.compat.api.event.PetTeleportEvent;
import com.dsh105.echopet.compat.api.plugin.EchoPet;
import com.dsh105.echopet.compat.api.plugin.uuid.UUIDMigration;
import com.dsh105.echopet.compat.api.util.Lang;

import net.techcable.sonarpet.EntityHookType;
import net.techcable.sonarpet.nms.INMS;
import com.dsh105.echopet.compat.api.util.PetNames;
import com.dsh105.echopet.compat.api.util.StringSimplifier;
import com.google.common.base.Preconditions;

import net.techcable.sonarpet.particles.Particle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static com.google.common.base.Preconditions.*;

public abstract class Pet implements IPet {

    protected IEntityPet hook;
    private PetType petType;

    private Object ownerIdentification;
    private Pet rider;
    private String name;
    private ArrayList<PetData> petData = new ArrayList<PetData>();

    private boolean isRider = false;

    public boolean ownerIsMounting = false;
    private boolean ownerRiding = false;
    private boolean isHat = false;

    public Pet(Player owner) {
        if (owner != null) {
            this.ownerIdentification = UUIDMigration.getIdentificationFor(owner);
            this.setPetType();
            this.setPetName(this.getPetType().getDefaultName(this.getNameOfOwner()));
            spawnPet(owner, getPetType().getPrimaryHookType(), false);
        }
    }

    private void spawnPet(Player owner, EntityHookType hookType, boolean forced) {
        checkState(this.hook == null, "Pet already spawned");
        if (!forced) {
            PetPreSpawnEvent spawnEvent = new PetPreSpawnEvent(this, owner.getLocation());
            EchoPet.getPlugin().getServer().getPluginManager().callEvent(spawnEvent);
            if (spawnEvent.isCancelled()) {
                owner.sendMessage(EchoPet.getPrefix() + ChatColor.YELLOW + "Pet spawn was cancelled externally.");
                EchoPet.getManager().removePet(this, true);
                return;
            }
        }
        this.hook = EchoPet.getPlugin().getHookRegistry().spawnEntity(this, hookType, owner.getLocation());
        this.applyPetName();
        this.teleportToOwner();
    }

    protected void switchHookType(Player owner, EntityHookType newHookType) {
        if (newHookType == hook.getHookType())
        checkState(this.hook != null, "Pet isn't spawned yet!");
        EchoPet.getManager().removePet(this, false);
        this.hook = null;
        spawnPet(owner, newHookType, true);
    }

    protected void setPetType() {
        EntityPetType entityPetType = this.getClass().getAnnotation(EntityPetType.class);
        if (entityPetType != null) {
            this.petType = entityPetType.petType();
        }
    }

    @Override
    public IEntityPet getEntityPet() {
        return this.hook;
    }

    @Override
    public LivingEntity getCraftPet() {
        return this.getEntityPet().getBukkitEntity();
    }

    @Override
    public Location getLocation() {
        return this.getCraftPet().getLocation();
    }

    @Override
    public Player getOwner() {
        if (this.ownerIdentification == null) {
            return null;
        }
        if (this.ownerIdentification instanceof UUID) {
            return Bukkit.getPlayer((UUID) ownerIdentification);
        } else {
            return Bukkit.getPlayerExact((String) this.ownerIdentification);
        }
    }

    @Override
    public String getNameOfOwner() {
        if (this.ownerIdentification instanceof String) {
            return (String) this.ownerIdentification;
        } else {
            return this.getOwner() == null ? "" : this.getOwner().getName();
        }
    }

    @Override
    public UUID getOwnerUUID() {
        if (this.ownerIdentification instanceof UUID) {
            return (UUID) this.ownerIdentification;
        } else {
            return this.getOwner() == null ? null : this.getOwner().getUniqueId();
        }
    }

    @Override
    public Object getOwnerIdentification() {
        return ownerIdentification;
    }

    @Override
    public PetType getPetType() {
        return this.petType;
    }

    @Override
    public boolean isRider() {
        return this.isRider;
    }

    protected void setRider() {
        this.isRider = true;
    }

    @Override
    public boolean isOwnerInMountingProcess() {
        return ownerIsMounting;
    }

    @Override
    public Pet getRider() {
        return this.rider;
    }

    @Override
    public String getPetName() {
        return name;
    }

    @Override
    public String getPetNameWithoutColours() {
        return ChatColor.stripColor(this.getPetName());
    }
    
    @Override
    public String serialisePetName() {
        return getPetName().replace(ChatColor.COLOR_CHAR, '&');
    }

    @Override
    public boolean setPetName(String name) {
        return this.setPetName(name, true);
    }

    @Override
    public boolean setPetName(String name, boolean sendFailMessage) {
        if (PetNames.allow(name, this)) {
            this.name = ChatColor.translateAlternateColorCodes('&', name);
            if (EchoPet.getPlugin().getMainConfig().getBoolean("stripDiacriticsFromNames", true)) {
                this.name = StringSimplifier.stripDiacritics(this.name);
            }
            if (this.name == null || this.name.equalsIgnoreCase("")) {
                this.name = this.petType.getDefaultName(this.getNameOfOwner());
            }
            this.applyPetName();
            return true;
        } else {
            if (sendFailMessage) {
                if (this.getOwner() != null) {
                    Lang.sendTo(this.getOwner(), Lang.NAME_NOT_ALLOWED.toString().replace("%name%", name));
                }
            }
            return false;
        }
    }

    private void applyPetName() {
        if (this.getEntityPet() != null && this.getCraftPet() != null) {
            this.getCraftPet().setCustomName(this.name);
            this.getCraftPet().setCustomNameVisible(EchoPet.getConfig().getBoolean("pets." + this.getPetType().toString().toLowerCase().replace("_", " ") + ".tagVisible", true));
        }
    }

    @Override
    public ArrayList<PetData> getPetData() {
        return this.petData;
    }

    @Override
    public void removeRider() {
        if (rider != null) {
            INMS.getInstance().mount(rider.getCraftPet(), null);
            rider.removePet(true);
            this.rider = null;
        }
    }

    @Override
    public void removePet(boolean makeSound) {
        if (this.getCraftPet() != null) {
            Particle.CLOUD.show(getLocation());
            Particle.LAVA_SPARK.show(getLocation());
        }
        removeRider();
        if (this.getEntityPet() != null) {
            this.getEntityPet().remove(makeSound);
        }
    }

    @Override
    public boolean teleportToOwner() {
        if (this.getOwner() == null || this.getOwner().getLocation() == null) {
            this.removePet(false);
            return false;
        }
        return this.teleport(this.getOwner().getLocation());
    }

    @Override
    public boolean teleport(Location to) {
        if (this.getEntityPet() == null || this.getEntityPet().isDead()) {
            EchoPet.getManager().saveFileData("autosave", this);
            EchoPet.getSqlManager().saveToDatabase(this, false);
            EchoPet.getManager().removePet(this, false);
            EchoPet.getManager().createPetFromFile("autosave", this.getOwner());
            return false;
        }
        PetTeleportEvent teleportEvent = new PetTeleportEvent(this, this.getLocation(), to);
        EchoPet.getPlugin().getServer().getPluginManager().callEvent(teleportEvent);
        if (teleportEvent.isCancelled()) {
            return false;
        }
        Location l = teleportEvent.getTo();
        if (l.getWorld() == this.getLocation().getWorld()) {
            if (this.getRider() != null) {
                this.getRider().getCraftPet().eject();
                this.getRider().getCraftPet().teleport(l);
            }
            this.getCraftPet().teleport(l);
            if (this.getRider() != null) {
                this.getCraftPet().setPassenger(this.getRider().getCraftPet());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isOwnerRiding() {
        return this.ownerRiding;
    }

    @Override
    public boolean isHat() {
        return this.isHat;
    }

    @Override
    public void ownerRidePet(boolean flag) {
        if (this.ownerRiding == flag) {
            return;
        }

        this.ownerIsMounting = true;

        if (this.isHat) {
            this.setAsHat(false);
        }

        // Ew...This stuff is UGLY :c

        if (!flag) {
            INMS.getInstance().mount(this.getOwner(), null);
            //((CraftPlayer) this.getOwner()).getHandle().mount(null);
            if (this.getEntityPet() instanceof IEntityNoClipPet) {
                ((IEntityNoClipPet) this.getEntityPet()).noClip(true);
            }
            ownerIsMounting = false;
        } else {
            if (this.getRider() != null) {
                this.getRider().removePet(false);
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    INMS.getInstance().mount(getOwner(), getEntityPet().getBukkitEntity());
                    //((CraftPlayer) getOwner()).getHandle().mount(getEntityPet());
                    ownerIsMounting = false;
                    if (getEntityPet() instanceof IEntityNoClipPet) {
                        ((IEntityNoClipPet) getEntityPet()).noClip(false);
                    }
                }
            }.runTaskLater(EchoPet.getPlugin(), 5L);
        }
        this.teleportToOwner();
        this.getEntityPet().resizeBoundingBox(flag);
        this.ownerRiding = flag;
        Particle.PORTAL.show(getLocation());
        Location l = this.getLocation().clone();
        l.setY(l.getY() - 1D);
        Particle.BLOCK_DUST.builder().ofBlockType(l.getBlock().getType()).at(getLocation()).show();
    }

    @Override
    public void setAsHat(boolean flag) {
        if (this.isHat == flag) {
            return;
        }
        if (this.ownerRiding) {
            this.ownerRidePet(false);
        }
        this.teleportToOwner();

        //Entity craftPet = ((Entity) this.getCraftPet().getHandle());
        if (!flag) {
            if (this.getRider() != null) {
                //Entity rider = ((Entity) this.getRider().getCraftPet().getHandle());
                //rider.mount(null);
                INMS.getInstance().mount(this.getRider().getEntityPet().getBukkitEntity(), null);

                //craftPet.mount(null);
                INMS.getInstance().mount(this.getEntityPet().getBukkitEntity(), null);

                //rider.mount(craftPet);
                INMS.getInstance().mount(this.getRider().getEntityPet().getBukkitEntity(), this.getEntityPet().getBukkitEntity());
            } else {
                //craftPet.mount(null);
                INMS.getInstance().mount(this.getEntityPet().getBukkitEntity(), null);
            }
        } else {
            if (this.getRider() != null) {
                //Entity rider = ((Entity) this.getRider().getCraftPet().getHandle());
                //rider.mount(null);
                INMS.getInstance().mount(this.getRider().getEntityPet().getBukkitEntity(), null);

                //craftPet.mount(((CraftPlayer) this.getOwner()).getHandle());
                INMS.getInstance().mount(this.getEntityPet().getBukkitEntity(), this.getOwner());

                //this.getCraftPet().setPassenger(this.getRider().getCraftPet());
                INMS.getInstance().mount(this.getRider().getEntityPet().getBukkitEntity(), this.getEntityPet().getBukkitEntity());
            } else {
                //craftPet.mount(((CraftPlayer) this.getOwner()).getHandle());
                INMS.getInstance().mount(this.getEntityPet().getBukkitEntity(), this.getOwner());
            }
        }
        this.getEntityPet().resizeBoundingBox(flag);
        this.isHat = flag;
        Particle.PORTAL.show(getLocation());
        Location l = this.getLocation().clone();
        l.setY(l.getY() - 1D);
        Particle.PORTAL.show(getLocation());
    }

    @Override
    public Pet createRider(final PetType pt, boolean sendFailMessage) {
        if (pt == PetType.HUMAN) {
            if (sendFailMessage) {
                Lang.sendTo(this.getOwner(), Lang.RIDERS_DISABLED.toString().replace("%type%", StringUtil.capitalise(this.getPetType().toString())));
            }
            return null;
        }
        if (!EchoPet.getOptions().allowRidersFor(this.getPetType())) {
            if (sendFailMessage) {
                Lang.sendTo(this.getOwner(), Lang.RIDERS_DISABLED.toString().replace("%type%", StringUtil.capitalise(this.getPetType().toString())));
            }
            return null;
        }
        if (this.isOwnerRiding()) {
            this.ownerRidePet(false);
        }
        if (this.rider != null) {
            this.removeRider();
        }
        IPet newRider = pt.getNewPetInstance(this.getOwner());
        if (newRider == null) {
            if (sendFailMessage) {
                Lang.sendTo(getOwner(), Lang.PET_TYPE_NOT_COMPATIBLE.toString().replace("%type%", StringUtil.capitalise(getPetType().toString())));
            }
            return null;
        }
        this.rider = (Pet) newRider;
        this.rider.setRider();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (getCraftPet() != null) {
                    INMS.getInstance().mount(Pet.this.getRider().getCraftPet(), getCraftPet());
                }
                EchoPet.getSqlManager().saveToDatabase(Pet.this.rider, true);
            }
        }.runTaskLater(EchoPet.getPlugin(), 5L);

        return this.rider;
    }
}
