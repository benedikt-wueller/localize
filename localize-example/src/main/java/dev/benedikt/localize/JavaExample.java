package dev.benedikt.localize;

import static dev.benedikt.localize.TranslationHelpersKt.*;
import dev.benedikt.localize.json.JsonHttpLocaleProvider;

/**
 * @author Benedikt Wüller
 */
public class JavaExample {

    public static void main(String[] args) throws InterruptedException {
        LocalizeService.INSTANCE.provideLocale("en_EN", new JsonHttpLocaleProvider("https://raw.githubusercontent.com/i18next/ng-i18next/master/examples/locales/dev/translation.json"));
        LocalizeService.INSTANCE.setFallbackLocale("en_EN");

        trans("en_EN", "directive.cloak.description").thenAccept(System.out::println);

        Thread.sleep(5000);
    }

}
