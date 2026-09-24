"""Install identical offering snapshots on the unchanged local 1.21 reference."""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/MonolithClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock', 'dev.dubhe.anvilcraft.block.GiantAnvilBlock')
scene = scene.replace('import net.minecraft.world.entity.Relative;\n', '').replace('import java.util.Set;\n', '')
scene = scene.replace('import net.minecraft.world.level.storage.TagValueInput;\n', '')
start = scene.index("            client.level.clockManager()")
end = scene.index("            client.level.environmentAttributes().invalidateTickCache();", start)
scene = scene[:start] + "            client.level.setDayTime(6000);\n" + scene[end + len("            client.level.environmentAttributes().invalidateTickCache();\n"):]
scene = scene.replace('.setTimeFromServer(', '.setGameTime(').replace('Set.<Relative>of(), ', '').replace('140, 12, false)', '140, 12)')
scene = scene.replace('tag.store("Offering", BlockState.CODEC, offering);', 'tag.put("Offering", net.minecraft.nbt.NbtUtils.writeBlockState(offering));')
scene = scene.replace('core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));',
                      'core.loadWithComponents(tag, level.registryAccess());')
scene = scene.replace('.count()', '.getCount()').replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('monolith-26.1-', 'monolith-1.21-')
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
(folder / 'MonolithClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MunSurfaceReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MunSurfaceReferenceScene', 'MonolithReferenceScene').replace('MunSurfaceClientScene', 'MonolithClientScene')
wrapper = wrapper.replace('portMunSurfaceScene', 'portMonolithScene')
(folder / 'MonolithReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')

p = reference / "build.gradle"
s = p.read_text(encoding="utf-8").replace("systemProperty 'anvilcraft.portMunSurfaceScene', 'true'", "systemProperty 'anvilcraft.portMunSurfaceScene', 'false'")
p.write_text(s, encoding="utf-8", newline="\r\n")
