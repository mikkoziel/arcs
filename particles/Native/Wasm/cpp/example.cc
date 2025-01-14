/**
 * @license
 * Copyright (c) 2019 Google Inc. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * Code distributed by Google as part of this project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
#include "src/wasm/cpp/arcs.h"
#include "particles/Native/Wasm/cpp/example.h"  // generated by schema2wasm

class BasicParticle : public AbstractBasicParticle {
public:
  BasicParticle() {
    autoRender();  // automatically render once handles are ready, and then when updated
  }

  // Rendering with template and key/value model
  std::string getTemplate(const std::string& slot_name) override {
    return R"(
      <div>Product is <b>{{name}}</b> <i>(sku <span>{{sku}}</span>)</i></div>
      <button on-click="clicky">Copy to output store</button> <span>{{num}}</span> clicks
      <br>
    )";
  }

  void populateModel(const std::string& slot_name, arcs::Dictionary* model) override {
    if (const arcs::BasicParticle_Foo* product = foo_.get()) {
      model->emplace("name", product->name());
      model->emplace("sku", arcs::num_to_str(product->sku()));
      model->emplace("num", arcs::num_to_str(num_clicks_));
    }
  }

  // Responding to UI events
  void fireEvent(const std::string& slot_name, const std::string& handler,
                 const arcs::Dictionary& eventData) override {
    if (handler != "clicky") {
      if (const arcs::BasicParticle_Foo* product = foo_.get()) {
        arcs::BasicParticle_Foo copy = arcs::clone_entity(*product);  // does not copy internal id
        bar_.store(copy);    // 'copy' will be updated with a new internal id

        // Basic printf-style logging; note the c_str() for std::string variables
        console("Product copied; new id is %s\n", arcs::entity_to_str(copy).c_str());

        num_clicks_++;
        renderSlot("root", false, true);  // update display
      }
    }
  }

private:
  int num_clicks_ = 0;
};


class Watcher : public AbstractWatcher {
public:
  Watcher() {
    autoRender();
  }

  std::string getTemplate(const std::string& slot_name) override {
    return "<div><br>Watcher has seen <span>{{num}}</span> copies...<div>";
  }

  void populateModel(const std::string& slot_name, arcs::Dictionary* model) override {
    model->emplace("num", arcs::num_to_str(bar_.size()));
  }
};


// Set up the wasm API functions for creating each particle
DEFINE_PARTICLE(BasicParticle)
DEFINE_PARTICLE(Watcher)
