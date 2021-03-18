# localize

A stupid simple, flexible and extremely lightweight localization framework for Java 1.8+.

## Locale Providers

For every locale you want to support, you have to provide a `LocationProvider`. Those are responsible for retrieving the translation strings and
discarding them when no longer needed (to free memory). By default, the following providers are available:

* `YamlLocaleProvider` (through `localize-yaml`) &mdash; Reads the strings from a yaml file using either nested keys or keys with dot notation.
* `JsonLocaleProvider` (through `localize-json`) &mdash; Reads the strings from a json file using either nested keys or keys with dot notation.

### Registering Locale Providers

```kotlin
// Kotlin
LocalizeService.provideLocale("en_EN", YamlLocaleProvider(Paths.get("./localize-example/locales/en_EN.yaml")))
```

```java
// Java
LocalizeService.INSTANCE.provideLocale("en_EN", new YamlLocaleProvider(Paths.get("./localize-example/locales/en_EN.yaml")));
```

### Custom Locale Providers

Creating a custom locale provider is dead simple. Create a class extending the `BaseLocaleProvider` (or implement the `LocaleProvider` interface
if you need more flexibility).

When extending the `BaseLocaleProvider` you do not have to worry about the concurrency or blocking of threads. Simply override the `loadStrings`
method to contain your custom retrieval logic, and you are good to go.

```kotlin
class CustomLocaleProvider : BaseLocaleProvider() {

    override fun loadStrings(): Map<String, String> {
        // Retrieve and return your translation strings here.
    }

}
```

The `LocalizeService` will make sure, that the strings are loaded or being loaded, when required.


## Fallback Locale

In most cases you probably want to define the default locale to fall back to if a given translation is not available.

```kotlin
// Kotlin
LocalizeService.fallbackLocale = "en_EN"
```

```java
// Java
LocalizeService.INSTANCE.setFallbackLocale("en_EN");
```

Make sure to register the corresponding locale provider _before_ setting the fallback locale. 


## Translating Strings

### Simple Translations

For the most basic use case, the LocalizeService provides the `translate` function. This allows you to translate any key for the given locale.

```kotlin
// Kotlin
LocalizeService.translate("en_EN", "common.hello", "Bob").thenAccept(::println) // Hello, Bob!
```

```java
// Java
LocalizeService.INSTANCE.translate("en_EN", "common.hello", "Bob").thenAccept(System.out::println) // Hello, Bob!
```

Because the strings may have to be loaded, this function returns a `CompletableFuture` containing the translated string. If you would like to wait
for the translation synchronously, use `translate(...).get()`.

### Context-based Translations

In the previous example, you would have to determine the locale to use for every use case. This however, is impractical when dealing with multiple
users or frontends with different non-volatile locales.

To bypass this issue, helper functions for context-based translations are provided. When using kotlin, extension functions can be used to assign and
retrieve the locale of any object. In Java, those are replaced with static helpers.

```kotlin
// Kotlin
tenant.setLocale("de_DE")
tenant.getLocale() // de_DE
tenant.translate("common.hello", tenant.name).thenAccept(...)
tenant.translateSync("common.hello", tenant.name) // blocking
```

```java
// Java
TranslationHelpersKt.setLocale(tenant, "de_DE");
TranslationHelpersKt.getLocale(tenant); // de_DE
TranslationHelpersKt.translate(tenant, "common.hello", tenant.name).thenAccept(...);
TranslationHelpersKt.translateSync(tenant, "common.hello", tenant.name) // blocking
```

When a locale is assigned to at least one context object, the provider will preload all translation strings. The `LocalizeService` uses weak 
references for caching context objects and their locales. When all assigned context objects are garbage collected, the provider will clear the
loaded translations strings to free memory.

### Translation Format

Translation strings may contain placeholders for any amount of given parameters. Those can either be expressed as default Java string format
placeholders (`%1$s`, `%2$s`, ...) or as the custom localize placeholders (`{1}`, `{2}`, ...). Both placeholder types start at **one**. This is due
to technical limitations of the Java string formatter.

Valid examples:
```
"Hello {1}!"
"Hello %1$s!"
"Hello {1} and %2$s!"
```
