Provide Code Refactor
=====================

Personal notes.

TODO NOW:
---------

* Checkout all unrelated changes
* Refactor: Move network.rs485.logisticspipes.util.* to ..network
* Apply last stash item


General Reference
-----------------

* Request
  - Results in a TransportPromise, if it can be fulfilled

* abstract Resource

* FluidResource extends Resource
  - Amount (int)

* ItemResource extends Resource
  - Amount (int)

* DictionaryResource extends Resource
  - DictionaryMatcher
  - List<ItemResource>
  - Amount (int) ?

* TransportPromise
  - Resource
  - Destination

* abstract Provider
  - Type
    + Crafting
    + Provide
  - Priority?
    + Crafting-Priority
    + Distance
  - Filter?
    + Firewall-Pipes
    
* Provider\<Resource> extends Provider

* Provider-Map on Logistics Network
  - Map\<Resource, List\<Provider>>
  - Distance?

Algorithm
---------

Take Resource, filter provider list by filters, then sort provider list by priority. Use first provider as much as possible, then the next, etc. Should be done lazily.

Crafting should be considered after providing

Crafting Recipes that request their own target Resource should be considered different. They may be used before providers.
- What about options to keep at least x of the Resource to craft again?

Request should be cancallable
- `FutureTask` (which is cancellable)?


Update Nov-15
-------------

* End Result should be Pair<Resource, Provider>
* Request needs to be done on Provider

* **Algorithm**
  - Pair\<Resource, Provider> a = stream.next()
  - Pair\<Resource, Destination> b = needs.findByResource(a.getResource())
  - needs.update(a.getProvider().requestPartial(b.getResource(), b.getDestination()))

* **Needs**
  - request(
    + ItemIdentifierStack item,
    + IRequestItems requester,
    + RequestLog log,
    + EnumSet\<ActiveRequestType> requestFlags,
    + IAdditionalTargetInformation info)
  - Needs{
    + Resource\<V> resource,
    + IDestination destination}
  - ProviderNetwork{
    + List\<Provider\<V>> providers}

* **SinkReply**
  - Comparable (Priority)
    + Sink
    + Terminus
    + Passive Supplier
  - Information for `TransportMode`
    + passive sink
    + default sink
  - Filtering, if default
  - Energy use
  - Max item count (0 = infinte)
  - Inventory fullness detection with BufferMode
  - Chassis Module Index as Additional Info (if applicable)

* **Interest**
  - can be any Resource
  - can match anything (default route)
  - has a priority
