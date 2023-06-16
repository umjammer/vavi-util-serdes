[![](https://jitpack.io/v/umjammer/vavi-util-serdes.svg)](https://jitpack.io/#umjammer/vavi-util-serdes)
[![Java CI](https://github.com/umjammer/vavi-util-serdes/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-util-serdes/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-util-serdes/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-util-serdes/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-8-b07219)

# vavi-util-serdes

🌏 serialize/deserialize the world!

## Install

 * [jitpack](https://jitpack.io/#umjammer/vavi-util-serdes)

## How To

 * define

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

```jshelllanguage
    InputStream is = Files.newInputStream(Paths.get("/home/me/games/pc98/my.nhd"));
    NHDHeader header = new NHDHeader();
    Serdes.Util.deserialize(is, header);
```

## Feature

 * validation
 * condition
 * more

    https://github.com/umjammer/vavi-util-binaryio/blob/master/src/test/java/vavi/util/serdes/SerdesTest.java

## Binders

 [My plan](https://github.com/umjammer/vavi-util-screenscraping/wiki/AnnotationIinjectionIntoPOJO) will finally come into play.

|name|status|library|
|----|------|-------|
|binary| ✅ | this  |
|preferences| ✅ | this  |
|properties | ⏳ | vavi-commons |
|ini| ⏳ | vavi-commons |
|json | ⏳ | vavi-util-screenscraping |
|xml | ⏳ | vavi-util-screenscraping |
|cli | ⏳ | klab-commons-cli |
|csv | ⏳ | klab-commons-csv |


## Rivals

 * [Kaitai.io](http://kaitai.io/)
 * [struct](https://docs.python.org/3/library/struct.html)
 * https://github.com/raydac/java-binary-block-parser

## TODO

 * ~~got error cause betwixt.Validator is abstract~~ 
 * ~~Binary Binding~~
 * Text Binding
 * validation engine -> "spi" or "method" like *condition*
 * appallingly slow, it's not sufficient performance for huge repetition (e.g. filesystem has a huge amount of files)
 * enable subclass to set BeanBinder (currently uses the same BeanBinder of the super class)
 * toString bean binder? (implementation is apache commons?)
 * [MessagePack](https://github.com/msgpack/msgpack-java), [protobuf](https://github.com/protocolbuffers/protobuf/tree/master/java)
 * annotation translator
   * e.g. Jackson's `@JsonProperty` -> my `@Target`
 * xml
   * [simple](http://simple.sourceforge.net/home.php)
   * [jackson-dataformat-xml](https://github.com/FasterXML/jackson-dataformat-xml)
 * read write xml by jaxp document
   * see vavi-apps-ebook:EpubManipulator
