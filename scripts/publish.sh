acp() {
	aws s3 cp $* &
}

acp target/scala-2.12/chem_prog_exp-opt.js s3://matthagy-com/target/scala-2.12/chem_prog_exp-opt.js
acp random_walk_scalajs.html s3://matthagy-com/random_walk.html
acp soft_spheres.html s3://matthagy-com/soft_spheres.html


wait
