set -e
python3 train_Struct2GO.py -train_data 'divided_data/mf_train_dataset' -valid_data 'divided_data/mf_valid_dataset' -branch 'mf' -labels_num 273 -label_network 'processed_data/label_mf_network '&
python3 train_Struct2GO.py -train_data 'divided_data/bp_train_dataset' -valid_data 'divided_data/bp_valid_dataset' -branch 'bp' -labels_num 809 -label_network 'processed_data/label_bp_network '&
python3 train_Struct2GO.py -train_data 'divided_data/cc_train_dataset' -valid_data 'divided_data/cc_valid_dataset' -branch 'cc' -labels_num 298 -label_network 'processed_data/label_cc_network '&
