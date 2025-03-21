## Dependencies

### HarmonyOS Command-Line Tool Configuration

[Huawei Official Documentation](https://developer.huawei.com/consumer/cn/doc/harmonyos-guides-V13/ide-commandline-get-V13)

Mandatory configuration. The plugin requires this toolkit for packaging.

## Plugin Configuration
Add the following code to settings.gradle.kts:

```
pluginManagement {  
    plugins {  
        id("me.olivine.harmony").version("1.0.0")  
    }  
}

buildscript {  
    dependencies {  
        classpath(files("repo/com/kugou/harmony/kg-harmony/1.0.0/kg-harmony-1.0.0.jar"))  
    }  
}
```
### Apply the plugin:
```
plugins {  
    id("com.kugou.harmony.kg-harmony")  
}
```

### Configure plugin options:
```
harmony {  
  bundle = "kmm_kg" // Output HAR package name  
  workspace = file("../../HMKugou") // HarmonyOS project directory  
  output = file("../../HMKugou/entry/har") // Output HAR target path  
}
```

### Running Tasks
To compile the HAR package, execute
`./gradlew harmonyBuild`

To run the HAP project, execute
`./gradlew harmonyRun `
Add the `-Pfull` parameter for a full compilation.
