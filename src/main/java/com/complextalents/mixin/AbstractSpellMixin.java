package com.complextalents.mixin;

import com.complextalents.spellmastery.SpellMasteryManager;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellMixin {

    @Inject(method = "canBeCastedBy", at = @At("HEAD"), cancellable = true)
    private void complextalents$canBeCastedBy(int spellLevel, CastSource castSource, MagicData playerMagicData, Player player, CallbackInfoReturnable<CastResult> cir) {
        Optional<CastResult> masteryResult = SpellMasteryManager.verifyCast((AbstractSpell) (Object) this, spellLevel, castSource, player);
        if (masteryResult.isPresent()) {
            cir.setReturnValue(masteryResult.get());
        }
    }
}
