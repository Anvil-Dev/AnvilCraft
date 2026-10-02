package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class MessageLang {
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        provider.add("message.anvilcraft.buffer_boots.charged", "Press %1$s to jump");
        provider.add("hud.anvilcraft.weatherproof_chestplate_power", "%s%%");
        provider.add("screen.anvilcraft.pockets.empty", "Empty pocket");
        provider.add("message.anvilcraft.monolith.return_confirmation", "The monolith can help you return. Touch it again within 3 seconds to travel back.");
        provider.add("message.anvilcraft.monolith.offering", "Offer an anvil to the monolith to gain knowledge");
        provider.add("message.anvilcraft.monolith.giant_offering", "Offer a giant anvil to the monolith to gain knowledge");
        provider.add("book.anvilcraft.monolith.title", "Celestial Knowledge");
        provider.add("book.anvilcraft.monolith.page", "[%1$s]\nTime Anvils: %2$s\nSpace Anvils: %3$s\nMass Anvils: %4$s\nEnergy Anvils: %5$s\nSeed Item: %6$s");
        provider.add("message.anvilcraft.trading_station.break.player.title", "===|| Someone broke a trading station! ||===");
        provider.add("message.anvilcraft.trading_station.break.non_player.title", "===|| A trading station was broken! ||===");
        provider.add("message.anvilcraft.trading_station.break.owner", "Owner: %s");
        provider.add("message.anvilcraft.trading_station.break.breaker", "Breaker: %s");
        provider.add("message.anvilcraft.trading_station.break.pos", "Position: %1$d %2$d %3$d in %4$s");
        provider.add("message.anvilcraft.trading_station.break.time", "Time: %s");
        provider.add("message.anvilcraft.trading_station.break.onliners", "Online Players: ");
        provider.add("message.anvilcraft.trading_station.break.closest", "Closest Player: %s");
        provider.add("message.anvilcraft.hyperdimension_terminal.bound", "Terminal bound");
        provider.add("message.anvilcraft.hyperdimension_terminal.not_bound", "Terminal is not bound");
        provider.add("message.anvilcraft.hyperdimension_terminal.not_found", "Bound storage station not found");
        provider.add("message.anvilcraft.hyperdimension_uploader.bound", "Hyperdimension Uploader bound");
        provider.add("message.anvilcraft.local_terminal.not_found", "No large crate within 32 blocks");
        provider.add("message.anvilcraft.shulker_terminal.not_found", "No shulker container or shulker box found");
        provider.add("message.anvilcraft.monolith.joke.chute_steal", "Chutes used to be able to steal items from players and villagers");
        provider.add("message.anvilcraft.monolith.joke.reinforced_concrete", "Reinforced concrete was initially not blast-resistant, and it dropped a cauldron when broken.");
        provider.add("message.anvilcraft.monolith.knowledge.celestial_forging_anvil_gravity", "You can press shift to escape when sucked by the Celestial Forging Anvil's gravity");
        provider.add("message.anvilcraft.monolith.knowledge.celestial_forging_anvil_portal", "Celestial Forging Anvil portals can transport water");
        provider.add("message.anvilcraft.monolith.knowledge.corrupted_beacon", "Corrupted Beacons can only use Cursed Gold Blocks as bases");
        provider.add("message.anvilcraft.monolith.knowledge.crab_claw", "Crab Claws can pry open Shulkers");
        provider.add("message.anvilcraft.monolith.knowledge.ember_metal", "Making Ember Metal does not require heating Netherite");
        provider.add("message.anvilcraft.monolith.knowledge.filter", "Filter slots can adjust item stack limits");
        provider.add("message.anvilcraft.monolith.knowledge.fish_tank", "Fish Tanks can be used to raise fish");
        provider.add("message.anvilcraft.monolith.knowledge.flying_anvil_hammer", "Wearing an Anvil Hammer while flying and colliding with mobs deals massive damage");
        provider.add("message.anvilcraft.monolith.knowledge.heater", "Heaters can heat Tungsten Blocks and Netherite Blocks");
        provider.add("message.anvilcraft.monolith.knowledge.horizontal_anvil_damage", "Anvils moving horizontally at high speed can deal impact damage");
        provider.add("message.anvilcraft.monolith.knowledge.melt_gem", "Melt Gem reacting with water generates diorite, granite, or andesite");
        provider.add("message.anvilcraft.monolith.knowledge.menger_sponge", "Menger Sponges can act as fluid trash cans");
        provider.add("message.anvilcraft.monolith.knowledge.player_acceleration", "Players can also be accelerated by Acceleration Rings");
        provider.add("message.anvilcraft.monolith.knowledge.projectile_acceleration", "Acceleration Rings can accelerate all projectiles");
        provider.add("message.anvilcraft.monolith.knowledge.rocket_jump", "Right-clicking the ground with an Anvil Hammer and Firework Rockets performs a rocket jump");
        provider.add("message.anvilcraft.monolith.knowledge.vault_reset", "Smashing a Lead Block into a Vault resets it");
        provider.add("message.anvilcraft.monolith.knowledge.villager_reset", "Villagers can be struck by anvils to cause amnesia and reset trades, but they might get dazed");
        provider.add("message.anvilcraft.monolith.knowledge.waterlogged_acceleration_ring", "Waterlogged Acceleration Rings accelerate anvils at a very slow speed");
    }
}
