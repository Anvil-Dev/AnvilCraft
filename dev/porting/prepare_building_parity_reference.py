"""Install the same visual fixture in the task-owned local 1.21 reference worktree."""
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[2]
reference = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "build/porting/reference-frost-1.21"
if not reference.is_relative_to((root / "build/porting").resolve()):
    raise SystemExit("Reference must be inside the task-owned build/porting directory")
source_ref = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root, text=True).strip()
reference_ref = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
if source_ref != reference_ref:
    raise SystemExit("Reference worktree must match the latest local source branch")
relative = "src/main/java/dev/dubhe/anvilcraft/porting/BuildingRodVisualParityScene.java"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/BuildingRodVisualParityScene.java").read_text(encoding="utf-8")
scene = scene.replace("import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;\n", "")
scene = scene.replace('private static final String VERSION = "26.1";', 'private static final String VERSION = "1.21";')
scene = scene.replace("new StoredEnergy(current == 11 ? 0 : 10000)", "current == 11 ? 0 : 10000")
scene = scene.replace("client.getToastManager()", "client.getToasts()")
scene = scene.replace("BuildingEntityTransform.withWorldPos(", "dev.dubhe.anvilcraft.building.EntityBuildAdapters.withWorldPos(")
scene = scene.replace(
    "new LevelSettings(name, GameType.CREATIVE,\n                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT)",
    "new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,\n                    new net.minecraft.world.level.GameRules(), WorldDataConfiguration.DEFAULT)")
scene = scene.replace("WorldPresets::createFlatWorldDimensions",
    "registries -> registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)\n                    .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()")
scene = scene.replace("client.getMainRenderTarget(), 1,\n", "client.getMainRenderTarget(),\n")
(reference / relative).write_bytes(scene.replace("\n", "\r\n").encode("utf-8"))
build = reference / "build.gradle"
text = build.read_text(encoding="utf-8").replace("systemProperty 'anvilcraft.portProcessingTableScene', 'true'", "systemProperty 'anvilcraft.portProcessingTableScene', 'false'")
setting = "neoForge.runs.client { systemProperty 'anvilcraft.portBuildingVisualParityScene', 'true' }"
if setting not in text:
    text += "\n" + setting + "\n"
build.write_bytes(text.replace("\n", "\r\n").encode("utf-8"))
print(source_ref)
