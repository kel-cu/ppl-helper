package ru.kelcuprum.pplhelper.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kelcuprum.abi.ActionBarInfo;
import ru.kelcuprum.alinlib.AlinLib;
import ru.kelcuprum.alinlib.config.Localization;
import ru.kelcuprum.alinlib.info.World;
import ru.kelcuprum.pplhelper.PepelandHelper;
import ru.kelcuprum.pplhelper.TabHelper;

import static java.lang.Integer.parseInt;

@Mixin(ActionBarInfo.class)
public class ABIMixin {
    @Inject(method = "getMessage", at=@At("RETURN"), remap = false, cancellable = true)
    private static void getMessage(CallbackInfoReturnable<String> cir){
        if(PepelandHelper.selectedProject == null || TabHelper.getWorld() == null) return;
        String huy = "\\n";
        String parsedCoordinates = getString();
        if(parsedCoordinates.isEmpty()) return;
        huy += String.format("&6%s:&r %s", PepelandHelper.selectedProject.world, parsedCoordinates);
        LocalPlayer p = AlinLib.MINECRAFT.player;
        if(p != null && PepelandHelper.selectedProject.world.equalsIgnoreCase(TabHelper.getWorld().shortName)) {
            String[] args = parsedCoordinates.split(" ");
            int near = (int) dist(parseInt(args[0]), parseInt(args[args.length-1]),p.getBlockX(), p.getBlockZ());
            if(near <= PepelandHelper.config.getNumber("SELECTED_PROJECT.AUTO_HIDE", 15).intValue()){
                PepelandHelper.selectedProject = null;
                return;
            } else huy+= String.format(" &6(%s блоков от вас)&r", near);
        }
        cir.setReturnValue(cir.getReturnValue()+Localization.fixFormatCodes(huy));
    }

    @Unique
    private static @NotNull String getString() {
        String coordinates = "";
        if(World.getCodeName().equals("minecraft:overworld") && PepelandHelper.selectedProject.coordinates$overworld != null && !PepelandHelper.selectedProject.coordinates$overworld.isEmpty())
            coordinates = PepelandHelper.selectedProject.coordinates$overworld;
        else if(World.getCodeName().equals("minecraft:the_nether") && PepelandHelper.selectedProject.coordinates$nether != null && !PepelandHelper.selectedProject.coordinates$nether.isEmpty())
            coordinates = PepelandHelper.selectedProject.coordinates$nether;
        else if(World.getCodeName().equals("minecraft:the_end") && PepelandHelper.selectedProject.coordinates$end != null && !PepelandHelper.selectedProject.coordinates$end.isEmpty())
            coordinates = PepelandHelper.selectedProject.coordinates$end;
        return coordinates.replaceAll("[^0-9 \\-.]", "");
    }

    @Unique
    private static float dist(int i, int j, int k, int l) {
        int m = k - i;
        int n = l - j;
        return Mth.sqrt((float)(m * m + n * n));
    }
}
