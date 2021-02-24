
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
  - pro: simplier UI and data model for steps and ingredients
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