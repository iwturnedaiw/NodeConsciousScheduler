CONV_INPUT=$1
AO_INPUT=$2
TARGET=for_heat_input.out

cat ${CONV_INPUT} | awk '{print $1, $12, $15, $8, -1, -1, -1}' | sort -n > wk

cat ${AO_INPUT} | awk '{print $1, $12}' | sort -n > wk1

join wk wk1 > wk2

cat wk2 | awk '{print $1, $2, $8, $3, $4, $5, $6, $7}' > wk3

mv wk3 ${TARGET}

sed -i '1d' ${TARGET}

sed -i -e "1i JobID\tconv_SOSub\tOSub_SOSub\tCore\tTa\tRatio\tSpaceclass\tTimeClass" ${TARGET}
sed -i "s/ /\t/g" ${TARGET}
sed -i "s/Spaceclass/Space class/g" ${TARGET}
sed -i "s/TimeClass/Time Class/g" ${TARGET}

rm wk wk1 wk2
