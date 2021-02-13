# Brotli and GZip compression sample WARCs

The WARCs in this folder are used to test compression support. 
Each WARC contains a single HTML-page with the text 
_"Extremely simple webpage used for testing GZip and Brotli transmission compression."_.

The WARCs has been harvested using wget, specifying either none, GZip or Brotli as transmission compression. For completeness this was done with and without WARC-GZip compression. A sample call is
```
./wget_latest --delete-after --no-warc-keep-log --header="accept-encoding: br" --warc-file="transfer_compression_brotli" 'http://tokemon.sb.statsbiblioteket.dk'
```

