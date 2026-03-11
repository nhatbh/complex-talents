package com.complextalents.item;

import com.complextalents.TalentsMod;
import mod.chloeprime.aaaparticles.api.common.AAALevel;
import mod.chloeprime.aaaparticles.api.common.ParticleEmitterInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class AAAParticleTesterItem extends Item {
    private final String particleName;

    public AAAParticleTesterItem(Properties properties, String particleName) {
        super(properties);
        this.particleName = particleName;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        if (level.isClientSide) {
            ParticleEmitterInfo info = new ParticleEmitterInfo(
                    ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, particleName));
            AAALevel.addParticle(level, false, info.position(
                    blockPos.getX() + 0.5d, blockPos.getY() + 1d, blockPos.getZ() + 0.5d));
        }
        return InteractionResult.SUCCESS;
    }
}
