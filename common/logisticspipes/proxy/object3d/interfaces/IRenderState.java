package logisticspipes.proxy.object3d.interfaces;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface IRenderState {

	void reset();

	void setAlphaOverride(int i);

	void draw();

	void setBrightness(IWorld world, BlockPos pos);

	@OnlyIn(Dist.CLIENT)
	void startDrawing(int mode, VertexFormat format);

}
