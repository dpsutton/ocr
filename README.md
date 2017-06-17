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

## Deviations from spec ##

In order to make testing easier I have undertaken one extra bit and deviated from the original problem in a small and, in my view, unimportant way. I made a way to write files of these ascii numbers, necessitating a translation function from integers to the ascii numbers. To make it easier to randomly generate these numbers I also added the `0` (zero) to the numbers I can parse. I didn't particularly understand this omission in the original but it can quickly be removed if it proves undersirable (for instance, account `000000000` would be a valid account).

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
