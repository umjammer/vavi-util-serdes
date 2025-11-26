[![](https://jitpack.io/v/umjammer/vavi-util-serdes.svg)](https://jitpack.io/#umjammer/vavi-util-serdes)
[![Java CI](https://github.com/umjammer/vavi-util-serdes/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-util-serdes/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-util-serdes/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-util-serdes/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-util-serdes

🌏 serialize/deserialize the world!

## Install

 * [maven](https://jitpack.io/#umjammer/vavi-util-serdes)

## Usage

### How To

 * define
   * mark by `@Serdes` to a pojo you want to serialize/deserialize
   * mark by `@Element` to a fields in the pojo and describe data structure like order, type and size etc.

```java
    @Serdes(bigEndian = false)
    public class NHDHeader {
        /** signature "T98HDDIMAGE.R0" */
        @Element(sequence = 1, validation = "new byte[] { 0x84, 0x57, 0x56, 0x72, 0x68, 0x68, 0x73, 0x77, 0x65, 0x71, 0x69, 0x46, 0x82, 0x48, 0, 0 }")
        byte[] sig = new byte[16];
        @Element(sequence = 2, value = "0x100")
        String comment;
        @Element(sequence = 3, value = "unsigned int")
        long headersize;
        @Element(sequence = 4, value = "unsigned int")
        long cylinders;
        @Element(sequence = 5, value = "unsigned short")
        int surfaces;
        @Element(sequence = 6, value = "unsigned short")
        int sectors;
        @Element(sequence = 7, value = "unsigned short")
        int sectorsize;
        @Element(sequence = 8)
        byte[] reserved = new byte[0xe2];
    }
```

 * read
    * using `Serdes.Util#deseralize` or `Serdes.Util#seralize` method you can serialize/deserialize

```jshelllanguage
    InputStream is = Files.newInputStream(Paths.get("/home/me/games/pc98/my.nhd"));
    NHDHeader header = new NHDHeader();
    Serdes.Util.deserialize(is, header);
```

### Feature

 * validation
 * condition
 * more ... https://github.com/umjammer/vavi-util-binaryio/blob/master/src/test/java/vavi/util/serdes/SerdesTest.java

### Binders

 [My plan](https://github.com/umjammer/vavi-util-screenscraping/wiki/AnnotationIinjectionIntoPOJO) will finally come into play.

| name        | ser  | des | implemented at           | library  | path language |
|-------------|:----:|:---:|--------------------------|----------|---------------|
| binary      | ✅ 🚧 |  ✅  | built-in                 | -        | -             |
| xml         |  ✅   |  ✅  | built-in                 | jackson  | xpath         |
| xml         |      |  ⏳  | built-in                 | jsoup    | xpath         |
| json        |      |  ⏳  | built-in                 | jayway   | jsonpath      |
| preferences |      |  ✅  | built-in                 | jdk      | -             |
| xml         |  -   |  ⏳  | vavi-util-screenscraping | saxon    | xpath         |
| json        |  -   |  ⏳  | vavi-util-screenscraping | jsonpath | jsonpath      |
| properties  |  -   |  ⏳  | vavi-commons             |          |               |
| ini         |  -   |  ⏳  | vavi-commons             |          |               |
| cli         |  -   |  ⏳  | klab-commons-cli         |          |               |
| csv         |  -   |  ⏳  | klab-commons-csv         |          |               |

## References

### Rivals

 * [Kaitai.io](http://kaitai.io/)
 * [struct](https://docs.python.org/3/library/struct.html)
 * https://github.com/raydac/java-binary-block-parser

## TODO

 * ~~got error cause betwixt.Validator is abstract~~ 
 * ~~Binary Binding~~
 * Text Binding
 * validation engine → "spi" or "method" like *condition*
 * ~~painfully **slow**, it's not sufficient performance for huge repetition (e.g. filesystem has a huge amount of files)~~
   * cache reflection ... It didn't have much effect
   * it's because of script engine "beanshell", using groovy resolve it. 🎉
 * enable subclass to set `BeanBinder` (currently uses the same BeanBinder of the super class)
 * toString bean binder? (implementation is apache commons?)
 * [MessagePack](https://github.com/msgpack/msgpack-java), [protobuf](https://github.com/protocolbuffers/protobuf/tree/master/java)
 * annotation translator
   * e.g. Jackson's `@JsonXmlProperty` → my `@Element`
 * xml
   * [simple](http://simple.sourceforge.net/home.php)
   * [jackson-dataformat-xml](https://github.com/FasterXML/jackson-dataformat-xml)
 * read write xml by jaxp document
   * see vavi-apps-ebook:EpubManipulator
 * bits operation
```java
   @BitElement(sequence = 1, value = "****....")
   int a;
   @BitElement(sequence = 2, value = "....**..")
   int b;
   @BitElement(sequence = 3, value = "......**|**......")
   int c;
```
 * `int[] type = "unsigned byte"`
 * use w/ default java serialization functionality Object(Input|Output)Stream
 * before/after method???
