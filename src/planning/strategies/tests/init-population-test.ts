/**
 * @license
 * Copyright (c) 2018 Google Inc. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * Code distributed by Google as part of this project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */


import {assert} from '../../../platform/chai-web.js';
import {Manifest} from '../../../runtime/manifest.js';
import {Loader} from '../../../platform/loader.js';
import {InitPopulation} from '../../strategies/init-population.js';
import {StrategyTestHelper} from '../../testing/strategy-test-helper.js';
import {ArcId} from '../../../runtime/id.js';
import {Entity} from '../../../runtime/entity.js';
import {SingletonType} from '../../../types/lib-types.js';
import {Runtime} from '../../../runtime/runtime.js';
import {Flags} from '../../../runtime/flags.js';

describe('InitPopulation', () => {
  it('penalizes resolution of particles that already exist in the arc', async () => {
    const manifest = await Manifest.parse(`
      schema Product

      particle A in 'A.js'
        product: reads Product

      recipe
        handle1: create *
        A
          product: reads handle1`);
    const loader = new Loader(null, {
      'A.js': 'defineParticle(({Particle}) => class extends Particle {})'
    });
    const recipe = manifest.recipes[0];
    assert(recipe.normalize());
    const runtime = new Runtime({loader, context: manifest});
    const arcInfo = await runtime.allocator.startArc({arcName: 'test-plan-arc'});

    async function scoreOfInitPopulationOutput() {
      const results = await new InitPopulation(arcInfo, StrategyTestHelper.createTestStrategyArgs(
          arcInfo, {contextual: false})).generate({generation: 0});
      assert.lengthOf(results, 1);
      return results[0].score;
    }

    assert.strictEqual(await scoreOfInitPopulationOutput(), 1);
    await runtime.allocator.runPlanInArc(arcInfo, recipe);
    assert.strictEqual(await scoreOfInitPopulationOutput(), 0);
  });

  it('reads from RecipeIndex', async () => {
    const manifest = await Manifest.parse(`
      particle A
      recipe
        A`);

    const [recipe] = manifest.recipes;
    assert(recipe.normalize());

    const loader = new Loader(null, {
      'A.js': 'defineParticle(({Particle}) => class extends Particle {})'
    });
    const runtime = new Runtime({loader, context: new Manifest({id: ArcId.newForTest('test')})});
    const arc = runtime.getArcById((await runtime.allocator.startArc({arcName: 'test-plan-arc'})).id);

    const results = await new InitPopulation(arc.arcInfo, {contextual: false,
        recipeIndex: {recipes: manifest.recipes}}).generate({generation: 0});
    assert.lengthOf(results, 1);
    assert.strictEqual(results[0].result.toString(), recipe.toString());
  });

  it('contextual population has recipes matching arc handles and slots', async () => {
    const manifest = await Manifest.parse(`
      schema Burrito

      // Binds to handle Burrito
      particle EatBurrito
        burrito: reads Burrito
      recipe EatBurrito
        EatBurrito

      // Binds to slot tortilla
      particle FillsTortilla
        tortilla: consumes Slot
      recipe FillsTortilla
        FillsTortilla

      // Provides handle Burrito and slot tortilla
      particle BurritoRestaurant
        burrito: writes Burrito
        root: consumes Slot
          tortilla: provides? Slot
      recipe BurritoRestaurant
        burrito: create *
        BurritoRestaurant
          burrito: writes burrito

      schema Burger

      // Binds to handle Burger
      particle EatBurger
        burger: reads Burger
      recipe EatBurger
        EatBurger

      // Binds to slot bun
      particle FillsBun
        bun: consumes Slot
      recipe FillsBun
        FillsBun

      // Provides handle Burger and slot bun
      particle BurgerRestaurant
        burger: writes Burger
        root: consumes Slot
          bun: provides? Slot
      recipe BurgerRestaurant
        burger: create *
        BurgerRestaurant
          burger: writes burger
    `);

    const arcInfo = await StrategyTestHelper.createTestArcInfo(manifest);

    async function openRestaurantWith(foodType: string) {
      const restaurant = manifest.recipes.find(recipe => recipe.name === `${foodType}Restaurant`);
      restaurant.normalize();
      restaurant.mergeInto(arcInfo.activeRecipe);
    }

    let results = await new InitPopulation(arcInfo, StrategyTestHelper.createTestStrategyArgs(
      arcInfo, {contextual: true})).generate({generation: 0});
    assert.lengthOf(results, 0, 'Initially nothing is available to eat');

    await openRestaurantWith('Burrito');

    results = await new InitPopulation(arcInfo, StrategyTestHelper.createTestStrategyArgs(
        arcInfo, {contextual: true})).generate({generation: 0});
    assert.sameMembers(results.map(r => r.result.name), [
      'FillsTortilla',
      'EatBurrito'
    ], 'After a Burrito restaurant opened, tortilla wrapped goodness can be consumed');

    await openRestaurantWith('Burger');
    results = await new InitPopulation(arcInfo, StrategyTestHelper.createTestStrategyArgs(
        arcInfo, {contextual: true})).generate({generation: 0});
    assert.lengthOf(results, 4, );
    assert.sameMembers(results.map(r => r.result.name), [
      'FillsTortilla',
      'FillsBun',
      'EatBurrito',
      'EatBurger'
    ], 'Eventually both a burrito and a burger can be enjoyed');

    results = await new InitPopulation(arcInfo, StrategyTestHelper.createTestStrategyArgs(
        arcInfo, {contextual: true})).generate({generation: 1});
    assert.lengthOf(results, 0, 'Food is only served once');
  });
});
