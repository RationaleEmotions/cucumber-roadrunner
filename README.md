
```
8888888b.                         888                                                           
888   Y88b                        888                                                           
888    888                        888                                                           
888   d88P  .d88b.   8888b.   .d88888       888d888 888  888 88888b.  88888b.   .d88b.  888d888 
8888888P"  d88""88b     "88b d88" 888       888P"   888  888 888 "88b 888 "88b d8P  Y8b 888P"   
888 T88b   888  888 .d888888 888  888       888     888  888 888  888 888  888 88888888 888     
888  T88b  Y88..88P 888  888 Y88b 888       888     Y88b 888 888  888 888  888 Y8b.     888     
888   T88b  "Y88P"  "Y888888  "Y88888       888      "Y88888 888  888 888  888  "Y8888  888     
                                                                                                
                                                                                                
```

# Road Runner 

This library is a PoC like implementation which lets you use TestNG to run your Cucumber based BDD tests in Parallel.
Well, there are already implementations and approaches available on the internet which lets you do that. So what's so 
special about **Road-Runner**.

**Road-Runner** unlike other implementations, lets you :

* Run your **Scenarios** in parallel.
* Provides you with a threadsafe version of the same reports that cucumber provides viz., **JSON, and Html**

## Pre-requisite

* JDK-8 or higher.
* Hard-wired versions of the following cucumber artifacts:
    * `info.cukes:cucumber-testng:jar:1.2.5`. See [here](http://central.maven.org/maven2/info/cukes/cucumber-testng/1.2.5/)
    * `info.cukes:cucumber-java:jar:1.2.5:compile`. See [here](http://central.maven.org/maven2/info/cukes/cucumber-java/1.2.5/)

## Why the hard-wired dependency on Cucumber ?

The implementation hacks into the cucumber codebase and resorts to **Classpath overriding[1]** to hook in the support for parallel scenario execution. So if the version is upgraded, then the entire logic of parallel execution support will fall apart. This was done specifically because Cucumber implementation is Java is not **thread-safe** yet.

## Getting started with cucumber-roadrunner.

### Add a dependency to the artifact via

```xml
<dependency>
    <groupId>com.rationaleemotions</groupId>
    <artifactId>roadrunner</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Create a TestNG runner that looks like below:

```java
@CucumberOptions(plugin = {"html:output/old", "pretty:output/old/output.txt", "json:output/old/output.json"})
@Reporters(plugin = {
        "html:output/new/cucumber-html-report",
        "json:output/new/cucumber-report.json"
})
public class ShippingRunCukesTest extends TestNGCucumberTests {
}
```

You can provide all the options that are part of `--options` as detailed [here](https://cucumber.io/docs/reference/jvm#list-configuration-options) via the annotation `@CucumberOptions`.

`Cucumber-roadrunner` provides one extra annotation called `@Reporters`. This annotation is specifically used to wire in **thread-safe** variants of the default cucumber reporters.

Currently the following two reporters are supported.

1. Thread-safe HTML report.

This can be injected via `"html:output/new/cucumber-html-report"`. This causes RoadRunner to generate the new reports  in the folder `output/new/cucumber-html-report`. The folder value is customizable and can be changed to anything that you desire.

2. Thread-safe JSON Report.
This can be injected via `"html:output/new/cucumber-html-report"`. This causes RoadRunner to generate the new reports  in the folder `output/new/` inside a file called `cucumber-report.json`. The folder name and file names are customizable and can be changed to anything that you desire.

**Note:** Both the reports have the same look and feel as that of the default reports.

### Controlling parallelism.

By default this library enables scenario level parallel execution. This can be disabled via the JVM argument `-Droadrunner.parallel=false`.

By default the threadpool size is 10. But this can be altered to anything via the JVM argument `-Droadrunner.threads=15` (This causes the threadpool size to be `15`).


## Further customization.

You can wire in your own threadsafe plugins (formatter or reporter) using [Service Provider Interface](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html) approach. 

In order to do this, here's the set of steps.

1. Implement the interface `com.rationaleemotions.PluginMapper`
2. Now either within `src/main/resources` (OR) `src/test/resources`, create a file `META-INF/services/com.rationaleemotions.PluginMapper` and add references of the implementation from (1) into this file.

Roadrunner will now start wiring in your threadsafe reporters as well.

**[1] Classpath overriding** - *The process of duplicating a class from a jar to the local project, wherein the original class is created in the local project with the same project structure, but the contents are altered.*
