package firearms.data;

import firearms.Firearms;
import firearms.model.WeaponBase;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Reads {@code data/firearms/weapon/*.json} into {@link WeaponRegistry} on every resource reload
 * (`docs/spec/domains/weapon.md` `WEAPON-DEC-003`, `docs/spec/contracts/public-surface.md`). This
 * mod's own six files under that path ship the same numbers {@code firearms.model.WeaponBase}'s
 * compiled constants do (`TEST-REQ-001`'s own game test proves the two agree); a datapack shipping
 * a file at the same path wins, per ordinary resource-pack layering — nothing here special-cases
 * that override.
 */
public final class WeaponDataLoader extends SimpleJsonResourceReloadListener<WeaponBaseFile>
    implements IdentifiableResourceReloadListener {

    public WeaponDataLoader() {
        // "weapon", not "firearms/weapon": FileToIdConverter's prefix sits inside each namespace's
        // own data/<namespace>/ folder, and this mod's own namespace ("firearms") already supplies
        // that segment, matching data/firearms/weapon/*.json exactly.
        super(WeaponBaseFile.CODEC, FileToIdConverter.json("weapon"));
    }

    @Override
    protected void apply(Map<Identifier, WeaponBaseFile> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, WeaponBase> weapons = loaded.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toWeaponBase(e.getKey().getPath())));
        WeaponRegistry.set(weapons);
        Firearms.LOGGER.info("Loaded {} weapon(s) from data/firearms/weapon/", weapons.size());
    }

    @Override
    public Identifier getFabricId() {
        return Firearms.id("weapon_data");
    }
}
