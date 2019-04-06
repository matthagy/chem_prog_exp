acp() {
	echo $*
	aws s3 cp $* 
}

for f in ace-builds/src-noconflict/*.js; do
	acp $f s3://matthagy-com/$f
done

for f in ace-builds/src-noconflict/snippets/*.js; do
	acp $f s3://matthagy-com/$f
done
