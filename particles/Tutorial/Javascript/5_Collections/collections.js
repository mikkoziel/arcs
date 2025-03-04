/**
 * @license
 * Copyright 2019 Google LLC.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * Code distributed by Google as part of this project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
defineParticle(({SimpleParticle, html}) => {
  return class extends SimpleParticle {
    get template() {
      // This template defines a subtemplate called "person". By filling in the "people" placeholder with a special construction given below in
      // the render() method, we can apply the "person" template on every element in our input list (which here turns it into an <li> element).
      return html`
        Hello to everyone:
        <ul>{{people}}</ul>

        <template person>
          <!-- This template is given a model object. It can access the properties on that model via the usual placeholder syntax. -->
          <li>Hello <span>{{name}}</span>, age <span>{{age}}</span>!</li>
        </template>
      `;
    }

    shouldRender(props) {
      return props && props.inputData;
    }

    // inputData is a list of PersonDetails objects.
    render({inputData}) {
      return {
        // This will fill in the "people" placeholder in the template above. We construct an object with special properties named "$template"
        // and "models", which defines how to render each item in the list.
        people: {
          // $template gives the name of the template to use to render each element.
          $template: 'person',
          // Each model in this list will get passed into the person template. The template can access the properties in this model (here, name
          // and age) via placeholders.
          models: inputData.map(personDetails => ({name: personDetails.name, age: personDetails.age})),
        }
      };
    }
  };
});
