module imagesort {
    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires javafx.controls;
    requires transitive java.prefs;
    requires java.desktop;
    requires metadata.extractor;

    exports com.github.racopokemon.imagesort;
}