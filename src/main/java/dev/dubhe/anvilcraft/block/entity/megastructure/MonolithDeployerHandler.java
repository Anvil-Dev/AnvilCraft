package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetResourceGenerator;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Projects a monolith onto a planet, advancing its civilization after ten seconds. */
public class MonolithDeployerHandler extends BaseMegastructureHandler {
    private static final int DEPLOYMENT_TICKS = 200;
    private static final int ADVANCEMENT_RADIUS = 16;
    private int ticksRemaining;

    @Override
    public String name() {
        return "monolith_deployer";
    }

    public static boolean canDeploy(@Nullable PlanetaryResourceSet resources) {
        return resources != null && !resources.isWasteland()
            && (resources.hasCivilization() || resources.getBiologicalItems().stream()
                .filter(entry -> entry.weight() > 0)
                .map(PlanetaryResourceSet.WeightedItemStack::itemId)
                .distinct().count() >= 3);
    }

    @Override
    public void onBuild(CelestialForgingAnvilBlockEntity be) {
        this.ticksRemaining = DEPLOYMENT_TICKS;
        be.setChanged();
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.ticksRemaining = 0;
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel level) || this.ticksRemaining <= 0) return;
        this.ticksRemaining--;
        be.setChanged();
        if (this.ticksRemaining > 0) return;
        PlanetaryResourceSet resources = be.getPlanetaryResourceSet();
        CelestialBodyData body = be.getCelestialBodyData();
        boolean planet = body instanceof RockyPlanetData
            || body instanceof SpecialCelestialBodyData special && !special.isErrorPlanet();
        if (!planet || !canDeploy(resources)) {
            be.clearMegastructure();
            level.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            return;
        }
        if (resources.hasCivilization() && level.getRandom().nextBoolean()) {
            be.completeMonolithDeployment(null);
            awardNewCycle(be, level);
        } else {
            be.completeMonolithDeployment(PlanetResourceGenerator.advanceCivilization(resources, level, level.getRandom().nextLong()));
        }
    }

    private static void awardNewCycle(CelestialForgingAnvilBlockEntity be, ServerLevel level) {
        AdvancementHolder advancement = level.getServer().getAdvancements().get(AnvilCraft.of("anvilcraft/new_cycle"));
        if (advancement == null) return;
        for (ServerPlayer player : level.getPlayers(player -> player.distanceToSqr(be.getBlockPos().getCenter())
            <= ADVANCEMENT_RADIUS * ADVANCEMENT_RADIUS)) {
            player.getAdvancements().award(advancement, "new_cycle");
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.putInt("monolithDeploymentTicks", this.ticksRemaining);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        this.ticksRemaining = Math.clamp(input.getIntOr("monolithDeploymentTicks", 0), 0, DEPLOYMENT_TICKS);
    }
}
