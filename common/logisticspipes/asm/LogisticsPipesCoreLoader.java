package logisticspipes.asm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import lombok.Getter;

import logisticspipes.LogisticsPipes;

public class LogisticsPipesCoreLoader {

	@Getter
	private static boolean coremodLoaded = false;
	private static boolean developmentEnvironment = false;
	private static LogisticsPipesCoreLoader instance;

	private final TransformingClassLoader loader;

	public LogisticsPipesCoreLoader() throws Exception {
		try {
			final Field classLoaderField = Launcher.class.getDeclaredField("classLoader");
			classLoaderField.setAccessible(true);
			loader = (TransformingClassLoader) classLoaderField.get(Launcher.INSTANCE);
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			LogisticsPipes.getLOGGER().error("Cannot fetch Launcher.classLoader");
			throw new RuntimeException(e);
		}
		coremodLoaded = true;
		addTransformerExclusion("logisticspipes.asm.");
		instance = this;
	}

	public static LogisticsPipesCoreLoader getInstance() {
		return instance;
	}

	public void addTransformerExclusion(String packageName) throws NoSuchFieldException, IllegalAccessException {
		final Field packageExceptionsField = TransformingClassLoader.class.getDeclaredField("SKIP_PACKAGE_PREFIXES");
		packageExceptionsField.setAccessible(true);
		//noinspection unchecked
		final List<String> exceptions = (List<String>) packageExceptionsField.get(null);
		final ArrayList<String> newExceptions = new ArrayList<>(exceptions);
		newExceptions.add(packageName);
		packageExceptionsField.set(null, newExceptions);
	}

	public String[] getASMTransformerClass() {
		return new String[] { "logisticspipes.asm.LogisticsClassTransformer" };
	}

	public String getModContainerClass() {
		return null;
	}

	public String getSetupClass() {
		return null;
	}

	public void injectData(Map<String, Object> data) {
		if (data.containsKey("runtimeDeobfuscationEnabled")) {
			developmentEnvironment = !((Boolean) data.get("runtimeDeobfuscationEnabled"));
		}
	}

	public String getAccessTransformerClass() {
		return null;
	}

	public static boolean isDevelopmentEnvironment() {
		return developmentEnvironment;
	}
}
