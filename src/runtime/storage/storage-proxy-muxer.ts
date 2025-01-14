/**
 * @license
 * Copyright (c) 2020 Google Inc. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * Code distributed by Google as part of this project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */

import {CRDTTypeRecord} from '../../crdt/lib-crdt.js';
import {ProxyMessage, ProxyCallback, StorageCommunicationEndpoint} from './store-interface.js';
import {PropagatedException} from '../arc-exceptions.js';
import {StorageProxy} from './storage-proxy.js';
import {Dictionary, BiMap, noAwait} from '../../utils/lib-utils.js';
import {Type} from '../../types/lib-types.js';
import {assert} from '../../platform/assert-web.js';
import {StoreInfo} from './store-info.js';
import {CRDTTypeRecordToType} from './storage.js';
import {StorageFrontend} from './storage-frontend.js';

export class StorageProxyMuxer<T extends CRDTTypeRecord> {
  private readonly storageProxies = new BiMap<string, StorageProxy<T>>();
  private readonly callbacks: Dictionary<ProxyCallback<T>> = {};
  private readonly storageKey: string;
  private readonly type: Type;

  constructor(private readonly storageEndpoint: StorageCommunicationEndpoint<T>) {
    this.storageKey = this.storageEndpoint.storeInfo.storageKey.toString();
    this.type = this.storageEndpoint.storeInfo.type;
  }

  get storeInfo(): StoreInfo<CRDTTypeRecordToType<T>> { return this.storageEndpoint.storeInfo; }

  getStorageProxy(muxId: string): StorageProxy<T> {
    this.storageEndpoint.setCallback(this.onMessage.bind(this));
    if (!this.storageProxies.hasL(muxId)) {
      this.storageProxies.set(muxId, new StorageProxy(this.createStorageCommunicationEndpoint(muxId, this.storageEndpoint, this)));
    }
    return this.storageProxies.getL(muxId);
  }

  private createStorageCommunicationEndpoint(muxId: string, storageEndpoint: StorageCommunicationEndpoint<T>, storageProxyMuxer: StorageProxyMuxer<T>): StorageCommunicationEndpoint<T> {
    return {
      get storeInfo(): StoreInfo<CRDTTypeRecordToType<T>> {
        return storageEndpoint.storeInfo;
      },
      async onProxyMessage(message: ProxyMessage<T>): Promise<void> {
        message.muxId = muxId;
        await storageEndpoint.onProxyMessage(message);
      },
      setCallback(callback: ProxyCallback<CRDTTypeRecord>): void {
        storageProxyMuxer.callbacks[muxId] = callback;
      },
      reportExceptionInHost(exception: PropagatedException): void {
        storageEndpoint.reportExceptionInHost(exception);
      },
      getStorageFrontend(): StorageFrontend {
        return storageEndpoint.getStorageFrontend();
      }
    };
  }

  async onMessage(message: ProxyMessage<T>): Promise<void> {
    assert(message.muxId != null);
    if (!this.callbacks[message.muxId]) {
      throw new Error('callback has not been set');
    }
    noAwait(this.callbacks[message.muxId](message));
  }
}
