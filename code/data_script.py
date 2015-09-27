from pandas import DataFrame as df
import pandas as pd 
import numpy as np 
import matplotlib.pyplot as plt

def plot_cov_mat(cov_mat):
	'''function to plot covariance matrix used first to identify the related features
	Not being used on current script anymore'''
	fig = plt.figure()

	ax = fig.add_subplot(111)
	axc = ax.matshow(cov_mat)
	fig.colorbar(axc)

	ax.set_xticks(np.arange(len(cov_mat.columns)))
	ax.set_yticks(np.arange(len(cov_mat.columns)))

	ax.set_xticklabels(cov_mat.columns,rotation='vertical')
	ax.set_yticklabels(cov_mat.columns)
	plt.show()
def getFilteredFeatures(data,X_cols,Y_cols,attributes,dict_attributes):
	'''return the most relevant features of the data'''
	global new_names_dict

	#Calculate correlation matrices
	X = data[X_cols]
	Y = data[Y_cols]
	cov_mat = data.corr(method='pearson')

	#for each features, put a threshold on the minimum correlation for features to be considered
	#very similar
	mostSimilar = []
	threshold = 0.6
	for c in cov_mat.columns:
		tmp = cov_mat.loc[c]
		tmp = tmp[abs(tmp) > threshold]
		tmp = tmp[tmp < 1.0]

		mostSimilar.append(tmp)
	
	#Sort features in descending order of number of related features with correlation greater than
	#threshold
	mostSimilar.sort(key =lambda x : -len(x))

	removed_features_set = set()

	used_features_list = []
	#store only the futures that have more influence, or the first ones in the previous list
	#and the ones that are more singular, the last ones in the previous list
	for i in range(len(mostSimilar)):
		#do not store labels
		if mostSimilar[i].name in Y_cols:
			continue

		if not mostSimilar[i].name in removed_features_set:
			new_name = mostSimilar[i].name
			for features in mostSimilar[i]:
				aux = mostSimilar[i][mostSimilar[i] ==features].first_valid_index()
				removed_features_set.add(aux)
				#edit the name of a feature so that we know which features are being represented
				new_name += "_"+ aux
			used_features_list.append(mostSimilar[i])
			#stores the feature and the features it represent just so we can use it later
			new_names_dict[mostSimilar[i].name] = new_name

	filteredAttributesIdx = []

	for f in used_features_list:
		filteredAttributesIdx.append(dict_attributes[f.name])

	filteredAttributesIdx.sort()
	#return list of tuples (attribute name, index) in sorted ascending order of indexes
	return [(attributes[x],x) for x in filteredAttributesIdx]

def preprocess_kdd_data(row):
	'''Add label column to dataset'''
	if row['attack_cat'] != "normal." :
   		return 1
   	return 0
  	

def convertRate(rate):
	return float(rate)/100



"""
	map of attributes:
		duration - dur
		protocol_type - proto
		src_bytes - sbytes
		dst_bytes - dbytes
		land - is_sm_ips_ports
		same_srv_rate - ct_srv_src #have to divide value by 100 to convert
		dst_host_same_srv_rate - ct_srv_dst #have to divide value by 100 to convert

"""
attributes_dict ={}

attributes_dict["duration"] = "dur"
attributes_dict["protocol_type"] = "proto"
attributes_dict["src_bytes"] = "sbytes"
attributes_dict["dst_bytes"] = "dbytes"
attributes_dict["land"] = "is_sm_ips_ports"
attributes_dict["same_srv_rate"] = "ct_srv_src"
attributes_dict["dst_host_same_srv_rate"] = "ct_srv_dst"
#index of values which should be converted
divide_set = set(["same_srv_rate","dst_host_same_srv_rate"])


"""

	Map of Attacks to categories

		back dos
		buffer_overflow u2r
		ftp_write r2l
		guess_passwd r2l
		imap r2l
		ipsweep probe
		land dos
		loadmodule u2r
		multihop r2l
		neptune dos
		nmap probe
		perl u2r
		phf r2l
		pod dos
		portsweep probe
		rootkit u2r
		satan probe
		smurf dos
		spy r2l
		teardrop dos
		warezclient r2l
		warezmaster r2l
		#From second dataset
		Reconnaissance 	probe
		DoS	dos
		Exploit	r2l
		Analysis	probe
		Fuzzers	dos
		Worms	r2l
		Generic	
		Shellcode	u2r
		Backdoors	r2l

"""
data_dir = "/Users/lrmneves/workspace/Fall 2015/BigData/data/Cyber Security"
attack_types_map ={}
attack_names_data = open(data_dir +"/data/training_attack_types.txt","r")

attack_names = attack_names_data.readlines()
for attack in attack_names:
	key_value = attack.split(" ")
	if(len(key_value) > 1):
		attack_types_map[key_value[0]+"."] = key_value[1][:-1]
		attack_types_map[key_value[0]] = key_value[1][:-1]

attack_names_data.close()

attack_types_map["Reconnaissance"] = "probe"
attack_types_map["DoS"] = "dos"
attack_types_map["Exploits"] = "r2l"
attack_types_map["Analysis"] = "probe"
attack_types_map["Fuzzers"] = "dos"
attack_types_map["Worms"] = "r2l"
attack_types_map["Generic"] = "generic"
attack_types_map["Shellcode"] = "u2r"
attack_types_map["Backdoor"] = "r2l"
attack_types_map["Normal"] = "normal"
attack_types_map["normal."] = "normal"

#Loading the attribute names

names_file = open(data_dir + "/data/kddcup.names.txt", "r")

kdd_attributes = names_file.readlines()
kdd_attributes = kdd_attributes[1:]
dict_attributes_kdd = {}
for i in range(len(kdd_attributes)):
	temp = kdd_attributes[i].split(":")
	kdd_attributes[i] = temp[0]
	dict_attributes_kdd[kdd_attributes[i]] = i

kdd_attributes.append("attack_cat")

dict_attributes_kdd["attack_cat"] = i+1
dict_attributes_kdd["label"] = i+2



names_file.close()
# my_data = np.genfromtxt('../data/kddcup.data_10_percent.csv', delimiter=',')

data_kdd  = df.from_csv(data_dir + '/data/kddcup.data_10_percent.csv',header=None  ,sep=',', index_col=False)
data_kdd.columns = kdd_attributes

#maps the attacks to their new categories


#create label column
data_kdd['label'] = data_kdd.apply (lambda row: preprocess_kdd_data(row),axis=1)
kdd_attributes.append("label")

#separate features and labels
X_cols = data_kdd.columns[0:len(data_kdd.columns)-2]
Y_cols = data_kdd.columns[len(data_kdd.columns)-2:]



new_names_dict ={}

filt_attributes_kdd_data =getFilteredFeatures(data_kdd,X_cols,Y_cols,kdd_attributes,dict_attributes_kdd)

print "Got features for kdd dataset"

data_unsw  = df.from_csv(data_dir + "/data/UNSW_NB15_training-set.csv",header=0  ,sep=',', index_col=False)

# for a_val, b_val in attack_types_map.iteritems():
# 	print data_kdd.loc[data_kdd.attack_cat==a_val, 'attack_cat']
# 	data_unsw.loc[data_unsw.attack_cat==a_val, 'attack_cat'] = b_val


unsw_attributes = list(data_unsw.columns.values)

dict_unsw_attributes = {}
for i in range(len(unsw_attributes)):

	dict_unsw_attributes[unsw_attributes[i]] = i

X_cols = data_unsw.columns[0:len(data_unsw.columns)-2]
Y_cols = data_unsw.columns[len(data_unsw.columns)-2:]

#filter the features on unsw dataset
filt_attributes_unsw_data = getFilteredFeatures(data_unsw,X_cols,Y_cols,unsw_attributes,dict_unsw_attributes)

print "Got features for unsw data"
final_attributes = []
#store all attributes to a list
for att in filt_attributes_kdd_data:
	final_attributes.append(att[0])


for att in filt_attributes_unsw_data:
	if att[0] in attributes_dict.values():
		continue
	else:
		#remove bad formatted id
		if att[0] == "\xef\xbb\xbfid":
			continue
		final_attributes.append(att[0])


#add label columns
final_attributes.append("attack_cat")
final_attributes.append("label")
#delete old id column
del data_unsw["\xef\xbb\xbfid"]

#create a list of rows as dicts to build a new data frame
row_list =[]
#store rows from kdd
for index, row in data_kdd.iterrows():
	row_dict = {}
	for c in final_attributes:
		if c in data_kdd.columns:
			row_dict[c] = row[c]
		else:
			row_dict[c] = pd.np.nan
	row_list.append(row_dict)
#store rows from unsw
for i, row in data_unsw.iterrows():
	index+=1
	row_dict = {}

	for c in final_attributes:
		if c in data_unsw.columns:
			row_dict[c] = row[c]
		#converter de kdd para unsw
		elif c in attributes_dict:
			col_name = attributes_dict[c]
			row_dict[c] = row[col_name] if not c in divide_set else convertRate(row[col_name])
		else:
			row_dict[c] = pd.np.nan
	row_list.append(row_dict)

print "Data size: "  + str(len(row_list))
#get final data
final_data =pd.DataFrame(row_list) 
#reorder columns
final_data = final_data[final_attributes]

#change names of columns data have aggregated values to the new values
keys = []
for att in new_names_dict:
	keys.append(att)
for k in keys:
	if new_names_dict[k] not in final_attributes:
		del new_names_dict[k]
final_data.replace({"attack_cat": attack_types_map},inplace=True)

final_data=final_data.rename(columns = new_names_dict)
final_data.interpolate(method = "linear", inplace = True,axis = 0)
# final_data.fillna(method='pad')
msk = np.random.rand(len(final_data)) < 0.80
final_data=final_data[~msk]
msk = np.random.rand(len(final_data)) < 0.80
train_data = final_data[msk]
test_data = final_data[~msk]
#save to csv
train_data.to_csv("../output/train_data.csv")
test_data.to_csv("../output/test_data.csv")

