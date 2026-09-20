package firearms.client;

import firearms.client.combat.CombatClient;
import firearms.client.fire.RecoilHandler;
import firearms.client.ui.WeaponTooltip;
import net.fabricmc.api.ClientModInitializer;

/**
 * The client entrypoint. Item models are composite/condition JSON, driven by data components with
 * zero registration code (docs/spec/04-architecture.md ARCH-DEC-006); the scope mechanic is three
 * client-side mixins (ARCH-DEC-001), not an entrypoint hook.
 */
public final class FirearmsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CombatClient.register();
        RecoilHandler.register();
        WeaponTooltip.register();
    }
}
