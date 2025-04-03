#! /bin/bash

function split() {
  input=$1
  fname=$(basename "$input")
  extension="${fname##*.}"
  fname="${fname%.*}"

  echo $input $fname $extension

  # Split the file into two files
  awk '{if ($15 == 0) print $0}' $input > $fname"_int_alloced."$extension
  awk '{if ($15 != 0) print $0}' $input > $fname"_only_batch."$extension
}



split $1
