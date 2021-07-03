package logisticspipes.pipefxhandlers;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;

public interface ParticleProvider {

	float red = 1;
	float green = 1;
	float blue = 1;

	Particle createGenericParticle(ClientWorld world, double x, double y, double z, int amount);

}
