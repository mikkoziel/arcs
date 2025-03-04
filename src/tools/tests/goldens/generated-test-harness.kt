/* ktlint-disable */
@file:Suppress("PackageName", "TopLevelName")

package arcs.golden

//
// GENERATED CODE -- DO NOT EDIT
//

import arcs.sdk.ArcsDuration
import arcs.sdk.ArcsInstant
import arcs.sdk.BigInt
import arcs.sdk.Particle
import arcs.sdk.testing.*
import arcs.sdk.toBigInt
import kotlinx.coroutines.CoroutineScope

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GoldTestHarness<P : Particle>(
    factory : (CoroutineScope) -> P
) : BaseTestHarness<P>(factory, listOf(
    arcs.core.entity.HandleSpec(
        "data",
        arcs.core.data.HandleMode.Read,
        arcs.core.data.SingletonType(arcs.core.data.EntityType(Gold_Data.SCHEMA)),
        setOf(Gold_Data)
    ),
    arcs.core.entity.HandleSpec(
        "allPeople",
        arcs.core.data.HandleMode.Read,
        arcs.core.data.CollectionType(arcs.core.data.EntityType(Gold_AllPeople.SCHEMA)),
        setOf(Gold_AllPeople)
    ),
    arcs.core.entity.HandleSpec(
        "qCollection",
        arcs.core.data.HandleMode.ReadQuery,
        arcs.core.data.CollectionType(arcs.core.data.EntityType(Gold_QCollection.SCHEMA)),
        setOf(Gold_QCollection)
    ),
    arcs.core.entity.HandleSpec(
        "alias",
        arcs.core.data.HandleMode.Write,
        arcs.core.data.SingletonType(arcs.core.data.EntityType(Gold_Alias.SCHEMA)),
        setOf(Gold_Alias)
    ),
    arcs.core.entity.HandleSpec(
        "collection",
        arcs.core.data.HandleMode.Read,
        arcs.core.data.CollectionType(arcs.core.data.EntityType(Gold_Collection.SCHEMA)),
        setOf(Gold_Collection)
    )
)) {
    val data: arcs.sdk.ReadWriteSingletonHandle<Gold_Data, Gold_Data_Slice> by handleMap
    val allPeople: arcs.sdk.ReadWriteCollectionHandle<Gold_AllPeople, Gold_AllPeople_Slice> by handleMap
    val qCollection: arcs.sdk.ReadWriteQueryCollectionHandle<Gold_QCollection, Gold_QCollection_Slice, String> by handleMap
    val alias: arcs.sdk.ReadWriteSingletonHandle<Gold_Alias, Gold_Alias_Slice> by handleMap
    val collection: arcs.sdk.ReadWriteCollectionHandle<Gold_Collection, Gold_Collection_Slice> by handleMap
}
