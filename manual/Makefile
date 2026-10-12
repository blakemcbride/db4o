db4o-manual.pdf: db4o-manual.tex chapters.tex chapters/*.tex
	latexmk -pdf -interaction=nonstopmode -halt-on-error db4o-manual.tex

clean:
	latexmk -C db4o-manual.tex
	rm -f chapters/*.aux

.PHONY: clean
