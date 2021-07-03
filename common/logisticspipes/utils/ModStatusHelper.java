package logisticspipes.utils;

import net.minecraftforge.fml.ModList;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class ModStatusHelper {

	public static boolean isModLoaded(String modId) {
		if (modId.contains("@")) {
			final String version = modId.substring(modId.indexOf('@') + 1);
			modId = modId.substring(0, modId.indexOf('@'));
			return ModList.get().getModContainerById(modId).map(mod -> mod.getModInfo().getVersion().compareTo(new DefaultArtifactVersion(version)) == 0).orElse(false);
		} else {
			return ModList.get().isLoaded(modId);
		}
	}

	public static boolean areModsLoaded(String modIds) {
		if (modIds.contains("+")) {
			for (String modId : modIds.split("\\+")) {
				if (!isModLoaded(modId)) {
					return false;
				}
			}
			return true;
		} else {
			return isModLoaded(modIds);
		}
	}

	public static boolean isModVersionEqualsOrHigher(String modId, String version) {
		return ModList.get().getModContainerById(modId).map(mod -> mod.getModInfo().getVersion().compareTo(new DefaultArtifactVersion(version)) >= 0).orElse(false);
	}
}
