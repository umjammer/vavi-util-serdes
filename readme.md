# vavi-util-binaryio

🌏 Read the world!

## How To

 * define

```java
    @Injector(bigEndian = false)
    public class NHDHeader {
        /** signature "T98HDDIMAGE.R0" */
        @Element(sequence = 1, validation = "new byte[] { 0x84, 0x57, 0x56, 0x72, 0x68, 0x68, 0x73, 0x77, 0x65, 0x71, 0x69, 0x46, 0x82, 0x48, 0, 0 })
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

```java
    InputStream is = Files.newInputStream(Paths.get("/home/me/games/pc98/my.nhd"));
    NHDHeader header = new NHDHeader();
    Injector.Util.inject(is, header);
```

## Feature

 * validation
 * condition
 * more

    https://github.com/umjammer/vavi-util-binaryio/blob/master/src/test/java/vavi/util/injection/InjectorTest.java

## Rivals

 * [Kaitai.io](http://kaitai.io/)
 * [struct](https://docs.python.org/3/library/struct.html)

## TODO

 * ~~got error cause Validator is abstract~~ 
 * ~~Binary Binding~~
 * Text Binding
 * validation engine -> "spi" or "method" like condition