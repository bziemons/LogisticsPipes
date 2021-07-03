package logisticspipes.pipefxhandlers.providers;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;

import logisticspipes.pipefxhandlers.GenericSparkleFactory;
import logisticspipes.pipefxhandlers.ParticleProvider;

public class EntityWhiteSparkleFXProvider implements ParticleProvider {

	@Override
	public Particle createGenericParticle(ClientWorld world, double x, double y, double z, int amount) {

		return GenericSparkleFactory.getSparkleInstance(world, x, y, z, ParticleProvider.red, ParticleProvider.green, ParticleProvider.blue, amount);
	}

}
