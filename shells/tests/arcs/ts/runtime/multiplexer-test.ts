/**
 * @license
 * Copyright (c) 2017 Google Inc. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * Code distributed by Google as part of this project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */

import {assert} from '../../../../../build/platform/chai-web.js';
import {Loader} from '../../../../../build/platform/loader.js';
import {Manifest} from '../../../../../build/runtime/manifest.js';
import {checkDefined} from '../../../../../build/runtime/testing/preconditions.js';
import {EntityType} from '../../../../../build/types/lib-types.js';
import {Runtime} from '../../../../../build/runtime/runtime.js';
import '../../../../lib/arcs-ui/dist/install-ui-classes.js';

describe('Multiplexer', () => {
  it('processes multiple inputs', async () => {
    const runtime = new Runtime();
    const manifest = await runtime.parse(`
      import 'shells/tests/artifacts/Common/Multiplexer.manifest'
      import 'shells/tests/artifacts/test-particles.manifest'

      recipe
        slot0: slot 'rootslotid-slotid'
        handle0: use 'test:1'
        Multiplexer
          hostedParticle: ConsumerParticle
          annotation: consumes slot0
          list: reads handle0
    `);

    const recipe = manifest.recipes[0];
    const barType = checkDefined(manifest.findTypeByName('Bar')) as EntityType;

    const arcInfo = await runtime.allocator.startArc({arcName: 'test'});
    const barStore = await arcInfo.createStoreInfo(barType.collectionOf(), {id: 'test:1'});
    const barHandle = await runtime.host.handleForStoreInfo(barStore, arcInfo);
    recipe.handles[0].mapToStorage(barStore);
    assert(recipe.normalize(), 'normalize');
    assert(recipe.isResolved());

    await runtime.allocator.runPlanInArc(arcInfo, recipe);
    const arc = runtime.getArcById(arcInfo.id);
    await arc.idle;

    await barHandle.add(new barHandle.entityClass({value: 'one'}));
    await barHandle.add(new barHandle.entityClass({value: 'two'}));
    await barHandle.add(new barHandle.entityClass({value: 'three'}));

    await arc.idle;
  });
});
