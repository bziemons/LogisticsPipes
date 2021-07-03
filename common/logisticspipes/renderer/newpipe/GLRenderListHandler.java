package logisticspipes.renderer.newpipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.GLAllocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class GLRenderListHandler {

	private List<GLRenderList> collection = new ArrayList<>();
	private final Object lockCollection = new Object();

	public GLRenderList getNewRenderList() {
		GLRenderList list = new GLRenderList();
		synchronized (lockCollection) {
			collection.add(list);
		}
		return list;
	}

	@OnlyIn(Dist.CLIENT)
	public void tick() {
		synchronized (lockCollection) {
			List<GLRenderList> newCollection = new ArrayList<>(collection);
			collection.stream().filter(ref -> !ref.check()).forEach(ref -> {
				GLAllocation.deleteDisplayLists(ref.getID());
				newCollection.remove(ref);
			});
			if (newCollection.size() != collection.size()) {
				collection = newCollection;
			}
		}
	}
}
