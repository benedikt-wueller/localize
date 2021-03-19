package dev.benedikt.localize;

import static dev.benedikt.localize.TranslationHelpersKt.*;
import dev.benedikt.localize.yaml.YamlLocaleProvider;
import java.nio.file.Paths;

/**
 * @author Benedikt Wüller
 */
public class JavaExample {

    public static void main(String[] args) throws InterruptedException {
        LocalizeService.INSTANCE.provideLocale("en_EN", new YamlLocaleProvider(Paths.get("./localize-example/locales/en_EN.yaml")));
        LocalizeService.INSTANCE.provideLocale("de_DE", new YamlLocaleProvider(Paths.get("./localize-example/locales/de_DE.json")));

        LocalizeService.INSTANCE.setFallbackLocale("en_EN");

        LocalizeService.INSTANCE.translate("de_DE", "common.hello2", "Bob", "Alice").thenAccept(System.out::println);

        trans("de_DE", "common.hello2", "Bob", "Alice").thenAccept(System.out::println);

        Thread.sleep(1000);
    }

}
