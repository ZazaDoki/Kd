package hu.feherhollo69.cooldowntracker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class CooldownTrackerClient implements ClientModInitializer {
    public static final String MOD_ID = "cooldowntracker";
    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        // Jobb klikk (ender pearl, nyúlláb, horgászbot)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) {
                CooldownManager.handle(player.getStackInHand(hand), "USE");
            }
            return ActionResult.PASS;
        });

        // Ütés (fejsze, blaze rod, lila festék, jég)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity) {
                CooldownManager.handle(player.getStackInHand(hand), "HIT");
            }
            return ActionResult.PASS;
        });

        // HUD a hotbar fölött
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.of(MOD_ID, "cooldowns"),
                (ctx, tick) -> CooldownHud.render(ctx, MinecraftClient.getInstance()));

        // Boss időzítők: a /boss válaszának olvasása a chatből
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || BossTracker.onMessage(message.getString()));

        // Config megnyitása (alapból: K)
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cooldowntracker.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CooldownManager.tick(client);
            BossTracker.tick(client);
            while (openConfigKey.wasPressed()) {
                client.setScreen(new ConfigScreen(client.currentScreen));
            }
        });
    }
}
