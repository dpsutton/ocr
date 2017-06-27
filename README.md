# ocr

This is a solution to the bank account "OCR" [problem](https://github.com/codingdojo-org/codingdojo.org/blob/master/content/kata/BankOCR.md):

## Usage

There is a main entry point in `ocr.core`: `parse-file`. This should do all that is required for the problem itself. This requires a file formatted with the ascii spanning three lines and a blank line separating it from the next formwhich we can generate as well.

The file should look like the following:

``` text
    _  _  _  _  _  _     _
|_|  ||_   |  ||_| _||_||_|
  |  ||_|  |  | _||_   | _|

 _  _  _        _  _  _
 _| _||_ |_||_||_| _|  ||_|
 _| _||_|  |  | _| _|  |  |

 _     _     _
|_||_||_ |_| _||_|  ||_|  |
 _|  | _|  ||_   |  |  |  |
```

To generate a file, use the `ocr.files` namespace. The `create-file` function just needs a filename and a count for the number of ascii account numbers to create. This will place this file under the resources directory.

There are two parsing options, one that just raw parses the file and one that that attempts to correct when glyphs are misread or the checksum fails, parse and parse-completely, respectively.

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
