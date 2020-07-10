BCS - Block Clustering Segmentation
===================================

(c) Jan Zelený 2017

This is a reference implementation of the Block Clustering Segmentation algorithm for web page segmentation. It renders a given web page and discovers a list of visual segments using the BCS algorithm (see the paper below).

Installation
------------

The entire package can be built using maven. Just run 

```
mvn package
```

in the project root. This will create the target package in `target/` and install the necessary dependencies to `lib/`. 

Usage
-----

For starting the segmentation run

```
./run.sh <page_url> [<threshold>]
```

in the project root, where the threshold is the Clustering Threshold (CT) (0..1) which defaults to 0.3 when not specified. The tool renders the specified page and runs the segmentation. When completed correctly, three files are produced:

- *page_url*.png -- a preview of the page contents after being converted to color boxes (the used segmentation input)
- *page_url*-boxes-*threshold*.png -- the graphical segmentation result that shows the boundaries of the detected visual areas
- *page_url*-boxes-*threshold*.txt -- the textual segmentation result that contains the boundaries of the detected visual areas. For each area, it contains its *x* and *y* coordinates, *width*, *height* and average RGB color.

Please note that this implementation uses the experimental [CSSBox rendering engine](https://github.com/radkovo/CSSBox) for rendering the web pages (not a real web browser). CSSBox only supports a limited set of CSS3 and no JavaScript and therefore, it may fail on some modern real-world web pages.


Publication
-----------

When using BCS for your scientific work, please cite the following paper:

ZELENÝ Jan, BURGET Radek and ZENDULKA Jaroslav. Box Clustering Segmentation: A New Method for Vision-based Page Preprocessing. Information Processing and Management, vol. 53, no. 3, pp. 735-750. ISSN 0306-4573.

BibTeX entry:

```
@ARTICLE{FITPUB10821,
   author = "Jan Zelen\'{y} and Radek Burget and Jaroslav Zendulka",
   title = "Box clustering segmentation: A new method for vision-based web page preprocessing",
   pages = "735--750",
   journal = "Information Processing and Management",
   volume = 53,
   number = 3,
   year = 2017,
   ISSN = "0306-4573",
   doi = "10.1016/j.ipm.2017.02.002",
   language = "english",
   url = "https://www.fit.vut.cz/research/publication/10821"
}
```
