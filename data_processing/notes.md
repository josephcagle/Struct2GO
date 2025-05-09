
What I have done:

1. Run `data_processing/_convert_to_Struct2GO.py` to generate `data/protein_list.csv` from the APF splits.
2. Modified and run `predicted_protein_struct2map.py` to generate edge lists for all proteins in `../APF/AlphaFold Data/raw_data`.
3. Reimplemented `angel-master/spark-on-angel/examples/src/main/scala/com/tencent/angel/spark/examples/local/Node2VecExample.scala` in python in `node2vec_replacement.py`.
4. Changed `data/node2vec` to `data/struct_feature` as the output directory for `node2vec_replacement.py` as it was inconsistent across files.
5. Run `node2vec_replacement.py` to generate random walks for all proteins in `data/protein_list.csv`.
