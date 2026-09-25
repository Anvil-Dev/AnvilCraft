"""Compile unmodified local 1.21 orbital math and regenerate its deterministic oracle samples."""
from pathlib import Path
import argparse
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--java-home", default="C:/Program Files/Java/jdk-21")
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]
folder = root / "build/porting/overworld-oracle"
folder.mkdir(parents=True, exist_ok=True)
source = subprocess.check_output(["git", "show", "dev/1.21/1.6:src/main/java/dev/dubhe/anvilcraft/worldgen/OverworldLikeOrbitMath.java"], cwd=root)
(folder / "OverworldLikeOrbitMath.java").write_bytes(source)
(folder / "OrbitOracle.java").write_text('import dev.dubhe.anvilcraft.worldgen.OverworldLikeOrbitMath;\npublic class OrbitOracle {\n    public static void main(String[] args) {\n        for (long seed : new long[]{0, 42, -1, Long.MIN_VALUE}) {\n            for (long time : new long[]{-24000, 0, 1, 7777, 24000, 1000000}) {\n                for (long day : new long[]{0, 3000, 6000, 12000, 18000}) {\n                    var pose = OverworldLikeOrbitMath.ringPose(4, time, 0.25F, 125, seed);\n                    System.out.println(seed + "," + time + "," + day + "," + pose.outerRotation() + ","\n                        + pose.middleRotation() + "," + pose.innerRotation() + ","\n                        + OverworldLikeOrbitMath.eclipseFactor(time, day, 125, seed) + ","\n                        + OverworldLikeOrbitMath.additionalSkyDarken(time, day, 125, seed));\n                }\n            }\n        }\n    }\n}\n', encoding="utf8")
subprocess.run([str(Path(args.java_home) / "bin/javac.exe"), "-d", str(folder / "classes"),
                str(folder / "OverworldLikeOrbitMath.java"), str(folder / "OrbitOracle.java")], check=True)
output = subprocess.check_output([str(Path(args.java_home) / "bin/java.exe"), "-cp", str(folder / "classes"), "OrbitOracle"])
(root / "dev/porting/resources/overworld-orbit-source.csv").write_bytes(output)
print("Generated 120 source orbital/eclipsing samples")
