package net.techcable.sonarpet.nms.entity.type;

import com.dsh105.echopet.compat.api.entity.IPet;
import com.dsh105.echopet.compat.api.entity.type.nms.IEntityVillagerPet;

import net.techcable.sonarpet.EntityHook;
import net.techcable.sonarpet.EntityHookType;
import net.techcable.sonarpet.nms.NMSInsentientEntity;
import net.techcable.sonarpet.nms.entity.EntityAgeablePet;

import org.bukkit.entity.Villager;

@EntityHook(EntityHookType.VILLAGER)
public class EntityVillagerPet extends EntityAgeablePet implements IEntityVillagerPet {
    protected EntityVillagerPet(IPet pet, NMSInsentientEntity entity, EntityHookType hookType) {
        super(pet, entity, hookType);
    }

    @Override
    public void setProfession(Villager.Profession profession) {
        ((Villager) getBukkitEntity()).setProfession(profession);
    }
}
