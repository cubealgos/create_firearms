/**
 * The development-environment-only {@code /firearms debug} command (`FA-12`,
 * `docs/spec/domains/ui.md` `UI-REQ-006`): {@link firearms.debug.DebugCommand} gives a fully
 * attached, fully loaded weapon for fast iteration and prints a weapon stack's derived stats and
 * component state without hovering. Registered only when {@code
 * net.fabricmc.loader.api.FabricLoader#isDevelopmentEnvironment()} is {@code true} (the one guarded
 * line in {@code firearms.Firearms#onInitialize()}); never present in a released jar.
 */
package firearms.debug;
