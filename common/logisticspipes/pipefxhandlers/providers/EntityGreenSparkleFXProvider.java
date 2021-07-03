package logisticspipes.pipefxhandlers.providers;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;

import logisticspipes.pipefxhandlers.GenericSparkleFactory;
import logisticspipes.pipefxhandlers.ParticleProvider;

public class EntityGreenSparkleFXProvider implements ParticleProvider {

	float red = 0F;
	float green = 1F;
	float blue = 0F;

	@Override
	public Particle createGenericParticle(ClientWorld world, double x, double y, double z, int amount) {

		return GenericSparkleFactory.getSparkleInstance(world, x, y, z, red, green, blue, amount);

	}

}
