package firearms.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * The client entrypoint. Item models are composite/condition JSON, driven by data components with
 * zero registration code (docs/spec/04-architecture.md ARCH-DEC-006); the scope mechanic is three
 * client-side mixins (ARCH-DEC-001), not an entrypoint hook. Nothing registers here yet.
 */
public final class FirearmsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
    }
}
