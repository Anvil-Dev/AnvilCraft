"""Verify exact source math/history code and the real GL resource probe result."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
for name in ["MunShadowClock.java", "MunShadowProjection.java"]:
    path = "src/main/java/dev/dubhe/anvilcraft/client/renderer/mun/" + name
    assert (root / path).read_text(encoding="utf8") == (reference / path).read_text(encoding="utf8"), name


def function(text, signature):
    start = text.index(signature)
    position = text.index("{", start) + 1
    depth = 1
    while depth:
        depth += (text[position] == "{") - (text[position] == "}")
        position += 1
    return text[start:position]


path = "src/main/resources/assets/anvilcraft/shaders/include/mun/mun_surface.glsl"
actual = (root / path).read_text(encoding="utf8")
original = (reference / path).read_text(encoding="utf8")
for signature in ["uint shadowHash(", "float stableShadowVisibility("]:
    assert function(actual, signature) == function(original, signature), signature
log_path = root / "build/porting/client-mun-shadow-resources-final-2.log"
log = log_path.read_text(encoding="utf8", errors="replace")
assert "PORT_MUN_SHADOW_RESOURCES_PASSED" in log
assert "PORT_MUN_SKY_PASSED" in log and "All dimensions are saved" in log and "BUILD SUCCESSFUL" in log
report = {"clock_and_projection_source_equal": True, "history_shader_algorithm_source_equal": True,
          "gpu_resource_probe": "passed", "client_save_exit": "passed",
          "limits": ["Resource semantics are verified; actual world shadow mesh/receiver integration is still pending.",
                     "Only the current OpenGL backend with image load/store was exercised."]}
output = root / "build/porting/mun-shadow-resources-comparison.json"
output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")
print(json.dumps(report, indent=2))
