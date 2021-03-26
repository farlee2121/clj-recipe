
## Motivation

I want to improve my clojure skills
- partially to prepare for a potential consulting project
- partially to understand the "symbolic" computing paradigm

## Requirements & constraints
This project should
- require a full-stack (api, logic, storage, maybe UI)
- use datomic in some way
- Try to coordinate a few different modules
  - require some composition
- Include tests

Domain requirements
- View a list of recipes
- Add a new recipe
  - Title, ingredients, instructions
- View recipe details
- Update a recipe
- Delete a recipe
- rate a recipe?

current domain requirements don't seem to require much coordination... We'll start here though. We can add later

## Knowns & Unknowns

U: How to use datomic
K: basic idea of building an API with Clojure. See clj-test repository for example
- http://clojure-doc.org/articles/tutorials/basic_web_development.html

U: Architecture and overall design
- It seems to make sense that I use what I learned from Domain Modeling Made Functional. 
  - Con: This will increase the cognitive load
  - Pro: but it will give me another pass at the style of design
  - Pro: the pure style fits well with functional and clojure ideals
  - Pro: I've got a partial event storm available for a recipe app. Reduces the domain considerations
- A: use transform and event style modeling

K: Basic experience with spec
U: Need better concept of testing based on spec (generative testing)

U: How do I want to represent the recipe parts?
- OPT: Individual values
  - pro: easier for me to validate
  - con: recipes are often pretty free-form
- OPT: Separate markdown sections
  - pro: can differentiate ingredient list from steps just by which field they added it to
  - pro: gain powerful formatting with minimal effort
    - e.g. they can now add sections for parts of the recipe like Sauce Ingredients
  - pro: simpler UI and data model for steps and ingredients
  - con: easy for them to enter nonsense
  - con: need to sanitize the input
  - con?: They can add pretty much whatever, it need not be a recipe
  - con: maybe harder to dig out info. I also can't really migrate away once I choose this route
  - pro: better matches the pretty free-form nature of recipes
  - Q: Would it feel natural for users to enter different
    - A: this is a sample for myself. I think it's fine
  - Q: should I strip image links?
    - seems wise. I don't plan to self-host the images
    - A: I can come back to this if I feel like it. Not high-priority
- OPT: just one markdown field
  - Let's them decide the key sections... Seems a bit too freeform. Maybe nice for the author, less consistent for end users
- Decided: use two markdown fields  

Q: Does clojure have a good library for sanitizing markdown?
- https://github.com/yogthos/markdown-clj
- https://github.com/alxlit/autoclave
- A: there are some options


## Plan

- [x] Define my events and transforms
- [ ] Create a spec model
- [ ] Implement the basic CRUD transforms and read models
- [ ] Map events to datomic store
- [ ] Wrap the transforms with API methods
- [ ] Look into adding ratings

## Domain model

```fs
type SanitizedMarkdown = Markdown of String
type RecipeTitle = RecipeTitle of String // limitations: len 100, chars: [letters, spaces, hyphens]
type RecipeId = RecipeId of Guid

type UnvalidatedRecipe = {
    Title: string;
    Ingredients: string;
    Instructions: string
}

type ValidatedRecipe = {
    Id: RecipeId;
    Title: RecipeTitle;
    Ingredients: SanitizedMarkdown;
    Instructions: SanitizedMarkdown
}

type ValidateRecipe = RecipeId, UnvalidatedRecipe -> ValidatedRecipe
type SanitizeMarkdown = string -> SanitizedMarkdown
type ValidateTitle = string -> RecipeTitle

type RecipeCreated = RecipeCreated of ValidatedRecipe
type CreateRecipeError = 
    // This should probably be broken down into error types per field
    | MissingTitle
    | InvalidTitle
    | MissingIngredients
    | MissingInstructions
type CreateRecipeCommand = UnvalidatedRecipe
type CreateRecipe = CreateRecipeCommand -> Result<RecipeCreated, CreateRecipeError>

type UpdateRecipeCommand =  {
    Id: RecipeId;
    Recipe: UnvalidatedRecipe
}
type RecipeUpdated = RecipeUpdated of ValidatedRecipe
type UpdateRecipeError = 
    | UnknownRecipe
    // the other validation errors
type UpdateRecipe = UpdateRecipeCommand -> Result<RecipeUpdated, UpdateRecipeError>

type DeleteRecipeCommand = RecipeId
type RecipeDeleted = RecipeDeleted of RecipeId
type DeleteRecipeError = 
    | UnknownRecipe
type DeleteRecipe = DeleteRecipeCommand -> Result<RecipeDeleted, DeleteRecipeError>

module ReadModels = 
    type RecipeListItem = {
        Id: RecipeId
        Title: RecipeTitle
    }
    type RecipeDetails = {
        Id: RecipeId;
        Title: RecipeTitle;
        Ingredients: Markdown;
        Instructions: Markdown
    }

type ListRecipes = unit -> ReadModels.RecipeListItem list

type ViewRecipeDetails = RecipeId -> ReadModels.RecipeDetails

```


## Spec learnings

https://clojure.org/guides/spec


I like how function specs `fdef` effect documentation and compiler warnings. 
- this is true integrated design-by-contract
- `defn` supports a weaker `{:pre ... :post ...}` paradigm
  - pre and post don't show in docs :(

Q: does `fdef` need to applied to an existing function definition?
- A: sorta, it requires a "symbol naming a function"
  - the fspec can be defined before the function
  - the function must have the same name  
Q: can `fspec` be used to define function specs to test later conformity
- A:? appears to be a no.
```clj
(s/valid? (s/fspec :args string? :ret string?) identity) ;; false

(def nope identity)
(s/valid? (s/fspec :args string? :ret string?) nope) ;; false

(s/conform (s/fspec :args string? :ret string?) identity) ;; invalid

(defn strmap [s] {:pre [(s/valid? string? s)] :post [(s/valid? string? %)]} s)
(s/valid? (s/fspec :args string? :ret string?) strmap) ;; false

(s/fdef strmap :args string? :ret string?)
(s/valid? strmap strmap) ;; true

(s/def ::str-pred (s/fspec :args string? :ret boolean?))
(s/valid ::str-pred (fn [s] true)) ;; false
```

I suppose I could define the function specs in a way that they can be applied to arbitrary functions later
- OPT: bind the keys to a symbol and apply them later using `s/fdef`
- OPT: build each spec into a macro so I can call it on arbirary symbols later

## Datomic learnings

[Tutorial series](https://www.youtube.com/watch?list=PLZdCLR02grLpRgqU50KY3YfMePw1SHnpp&v=faoXSarGgEI&ab_channel=ClojureTV)

### Install

The install process for Datomic was very frustrating. First, I'd expect it to be available via the standard package source: clojars. There are many datomic packages, but they're all unoffical as the framework is no longer distributed that way.

The instructions for dev-local are split between https://docs.datomic.com/cloud/dev-local.html and a link in the email you get with credentials.

The install instructions from the first link don't even work on windows. The second link has three set of *alternative* instructions, but it isn't clear that they are strict alternatives. I ended up concluding that maven, deps.edn, and lein are not friendly with each other. I'm using lein, so following the other directions just ended up leading me to a dead end.

Short version
- add repository configuration to lein
  - this includes potential maven repositories, no need to have maven, a pom.xml, or a deps.edn
- Set environment variables for username and password
  - [or use an encrypted password field](https://github.com/technomancy/leiningen/blob/master/doc/DEPLOY.md#gpg)
  - [or use a profile](https://github.com/technomancy/leiningen/blob/master/doc/DEPLOY.md#full-disk-encryption)
```clj
:repositories [
              ["cognitect-dev-tools" {:url      "https://dev-tools.cognitect.com/maven/releases/"
                                      :username :env/username_var_here
                                      :password :env/pw_var__here }]]
```

And that's all I really needed from the get go. I didn't need to install or download anything


### Datomic querying

It looks like index-pull is the way to traverse whole tables/databases
[index pull doc](https://docs.datomic.com/on-prem/query/index-pull.html#indexes)

There are several main indexes for different purposes
- :aevt when the property is of first importance (like listing all ids)
- :eavt for getting whole entities at a time
- :avet for lookup of all instances of a field that contain a certain value
- :vaet value lookup is of first importance (more what I think of a traditional sql index)

## Test API

I want to bring test API over to clojure. The test portability isn't something i'm willing to give up.
Clojure does not have records or interfaces. My experiments also showed limitations in spec for functions. I don't think prevent me from just making a map of functions that meet a signature.

I asked some StackOverflow questions about constrained function groupings
- https://stackoverflow.com/questions/66555972/can-i-validate-functions-with-clojure-spec
- https://stackoverflow.com/questions/66555904/can-clojure-spec-be-used-to-constrain-protocol-argument-and-return-types


It currently appears that I cannot really constrain the test api inputs and outputs. This means that I can use a protocol or map to define the set of operations, but I can't really give the tester any information about the expected data.


## REPL Learnings

A few things that ate time or have tripped me up more than once

If a namespace won't load **look real hard at the file name**. Make sure it doesn't have any dashes

Aliasing apparently doesn't work for individual functions. For example, none of the following work
- `[java.util.UUID/randomUUID :as new-id]`
- `[java.util.UUID :as id]`
- `[java.util :refer [UUID :as id]]`

They throw the unhelpful error 
```
Syntax error macroexpanding clojure.core/ns at ... - failed: Extra input spec: :clojure.core.specs.alpha/ns-form
```

switch active namespace if you want to test relative to a different file using `in-ns`