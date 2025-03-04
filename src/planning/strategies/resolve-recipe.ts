/**
 * @license
 * Copyright (c) 2018 Google Inc. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * Code distributed by Google as part of this project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */

import {ResolveWalker} from '../../runtime/recipe-resolver.js';
import {StrategizerWalker, Strategy, StrategyParams} from '../strategizer.js';

export class ResolveRecipe extends Strategy {

  async generate(inputParams: StrategyParams) {
    return StrategizerWalker.over(this.getResults(inputParams),
      new ResolveWalker(ResolveWalker.Permuted, this.arcInfo), this);
  }
}
