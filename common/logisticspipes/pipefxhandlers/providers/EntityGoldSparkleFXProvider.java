package logisticspipes.pipefxhandlers.providers;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;

import logisticspipes.pipefxhandlers.GenericSparkleFactory;
import logisticspipes.pipefxhandlers.ParticleProvider;

public class EntityGoldSparkleFXProvider implements ParticleProvider {

	float red = 0.93F;
	float green = 0.80F;
	float blue = 0.36F;

	@Override
	public Particle createGenericParticle(ClientWorld world, double x, double y, double z, int amount) {

		return GenericSparkleFactory.getSparkleInstance(world, x, y, z, red, green, blue, amount);

	}
}
