/*
 * Copyright 2020 Google LLC.
 *
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 *
 * Code distributed by Google as part of this project is also subject to an additional IP rights
 * grant found at
 * http://polymer.github.io/PATENTS.txt
 */
package arcs.core.host

import arcs.core.analytics.Analytics
import arcs.core.common.Id
import arcs.core.common.Referencable
import arcs.core.common.toArcId
import arcs.core.crdt.CrdtSet
import arcs.core.crdt.CrdtSingleton
import arcs.core.data.Capability.Ttl
import arcs.core.data.CollectionType
import arcs.core.data.EntityType
import arcs.core.data.HandleMode
import arcs.core.data.Schema
import arcs.core.data.SingletonType
import arcs.core.entity.CollectionHandle
import arcs.core.entity.CollectionProxy
import arcs.core.entity.Entity
import arcs.core.entity.EntityDereferencerFactory
import arcs.core.entity.EntityStorageAdapter
import arcs.core.entity.ForeignReferenceChecker
import arcs.core.entity.Handle
import arcs.core.entity.HandleContainerType
import arcs.core.entity.HandleDataType
import arcs.core.entity.HandleSpec
import arcs.core.entity.ReadCollectionHandle
import arcs.core.entity.ReadQueryCollectionHandle
import arcs.core.entity.ReadSingletonHandle
import arcs.core.entity.ReadWriteCollectionHandle
import arcs.core.entity.ReadWriteQueryCollectionHandle
import arcs.core.entity.ReadWriteSingletonHandle
import arcs.core.entity.ReferenceStorageAdapter
import arcs.core.entity.SingletonHandle
import arcs.core.entity.SingletonProxy
import arcs.core.entity.Storable
import arcs.core.entity.StorageAdapter
import arcs.core.entity.WriteCollectionHandle
import arcs.core.entity.WriteQueryCollectionHandle
import arcs.core.entity.WriteSingletonHandle
import arcs.core.storage.StorageEndpointManager
import arcs.core.storage.StorageKey
import arcs.core.storage.StorageProxyImpl
import arcs.core.storage.StoreOptions
import arcs.core.storage.WriteOnlyStorageProxyImpl
import arcs.core.storage.referencemode.ReferenceModeStorageKey
import arcs.core.util.FORBIDDEN_STRINGS
import arcs.core.util.Scheduler
import arcs.core.util.Time
import arcs.core.util.guardedBy
import java.lang.IllegalStateException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Creates [Entity] handles based on [HandleMode], such as
 * [ReadSingletonHandle] for [HandleMode.Read]. To obtain a [HandleHolder], use
 * `arcs_kt_schema` on a manifest file to generate a `{ParticleName}Handles' class, and
 * invoke its default constructor, or obtain it from the [BaseParticle.handles] field.
 *
 * The [scheduler] provided to the [HandleManagerImpl] at construction-time will be shared across
 * all handles and storage-proxies created by the [HandleManagerImpl].
 *
 * Call [close] on an instance that will no longer be used to ensure that all [StorageProxy]
 * instances created by this [HandleManagerImpl] will also be closed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HandleManagerImpl(
  private val arcId: String = Id.Generator.newSession().newArcId("arc").toString(),
  private val hostId: String = "nohost",
  private val time: Time,
  private val scheduler: Scheduler,
  private val storageEndpointManager: StorageEndpointManager,
  private val idGenerator: Id.Generator = Id.Generator.newSession(),
  private val analytics: Analytics? = null,
  foreignReferenceChecker: ForeignReferenceChecker
) : HandleManager {
  private val proxyMutex = Mutex()
  private val singletonStorageProxies by guardedBy(
    proxyMutex,
    mutableMapOf<StorageKey, SingletonProxy<Referencable>>()
  )
  private val collectionStorageProxies by guardedBy(
    proxyMutex,
    mutableMapOf<StorageKey, CollectionProxy<Referencable>>()
  )
  private val dereferencerFactory = EntityDereferencerFactory(
    storageEndpointManager,
    foreignReferenceChecker
  )

  override fun scheduler() = scheduler

  override suspend fun allStorageProxies() = proxyMutex.withLock {
    singletonStorageProxies.values.plus(collectionStorageProxies.values)
  }

  override suspend fun createHandle(
    spec: HandleSpec,
    storageKey: StorageKey,
    ttl: Ttl,
    particleId: String,
    immediateSync: Boolean,
    storeSchema: Schema?,
    actor: String?,
    writeOnly: Boolean
  ): Handle {
    val handleName: String = if (actor.isNullOrEmpty()) {
      idGenerator.newChildId(
        idGenerator.newChildId(arcId.toArcId(), hostId),
        spec.baseName
      ).toString()
    } else {
      actor
    }.also { handleName ->
      if (handleName.findAnyOf(FORBIDDEN_STRINGS) != null) {
        throw IllegalArgumentException(
          "Handle name $handleName contains illegal char in set $FORBIDDEN_STRINGS."
        )
      }
    }

    if (writeOnly && spec.mode.canRead) {
      throw IllegalArgumentException("Readable handle cannot use write only mode.")
    }

    val storageAdapter = when (spec.dataType) {
      HandleDataType.Entity -> {
        EntityStorageAdapter(
          handleName,
          idGenerator,
          spec.entitySpecs.single(),
          ttl,
          time,
          dereferencerFactory,
          storageKey,
          storeSchema
        )
      }
      HandleDataType.Reference -> {
        require(storageKey !is ReferenceModeStorageKey) {
          "Reference-mode storage keys are not supported for reference-typed handles."
        }
        ReferenceStorageAdapter(
          spec.entitySpecs.single(),
          dereferencerFactory,
          ttl,
          time,
          storageKey
        )
      }
    }

    val config = HandleConfig(
      handleName,
      spec,
      storageKey,
      storageAdapter,
      particleId,
      immediateSync,
      writeOnly
    )
    return createHandle(config)
  }

  /** Overload of [createHandle] parameterized by a type [R] of the data that is to be stored. */
  private suspend fun <E : I, I : Storable, R : Referencable> createHandle(
    config: HandleConfig<E, I, R>
  ): Handle = when (config.spec.containerType) {
    HandleContainerType.Singleton -> createSingletonHandle(config)
    HandleContainerType.Collection -> createCollectionHandle(config)
  }

  /** Close all [StorageProxy] instances in this [HandleManagerImpl]. */
  override suspend fun close() {
    proxyMutex.withLock {
      // Needed to avoid receiving ModelUpdate after Proxy closed error
      scheduler.waitForIdle()
      singletonStorageProxies.values.forEach {
        it.waitForIdle()
        it.close()
      }
      collectionStorageProxies.values.forEach {
        it.waitForIdle()
        it.close()
      }
      singletonStorageProxies.clear()
      collectionStorageProxies.clear()
    }
    scheduler.close()
  }

  data class HandleConfig<E : I, I : Storable, R : Referencable>(
    val handleName: String,
    val spec: HandleSpec,
    val storageKey: StorageKey,
    val storageAdapter: StorageAdapter<E, I, R>,
    val particleId: String,
    val immediateSync: Boolean,
    val writeOnly: Boolean
  )

  private suspend fun <E : I, I : Storable, R : Referencable> createSingletonHandle(
    config: HandleConfig<E, I, R>
  ): Handle {
    require(!config.writeOnly) { "Singleton handles not supported in write-only mode." }
    val singletonConfig = SingletonHandle.Config(
      name = config.handleName,
      spec = config.spec,
      proxy = singletonStoreProxy(
        config.storageKey,
        config.spec.entitySpecs.single().SCHEMA
      ),
      storageAdapter = config.storageAdapter,
      dereferencerFactory = dereferencerFactory,
      particleId = config.particleId
    )

    val singletonHandle = SingletonHandle(singletonConfig)
    if (config.immediateSync) {
      singletonConfig.proxy.maybeInitiateSync()
    }
    return when (config.spec.mode) {
      HandleMode.Read -> object : ReadSingletonHandle<E> by singletonHandle {}
      HandleMode.Write -> object : WriteSingletonHandle<I> by singletonHandle {}
      HandleMode.ReadWrite -> object : ReadWriteSingletonHandle<E, I> by singletonHandle {}
      else ->
        throw IllegalArgumentException("Singleton Handles do not support mode ${config.spec.mode}")
    }
  }

  private suspend fun <E : I, I : Storable, R : Referencable> createCollectionHandle(
    config: HandleConfig<E, I, R>
  ): Handle {
    val collectionConfig = CollectionHandle.Config(
      name = config.handleName,
      spec = config.spec,
      proxy = collectionStoreProxy(
        config.storageKey,
        config.spec.entitySpecs.single().SCHEMA,
        config.writeOnly
      ),
      storageAdapter = config.storageAdapter,
      dereferencerFactory = dereferencerFactory,
      particleId = config.particleId
    )
    val collectionHandle = CollectionHandle(collectionConfig)
    if (config.immediateSync) {
      collectionConfig.proxy.maybeInitiateSync()
    }
    return when (config.spec.mode) {
      HandleMode.Read -> object : ReadCollectionHandle<E> by collectionHandle {}
      HandleMode.Write -> object : WriteCollectionHandle<I> by collectionHandle {}
      HandleMode.Query -> object : ReadQueryCollectionHandle<E, Any> by collectionHandle {}
      HandleMode.ReadWrite -> object : ReadWriteCollectionHandle<E, I> by collectionHandle {}
      HandleMode.ReadQuery -> object : ReadQueryCollectionHandle<E, Any> by collectionHandle {}
      HandleMode.WriteQuery -> object : WriteQueryCollectionHandle<I, Any> by collectionHandle {}
      HandleMode.ReadWriteQuery ->
        object : ReadWriteQueryCollectionHandle<E, I, Any> by collectionHandle {}
    }
  }

  @Suppress("UNCHECKED_CAST")
  private suspend fun <R : Referencable> singletonStoreProxy(
    storageKey: StorageKey,
    schema: Schema
  ): SingletonProxy<R> = proxyMutex.withLock {
    if (collectionStorageProxies.containsKey(storageKey)) {
      throw IllegalStateException(
        "Storage key is already being used for a collection, it cannot be reused for a singleton."
      )
    }
    singletonStorageProxies.getOrPut(storageKey) {
      StorageProxyImpl.create(
        storeOptions = StoreOptions(
          storageKey = storageKey,
          type = SingletonType(EntityType(schema))
        ),
        storageEndpointManager = storageEndpointManager,
        crdt = CrdtSingleton(),
        scheduler = scheduler,
        time = time,
        analytics = analytics
      )
    } as SingletonProxy<R>
  }

  @Suppress("UNCHECKED_CAST")
  private suspend fun <R : Referencable> collectionStoreProxy(
    storageKey: StorageKey,
    schema: Schema,
    writeOnly: Boolean
  ): CollectionProxy<R> = proxyMutex.withLock {
    if (singletonStorageProxies.containsKey(storageKey)) {
      throw IllegalStateException(
        "Storage key is already being used for a singleton, it cannot be reused for a collection."
      )
    }

    collectionStorageProxies.getOrPut(storageKey) {
      if (writeOnly) {
        WriteOnlyStorageProxyImpl.create(
          storeOptions = StoreOptions(
            storageKey = storageKey,
            type = CollectionType(EntityType(schema)),
            writeOnly = true
          ),
          storageEndpointManager = storageEndpointManager,
          scheduler
        )
      } else {
        StorageProxyImpl.create(
          storeOptions = StoreOptions(
            storageKey = storageKey,
            type = CollectionType(EntityType(schema))
          ),
          storageEndpointManager = storageEndpointManager,
          crdt = CrdtSet(),
          scheduler = scheduler,
          time = time,
          analytics = analytics
        )
      }
    } as CollectionProxy<R>
  }
}
