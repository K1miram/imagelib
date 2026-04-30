# How to use it
To be able to use it you need to add jitpack repository to your build.gradle file:
```
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```
And then add mod to dependencies:

Fabric since 26.1:
```
dependencies {
    implementation "com.github.K1miram.imagelib:fabric:<version>"
}
```
Fabric before 26.1:
```
dependencies {
    modImplementation("com.github.K1miram.imagelib:fabric:<version>") {
        transitive false
    }
}
```
Neoforge since 26.1:
```
dependencies {
    implementation "com.github.K1miram.imagelib:neoforge:<version>"
}
```
Neoforge before 26.1:
```
dependencies {
    implementation("com.github.K1miram.imagelib:neoforge:<version>") {
        transitive false
    }
}
```
Replace <version> with mod version you want to use. Full list of available versions is available at project tags page: https://github.com/K1miram/imagelib/tags

All versions have the same structure: <mod_version>-<minecraft_version>-<patch_number_if_there_is_one>

I recommend to always use the latest mod version

#
To load custom images you need to create ImageLib instance. Then you can download image or gif by using `ImageLib.downloadImage(...)` method. 
To get your image identifier use `ImageLib.getImageId(...)`. 
To get default image size use `ImageLib.getImageSize(...)`. 
To fit image in specific area saving proportions use `ImageLib.fitImageSize(...)`. 

You need to use `ImageLib.getImageId(...)` every frame because this method returns default image id before your image is registered and also because gif frames have different ids.
