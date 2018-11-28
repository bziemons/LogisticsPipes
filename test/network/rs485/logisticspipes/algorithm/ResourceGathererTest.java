/*
 * Copyright (c) 2018  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2018  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.rs485.logisticspipes.algorithm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import network.rs485.logisticspipes.logistic.IDestination;
import network.rs485.logisticspipes.logistic.IProvider;
import network.rs485.logisticspipes.logistic.TransportPromise;
import network.rs485.logisticspipes.resource.IntegerResource;
import network.rs485.logisticspipes.resource.Resource;
import network.rs485.logisticspipes.resource.ResourceGroup;
import network.rs485.logisticspipes.resource.ResourceSet;
import network.rs485.logisticspipes.resource.SingleResource;

public class ResourceGathererTest {

	@Test
	public void testAlgorithmNoResources() {
		IDestination destination = new IDestination() {};

		Stream<Resource<?>> resourceStream = ResourceGatherer.gather(
				Stream.empty(), (o1, o2) -> 0, Collections.emptyList(), destination);

		assertEquals("Resource stream should be empty", 0, resourceStream.toArray().length);
	}

	@Test
	public void testAlgorithmOneResourceNoFilters() {
		IntegerResource integerResource = new IntegerResource(2);
		List<IProvider> resourceProviders = Collections.singletonList(new IProvider() {

			private final SingleResource<Integer> singleIntResource = new SingleResource<>(integerResource);
			private final List<Predicate<IDestination>> destinationFilters = Collections.emptyList();

			@Override
			public ResourceGroup getProvidedResources() {
				return singleIntResource;
			}

			@Override
			public List<Predicate<IDestination>> getDestinationFilters() {
				return destinationFilters;
			}

			@Override
			public TransportPromise request(Collection<Resource<?>> resources) {
				throw new RuntimeException();
			}
		});
		List<Predicate<Resource<?>>> resourceFilters = Collections.emptyList();
		IDestination destination = new IDestination() {};

		Stream<Resource<?>> resourceStream = ResourceGatherer.gather(
				resourceProviders.stream(), (o1, o2) -> 0, resourceFilters, destination);

		List<Resource<?>> resources = resourceStream.collect(Collectors.toList());
		assertEquals("Resource result should be of size 1", 1, resources.size());
		assertEquals("Resource should equal the only available resource", integerResource, resources.get(0));
	}

	@Test
	public void testAlgorithmOneResourceFiltered() {
		IntegerResource searchIntegerResource = new IntegerResource(1);
		IntegerResource providedIntegerResource = new IntegerResource(2);

		List<IProvider> resourceProviders = Collections.singletonList(new IProvider() {

			private final SingleResource<Integer> singleIntResource = new SingleResource<>(providedIntegerResource);
			private final List<Predicate<IDestination>> destinationFilters = Collections.emptyList();

			@Override
			public ResourceGroup getProvidedResources() {
				return singleIntResource;
			}

			@Override
			public List<Predicate<IDestination>> getDestinationFilters() {
				return destinationFilters;
			}

			@Override
			public TransportPromise request(Collection<Resource<?>> resources) {
				throw new RuntimeException();
			}
		});
		List<Predicate<Resource<?>>> resourceFilters = Collections.singletonList(resource -> resource.equals(searchIntegerResource));
		IDestination destination = new IDestination() {};

		Stream<Resource<?>> resourceStream = ResourceGatherer.gather(
				resourceProviders.stream(), (o1, o2) -> 0, resourceFilters, destination);

		assertEquals("Resource stream should be empty", 0, resourceStream.toArray().length);
	}

	@Test
	public void testAlgorithmResourceGroupFiltered() {
		IntegerResource searchIntegerResource = new IntegerResource(1);

		List<IProvider> resourceProviders = Collections.singletonList(new IProvider() {

			private final List<Predicate<IDestination>> destinationFilters = Collections.emptyList();
			private final ResourceSet providedResources = new ResourceSet(Arrays.asList(
					new IntegerResource(1),
					new IntegerResource(2),
					new IntegerResource(3)));

			@Override
			public ResourceGroup getProvidedResources() {
				return providedResources;
			}

			@Override
			public List<Predicate<IDestination>> getDestinationFilters() {
				return destinationFilters;
			}

			@Override
			public TransportPromise request(Collection<Resource<?>> resources) {
				throw new RuntimeException();
			}
		});
		List<Predicate<Resource<?>>> resourceFilters = Collections.singletonList(resource -> resource.equals(searchIntegerResource));
		IDestination destination = new IDestination() {};

		Stream<Resource<?>> resourceStream = ResourceGatherer.gather(
				resourceProviders.stream(), (o1, o2) -> 0, resourceFilters, destination);

		List<Resource<?>> resources = resourceStream.collect(Collectors.toList());
		assertEquals("Resource result should be of size 1", 1, resources.size());
		assertEquals("Resource should be in the result", searchIntegerResource, resources.get(0));
	}
}
