package net.techcable.sonarpet.nms.entity.type;

import com.dsh105.echopet.compat.api.entity.EntityPetType;
import com.dsh105.echopet.compat.api.entity.IPet;
import com.dsh105.echopet.compat.api.entity.PetType;
import com.dsh105.echopet.compat.api.entity.type.nms.IEntityRabbitPet;

import net.techcable.sonarpet.EntityHook;
import net.techcable.sonarpet.EntityHookType;
import net.techcable.sonarpet.nms.entity.EntityAgeablePet;
import net.techcable.sonarpet.nms.NMSInsentientEntity;

import org.bukkit.entity.Rabbit;

@EntityHook(EntityHookType.RABBIT)
public class EntityRabbitPet extends EntityAgeablePet implements IEntityRabbitPet {
    protected EntityRabbitPet(IPet pet, NMSInsentientEntity entity, EntityHookType hookType) {
        super(pet, entity, hookType);
    }

    @Override
    public Rabbit.Type getType() {
        return getBukkitEntity().getRabbitType();
    }

    @Override
    public void setType(Rabbit.Type type) {
        getBukkitEntity().setRabbitType(type);
    }

    @Override
    public Rabbit getBukkitEntity() {
        return (Rabbit) super.getBukkitEntity();
    }
}
